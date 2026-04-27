# Modular Architecture Proposal

## 1. System Overview

The application is a legacy Java web monolith similar to JPetStore. It is currently organized by technical layers rather than business modules:

- `org.mybatis.jpetstore.web.actions`
  - Stripes action beans, HTTP flow, session state, navigation, and form handling.
- `org.mybatis.jpetstore.service`
  - Spring services used as application-level orchestration.
- `org.mybatis.jpetstore.mapper`
  - MyBatis mapper interfaces.
- `org.mybatis.jpetstore.domain`
  - Shared domain/data objects used by web, service, and persistence layers.
- `src/main/resources/org/mybatis/jpetstore/mapper`
  - MyBatis SQL mappings.
- `src/main/webapp/WEB-INF/jsp`
  - JSP views grouped by feature: `account`, `catalog`, `cart`, `order`, and `common`.

The current architectural direction is mostly:

```text
Web Actions -> Services -> MyBatis Mappers -> Database
                    |
                 Domain
```

The main business capabilities found in the codebase are:

- Customer account, authentication, and profile management
- Product catalog browsing and search
- Shopping cart management
- Order checkout, submission, and order history
- Inventory availability and stock decrement

The system already has useful separation between web, service, domain, and mapper layers, but it does not yet have clear business module boundaries. The package `domain` is shared by all features, and several services or actions cross business boundaries directly.

Important coupling problems found in the current implementation:

- `AccountActionBean` depends on `CatalogService` to load `myList` after login, account creation, and account edit.
- `CartActionBean` depends on `CatalogService` both to load an `Item` and to check stock.
- `OrderActionBean` reads `AccountActionBean` and `CartActionBean` directly from the HTTP session.
- `Order.initOrder(Account, Cart)` couples the order domain directly to account and cart domain objects.
- `LineItem` has a constructor that depends on `CartItem`.
- `OrderService` depends on `ItemMapper` to decrement inventory and enrich order line items.
- `ItemMapper` mixes catalog queries with inventory reads and updates.
- `Item` contains `quantity`, even though quantity belongs to inventory, not catalog.
- `Account` contains Stripes validation annotations, causing the domain model to depend on the web framework.
- JSPs in `common` directly read `sessionScope.accountBean`, exposing session internals to shared views.
- Categories are partially duplicated in code and views instead of being consistently obtained from the catalog.

The target architecture should remain a modular monolith:

- Same deployable application.
- Same database initially.
- Same Spring/MyBatis/Stripes stack initially.
- No microservices split.
- Incremental package and dependency refactoring.

## 2. Proposed Modules

The proposed modular decomposition is business-oriented, validated against current dependencies.

```text
jpetstore
├── account
├── catalog
├── inventory
├── cart
├── order
└── shared
```

### 2.1 Account Module

Responsible for customer identity, authentication, profile data, and customer preferences.

Current core classes:

- `AccountActionBean`
- `AccountService`
- `AccountMapper`
- `Account`
- `AccountMapper.xml`

Main data ownership:

- `ACCOUNT`
- `SIGNON`
- `PROFILE`
- `BANNERDATA`

### 2.2 Catalog Module

Responsible for product discovery: categories, products, items, product search, and catalog details.

Current core classes:

- `CatalogActionBean`
- `CatalogService`
- `CategoryMapper`
- `ProductMapper`
- Catalog part of `ItemMapper`
- `Category`
- `Product`
- Catalog part of `Item`

Main data ownership:

- `CATEGORY`
- `PRODUCT`
- `ITEM`
- `SUPPLIER`

### 2.3 Inventory Module

Responsible for stock availability and stock mutation.

This module is currently implicit. Its responsibility is hidden inside `CatalogService`, `ItemMapper`, `Item`, and `OrderService`.

Current code fragments that should move into this module:

- `CatalogService.isItemInStock(...)`
- `ItemMapper.getInventoryQuantity(...)`
- `ItemMapper.updateInventoryQuantity(...)`
- `Item.quantity`
- Inventory SQL in `ItemMapper.xml`
- `INVENTORY` table

Main data ownership:

- `INVENTORY`

### 2.4 Cart Module

Responsible for the temporary purchase selection stored in the user session.

Current core classes:

- `CartActionBean`
- `Cart`
- `CartItem`
- Cart JSPs

Main data ownership:

- No database tables.
- Owns session-scoped cart state and cart behavior.

