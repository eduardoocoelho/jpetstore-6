# Modularization & Modernization Plan

## Summary

This plan modernizes the JPetStore-like legacy monolith into a **modular monolith**, preserving the current Spring, MyBatis, Stripes, JSP, HSQLDB, and WAR deployment model initially.

Target modules:

```text
org.mybatis.jpetstore
├── account
├── catalog
├── inventory
├── cart
├── order
└── shared
```

Target dependency rule:

```text
account   -> shared
catalog   -> shared
inventory -> shared
cart      -> catalog.api, inventory.api, shared
order     -> account.api, cart.api, catalog.api, inventory.api, shared
shared    -> no business modules
```

Execution rule for Codex:

- Execute **one step at a time**.
- Keep behavior unchanged unless the step explicitly says otherwise.
- After each step, run at least `./mvnw test`.
- Avoid package moves until APIs and dependencies are stable.
- Do not introduce microservices, REST extraction, database splitting, or framework rewrites.

---

## Phase 1 — Stabilization & Boundary Definition

### Step 1.1 — Establish a Characterization Test Baseline

**Objective**

Create a reliable safety net before changing module boundaries.

**Why It Matters**

The current system has hidden coupling through session state, shared domain objects, and MyBatis mappers. Characterization tests protect existing behavior while boundaries are introduced incrementally.

**Actions**

- Run the full existing test suite:

```bash
./mvnw test
```

- Identify existing tests that cover:
  - `AccountService`
  - `CatalogService`
  - `OrderService`
  - mapper behavior
  - cart domain behavior
  - action beans
  - screen transitions
- Add or strengthen characterization tests for:
  - login loads account and favorite-category product list
  - cart add/remove/update quantity behavior
  - checkout creates an order from authenticated account and cart
  - order submission decrements inventory
  - order history is filtered by username
  - unauthorized order viewing is rejected
- Do not refactor production code in this step.

**Dependencies**

- None.

**Expected Outcome**

The current behavior is documented by tests, giving later refactorings a regression safety net.

---

### Step 1.2 — Define Module API Packages Without Changing Behavior

**Objective**

Introduce explicit module boundary contracts while existing classes remain in their current packages.

**Why It Matters**

The current architecture exposes concrete services, mappers, action beans, and domain objects across contexts. API interfaces allow dependencies to be redirected before physical package moves.

**Actions**

- Create API packages conceptually aligned with the target architecture:
  - `org.mybatis.jpetstore.account.api`
  - `org.mybatis.jpetstore.catalog.api`
  - `org.mybatis.jpetstore.inventory.api`
  - `org.mybatis.jpetstore.cart.api`
  - `org.mybatis.jpetstore.order.api`
- Define small interfaces:
  - `AccountQueryService`
  - `CatalogQueryService`
  - `InventoryQueryService`
  - `InventoryReservationService`
  - `CartQueryService`
  - `OrderQueryService`
- Initially implement these interfaces by adapting existing services:
  - `AccountService`
  - `CatalogService`
  - future `InventoryService`
  - future `CartService`
  - `OrderService`
- Keep existing public methods intact during this phase.

**Dependencies**

- Step 1.1.

**Expected Outcome**

The codebase has named module contracts, but runtime behavior remains unchanged.

---

### Step 1.3 — Introduce Cross-Module Snapshot Types

**Objective**

Define data transfer objects that modules can exchange without exposing internal domain objects.

**Why It Matters**

Today, `Order` depends directly on `Account` and `Cart`, `LineItem` depends on `CartItem`, and `CartItem` depends on `Item`. Snapshot types reduce semantic overloading and prepare the system for true module boundaries.

**Actions**

- Add API-level snapshot types:
  - `CustomerProfile` in `account.api`
  - `ItemSnapshot` and `ProductSummary` in `catalog.api`
  - `StockLevel` or `InventoryStatus` in `inventory.api`
  - `CartSnapshot` and `CartLineSnapshot` in `cart.api`
  - `CheckoutCommand` in `order.api`
