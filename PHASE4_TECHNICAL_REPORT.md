# Phase 4 Technical Report: Architectural Hardening

Branch/context: hardening of module boundaries after Phase 3 package consolidation.

Phase 4 moved the modular monolith from "packages express intent" toward "code and tests enforce intent." The main architectural goal was not another large package migration. Instead, this phase removed remaining semantic leaks, protected boundaries with executable tests, and made inventory consistency a module-owned rule during order submission.

The codebase remains a single Spring, MyBatis, Stripes, JSP, HSQLDB, WAR-style application. The architectural evolution is internal: modules now expose fewer framework details and fewer domain objects across boundaries, while cross-module behavior is routed through explicit APIs.

## 1. Summary of Changes

Phase 4 implemented five hardening steps:

- Removed Stripes validation annotations from the `account.domain.Account` domain object.
- Recreated equivalent validation metadata on `account.web.AccountActionBean`, where Stripes request/form concerns belong.
- Removed inventory quantity from catalog item mapping and catalog item snapshots.
- Changed catalog item display composition so item details come from Catalog and stock state comes from Inventory.
- Confirmed transitional direct order/cart/account coupling methods are absent and added regression tests against their return.
- Added architecture boundary tests that enforce module dependency rules and framework-neutral domain packages.
- Hardened inventory decrement behavior so insufficient stock is detected inside Inventory.
- Added a module-level inventory exception and translated it in the order web flow to the existing error page behavior.
- Added transactional coverage proving inventory decrement rolls back if order persistence fails after stock has been decremented.

Architecturally, Phase 4 reduced three important kinds of coupling:

- Framework coupling: domain objects no longer carry Stripes validation metadata.
- Semantic coupling: catalog item data no longer carries inventory quantity.
- Module coupling: order creation and persistence use account/cart/catalog/inventory APIs instead of concrete web, persistence, or foreign domain types.

## 2. Files Modified and Their Roles

### Account Module

`src/main/java/org/mybatis/jpetstore/account/domain/Account.java`

- Removed Stripes validation annotations from the domain model.
- Keeps `Account` as a serializable account data/domain object with no web framework dependency.
- Supports the Phase 4 rule that domain packages must remain framework-neutral.

`src/main/java/org/mybatis/jpetstore/account/web/AccountActionBean.java`

- Owns Stripes validation metadata for account-related web actions.
- Adds validation on web bean setter methods for `username` and `password`.
- Adds nested validation metadata for `account.firstName` and `account.lastName`.
- Keeps signon, new account, and edit account validation behavior at the web boundary.

`src/test/java/org/mybatis/jpetstore/account/web/AccountActionBeanTest.java`

- Verifies required validation metadata remains available on `AccountActionBean`.
- Verifies `Account` no longer exposes Stripes validation metadata.
- Protects the behavioral compatibility of signon/new account/edit account validation after moving annotations out of the domain object.

### Catalog Module

`src/main/java/org/mybatis/jpetstore/catalog/domain/Item.java`

- Removed catalog-owned inventory quantity state.
- Keeps the item model focused on item identity, product association, pricing, supplier/status, and descriptive attributes.

`src/main/resources/org/mybatis/jpetstore/catalog/persistence/ItemMapper.xml`

- Removed the inventory join from catalog item queries.
- Removed quantity mapping from item list and item detail queries.
- Keeps Catalog persistence focused on `ITEM` and `PRODUCT` data.

`src/main/java/org/mybatis/jpetstore/catalog/api/ItemSnapshot.java`

- Exposes catalog item data without inventory quantity.
- Prevents API consumers from treating stock quantity as part of the catalog item description.

`src/main/java/org/mybatis/jpetstore/catalog/application/CatalogService.java`

- Maps `Item` to `ItemSnapshot` without inventory quantity.
- Provides inventory status composition through `InventoryQueryService` when callers need stock state.
- Keeps `getItem` and `getItemSnapshot` catalog-focused while delegating stock reads to Inventory.

`src/main/java/org/mybatis/jpetstore/catalog/web/CatalogActionBean.java`

