# StormAPI — Full Engineering Audit

**Audit date:** 2026-07-21  
**Scope:** Backend, frontend, database, REST/WebSocket APIs, engine/concurrency
code, tests, security, infrastructure, documentation, and Phases 1–17 of the
implementation plan.  
**Change policy:** No existing file was modified. This report is the sole
created file.

---

## Executive verdict

StormAPI is an ambitious full-stack performance-testing prototype with an
above-average initial architecture and unusually good visual polish. It goes
well beyond a normal student CRUD project: six test engines, Java virtual
threads, real-time WebSockets, persistence, charts, collections, scenarios,
assertions, data-driven execution, authentication, and exports are all present.

It is **not production-ready** and should not be deployed publicly as-is.
The highest-risk faults are substantive, rather than cosmetic:

1. There is no user ownership or authorization for resources.
2. Target URL handling permits server-side request forgery (SSRF).
3. WebSocket test/log topics are publicly readable.
4. Test lifecycle, request-log capture, and RPS logic contain concurrency bugs.
5. Advanced test-engine conclusions and some result charts are not reliable.
6. Docker/production schema/CI delivery is incomplete.

The strongest portfolio presentation is an honest one: a sophisticated,
visually polished prototype with clear next steps—not a production-grade
benchmarking service.

### Evidence collected

- Backend tests: **386 tests, 0 failures, 0 errors**.
- Frontend tests: **196 tests in 25 files passed** through Vitest.
- Frontend production build passed, with a **1.14 MB uncompressed main chunk**
  (about 340 KB gzip) and a Vite chunk-size warning.
- All backend controllers, entities, repositories, execution engines,
  security/authentication code, WebSocket implementation, frontend routes,
  hooks, components, exports, infrastructure, and roadmap sections were read.

### Audit boundary

This is a source-level and build/test audit. It did not run a real 10,000-user
benchmark, OAuth against live providers, a fresh PostgreSQL deployment, or a
public network penetration test. Those cases are identified as unverified,
rather than assumed to work.

---

## Architecture understanding