- Include only fields required across boundaries.
- Do not replace existing domain usage yet.
- Map from existing domain objects to snapshots in adapter methods.

**Dependencies**

- Step 1.2.

**Expected Outcome**

Modules have stable exchange models that avoid passing mutable internal objects across boundaries.

---

### Step 1.4 — Introduce a Shared Session Access Facade

**Objective**

Hide direct Stripes session keys behind a shared web abstraction.

**Why It Matters**

`OrderActionBean` currently reads `AccountActionBean` and `CartActionBean` directly from the HTTP session using keys such as `"/actions/Account.action"`, `"/actions/Cart.action"`, and `"accountBean"`. This is one of the most fragile coupling points.

**Actions**

- Create a shared web component such as `SessionState`.
- Initially let `SessionState` read the existing session attributes internally.
- Expose methods such as:
  - `isAuthenticated()`
  - `getCurrentCustomerProfile()`
  - `getCurrentUsername()`
  - `getCurrentCartSnapshot()`
  - `clearCart()`
- Do not change session keys yet.
- Add tests for session lookup behavior.

**Dependencies**

- Steps 1.2 and 1.3.

**Expected Outcome**

Future code can depend on `SessionState` instead of concrete action beans and raw session keys.

---

### Step 1.5 — Prepare Framework Scanning for Modular Packages

**Objective**

Make Spring, MyBatis, and Stripes ready for later package moves.

**Why It Matters**

The current configuration scans narrow layered packages:
- Spring scans `org.mybatis.jpetstore.service`
- MyBatis scans `org.mybatis.jpetstore.mapper`
- Stripes resolves actions under `org.mybatis.jpetstore.web`

Package moves will fail unless scanning is adjusted deliberately.

**Actions**

- Plan Spring component scanning to include module application packages.
- Plan MyBatis mapper scanning to include module persistence packages.
- Plan MyBatis type aliases to include moved domain packages.
- Plan Stripes action discovery to include module web packages.
- Prefer a conservative transition:
  - first expand scanning to support both old and new packages
  - only later remove old scanning paths

**Dependencies**

- Step 1.2.

**Expected Outcome**

The implementation has a clear wiring strategy for package migration without breaking runtime discovery.

---

## Phase 2 — Core Decoupling

### Step 2.1 — Extract Inventory Persistence from `ItemMapper`

**Objective**

Create an explicit inventory persistence boundary.

**Why It Matters**

`ItemMapper` currently mixes catalog reads with inventory reads and stock updates. This violates both the Catalog and Inventory boundaries and creates coupling between Cart, Order, and Catalog persistence.

**Actions**

- Create `InventoryMapper`.
- Create `InventoryMapper.xml`.
- Move these operations from `ItemMapper` to `InventoryMapper`:
  - `getInventoryQuantity`
  - `updateInventoryQuantity`
- Keep the SQL and table unchanged.
- Keep `INVENTORY` in the same database.
- Update MyBatis namespace and tests.
- Remove inventory SQL from `ItemMapper.xml` only after all callers are migrated.

**Dependencies**

- Phase 1 complete.

**Expected Outcome**

Inventory reads and writes are no longer owned by the catalog item mapper.

---

### Step 2.2 — Introduce `InventoryService`

**Objective**

Centralize stock availability and stock decrement behind an application service.

**Why It Matters**

`CatalogService.isItemInStock` and `OrderService.insertOrder` currently own inventory behavior that belongs to the Inventory module.

**Actions**

- Create `InventoryService`.
- Implement:
  - `InventoryQueryService`
  - `InventoryReservationService`
- Add methods:
  - `getQuantity(String itemId)`
  - `isInStock(String itemId)`
  - `decrement(String itemId, int quantity)`
- Delegate to `InventoryMapper`.
- Keep existing stock behavior initially:
  - `isInStock` means quantity greater than zero
  - decrement subtracts submitted quantity