- Composes item detail pages from both catalog data and `InventoryStatus`.
- Keeps the view behavior compatible while making the source of stock state explicit.

`src/test/java/org/mybatis/jpetstore/catalog/application/CatalogServiceTest.java`

- Verifies `ItemSnapshot` no longer includes inventory quantity.
- Verifies stock status is obtained through `InventoryQueryService`.

`src/test/java/org/mybatis/jpetstore/catalog/persistence/ItemMapperTest.java`

- Protects catalog item mapper behavior after removing inventory quantity from catalog queries.

### Order Module

`src/main/java/org/mybatis/jpetstore/order/domain/Order.java`

- No longer exposes transitional methods that initialize orders from `Account` and `Cart` domain objects.
- Retains only order-owned state and line item collection behavior.

`src/main/java/org/mybatis/jpetstore/order/domain/LineItem.java`

- No longer exposes constructors or methods that accept `CartItem`.
- Uses `catalog.api.ItemSnapshot` for item read-model data required by order display and total calculation.

`src/main/java/org/mybatis/jpetstore/order/application/OrderFactory.java`

- Creates orders from `CustomerProfile` and `CartSnapshot`.
- Converts `CartLineSnapshot` to `LineItem`.
- Keeps account/cart domain types out of the order domain.

`src/main/java/org/mybatis/jpetstore/order/application/OrderService.java`

- Uses `InventoryReservationService.decrement(itemId, quantity)` during order insertion.
- Keeps inventory mutation behind the inventory API.
- Runs order insertion under `@Transactional`, covering sequence update, inventory decrement, order inserts, status insert, and line item inserts.

`src/main/java/org/mybatis/jpetstore/order/web/OrderActionBean.java`

- Catches `InsufficientInventoryException` during confirmed order submission.
- Translates the inventory application exception into the existing error flow.
- Avoids leaking persistence or database details into the web layer.

`src/test/java/org/mybatis/jpetstore/order/domain/OrderTest.java`

- Adds a reflection-based guard that `Order` and `LineItem` do not expose account or cart domain types.
- Protects against reintroducing removed transitional APIs.

`src/test/java/org/mybatis/jpetstore/order/application/OrderServiceTest.java`

- Verifies order insertion delegates stock decrement to `InventoryReservationService`.
- Keeps the order service dependent on inventory API behavior rather than inventory persistence.

`src/test/java/org/mybatis/jpetstore/order/application/OrderServiceTransactionTest.java`

- Verifies inventory changes roll back when order persistence fails after decrement.
- Exercises the Spring transaction boundary around order submission.

`src/test/java/org/mybatis/jpetstore/order/web/OrderActionBeanTest.java`

- Verifies insufficient inventory is translated to the existing error page/message flow.

### Inventory Module

`src/main/java/org/mybatis/jpetstore/inventory/api/InventoryReservationService.java`

- Continues to expose the single reservation operation used by order submission: `decrement(String itemId, int quantity)`.
- Defines the module boundary for inventory mutation.

`src/main/java/org/mybatis/jpetstore/inventory/api/InsufficientInventoryException.java`

- Introduced a module-level application exception for insufficient stock.
- Carries `itemId`, requested quantity, and available quantity.
- Allows Inventory to communicate business failure without exposing mapper behavior or SQL details.

`src/main/java/org/mybatis/jpetstore/inventory/application/InventoryService.java`

- Performs final stock validation inside the inventory module.
- Calls the mapper decrement and throws `InsufficientInventoryException` when no row is updated.
- Reads current quantity for exception context after a rejected decrement.

`src/main/java/org/mybatis/jpetstore/inventory/persistence/InventoryMapper.java`

- Changed `updateInventoryQuantity` to return the affected row count.
- Makes the application service able to distinguish successful decrement from rejected decrement.

`src/main/resources/org/mybatis/jpetstore/inventory/persistence/InventoryMapper.xml`

- Adds `AND QTY >= #{increment}` to the inventory update statement.
- Makes stock validation atomic at the database update level.

`src/test/java/org/mybatis/jpetstore/inventory/application/InventoryServiceTest.java`