### 2.5 Order Module

Responsible for checkout, order creation, order persistence, order history, and order viewing.

Current core classes:

- `OrderActionBean`
- `OrderService`
- `OrderMapper`
- `LineItemMapper`
- `SequenceMapper`
- `Order`
- `LineItem`
- `Sequence`
- Order JSPs

Main data ownership:

- `ORDERS`
- `ORDERSTATUS`
- `LINEITEM`
- `SEQUENCE`, at least initially for order number generation

### 2.6 Shared Module

Responsible only for technical infrastructure and cross-cutting code that is not a business capability.

Current core classes/files:

- `AbstractActionBean`
- Shared JSPs such as `IncludeTop.jsp`, `IncludeBottom.jsp`, and `Error.jsp`
- Spring/MyBatis/web configuration

This module must stay small. It should not become a dumping ground for shared business objects.

## 3. Module Specifications

### 3.1 Account

#### Name

`account`

#### Responsibility

Manage customer identity, authentication, account profile, addresses, language preference, favorite category, and display preferences.

#### Main Entities/Classes

Current:

- `Account`
- `AccountService`
- `AccountMapper`
- `AccountActionBean`
- `AccountMapper.xml`

Target package structure:

```text
org.mybatis.jpetstore.account
├── web
│   └── AccountActionBean
├── application
│   ├── AccountService
│   └── CurrentAccountService
├── domain
│   └── Account
├── persistence
│   ├── AccountMapper
│   └── AccountMapper.xml
└── api
    ├── CustomerProfile
    └── AccountQueryService
```

#### Key Operations/Services

- Register new account.
- Authenticate account by username and password.
- Sign out current user.
- Load account by username.
- Update account profile.
- Update password when explicitly provided.
- Expose authenticated customer identity to other modules through a small API.

#### Owns

- Customer identity.
- Credentials.
- Profile data.
- Address data.
- Language preference.
- Favorite category preference.
- Banner/list preferences.

#### Should Not Access Directly

- Catalog mappers or catalog persistence.
- Product lists for `myList`.
- Cart state.
- Order history.
- Inventory stock.
- Stripes session details outside the web adapter.

#### Required Boundary Improvements

- Remove the direct dependency from `AccountActionBean` to `CatalogService`.
- Keep `favouriteCategoryId` as an account preference, but let catalog or a presentation composition service resolve products for that category.
- Remove Stripes `@Validate` annotations from `Account` and move validation to action/request DTOs.
- Expose account data to order as a customer snapshot, not as the mutable `AccountActionBean`.

---

### 3.2 Catalog

#### Name

`catalog`

#### Responsibility

Expose the commercial product catalog: categories, products, sellable items, item details, and product search.

#### Main Entities/Classes

Current:

- `Category`
- `Product`
- `Item`
- `CatalogService`
- `CatalogActionBean`
- `CategoryMapper`
- `ProductMapper`
- Catalog query methods from `ItemMapper`
- `CategoryMapper.xml`
- `ProductMapper.xml`
- Catalog query SQL from `ItemMapper.xml`

Target package structure:

```text
org.mybatis.jpetstore.catalog
├── web
│   └── CatalogActionBean
├── application
│   └── CatalogService
├── domain
│   ├── Category
│   ├── Product
│   └── CatalogItem
├── persistence
│   ├── CategoryMapper
│   ├── ProductMapper
│   └── CatalogItemMapper
└── api
    ├── CatalogQueryService
    ├── ProductSummary
    └── ItemSnapshot
```

#### Key Operations/Services

- List categories.
- Get category by id.
- Get product by id.
- List products by category.
- Search products by keyword.
- List items by product.
- Get sellable item details.
- Provide item snapshots for cart and order display.

#### Owns

- Category data.
- Product data.
- Sellable item descriptive data.
- Product search logic.
- Product-to-item relationship.
- Supplier relationship as catalog supporting data.

#### Should Not Access Directly

- Account data or session state.
- Cart contents.
- Order persistence.
- Inventory mutation.
- `INVENTORY` table updates.
- Authentication state.

#### Required Boundary Improvements

- Split `ItemMapper` into catalog and inventory responsibilities.
- Remove stock mutation from catalog persistence.
- Remove `quantity` from catalog item concepts or treat it as an inventory view field returned by an inventory API.
- Centralize categories in catalog instead of hardcoding them in JSPs and `AccountActionBean`.