- Add mapper and service tests.

**Dependencies**

- Step 2.1.

**Expected Outcome**

Inventory becomes an explicit module responsibility with a service API.

---

### Step 2.3 — Redirect Cart Stock Checks to Inventory

**Objective**

Remove stock availability responsibility from `CatalogService` and `CartActionBean`.

**Why It Matters**

Cart needs item details from Catalog and availability from Inventory. Using `CatalogService.isItemInStock` hides this distinction.

**Actions**

- Inject `InventoryQueryService` into `CartActionBean` temporarily.
- Replace:

```text
catalogService.isItemInStock(itemId)
```

with:

```text
inventoryQueryService.isInStock(itemId)
```

- Keep `catalogService.getItem(itemId)` for item details in this step.
- Keep UI behavior unchanged.
- Update `CartActionBeanTest`.

**Dependencies**

- Step 2.2.

**Expected Outcome**

Cart no longer depends on Catalog for inventory availability.

---

### Step 2.4 — Redirect Order Stock Mutation to Inventory

**Objective**

Remove direct inventory mutation from `OrderService`.

**Why It Matters**

`OrderService` currently calls `ItemMapper.updateInventoryQuantity`, which means Order directly mutates inventory persistence. The target boundary is `OrderService -> InventoryReservationService`.

**Actions**

- Inject `InventoryReservationService` into `OrderService`.
- Replace direct `ItemMapper.updateInventoryQuantity` calls with:

```text
inventoryReservationService.decrement(itemId, quantity)
```

- Keep the transaction on `OrderService.insertOrder`.
- Ensure inventory decrement remains part of the same monolith transaction.
- Update `OrderServiceTest`.

**Dependencies**

- Step 2.2.

**Expected Outcome**

Order no longer updates the `INVENTORY` table through catalog persistence.

---

### Step 2.5 — Introduce `CatalogQueryService` for Item Details

**Objective**

Expose catalog item reads through a Catalog API instead of concrete catalog service or mapper internals.

**Why It Matters**

Cart and Order need sellable item data, but they should not depend on `ItemMapper` or mutable catalog internals.

**Actions**

- Make `CatalogService` implement `CatalogQueryService`.
- Add methods such as:
  - `getItemSnapshot(String itemId)`
  - `getProductSummary(String productId)`
  - `getProductsByCategory(String categoryId)`
- Initially map from existing `Item` and `Product` objects.
- Keep existing `getItem` and `getProductListByCategory` methods for compatibility.
- Update tests for snapshot mapping.

**Dependencies**

- Step 1.3.

**Expected Outcome**

Cross-module reads of catalog data can use stable API snapshots.

---

### Step 2.6 — Introduce `CartService`

**Objective**

Move cart application logic out of `CartActionBean`.

**Why It Matters**

`CartActionBean` currently handles request logic, catalog lookup, inventory lookup, and cart mutation. A `CartService` makes Cart a real application module instead of only session state manipulated by the web layer.

**Actions**

- Create `CartService`.
- Move add-item orchestration into:

```text
addItem(Cart cart, String itemId)
```

- `CartService` should use:
  - `CatalogQueryService` for item details
  - `InventoryQueryService` for stock status
- Keep `Cart` session-scoped in `CartActionBean`.
- Keep quantity update and remove behavior either in `Cart` or through thin service methods.
- Update `CartActionBean` to delegate to `CartService`.
- Update `CartActionBeanTest` and `CartTest`.

**Dependencies**

- Steps 2.3 and 2.5.

**Expected Outcome**

Cart behavior is accessible through an application service, while the action becomes a Stripes adapter.

---

### Step 2.7 — Replace Action-to-Action Checkout Coupling

**Objective**

Stop `OrderActionBean` from reading `AccountActionBean` and `CartActionBean` directly from the session.

**Why It Matters**

Order should depend on account and cart data contracts, not on other web controllers. This is a major boundary improvement with high architectural impact.

**Actions**

