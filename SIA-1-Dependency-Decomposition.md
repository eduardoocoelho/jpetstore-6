# 1. Visão geral da análise

Esta análise foi realizada exclusivamente a partir do código-fonte do repositório, com foco em dependências estruturais entre pacotes, classes e camadas. A inspeção cobriu as classes Java em `src/main/java`, os mapeamentos MyBatis em `src/main/resources/org/mybatis/jpetstore/mapper` e os arquivos de configuração `applicationContext.xml` e `web.xml`.

O sistema apresenta uma arquitetura monolítica em camadas, com quatro blocos principais:

| Camada / pacote | Quantidade | Papel estrutural |
| --- | ---: | --- |
| `org.mybatis.jpetstore.web.actions` | 5 classes | Controladores Stripes, fluxo web e estado de sessão |
| `org.mybatis.jpetstore.service` | 3 classes | Orquestração transacional e acesso à persistência |
| `org.mybatis.jpetstore.domain` | 9 classes | Objetos de dados compartilhados entre camadas |
| `org.mybatis.jpetstore.mapper` | 7 interfaces | Contratos de persistência MyBatis |
| `src/main/resources/.../mapper` | 7 XMLs | Implementação SQL dos mappers |

O `applicationContext.xml` confirma o desenho em camadas: serviços são descobertos por component scan, mappers por `mybatis:scan`, e os objetos de domínio são usados como `typeAliases` do MyBatis. Portanto, a direção estrutural principal é `web -> service -> mapper -> banco`, com `domain` atravessando todas as camadas.

# 2. Mapa de dependências

## 2.1 Pacotes principais

| Pacote | Componentes principais | Descrição |
| --- | --- | --- |
| `web.actions` | `AbstractActionBean`, `AccountActionBean`, `CatalogActionBean`, `CartActionBean`, `OrderActionBean` | Entrada HTTP, navegação Stripes, mensagens e estado de sessão |
| `service` | `AccountService`, `CatalogService`, `OrderService` | Coordena casos de uso e transações |
| `domain` | `Account`, `Category`, `Product`, `Item`, `Cart`, `CartItem`, `Order`, `LineItem`, `Sequence` | Modelo de dados compartilhado |
| `mapper` | `AccountMapper`, `CategoryMapper`, `ProductMapper`, `ItemMapper`, `OrderMapper`, `LineItemMapper`, `SequenceMapper` | Acesso a dados por entidade/aggregate técnico |

## 2.2 Dependências entre camadas

| Origem | Destino | Evidência estrutural |
| --- | --- | --- |
| `web.actions` | `service` | `@SpringBean` injeta `AccountService`, `CatalogService` e `OrderService` nas actions |
| `web.actions` | `domain` | Actions mantêm estado com `Account`, `Order`, `Cart`, `Item`, `Product`, `Category` |
| `web.actions` | `web.actions` | `OrderActionBean` lê `AccountActionBean` e `CartActionBean` da sessão; `AccountActionBean` redireciona para `CatalogActionBean` |
| `service` | `mapper` | Serviços recebem mappers por construtor |
| `service` | `domain` | Serviços consomem e retornam `Account`, `Order`, `Item`, `Category`, `Product`, `Sequence` |
| `mapper` | `domain` | Interfaces e XMLs mapeiam resultados para classes de domínio |
| `domain` | `domain` | Composição entre objetos: `Cart -> CartItem -> Item -> Product` e `Order -> LineItem -> Item -> Product` |
| `domain` | framework web | `Account` usa `@Validate` do Stripes, vazando dependência de framework para o modelo |

## 2.3 Direção predominante das dependências

1. A direção principal é descendente: `web.actions -> service -> mapper`.
2. Não há dependência de `service` para `web.actions`.
3. Não há dependência de `mapper` para `service`.
4. O pacote `domain` funciona como camada compartilhada e é referenciado por `web`, `service` e `mapper`.
5. As dependências cruzadas mais relevantes acontecem dentro da camada web, via sessão, e dentro do domínio, via composição de objetos.