---

### 3.3 Inventory

#### Name

`inventory`

#### Responsibility

Manage item availability and stock changes.

#### Main Entities/Classes

Current implicit ownership:

- `ItemMapper.getInventoryQuantity(...)`
- `ItemMapper.updateInventoryQuantity(...)`
- Inventory SQL inside `ItemMapper.xml`
- `CatalogService.isItemInStock(...)`
- `Item.quantity`
- Inventory mutation inside `OrderService.insertOrder(...)`

Target package structure:

```text
org.mybatis.jpetstore.inventory
├── application
│   └── InventoryService
├── domain
│   └── StockLevel
├── persistence
│   ├── InventoryMapper
│   └── InventoryMapper.xml
└── api
    ├── InventoryQueryService
    └── InventoryReservationService
```

#### Key Operations/Services

- Get inventory quantity for an item.
- Check whether an item is in stock.
- Decrement stock after order submission.
- Validate that requested quantities can be fulfilled.
- In a later increment, reserve or release stock if checkout becomes more sophisticated.

#### Owns

- `INVENTORY` table.
- Stock quantity.
- Availability rules.
- Stock decrement logic.

#### Should Not Access Directly

- Product/category descriptive data.
- Account information.
- Cart session state.
- Order internals beyond an item id and quantity request.
- JSPs or web action beans.

#### Required Boundary Improvements

- Create `InventoryMapper` and move inventory SQL out of `ItemMapper.xml`.
- Create `InventoryService`.
- Replace `CatalogService.isItemInStock(...)` with `InventoryService.isInStock(...)`.
- Replace direct `ItemMapper.updateInventoryQuantity(...)` in `OrderService` with an inventory API call.
- Keep the transaction inside the monolith; no distributed transaction or service split is needed.

---

### 3.4 Cart

#### Name

`cart`

#### Responsibility

Manage the customer's temporary purchase selection before order submission.

#### Main Entities/Classes

Current:

- `Cart`
- `CartItem`
- `CartActionBean`
- `Cart.jsp`
- `Checkout.jsp`
- `IncludeMyList.jsp`

Target package structure:

```text
org.mybatis.jpetstore.cart
├── web
│   └── CartActionBean
├── application
│   └── CartService
├── domain
│   ├── Cart
│   └── CartItem
└── api
    ├── CartSnapshot
    └── CartLineSnapshot
```

#### Key Operations/Services

- Add item to cart.
- Remove item from cart.
- Increment item quantity.
- Update quantities.
- Remove items with quantity below one.
- Calculate subtotal.
- Expose cart snapshot for checkout.

#### Owns

- Session-scoped cart state.
- Cart item quantity.
- Cart subtotal calculation.
- In-cart item availability flag.

#### Should Not Access Directly

- MyBatis mappers.
- Order persistence.
- Account persistence.
- Inventory table.
- Catalog persistence.
- Other action beans from the HTTP session.

#### Required Boundary Improvements

- Introduce `CartService` to move cart application logic out of `CartActionBean`.
- Keep `Cart` as a domain object, but avoid storing full mutable catalog `Item` long-term.
- Prefer storing an `ItemSnapshot` with item id, product id, name, attributes, and list price.
- Use catalog API for item details.
- Use inventory API for stock availability.
- Expose a `CartSnapshot` to order instead of passing the mutable `Cart`.

---

### 3.5 Order

#### Name

`order`

#### Responsibility

Formalize a cart into an order, persist order data, decrement inventory through an inventory API, and expose order history.

#### Main Entities/Classes

Current:

- `Order`
- `LineItem`
- `Sequence`
- `OrderService`
- `OrderActionBean`
- `OrderMapper`
- `LineItemMapper`
- `SequenceMapper`
- `OrderMapper.xml`
- `LineItemMapper.xml`
- `SequenceMapper.xml`

Target package structure:

```text
org.mybatis.jpetstore.order
├── web
│   └── OrderActionBean
├── application
│   ├── OrderService
│   ├── CheckoutService
│   └── OrderFactory
├── domain
│   ├── Order
│   ├── LineItem
│   └── OrderStatus
├── persistence
│   ├── OrderMapper
│   ├── LineItemMapper
│   └── SequenceMapper
└── api
    ├── OrderQueryService
    ├── CheckoutCommand
    └── OrderSummary
```

