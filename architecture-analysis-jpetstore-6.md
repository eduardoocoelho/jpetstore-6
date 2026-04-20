# 1. Visão geral técnica do projeto

O repositório implementa um monólito web Java tradicional, empacotado como `WAR`, com renderização server-side. A entrada HTTP é feita por Stripes, a injeção de dependências e transações por Spring, a persistência por MyBatis com SQL em XML, as views por JSP/JSTL e o banco por HSQLDB embutido. Isso aparece em [`pom.xml`](./pom.xml), [`src/main/webapp/WEB-INF/web.xml`](./src/main/webapp/WEB-INF/web.xml) e [`src/main/webapp/WEB-INF/applicationContext.xml`](./src/main/webapp/WEB-INF/applicationContext.xml).

A estrutura principal do backend está organizada por camadas técnicas:

- `src/main/java/org/mybatis/jpetstore/web/actions`
- `src/main/java/org/mybatis/jpetstore/service`
- `src/main/java/org/mybatis/jpetstore/mapper`
- `src/main/java/org/mybatis/jpetstore/domain`

As views ficam em `src/main/webapp/WEB-INF/jsp`, separadas por funcionalidade:

- `account`
- `catalog`
- `cart`
- `order`
- `common`

O ponto inicial público da aplicação é [`src/main/webapp/index.html`](./src/main/webapp/index.html), que encaminha para `actions/Catalog.action`. Portanto, o fluxo real de navegação entra por uma página estática e depois passa para o dispatcher Stripes.

Do ponto de vista arquitetural, o sistema é simples de seguir, mas não é “flat”: ele já contém separação explícita entre UI web, serviços de aplicação, domínio e persistência. Isso o torna um caso interessante para um TCC sobre modernização incremental e modularização de monólitos legados.

# 2. Fluxo técnico de login

## Entrada do fluxo

O login é exposto pela UI compartilhada em [`src/main/webapp/WEB-INF/jsp/common/IncludeTop.jsp`](./src/main/webapp/WEB-INF/jsp/common/IncludeTop.jsp), onde o link “Sign In” chama `AccountActionBean` com o evento `signonForm`.

A tela de login está em [`src/main/webapp/WEB-INF/jsp/account/SignonForm.jsp`](./src/main/webapp/WEB-INF/jsp/account/SignonForm.jsp). O formulário envia `username` e `password` para o método `signon` da action bean.

## Controller/action envolvido

O fluxo é centralizado em [`src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java):

- `signonForm()`: exibe a JSP de login
- `signon()`: executa a autenticação
- `signoff()`: invalida a sessão
- `newAccount()` e `editAccount()`: tangenciam o mesmo subdomínio de identidade/conta

Aspectos técnicos relevantes:

- a classe é `@SessionScope`
- ela injeta `AccountService` e `CatalogService` via `@SpringBean`
- ela mantém estado de autenticação na própria action bean

Ou seja, o controller não é stateless; ele funciona como objeto conversacional de sessão.

## Sequência técnica ponta a ponta

1. O request chega em `*.action`, configurado em [`src/main/webapp/WEB-INF/web.xml`](./src/main/webapp/WEB-INF/web.xml).
2. `StripesFilter` resolve a `ActionBean` no pacote `org.mybatis.jpetstore.web`.
3. O formulário de [`SignonForm.jsp`](./src/main/webapp/WEB-INF/jsp/account/SignonForm.jsp) chama `AccountActionBean.signon()`.
4. `signon()` chama `accountService.getAccount(getUsername(), getPassword())`.
5. `AccountService.getAccount(username, password)` delega para `AccountMapper.getAccountByUsernameAndPassword(...)`.
6. O SQL correspondente em [`src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml) consulta `ACCOUNT`, `PROFILE`, `SIGNON` e `BANNERDATA`.
7. Se a conta não for encontrada, a action grava mensagem e retorna à JSP de login.
8. Se a conta for encontrada:
   - zera a senha em memória com `account.setPassword(null)`
   - carrega `myList` com base na categoria favorita via `CatalogService`
   - marca `authenticated = true`
   - grava a própria bean na sessão
   - redireciona para `CatalogActionBean`

## Camadas e dependências envolvidas

