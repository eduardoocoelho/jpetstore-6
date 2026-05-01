# Phase 3 Technical Report: Module Consolidation

Branch/context: physical package consolidation after Phase 2 decoupling.

Phase 3 moves the codebase from a mostly layered package structure into a modular monolith structure organized by business capability. The main architectural change is that inventory, catalog, cart, account, order, and shared infrastructure now have explicit package ownership instead of being grouped primarily by technical layer names such as `service`, `mapper`, `domain`, and `web.actions`.

## 1. Summary of Changes

Phase 3 implemented the physical consolidation of module boundaries that were prepared in earlier phases.

The main changes are:

- Inventory was consolidated under `org.mybatis.jpetstore.inventory`.
- Catalog was consolidated under `org.mybatis.jpetstore.catalog`.
- Cart was consolidated under `org.mybatis.jpetstore.cart`.
- Account was consolidated under `org.mybatis.jpetstore.account`.
- Order was consolidated under `org.mybatis.jpetstore.order`.
- Shared web/session infrastructure was moved under `org.mybatis.jpetstore.shared.web`.
- Legacy package scan entries for `service`, `mapper`, `domain`, and `web` were removed from runtime and test configuration.
- MyBatis mapper XML namespaces were aligned with the new `*.persistence` mapper interfaces.
- JSP Stripes action references were aligned with module-owned web action packages.
- Tests were moved or updated to use the new package layout and module-specific imports.

Architecturally, the codebase now reads as a modular monolith:

- `account` owns identity, profile, authentication, and account persistence.
- `catalog` owns categories, products, item description, and search.
- `inventory` owns stock state and inventory reservation.
- `cart` owns session cart behavior and cart snapshots.
- `order` owns checkout, order creation, order persistence, order history, and order viewing.
- `shared` owns technical web/session infrastructure only.

The result is not a set of independently built modules yet. It is a single deployable application whose package structure now expresses module ownership and dependency direction more clearly.

## 2. Files Modified and Their Roles

### Runtime Configuration

`src/main/webapp/WEB-INF/applicationContext.xml`

- Removed obsolete component scan entry for `org.mybatis.jpetstore.service`.
- Removed obsolete MyBatis mapper scan entry for `org.mybatis.jpetstore.mapper`.
- Removed obsolete type alias package entry for `org.mybatis.jpetstore.domain`.
- Kept scans only for module-owned application, persistence, and domain packages.
- This file now wires Spring and MyBatis through the modular package layout instead of the legacy layered layout.

`src/main/webapp/WEB-INF/web.xml`

- Removed the old Stripes action scan package `org.mybatis.jpetstore.web`.
- Kept action discovery scoped to module web packages:
  - `org.mybatis.jpetstore.account.web`
  - `org.mybatis.jpetstore.catalog.web`
  - `org.mybatis.jpetstore.cart.web`
  - `org.mybatis.jpetstore.order.web`

This makes action discovery match the physical module structure.

### Shared Web Infrastructure

`src/main/java/org/mybatis/jpetstore/shared/web/AbstractActionBean.java`

- Moved from the legacy web action package into `shared.web`.
- Remains the common Stripes base class for action beans.
- Provides shared action context handling and message support.

`src/main/java/org/mybatis/jpetstore/shared/web/SessionState.java`

- Centralizes access to session-backed account/cart state used by checkout.
- No longer imports account or cart web action classes directly.
- Reads session objects through shared interfaces rather than concrete module web classes.

`src/main/java/org/mybatis/jpetstore/shared/web/SessionAccount.java`

- Introduced as a small shared contract implemented by the account web adapter.
- Allows `SessionState` to ask for authentication state, current username, and customer profile without depending on `AccountActionBean`.

`src/main/java/org/mybatis/jpetstore/shared/web/SessionCart.java`

- Introduced as a small shared contract implemented by the cart web adapter.
- Allows `SessionState` to obtain a cart snapshot and clear cart state without depending on `CartActionBean`.

### Account Module

`src/main/java/org/mybatis/jpetstore/account/api/AccountQueryService.java`

- Account-facing query contract for account information.
- Keeps account read behavior behind an account API instead of forcing callers to know persistence details.

`src/main/java/org/mybatis/jpetstore/account/api/CustomerProfile.java`

- Snapshot-style account profile used by order creation.
- Prevents checkout from depending on the mutable `Account` domain object or the account web action.

`src/main/java/org/mybatis/jpetstore/account/application/AccountService.java`

