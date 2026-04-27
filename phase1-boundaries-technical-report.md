# Phase 1 Boundaries Technical Report

This report documents the architectural and code-level changes implemented on branch `phase1-boundaries`, using `master` as the baseline for comparison.

## 1. Summary of changes

Phase 1 introduced the first explicit architectural boundaries needed to evolve JPetStore from a package-centric monolith into a modular monolith with bounded contexts. The branch does not yet complete the module extraction, but it establishes the first stable contracts and supporting documentation required for that transition.

The main outcomes are:

1. Architectural analysis artifacts were added to describe the current system, its dependency structure, domain decomposition, and a target modular architecture.
2. New module-facing API interfaces were created for `account`, `catalog`, `inventory`, `cart`, and `order`.
3. Existing service classes were adapted to implement these APIs instead of exposing only concrete class contracts.
4. Snapshot/DTO records were introduced to reduce direct sharing of mutable domain objects across future module boundaries.
5. A `SessionState` abstraction was added to centralize access to Stripes session keys and expose session data through boundary-safe representations.
6. Spring/MyBatis/Stripes configuration was widened to support the target package layout for future module relocation.
7. The automated test suite was expanded to characterize the new contracts and protect the current behavior during future refactoring.

In practical terms, this phase moves the codebase from "services and actions directly share domain state and session details" to "services expose explicit boundary contracts and the web session has an adapter layer". The implementation is intentionally conservative: it preserves the current runtime behavior while preparing the code for deeper modularization in later phases.

## 2. Files modified and their roles

### 2.1 Architecture and planning documents

| File | Role |
| --- | --- |
| `architecture-analysis-jpetstore-6.md` | Technical analysis of the current architecture, runtime flows, coupling points, and modernization constraints. |
| `SIA-1-Dependency-Decomposition.md` | Structural dependency analysis and candidate module decomposition from a dependency perspective. |
| `SIA-2-Domain-Decomposition.md` | Domain-oriented decomposition and bounded-context identification. |
| `Modular-Architecture-Proposal.md` | Target modular monolith proposal, dependency rules, code mapping, and staged refactoring plan. |

### 2.2 New boundary APIs and data contracts

| File | Role |
| --- | --- |
| `src/main/java/org/mybatis/jpetstore/account/api/AccountQueryService.java` | Account read-side boundary for account retrieval and customer profile projection. |
| `src/main/java/org/mybatis/jpetstore/account/api/CustomerProfile.java` | Immutable customer-facing account projection for cross-module use. |
| `src/main/java/org/mybatis/jpetstore/cart/api/CartQueryService.java` | Cart read-side contract plus default snapshot mapping logic. |
| `src/main/java/org/mybatis/jpetstore/cart/api/CartSnapshot.java` | Immutable cart aggregate snapshot for boundary-safe transfer. |
| `src/main/java/org/mybatis/jpetstore/cart/api/CartLineSnapshot.java` | Immutable cart line projection. |
| `src/main/java/org/mybatis/jpetstore/catalog/api/CatalogQueryService.java` | Catalog read-side boundary for categories, products, items, and projections. |
| `src/main/java/org/mybatis/jpetstore/catalog/api/ProductSummary.java` | Compact immutable product projection. |
| `src/main/java/org/mybatis/jpetstore/catalog/api/ItemSnapshot.java` | Immutable item projection without exposing the mutable domain aggregate. |
| `src/main/java/org/mybatis/jpetstore/inventory/api/InventoryQueryService.java` | Inventory read-side contract for stock visibility. |
| `src/main/java/org/mybatis/jpetstore/inventory/api/InventoryReservationService.java` | Inventory write-side contract for stock reservation/decrement behavior. |
| `src/main/java/org/mybatis/jpetstore/inventory/api/InventoryStatus.java` | Immutable inventory projection. |
| `src/main/java/org/mybatis/jpetstore/order/api/CheckoutCommand.java` | Intent-revealing checkout input model combining customer and cart snapshots. |
| `src/main/java/org/mybatis/jpetstore/order/api/OrderQueryService.java` | Order read-side boundary for order retrieval and listing. |

### 2.3 Adapted service layer

| File | Role |
| --- | --- |
| `src/main/java/org/mybatis/jpetstore/service/AccountService.java` | Now implements `AccountQueryService` and maps `Account` to `CustomerProfile`. |
| `src/main/java/org/mybatis/jpetstore/service/CatalogService.java` | Now implements `CatalogQueryService` and `InventoryQueryService`, and maps catalog entities to snapshots/projections. |
| `src/main/java/org/mybatis/jpetstore/service/OrderService.java` | Now implements `OrderQueryService` and `InventoryReservationService`, and exposes inventory decrement as an explicit boundary operation. |