~~~mermaid
flowchart LR
  UI["React SPA
pages · hooks · CSS modules"] --> REST["REST API /api/*"]
  UI --> WS["SockJS/STOMP /ws"]
  REST --> SEC["Spring Security
JWT cookie + CSRF"]
  SEC --> CTRL["Feature controllers"]
  CTRL --> ORCH["TestOrchestrator"]
  CTRL --> FEATURES["Scenario · collection · export services"]
  ORCH --> FACTORY["TestEngineFactory"]
  FACTORY --> ENGINE["Load · Stress · Spike
Soak · Breakpoint · Scalability"]
  ENGINE --> VU["Virtual-user virtual threads"]
  VU --> HTTP["Java HttpClient + executor"]
  VU --> METRICS["MetricsCollector
LongAdder + HdrHistogram"]
  VU --> LOGS["WebSocket / DB log capture"]
  METRICS --> SNAP["One-second snapshots"]
  SNAP --> DB["JPA: H2 dev / PostgreSQL prod"]
  LOGS --> DB
  METRICS --> WS
  ORCH --> WS
~~~

### Runtime flow

1. The Test Builder posts a test configuration.
2. The orchestrator creates a RUNNING result/configuration, puts an in-memory
   handle into a map, and launches a virtual thread.
3. The selected engine creates virtual-user threads. Each performs a closed-loop
   sequence: request, record result, think time, repeat.
4. Metrics, WebSocket logs, and DB log capture all receive each request result.
5. A scheduled virtual-thread executor snapshots/broadcasts metrics every second.
6. Finalization flushes snapshots/results/logs and sends a terminal event.

### Design strengths

- Feature-sliced packages are better than a flat controller/service/repository
  hierarchy.
- Test configuration is correctly separate from repeatable test results.
- Factory, template-method, ramp-up strategy, and think-time strategy patterns
  are appropriate.
- Java 21 virtual threads are used in the actual execution path.
- DTOs/mappers avoid direct entity exposure on the major APIs.
- HdrHistogram and LongAdder are credible starting primitives.
- The React application has coherent feature components, shared tokens, CSS
  modules, API modules, and useful custom hooks.

### Architectural weaknesses

The visible architecture is stronger than its invariants. There is no durable
active-run guarantee, no tenant boundary, no outbound-target security policy,
no cluster/distributed-worker design, and no reliable interval-metric model.
Those are fundamental properties for a performance-testing service.

---

## Phase 1–17 roadmap validation

| Phase | Correctly implemented | Partial / missing | Completion | Quality |
|---|---|---|---:|---:|
| 1. Scaffolding | Maven, Vite, health, H2, dev profile | Compose references missing Dockerfiles | 65% | 5.5/10 |
| 2. Architecture | Feature slices, envelope, validation, logging | Comments/contracts sometimes diverge | 75% | 6.5/10 |
| 3. Domain/database | Core entities, relations, indexes, repository tests | No ownership, migrations, locking, order constraints | 65% | 5.5/10 |
| 4. HTTP engine | Client, virtual user, retries, WireMock tests | Bodies buffered; bytes/error containment wrong | 65% | 6.0/10 |
| 5. Metrics | Counters, histogram, snapshots | RPS rollover and interval semantics incorrect | 50% | 4.5/10 |
| 6. Orchestration | Engines, factory, virtual thread, persistence path | Non-atomic start and lifecycle defects | 60% | 5.0/10 |
| 7. Advanced engines | All six classes exist | Stage analysis/persisted scalability curve unreliable | 45% | 4.0/10 |
| 8. REST API | Broad DTO/controller surface, OpenAPI | No update route, authz, safe sorts/delete semantics | 60% | 5.5/10 |
| 9. WebSocket | STOMP/SockJS, events, frontend hook | No authz, cluster model, or backpressure | 50% | 5.0/10 |
| 10. Design system | Strong shell, themes, reusable components | No route splitting/error boundary | 80% | 7.5/10 |
| 11. Dashboard | KPIs, recent tests, real API | Global/unweighted/N+1-prone aggregation | 65% | 6.0/10 |
| 12. Test Builder | Four steps, dynamic config, polished UI | Validation parity/default errors | 65% | 6.0/10 |
| 13. Live monitor | Charts, logs, stop flow | Event timing and data reliability weak | 55% | 5.0/10 |
| 14. Results/history | Charts/history/comparison UI | Sort/filter/comparison/scalability defects | 55% | 5.0/10 |
| 15. Collections/scenarios | CRUD, extraction, templates | Ownership/order/assertion semantics absent | 60% | 5.5/10 |
| 16. Assertions/data driven | Five assertion/readers/upload UI | Headers, limits, true failure behavior missing | 50% | 4.5/10 |
| 17. Exports | JSON/CSV/HTML | No PDF; buffered/incomplete reporting | 50% | 4.0/10 |

### Phase 18–20 status

Phase 18 is partially present: test volume is good, but there is no frontend
test script, coverage report/threshold, CI, PostgreSQL/Testcontainers suite, or
meaningful end-to-end coverage for security, SSRF, authorization, scenario
execution, and concurrency races.

Phases 19–20 are incomplete. A production Spring profile exists, but there are
no Dockerfiles, Nginx config, production Compose, database migrations, GitHub
Actions, architecture/API/setup/deployment docs, or deployment artifacts.

---

## Backend and engine audit

### Test orchestration

The start path has a check-then-act race:

    if (runningTests.containsKey(configId)) { ... }
    ...
    runningTests.put(configId, ...)

Two simultaneous starts for the same config can both pass the check, create two
RUNNING results, and start two engines. The latter map entry overwrites the
former, so stop only controls one run. This is a **P0 correctness defect**.

Further lifecycle defects:

- Start obtains a result ID then returns the “latest” result instead of that
  exact run; races can return the wrong result.
- TEST_STOPPED is broadcast before durable result persistence. The frontend can
  navigate/disconnect before TEST_CANCELLED and final metrics exist.
- Persistence failures are caught and logged, then hidden. A test can appear
  complete with missing snapshots/results/logs.
- Cancelling a run resets the configuration to CREATED while its result remains
  CANCELLED, so dashboard/history state is inconsistent.
- The snapshot ArrayList is written by the timer and finalized elsewhere without
  a clean synchronized ownership boundary.

### Virtual users and load model

The model is closed-loop, not arrival-rate/open-loop: a user issues the next
request only after the prior response and optional think time. That is valid but
must be documented because it is not equivalent to k6/Gatling arrival-rate
testing.

User threads are held in CopyOnWriteArrayList. Each add/remove copies the entire
array, leading to O(n²) copying/allocation for dynamic engines. That is
inappropriate for a product with a 10,000-user API maximum and a “100K+”
marketing claim.

Linear ramp-up starts the first user immediately and sleeps only between users.
For a one-user, 60-second ramp the user starts immediately and sustain becomes
zero; more generally the final user starts before ramp-up ends. The user-visible
meaning of ramp-up is not implemented exactly.

### HTTP executor

The normal load executor uses a string body handler, fully buffering and
decoding every response even though its documentation says bodies are discarded.
It stores character length rather than actual response bytes.

Impact:

- Large responses can exhaust heap under load.
- Response-size metrics are inaccurate for multibyte bodies.
- The tool measures its own body buffering, not only target behavior.

Other defects:

- URI/header construction can throw IllegalArgumentException outside the
  executor’s catch paths; the virtual user exits without a recorded failure.
- Retried requests are not counted individually, so outbound traffic can exceed
  totalRequests.
- All non-2xx responses are retried, including permanent client errors.
- Async execution uses the common ForkJoin pool, contradicting the virtual
  thread approach.
- Scenario execution creates clients per run and does not close them.
- Redirect policy, HTTP/2, proxy, TLS policy, and connection caps are absent or
  hard-coded.

### Metrics correctness

This is the project’s central technical risk because the product claims to
measure performance.

#### Throughput tracker

Concurrent writers reset the same LongAdder bucket while other writers
increment it. Bucket rollover is not atomic.

The stale check can never run:

    previousSecond = currentSecond - 1
    currentSecond - previousSecond == 1

Old bucket data can be returned after inactivity.

#### Percentiles

All histogram recordings lock one ReentrantLock. Under high RPS, virtual users
serialize latency writes through that lock. HdrHistogram is appropriate; one
shared lock for every request is not.

The histogram clips values above 30 seconds while configuration permits 60-second
timeouts. P95/P99 hide the real duration of 30–60 second failures.

#### Snapshots

Several fields presented as one-second values are cumulative since test start:
average latency, error rate, and P95. Timeline charts can imply interval
behavior while hiding short failure bursts or late degradation.

### Advanced engines

| Engine | Audit result |
|---|---|
| Load | Basic run path exists; ramp/duration semantics are inaccurate. |
| Stress | Uses aggregate rather than stage-local conclusions. |
| Spike | Phase allocation for small durations is weak; recovery uses cumulative latency. |
| Soak | Cumulative averages distort late degradation detection. |
| Breakpoint | Global P95/error state contaminates binary-search refinement. |
| Scalability | Computes step deltas but cumulative latency/error; actual curve is discarded. |

Scalability points are stored only in an in-memory analysis object. They are not
represented in TestResult, DTOs, exports, or the database. The frontend displays
generic snapshots as “Scalability Curve,” which is not the calculated curve.

### Request logs

Two log paths violate their own bounds:

- The WebSocket broadcaster maintains deque and size independently. Offer/poll
  and increment/decrement are not atomic; size can become negative and evictions
  can be bypassed.
- The DB persister uses volatile int count rather than an atomic reservation.
  Concurrent callers can exceed the supposed 10,000 log cap and lose increments.
- The first captured logs are retained, not a representative sample. Charts
  derived from them may not represent the full run.
- Error strings can exceed the database column’s default length and cause batch
  failures.
- Grouping Java collections for saveAll does not ensure JDBC batching without
  Hibernate batch configuration.

---

## Database audit

### Strengths

- TestConfig/TestResult supports reruns.
- Metric snapshots have a useful result/timestamp index.
- Request-log indexes support result and success filtering.
- Result child cascades are conceptually sensible.

### Material gaps

1. **No owner ID:** tests, results, collections, scenarios, metrics, and logs are
   not user-owned.
2. **No optimistic locking:** concurrent edits/reorders silently overwrite.
3. **Ordering integrity is missing:** stepOrder/sortOrder lack unique constraints
   and endpoints lack explicit order mapping.
4. **No migrations:** production uses ddl-auto validate but has no Flyway or
   Liquibase history; fresh PostgreSQL cannot obtain a schema.
5. **Hidden lazy loads:** mappers use lazy collections, creating N+1 behavior
   and dependence on Open Session in View.
6. **Dashboard aggregation is unweighted:** a one-request run has the same
   influence as a million-request run.
7. **No PostgreSQL integration test:** H2 is not proof of production behavior.
8. **No retention/archival:** snapshots/results/logs grow indefinitely.

### Scale concerns

- Snapshot endpoint is unpaginated; a 24-hour test can return approximately
  86,400 rows at once.
- Export endpoints build whole byte arrays in memory rather than stream.
- Ordered request-log queries would benefit from a result/timestamp index.
- Request-log sampling/truncation is not surfaced in result metadata.

---

## Security audit

### P0 — authorization does not exist

Most API routes require an authenticated user, but no route validates that the
user owns the requested resource. Because no owner relationship is stored, any
authenticated user can access another user’s tests, target URLs, headers/bodies,
results, metrics, logs, exports, collections, and scenarios by ID.

### P0 — WebSocket data is public

The WebSocket endpoint is permitted without authentication and accepts wildcard
origins. There is no JWT handshake validation and no authorization for
subscriptions. Anyone who guesses a test ID can subscribe to metrics, logs, and
events.

### P0 — SSRF / traffic amplification

URL validation accepts syntactically valid HTTP(S) hosts, including localhost,
private addresses, link-local addresses, cloud metadata endpoints, and internal
DNS names. The server then sends user-controlled methods, headers, and bodies to
those targets. This enables SSRF, internal scanning, metadata access, and
outbound traffic abuse.

### P0 — known default JWT secret

The shared application configuration provides a fallback signing secret. If a
production deployment omits JWT_SECRET, someone with repository knowledge can
forge tokens for existing users. Production must fail startup without a secure
external secret.

### Major findings

- Local login/register cookies do not call setSecure; the cookie-secure setting
  is honored by OAuth success handling only.
- Auth request records have no validation annotations, so @Valid performs no
  meaningful credential/profile validation.
- Registration has a check-then-insert race and can return generic 500 errors.
- Login/register error envelopes are returned with HTTP 200 instead of 401/400.
- JWTs lack issuer/audience, key rotation, revocation, logout revocation, user
  ID/role claims; each request queries the database by email.
- CSRF eager token generation likely reads the wrong request attribute key.
- Actuator health details and Swagger are public in production.
- anyRequest().permitAll() turns future non-API routes public by default.
- Full URIs, including possible query-string secrets, are logged.
- Data-driven row data may log/return credentials or tokens.
- Jackson configuration disables unknown-property rejection while comments claim
  the opposite.

### Required security order

1. Add owner relations and owner-scoped queries everywhere.
2. Authenticate WebSocket handshake/subscriptions and authorize topic access.
3. Add an outbound target policy: deny private/loopback/link-local/metadata
   ranges, handle DNS and redirects safely, and require quotas/allow-lists.
4. Remove secret fallbacks, enforce secure cookies, validate auth DTOs, and add
   security tests.
5. Add audit trails, rate/concurrency limits, maximum duration, and target
   approval controls.

---

## REST API audit

| Family | Reviewed operations | Main outcome |
|---|---|---|
| Tests | create/list/get/result/results/start/stop/rerun/delete | Start race; no update endpoint; invalid sorting; 204 bodies |
| Metrics | snapshots/logs/status distribution/histogram | Snapshots unpaged; chart data sampled/incomplete; no ownership |
| Results | compare | Endpoint expects result IDs; UI supplies test config IDs |
| Dashboard | stats | Works structurally; global/N+1/unweighted values |
| Export | JSON/CSV/HTML | Downloads work; whole payload buffered; no PDF |
| Collections | CRUD, endpoint CRUD, reorder, duplicate | Useful surface; authorization/order/lazy-loading defects |
| Scenarios | CRUD, steps, reorder, execute | Basic chaining; assertion and client-lifecycle failures |
| Data driven | execute | Synchronous and uncapped; header substitution fails |
| Auth | register/login/status/me/logout | Surface exists; validation/status/cookie security weak |

### Confirmed API defects

- DELETE test/collection/scenario endpoints return an ApiResponse body with HTTP
  204. A 204 response must not contain a body.
- History sends lastAvgResponseTimeMs, lastErrorRate, and totalRuns as Spring
  sort fields. These are DTO fields, not TestConfig properties, and sorting
  produces a backend PropertyReferenceException/500.
- History offers ENDURANCE though backend has SOAK; it omits Scalability.
- History search is only frontend state; it is never sent and has no backend API.
- Comparison sends TestSummaryResponse IDs (configuration IDs), but the compare
  endpoint loads TestResult IDs.
- README claims PDF/OpenHTMLtoPDF/export endpoint behavior not implemented in
  source.

---

## Scenarios, assertions, and data-driven execution

### Existing feature strength

Collections/scenarios, JSON extraction, template replacement, five assertion
classes, CSV/JSON readers, data upload, and an assertion builder are real
features—not placeholders.

### Confirmed defects

1. Assertion failures do not make scenario steps fail. A 2xx response with
   failed assertions still succeeds, so failFast and overall scenario success can
   report false positives.
2. Scenario execution allocates an HTTP client and does not close it.
3. Scenario and data-driven execution run synchronously in the request thread.
   Large datasets/timeouts can occupy a servlet request for a long time.
4. Data-driven code claims to substitute URL, headers, and body but copies
   headers unchanged. Header placeholders remain unresolved.
5. Empty scenarios vacuously make all data rows pass.
6. CSV parsing lacks robust size limits, duplicate-header validation, malformed
   quote behavior, and large-input protection.
7. Assertion operator values are accepted but ignored.
8. Response-time assertion expected values are insufficiently validated.
9. Extraction failure becomes empty string rather than explicit failure state.
10. Resolved scenario URLs are not revalidated against a target policy.

---

## Frontend audit

### What is strong

The visual system is a genuine strength. The landing page/application shell has
good typography, spacing, hierarchy, dark styling, calls to action, and polished
chart/card presentation. Components and CSS tokens are coherent. Accessibility
effort is visible through labels, skip navigation, alerts, keyboard handling on
test type cards, and theme support.

### Defects

- All routes are eagerly imported; the main bundle is oversized.
- No React error boundary exists.
- The WebSocket hook claims exponential backoff but uses a fixed two-second
  reconnect delay; callback dependencies can be stale; old test state is not
  cleared on test ID change; deactivation is not awaited.
- An early stop event can make live UI leave before persistence.
- Results page renders generic snapshots as a scalability curve for all tests.
- History loading errors are stored but not shown.
- Axios rejects API errors as plain objects; callers that use instanceof Error
  lose server error messages.
- Builder permits rampUp equal to duration although backend rejects it.
- Builder does not enforce request bodies for POST/PUT/PATCH like backend does.
- Scalability defaults are invalid: 10 users and 50-user step size.
- Preselected type can skip the default-setting effect.
- Landing claims seven test types and 100K+ users, while implementation has six
  types and API validation caps users at 10,000.
- Regular scenario execution does not use assertions despite landing claims.
- Settings is substantially less complete than core screens.
- Reduced-motion support is incomplete.

### UX scoring

| Area | Score | Assessment |
|---|---:|---|
| Visual polish | 7.5/10 | Strong portfolio-level presentation |
| Information hierarchy | 7.0/10 | Cards/charts/layout generally clear |
| Interaction correctness | 4.0/10 | Builder/history/comparison/event defects are visible |
| Accessibility | 6.0/10 | Good basics; needs a full focus/modal/motion audit |
| Product trust | 3.0/10 | Incorrect metrics and claims are severe in this category |

---

## Testing audit

### Positive evidence

- 62 backend test classes exercise engines, mappers, repositories, metrics,
  controllers, and WebSocket components.
- 25 frontend test files exercise components, hooks, pages, schemas, and client.
- WireMock is used for parts of HTTP/virtual-user behavior.
- MVC and JPA slice tests exist.

### Missing tests

- JWT/cookie/CSRF/OAuth behavior with security filters.
- Cross-user ownership/authorization.
- Unauthorized WebSocket handshake and subscription.
- SSRF/private-network target rejection.
- Concurrent start/stop/rerun lifecycle behavior.
- Request-log boundedness race.
- RPS time-boundary behavior under concurrent writers.
- Scenario assertions/client closure.
- Data-driven header substitutions/empty scenarios.
- PostgreSQL migrations/schema compatibility.
- Full browser E2E flow: create → run → live monitor → stored result.
- Coverage reporting and enforced thresholds.

### Tooling gap

Vitest is installed but frontend package scripts omit test. The roadmap’s required
npm run test command therefore fails. There is no CI executing test/build paths.

---

## Infrastructure and production readiness

### Present

- Docker Compose file
- H2 dev profile and PostgreSQL production profile
- PostgreSQL runtime dependency
- Actuator health dependency

### Absent or broken

- Backend Dockerfile
- Frontend Dockerfile
- Nginx configuration
- Production Compose
- Flyway/Liquibase migrations
- GitHub Actions CI/deploy workflows
- Environment example
- Architecture/API/setup/deployment docs
- PostgreSQL deployment smoke test

The Compose file names backend/frontend Dockerfiles that do not exist. Production
uses ddl-auto validate, but without migrations a new PostgreSQL database cannot
be initialized.

**Conclusion:** local demo is viable; public production deployment is blocked by
security, lifecycle, metric, and delivery failures.

---

## Portfolio and competitive assessment

This project stands out among typical B.Tech projects. It shows ambition in real
time systems, virtual threads, performance metrics, scenarios, and polished UI.
It can help for backend/full-stack interviews once the README is made accurate
and P0 findings are fixed.

It is not yet competitive with established tools:

| Product | Relative position |
|---|---|
| Postman / Insomnia | Focused idea, but far behind in auth, collections, request correctness, safety, and reliability |
| Grafana | Respectable presentation, far behind in data semantics, operations, and observability |
| JMeter | Better UX, much less mature protocol/load-engine correctness |
| k6 Cloud | Attractive concept, but not close on distributed execution, arrival-rate models, measurement validity, or security |

Experienced reviewers are likely to see a capable developer with modern-tool
knowledge and strong ambition, but not yet a developer consistently applying
production invariants under concurrency, adversarial input, and multi-tenant
access.

- Best component maturity: strong mid-level exploration.
- Overall implementation maturity: early-to-mid professional engineer.
- Staff-level: not demonstrated.
- Visible practice equivalent: roughly 12–18 months of focused
  production-style engineering, with unusually ambitious scope.

---

## Scorecard

| Dimension | Score / 10 |
|---|---:|
| Architecture | 5.5 |
| Backend | 5.0 |
| Frontend | 6.5 |
| Database | 4.5 |
| Performance | 3.0 |
| Scalability | 2.5 |
| Concurrency | 3.0 |
| Security | 2.0 |
| UI | 7.5 |
| UX | 5.0 |
| Code quality | 5.5 |
| Maintainability | 5.0 |
| Documentation | 3.5 |
| Testing | 5.0 |
| Production readiness | 1.5 |
| Portfolio value | 6.5 |
| Originality | 7.0 |
| Engineering difficulty | 7.5 |

---

## Prioritized remediation roadmap

### P0 — before any public deployment

1. Add owner IDs and owner-scoped authorization to every resource/API/WebSocket
   topic.
2. Block SSRF targets, implement DNS/redirect controls, and add user/target
   quotas.
3. Remove JWT fallbacks; enforce secrets, secure cookies, validated auth, and
   tested CSRF.
4. Make start/stop active state atomic and durable; emit terminal events after
   durable persistence.
5. Replace log capture with atomic bounded queues/reservations and disclose
   sampling/truncation.
6. Redesign RPS/interval metrics and histogram recording before claiming
   performance results.
7. Add migrations, Dockerfiles, production Compose/Nginx, CI, and PostgreSQL
   smoke tests.

### P1 — product correctness

1. Rework engines around explicit deadlines and stage-local measurements.
2. Persist/render the actual scalability curve.
3. Make assertion failures fail scenarios; close clients; move long data-driven
   work behind cancellable jobs.
4. Fix history type/sort/search, comparison IDs, builder defaults, validation
   parity, and terminal event behavior.
5. Eliminate N+1/lazy mapping issues and use weighted dashboard metrics.
6. Add unique order constraints and optimistic locking.

### P2 — quality and performance

1. Replace CopyOnWrite user tracking and benchmark the engine honestly.
2. Stream exports and paginate snapshots.
3. Add route splitting, error boundaries, robust API error display, and complete
   reduced-motion support.
4. Make HTTP behavior configurable and document the closed-loop model.

### P3 — future platform work

1. Distributed workers, job queues, durable recovery.
2. Team RBAC, audit trails, encrypted secrets, scheduled suites.
3. Open-loop arrival-rate models, scripting, protocol plugins.
4. OpenTelemetry/Prometheus, SLOs, traces, alerts, and generator observability.

---

## Final conclusion

StormAPI’s best next step is not another feature. It is restoring trust in the
core: authorization, SSRF controls, authenticated WebSockets, atomic execution
state, bounded logs, correct metrics, database migrations, and reproducible
delivery.

After those changes, the existing UI, feature breadth, and architecture can
become a differentiated and credible portfolio project. Until then, it should be
marketed as a sophisticated prototype, not a production-ready API
performance-testing platform.