- Moved to the account application package.
- Owns authentication, account lookup, account creation, and account update use cases.

`src/main/java/org/mybatis/jpetstore/account/domain/Account.java`

- Moved to the account domain package.
- Remains the account aggregate/data model for account persistence and form binding.

`src/main/java/org/mybatis/jpetstore/account/persistence/AccountMapper.java`

- Moved to account persistence.
- Owns account database mapping operations.

`src/main/resources/org/mybatis/jpetstore/account/persistence/AccountMapper.xml`

- Moved to the matching account persistence resource path.
- Namespace points to `org.mybatis.jpetstore.account.persistence.AccountMapper`.

`src/main/java/org/mybatis/jpetstore/account/web/AccountActionBean.java`

- Moved to account web.
- Imports shared `AbstractActionBean`.
- Implements `SessionAccount` so shared session infrastructure can access account session state without depending on account web implementation details.
- Uses catalog API for favorite-category product list composition instead of direct catalog persistence access.

### Catalog Module

`src/main/java/org/mybatis/jpetstore/catalog/api/CatalogQueryService.java`

- Catalog-facing API for item and product summary reads.
- Provides module-safe access for cart/account/order workflows that need catalog data.

`src/main/java/org/mybatis/jpetstore/catalog/api/ProductSummary.java`

- Lightweight product snapshot exposed through catalog API.
- Used where callers do not need the full catalog domain object.

`src/main/java/org/mybatis/jpetstore/catalog/api/ItemSnapshot.java`

- Lightweight item snapshot exposed through catalog API.
- Used by cart and order to avoid depending directly on catalog persistence/domain internals.

`src/main/java/org/mybatis/jpetstore/catalog/application/CatalogService.java`

- Moved to catalog application.
- Owns product discovery, item lookup, category lookup, and search use cases.
- Implements catalog-facing query behavior for other modules.

`src/main/java/org/mybatis/jpetstore/catalog/domain/Category.java`

- Moved to catalog domain.
- Represents catalog category data.

`src/main/java/org/mybatis/jpetstore/catalog/domain/Product.java`

- Moved to catalog domain.
- Represents catalog product data.

`src/main/java/org/mybatis/jpetstore/catalog/domain/Item.java`

- Moved to catalog domain.
- Represents item description and pricing data, separate from inventory stock ownership.

`src/main/java/org/mybatis/jpetstore/catalog/persistence/CategoryMapper.java`

- Moved to catalog persistence.
- Owns category query persistence.

`src/main/java/org/mybatis/jpetstore/catalog/persistence/ProductMapper.java`

- Moved to catalog persistence.
- Owns product query persistence.

`src/main/java/org/mybatis/jpetstore/catalog/persistence/ItemMapper.java`

- Moved to catalog persistence.
- Owns item description query persistence.

`src/main/resources/org/mybatis/jpetstore/catalog/persistence/CategoryMapper.xml`

- Namespace points to `org.mybatis.jpetstore.catalog.persistence.CategoryMapper`.

`src/main/resources/org/mybatis/jpetstore/catalog/persistence/ProductMapper.xml`

- Namespace points to `org.mybatis.jpetstore.catalog.persistence.ProductMapper`.

`src/main/resources/org/mybatis/jpetstore/catalog/persistence/ItemMapper.xml`

- Namespace points to `org.mybatis.jpetstore.catalog.persistence.ItemMapper`.

`src/main/java/org/mybatis/jpetstore/catalog/web/CatalogActionBean.java`

- Moved to catalog web.
- Imports shared `AbstractActionBean`.
- Continues to own web navigation for catalog browsing and search.

### Inventory Module

`src/main/java/org/mybatis/jpetstore/inventory/api/InventoryQueryService.java`

- Inventory-facing read contract.
- Used by cart and order workflows that need stock availability or quantity.

`src/main/java/org/mybatis/jpetstore/inventory/api/InventoryReservationService.java`

- Inventory-facing mutation contract for stock reservation/decrement behavior.
- Allows order placement to reserve stock through an API rather than through catalog/item persistence.

`src/main/java/org/mybatis/jpetstore/inventory/api/InventoryStatus.java`

- Inventory status value object/API type.
- Represents inventory state without exposing mapper details.

`src/main/java/org/mybatis/jpetstore/inventory/application/InventoryService.java`

- Moved to inventory application.
- Owns inventory query and reservation behavior.

`src/main/java/org/mybatis/jpetstore/inventory/persistence/InventoryMapper.java`