- Inject or create `SessionState` in `OrderActionBean`.
- Replace session casts to:
  - `AccountActionBean`
  - `CartActionBean`
- Use:
  - `SessionState.getCurrentCustomerProfile()`
  - `SessionState.getCurrentCartSnapshot()`
  - `SessionState.clearCart()`
- Preserve current redirect/forward behavior.
- Preserve authentication checks.
- Update `OrderActionBeanTest`.

**Dependencies**

- Steps 1.4, 1.3, and 2.6.

**Expected Outcome**

Order web flow no longer depends on account/cart action bean classes.

---

### Step 2.8 — Move Order Creation to `OrderFactory` or `CheckoutService`

**Objective**

Remove order initialization logic from the `Order` domain entity.

**Why It Matters**

`Order.initOrder(Account, Cart)` makes Order depend directly on Account and Cart internals. The target is explicit checkout orchestration using snapshots.

**Actions**

- Create `OrderFactory` or `CheckoutService`.
- Add method:

```text
createOrder(CustomerProfile customer, CartSnapshot cart)
```

- Move logic from `Order.initOrder(Account, Cart)` into the new component.
- Preserve existing default values initially:
  - credit card placeholder
  - expiry date
  - card type
  - courier
  - locale
  - status
- Convert cart lines to order lines using `CartLineSnapshot`.
- Keep old `initOrder` temporarily if needed, but mark it as transitional.
- Update `OrderTest` and `OrderActionBeanTest`.

**Dependencies**

- Step 2.7.

**Expected Outcome**

Order creation no longer requires mutable `Account`, `Cart`, or `CartItem` objects.

---

### Step 2.9 — Remove `OrderService -> ItemMapper` Dependency

**Objective**

Ensure `OrderService` no longer depends on catalog/inventory persistence.

**Why It Matters**

`OrderService` currently uses `ItemMapper` both for inventory mutation and order display enrichment. This makes Order structurally dependent on Catalog persistence.

**Actions**

- Confirm inventory mutation was moved to `InventoryReservationService`.
- For `getOrder(orderId)`, replace `ItemMapper.getItem` with `CatalogQueryService.getItemSnapshot`.
- If inventory quantity is still displayed on order view, obtain it through `InventoryQueryService`.
- Keep `LineItem.item` temporarily if JSPs still require it, but populate it through API-based mapping instead of mapper injection.
- Remove `ItemMapper` from the `OrderService` constructor.
- Update `OrderServiceTest`.

**Dependencies**

- Steps 2.4 and 2.5.

**Expected Outcome**

Order depends on Catalog and Inventory APIs, not on `ItemMapper`.

---

## Phase 3 — Module Consolidation

### Step 3.1 — Move Inventory into Its Module Package

**Objective**

Physically consolidate inventory code after its logical boundary is stable.

**Why It Matters**

Inventory is the highest-value early module because it was previously implicit and mixed into Catalog and Order.

**Actions**

- Move inventory classes to:
  - `org.mybatis.jpetstore.inventory.application`
  - `org.mybatis.jpetstore.inventory.api`
  - `org.mybatis.jpetstore.inventory.persistence`
- Move `InventoryMapper.xml` to a matching resource location.
- Update MyBatis namespace.
- Update mapper scanning.
- Run inventory mapper and service tests.

**Dependencies**

- Phase 2 inventory steps complete.

**Expected Outcome**

Inventory exists as a clear internal module owning `INVENTORY`.

---

### Step 3.2 — Move Catalog into Its Module Package

**Objective**

Consolidate catalog classes around product discovery and item descriptions.

**Why It Matters**

Catalog is already cohesive, but it still lives inside technical layer packages. Moving it after inventory extraction avoids carrying stock mutation into the Catalog module.

**Actions**

- Move catalog classes to:
  - `org.mybatis.jpetstore.catalog.web`
  - `org.mybatis.jpetstore.catalog.application`
  - `org.mybatis.jpetstore.catalog.api`
  - `org.mybatis.jpetstore.catalog.domain`
  - `org.mybatis.jpetstore.catalog.persistence`
