# Archemides

Archemides is an educational foreign-exchange backtesting engine and quantitative portfolio analytics lab.

> Educational use only. The analytics and simulations are not investment advice, a broker integration, or a claim of regulatory compliance.

## What is available

- Event-driven forex backtesting with signals, simulated orders, positions, stop orders, reconciliation, and transaction history.
- A framework-free quantitative analytics module with:
  - compounded and annualized return;
  - inflation-adjusted real return;
  - annualized volatility;
  - Sharpe and Sortino ratios with explicit economic assumptions;
  - maximum drawdown;
  - historical Value at Risk and Expected Shortfall.
- A validated Spring REST adapter for the analytics use case.
- Actuator health and liveness/readiness endpoints, plus opt-in local metrics and Prometheus endpoints.
- Micrometer observation around every analytics calculation.
- Spring Boot structured ECS JSON logging with bounded platform-thread request handling for the CPU-bound analytics endpoint.

The HTTP surface currently exposes portfolio analytics. The legacy backtest remains available through `ForexBuilder`/`ForexEngine` while its position, execution, and persistence model is modernized.

## Technology

- Java 21 target
- Spring Boot 3.5.16
- Maven 3.9.16 Wrapper and multi-module build
- JPMS encapsulation for the framework-free domain modules
- Spring MVC, Bean Validation, Actuator, Micrometer, and Prometheus
- JUnit 4/JUnit Jupiter, Mockito, Cucumber, and JaCoCo

## Build and run

```bash
./mvnw clean verify
java -jar runner/target/runner-1.0-SNAPSHOT.jar
```

With the default profile, health is available at `http://localhost:8080/actuator/health`. The `local` profile moves all Actuator endpoints to its loopback-only management port described below.

Build the container after packaging the JAR:

```bash
./mvnw -DskipTests package
docker build -t archemides .
docker run --rm -p 8080:8080 archemides
```

## Portfolio analytics API

```bash
curl --request POST http://localhost:8080/api/v1/analytics/performance \
  --header 'Content-Type: application/json' \
  --data '{
    "periodicReturns": [0.10, -0.05, 0.02, -0.01],
    "periodsPerYear": 12,
    "confidenceLevel": 0.95,
    "annualRiskFreeRate": 0.04,
    "annualInflationRate": 0.03,
    "annualTargetReturn": 0.00
  }'
```

`annualTargetReturn` is optional and defaults to zero. Returns and rates are decimal values, so `0.04` means 4%. A simple periodic return must be greater than `-1`.

Example response:

```json
{
  "observations": 4,
  "totalReturn": 0.055241,
  "annualizedReturn": 0.175046275915363,
  "realAnnualizedReturn": 0.140821627102294,
  "annualizedVolatility": 0.2199999999999999,
  "sharpeRatio": {"value": 0.6396141936982363, "status": "DEFINED"},
  "sortinoRatio": {"value": 2.038098661460272, "status": "DEFINED"},
  "maximumDrawdown": 0.05,
  "historicalValueAtRisk": 0.05,
  "expectedShortfall": 0.05
}
```

Run with `--spring.profiles.active=local` to expose metrics and Prometheus for local inspection on the loopback-only management address at `http://127.0.0.1:8081/actuator`. After the first request, its observation is visible at `/actuator/metrics/portfolio.analytics`. The default profile keeps those diagnostic endpoints off the public HTTP surface.

See [MODERNIZATION.md](MODERNIZATION.md) for the architecture decisions, finance research, exact metric semantics, and prioritized roadmap.