- Verifies successful decrement delegates to the mapper.
- Verifies rejected decrement raises `InsufficientInventoryException` with item, requested, and available quantities.

`src/test/java/org/mybatis/jpetstore/inventory/persistence/InventoryMapperTest.java`

- Verifies successful decrement updates stock.
- Verifies insufficient stock does not decrement the row and returns zero affected rows.

### Shared and Architecture Tests

`src/test/java/org/mybatis/jpetstore/DomainFrameworkDependencyTest.java`

- Verifies domain packages do not import Stripes, servlet APIs, Spring, or MyBatis.
- Introduced during Phase 4.1 as a targeted guard for framework-neutral domain objects.

`src/test/java/org/mybatis/jpetstore/ArchitectureBoundaryTest.java`

- Adds module dependency rules to the normal Maven test lifecycle.
- Prevents forbidden dependencies such as:
  - Account depending on Cart, Order, or Inventory persistence.
  - Catalog depending on Cart, Order, Inventory persistence, or inventory mutation.
  - Cart depending on Order.
  - Order depending on Account web, Cart web, or Catalog persistence.
  - Shared depending on business modules.
  - Domain packages depending on web, Spring, or MyBatis frameworks.

## 3. Architectural Impact of Each Change

### Step 4.1: Domain Objects Became Framework-Neutral

Before this step, `Account` mixed domain/account state with Stripes validation metadata. That meant the Account module domain could not be understood or reused independently from the web framework. It also meant a change in the web validation model could require editing the domain object.

After this step, validation belongs to `AccountActionBean`, which is already a Stripes adapter. The domain object now only models account state. This sharpens the internal layering of the account module:

```text
account.web -> account.application -> account.domain / account.persistence
```

The domain package no longer points back upward to the web layer.

### Step 4.2: Catalog Stopped Owning Inventory Quantity

Before this step, `Item` carried quantity and catalog item queries joined inventory data. That made an item description double as stock state. It also meant callers could accidentally read or pass inventory state through Catalog APIs.

After this step, Catalog owns item/product information and Inventory owns availability/quantity. Where the UI needs both, composition happens explicitly:

```text
CatalogActionBean.viewItem()
  -> CatalogService.getItem(itemId)
  -> CatalogService.getInventoryStatus(itemId)
       -> InventoryQueryService
```

This is a meaningful architectural shift. The system did not remove the need to show stock, but it changed where stock comes from. Stock is now read through Inventory rather than piggybacked on Catalog persistence.

### Step 4.3: Order Domain Stayed Independent from Account and Cart Domain Models

Earlier transitional methods allowed `Order` and `LineItem` to be built directly from `Account`, `Cart`, and `CartItem`. Those methods were useful during migration, but after snapshot APIs existed they became an architectural escape hatch.

Phase 4 confirms and guards the desired shape:

```text
account.api.CustomerProfile
cart.api.CartSnapshot
cart.api.CartLineSnapshot
        |
        v
order.application.OrderFactory
        |
        v
order.domain.Order / LineItem
```

The order domain now consumes stable boundary snapshots, not mutable foreign domain objects. That keeps checkout composition in the order application layer and leaves account/cart internals owned by their modules.

### Step 4.4: Boundaries Became Executable

Phase 3 gave the project a modular package layout. Phase 4 made that layout testable.

The new architecture tests convert dependency rules into automated regression checks. This matters because Java package moves alone do not prevent future imports from crossing boundaries. The tests make architectural drift visible in the regular Maven lifecycle.

The rules are intentionally pragmatic. They are string/path-based rather than a full ArchUnit model, which keeps the dependency footprint stable while still blocking the most important forbidden references.

### Step 4.5: Inventory Owns Final Stock Consistency

Before this step, order submission decremented inventory but did not clearly express insufficient stock as an inventory business failure. It also depended on the update operation as a side effect without using the affected row count as a consistency signal.

After this step, final stock validation happens inside Inventory:

```text
OrderService.insertOrder(order)
  -> InventoryReservationService.decrement(itemId, quantity)
       -> InventoryService
            -> InventoryMapper.updateInventoryQuantity(...)
                 UPDATE inventory
                 SET qty = qty - requested
                 WHERE itemid = ?
                   AND qty >= requested
```