## 2.4 Dependências entre classes principais

### Actions -> Services

| Action | Dependências diretas |
| --- | --- |
| `AccountActionBean` | `AccountService`, `CatalogService` |
| `CatalogActionBean` | `CatalogService` |
| `CartActionBean` | `CatalogService` |
| `OrderActionBean` | `OrderService` |

### Services -> Mappers

| Service | Dependências diretas |
| --- | --- |
| `AccountService` | `AccountMapper` |
| `CatalogService` | `CategoryMapper`, `ProductMapper`, `ItemMapper` |
| `OrderService` | `OrderMapper`, `LineItemMapper`, `SequenceMapper`, `ItemMapper` |

### Actions -> outras Actions

| Classe de origem | Classe de destino | Tipo de acoplamento |
| --- | --- | --- |
| `AccountActionBean` | `CatalogActionBean` | Redirecionamento explícito após cadastro, edição, login e logout |
| `OrderActionBean` | `AccountActionBean` | Leitura do bean de conta na sessão para autenticação e filtragem de pedidos |
| `OrderActionBean` | `CartActionBean` | Leitura do carrinho na sessão para checkout e limpeza após pedido |

O acoplamento entre actions não ocorre por injeção direta, mas por dependência do nome/registro do bean na sessão HTTP. Isso cria dependência temporal e implícita.

### Domain compartilhado entre camadas

| Classe de domínio | Camadas que a utilizam | Observação |
| --- | --- | --- |
| `Account` | web, service, mapper, domain | Objeto central do módulo de conta; contém anotação Stripes |
| `Product` | web, service, mapper, domain | Compartilhado entre catálogo e personalização da conta |
| `Item` | web, service, mapper, domain | Principal ponto de compartilhamento entre catálogo, carrinho e pedidos |
| `Order` | web, service, mapper, domain | Usado tanto como DTO persistente quanto como objeto de montagem de checkout |
| `Cart` e `CartItem` | web, domain | Estado em sessão; não possuem service ou mapper próprios |
| `Sequence` | service, mapper | Objeto técnico de geração de IDs |

## 2.5 Dependências cruzadas relevantes

1. `AccountActionBean` depende de `CatalogService` para montar `myList`, mesmo sendo uma action de conta/autenticação.
2. `OrderService` depende de `ItemMapper`, que também pertence ao fluxo de catálogo, para atualizar inventário e enriquecer itens do pedido.
3. `Order.initOrder(Account, Cart)` acopla diretamente o domínio de pedidos aos domínios de conta e carrinho.
4. `ItemMapper.xml` materializa `Item` com um `Product` embutido e também acessa `INVENTORY`, misturando consulta de catálogo com dado de estoque.

## 2.6 Ciclos de dependência

Não foram encontrados ciclos estáticos explícitos entre os pacotes principais.

Observações:

- `web.actions` depende de `service` e `domain`, mas `service` não depende de `web.actions`.
- `service` depende de `mapper`, mas `mapper` não depende de `service`.
- O pacote `domain` não depende de `service` nem de `mapper`, embora haja dependência de framework no caso de `Account`.
- Existe acoplamento indireto e runtime entre actions via sessão, mas isso não configura um ciclo estático de compilação.

# 3. Componentes centrais e acoplamento

## 3.1 Classes e pacotes mais acoplados

