# 1. Visão geral da análise

Esta análise aplica o SIA 2 com foco exclusivo em domínio e responsabilidade de negócio. A leitura do repositório cobriu o código Java em `src/main/java`, os mapeamentos MyBatis em `src/main/resources/org/mybatis/jpetstore/mapper`, as JSPs em `src/main/webapp/WEB-INF/jsp` e o esquema relacional em `src/main/resources/database/jpetstore-hsqldb-schema.sql`.

O sistema resolve o problema de uma loja virtual de pets com cinco capacidades de negócio claramente identificáveis:

| Capacidade de negócio | Problema atendido | Bounded Context principal |
| --- | --- | --- |
| Navegação e consulta de produtos | Permitir descoberta de categorias, produtos e itens comercializados | `Catalog` |
| Identidade e perfil do cliente | Permitir cadastro, autenticação e manutenção de preferências | `Account` |
| Seleção temporária de compra | Manter a intenção de compra antes da conversão em pedido | `Cart` |
| Formalização da compra | Coletar dados de pagamento/entrega, confirmar e registrar pedidos | `Order` |
| Controle de disponibilidade | Informar saldo e reduzir estoque após a venda | `Inventory` |

A visão TO-BE sugerida pelo SIA 2 é a separação do monólito em cinco módulos candidatos correspondentes aos bounded contexts já definidos: `Catalog`, `Account`, `Cart`, `Order` e `Inventory`.

Essa decomposição não parte da estrutura em camadas nem do acoplamento técnico entre classes. Ela parte da pergunta: "qual responsabilidade de negócio esta parte do sistema atende?". Por isso, componentes tecnicamente próximos, mas conceitualmente distintos, são separados nesta análise mesmo quando hoje aparecem misturados na mesma classe, mapper ou fluxo web.

# 2. Mapeamento de domínios e responsabilidades

## 2.1 Bounded contexts e propósito de negócio

| Bounded Context | Responsabilidade de negócio | Funcionalidades observadas |
| --- | --- | --- |
| `Catalog` | Expor o catálogo comercial da loja | tela principal, navegação por categoria, visualização de produto, visualização de item, busca textual |
| `Account` | Gerir identidade, perfil e preferências do cliente | cadastro, login, logout, edição de conta, idioma, categoria favorita, preferências de lista/banner |
| `Cart` | Manter o estado temporário da compra | adicionar item, remover item, alterar quantidades, calcular subtotal |
| `Order` | Transformar o carrinho em pedido formal | montar pedido a partir da conta/carrinho, capturar cobrança e entrega, confirmar, persistir, consultar histórico |
| `Inventory` | Controlar disponibilidade operacional dos itens | informar estoque atual, exibir disponibilidade, decrementar saldo após pedido |

## 2.2 Mapeamento principal de componentes

O quadro abaixo atribui cada componente a um domínio principal. Quando a implementação atual extrapola esse domínio, o compartilhamento é tratado explicitamente na seção 3.

| Domínio principal | Actions / classes de aplicação | Serviços | Entidades / objetos de domínio | Mappers e persistência |
| --- | --- | --- | --- | --- |
| `Catalog` | `CatalogActionBean` | `CatalogService` | `Category`, `Product`, `Item` | `CategoryMapper`, `ProductMapper`, `ItemMapper`, `CategoryMapper.xml`, `ProductMapper.xml`, `ItemMapper.xml`, tabelas `CATEGORY`, `PRODUCT`, `ITEM` |
| `Account` | `AccountActionBean` | `AccountService` | `Account` | `AccountMapper`, `AccountMapper.xml`, tabelas `ACCOUNT`, `PROFILE`, `SIGNON`, `BANNERDATA` |
| `Cart` | `CartActionBean` | Não há serviço dedicado | `Cart`, `CartItem` | Não há mapper próprio; estado mantido em sessão |
| `Order` | `OrderActionBean` | `OrderService` | `Order`, `LineItem`, `Sequence` | `OrderMapper`, `LineItemMapper`, `SequenceMapper`, `OrderMapper.xml`, `LineItemMapper.xml`, `SequenceMapper.xml`, tabelas `ORDERS`, `ORDERSTATUS`, `LINEITEM`, `SEQUENCE` |
| `Inventory` | Não há action dedicada | Não há serviço dedicado | Não há entidade exclusiva; o estoque aparece como `Item.quantity` | Não há mapper exclusivo; o estoque aparece na tabela `INVENTORY` e em operações hoje concentradas em `ItemMapper.xml` |

