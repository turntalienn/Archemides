# Spring, Finance, and Economics Modernization

## Decision

Archemides should evolve as a finance-grade modular monolith, not as premature microservices. The core calculations and trading rules stay framework-independent; Spring owns the composition root, HTTP adapters, validation, operations, and future infrastructure adapters.

Spring Boot 3.5.16 is the deliberate bridge from the repository's old 3.0.5 baseline. It supports Java 17 through 25 and provides virtual threads, structured logging, Actuator, and Micrometer without combining this work with the separate breaking changes of Spring Boot 4.

## Current module direction

```text
runner (Spring and adapters)
  ├── analytics (pure quantitative domain)
  └── forex (legacy trading engine)
        ├── feed
        └── common
```

The new `analytics` module exports only its API package through JPMS. It has no Spring dependency. `runner` is intentionally the classpath-based Spring adapter: it depends on domain APIs and wires implementations without leaking Spring annotations into quantitative code.

The default runtime exposes only Actuator health and info. The `local` profile exposes metrics and Prometheus on a separate loopback-only management port; production metric publication should still use an authenticated or isolated management interface.

## Patterns implemented

- **Ports and adapters:** `PortfolioAnalytics` is the inbound application port. The controller and Spring configuration are adapters.
- **Strategy:** `TailRiskModel` allows historical simulation to be replaced later by a separately tested model without changing the report use case.
- **Factory:** `PortfolioAnalyticsFactory` is the public construction boundary; implementation packages remain unexported.
- **Decorator:** `ObservedPortfolioAnalytics` adds Micrometer timing and outcome data without coupling the domain to Micrometer.
- **Value objects:** `ReturnSeries`, `EconomicAssumptions`, and `AnalyticsParameters` validate invariants once and are immutable.
- **Dependency inversion:** Spring depends on the quantitative interface; the quantitative module does not depend on Spring.

This is intentionally a small pattern set. Kafka, microservices, distributed CQRS, and event sourcing are not justified by the current single-process workload.

Virtual threads were evaluated but are intentionally not enabled: this endpoint performs CPU-bound calculations and sorting, where bounded platform-thread concurrency is the safer default. They become useful later for high-concurrency blocking I/O adapters, not as a shortcut for compute capacity.

## Metric semantics

For chronological simple returns `r`:

- Total return: `product(1 + r) - 1`.
- Annualized return: `(1 + totalReturn)^(periodsPerYear / observations) - 1`.
- Real return: `(1 + nominalAnnualReturn) / (1 + annualInflation) - 1`, using the Fisher relation.
- Volatility: sample standard deviation multiplied by `sqrt(periodsPerYear)`.
- Sharpe: mean return above the geometrically converted periodic risk-free rate, divided by sample volatility and annualized.
- Sortino: mean return above an explicit periodic target, divided by target downside deviation and annualized.
- Maximum drawdown: largest peak-to-trough decline in a compounded wealth index.
- Historical VaR: nearest-rank lower-tail loss at the requested confidence.
- Expected Shortfall: mean loss of the worst `ceil((1 - confidence) * observations)` returns.

Drawdown, VaR, and Expected Shortfall are returned as non-negative loss magnitudes. Sharpe and Sortino use a status-bearing value: a zero risk denominator returns `UNDEFINED_ZERO_DENOMINATOR` with no numeric value instead of a misleading zero or infinity. Public numeric results are `BigDecimal`; non-algebraic power and square-root operations are deterministically converted with DECIMAL64 precision.

Historical simulation is useful for an educational first model, but small samples make tail estimates unstable. The endpoint accepts a confidence level rather than implying Basel compliance. Basel's market-risk framework uses one-tailed 97.5% Expected Shortfall with many additional requirements that are not implemented here.

## Researched next changes

### 1. Position aggregate and append-only ledger

The current engine loses important financial invariants: equity is not used for sizing, quantity updates and average-cost math are fragile, and closed positions are removed rather than retained as audit history.

The next vertical slice should introduce:

```text
Position.open(fill)
position.apply(fill)
position.markToMarket(price)
position.close(reason, time)
```

It should enforce instrument matching, positive quantities, weighted average cost, partial reductions, exact closure, duplicate `FillId` suppression, and immutable audit events. Persist `OrderIntent`, `Execution`, `PositionLot`, `CashPosting`, `ValuationSnapshot`, and `RiskSnapshot` as an append-only journal.

### 2. Realistic execution strategies

Replace fill-at-candle-close with explicit strategies:

- deterministic mid-price fills for unit tests;
- bid/ask spread and commission;
- seeded slippage and latency for reproducible experiments;
- later, partial fills and market-impact models.

Every order needs a stable client order ID before retries or a live broker adapter are safe.

### 3. Risk-budget policies

Replace the hard-coded quantity of `10` with stop-distance or volatility risk budgeting. Add named risk rules returning auditable decisions for exposure, duplicate intent, maximum drawdown, and daily-loss limits. VaR and Expected Shortfall should report risk; they should not silently become order gates without an explicit policy.

### 4. Spring Modulith and Spring Batch

After the trade aggregates are corrected:

- Spring Modulith 1.4.x can verify module boundaries and provide a persistent publication registry for local transactional events.
- Spring Batch 5.2.x can model reproducible, restartable backtests as dataset validation → simulation → valuation → risk calculation → report publication.

Neither technology is a substitute for broker idempotency or a complete trading ledger.

### 5. Point-in-time economics

Macro strategies must not read revised data that was unavailable at simulated time. Store `observationDate`, `releaseDate`, and `vintageDate`, then source historical vintages from ALFRED/FRED and ECB SDMX. This enables honest carry, inflation, and policy-regime research without look-ahead leakage.

## Primary sources

- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Spring Boot observability](https://docs.spring.io/spring-boot/3.5/reference/actuator/observability.html)
- [Spring Boot virtual threads](https://docs.spring.io/spring-boot/3.5/reference/features/task-execution-and-scheduling.html)
- [Spring Boot structured logging](https://docs.spring.io/spring-boot/3.5/reference/features/logging.html#features.logging.structured)
- [Spring Modulith application events and publication registry](https://docs.spring.io/spring-modulith/reference/1.4/events.html)
- [Spring Batch job restartability](https://docs.spring.io/spring-batch/reference/job/configuring-job.html)
- [Spring Data JDBC aggregate design](https://docs.spring.io/spring-data/relational/reference/jdbc/domain-driven-design.html)
- [BIS FX execution algorithms and transaction costs](https://www.bis.org/publ/mktc13.pdf)
- [Basel Expected Shortfall framework](https://www.bis.org/basel_framework/chapter/MAR/33.htm)
- [ALFRED vintage-data API](https://fred.stlouisfed.org/docs/api/fred/alfred.html)
- [ECB Data Portal API](https://data.ecb.europa.eu/help/api/overview)