| Componente | Motivo do acoplamento |
| --- | --- |
| `CatalogService` | É usado por `AccountActionBean`, `CatalogActionBean` e `CartActionBean`; concentra acesso a `Category`, `Product` e `Item` |
| `OrderService` | Orquestra quatro mappers distintos e integra persistência de pedido, linha de pedido, sequência e inventário |
| `Item` | É compartilhado por catálogo, carrinho, pedido, serviços e mappers; é o principal hub do domínio |
| `ItemMapper` | É reutilizado por `CatalogService` e `OrderService`, conectando módulos diferentes ao mesmo contrato de persistência |
| `OrderActionBean` | Depende de `OrderService` e também de `AccountActionBean` e `CartActionBean` via sessão |
| `Order` | Atua como DTO persistente e também como objeto de montagem do checkout, referenciando `Account`, `Cart`, `CartItem` e `LineItem` |
| `AbstractActionBean` | É base obrigatória de todas as actions e centraliza contexto/mensagens web |

## 3.2 Hubs estruturais

Os principais hubs do sistema são:

1. `Item`, por ser compartilhado entre catálogo, carrinho, pedido e persistência.
2. `CatalogService`, por atender múltiplas actions e encapsular três mappers.
3. `OrderService`, por concentrar a orquestração mais ampla do sistema.
4. `OrderActionBean`, por coordenar o fluxo de checkout e depender de estado de outras actions.

## 3.3 Acoplamento indireto via sessão

O ponto mais sensível de acoplamento estrutural está na camada web:

1. `OrderActionBean` espera encontrar `AccountActionBean` em `"/actions/Account.action"` para `listOrders()` e `newOrderForm()`.
2. `OrderActionBean` espera encontrar `CartActionBean` em `"/actions/Cart.action"` para `newOrderForm()` e `newOrder()`.
3. `viewOrder()` usa a chave `"accountBean"`, enquanto `AccountActionBean` grava manualmente essa chave no login.
4. Esse desenho cria dependência em convenções de nomes de sessão, além de dependência temporal no fluxo de navegação.

# 4. Módulos candidatos (resultado do SIA)

## 4.1 Módulo candidato: Conta e Autenticação

**Componentes**

- `org.mybatis.jpetstore.web.actions.AccountActionBean`
- `org.mybatis.jpetstore.service.AccountService`
- `org.mybatis.jpetstore.mapper.AccountMapper`
- `src/main/resources/org/mybatis/jpetstore/mapper/AccountMapper.xml`
- `org.mybatis.jpetstore.domain.Account`

**Responsabilidades**

- Cadastro, edição e autenticação de usuários
- Persistência de conta, perfil e signon
- Manutenção do estado autenticado na sessão web

**Dependências internas**

- `AccountActionBean -> AccountService -> AccountMapper -> AccountMapper.xml`
- `AccountActionBean` mantém um `Account` como estado principal
- `AccountMapper` manipula de forma coesa as tabelas `ACCOUNT`, `PROFILE`, `SIGNON` e `BANNERDATA`

**Dependências externas**

- Depende de `CatalogService` e `Product` para preencher `myList` com base na categoria favorita
- Expõe estado para `OrderActionBean` via sessão HTTP
- Redireciona o fluxo para `CatalogActionBean`

**Leitura estrutural**

É um cluster relativamente coeso, mas já nasce acoplado ao catálogo e ao fluxo web.

## 4.2 Módulo candidato: Catálogo de Produtos

**Componentes**

- `org.mybatis.jpetstore.web.actions.CatalogActionBean`
- `org.mybatis.jpetstore.service.CatalogService`
- `org.mybatis.jpetstore.mapper.CategoryMapper`
- `org.mybatis.jpetstore.mapper.ProductMapper`
- `org.mybatis.jpetstore.mapper.ItemMapper`
- `src/main/resources/org/mybatis/jpetstore/mapper/CategoryMapper.xml`
- `src/main/resources/org/mybatis/jpetstore/mapper/ProductMapper.xml`
- `src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml`
- `org.mybatis.jpetstore.domain.Category`
- `org.mybatis.jpetstore.domain.Product`
- `org.mybatis.jpetstore.domain.Item`

**Responsabilidades**

- Navegação por categorias, produtos e itens
- Busca textual de produtos
- Consulta de disponibilidade de item

**Dependências internas**