## 2.3 Interpretação funcional por domínio

### Catalog

O contexto `Catalog` representa o domínio comercial de navegação e descoberta. Ele responde por:

- expor as categorias disponíveis
- listar produtos por categoria
- listar itens por produto
- mostrar detalhes de um item
- permitir busca textual por produto

Seu propósito de negócio é tornar o portfólio navegável e compreensível para o cliente.

### Account

O contexto `Account` representa identidade e relacionamento básico com o cliente. Ele responde por:

- cadastrar novas contas
- autenticar usuários
- encerrar sessão
- editar perfil e preferências
- manter idioma, categoria favorita e preferências de personalização

Seu propósito de negócio é identificar o cliente e persistir dados do perfil usados durante a experiência de compra.

### Cart

O contexto `Cart` representa a seleção temporária de compra. Ele responde por:

- adicionar itens ao carrinho
- remover itens do carrinho
- alterar quantidades
- calcular subtotal
- manter a seleção durante a navegação

Seu propósito de negócio é capturar intenção de compra antes da confirmação do pedido.

### Order

O contexto `Order` representa a formalização da compra. Ele responde por:

- criar um pedido a partir da conta autenticada e do carrinho
- capturar dados de pagamento e entrega
- confirmar o pedido
- persistir cabeçalho e linhas do pedido
- consultar pedidos anteriores
- visualizar um pedido específico

Seu propósito de negócio é converter um carrinho temporário em transação comercial registrada.

### Inventory

O contexto `Inventory` representa a disponibilidade operacional. Ele responde por:

- informar se um item está disponível
- mostrar quantidade atual em determinadas telas
- decrementar saldo após a submissão de um pedido

Seu propósito de negócio é garantir que a venda reflita a disponibilidade real do item. No estado atual, esse contexto existe funcionalmente, mas não aparece como módulo explícito no código.

# 3. Componentes centrais e compartilhamento

## 3.1 Componentes compartilhados entre domínios

| Componente | Domínio principal | Domínios que também o utilizam | Motivo do compartilhamento |
| --- | --- | --- | --- |
| `Item` | `Catalog` | `Cart`, `Order`, `Inventory` | o item funciona ao mesmo tempo como SKU comercial, referência de carrinho, linha de pedido enriquecida e portador da quantidade em estoque |
| `ItemMapper` / `ItemMapper.xml` | `Catalog` | `Inventory`, `Order` | o mesmo mapper consulta dados de item/produto, lê estoque e atualiza `INVENTORY` |
| `CatalogService` | `Catalog` | `Account`, `Cart` | a conta usa catálogo para personalização (`myList`) e o carrinho usa catálogo para recuperar itens e consultar disponibilidade |
| `OrderService` | `Order` | `Inventory` | ao inserir pedido, o serviço também reduz estoque |
| `Account` | `Account` | `Order` | dados da conta são usados para inicializar cobrança e entrega do pedido |
| `Product` | `Catalog` | `Account` | a conta armazena uma lista personalizada de produtos baseada na categoria favorita |

## 3.2 Compartilhamentos mais relevantes para a decomposição

### `Item`

`Item` é o principal elemento compartilhado do sistema. Do ponto de vista de domínio, isso ocorre porque o item comercial é referenciado durante toda a jornada de compra:

- no `Catalog`, ele representa a variante vendável do produto
- no `Cart`, ele é o objeto associado ao `CartItem`
- no `Order`, ele reaparece em `LineItem` para exibição posterior
- no `Inventory`, ele carrega o campo `quantity`, usado como saldo de estoque

Conceitualmente, o TO-BE mais limpo seria separar:

- uma representação de catálogo do item vendável
- uma representação de estoque focada em disponibilidade
- um snapshot de item no pedido, imutável após a compra

### `ItemMapper`

O mapper de item também está semanticamente sobrecarregado. Ele:

- monta item + produto para navegação de catálogo
- lê quantidade em `INVENTORY`
- atualiza `INVENTORY`

Isso indica duas responsabilidades distintas dentro do mesmo componente:

- consulta comercial do catálogo
- persistência operacional de estoque

Uma separação futura conceitual seria:

- `CatalogItemRepository` para leitura de catálogo
- `InventoryRepository` para saldo e atualização de estoque

### Integração `Account` -> `Catalog`

O contexto de conta mantém preferência por categoria e lista personalizada de produtos (`myList`). O compartilhamento ocorre porque a experiência do usuário usa preferências do perfil para sugerir produtos do catálogo.

No TO-BE, isso pode permanecer como integração legítima, mas não como dependência interna da action de conta. A conta deveria expor preferência; o catálogo ou um serviço de recomendação deveria materializar a lista.

## 3.3 Componentes sem domínio próprio

Alguns elementos pertencem mais à infraestrutura do monólito do que a um bounded context específico:

- `AbstractActionBean`
- `applicationContext.xml`
- `web.xml`
- JSPs comuns como `IncludeTop.jsp`, `IncludeBottom.jsp` e `Error.jsp`

Eles não devem ser tratados como parte de um módulo de negócio, embora hoje influenciem o desenho e o acoplamento entre os contextos.

# 4. Módulos candidatos (resultado do SIA)

## 4.1 Módulo candidato: Catalog

**Responsabilidades**

- expor categorias, produtos e itens comercializados
- permitir navegação e busca no portfólio
- apresentar dados comerciais do item ao cliente

**Componentes**

- Classes: `CatalogActionBean`
- Serviços: `CatalogService`
- Entidades de domínio: `Category`, `Product`, `Item`
- Mappers: `CategoryMapper`, `ProductMapper`, `ItemMapper`
- Persistência: `CategoryMapper.xml`, `ProductMapper.xml`, `ItemMapper.xml`, tabelas `CATEGORY`, `PRODUCT`, `ITEM`

**Limites**

Pertence ao módulo:

- classificação de produtos
- dados descritivos e comerciais do portfólio
- busca e navegação

Não pertence ao módulo:

- autenticação e perfil do cliente
- estado temporário do carrinho
- submissão de pedidos
- mutação de estoque

**Relações com outros módulos**

- fornece produtos e itens ao `Cart`
- é referenciado por `Account` para preferências de categoria e lista personalizada
- consulta disponibilidade que, conceitualmente, pertence a `Inventory`

## 4.2 Módulo candidato: Account

**Responsabilidades**

- cadastrar usuários
- autenticar e encerrar sessão
- manter dados cadastrais e preferências
- representar a identidade do cliente durante a compra

**Componentes**

- Classes: `AccountActionBean`
- Serviços: `AccountService`
- Entidades de domínio: `Account`
- Mappers: `AccountMapper`
- Persistência: `AccountMapper.xml`, tabelas `ACCOUNT`, `PROFILE`, `SIGNON`, `BANNERDATA`

**Limites**

Pertence ao módulo:

- identidade
- credenciais
- perfil cadastral
- preferências de idioma e personalização

Não pertence ao módulo:

- recuperação de produtos do catálogo
- consulta de pedidos
- montagem de carrinho
- controle de estoque

**Relações com outros módulos**

- fornece identidade e endereço base para `Order`
- referencia categorias do `Catalog` por meio da preferência do usuário
- hoje consulta diretamente `Catalog` para materializar `myList`, o que é um acoplamento da implementação AS-IS

## 4.3 Módulo candidato: Cart

**Responsabilidades**

- manter a seleção temporária de itens
- registrar quantidades escolhidas
- calcular subtotal
- representar a compra ainda não confirmada

**Componentes**

- Classes: `CartActionBean`
- Serviços: não há serviço dedicado no AS-IS
- Entidades de domínio: `Cart`, `CartItem`
- Mappers: não há
- Persistência: estado apenas em sessão HTTP

**Limites**

Pertence ao módulo:

- inclusão e remoção de itens
- atualização de quantidade
- subtotal
- persistência temporária em memória/sessão

Não pertence ao módulo:

- descoberta de produtos
- autenticação
- persistência de pedido
- baixa de estoque

**Relações com outros módulos**

- consome itens do `Catalog`
- consulta disponibilidade do `Inventory`
- fornece um snapshot de compra para `Order`

## 4.4 Módulo candidato: Order