### 2.4 Web/session boundary support

| File | Role |
| --- | --- |
| `src/main/java/org/mybatis/jpetstore/shared/web/SessionState.java` | Central adapter around `HttpSession` and Stripes action beans, exposing authenticated user and cart state without leaking raw session keys to future consumers. |
| `src/main/java/org/mybatis/jpetstore/web/actions/CartActionBean.java` | Touched in this phase, but effectively unchanged from an architectural standpoint. |

### 2.5 Runtime configuration updates

| File | Role |
| --- | --- |
| `src/main/webapp/WEB-INF/applicationContext.xml` | Expanded component scan, mapper scan, and type alias scan for future module package locations. |
| `src/main/webapp/WEB-INF/web.xml` | Expanded Stripes action package resolution for future module-specific web packages. |

### 2.6 Test updates

| File | Role |
| --- | --- |
| `src/test/java/org/mybatis/jpetstore/cart/api/CartQueryServiceTest.java` | Verifies mapping from mutable cart state to immutable cart snapshots. |
| `src/test/java/org/mybatis/jpetstore/shared/web/SessionStateTest.java` | Verifies centralized session access and snapshot extraction behavior. |
| `src/test/java/org/mybatis/jpetstore/mapper/MapperTestContext.java` | Aligns test mapper/type-alias scanning with the future modular package structure. |
| `src/test/java/org/mybatis/jpetstore/service/AccountServiceTest.java` | Verifies API implementation and `CustomerProfile` mapping. |
| `src/test/java/org/mybatis/jpetstore/service/CatalogServiceTest.java` | Verifies API implementation and product/item/inventory projections. |
| `src/test/java/org/mybatis/jpetstore/service/OrderServiceTest.java` | Verifies API implementation and inventory reservation behavior. |
| `src/test/java/org/mybatis/jpetstore/web/actions/AccountActionBeanTest.java` | Strengthens behavioral coverage for sign-on/session alias behavior. |
| `src/test/java/org/mybatis/jpetstore/web/actions/CartActionBeanTest.java` | Strengthens behavioral coverage around cart mutation workflows. |
| `src/test/java/org/mybatis/jpetstore/web/actions/OrderActionBeanTest.java` | Strengthens behavioral coverage around checkout, order submission, and authorization checks. |

## 3. Architectural impact of each change

### 3.1 Architecture documentation became part of the deliverable

The four new Markdown documents are not incidental documentation. They define the decomposition model that the code changes are implementing. This matters architecturally because the branch stops treating modularization as an implicit refactor and turns it into an explicit design program with named modules, allowed dependencies, and a staged migration sequence.

Impact:

- Establishes `account`, `catalog`, `inventory`, `cart`, `order`, and `shared` as the target architectural vocabulary.
- Provides a traceable rationale for why boundaries are being introduced before packages are moved.
- Reduces the risk of future refactors degenerating into ad hoc package reshuffling.

### 3.2 API interfaces decouple consumers from concrete services

Before this phase, the application layer was centered on concrete service classes in `org.mybatis.jpetstore.service`. After this phase, the branch introduces explicit read/write contracts under per-domain `api` packages.

Impact:

- Creates stable seams for future package relocation and module isolation.
- Makes service responsibilities explicit at the boundary level instead of only at the implementation level.
- Supports later substitution of implementations without changing callers.
- Starts separating read concerns (`*QueryService`) from stock mutation concerns (`InventoryReservationService`).

This is the most important architectural step in the branch.

### 3.3 Snapshot records reduce shared mutable-domain leakage

The introduction of `CustomerProfile`, `ProductSummary`, `ItemSnapshot`, `CartLineSnapshot`, `CartSnapshot`, and `InventoryStatus` changes the shape of cross-boundary data exchange.

Impact:

- Reduces exposure of mutable, persistence-oriented domain entities across future module boundaries.
- Narrows the transferred data to what consumers actually need.
- Makes downstream consumers less dependent on internal object graphs such as `Item -> Product`.
- Creates a path to remove accidental coupling caused by shared domain entities.

This is an architectural improvement even though the current code still uses the original domain entities in many places.

### 3.4 Service implementations now act as adapters between legacy internals and future module APIs

`AccountService`, `CatalogService`, and `OrderService` still use the existing mappers and domain model, but they now also implement domain APIs and perform projection logic.

Impact:

- Preserves runtime behavior while inserting a contract layer above legacy persistence and domain objects.
- Allows future call sites to depend on interfaces without immediately rewriting the persistence model.
- Keeps the refactor incremental: boundary creation first, module extraction later.