- Move:
  - `CatalogActionBean`
  - `CatalogService`
  - `Category`
  - `Product`
  - catalog item model
  - `CategoryMapper`
  - `ProductMapper`
  - catalog item mapper
- Update MyBatis XML namespaces and type aliases.
- Update JSP action references if package names affect Stripes links.
- Run catalog service, mapper, action, and screen transition tests.

**Dependencies**

- Step 3.1.
- `CatalogService` must no longer own inventory mutation.

**Expected Outcome**

Catalog owns category, product, item description, and search behavior.

---

### Step 3.3 — Move Cart into Its Module Package

**Objective**

Consolidate cart session state and cart application behavior.

**Why It Matters**

Cart becomes a module only after `CartService` exists and the action is thin.

**Actions**

- Move cart classes to:
  - `org.mybatis.jpetstore.cart.web`
  - `org.mybatis.jpetstore.cart.application`
  - `org.mybatis.jpetstore.cart.api`
  - `org.mybatis.jpetstore.cart.domain`
- Move:
  - `CartActionBean`
  - `CartService`
  - `Cart`
  - `CartItem`
  - cart snapshot types
- Ensure Cart depends only on:
  - `catalog.api`
  - `inventory.api`
  - `shared`
- Update Stripes discovery.
- Update cart JSP action references if needed.
- Run cart domain/action tests.

**Dependencies**

- Steps 2.6 and 3.2.

**Expected Outcome**

Cart is isolated as a session-backed module with explicit dependencies on Catalog and Inventory APIs.

---

### Step 3.4 — Move Account into Its Module Package

**Objective**

Consolidate account identity, profile, authentication, and preferences.

**Why It Matters**

Account is a coherent module, but it currently leaks into Catalog through `myList` and into Order through session-coupled access.

**Actions**

- Move account classes to:
  - `org.mybatis.jpetstore.account.web`
  - `org.mybatis.jpetstore.account.application`
  - `org.mybatis.jpetstore.account.api`
  - `org.mybatis.jpetstore.account.domain`
  - `org.mybatis.jpetstore.account.persistence`
- Move:
  - `AccountActionBean`
  - `AccountService`
  - `Account`
  - `AccountMapper`
  - `AccountMapper.xml`
  - `CustomerProfile`
- Keep favorite category as account data.
- Do not let Account directly load product lists as a module responsibility.
- Update MyBatis XML namespace and type aliases.
- Update Stripes discovery and JSP references.
- Run account mapper, service, and action tests.

**Dependencies**

- Step 2.7 should already remove direct Order dependency on `AccountActionBean`.
- Catalog product list personalization should already be behind an API or presentation composition boundary.

**Expected Outcome**

Account owns identity/profile and exposes customer data through `account.api`.

---

### Step 3.5 — Move Order into Its Module Package

**Objective**

Consolidate checkout, order persistence, order history, and order viewing.

**Why It Matters**

Order is the most coupled module and should move only after its dependencies are redirected through APIs.

**Actions**

- Move order classes to:
  - `org.mybatis.jpetstore.order.web`
  - `org.mybatis.jpetstore.order.application`
  - `org.mybatis.jpetstore.order.api`
  - `org.mybatis.jpetstore.order.domain`
  - `org.mybatis.jpetstore.order.persistence`
- Move:
  - `OrderActionBean`
  - `OrderService`
  - `CheckoutService`
  - `OrderFactory`
  - `Order`
  - `LineItem`
  - `Sequence`
  - `OrderMapper`
  - `LineItemMapper`
  - `SequenceMapper`
  - mapper XML files
- Ensure Order depends only on:
  - `account.api`
  - `cart.api`
  - `catalog.api`
  - `inventory.api`
  - `shared`
- Update MyBatis namespaces and type aliases.
- Run order service, mapper, action, domain, and screen transition tests.

**Dependencies**