**Responsabilidades**

- abrir o processo de checkout
- captar pagamento, cobrança e entrega
- confirmar e persistir o pedido
- manter histórico e consulta de pedidos

**Componentes**

- Classes: `OrderActionBean`
- Serviços: `OrderService`
- Entidades de domínio: `Order`, `LineItem`, `Sequence`
- Mappers: `OrderMapper`, `LineItemMapper`, `SequenceMapper`
- Persistência: `OrderMapper.xml`, `LineItemMapper.xml`, `SequenceMapper.xml`, tabelas `ORDERS`, `ORDERSTATUS`, `LINEITEM`, `SEQUENCE`

**Limites**

Pertence ao módulo:

- ciclo de vida do pedido
- dados de cobrança e entrega
- linhas de pedido
- geração de número do pedido
- histórico do cliente

Não pertence ao módulo:

- autenticação do cliente
- gerenciamento do carrinho em sessão
- propriedade do saldo de estoque
- descrição comercial de catálogo

**Relações com outros módulos**

- depende da identidade de `Account` para o comprador
- consome o estado temporário de `Cart`
- usa dados de item do `Catalog` para enriquecer visualização do pedido
- dispara atualização de estoque em `Inventory`

## 4.5 Módulo candidato: Inventory

**Responsabilidades**

- responder disponibilidade de itens
- manter saldo de estoque
- sofrer decremento após a venda

**Componentes**

- Classes: não há classe dedicada no AS-IS
- Serviços: não há serviço dedicado no AS-IS
- Entidades de domínio: não há entidade exclusiva; o saldo aparece em `Item.quantity`
- Mappers/persistência efetivamente relacionados: operações `getInventoryQuantity` e `updateInventoryQuantity` de `ItemMapper`, `ItemMapper.xml`, tabela `INVENTORY`
- Pontos de uso atuais: `CatalogService.isItemInStock`, `OrderService.insertOrder`, `OrderService.getOrder`

**Limites**

Pertence ao módulo:

- quantidade disponível por item
- atualização do saldo
- política operacional de disponibilidade

Não pertence ao módulo:

- descrição de produto
- conteúdo do carrinho
- dados de cobrança e entrega
- autenticação do cliente

**Relações com outros módulos**

- atende `Catalog` para exibir disponibilidade
- atende `Cart` indiretamente durante a adição de item
- atende `Order` para baixa de estoque

**Observação**

`Inventory` é o bounded context menos explícito no código atual. Ele existe como responsabilidade de negócio, mas não existe como módulo claro na implementação AS-IS.

# 5. Avaliação da decomposição

## 5.1 Coesão dos domínios

| Domínio | Avaliação | Justificativa |
| --- | --- | --- |
| `Catalog` | Alta | possui propósito claro de navegação/comercialização e fluxo funcional bem reconhecível |
| `Account` | Média | identidade e perfil são claros, mas a action também materializa preferências dependentes de catálogo |
| `Cart` | Média | a responsabilidade é coesa, porém o contexto é muito dependente da camada web e não possui serviço próprio |
| `Order` | Média | o fluxo de pedido é claro, mas ele absorve inicialização a partir de conta/carrinho e também participa de estoque |
| `Inventory` | Baixa no AS-IS, alta no TO-BE | a responsabilidade existe, mas está espalhada entre mapper de item e serviços de catálogo/pedido |

## 5.2 Clareza das responsabilidades

A clareza funcional do sistema é boa do ponto de vista do usuário: é fácil reconhecer catálogo, conta, carrinho e pedido. O problema aparece na tradução disso para a implementação:

- o domínio `Inventory` não é modelado de forma explícita
- `Item` concentra significados de catálogo, carrinho, pedido e estoque
- a orquestração web usa sessão HTTP para costurar contextos que deveriam se relacionar por contratos de aplicação

## 5.3 Nível de independência entre módulos

Como proposta TO-BE, os cinco módulos fazem sentido e são comparáveis ao modelo de bounded contexts. Como implementação AS-IS, a independência ainda é limitada:

- `Account` depende de `Catalog` para materializar preferências
- `Cart` depende de `Catalog` para recuperar itens e disponibilidade
- `Order` depende de `Account` e `Cart` por estado de sessão, e de `Inventory` por efeito colateral de baixa
- `Inventory` não possui fronteira própria