#### Key Operations/Services

- Start checkout from customer and cart snapshots.
- Capture billing data.
- Capture shipping data.
- Confirm order.
- Insert order header.
- Insert order status.
- Insert line items.
- Generate order id.
- List orders by username.
- View a specific order.
- Request inventory decrement for purchased items.

#### Owns

- Order lifecycle.
- Order header.
- Order line items.
- Order status.
- Payment fields currently stored in order.
- Shipping and billing snapshot.
- Order id generation, initially through `Sequence`.

#### Should Not Access Directly

- `AccountActionBean`.
- `CartActionBean`.
- HTTP session as a source of business data.
- `ItemMapper`.
- `INVENTORY` table.
- Catalog persistence.
- Account persistence.

#### Required Boundary Improvements

- Replace `Order.initOrder(Account, Cart)` with `OrderFactory.create(CustomerProfile, CartSnapshot)`.
- Replace `LineItem(CartItem)` with `LineItem.from(CartLineSnapshot)` or equivalent factory logic.
- Replace `OrderService -> ItemMapper` dependency with:
  - `InventoryReservationService` for stock decrement.
  - `CatalogQueryService` only if order viewing still needs live catalog enrichment.
- Prefer storing enough order line snapshot data to avoid depending on live catalog data for historical orders.
- Move checkout orchestration out of `OrderActionBean` into `CheckoutService`.

---

### 3.6 Shared

#### Name

`shared`

#### Responsibility

Hold technical infrastructure and truly generic support code.

#### Main Entities/Classes

Current:

- `AbstractActionBean`
- Common JSP includes
- Spring/MyBatis/web configuration

Target package structure:

```text
org.mybatis.jpetstore.shared
├── web
│   ├── AbstractActionBean
│   └── SessionState
├── application
│   └── ApplicationException
└── persistence
    └── PersistenceConfig
```

#### Key Operations/Services

- Common web action behavior.
- Error forwarding.
- Session access wrappers.
- Cross-module technical configuration.

#### Owns

- Shared web infrastructure.
- Common error handling.
- Technical wiring.

#### Should Not Access Directly

- Business mappers.
- Business services.
- Business domain rules.
- Catalog, account, cart, order, or inventory data ownership.

## 4. Inter-Module Relationships

### 4.1 Allowed Dependencies

The target dependency direction should be explicit and stable.

```text
Web adapters
  -> Own module application services
  -> Shared web infrastructure

Cart
  -> Catalog API for item details
  -> Inventory API for availability

Order
  -> Account API for customer profile snapshot
  -> Cart API for cart snapshot
  -> Inventory API for stock decrement
  -> Catalog API only for read-only item display, if historical item snapshots are not yet stored

Account
  -> Shared only
  -> May store favorite catalog category id as data, but should not load product lists directly

Catalog
  -> Shared only
  -> May expose item snapshots to Cart and Order

Inventory
  -> Shared only
  -> May validate item ids, but should not own product descriptions

Shared
  -> No business module dependencies
```

A practical target dependency graph:

```text
account   -> shared
catalog   -> shared
inventory -> shared
cart      -> catalog.api, inventory.api, shared
order     -> account.api, cart.api, inventory.api, catalog.api, shared
web shell -> module web adapters, shared
```

### 4.2 Forbidden Dependencies

The following dependencies should be considered modularity violations in the target architecture:

- `order` must not depend on `cart.web` or `CartActionBean`.
- `order` must not depend on `account.web` or `AccountActionBean`.
- `order` must not depend on `catalog.persistence` or `ItemMapper`.
- `order` must not update `INVENTORY` directly.
- `cart` must not depend on `catalog.persistence` or `ItemMapper`.
- `cart` must not depend on `order`.
- `account` must not depend on `catalog.application` just to build personalized product lists.
- `catalog` must not mutate inventory.
- `inventory` must not return full catalog `Item` or `Product` objects.
- `domain` objects must not depend on Stripes, Spring, MyBatis, JSP, or servlet APIs.
- JSPs should not depend on concrete action bean session keys such as `accountBean` or `"/actions/Account.action"` for business decisions.
- Mappers from one module should not be injected into services from another module.

### 4.3 Current Problematic Coupling

The following current dependencies should be addressed incrementally:

| Current Coupling | Problem | Target Direction |
| --- | --- | --- |
| `AccountActionBean -> CatalogService` | Account login/edit loads product recommendations directly | Account stores preference; catalog/presentation resolves recommendations |
| `CartActionBean -> CatalogService.isItemInStock` | Inventory responsibility hidden inside catalog | Cart uses `InventoryQueryService` |
| `CartActionBean -> CatalogService.getItem` | Cart depends on full catalog service | Cart uses `CatalogQueryService` returning `ItemSnapshot` |
| `OrderActionBean -> AccountActionBean` via session | Checkout depends on another web controller | Order uses `CurrentAccountService` or `CustomerProfile` |
| `OrderActionBean -> CartActionBean` via session | Checkout depends on another web controller | Order uses `CartSnapshot` |
| `Order.initOrder(Account, Cart)` | Order domain depends on account and cart internals | `OrderFactory.create(CustomerProfile, CartSnapshot)` |
| `LineItem(CartItem)` | Order line depends on cart item class | Convert from `CartLineSnapshot` |
| `OrderService -> ItemMapper.updateInventoryQuantity` | Order mutates inventory persistence directly | Order calls `InventoryReservationService` |
| `OrderService -> ItemMapper.getItem` | Order reads catalog persistence directly | Use catalog API or stored line snapshot |
| `ItemMapper` contains inventory SQL | Catalog and inventory persistence are mixed | Split into `CatalogItemMapper` and `InventoryMapper` |
| `Item.quantity` | Catalog entity contains inventory state | Move quantity to `StockLevel` or availability DTO |
| `Account` uses `@Validate` | Domain depends on Stripes | Move validation to web form/action DTO |
| JSPs read `sessionScope.accountBean` | Views depend on concrete session internals | Expose view/session model through shared web layer |

## 5. Code Mapping

### 5.1 Java Classes

| Existing Package/Class | Proposed Module | Target Role |
| --- | --- | --- |
| `org.mybatis.jpetstore.web.actions.AbstractActionBean` | `shared` | Shared web base class |
| `org.mybatis.jpetstore.web.actions.AccountActionBean` | `account` | Account web adapter |
| `org.mybatis.jpetstore.web.actions.CatalogActionBean` | `catalog` | Catalog web adapter |
| `org.mybatis.jpetstore.web.actions.CartActionBean` | `cart` | Cart web adapter |
| `org.mybatis.jpetstore.web.actions.OrderActionBean` | `order` | Order web adapter |
| `org.mybatis.jpetstore.service.AccountService` | `account` | Account application service |
| `org.mybatis.jpetstore.service.CatalogService` | `catalog` | Catalog application service, after inventory logic is removed |
| `org.mybatis.jpetstore.service.OrderService` | `order` | Order application service, after inventory/catalog mapper dependencies are removed |
| `org.mybatis.jpetstore.domain.Account` | `account` | Account domain object |
| `org.mybatis.jpetstore.domain.Category` | `catalog` | Catalog category |
| `org.mybatis.jpetstore.domain.Product` | `catalog` | Catalog product |
| `org.mybatis.jpetstore.domain.Item` | `catalog` + `inventory` split | Catalog item; `quantity` moves to inventory |
| `org.mybatis.jpetstore.domain.Cart` | `cart` | Cart aggregate/session domain object |
| `org.mybatis.jpetstore.domain.CartItem` | `cart` | Cart line item |
| `org.mybatis.jpetstore.domain.Order` | `order` | Order aggregate |
| `org.mybatis.jpetstore.domain.LineItem` | `order` | Order line item |
| `org.mybatis.jpetstore.domain.Sequence` | `order` or `shared.persistence` | Order id sequence, initially order-owned |

### 5.2 Mapper Interfaces and XML

| Existing Mapper | Proposed Module | Target Role |
| --- | --- | --- |
| `AccountMapper` / `AccountMapper.xml` | `account` | Account/profile/credential persistence |
| `CategoryMapper` / `CategoryMapper.xml` | `catalog` | Category persistence |
| `ProductMapper` / `ProductMapper.xml` | `catalog` | Product persistence and product search |
| `ItemMapper.getItemListByProduct` | `catalog` | Catalog item listing |
| `ItemMapper.getItem` | `catalog` with inventory split | Catalog item detail; remove direct inventory ownership |
| `ItemMapper.getInventoryQuantity` | `inventory` | Move to `InventoryMapper` |
| `ItemMapper.updateInventoryQuantity` | `inventory` | Move to `InventoryMapper` |
| `OrderMapper` / `OrderMapper.xml` | `order` | Order header and status persistence |
| `LineItemMapper` / `LineItemMapper.xml` | `order` | Order line persistence |
| `SequenceMapper` / `SequenceMapper.xml` | `order` initially | Order id generation |