- Steps 2.7, 2.8, and 2.9.

**Expected Outcome**

Order is physically modularized without direct dependency on account/cart web classes or catalog persistence.

---

### Step 3.6 — Move Shared Web Infrastructure

**Objective**

Create a small shared module for technical infrastructure only.

**Why It Matters**

Shared must not become a dumping ground for business concepts. It should contain only cross-cutting infrastructure.

**Actions**

- Move shared technical classes to:
  - `org.mybatis.jpetstore.shared.web`
  - `org.mybatis.jpetstore.shared.application`
- Move:
  - `AbstractActionBean`
  - `SessionState`
  - shared exception or utility classes only if needed
- Keep common JSPs under `WEB-INF/jsp/common` for now.
- Verify shared has no dependency on business modules.
- Run full test suite.

**Dependencies**

- Module web packages must already be discoverable by Stripes.

**Expected Outcome**

Common web/session infrastructure is separated from business modules.

---

### Step 3.7 — Clean Legacy Package Compatibility

**Objective**

Remove obsolete layered package references after module moves are complete.

**Why It Matters**

Leaving old package references can hide accidental dependencies and make the modularization appear incomplete.

**Actions**

- Remove unused imports from old packages:
  - `org.mybatis.jpetstore.service`
  - `org.mybatis.jpetstore.mapper`
  - `org.mybatis.jpetstore.domain`
  - `org.mybatis.jpetstore.web.actions`
- Remove old package scan entries only after tests pass with new module scans.
- Verify MyBatis XML namespaces match moved mapper interfaces.
- Verify type aliases resolve moved domain classes.
- Run:

```bash
./mvnw test
```

**Dependencies**

- Steps 3.1 through 3.6.

**Expected Outcome**

The codebase structure reflects the modular monolith architecture.

---

## Phase 4 — Architectural Hardening

### Step 4.1 — Remove Web Framework Dependencies from Domain

**Objective**

Make domain classes independent from Stripes and servlet concerns.

**Why It Matters**

`Account` currently imports `net.sourceforge.stripes.validation.Validate`. Domain objects should not depend on web framework annotations.

**Actions**

- Remove `@Validate` from `Account`.
- Move validation to:
  - `AccountActionBean`, or
  - account form/request DTOs.
- Keep validation behavior equivalent for:
  - signon
  - new account
  - edit account
- Add/adjust tests for validation paths.
- Check all domain packages for Stripes, servlet, Spring, and MyBatis imports.

**Dependencies**

- Account module moved or at least isolated.

**Expected Outcome**

Domain classes are framework-neutral.

---

### Step 4.2 — Remove Inventory State from Catalog Item Usage

**Objective**

Stop relying on `Item.quantity` as a catalog concept.

**Why It Matters**

Quantity belongs to Inventory, not Catalog. Keeping it on `Item` preserves the semantic overload identified by both SIA analyses.

**Actions**

- Replace new code usage of `Item.quantity` with:
  - `StockLevel`
  - `InventoryStatus`
  - `InventoryQueryService.getQuantity`
- Update catalog item queries to return catalog fields only where possible.
- If a view needs both item details and stock, compose:
  - `ItemSnapshot`
  - `InventoryStatus`
- Keep legacy getter/setter only temporarily if JSPs or MyBatis mappings still require it.
- Remove `quantity` from catalog mappings once no longer used.

**Dependencies**

- Inventory module and Catalog API in place.

**Expected Outcome**

Catalog owns item identity/description; Inventory owns quantity/availability.

---

### Step 4.3 — Remove Transitional Order/Cart Coupling Methods

**Objective**

Delete or deprecate old domain methods that cross module boundaries.

**Why It Matters**

Keeping `Order.initOrder(Account, Cart)` and `LineItem(int, CartItem)` allows future code to reintroduce direct coupling.

**Actions**

- Replace all usages of:
  - `Order.initOrder(Account, Cart)`
  - `Order.addLineItem(CartItem)`
  - `LineItem(int, CartItem)`