- View: [`SignonForm.jsp`](./src/main/webapp/WEB-INF/jsp/account/SignonForm.jsp)
- Action/controller: [`AccountActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java)
- Service: [`AccountService.java`](./src/main/java/org/mybatis/jpetstore/service/AccountService.java)
- Domínio: [`Account.java`](./src/main/java/org/mybatis/jpetstore/domain/Account.java)
- Persistência: [`AccountMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/AccountMapper.java) e [`AccountMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml)

Há também dependência indireta de catálogo, porque o login já carrega a lista personalizada `myList`:

- [`CatalogService.java`](./src/main/java/org/mybatis/jpetstore/service/CatalogService.java)

## Onde estão as regras de negócio

As regras de autenticação são poucas e estão distribuídas assim:

- validação básica de campos obrigatórios em `AccountActionBean` (`@Validate`)
- autenticação por busca direta `username + password`
- pós-login carregando preferências e lista favorita
- gestão de sessão e estado autenticado na própria action

## Avaliação arquitetural do fluxo

O fluxo é tecnicamente simples, mas não está bem delimitado como módulo independente.

Pontos observados:

- a autenticação está acoplada à sessão web
- o mesmo objeto `AccountActionBean` acumula UI, estado de sessão e parte da orquestração
- o login já depende de catálogo para montar `myList`
- o pedido lê o estado de conta da sessão, o que espalha a dependência do subdomínio de identidade

Além disso, a persistência mostra senha em texto puro:

- a tabela `SIGNON` em [`src/main/resources/database/jpetstore-hsqldb-schema.sql`](./src/main/resources/database/jpetstore-hsqldb-schema.sql)
- os inserts em [`src/main/resources/database/jpetstore-hsqldb-dataload.sql`](./src/main/resources/database/jpetstore-hsqldb-dataload.sql)
- o `updateSignon` e `insertSignon` em [`AccountMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml)

Para um TCC de modernização, isso é um ponto forte de diagnóstico: a fronteira “identidade/conta” existe, mas ainda está embutida em um modelo web stateful e com segurança fraca.

# 3. Fluxo técnico de catálogo

## Entrada do fluxo

O catálogo é o principal ponto de navegação da aplicação. O fluxo começa após o `Enter the Store` em [`src/main/webapp/index.html`](./src/main/webapp/index.html) e é manipulado por [`src/main/java/org/mybatis/jpetstore/web/actions/CatalogActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CatalogActionBean.java).

As principais views são:

- [`src/main/webapp/WEB-INF/jsp/catalog/Main.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Main.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/Category.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Category.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/Product.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Product.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/Item.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Item.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/SearchProducts.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/SearchProducts.jsp)

## Métodos e sequência técnica

### 1. Tela principal

`CatalogActionBean.viewMain()` retorna a JSP `Main.jsp`.

Essa tela contém:

- sidebar com links de categorias
- imagem mapeada com links diretos para categorias
- cabeçalho global com busca textual

### 2. Categoria

Ao abrir uma categoria:

1. a request chama `CatalogActionBean.viewCategory()`
2. se `categoryId != null`, a action chama:
   - `catalogService.getProductListByCategory(categoryId)`
   - `catalogService.getCategory(categoryId)`
3. o resultado é encaminhado para [`Category.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Category.jsp)

### 3. Produto

Ao abrir um produto:

1. a request chama `CatalogActionBean.viewProduct()`
2. a action chama:
   - `catalogService.getItemListByProduct(productId)`
   - `catalogService.getProduct(productId)`
3. o resultado vai para [`Product.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Product.jsp)

### 4. Item

Ao abrir um item:

1. a request chama `CatalogActionBean.viewItem()`
2. a action chama `catalogService.getItem(itemId)`
3. o `Item` retornado já vem com `Product` embutido
4. a action usa `item.getProduct()` para preencher `product`
5. a view renderiza [`Item.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Item.jsp)

### 5. Busca

Na busca:

1. o formulário do header em [`IncludeTop.jsp`](./src/main/webapp/WEB-INF/jsp/common/IncludeTop.jsp) chama `CatalogActionBean.searchProducts()`
2. a action valida que `keyword` não é vazia
3. chama `catalogService.searchProductList(keyword.toLowerCase())`
4. o service divide a string em termos por espaço e agrega resultados
5. a JSP resultante é [`SearchProducts.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/SearchProducts.jsp)

## Service, domínio e persistência envolvidos

O catálogo está concentrado em [`src/main/java/org/mybatis/jpetstore/service/CatalogService.java`](./src/main/java/org/mybatis/jpetstore/service/CatalogService.java), que depende de:

- [`CategoryMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/CategoryMapper.java)
- [`ProductMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/ProductMapper.java)
- [`ItemMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/ItemMapper.java)

Os XMLs relevantes são:

- [`CategoryMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/CategoryMapper.xml)
- [`ProductMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/ProductMapper.xml)
- [`ItemMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml)

Os objetos de domínio envolvidos:

- [`Category.java`](./src/main/java/org/mybatis/jpetstore/domain/Category.java)
- [`Product.java`](./src/main/java/org/mybatis/jpetstore/domain/Product.java)
- [`Item.java`](./src/main/java/org/mybatis/jpetstore/domain/Item.java)

## Onde estão as regras de negócio

As regras do catálogo são leves e estão principalmente em:

- `CatalogService.searchProductList(...)`
- `CatalogService.isItemInStock(...)`
- mapeamento SQL de `Item`, que já agrega `Product`