Isso indica que a decomposição orientada a domínio é viável conceitualmente, mas a implementação atual ainda não reflete esses limites.

## 5.4 Aderência à separação de responsabilidades

A aderência é parcial.

Há boa aderência nos níveis mais visíveis do negócio:

- um fluxo de catálogo distinto
- um fluxo de conta distinto
- um fluxo de carrinho distinto
- um fluxo de pedido distinto

Há baixa aderência nos pontos onde o sistema cruza contexts:

- item/estoque
- conta/personalização de catálogo
- pedido/carrinho/conta
- session state como mecanismo de integração

# 6. Principais problemas arquiteturais identificados

## 6.1 Violação de fronteiras de domínio

1. `OrderActionBean` monta o checkout lendo diretamente `AccountActionBean` e `CartActionBean` da sessão, o que mistura `Order` com detalhes internos de `Account` e `Cart`.
2. `Order.initOrder(Account, Cart)` faz o agregado de pedido depender diretamente dos modelos de conta e carrinho, copiando dados de dois contexts distintos para dentro do domínio de pedido.
3. `AccountActionBean` consulta `CatalogService` para preencher `myList`, introduzindo conhecimento de catálogo dentro do fluxo de conta.

## 6.2 Entidades compartilhadas entre múltiplos domínios

1. `Item` é usado por `Catalog`, `Cart`, `Order` e `Inventory`.
2. `Product` aparece em `Account` por causa da lista personalizada.
3. `Account` é lido por `Order` como fonte de dados de cobrança e entrega.

O principal problema aqui é a sobrecarga semântica: um mesmo objeto representa conceitos distintos em momentos diferentes da jornada de negócio.

## 6.3 Mistura de responsabilidades

1. `ItemMapper` mistura leitura de catálogo e persistência de estoque.
2. `CatalogService` incorpora consulta de disponibilidade, que pertence conceitualmente a `Inventory`.
3. `OrderService` reduz estoque ao inserir pedido, assumindo responsabilidade operacional que deveria estar atrás de uma fronteira de `Inventory`.
4. `Account` carrega anotações de validação Stripes, misturando domínio com framework web.

## 6.4 Problemas herdados da arquitetura atual (AS-IS)

1. `Inventory` não existe como módulo explícito; existe apenas como comportamento disperso.
2. O carrinho não possui serviço ou persistência de aplicação próprios; ele vive acoplado à sessão HTTP.
3. A UI e a conta repetem a taxonomia do catálogo com listas fixas de categorias, o que enfraquece a autoridade do contexto `Catalog`.
4. A autenticação e a sessão dependem de convenções implícitas de chave em sessão, inclusive com uso inconsistente entre `"/actions/Account.action"` e `"accountBean"`.
5. `Order` nasce com defaults operacionais fixos de cartão, courier, locale e status, o que simplifica o demo, mas mistura dados de exemplo com regra de domínio.

# 7. Observações finais

A decomposição orientada a domínio revela cinco módulos candidatos coerentes com a linguagem do negócio: `Catalog`, `Account`, `Cart`, `Order` e `Inventory`. Portanto, o sistema já contém bounded contexts reconhecíveis, mesmo que a implementação ainda esteja organizada principalmente por camadas técnicas.

O ponto mais importante da análise é que o maior obstáculo para a modernização não é a ausência de domínios, e sim a forma como eles estão entrelaçados:

- `Inventory` está embutido em `ItemMapper`, `CatalogService` e `OrderService`
- `Item` atua como entidade compartilhada demais
- `Order` se apoia em estado web de `Account` e `Cart`
- `Account` incorpora dependências do `Catalog` para personalização

Como visão TO-BE, a modernização pode avançar tratando cada bounded context como módulo independente, com especial atenção para três separações conceituais prioritárias:

1. explicitar `Inventory` como módulo próprio
2. reduzir o compartilhamento semântico de `Item`
3. substituir integrações por sessão e acesso direto entre actions por contratos de aplicação entre contexts

Essa leitura torna o SIA 2 diretamente comparável ao SIA 1: enquanto o SIA 1 descreve como o sistema está acoplado estruturalmente, o SIA 2 mostra como ele deveria ser organizado segundo responsabilidades de negócio.