- Moved to inventory persistence.
- Owns database access for the `INVENTORY` table.

`src/main/resources/org/mybatis/jpetstore/inventory/persistence/InventoryMapper.xml`

- Moved to the matching inventory persistence resource path.
- Namespace points to `org.mybatis.jpetstore.inventory.persistence.InventoryMapper`.

### Cart Module

`src/main/java/org/mybatis/jpetstore/cart/api/CartQueryService.java`

- Cart-facing query/snapshot contract.
- Defines how a cart is exposed as a `CartSnapshot`.

`src/main/java/org/mybatis/jpetstore/cart/api/CartSnapshot.java`

- Immutable cart snapshot used by order creation.
- Allows order checkout to consume cart contents without depending on cart web/session internals.

`src/main/java/org/mybatis/jpetstore/cart/api/CartLineSnapshot.java`

- Immutable snapshot of a cart line.
- Carries item snapshot, quantity, stock status, and total for checkout/order creation.

`src/main/java/org/mybatis/jpetstore/cart/application/CartService.java`

- Moved to cart application.
- Owns add-item cart behavior.
- Depends on `catalog.api` and `inventory.api` instead of catalog/inventory internals.

`src/main/java/org/mybatis/jpetstore/cart/domain/Cart.java`

- Moved to cart domain.
- Owns in-session cart state and cart mutations.

`src/main/java/org/mybatis/jpetstore/cart/domain/CartItem.java`

- Moved to cart domain.
- Represents a line in the session cart.

`src/main/java/org/mybatis/jpetstore/cart/web/CartActionBean.java`

- Moved to cart web.
- Imports shared `AbstractActionBean`.
- Implements `SessionCart` so shared session infrastructure can access cart state without depending on `CartActionBean`.
- Exposes checkout-safe cart snapshots through cart API types.

### Order Module

`src/main/java/org/mybatis/jpetstore/order/api/CheckoutCommand.java`

- Order-facing command type for checkout intent.
- Keeps checkout input separate from web framework classes.

`src/main/java/org/mybatis/jpetstore/order/api/OrderQueryService.java`

- Order-facing query contract.
- Allows order history/viewing behavior to be exposed without leaking mapper details.

`src/main/java/org/mybatis/jpetstore/order/application/OrderFactory.java`

- Moved to order application.
- Creates `Order` instances from `CustomerProfile` and `CartSnapshot`.
- Keeps order construction out of web action and out of account/cart domain objects.

`src/main/java/org/mybatis/jpetstore/order/application/OrderService.java`

- Moved to order application.
- Owns order insertion, order lookup, order history, and inventory reservation coordination.
- Depends on inventory reservation/query APIs rather than direct inventory mapper or catalog item mapper behavior.

`src/main/java/org/mybatis/jpetstore/order/domain/Order.java`

- Moved to order domain.
- Represents an order aggregate/data model.
- No longer needs to initialize itself directly from account/cart web state.

`src/main/java/org/mybatis/jpetstore/order/domain/LineItem.java`

- Moved to order domain.
- Uses item snapshot data rather than a direct cart item dependency for order-line creation.

`src/main/java/org/mybatis/jpetstore/order/domain/Sequence.java`

- Moved to order domain.
- Represents persisted sequence state for order IDs.

`src/main/java/org/mybatis/jpetstore/order/persistence/OrderMapper.java`

- Moved to order persistence.
- Owns order header persistence and order query mapping.

`src/main/java/org/mybatis/jpetstore/order/persistence/LineItemMapper.java`

- Moved to order persistence.
- Owns line-item persistence.

`src/main/java/org/mybatis/jpetstore/order/persistence/SequenceMapper.java`

- Moved to order persistence.
- Owns sequence persistence.

`src/main/resources/org/mybatis/jpetstore/order/persistence/OrderMapper.xml`

- Namespace points to `org.mybatis.jpetstore.order.persistence.OrderMapper`.

`src/main/resources/org/mybatis/jpetstore/order/persistence/LineItemMapper.xml`

- Namespace points to `org.mybatis.jpetstore.order.persistence.LineItemMapper`.

`src/main/resources/org/mybatis/jpetstore/order/persistence/SequenceMapper.xml`

- Namespace points to `org.mybatis.jpetstore.order.persistence.SequenceMapper`.

`src/main/java/org/mybatis/jpetstore/order/web/OrderActionBean.java`