The trade-off is that service classes temporarily carry more responsibilities than before: they remain legacy services while also becoming boundary adapters.

### 3.5 Inventory is being extracted conceptually from catalog and order

The new inventory interfaces are significant because they start naming inventory as an independent concern instead of an incidental behavior embedded in `CatalogService` and `OrderService`.

Impact:

- `CatalogService` now exposes inventory state through `InventoryQueryService`.
- `OrderService` now exposes stock decrement through `InventoryReservationService`.
- Inventory becomes a first-class bounded context candidate instead of a helper capability hidden inside unrelated services.

This is an architectural extraction in contract form, even though the underlying mapper and data ownership are still shared.

### 3.6 `SessionState` introduces a web-boundary adapter

The legacy web layer depends heavily on Stripes action beans stored directly in session under string keys. `SessionState` centralizes those lookups and returns semantic data such as authenticated user, username, customer profile, and cart snapshot.

Impact:

- Reduces direct dependence on hard-coded Stripes session keys.
- Creates one place to absorb future migration away from action-bean session coupling.
- Makes session access express business intent instead of framework mechanics.

This change is strategically important because session coupling was one of the clearest architectural blockers identified in the analysis documents.

### 3.7 Configuration was prepared for modular package relocation

`applicationContext.xml`, `web.xml`, and `MapperTestContext` now scan package locations that do not yet exist fully in the codebase.

Impact:

- Removes configuration as a blocker for subsequent package moves.
- Makes the runtime and tests tolerant to the target modular namespace.
- Signals that future phases are expected to introduce `*.application`, `*.persistence`, `*.web`, and domain-specific packages.

This is an enabling change. It does not by itself modularize the runtime behavior, but it lowers the friction of the next phases.

### 3.8 Test coverage was realigned around boundaries and characterization

The tests added in this branch are not just unit tests for new code. They are characterization tests that protect the current behavior while the architecture changes.

Impact:

- Validates that new projections preserve current semantics.
- Protects session behavior before web refactors replace direct action-bean access.
- Confirms that services satisfy their new interfaces.
- Reduces refactoring risk in subsequent phases where package moves and dependency inversion will be more invasive.

## 4. Dependencies removed or introduced

### 4.1 External build/runtime dependencies

No new Maven/library dependency was introduced in this branch, and no existing external dependency was removed. The branch changes the internal dependency structure, not the technology stack.

### 4.2 Internal dependencies introduced

The branch introduces these intentional internal dependencies:

- Consumers can now depend on `account.api`, `catalog.api`, `inventory.api`, `cart.api`, and `order.api`.
- `SessionState` depends on `CustomerProfile`, `CartQueryService`, and `CartSnapshot` to expose framework-neutral session data.
- `AccountService` now depends on `CustomerProfile`.
- `CatalogService` now depends on `ProductSummary`, `ItemSnapshot`, and `InventoryStatus`.
- `OrderService` now depends on `InventoryReservationService` and `OrderQueryService` as implemented contracts.

### 4.3 Internal dependencies reduced or better encapsulated

These reductions are partial, not complete:

- Direct semantic dependence on raw session key strings is centralized in `SessionState` instead of being spread to future consumers.
- Boundary callers can target interfaces instead of concrete service implementations.
- Cart and customer data can now cross boundaries as immutable snapshots rather than mutable entity graphs.

### 4.4 Dependencies still present after this phase

Important legacy dependencies remain:

- Web actions still depend directly on concrete services, not on the new APIs.
- Web actions still access action beans in session directly; `SessionState` exists but is not yet widely adopted.
- `CatalogService` still mixes catalog and inventory responsibilities in one implementation.
- `OrderService` still owns both order orchestration and inventory mutation.
- Shared domain entities and mappers still underpin multiple concerns.

## 5. Before vs after explanation of the system structure

### 5.1 Before

Before `phase1-boundaries`, the system structure was essentially:

- `web.actions` handled request flow and session manipulation directly.
- `service` held concrete application services with mixed responsibilities.
- `mapper` exposed persistence directly to services.
- `domain` entities were shared across flows and layers.
- Session state relied on hard-coded Stripes keys and action-bean instances.

Architecturally, this meant:

- Package organization was technical, not modular.
- Boundaries between business capabilities were implicit and weak.
- Catalog, inventory, order, and cart concerns were coupled through shared services and shared entities.
- Future modularization would require changing behavior and structure at the same time.

### 5.2 After

After this phase, the runtime still behaves as a modular monolith-in-transition rather than a fully modularized system, but the structure now includes:

- Explicit domain APIs under `account.api`, `catalog.api`, `inventory.api`, `cart.api`, and `order.api`.
- Immutable boundary DTOs/snapshots for customer, catalog item, product, inventory, and cart data.
- Service classes that implement domain-facing contracts.
- A `shared.web.SessionState` adapter for session-bound state.
- Configuration prepared for domain-specific `application`, `persistence`, and `web` packages.

Architecturally, the system has moved from:

- concrete-service-centered design

to:

- contract-centered transitional design

It has not yet moved to:

- independently organized per-module implementations

That distinction matters. This phase establishes boundaries; it does not complete the internal reorganization behind them.

### 5.3 Structural delta in one sentence

Before: a layered monolith with implicit domain boundaries.  
After: the same layered monolith, but with explicit boundary contracts and adapter points that prepare it for modular decomposition.

## 6. Design decisions made during implementation

### 6.1 Introduce APIs before moving packages

The branch chooses to define contracts first and delay invasive package movement. This is the right sequencing for a legacy system because it stabilizes behavior before structural churn.

Reasoning:

- Lower risk than moving classes and contracts simultaneously.
- Easier to test and review incrementally.
- Keeps rollback cost low.

### 6.2 Use immutable records for boundary data

Java records were chosen for snapshot types and small cross-boundary models.

Reasoning:

- Boundary data should be immutable by default.
- Records reduce boilerplate and make intent obvious.
- They are a better fit than reusing mutable persistence/domain objects.

### 6.3 Keep existing services as implementations of new APIs

Instead of creating parallel service implementations, the branch adapts the existing services.

Reasoning:

- Avoids a disruptive rewrite.
- Preserves current wiring and behavior.
- Concentrates change around boundaries rather than runtime flow.

### 6.4 Centralize session coupling instead of eliminating it immediately

`SessionState` does not remove Stripes session coupling from the system, but it isolates it.

Reasoning:

- Session coupling is deeply embedded in the current web layer.
- An adapter can be introduced safely without rewriting all actions in one step.
- Encapsulation is a necessary precursor to replacement.

### 6.5 Prepare configuration early

The Spring/MyBatis/Stripes configuration was expanded before the package reorganization happens.

Reasoning:

- Prevents configuration edits from being mixed with future behavioral changes.
- Makes upcoming file moves cheaper and less error-prone.
- Keeps test/runtime bootstrap aligned with the intended target architecture.

### 6.6 Add characterization tests around fragile behavior

The branch increases test coverage around services, session behavior, and web actions.

Reasoning:

- Boundary work on a legacy monolith needs safety rails.
- Existing action/session behavior is brittle and should be locked down before larger refactors.
- Tests document current semantics for future maintainers.

## 7. Potential trade-offs or limitations

### 7.1 Boundaries exist, but implementations are still co-located

The new APIs improve structure, but the implementations still live in the legacy `service` package. This means the architecture is cleaner conceptually than it is physically.

Consequence:

- The branch improves modular intent more than runtime separation.

### 7.2 Some contracts still expose legacy domain entities

Not all APIs use snapshots only. Several methods still return `Account`, `Product`, `Item`, `Category`, and `Order`.

Consequence:

- Domain leakage across future module boundaries is reduced, but not eliminated.
- More contract refinement will be needed in later phases.

### 7.3 Inventory is only partially separated

Inventory has dedicated interfaces, but no independent implementation or persistence ownership yet.

Consequence:

- The system names inventory as a module, but does not fully isolate it.

### 7.4 `SessionState` is introduced but not yet broadly adopted

The session abstraction exists, but existing action beans still access session data directly.

Consequence:

- The branch solves the boundary problem structurally, not yet operationally across the whole web layer.

### 7.5 Configuration leads the codebase

The scanning configuration references target package locations that are mostly future-facing.

Consequence:

- This is useful preparation, but it can look ahead of the actual implementation and may confuse readers if not documented.

### 7.6 Service classes temporarily become fatter

Projection and adapter logic were added to existing services.

Consequence:

- The current implementation increases service responsibilities in the short term to reduce migration risk in the medium term.

## 8. Overall architectural evolution

This phase should be understood as a boundary-establishment phase, not a full modularization phase.

What changed architecturally:

- The system now has named module contracts.
- Data crossing future module boundaries now has explicit immutable shapes.
- Session coupling has an adapter point.
- Configuration and tests are prepared for deeper structural change.

What did not change yet:

- Core runtime flows still follow the original Stripes -> service -> mapper model.
- Concrete implementations are still centralized in the legacy package structure.
- Shared domain and persistence models still carry much of the coupling.

The branch therefore represents a controlled transition from an implicit layered monolith to a boundary-aware modular monolith in preparation.
