# REST scenarios

A [Bruno](https://www.usebruno.com) collection that drives the bike-leasing process end to end against
a **running** service — either variant, they expose the same API. Domain endpoints trigger the business
actions; the CIB seven `/engine-rest` API completes user tasks and fires timer jobs, so the whole flow
runs without real 14-day waits. CI runs the collection against both variants on every pull request.

```bash
npx --yes @usebruno/cli@4.0.0 run . --env local -r
```

| Folder | Scenario |
|---|---|
| `01-happy-path` | submit → sign the contract → report the handover → active lease |
| `02-escalation` | the signature deadline passes and the application is rejected |
| `03-abort` | the customer withdraws after signing; the bike order is cancelled and the contract compensated |
| `04-not-solvent` | the DMN rejects the applicant |
| `05-bike-unavailable` | the bike is out of stock and the customer picks an alternative |
| `06-incident-demo` | a failing external task runs out of retries and raises an incident |
| `07-list-and-inbox` | the list and task-inbox endpoints |
| `08-alternative-declined` | the bike is out of stock and no alternative is found; contract and policy are compensated, no order is cancelled |
| `09-invalid-request` | invalid input is refused with a 400: a request without income, an accepted alternative without a bike |
| `10-alternative-via-tasklist` | the bike is out of stock and the alternative is chosen the way the Tasklist form does it |

The service tasks are completed asynchronously by the workers, so the requests poll until the process
has reached the state they need ([ADR-0012](../docs/adr/0012-polling-for-eventual-consistency-in-e2e-tests.md)).

## 📮 Start a case by hand

`POST http://localhost:8080/api/bike-leasing`

```json
{ "customerName": "…", "email": "…", "age": 35, "monthlyNetIncome": 3500, "bikeId": "BIKE-900", "bikeModel": "Gravel Explorer 900" }
```

`age` and `monthlyNetIncome` feed the `checkCreditRating` DMN. `bikeId` is the *only* bike attribute the
engine ever carries: the descriptive `bikeModel` lives in a separate **bike portfolio** aggregate (its
own `bike_portfolio` table) — never as a process variable — and `GET /api/bike-leasing/{id}` resolves it
back from there. Availability is decided by the `BikeDealerPort` outbound adapter, whose small
out-of-stock deny-list drives the branch.

The read endpoints round out the API: `GET /api/bike-leasing` lists applications (paged),
`GET /api/bikes` returns the bike portfolio and `GET /api/tasks/clarify-alternative` lists the open
clarification tasks. Every endpoint is described by the committed, drift-gated
[`openapi/openapi.json`](../openapi/openapi.json).

## 🔁 Two ways to complete a user task

If the requested bike is out of stock, the `Clarify alternative with customer` user task shows a
deliberate contrast:

- **Reaches the domain either way:** a client calls `POST /api/bike-leasing/{id}/clarify-alternative`, or
  a human completes the Camunda Form in the Tasklist. Both hand the chosen `bikeId` to the process, and
  the re-order reads it and persists it on the application. The REST call additionally registers the
  bike's model in the portfolio.
- **Counter-example:** `clarify-return` in `cancel-bike-order.bpmn` is completed via the Camunda Form or
  `/engine-rest` only. It never touches the domain, so its data lands only in process variables (see the
  `bpmn:documentation` on each task).

## 🚨 Incident demo

To teach **transaction boundaries, retries and incidents**, submit a request for the poison bike
`BIKE-FAIL`: the simulated dealer outage fails the *Order bike from dealer* external task, its retries
count down to zero (3 attempts, per the adapter's retry policy), and an **incident** appears in the
Cockpit to analyze and retry. `06-incident-demo/` has the requests ready to run.