### 5.3 JSP Views

| Existing JSP Area | Proposed Module | Notes |
| --- | --- | --- |
| `WEB-INF/jsp/account/*` | `account` | Account forms and sign-on views |
| `WEB-INF/jsp/catalog/*` | `catalog` | Catalog browsing and search views |
| `WEB-INF/jsp/cart/Cart.jsp` | `cart` | Cart view |
| `WEB-INF/jsp/cart/Checkout.jsp` | `cart` or remove later | Current checkout placeholder; real checkout is order flow |
| `WEB-INF/jsp/cart/IncludeMyList.jsp` | `catalog` presentation or shared composition | Uses account preference and catalog product list |
| `WEB-INF/jsp/order/*` | `order` | Checkout, confirmation, history, order view |
| `WEB-INF/jsp/common/IncludeTop.jsp` | `shared` with catalog/account view models | Currently mixes navigation, search, and account state |
| `WEB-INF/jsp/common/IncludeBottom.jsp` | `shared` with account view model | Currently reads account banner state directly |
| `WEB-INF/jsp/common/Error.jsp` | `shared` | Shared error view |

### 5.4 Database Tables

| Table | Proposed Owner Module | Notes |
| --- | --- | --- |
| `ACCOUNT` | `account` | Customer profile core |
| `SIGNON` | `account` | Credentials |
| `PROFILE` | `account` | Language, favorite category, display preferences |
| `BANNERDATA` | `account` or `catalog presentation` | Tied to favorite category; can remain account-owned initially |
| `CATEGORY` | `catalog` | Product classification |
| `PRODUCT` | `catalog` | Product data |
| `ITEM` | `catalog` | Sellable SKU data |
| `SUPPLIER` | `catalog` | Catalog supporting data |
| `INVENTORY` | `inventory` | Stock quantity |
| `ORDERS` | `order` | Order header |
| `ORDERSTATUS` | `order` | Order status history |
| `LINEITEM` | `order` | Order lines |
| `SEQUENCE` | `order` initially | Used for order id generation |

## 6. Refactoring Plan

### Step 1: Establish Safety Before Moving Code

- Keep the application as a single WAR.
- Keep the same database and framework stack.
- Run and preserve existing unit/integration tests.
- Add characterization tests around current cross-module behavior:
  - Login loads account and preferences.
  - Cart add/remove/update behavior.
  - Checkout creates order and clears cart.
  - Order submission decrements inventory.
  - Order history only shows the current user's orders.
- Do not start with package moves. First introduce boundaries while classes still live in their current packages.

### Step 2: Introduce Module APIs Without Changing Behavior

Create small interfaces that describe intended module boundaries:

```text
account.api.AccountQueryService
catalog.api.CatalogQueryService
inventory.api.InventoryQueryService
inventory.api.InventoryReservationService
cart.api.CartSnapshot
order.api.CheckoutCommand
```

Initial implementations may delegate to existing services.

Example target API responsibilities:

- `CatalogQueryService`
  - `getProductListByCategory(categoryId)`
  - `getItemSnapshot(itemId)`
- `InventoryQueryService`
  - `isInStock(itemId)`
  - `getQuantity(itemId)`
- `InventoryReservationService`
  - `decrement(itemId, quantity)`
- `AccountQueryService`
  - `getCustomerProfile(username)`

This step creates explicit contracts before physical reorganization.

### Step 3: Extract Inventory From Catalog and ItemMapper

This is the highest-value first refactoring because `ItemMapper` is the main persistence boundary violation.

Actions:

- Create `InventoryMapper`.
- Create `InventoryMapper.xml`.
- Move these operations from `ItemMapper` to `InventoryMapper`:
  - `getInventoryQuantity`
  - `updateInventoryQuantity`
- Create `InventoryService`.
- Move `CatalogService.isItemInStock(...)` to `InventoryService.isInStock(...)`.
- Update `CartActionBean` to use `InventoryService` for stock checks.
- Update `OrderService` to use `InventoryService` for stock decrement.
- Keep database schema unchanged.

Expected result:

```text
Before:
CartActionBean -> CatalogService -> ItemMapper -> INVENTORY
OrderService   -> ItemMapper -> INVENTORY

After:
CartActionBean -> InventoryService -> InventoryMapper -> INVENTORY
OrderService   -> InventoryService -> InventoryMapper -> INVENTORY
```

### Step 4: Separate Catalog Item From Inventory Quantity

Actions:

- Keep `Item` temporarily for compatibility.
- Stop relying on `Item.quantity` in new code.
- Introduce `StockLevel` or `InventoryStatus` in the inventory module.
- Introduce `ItemSnapshot` in the catalog API.
- Make catalog item queries return catalog data only.
- If a screen needs both catalog details and stock status, compose them in the application/web layer through catalog API plus inventory API.

Target rule:

```text
Catalog owns what the item is.
Inventory owns how many units are available.
```

### Step 5: Introduce CartService

Move application logic out of `CartActionBean`.

Actions:

- Create `CartService`.
- Move add-item logic into `CartService.addItem(cart, itemId)`.
- `CartService` should use:
  - `CatalogQueryService` for item details.
  - `InventoryQueryService` for availability.
- Keep `Cart` session-scoped for now.
- Keep `CartActionBean` as a thin Stripes adapter.

Expected result:

```text
Before:
CartActionBean -> CatalogService -> ItemMapper

After:
CartActionBean -> CartService
CartService -> CatalogQueryService
CartService -> InventoryQueryService
```

### Step 6: Remove Action-to-Action Session Coupling

Current `OrderActionBean` reads these session attributes directly:

```text
"/actions/Account.action"
"/actions/Cart.action"
"accountBean"
```

Actions:

- Introduce a shared web/session component, for example `SessionState`.
- Let `SessionState` expose:
  - current customer profile or username
  - current cart snapshot
  - authentication status
- Update `OrderActionBean` to use `SessionState` instead of casting action beans.
- Keep existing session keys internally during the transition if needed.
- Hide Stripes-specific session naming behind `SessionState`.

Expected result:

```text
Before:
OrderActionBean -> AccountActionBean
OrderActionBean -> CartActionBean

After:
OrderActionBean -> SessionState -> CustomerProfile
OrderActionBean -> SessionState -> CartSnapshot
```

### Step 7: Move Order Creation Out of the Domain Entity

Current problem:

```text
Order.initOrder(Account, Cart)
LineItem(int lineNumber, CartItem cartItem)
```

These methods couple order to account and cart internals.

Actions:

- Create `OrderFactory` or `CheckoutService`.
- Introduce `CustomerProfile` from account API.
- Introduce `CartSnapshot` and `CartLineSnapshot` from cart API.
- Move order initialization logic out of `Order`.
- Move cart-to-line-item conversion out of `LineItem`.
- Keep `Order` focused on order state and order behavior.

Target shape:

```text
OrderFactory.create(CustomerProfile customer, CartSnapshot cart)
```

This keeps order creation explicit while avoiding direct dependency on `Account`, `Cart`, and `CartItem`.

### Step 8: Refactor OrderService Dependencies

Current problem:

```text
OrderService -> ItemMapper
```

Actions:

- Remove direct `ItemMapper` injection from `OrderService`.
- Use `InventoryReservationService` for stock decrement.
- For order viewing, choose one of two incremental options:
  - Short term: use `CatalogQueryService` to enrich line items for display.
  - Better target: store item name/description snapshot in order line data so historical orders do not depend on live catalog data.
- Keep `OrderMapper`, `LineItemMapper`, and `SequenceMapper` inside the order module.

Target dependency:

```text
OrderService -> OrderMapper
OrderService -> LineItemMapper
OrderService -> SequenceMapper
OrderService -> InventoryReservationService
OrderService -> CatalogQueryService only for read-only display if still needed
```

### Step 9: Remove Web Framework Dependencies From Domain

Current problem:

```text
Account -> net.sourceforge.stripes.validation.Validate
```

Actions:

- Remove `@Validate` annotations from `Account`.
- Move validation to:
  - `AccountActionBean`, or
  - account form/request DTOs.
- Keep domain classes framework-neutral.
- Repeat this rule for all future domain objects.

Target rule:

```text
domain -> Java only
web -> Stripes, servlet, JSP
application -> Spring transactions
persistence -> MyBatis
```

### Step 10: Reorganize Packages by Module