- `CatalogActionBean -> CatalogService`
- `CatalogService -> CategoryMapper`, `ProductMapper`, `ItemMapper`
- `Item` contém `Product`; `ItemMapper.xml` monta esse relacionamento diretamente

**Dependências externas**

- É consumido por `AccountActionBean` para personalização da conta
- É consumido por `CartActionBean` para adicionar itens ao carrinho e consultar estoque
- É consumido por `OrderService` via `ItemMapper` para atualizar estoque e remontar pedidos

**Leitura estrutural**

É o cluster com maior reutilização lateral. Tem boa coesão interna, mas forte acoplamento externo por causa de `Item` e `ItemMapper`.

## 4.3 Módulo candidato: Carrinho de Compras

**Componentes**

- `org.mybatis.jpetstore.web.actions.CartActionBean`
- `org.mybatis.jpetstore.domain.Cart`
- `org.mybatis.jpetstore.domain.CartItem`

**Responsabilidades**

- Manutenção do carrinho em sessão
- Adição, remoção e atualização de quantidades
- Preparação do estado consumido no checkout

**Dependências internas**

- `CartActionBean` mantém `Cart` em memória de sessão
- `Cart` agrega `CartItem`
- `CartItem` encapsula `Item`, quantidade, estoque e total

**Dependências externas**

- Depende de `CatalogService` e `Item` para buscar item e estoque
- É lido por `OrderActionBean` via sessão
- É consumido por `Order.initOrder(Account, Cart)` durante a montagem do pedido

**Leitura estrutural**

É um cluster pequeno e relativamente coeso, porém fortemente dependente da camada web e sem autonomia de serviço/persistência.

## 4.4 Módulo candidato: Processamento de Pedidos

**Componentes**

- `org.mybatis.jpetstore.web.actions.OrderActionBean`
- `org.mybatis.jpetstore.service.OrderService`
- `org.mybatis.jpetstore.mapper.OrderMapper`
- `org.mybatis.jpetstore.mapper.LineItemMapper`
- `org.mybatis.jpetstore.mapper.SequenceMapper`
- `src/main/resources/org/mybatis/jpetstore/mapper/OrderMapper.xml`
- `src/main/resources/org/mybatis/jpetstore/mapper/LineItemMapper.xml`
- `src/main/resources/org/mybatis/jpetstore/mapper/SequenceMapper.xml`
- `org.mybatis.jpetstore.domain.Order`
- `org.mybatis.jpetstore.domain.LineItem`
- `org.mybatis.jpetstore.domain.Sequence`

**Responsabilidades**

- Orquestração de checkout
- Criação e persistência de pedidos
- Recuperação de pedidos por usuário
- Persistência de linhas de pedido e geração de identificador

**Dependências internas**

- `OrderActionBean -> OrderService`
- `OrderService -> OrderMapper`, `LineItemMapper`, `SequenceMapper`
- `Order` agrega `LineItem`
- `Sequence` suporta geração técnica de IDs

**Dependências externas**

- Depende de `ItemMapper` e `Item` para atualizar inventário e enriquecer `LineItem`
- Depende de `AccountActionBean` e `CartActionBean` via sessão
- Depende de `Account` e `Cart` através de `Order.initOrder(Account, Cart)`

**Leitura estrutural**

É um módulo funcionalmente identificável, mas com baixa independência estrutural. Ele cruza web, persistência, carrinho, conta e catálogo.

## 4.5 Cluster transversal: Infraestrutura Web e Integração

**Componentes**

- `org.mybatis.jpetstore.web.actions.AbstractActionBean`
- `src/main/webapp/WEB-INF/web.xml`
- `src/main/webapp/WEB-INF/applicationContext.xml`

**Responsabilidades**

- Inicialização do Stripes, Spring e MyBatis
- Base comum para todas as actions
- Registro de beans, transações e mappers

**Dependências internas**

- Todas as actions estendem `AbstractActionBean`
- O contêiner Spring registra serviços e mappers para todo o sistema