If the update affects zero rows, Inventory throws `InsufficientInventoryException`. The web layer catches that exception and uses the existing error flow. The order service remains transactional, so a later order persistence failure rolls back the earlier stock decrement.

## 4. Dependencies Removed or Introduced

### Removed or Reduced Dependencies

- Removed `net.sourceforge.stripes.validation.*` dependency from `account.domain.Account`.
- Removed web framework imports from domain package behavior by enforcing them in tests.
- Removed catalog item query dependency on the `INVENTORY` table join.
- Removed inventory quantity from `catalog.domain.Item`.
- Removed inventory quantity from `catalog.api.ItemSnapshot`.
- Removed the ability for order domain APIs to expose `account.domain.*` or `cart.domain.*` types.
- Removed direct order dependency on account web and cart web packages from the allowed architecture.
- Removed direct catalog persistence dependency from the allowed order architecture.

### Dependencies Kept Deliberately

- `account.web.AccountActionBean` still depends on Stripes validation annotations because it is the web adapter.
- `catalog.application.CatalogService` depends on `InventoryQueryService` for read-side composition. This is allowed because Catalog may compose item details with stock status for UI workflows, but it must not mutate inventory.
- `order.application.OrderService` depends on `InventoryReservationService` for stock decrement during order submission.
- `order.domain.LineItem` depends on `catalog.api.ItemSnapshot` for item read-model data used in display and total calculation.
- Spring, MyBatis, Stripes, JSP, and HSQLDB remain in place by design.

### Introduced Dependencies

- Introduced `inventory.api.InsufficientInventoryException` as a module-level business exception.
- Introduced affected-row semantics from `InventoryMapper.updateInventoryQuantity` to `InventoryService`.
- Introduced automated architecture tests as a test-time dependency on package and import structure, without introducing a new production library.

No new external runtime framework was introduced in this phase.

## 5. Before vs After System Structure

### Before Phase 4

The package layout already looked modular after Phase 3, but several older couplings were still possible or present:

```text
account.domain.Account
  -> Stripes validation annotations

catalog.persistence.ItemMapper
  -> ITEM + PRODUCT + INVENTORY data in item queries

catalog.domain.Item
  -> item description + inventory quantity

order.domain.Order / LineItem
  -> historically allowed construction from Account, Cart, CartItem

architecture rules
  -> documented in PLAN.md, not fully executable

order submission
  -> decremented inventory, but insufficient stock was not explicitly modeled
```

This structure made the modular package layout somewhat fragile. A developer could still treat inventory as catalog data, put web annotations back in domain objects, or reintroduce order-to-cart/order-to-account domain coupling without an immediate test failure.

### After Phase 4

Phase 4 produces a stricter modular monolith structure:

```text
account.web
  -> owns Stripes validation
  -> delegates to account.application

account.domain
  -> framework-neutral account state

catalog.application
  -> owns item/product/category reads
  -> composes stock reads through inventory.api.InventoryQueryService

catalog.domain.Item
  -> no quantity

catalog.api.ItemSnapshot
  -> catalog data only

inventory.application
  -> owns stock reads and final stock validation
  -> throws inventory.api.InsufficientInventoryException

order.application
  -> creates orders from account/cart snapshots
  -> reserves stock through inventory.api.InventoryReservationService
  -> persists order inside one transaction

order.web
  -> translates inventory business failure to existing error flow

architecture tests
  -> enforce module and domain dependency rules
```

The important shift is that cross-module interactions are now explicit:

- Account information crosses into checkout as `CustomerProfile`.
- Cart contents cross into checkout as `CartSnapshot`.
- Catalog item details cross into cart/order as `ItemSnapshot`.
- Inventory availability crosses as `InventoryStatus`.
- Inventory mutation crosses only through `InventoryReservationService`.

## 6. Design Decisions Made During Implementation

### Keep Validation at the Stripes Adapter