After the dependency boundaries are stable, move classes physically.

Suggested package migration order:

1. `inventory`
   - New module with few dependencies and high boundary value.
2. `catalog`
   - Move `Category`, `Product`, catalog part of `Item`, catalog service, and catalog mappers.
3. `cart`
   - Move `Cart`, `CartItem`, `CartService`, and cart action.
4. `account`
   - Move `Account`, account service, account mapper, and account action.
5. `order`
   - Move `Order`, `LineItem`, order service, order action, and order mappers.
6. `shared`
   - Move `AbstractActionBean`, session helper, and common technical support.

Target package example:

```text
org.mybatis.jpetstore.account.web.AccountActionBean
org.mybatis.jpetstore.account.application.AccountService
org.mybatis.jpetstore.account.domain.Account
org.mybatis.jpetstore.account.persistence.AccountMapper

org.mybatis.jpetstore.catalog.web.CatalogActionBean
org.mybatis.jpetstore.catalog.application.CatalogService
org.mybatis.jpetstore.catalog.domain.Product
org.mybatis.jpetstore.catalog.persistence.ProductMapper

org.mybatis.jpetstore.inventory.application.InventoryService
org.mybatis.jpetstore.inventory.domain.StockLevel
org.mybatis.jpetstore.inventory.persistence.InventoryMapper

org.mybatis.jpetstore.cart.web.CartActionBean
org.mybatis.jpetstore.cart.application.CartService
org.mybatis.jpetstore.cart.domain.Cart

org.mybatis.jpetstore.order.web.OrderActionBean
org.mybatis.jpetstore.order.application.OrderService
org.mybatis.jpetstore.order.domain.Order
org.mybatis.jpetstore.order.persistence.OrderMapper

org.mybatis.jpetstore.shared.web.AbstractActionBean
```

Configuration changes needed:

- Expand Spring component scanning from only `org.mybatis.jpetstore.service` to module application packages.
- Expand MyBatis mapper scanning from `org.mybatis.jpetstore.mapper` to module persistence packages.
- Adjust MyBatis XML namespaces after mapper package moves.
- Adjust type aliases when domain classes move.

### Step 11: Modularize JSP/View Composition Gradually

Actions:

- Keep JSP files in place initially to avoid unnecessary churn.
- Introduce view models for shared header/footer data.
- Stop reading concrete session action beans in JSPs.
- Replace direct `sessionScope.accountBean` usage with a stable session/view model.
- Move catalog navigation data to catalog service/query API.
- Remove hardcoded category lists from:
  - account action category list
  - common header
  - catalog main view

Target rule:

```text
JSPs may render view data.
JSPs should not know module internals or concrete action bean session keys.
```

### Step 12: Add Automated Architecture Checks

After package moves, add architecture tests to prevent regression.

Suggested rules:

```text
..account.. must not depend on ..order..
..account.. must not depend on ..cart..
..account.. must not depend on ..inventory.persistence..
..catalog.. must not depend on ..order..
..catalog.. must not depend on ..cart..
..catalog.. must not update inventory
..cart.. must not depend on ..order..
..order.. must not depend on ..account.web..
..order.. must not depend on ..cart.web..
..order.. must not depend on ..catalog.persistence..
..shared.. must not depend on any business module
..domain.. must not depend on Stripes, Spring, MyBatis, servlet, or JSP APIs
```

ArchUnit is a good fit for this in a Java modular monolith.

### Step 13: Keep the Database Shared but Make Ownership Explicit

Do not split the database.

Instead:

- Document table ownership per module.
- Allow only the owning module's mapper to write its tables.
- Allow cross-module reads only through application APIs, not direct mapper access.
- Keep referential ids such as `itemId`, `productId`, and `username` as integration identifiers.
- Avoid foreign module object graphs where possible.

Target rule:

```text
Shared database is acceptable.
Shared table ownership is not.
```

### Step 14: Final Target State

The practical final state is a modular monolith with clear internal boundaries:

```text
Single deployable application
Single database
Business-oriented packages
Explicit module APIs
No direct mapper access across modules
No action-to-action session coupling
Domain objects independent from web framework
Inventory separated from catalog
Order creation based on snapshots, not mutable foreign domain objects
```

This architecture improves cohesion, reduces coupling, and prepares the system for possible future extraction without requiring a rewrite or introducing microservices prematurely.