- Use:
  - `OrderFactory.createOrder(CustomerProfile, CartSnapshot)`
  - line creation from `CartLineSnapshot`
- Remove the old methods if no longer used.
- Update `OrderTest`.
- Run full tests.

**Dependencies**

- Step 2.8 complete.

**Expected Outcome**

Order domain no longer depends on Account or Cart domain classes.

---

### Step 4.4 — Add Architecture Boundary Tests

**Objective**

Prevent regression to layered cross-module coupling.

**Why It Matters**

Package moves alone do not enforce architecture. Boundary tests make the modular monolith sustainable.

**Actions**

- Add architecture tests, preferably with ArchUnit.
- Enforce rules such as:
  - `account` must not depend on `cart`, `order`, or `inventory.persistence`
  - `catalog` must not depend on `cart`, `order`, or inventory mutation
  - `cart` must not depend on `order`
  - `order` must not depend on `account.web` or `cart.web`
  - `order` must not depend on `catalog.persistence`
  - `shared` must not depend on business modules
  - domain packages must not depend on Stripes, servlet, Spring, MyBatis, or JSP APIs
- Add the architecture tests to the normal Maven test lifecycle.

**Dependencies**

- Phase 3 package consolidation complete.

**Expected Outcome**

Modular boundaries become executable constraints.

---

### Step 4.5 — Harden Inventory Consistency During Order Submission

**Objective**

Make stock decrement behavior explicit and safer.

**Why It Matters**

The current order submission decrements inventory but does not clearly model final availability validation. Keeping the same database and transaction model is fine, but the rule should live in Inventory.

**Actions**

- Add final stock validation inside `InventoryReservationService`.
- Keep behavior compatible unless tests define stricter rejection behavior.
- Prefer one method for order submission use:

```text
decrement(String itemId, int quantity)
```

or:

```text
decrementAll(List<StockDecreaseRequest> requests)
```

- If insufficient stock is detected, throw a module-level application exception.
- Translate the exception in the web layer to the existing error flow.
- Add tests for:
  - successful decrement
  - insufficient stock
  - transaction rollback when order persistence fails after decrement

**Dependencies**

- Step 2.4 and inventory module consolidation.

**Expected Outcome**

Inventory consistency rules live in Inventory and remain transactional inside the monolith.

---

## Phase 5 — Optional Improvements (Lower Priority)

### Step 5.1 — Introduce View Models for Shared JSPs

**Objective**

Reduce JSP dependency on concrete action bean session internals.

**Why It Matters**

Common JSPs currently read session state directly, including account-related state. That keeps views coupled to controller implementation details.

**Actions**

- Create shared view/session models for:
  - authenticated user display
  - header search data
  - cart summary
  - account banner/list preferences
- Replace direct reads of concrete session action beans in:
  - `IncludeTop.jsp`
  - `IncludeBottom.jsp`
  - `IncludeMyList.jsp`
- Keep JSP and Stripes stack unchanged.
- Add screen transition tests for header/footer behavior.

**Dependencies**

- `SessionState` and module APIs in place.

**Expected Outcome**

JSPs render stable view data instead of knowing action bean session keys.

---

### Step 5.2 — Centralize Category Navigation in Catalog

**Objective**

Remove duplicated hardcoded category lists.

**Why It Matters**

Categories are currently repeated in JSPs and account action code. Catalog should be the authority for category data.

**Actions**

- Replace hardcoded category lists in:
  - account preferences
  - common header
  - catalog main page
- Use `CatalogQueryService.getCategoryList()`.
- Preserve current category ordering and labels.
- Add tests or screen checks for category navigation.

**Dependencies**

- Catalog API available.
- JSP view model composition started or planned.

**Expected Outcome**

Catalog becomes the single source of category navigation data.

---

### Step 5.3 — Add Order Line Snapshot Data

**Objective**

Reduce historical order viewing dependency on live catalog data.

