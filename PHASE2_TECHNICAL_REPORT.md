# Phase 2 Technical Report: Decoupling

Branch: `phase2-decoupling`
Comparison base: `modernization`

## 1. Summary of Changes

Phase 2 continued the architectural evolution started in Phase 1 by reducing direct coupling between the web layer, domain objects, catalog behavior, inventory behavior, and order persistence. The implementation moved several responsibilities out of legacy classes and into narrower application services and module-facing contracts.

The main changes are:

- Inventory persistence was extracted from `ItemMapper` into a dedicated `InventoryMapper`.
- Inventory behavior was centralized in `InventoryService`, which now implements both inventory query and reservation contracts.
- Cart item addition was moved from `CartActionBean` into a new `CartService`.
- Order creation was moved from `Order.initOrder(Account, Cart)` into a new `OrderFactory` that consumes Phase 1 snapshot contracts.
- `OrderService` no longer mutates inventory directly and no longer reads catalog or inventory data through `ItemMapper`.
- `OrderActionBean` now uses `SessionState`, `CustomerProfile`, `CartSnapshot`, and `OrderFactory` instead of directly coupling checkout flow to Stripes session action beans and domain objects.
- Tests were added or updated to characterize the new service boundaries and mapper ownership.

Architecturally, Phase 2 changes the system from a service/web/domain structure with shared mapper access into a more modular monolith shape where catalog, inventory, cart, and order responsibilities are represented by explicit package-level APIs and application services.

## 2. Files Modified and Their Roles

### Cart

`src/main/java/org/mybatis/jpetstore/cart/application/CartService.java`

- New application service for cart mutation.
- Owns the `addItem(Cart, String)` workflow.
- Coordinates item details from `CatalogQueryService` and stock availability from `InventoryQueryService`.
- Keeps Stripes action code focused on request handling rather than business orchestration.

`src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java`

- Replaced direct use of `CatalogService` with `CartService`.
- Removed item lookup, stock lookup, and increment/add branching from the web action.
- Now delegates valid add-item requests to the cart application layer.

`src/test/java/org/mybatis/jpetstore/cart/application/CartServiceTest.java`

- New unit tests for the cart mutation service.
- Verifies that absent items load catalog details and stock state.
- Verifies that existing cart items are incremented without reloading catalog or inventory data.

`src/test/java/org/mybatis/jpetstore/web/actions/CartActionBeanTest.java`

- Updated web tests to verify delegation to `CartService`.
- Removed web-layer expectations around catalog and inventory behavior.

### Catalog

`src/main/java/org/mybatis/jpetstore/catalog/api/CatalogQueryService.java`

- Added `getProductsByCategory(String categoryId)`.
- Keeps product-summary reads available through the catalog API instead of forcing callers toward concrete service or persistence classes.

`src/main/java/org/mybatis/jpetstore/service/CatalogService.java`

- No longer implements `InventoryQueryService`.
- Continues to implement `CatalogQueryService`.
- Delegates legacy inventory convenience methods to `InventoryQueryService`.
- Provides `getProductsByCategory` and keeps `getProductSummariesByCategory` as a compatibility alias.

`src/test/java/org/mybatis/jpetstore/service/CatalogServiceTest.java`

- Updated API expectations so `CatalogService` is only asserted as a catalog query implementation.
- Added coverage for `getProductsByCategory`.
- Updated inventory-related tests to verify delegation to `InventoryQueryService`.

### Inventory

`src/main/java/org/mybatis/jpetstore/inventory/api/InventoryQueryService.java`

- Expanded the query contract with `getQuantity(String itemId)` and `isInStock(String itemId)`.
- Kept `isItemInStock(String itemId)` as a default compatibility method that delegates to `isInStock`.

`src/main/java/org/mybatis/jpetstore/inventory/application/InventoryService.java`

- New inventory application service.
- Implements `InventoryQueryService` and `InventoryReservationService`.
- Owns stock quantity lookup, stock availability calculation, inventory status mapping, and decrement behavior.