Em especial, [`ItemMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml) é central porque:

- retorna lista de itens por produto com `product.*` embutido
- retorna item com `quantity` do inventário
- serve tanto ao catálogo quanto ao carrinho e ao pedido

## Avaliação arquitetural do fluxo

O fluxo de catálogo é o mais próximo de um módulo bem delimitado.

Pontos positivos:

- action dedicada
- service dedicado
- mappers separados por entidade do subdomínio
- baixa complexidade transacional

Pontos que enfraquecem essa delimitação:

- categorias estão hardcoded em JSP e também em `AccountActionBean`
- `categoryList` existe em `CatalogActionBean`, mas não sustenta a navegação principal atual
- descrições e banners armazenam HTML no banco, o que mistura dado e apresentação

Exemplos concretos:

- categorias fixas no header em [`IncludeTop.jsp`](./src/main/webapp/WEB-INF/jsp/common/IncludeTop.jsp)
- categorias fixas na home em [`Main.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Main.jsp)
- lista fixa de categorias em [`AccountActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java)
- HTML persistido em [`jpetstore-hsqldb-dataload.sql`](./src/main/resources/database/jpetstore-hsqldb-dataload.sql)

Conclusão: o catálogo é relativamente coeso e é uma boa fronteira inicial de modularização, mas ainda traz acoplamento de apresentação e duplicação de referência estática.

# 4. Fluxo técnico de compra/checkout

## Equivalente funcional no código

O sistema não possui um fluxo principal implementado como “CheckoutService” ou algo nomeado exatamente assim. O equivalente funcional real é o wizard de pedido controlado por [`src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java), apoiado pelo carrinho em [`CartActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java).

Há um método `checkOut()` em `CartActionBean`, mas não encontrei uso relevante dele no fluxo principal. O caminho efetivo do usuário é “Proceed to Checkout” em [`Cart.jsp`](./src/main/webapp/WEB-INF/jsp/cart/Cart.jsp), que chama `OrderActionBean.newOrderForm`.

## Subfluxo 1: carrinho

### Entrada

O item é adicionado ao carrinho a partir de:

- [`Product.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Product.jsp)
- [`Item.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Item.jsp)

Ambos chamam `CartActionBean.addItemToCart`.

### Sequência técnica

1. A UI envia `workingItemId`.
2. `CartActionBean.addItemToCart()` valida o ID.
3. Se o item já existe no carrinho, incrementa quantidade via `cart.incrementQuantityByItemId`.
4. Caso contrário:
   - chama `catalogService.isItemInStock(workingItemId)`
   - chama `catalogService.getItem(workingItemId)`
   - chama `cart.addItem(item, isInStock)`
5. A action retorna [`Cart.jsp`](./src/main/webapp/WEB-INF/jsp/cart/Cart.jsp)

### Regras de negócio

As regras do carrinho estão espalhadas entre:

- [`CartActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java)
- [`Cart.java`](./src/main/java/org/mybatis/jpetstore/domain/Cart.java)
- [`CartItem.java`](./src/main/java/org/mybatis/jpetstore/domain/CartItem.java)

Em especial:

- subtotal em `Cart.getSubTotal()`
- total por item em `CartItem.calculateTotal()`
- remoção e atualização de quantidade no agregado `Cart`

Não existe um `CartService`, então o carrinho é um estado de sessão manipulado diretamente pela camada web.

## Subfluxo 2: criação do pedido

### Entrada

O botão “Proceed to Checkout” em [`Cart.jsp`](./src/main/webapp/WEB-INF/jsp/cart/Cart.jsp) chama `OrderActionBean.newOrderForm()`.

### Sequência técnica

1. `newOrderForm()` obtém `AccountActionBean` e `CartActionBean` diretamente da sessão HTTP.
2. Executa `clear()` no estado anterior da order action.
3. Se o usuário não estiver autenticado, redireciona para login.
4. Se o carrinho existir, chama `order.initOrder(accountBean.getAccount(), cartBean.getCart())`.
5. O pedido é pré-montado com dados de conta e itens do carrinho.
6. A UI segue para [`NewOrderForm.jsp`](./src/main/webapp/WEB-INF/jsp/order/NewOrderForm.jsp)

Esse ponto é importante: a montagem inicial do pedido acontece no objeto de domínio [`Order.java`](./src/main/java/org/mybatis/jpetstore/domain/Order.java), não no service.

## Subfluxo 3: wizard de checkout

O método `newOrder()` em `OrderActionBean` controla o wizard:

1. Se `shippingAddressRequired == true`, encaminha para [`ShippingForm.jsp`](./src/main/webapp/WEB-INF/jsp/order/ShippingForm.jsp)
2. Caso contrário, se `confirmed == false`, encaminha para [`ConfirmOrder.jsp`](./src/main/webapp/WEB-INF/jsp/order/ConfirmOrder.jsp)
3. Se `confirmed == true` e o pedido existe:
   - chama `orderService.insertOrder(order)`
   - limpa o carrinho
   - grava mensagem de sucesso
   - encaminha para [`ViewOrder.jsp`](./src/main/webapp/WEB-INF/jsp/order/ViewOrder.jsp)

Ou seja, a orquestração do processo de compra está concentrada na action web.

## Subfluxo 4: persistência do pedido

O núcleo transacional está em [`src/main/java/org/mybatis/jpetstore/service/OrderService.java`](./src/main/java/org/mybatis/jpetstore/service/OrderService.java).

### `insertOrder(Order order)`