**Why It Matters**

Order history should ideally show what was purchased at the time of purchase, not depend on current catalog state. This is architecturally cleaner but may require schema changes, so it is lower priority.

**Actions**

- Add snapshot fields to order line persistence if acceptable for the thesis implementation:
  - item display name
  - product name
  - attributes
  - unit price already exists
- Populate snapshot fields during order creation.
- Update order view to prefer stored snapshot fields.
- Keep live catalog enrichment only as a fallback during migration.
- Add migration or schema update for HSQLDB scripts.

**Dependencies**

- OrderFactory and Catalog API in place.
- Core modularization complete.

**Expected Outcome**

Order history becomes more independent from Catalog.

---

### Step 5.4 — Improve Authentication Storage

**Objective**

Replace plain-text password storage with hashed passwords.

**Why It Matters**

The analysis identifies plain-text passwords as a legacy weakness. This is important for real modernization, but not required for modular boundaries.

**Actions**

- Introduce password hashing in Account.
- Update account creation and password update.
- Support migration or test data regeneration for existing demo users.
- Update `AccountMapper.xml` and seed data.
- Add tests for authentication success/failure.

**Dependencies**

- Account module isolated.
- Existing account behavior covered by tests.

**Expected Outcome**

Account security improves without changing module boundaries.

---

### Step 5.5 — Reduce HTML Stored in Database

**Objective**

Separate presentation markup from domain/catalog data.

**Why It Matters**

Product descriptions and banners contain HTML fragments in seed data. This limits future UI evolution and makes content less portable.

**Actions**

- Identify fields containing HTML in catalog/account data.
- Decide whether to:
  - sanitize at render time, or
  - migrate stored content to plain text plus view formatting.
- Update JSP rendering accordingly.
- Keep this outside the core modularization path.

**Dependencies**

- Catalog and Account modules stable.

**Expected Outcome**

Catalog data becomes less coupled to JSP-specific presentation.

---

### Step 5.6 — Modernize Configuration Incrementally

**Objective**

Improve maintainability of framework wiring without changing the application architecture.

**Why It Matters**

The project uses XML Spring, XML MyBatis, `web.xml`, JSP, and Stripes. These are preserved initially, but configuration can be cleaned after modularization.

**Actions**

- Keep the WAR deployment and current stack.
- Consolidate component scans around module roots.
- Remove obsolete scan paths.
- Document module ownership in configuration comments.
- Avoid migrating to Spring Boot or REST unless a separate future modernization track explicitly requires it.

**Dependencies**

- Phase 3 complete.

**Expected Outcome**

Configuration reflects the modular monolith structure while preserving legacy runtime behavior.

---

## Assumptions and Defaults

- The system remains a **single deployable WAR**.
- The database remains shared initially.
- No microservice extraction is part of this plan.
- No frontend rewrite is part of this plan.
- Spring, MyBatis, Stripes, and JSP are preserved during core modularization.
- Package moves happen only after boundary APIs are introduced and tested.
- Inventory is extracted before package consolidation because it is the most important hidden domain boundary.
- Order is moved later because it has the strongest current coupling to Account, Cart, Catalog, and Inventory.
- Optional security and view improvements are lower priority than modular boundaries.

## Acceptance Criteria

- `./mvnw test` passes after every step.
- The application still supports:
  - login/logout
  - account creation and edit
  - catalog browsing and search
  - cart add/remove/update
  - checkout
  - order submission
  - inventory decrement
  - order history and order viewing
- `OrderActionBean` no longer depends on `AccountActionBean` or `CartActionBean`.
- `OrderService` no longer depends on `ItemMapper`.
- Inventory SQL is no longer inside catalog item persistence.
- Cart uses Catalog and Inventory through APIs.
- Domain classes do not depend on Stripes or servlet APIs.
- Module packages reflect `account`, `catalog`, `inventory`, `cart`, `order`, and `shared`.
- Architecture tests prevent forbidden dependencies from returning.