`src/main/java/org/mybatis/jpetstore/inventory/persistence/InventoryMapper.java`

- New MyBatis mapper dedicated to the `INVENTORY` table.
- Owns `getInventoryQuantity` and `updateInventoryQuantity`.

`src/main/resources/org/mybatis/jpetstore/inventory/persistence/InventoryMapper.xml`

- New SQL mapping file for inventory reads and updates.
- Moves inventory SQL out of `ItemMapper.xml`.

`src/main/java/org/mybatis/jpetstore/mapper/ItemMapper.java`

- Removed inventory quantity and update methods.
- Now remains focused on item catalog reads.

`src/main/resources/org/mybatis/jpetstore/mapper/ItemMapper.xml`

- Removed `INVENTORY` SQL statements.
- Now maps item retrieval only.

`src/test/java/org/mybatis/jpetstore/inventory/application/InventoryServiceTest.java`

- New unit tests for the inventory service contract.
- Verifies query, stock status, and reservation behavior.

`src/test/java/org/mybatis/jpetstore/inventory/persistence/InventoryMapperTest.java`

- New mapper integration tests for inventory SQL.
- Verifies quantity lookup and decrement update against the embedded database.

`src/test/java/org/mybatis/jpetstore/mapper/ItemMapperTest.java`

- Removed inventory mapper tests from the item mapper test class.
- Keeps `ItemMapperTest` aligned with item-only persistence responsibility.

### Order

`src/main/java/org/mybatis/jpetstore/order/application/OrderFactory.java`

- New application component responsible for constructing an `Order`.
- Consumes `CustomerProfile` and `CartSnapshot` rather than web action beans or mutable domain aggregates.
- Reconstructs the order and line items from stable module-facing snapshots.

`src/main/java/org/mybatis/jpetstore/domain/Order.java`

- Marked `initOrder(Account, Cart)` as deprecated.
- Keeps the method as a transitional compatibility path.
- Signals that new checkout code should use `OrderFactory#createOrder(CustomerProfile, CartSnapshot)`.

`src/main/java/org/mybatis/jpetstore/service/OrderService.java`

- No longer implements `InventoryReservationService`.
- Removed direct dependency on `ItemMapper`.
- Uses `InventoryReservationService` for stock decrement during order insertion.
- Uses `CatalogQueryService` and `InventoryQueryService` to rehydrate order line item details when reading orders.
- Retains order persistence and sequence management responsibilities.

`src/main/java/org/mybatis/jpetstore/web/actions/OrderActionBean.java`

- Uses `SessionState` for current username, customer profile, cart snapshot, and cart clearing.
- Uses `OrderFactory` to create checkout orders.
- Removes direct session extraction of `AccountActionBean` and `CartActionBean` from checkout flow.
- Keeps the Stripes action focused on navigation, validation flow, and interaction with application services.

`src/test/java/org/mybatis/jpetstore/order/application/OrderFactoryTest.java`

- New tests for order creation from snapshots.
- Verifies customer data, billing/shipping defaults, payment defaults, totals, line item numbering, prices, quantities, and product data.

`src/test/java/org/mybatis/jpetstore/service/OrderServiceTest.java`

- Updated to use catalog and inventory contracts instead of `ItemMapper`.
- Verifies inventory decrement through `InventoryReservationService`.
- Verifies order read rehydration through `CatalogQueryService` and `InventoryQueryService`.

`src/test/java/org/mybatis/jpetstore/domain/OrderTest.java`

- Minor update only.
- Existing domain behavior remains covered while new checkout creation moves elsewhere.

`src/test/java/org/mybatis/jpetstore/web/actions/OrderActionBeanTest.java`

- Updated checkout test setup to inject `OrderFactory`.
- Keeps action-level checkout behavior covered after the factory extraction.

## 3. Architectural Impact of Each Change

### Dedicated Inventory Module Boundary

Before Phase 2, inventory reads and writes were physically located inside `ItemMapper` and `ItemMapper.xml`. This made inventory look like a subfeature of catalog items even though stock availability and stock reservation are separate business capabilities.