1. Gera `orderId` com `getNextId("ordernum")`
2. Percorre line items
3. Decrementa o estoque item a item via `ItemMapper.updateInventoryQuantity`
4. Insere registro em `ORDERS`
5. Insere registro em `ORDERSTATUS`
6. Insere cada `LINEITEM`

Persistência envolvida:

- [`OrderMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/OrderMapper.java)
- [`LineItemMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/LineItemMapper.java)
- [`SequenceMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/SequenceMapper.java)
- [`ItemMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/ItemMapper.java)

e os XMLs:

- [`OrderMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/OrderMapper.xml)
- [`LineItemMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/LineItemMapper.xml)
- [`SequenceMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/SequenceMapper.xml)
- [`ItemMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml)

## Subfluxo 5: visualização de pedidos

O histórico de pedidos usa:

- `OrderActionBean.listOrders()`
- `OrderActionBean.viewOrder()`
- [`ListOrders.jsp`](./src/main/webapp/WEB-INF/jsp/order/ListOrders.jsp)
- [`ViewOrder.jsp`](./src/main/webapp/WEB-INF/jsp/order/ViewOrder.jsp)

`viewOrder()` chama `orderService.getOrder(orderId)`, que:

1. busca o pedido em `ORDERS + ORDERSTATUS`
2. busca os line items
3. para cada line item, busca o item correspondente
4. injeta quantidade de inventário atual no objeto `Item`

## Onde estão as principais regras de negócio

As regras mais importantes do fluxo de compra estão distribuídas entre:

- `Cart` e `CartItem`: subtotal e cálculo por item
- `Order.initOrder(...)`: montagem inicial do pedido
- `OrderService.insertOrder(...)`: geração de ID, baixa de estoque e persistência transacional
- `OrderActionBean.newOrder()`: orquestração do wizard

## Avaliação arquitetural do fluxo

Esse fluxo existe funcionalmente, mas está acoplado.

Diagnóstico:

- o carrinho não tem service próprio
- a camada web lê actions diretamente da sessão
- o wizard de pedido depende fortemente de estado conversacional
- a baixa de estoque ocorre sem revalidação final explícita
- a sequência de pedido é gerida manualmente em tabela `SEQUENCE`

Portanto, o fluxo de compra não está bem encapsulado como módulo de aplicação isolado; ele está funcionalmente claro, mas tecnicamente espalhado entre UI stateful, domínio e serviço transacional.

# 5. Estrutura de pacotes e responsabilidades

## Pacotes principais

### `org.mybatis.jpetstore.web.actions`

Responsável por:

- entrada HTTP via Stripes
- binding de parâmetros de formulário
- escolha de JSP de saída
- navegação entre telas
- manutenção de estado em sessão

Classes principais:

- [`AccountActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java)
- [`CatalogActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CatalogActionBean.java)
- [`CartActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java)
- [`OrderActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java)
- [`AbstractActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/AbstractActionBean.java)

### `org.mybatis.jpetstore.service`

Responsável por casos de uso de aplicação e coordenação entre mappers.

Classes:

- [`AccountService.java`](./src/main/java/org/mybatis/jpetstore/service/AccountService.java)
- [`CatalogService.java`](./src/main/java/org/mybatis/jpetstore/service/CatalogService.java)
- [`OrderService.java`](./src/main/java/org/mybatis/jpetstore/service/OrderService.java)

### `org.mybatis.jpetstore.mapper`

Responsável pela fronteira de acesso a dados com MyBatis.

Interfaces:

- [`AccountMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/AccountMapper.java)
- [`CategoryMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/CategoryMapper.java)
- [`ProductMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/ProductMapper.java)
- [`ItemMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/ItemMapper.java)
- [`OrderMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/OrderMapper.java)
- [`LineItemMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/LineItemMapper.java)
- [`SequenceMapper.java`](./src/main/java/org/mybatis/jpetstore/mapper/SequenceMapper.java)

### `org.mybatis.jpetstore.domain`

Responsável pelo modelo de domínio compartilhado entre web, service e persistência.

Classes:

- [`Account.java`](./src/main/java/org/mybatis/jpetstore/domain/Account.java)
- [`Category.java`](./src/main/java/org/mybatis/jpetstore/domain/Category.java)
- [`Product.java`](./src/main/java/org/mybatis/jpetstore/domain/Product.java)
- [`Item.java`](./src/main/java/org/mybatis/jpetstore/domain/Item.java)
- [`Cart.java`](./src/main/java/org/mybatis/jpetstore/domain/Cart.java)
- [`CartItem.java`](./src/main/java/org/mybatis/jpetstore/domain/CartItem.java)
- [`Order.java`](./src/main/java/org/mybatis/jpetstore/domain/Order.java)
- [`LineItem.java`](./src/main/java/org/mybatis/jpetstore/domain/LineItem.java)
- [`Sequence.java`](./src/main/java/org/mybatis/jpetstore/domain/Sequence.java)

### Views em `WEB-INF/jsp`

Responsabilidade por renderização server-side, separadas por funcionalidade:

- `account`
- `catalog`
- `cart`
- `order`
- `common`

## Padrão de organização adotado

O padrão predominante é por camadas técnicas:

- web
- service
- mapper
- domain

Ao mesmo tempo, há um traço híbrido na camada de views, porque as JSPs estão organizadas por feature.

## Sinais de acoplamento excessivo

1. Dependência direta entre actions via sessão HTTP

`OrderActionBean` lê `AccountActionBean` e `CartActionBean` diretamente da sessão. Isso é um acoplamento de runtime entre controllers.

2. Estado na camada web

As principais actions estão em `@SessionScope`, o que torna a camada web stateful.

3. Domínio compartilhado por todas as camadas

O pacote `domain` é usado por web, services, mappers e views. Isso facilita reaproveitamento, mas reduz isolamento.

4. UI acoplada a referências estáticas

Categorias e preferências aparecem hardcoded em mais de um lugar.

5. Conteúdo de apresentação persistido no banco

Descrições e banners armazenam HTML em vez de apenas dados de domínio.

## Sinais de baixa coesão ou mistura de responsabilidades

- `Account` carrega validações Stripes, aproximando domínio e camada web
- `Order` contém muitos dados heterogêneos: cobrança, entrega, status, pagamento, itens
- `OrderActionBean` concentra navegação, coordenação de wizard e parte do processo
- `CartActionBean` manipula diretamente regras de carrinho sem service intermediário

## Possíveis fronteiras de domínio sugeridas pela estrutura

A estrutura atual sugere os seguintes subdomínios:

- conta/perfil/autenticação
- catálogo
- carrinho
- pedido/checkout
- inventário/suporte operacional

## Pacotes centrais para modularização futura

Os mais relevantes para uma estratégia de modularização são:

- `service`
- `domain`
- `mapper`
- `web/actions`
- `WEB-INF/jsp/account`
- `WEB-INF/jsp/catalog`
- `WEB-INF/jsp/cart`
- `WEB-INF/jsp/order`

# 6. Tecnologias identificadas

## Linguagem principal e versão

- Java
- alvo configurado: Java 17

Evidência:

- [`pom.xml`](./pom.xml)

## Framework web

- Stripes `1.6.0`

Evidências:

- dependência em [`pom.xml`](./pom.xml)
- `StripesFilter` e `DispatcherServlet` em [`src/main/webapp/WEB-INF/web.xml`](./src/main/webapp/WEB-INF/web.xml)

## Framework de persistência

- MyBatis `3.5.19`
- MyBatis-Spring `3.0.5`

Evidências:

- [`pom.xml`](./pom.xml)
- `SqlSessionFactoryBean` e `mybatis:scan` em [`src/main/webapp/WEB-INF/applicationContext.xml`](./src/main/webapp/WEB-INF/applicationContext.xml)

## Spring

- Spring Context
- Spring JDBC
- Spring Tx
- Spring Test
- `spring-web` em versão distinta, com comentário explícito de transição

Evidência:

- [`pom.xml`](./pom.xml)

## Banco de dados

- HSQLDB embutido

Evidências:

- dependência em [`pom.xml`](./pom.xml)
- `jdbc:embedded-database` em [`src/main/webapp/WEB-INF/applicationContext.xml`](./src/main/webapp/WEB-INF/applicationContext.xml)
- schema em [`src/main/resources/database/jpetstore-hsqldb-schema.sql`](./src/main/resources/database/jpetstore-hsqldb-schema.sql)

## Mecanismo de views/template

- JSP
- JSTL
- taglibs Apache Standard
- tags do Stripes

Evidências:

- dependências em [`pom.xml`](./pom.xml)
- imports de taglib em [`src/main/webapp/WEB-INF/jsp/common/IncludeTop.jsp`](./src/main/webapp/WEB-INF/jsp/common/IncludeTop.jsp)

## Sistema de build e gerenciamento de dependências

- Maven
- Maven Wrapper (`mvnw`)

Evidências:

- [`pom.xml`](./pom.xml)
- [`mvnw`](./mvnw)

## Container/servidor de aplicação

Perfis declarados:

- Tomcat 9
- TomEE 8
- WildFly 26
- Liberty EE8
- Jetty 12
- GlassFish 5
- Payara 5
- Resin

Evidência:

- perfis em [`pom.xml`](./pom.xml)

Além disso:

- Dockerfile usa `cargo:run -P tomcat90`, mas o README documenta `tomcat9`

Evidências:

- [`Dockerfile`](./Dockerfile)
- [`README.md`](./README.md)

Observação: há uma inconsistência textual aí; o POM mostra perfil `tomcat9`, não `tomcat90`.

## Bibliotecas relevantes

- SLF4J (`slf4j-api`, `slf4j-simple`)
- Spring Batch Infrastructure
- AssertJ
- Mockito
- JUnit Jupiter
- Selenide
- Selenium HtmlUnit driver

Evidência:

- [`pom.xml`](./pom.xml)

## Testes

Tipos identificados:

- testes de domínio
- testes de mapper com banco embutido
- testes de service com Mockito
- testes de action bean
- teste de navegação de telas com Selenide

Arquivos representativos:

- [`src/test/java/org/mybatis/jpetstore/service/OrderServiceTest.java`](./src/test/java/org/mybatis/jpetstore/service/OrderServiceTest.java)
- [`src/test/java/org/mybatis/jpetstore/mapper/MapperTestContext.java`](./src/test/java/org/mybatis/jpetstore/mapper/MapperTestContext.java)
- [`src/test/java/org/mybatis/jpetstore/ScreenTransitionIT.java`](./src/test/java/org/mybatis/jpetstore/ScreenTransitionIT.java)

## Arquivos de configuração importantes

- [`pom.xml`](./pom.xml)
- [`src/main/webapp/WEB-INF/web.xml`](./src/main/webapp/WEB-INF/web.xml)
- [`src/main/webapp/WEB-INF/applicationContext.xml`](./src/main/webapp/WEB-INF/applicationContext.xml)
- [`src/main/webapp/WEB-INF/beans.xml`](./src/main/webapp/WEB-INF/beans.xml)
- [`src/main/resources/database/jpetstore-hsqldb-schema.sql`](./src/main/resources/database/jpetstore-hsqldb-schema.sql)
- [`src/main/resources/database/jpetstore-hsqldb-dataload.sql`](./src/main/resources/database/jpetstore-hsqldb-dataload.sql)
- XMLs em `src/main/resources/org/mybatis/jpetstore/mapper`

# 7. Arquitetura atual do sistema

## Estilo arquitetural predominante

O estilo predominante é MVC server-side monolítico.

Mais precisamente:

- front controller via Stripes
- views renderizadas no servidor em JSP
- camada de serviços de aplicação
- persistência relacional orientada a mapper

Não há API REST separada, nem frontend desacoplado.

## Principais camadas e responsabilidades

### Camada web

Responsável por:

- receber requests
- manter estado de sessão
- ligar formulários e parâmetros a objetos Java
- decidir forward/redirect

Arquivos:

- `web/actions`
- `WEB-INF/jsp`

### Camada de aplicação

Responsável por:

- coordenar casos de uso
- encapsular transações
- orquestrar chamadas a mappers

Arquivos:

- `service`

### Camada de domínio

Responsável por:

- representar conta, produto, item, carrinho, pedido etc.
- concentrar parte de regras de montagem e cálculo

Arquivos:

- `domain`

### Camada de persistência

Responsável por:

- executar SQL explícito
- mapear linhas para objetos de domínio

Arquivos:

- `mapper`
- `resources/.../mapper/*.xml`

## Pontos de entrada da aplicação

1. Página inicial estática:
   - [`src/main/webapp/index.html`](./src/main/webapp/index.html)
2. Dispatcher Stripes:
   - `*.action` configurado em [`src/main/webapp/WEB-INF/web.xml`](./src/main/webapp/WEB-INF/web.xml)

## Componentes centrais

Os componentes mais centrais do ponto de vista arquitetural são:

- `AccountActionBean`
- `CatalogActionBean`
- `CartActionBean`
- `OrderActionBean`
- `CatalogService`
- `OrderService`
- `ItemMapper.xml`
- `OrderMapper.xml`
- `AccountMapper.xml`

## Como ocorre o acesso a dados

O acesso a dados acontece por interfaces mapper MyBatis, com SQL escrito manualmente em XML.

Fluxo típico:

1. service chama mapper
2. mapper executa statement XML
3. MyBatis materializa objetos do pacote `domain`

Não há camada repository independente do MyBatis.

## Como a interface se conecta ao backend

A interface usa:

- `<stripes:form>`
- `<stripes:link>`
- `<stripes:param>`

Ou seja, a JSP já conhece diretamente a action bean que tratará a interação. Isso acopla a view ao controlador Stripes.

## Como as regras de negócio estão distribuídas

As regras estão distribuídas entre quatro pontos:

1. actions
   - navegação
   - estado de sessão
   - mensagens de erro
2. services
   - orquestração
   - transação
3. domínio
   - subtotal do carrinho
   - montagem do pedido
4. SQL/mappers
   - parte da modelagem de agregados via joins e aliases

Portanto, não existe uma camada de domínio rica e isolada; o sistema é híbrido.

## Dependências estruturais importantes

- `OrderActionBean` depende de estado de `AccountActionBean` e `CartActionBean`
- `AccountActionBean` depende de `CatalogService` para `myList`
- `CartActionBean` depende de `CatalogService`
- `OrderService` depende de quatro mappers diferentes
- `ItemMapper.xml` serve a catálogo, carrinho e pedido

## Gargalos arquiteturais percebidos

1. Estado web em sessão

O estado de conta, carrinho e pedido fica espalhado em action beans stateful.

2. Acoplamento entre controllers

O pedido lê outras actions da sessão, em vez de depender de serviços bem definidos.

3. Modelo de domínio compartilhado e anêmico

O domínio é reutilizado por todas as camadas, mas não protege bem fronteiras.

4. Mistura de tecnologias legadas

XML Spring, XML MyBatis, `web.xml`, JSP, Stripes e SQL manual convivem simultaneamente.

5. Dependência de apresentação no banco

HTML persistido no banco dificulta evolução da interface e migração para novos frontends.

## Pontos que dificultariam manutenção, evolução ou modularização

- autenticação em texto puro
- sessão como mecanismo de integração entre fluxos
- ausência de contratos explícitos entre contextos de negócio
- carrinho sem service
- wizard de checkout centrado na camada web
- referências hardcoded de categorias/preferências
- inconsistência de chave de sessão (`accountBean` versus `"/actions/Account.action"`)

## Pontos que favorecem futura modernização

- separação já visível em camadas
- serviços pequenos e legíveis
- SQL explícito e rastreável
- domínio funcional claro
- mappers já segmentados
- suíte de testes presente
- catálogo relativamente bem delimitado

# 8. Pontos de atenção para manutenção e modernização

## 1. Segurança de autenticação

O sistema usa senha em texto puro e comparação direta no banco.

Isso é tecnicamente simples, mas inadequado para evolução real.

## 2. Acoplamento de sessão

O uso de `@SessionScope` nas actions e leitura cruzada de objetos na sessão cria forte dependência de fluxo conversacional.

Isso dificulta:

- testes mais realistas da camada web
- extração de APIs
- paralelização de navegação
- modularização por contexto

## 3. Mistura de domínio e web

`Account` contém validação Stripes, aproximando modelo e framework web.

Isso é um indício de baixo isolamento do domínio.

## 4. Pedido como agregado grande

`Order` concentra:

- dados de usuário
- cobrança
- entrega
- pagamento
- status
- itens

Esse desenho facilita o monólito original, mas dificulta modularização fina.

## 5. Carrinho sem fronteira de aplicação

Hoje o carrinho é um agregado em sessão, manipulado diretamente por controller. Para modernização, ele provavelmente precisaria ganhar:

- um service próprio
- uma política explícita de persistência ou sessão
- contrato de integração com checkout

## 6. Dependência de HTML persistido

Descrições e banners no banco contêm fragmentos de HTML. Isso é um obstáculo para:

- APIs headless
- frontend moderno
- sanitização e governança de conteúdo

## 7. Stack parcialmente em transição

O POM mostra um cenário de transição tecnológica:

- Java 17
- Spring 6.2 em parte
- `spring-web` ainda em 5.3
- `web.xml` em schema Java EE antigo

Isso é relevante para uma narrativa de modernização incremental: o projeto já mostra sinais de adaptação parcial, não de reescrita completa.

## 8. Cobertura de testes desigual

Durante esta análise, `./mvnw -q test` executou com sucesso.

Mesmo assim, a cobertura não é homogênea:

- mappers e services têm testes úteis
- action beans têm vários testes superficiais
- o fluxo de tela existe, mas como teste integrado separado

Isso é bom para manutenção básica, mas não elimina o risco arquitetural.

# 9. Possíveis fronteiras iniciais de modularização

## 1. Catálogo

É a fronteira mais natural para modularização inicial.

Razões:

- leitura predominante
- baixa complexidade transacional
- service dedicado
- mappers separados
- pouca dependência de sessão

Escopo provável:

- `Category`
- `Product`
- `Item`
- `CatalogService`
- `CategoryMapper`
- `ProductMapper`
- `ItemMapper`

## 2. Conta/Perfil

Pode formar um módulo de identidade/perfil.

Escopo provável:

- `Account`
- `AccountService`
- `AccountMapper`
- telas de cadastro/edição/autenticação

Principal obstáculo:

- acoplamento com sessão
- dependência de catálogo para `myList`

## 3. Pedido/Checkout

É uma boa fronteira funcional, mas tecnicamente mais difícil.

Escopo provável:

- `Order`
- `LineItem`
- `OrderService`
- `OrderMapper`
- `LineItemMapper`
- `SequenceMapper`

Principais dificuldades:

- dependência do carrinho em sessão
- dependência da conta em sessão
- wizard controlado pela action web

## 4. Carrinho

Hoje ainda não é um módulo maduro, mas é um candidato importante para modularização interna.

Escopo provável:

- `Cart`
- `CartItem`
- `CartActionBean`

Para virar módulo real, precisaria ser elevado para uma fronteira de aplicação.

## 5. Inventário / suporte operacional

O inventário aparece como subdomínio auxiliar:

- tabela `INVENTORY`
- `ItemMapper.getInventoryQuantity`
- `ItemMapper.updateInventoryQuantity`

Hoje ele está colado a catálogo e pedido, mas pode ser tratado como submódulo interno em uma refatoração posterior.

## Estratégia inicial recomendável para modularização

Uma decomposição incremental plausível, com base na estrutura atual, seria:

1. separar claramente `catalog`, `account`, `order`, `cart` em módulos internos do monólito
2. remover dependências entre actions via sessão
3. introduzir serviços de aplicação mais explícitos entre contextos
4. isolar persistência por contexto
5. só depois avaliar extrações externas ou APIs independentes

Para um TCC, essa narrativa é forte porque mostra modernização evolutiva, não uma reescrita total.

# 10. Evidências principais encontradas no código

## Bootstrap e infraestrutura

- [`pom.xml`](./pom.xml)
- [`src/main/webapp/WEB-INF/web.xml`](./src/main/webapp/WEB-INF/web.xml)
- [`src/main/webapp/WEB-INF/applicationContext.xml`](./src/main/webapp/WEB-INF/applicationContext.xml)
- [`src/main/webapp/index.html`](./src/main/webapp/index.html)

## Fluxo de login

- [`src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/AccountActionBean.java)
- [`src/main/java/org/mybatis/jpetstore/service/AccountService.java`](./src/main/java/org/mybatis/jpetstore/service/AccountService.java)
- [`src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml)
- [`src/main/webapp/WEB-INF/jsp/account/SignonForm.jsp`](./src/main/webapp/WEB-INF/jsp/account/SignonForm.jsp)

## Fluxo de catálogo

- [`src/main/java/org/mybatis/jpetstore/web/actions/CatalogActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CatalogActionBean.java)
- [`src/main/java/org/mybatis/jpetstore/service/CatalogService.java`](./src/main/java/org/mybatis/jpetstore/service/CatalogService.java)
- [`src/main/resources/org/mybatis/jpetstore/mapper/CategoryMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/CategoryMapper.xml)
- [`src/main/resources/org/mybatis/jpetstore/mapper/ProductMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/ProductMapper.xml)
- [`src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml)
- [`src/main/webapp/WEB-INF/jsp/catalog/Main.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Main.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/Category.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Category.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/Product.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Product.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/Item.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/Item.jsp)
- [`src/main/webapp/WEB-INF/jsp/catalog/SearchProducts.jsp`](./src/main/webapp/WEB-INF/jsp/catalog/SearchProducts.jsp)

## Fluxo de compra/checkout

- [`src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java)
- [`src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java`](./src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java)
- [`src/main/java/org/mybatis/jpetstore/service/OrderService.java`](./src/main/java/org/mybatis/jpetstore/service/OrderService.java)
- [`src/main/java/org/mybatis/jpetstore/domain/Cart.java`](./src/main/java/org/mybatis/jpetstore/domain/Cart.java)
- [`src/main/java/org/mybatis/jpetstore/domain/Order.java`](./src/main/java/org/mybatis/jpetstore/domain/Order.java)
- [`src/main/resources/org/mybatis/jpetstore/mapper/OrderMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/OrderMapper.xml)
- [`src/main/resources/org/mybatis/jpetstore/mapper/LineItemMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/LineItemMapper.xml)
- [`src/main/resources/org/mybatis/jpetstore/mapper/SequenceMapper.xml`](./src/main/resources/org/mybatis/jpetstore/mapper/SequenceMapper.xml)
- [`src/main/webapp/WEB-INF/jsp/cart/Cart.jsp`](./src/main/webapp/WEB-INF/jsp/cart/Cart.jsp)
- [`src/main/webapp/WEB-INF/jsp/order/NewOrderForm.jsp`](./src/main/webapp/WEB-INF/jsp/order/NewOrderForm.jsp)
- [`src/main/webapp/WEB-INF/jsp/order/ShippingForm.jsp`](./src/main/webapp/WEB-INF/jsp/order/ShippingForm.jsp)
- [`src/main/webapp/WEB-INF/jsp/order/ConfirmOrder.jsp`](./src/main/webapp/WEB-INF/jsp/order/ConfirmOrder.jsp)
- [`src/main/webapp/WEB-INF/jsp/order/ListOrders.jsp`](./src/main/webapp/WEB-INF/jsp/order/ListOrders.jsp)
- [`src/main/webapp/WEB-INF/jsp/order/ViewOrder.jsp`](./src/main/webapp/WEB-INF/jsp/order/ViewOrder.jsp)

## Banco e dados iniciais

- [`src/main/resources/database/jpetstore-hsqldb-schema.sql`](./src/main/resources/database/jpetstore-hsqldb-schema.sql)
- [`src/main/resources/database/jpetstore-hsqldb-dataload.sql`](./src/main/resources/database/jpetstore-hsqldb-dataload.sql)

## Testes relevantes

- [`src/test/java/org/mybatis/jpetstore/ScreenTransitionIT.java`](./src/test/java/org/mybatis/jpetstore/ScreenTransitionIT.java)
- [`src/test/java/org/mybatis/jpetstore/service/OrderServiceTest.java`](./src/test/java/org/mybatis/jpetstore/service/OrderServiceTest.java)
- [`src/test/java/org/mybatis/jpetstore/service/CatalogServiceTest.java`](./src/test/java/org/mybatis/jpetstore/service/CatalogServiceTest.java)
- [`src/test/java/org/mybatis/jpetstore/service/AccountServiceTest.java`](./src/test/java/org/mybatis/jpetstore/service/AccountServiceTest.java)
- [`src/test/java/org/mybatis/jpetstore/mapper/MapperTestContext.java`](./src/test/java/org/mybatis/jpetstore/mapper/MapperTestContext.java)

## Observação final de evidência operacional

Durante esta análise, a suíte `./mvnw -q test` executou com sucesso no repositório local. Isso confirma que o estado atual do projeto está consistente o suficiente para servir como base experimental de diagnóstico e proposta de modernização.