- Moved to order web.
- Imports shared `AbstractActionBean`.
- Reads account/cart session data through `SessionState`.
- Uses `CustomerProfile` and `CartSnapshot` rather than `AccountActionBean`, `CartActionBean`, `Account`, or `Cart` directly during checkout.

### JSP Files

`src/main/webapp/WEB-INF/jsp/**`

- JSP Stripes references were updated to module web action classes.
- Examples:
  - `org.mybatis.jpetstore.account.web.AccountActionBean`
  - `org.mybatis.jpetstore.catalog.web.CatalogActionBean`
  - `org.mybatis.jpetstore.cart.web.CartActionBean`
  - `org.mybatis.jpetstore.order.web.OrderActionBean`

The common JSP folder remains under `WEB-INF/jsp/common`, as planned. It is shared presentation infrastructure, not a Java package dependency boundary.

### Tests

`src/test/java/org/mybatis/jpetstore/shared/persistence/MapperTestContext.java`

- Moved from the old test package `org.mybatis.jpetstore.mapper`.
- Provides shared mapper test wiring for embedded HSQLDB, MyBatis, and transaction management.
- Scans only module persistence packages.
- Uses only module domain packages for type aliases.

`src/test/java/org/mybatis/jpetstore/**`

- Tests were moved or updated to match module packages.
- Mapper tests now import `org.mybatis.jpetstore.shared.persistence.MapperTestContext`.
- Web/action tests now use the module web packages.
- Service/domain tests now sit under their owning module packages.

## 3. Architectural Impact of Each Change

### Runtime Scanning Now Enforces Module Ownership

Before Phase 3, Spring, MyBatis, and Stripes still scanned old umbrella packages. This meant old package references could continue to work accidentally and hide incomplete modularization.

After Phase 3:

- Spring scans application services by module.
- MyBatis scans mapper interfaces by module persistence package.
- Stripes scans action beans by module web package.
- MyBatis type aliases resolve module domain packages.

This makes the physical package structure part of the executable architecture.

### Inventory Became an Explicit Module

Inventory was previously implicit because stock behavior was historically entangled with item/catalog behavior. After consolidation, inventory has its own API, application service, mapper, XML namespace, and tests.

Architectural impact:

- Catalog can describe items without owning stock.
- Cart can ask whether an item is in stock through `inventory.api`.
- Order can reserve stock through `inventory.api`.
- Inventory persistence is no longer hidden behind catalog mappers.

### Catalog Was Reduced to Product and Item Description Ownership

Catalog now owns browsing, search, categories, products, and item descriptive data. It no longer acts as a general product-plus-inventory service.

Architectural impact:

- Catalog is cohesive around product discovery.
- Other modules consume catalog data through `CatalogQueryService`, `ProductSummary`, and `ItemSnapshot`.
- Cart and order can use catalog data without depending on catalog mapper interfaces.

### Cart Became a Session-Backed Business Module

Cart now owns its domain model, application service, web action, and API snapshots.

Architectural impact:

- Cart mutation logic is separate from web request handling.
- Cart item addition coordinates with catalog and inventory through APIs.
- Checkout receives immutable cart snapshots instead of the mutable session cart object.

### Account Became the Identity/Profile Module

Account now owns authentication, profile data, preferences, account persistence, and account web flow.

Architectural impact:

- Checkout no longer reaches into `AccountActionBean` directly.
- Order construction uses `CustomerProfile`, which is stable and narrower than the full `Account` domain object.
- Account remains the owner of favorite category data while catalog remains the owner of product lists.

### Order Became the Checkout and Order History Module

Order now owns order creation, persistence, order history, and order viewing.

Architectural impact:

- Order no longer depends directly on account/cart web classes.
- Order creation uses `OrderFactory` with `CustomerProfile` and `CartSnapshot`.
- Order service coordinates inventory reservation through an inventory API.
- Order persistence is grouped under `order.persistence` with matching XML namespaces.

### Shared Is Technical Infrastructure Only

Shared now contains web/session infrastructure that is not specific to any business module.

Architectural impact:

- `AbstractActionBean` is available to module web adapters without keeping the old `web.actions` package alive.
- `SessionState` provides a small session abstraction for checkout.
- `SessionState` does not import account/cart/order/catalog classes from production code.
- The shared package does not become a business model package.

### Legacy Package Compatibility Was Removed

The removal of legacy scan entries is important because it turns module consolidation from a cosmetic package move into an enforced runtime configuration.

Architectural impact:

- New classes added under old packages will no longer be picked up by Spring/MyBatis/Stripes accidentally.
- Mapper XML namespaces must match module persistence interfaces.
- Type aliases must come from module domain packages.

## 4. Dependencies Removed or Introduced

### Removed Dependencies

The phase removed or reduced these legacy dependencies:

- Runtime scanning of `org.mybatis.jpetstore.service`.
- Runtime scanning of `org.mybatis.jpetstore.mapper`.
- Runtime type alias scanning of `org.mybatis.jpetstore.domain`.
- Stripes action scanning of `org.mybatis.jpetstore.web`.
- Production imports from `org.mybatis.jpetstore.web.actions.AbstractActionBean`.
- Shared session infrastructure dependency on concrete account/cart web action classes.
- Order checkout dependency on account/cart web action classes for order creation.
- Cart/order dependency on catalog persistence for item data.
- Order dependency on inventory persistence for stock reservation.

### Introduced Dependencies

The phase introduced or formalized these dependencies:

- Module web actions depend on `shared.web.AbstractActionBean`.
- `AccountActionBean` implements `shared.web.SessionAccount`.
- `CartActionBean` implements `shared.web.SessionCart`.
- `OrderActionBean` depends on `shared.web.SessionState`.
- Cart application depends on:
  - `catalog.api`
  - `inventory.api`
- Order application depends on:
  - `account.api`
  - `cart.api`
  - `catalog.api`
  - `inventory.api`
- Shared test mapper configuration depends on module persistence packages for mapper tests.

### Dependency Direction After Phase 3

The intended dependency direction is:

```text
web adapters
  -> application services
  -> domain/persistence inside the same module
  -> other modules only through api packages

shared.web
  -> framework infrastructure only
```

Cross-module dependencies are now mostly API-level dependencies rather than direct implementation dependencies.

## 5. Before vs After System Structure

### Before

Before Phase 3, the structure was primarily layered:

```text
org.mybatis.jpetstore.domain
org.mybatis.jpetstore.mapper
org.mybatis.jpetstore.service
org.mybatis.jpetstore.web.actions
```

This made the code easy to group technically, but harder to reason about by business capability. A developer had to inspect individual classes to determine which business area owned a behavior. Configuration also scanned the old packages, so incomplete moves could remain hidden.

Typical coupling patterns before consolidation:

- Web actions from different workflows lived together in `web.actions`.
- Service classes lived together in `service`, regardless of business capability.
- Mapper classes lived together in `mapper`, regardless of table ownership.
- Domain classes lived together in `domain`, even when they represented different business concepts.
- Checkout behavior could reach across account/cart/catalog/inventory through concrete classes.

### After

After Phase 3, the structure is module-first:

```text
org.mybatis.jpetstore.account
  api
  application
  domain
  persistence
  web

org.mybatis.jpetstore.catalog
  api
  application
  domain
  persistence
  web

org.mybatis.jpetstore.inventory
  api
  application
  persistence

org.mybatis.jpetstore.cart
  api
  application
  domain
  web

org.mybatis.jpetstore.order
  api
  application
  domain
  persistence
  web

org.mybatis.jpetstore.shared
  web
```

Each package now communicates ownership:

- `*.web` contains Stripes adapters for a module.
- `*.application` contains use-case orchestration.
- `*.domain` contains module-owned domain/data models.
- `*.persistence` contains module-owned MyBatis mappers.
- `*.api` contains cross-module contracts and snapshots.
- `shared.web` contains framework/session infrastructure only.

### Before vs After Dependency Example: Checkout

Before:

```text
Order web flow
  -> AccountActionBean / Account
  -> CartActionBean / Cart
  -> Item/catalog/inventory implementation details
  -> Order construction and persistence
```

After:

```text
OrderActionBean
  -> SessionState
  -> CustomerProfile
  -> CartSnapshot
  -> OrderFactory
  -> OrderService
  -> InventoryReservationService
```

The after structure is still a monolith, but checkout now consumes narrower contracts that represent what it needs rather than where the data happens to live in the web session.

## 6. Design Decisions Made During Implementation

### Keep Module Boundaries as Packages, Not Build Modules

The implementation keeps the project as one Maven module. This was a deliberate incremental choice.

Reasoning:

- The existing application is a single WAR.
- Package consolidation delivers immediate architectural clarity without changing build, deployment, or dependency management.
- Stronger enforcement can be added later with ArchUnit or separate build modules.

### Use API Snapshot Types for Cross-Module Data