After Phase 2, inventory has:

- An API surface: `InventoryQueryService`, `InventoryReservationService`, `InventoryStatus`.
- An application implementation: `InventoryService`.
- A persistence mapper: `InventoryMapper`.
- Its own SQL mapping file: `InventoryMapper.xml`.

This creates a clearer module boundary. Other parts of the system now talk to inventory through service contracts instead of reaching into item persistence.

### Catalog No Longer Owns Inventory

Before Phase 2, `CatalogService` implemented both `CatalogQueryService` and `InventoryQueryService`, and it used `ItemMapper` to answer stock questions.

After Phase 2, `CatalogService` implements only `CatalogQueryService`. It still exposes legacy inventory-style methods, but those methods delegate to `InventoryQueryService`. This is a transitional design: callers that still depend on the older `CatalogService` methods continue to work, while the architectural ownership moves to inventory.

The architectural direction is that catalog owns item and product information; inventory owns stock state.

### Cart Mutation Moved Out of the Web Layer

Before Phase 2, `CartActionBean.addItemToCart` contained business logic:

- Validate the item id.
- Check whether the item already exists in the cart.
- Increment quantity for existing items.
- Query stock availability.
- Query item details.
- Add a new cart item.

After Phase 2, the action validates request state and delegates cart mutation to `CartService`. The service coordinates catalog and inventory APIs.

This reduces web-layer business logic and makes cart behavior testable without Stripes infrastructure.

### Order Creation Moved Out of the Domain Entity

Before Phase 2, checkout used `Order.initOrder(Account, Cart)`. That meant the `Order` domain object knew how to initialize itself from account and cart domain objects, and the checkout flow was tied to mutable domain aggregates held by session-backed action beans.

After Phase 2, `OrderFactory` creates an order from `CustomerProfile` and `CartSnapshot`. These are stable contracts introduced in Phase 1. The `Order` entity no longer needs to be the main orchestration point for checkout creation, and the old method is deprecated.

This is an important decoupling step because order creation now depends on input contracts rather than concrete web/session state.

### Order Service No Longer Owns Inventory Reservation

Before Phase 2, `OrderService` implemented `InventoryReservationService` and decremented inventory directly by calling `ItemMapper.updateInventoryQuantity`.

After Phase 2, `OrderService` depends on `InventoryReservationService` and delegates decrement operations during `insertOrder`.

This separates order persistence from inventory mutation. `OrderService` still coordinates order insertion and inventory reservation in the same transaction, but inventory update mechanics now belong to the inventory module.

### Order Reads Use Catalog and Inventory Contracts

Before Phase 2, `OrderService.getOrder` used `ItemMapper` to retrieve item details and inventory quantity for each line item.

After Phase 2, it uses:

- `CatalogQueryService.getItemSnapshot` for item and product details.
- `InventoryQueryService.getQuantity` for inventory quantity.

This removes direct catalog/inventory persistence dependencies from the order application service and replaces them with module APIs.

### Web Session Access Encapsulated Further

Before Phase 2, `OrderActionBean` reached into `HttpSession` and cast stored Stripes action beans directly.

After Phase 2, it uses `SessionState` for:

- Authentication checks.
- Current username lookup.
- Customer profile extraction.
- Cart snapshot extraction.
- Cart clearing.

The web action is still part of the Stripes layer, but session key knowledge and conversion from session objects to contracts are centralized in `SessionState`.

## 4. Dependencies Removed or Introduced

### External Dependencies

No Maven or third-party dependencies were introduced or removed in Phase 2. `pom.xml` is unchanged in the `modernization...phase2-decoupling` diff.

### Internal Dependencies Removed

- `CartActionBean -> CatalogService` was removed for add-to-cart behavior.
- `CatalogService -> ItemMapper inventory SQL` was removed for inventory state.
- `ItemMapper -> INVENTORY table operations` was removed.
- `OrderService -> ItemMapper` was removed.
- `OrderService implements InventoryReservationService` was removed.
- `OrderService -> direct inventory update map construction` was removed.
- `OrderActionBean -> direct HttpSession action bean extraction` was reduced through `SessionState`.
- New checkout flow no longer depends on `Order.initOrder(Account, Cart)`.