**Dependências externas**

- É transversal a todos os módulos candidatos

**Leitura estrutural**

É um cluster estrutural real do monólito, mas não é um bom candidato a serviço isolado.

# 5. Avaliação da decomposição

## 5.1 Coesão dos módulos

| Módulo | Coesão | Avaliação |
| --- | --- | --- |
| Conta e Autenticação | Média/alta | Fluxo principal bem delimitado, mas contaminado por personalização de catálogo |
| Catálogo de Produtos | Alta | Responsabilidades de consulta estão bem concentradas |
| Carrinho de Compras | Média | Coeso internamente, porém limitado à sessão e sem camada própria de serviço |
| Processamento de Pedidos | Média/baixa | Agrupa funções relacionadas a pedido, mas depende fortemente de catálogo, conta e carrinho |
| Infraestrutura Web e Integração | Técnica | Coesão técnica, não funcional |

## 5.2 Acoplamento entre módulos

O acoplamento entre módulos é de moderado para alto.

Principais causas:

1. `Item` e `ItemMapper` são compartilhados entre catálogo, carrinho e pedido.
2. `OrderActionBean` acessa diretamente outras actions via sessão.
3. `AccountActionBean` usa `CatalogService` para personalização.
4. `Order` conhece `Account` e `Cart` para montar o checkout.

## 5.3 Módulos mistos

Os seguintes módulos apresentam mistura de responsabilidades:

1. **Conta e Autenticação**
   Inclui autenticação/conta e também lógica de personalização de catálogo (`myList`).
2. **Processamento de Pedidos**
   Combina fluxo web, montagem de pedido, persistência de pedido e atualização de inventário.
3. **Carrinho de Compras**
   Mistura estado de interface web com comportamento de domínio, sem separação de camada de aplicação.

# 6. Principais problemas arquiteturais identificados

1. Forte dependência da camada web por uso de `SessionScope` e compartilhamento de actions na sessão.
2. Acoplamento implícito entre `OrderActionBean`, `AccountActionBean` e `CartActionBean` por convenções de chave de sessão.
3. `OrderService` depende de `ItemMapper`, criando ponte direta entre pedido e catálogo/estoque.
4. `Order.initOrder(Account, Cart)` acopla o domínio de pedidos aos domínios de conta e carrinho.
5. `Item` é um hub estrutural excessivamente compartilhado entre módulos.
6. `Cart` e `CartItem` não possuem uma camada de serviço própria; a lógica fica concentrada na action.
7. O modelo `Account` contém anotação Stripes, introduzindo dependência de framework na camada de domínio.
8. `ItemMapper.xml` mistura dados de item, produto e inventário no mesmo contrato de persistência.
9. O módulo de pedidos é grande e com múltiplas responsabilidades técnicas.
10. A infraestrutura de autenticação e de perfil está acoplada ao fluxo de navegação do catálogo.

# 7. Observações finais

A decomposição baseada em dependências mostra que o sistema atual não está organizado por serviços autônomos, mas por um monólito em camadas com alguns agrupamentos funcionais identificáveis. Os candidatos mais claros são **Conta e Autenticação**, **Catálogo de Produtos**, **Carrinho de Compras** e **Processamento de Pedidos**.

Entre esses grupos, o **Catálogo** é o mais reutilizado e o **Processamento de Pedidos** é o mais acoplado. O principal fator de fragilidade estrutural não está apenas nas chamadas entre serviços e mappers, mas na combinação de três elementos: domínio compartilhado, acoplamento por sessão entre actions e reutilização transversal de `Item`/`ItemMapper`.

Como retrato AS-IS, a decomposição obtida é adequada para comparação futura com uma decomposição orientada a domínio. Ela evidencia que a fronteira real atual do sistema é determinada menos pelo negócio e mais pelos pontos de compartilhamento técnico e pelo fluxo web do monólito.