The validation annotations were moved to `AccountActionBean` instead of introducing a separate form DTO. That keeps the change small and behavior-compatible with the existing Stripes binding model. A form DTO could be cleaner long term, but it would be a broader web-layer refactor.

### Use Composition Instead of Recombining Catalog and Inventory

The item detail flow still shows item data and inventory status together, but composition happens in the web/application boundary instead of in the catalog mapper. This preserves existing UI behavior while keeping Catalog and Inventory semantics separate.

### Keep `decrement(String itemId, int quantity)` as the Reservation API

The plan allowed either `decrement(...)` or a bulk `decrementAll(...)` operation. The implementation kept the existing single-line decrement API because the current order flow already iterates line items and the service transaction covers the whole order submission.

This avoids introducing a larger request type before the domain actually needs it.

### Use Atomic SQL for Final Stock Validation

Inventory validation was implemented as a conditional update:

```sql
UPDATE INVENTORY
SET QTY = QTY - #{increment}
WHERE ITEMID = #{itemId}
  AND QTY >= #{increment}
```

This avoids a separate read-then-write race inside the application service. The affected row count becomes the consistency signal.

### Throw a Module-Level Runtime Exception

`InsufficientInventoryException` is in `inventory.api` so callers can handle an inventory business failure without depending on inventory persistence or implementation classes.

It is a runtime exception so Spring transaction rollback behavior remains straightforward and does not force checked exception plumbing through every caller.

### Add Lightweight Architecture Tests Instead of ArchUnit

The plan suggested ArchUnit as a preference, but the implementation used focused source-level tests. This keeps the project dependency set stable and still enforces the critical constraints. The rules can be migrated to ArchUnit later if richer bytecode-level dependency analysis becomes useful.

### Preserve Existing Web Error Flow

Insufficient inventory is translated to the existing `Error.jsp` path through `OrderActionBean`. This keeps user-facing flow compatible while allowing Inventory to express the failure explicitly.

## 7. Potential Trade-offs or Limitations

### Architecture Tests Are Source-Text Based

The boundary tests inspect Java source text and imports/references. They are effective for the current codebase and low-cost to maintain, but they are not as complete as bytecode analysis. They may miss indirect dependencies introduced through reflection, generated sources, or string-built class names.

### Catalog Still Performs Read-Side Composition with Inventory

`CatalogService` exposes `getInventoryStatus(itemId)` by delegating to `InventoryQueryService`. This is acceptable for the current UI-oriented modular monolith, but it means Catalog application code still participates in read-side stock composition.

A stricter future design could move that composition into a dedicated presentation/query facade, leaving Catalog completely unaware of Inventory.

### Inventory Reservation Is Per Line Item

Order submission decrements inventory one line at a time. The Spring transaction protects consistency across the whole order, and the conditional update protects each item from going negative. However, there is not yet a bulk reservation API that can validate all line items before applying any decrement.

That means a multi-line order may decrement earlier lines before failing on a later line, then rely on transaction rollback to undo earlier decrements. This is safe under the current transaction model, but a bulk reservation command could make the business operation clearer.

### Exception Message Is Technical Enough for Internal Flow, Not Final UX

`InsufficientInventoryException` carries precise item and quantity data. The current web translation sends the exception message to the existing error flow. That is behavior-compatible and useful for tests, but a production UX might want a friendlier, localized message and a cart recovery path.

### Domain Still Contains Some Read-Model API Data

`LineItem` uses `catalog.api.ItemSnapshot` to support item display and total calculation. This is an improvement over depending on catalog domain or cart domain types, but it still means the order domain stores a read-side snapshot from another module's API.

This is a deliberate modular monolith compromise. A stricter design could separate persisted order line state from display enrichment more aggressively.

### Database Is Still Shared

Inventory consistency improved without splitting schemas or services. The `INVENTORY` table remains in the same database as catalog and order tables. This matches the modernization plan and keeps deployment simple, but it means module boundaries are enforced in code/tests rather than by physical database ownership.

## Verification

Phase 4 was verified through the normal Maven test lifecycle after each step. The final full suite result was:

```bash
./mvnw test
```

Result: 133 tests passed.