`CustomerProfile`, `ProductSummary`, `ItemSnapshot`, `CartSnapshot`, and `CartLineSnapshot` are used to pass data between modules.

Reasoning:

- Snapshots reduce dependency on mutable domain objects.
- They avoid leaking persistence models across module boundaries.
- They make checkout/cart/account interactions easier to test.

### Keep Shared Free of Business Concepts

`SessionState` needs to bridge session-backed account/cart state, but shared infrastructure should not import account/cart implementations.

Decision:

- Introduce `SessionAccount` and `SessionCart` as tiny technical contracts.
- Have module web beans implement those contracts.
- Let callers request typed values from `SessionState`.

This keeps `shared.web` decoupled from business modules while preserving the existing Stripes session behavior.

### Keep Common JSPs in Place

The common JSP directory remains under `WEB-INF/jsp/common`.

Reasoning:

- JSP layout fragments are presentation infrastructure, not Java module internals.
- Moving them during Phase 3 would add churn without materially improving Java dependency boundaries.

### Keep MyBatis XML Beside Module Persistence Paths

Mapper XML files were moved to resource paths matching the module persistence packages.

Reasoning:

- Mapper interface and XML namespace alignment is easier to audit.
- MyBatis configuration becomes consistent with Java package ownership.

### Remove Legacy Scan Entries Only After Module Scans Worked

The old package scan entries were removed at the end of the phase.

Reasoning:

- Removing scans too early would make intermediate package moves harder to validate.
- Removing them at Step 3.7 ensures the codebase no longer relies on compatibility scanning.

## 7. Potential Trade-offs or Limitations

### Package Boundaries Are Not Hard Build Boundaries

The modules are package-level modules inside one Maven project. Java still permits imports across packages unless enforced by tooling or review.

Implication:

- The architecture is clearer, but not fully mechanically enforced.
- Future hardening should add ArchUnit rules or split modules if stronger enforcement is required.

### Shared Session Interfaces Return Generic Objects Internally

`SessionAccount` and `SessionCart` expose snapshot/profile values without importing business API types into `shared.web`.

Implication:

- This keeps shared independent from business modules.
- The trade-off is that `SessionState` uses typed casts at its boundary.
- Incorrect type requests would fail at runtime rather than compile time.

This is acceptable for the current Stripes session bridge, but a future design could move session composition into a dedicated application-facing facade if stronger typing is desired.

### Web Framework Dependency Still Exists in Web and Some Domain Code

The web modules intentionally still depend on Stripes. Also, account domain validation annotations remain a known issue.

Implication:

- Phase 3 is about physical module consolidation, not full framework independence.
- Removing web framework annotations from domain classes is deferred to Phase 4.

### JSPs Still Reference Concrete Action Beans

JSPs continue to reference Stripes action bean classes directly.

Implication:

- This matches the existing framework style.
- It means presentation templates still know concrete web adapter classes.
- This is acceptable for the current architecture, but limits how easily web adapters could be swapped.

### Test Support Is Shared, But Still Infrastructure-Specific

`MapperTestContext` moved to `shared.persistence` in test sources.

Implication:

- Mapper tests have a common setup point.
- The test helper still knows all persistence packages because it bootstraps an integrated mapper test context.
- This is a test infrastructure dependency, not a production dependency.

### Empty Legacy Directories May Still Exist Locally

After moving files, empty legacy directories can remain in the working tree depending on local filesystem state.

Implication:

- Empty directories are not tracked by Git.
- The important cleanup is removal of Java/resource/config references to legacy packages.

## Verification

The following verification was performed during the phase:

```bash
./mvnw clean test
./mvnw test
```

The final test run completed successfully:

```text
Tests run: 121, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Additional source/config scans verified that production code and runtime configuration no longer reference:

- `org.mybatis.jpetstore.service`
- `org.mybatis.jpetstore.mapper`
- `org.mybatis.jpetstore.domain`
- `org.mybatis.jpetstore.web.actions`

Mapper XML namespaces were also verified to point to the moved module persistence interfaces.

## Conclusion

Phase 3 completes the physical consolidation of the modular monolith structure. The codebase is no longer organized primarily around technical layers. It is now organized around business capabilities, with cross-module interaction routed through API packages and shared infrastructure kept deliberately small.

The most important architectural outcome is that module boundaries are now visible in package structure, runtime scanning, mapper namespaces, JSP action references, and test organization. This provides a stable base for Phase 4, where the next focus can be hardening boundaries further by removing framework leakage from domain classes and adding stronger architectural enforcement.