### Internal Dependencies Introduced

- `CartActionBean -> CartService`
- `CartService -> CatalogQueryService`
- `CartService -> InventoryQueryService`
- `CatalogService -> InventoryQueryService` for transitional compatibility methods
- `InventoryService -> InventoryMapper`
- `OrderActionBean -> OrderFactory`
- `OrderActionBean -> SessionState`
- `OrderFactory -> CustomerProfile`
- `OrderFactory -> CartSnapshot`
- `OrderFactory -> CartLineSnapshot`
- `OrderFactory -> ItemSnapshot`
- `OrderService -> CatalogQueryService`
- `OrderService -> InventoryQueryService`
- `OrderService -> InventoryReservationService`
- `InventoryMapper.xml -> org.mybatis.jpetstore.inventory.persistence.InventoryMapper`

### Dependency Direction After Phase 2

The dominant dependency direction is now:

```text
web actions
  -> application services / factories
  -> module APIs
  -> persistence mappers
```

The system is not fully modularized yet, but several direct cross-module persistence dependencies were removed.

## 5. Before vs After System Structure

### Before Phase 2

```text
CartActionBean
  -> CatalogService
    -> ItemMapper
      -> ITEM / PRODUCT / INVENTORY SQL

OrderActionBean
  -> HttpSession
  -> AccountActionBean
  -> CartActionBean
  -> Order.initOrder(Account, Cart)
  -> OrderService

OrderService
  -> ItemMapper
    -> item reads
    -> inventory reads
    -> inventory decrements
  -> OrderMapper
  -> LineItemMapper
  -> SequenceMapper

CatalogService
  -> implements CatalogQueryService
  -> implements InventoryQueryService
  -> ItemMapper inventory methods
```

In the previous structure, catalog item persistence was also inventory persistence, order service was also an inventory reservation implementation, and the web layer contained cart and checkout orchestration.

### After Phase 2

```text
CartActionBean
  -> CartService
    -> CatalogQueryService
    -> InventoryQueryService

OrderActionBean
  -> SessionState
    -> CustomerProfile
    -> CartSnapshot
  -> OrderFactory
    -> Order
  -> OrderService

OrderService
  -> CatalogQueryService
  -> InventoryQueryService
  -> InventoryReservationService
  -> OrderMapper
  -> LineItemMapper
  -> SequenceMapper

CatalogService
  -> implements CatalogQueryService
  -> delegates inventory compatibility methods to InventoryQueryService
  -> CategoryMapper / ProductMapper / ItemMapper

InventoryService
  -> implements InventoryQueryService
  -> implements InventoryReservationService
  -> InventoryMapper
    -> INVENTORY SQL
```

The after structure makes inventory an explicit module, moves cart mutation to an application service, and makes order creation depend on snapshots rather than web/session-held mutable objects.

## 6. Design Decisions Made During Implementation

### Keep Compatibility While Moving Ownership

`InventoryQueryService.isItemInStock` remains as a default method, and `CatalogService` still has `isItemInStock` and `getInventoryStatus` methods delegating to inventory. This avoids a large breaking change while moving ownership to the inventory module.

The trade is temporary duplication in API naming, but it provides a low-risk migration path.

### Use Application Services for Orchestration

`CartService`, `InventoryService`, and `OrderFactory` were added under module-specific `application` packages. This reflects a decision to keep orchestration outside Stripes actions and outside persistence mappers.

The result is easier unit testing and clearer boundaries around business workflows.

### Use Phase 1 Snapshot Contracts for Checkout

`OrderFactory` accepts `CustomerProfile` and `CartSnapshot` rather than `Account`, `Cart`, `AccountActionBean`, or `CartActionBean`. This deliberately builds on the Phase 1 API boundary work.

That decision reduces coupling to mutable session-backed objects and makes checkout order creation portable across possible future UI or API adapters.

### Preserve Existing Domain and Database Model

The change does not redesign `Order`, `Item`, `Product`, `LineItem`, or the database schema. Instead, it creates cleaner ownership around the existing model.

This keeps Phase 2 focused on decoupling rather than data model redesign.

### Keep Transaction Coordination in `OrderService`

`OrderService.insertOrder` still coordinates inventory reservation and order persistence in one method. The difference is that inventory mutation is delegated to `InventoryReservationService`.

This keeps current transactional behavior intact while moving inventory mechanics out of order code.

### Reconstruct Domain Objects at Module Boundaries

`OrderService` and `OrderFactory` convert snapshot records back into `Item` and `Product` domain objects where legacy APIs still expect them.

This is a pragmatic compatibility decision. It allows the internal architecture to move toward contracts without forcing all existing JSP/action/domain code to change in one phase.

## 7. Potential Trade-offs or Limitations

### Transitional Duplication Remains

There are still compatibility methods such as `CatalogService.isItemInStock` and `InventoryQueryService.isItemInStock`. They are useful during migration, but they leave some ambiguity about the preferred API until old call sites are removed.

### `OrderFactory` Still Creates Legacy Domain Objects

Although `OrderFactory` consumes snapshots, it still constructs legacy `Order`, `LineItem`, `Item`, and `Product` objects. This is necessary for compatibility with existing persistence and JSP flows, but it means the order module is not fully isolated from shared domain classes.

### Manual Snapshot-to-Domain Mapping Exists in More Than One Place

`OrderFactory` and `OrderService` both contain mapping logic from `ItemSnapshot` / `ProductSummary` to `Item` / `Product`. This duplication is small, but it may become a maintenance issue if item/product fields evolve.

A later phase could extract a mapper or move read models further away from mutable domain objects.

### Inventory Decrement Semantics Are Unchanged

`InventoryMapper.updateInventoryQuantity` still performs `QTY = QTY - #{increment}` without an explicit availability check in the update statement. Phase 2 moved ownership of the operation but did not add concurrency protection, stock reservation validation, or failure handling for insufficient stock.

### `OrderService` Still Coordinates Cross-Module Workflow

`OrderService` no longer implements inventory reservation and no longer calls inventory persistence directly, but it still coordinates order insertion and inventory reservation. This is acceptable for the current modular monolith stage, but a stronger separation could introduce an explicit checkout application service or transaction script.

### Web Layer Still Depends on Stripes Session Shape

`OrderActionBean` now uses `SessionState`, but `SessionState` still knows about Stripes action bean session keys and casts session objects. The coupling has been centralized, not eliminated.

### No Build Dependency Change Means No Framework-Level Enforcement

Because this phase did not introduce module boundaries at the build level, architectural boundaries are currently enforced by package structure, service contracts, and tests rather than by Maven modules or compiler-level dependency restrictions.

## Test and Verification Notes

Phase 2 added and updated tests around the new boundaries:

- `CartServiceTest`
- `InventoryServiceTest`
- `InventoryMapperTest`
- `OrderFactoryTest`
- Updated `CatalogServiceTest`
- Updated `OrderServiceTest`
- Updated action tests for cart and order flows
- Updated mapper tests to reflect the inventory extraction

Attempted local verification with:

```sh
mvn test
```

The command could not run in the current environment because `mvn` is not installed or not available on `PATH`.

## Overall Architectural Evolution

Phase 2 changes the application from a legacy layered structure with shared service and mapper responsibilities into a clearer modular monolith direction:

- Catalog is responsible for product and item details.
- Inventory is responsible for stock state and stock mutation.
- Cart mutation is handled by a cart application service.
- Order creation is handled by an order application factory.
- Order persistence coordinates with inventory through contracts instead of concrete inventory SQL.
- Web actions are thinner and increasingly delegate state conversion and business workflows.

The system is not fully decoupled yet, but the major direction is established: boundaries are now represented by explicit APIs, application services perform orchestration, and persistence ownership is closer to the module that owns the data.
