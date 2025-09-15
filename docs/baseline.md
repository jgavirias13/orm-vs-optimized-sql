# Baseline Scenarios — Performance Comparison

This document captures performance metrics for the **three implementations**:
- **Scenario 1**: Unoptimized ORM
- **Scenario 2**: Optimized ORM
- **Scenario 3**: Native SQL

---

## 🔎 Summary Table

| Scenario | Business Question                               | ORM Unoptimized (time / #queries) | ORM Optimized (time / #queries) | Native SQL (time / #queries) |
|----------|-------------------------------------------------|-----------------------------------|---------------------------------|------------------------------|
| A        | Monthly summary per account (orig + COP totals) |                                   |                                 |                              |
| B        | Top-N counterparties of the month (in COP)      |                                   |                                 |                              |
| C        | Declaration detail (line by line)               |                                   |                                 |                              |
| D        | Consolidated monthly totals in COP              |                                   |                                 |                              |

> Fill in headline metrics here (execution time in ms/s, total queries executed).  
> Use the detailed sections below for full breakdown (plans, rows read, payload size, etc.).

---

## Scenario A — Monthly Summary per Account

**Parameters**
- `companyId = ?`
- `period = YYYY-MM`
- `accounts = all company accounts`

**Expected Result**  
Totals per original currency and totals converted to COP for each account.

**Metrics Comparison**

| Metric               | ORM Unoptimized | ORM Optimized | Native SQL |
|-----------------------|-----------------|---------------|------------|
| Total execution time  |                 |               |            |
| # of queries executed |                 |               |            |
| Representative plan   |                 |               |            |
| Rows read / returned  |                 |               |            |
| JSON payload size     |                 |               |            |

---

## Scenario B — Top Counterparties of the Month

**Parameters**
- `companyId = ?`
- `period = YYYY-MM`
- `N = top counterparties`

**Expected Result**  
List of counterparties with totals in COP, ordered by amount.

**Metrics Comparison**

| Metric               | ORM Unoptimized | ORM Optimized | Native SQL |
|-----------------------|-----------------|---------------|------------|
| Total execution time  |                 |               |            |
| # of queries executed |                 |               |            |
| Representative plan   |                 |               |            |
| Rows read / returned  |                 |               |            |
| JSON payload size     |                 |               |            |

---

## Scenario C — Auditable Declaration Detail

**Parameters**
- `companyId = ?`
- `period = YYYY-MM`
- `pageSize = ?`

**Expected Result**  
Detailed list of movements with original amount, applied exchange rate, and converted COP amount.

**Metrics Comparison**

| Metric               | ORM Unoptimized | ORM Optimized | Native SQL |
|-----------------------|-----------------|---------------|------------|
| Total execution time  |                 |               |            |
| # of queries executed |                 |               |            |
| Representative plan   |                 |               |            |
| Rows read / returned  |                 |               |            |
| JSON payload size     |                 |               |            |

---

## Scenario D — Consolidated Monthly Summary in COP

**Parameters**
- `companyId = ?`
- `period = YYYY-MM`

**Expected Result**
- Totals in COP per account
- Totals per original currency
- Overall company total in COP

**Metrics Comparison**

| Metric               | ORM Unoptimized | ORM Optimized | Native SQL |
|-----------------------|-----------------|---------------|------------|
| Total execution time  |                 |               |            |
| # of queries executed |                 |               |            |
| Representative plan   |                 |               |            |
| Rows read / returned  |                 |               |            |
| JSON payload size     |                 |               |            |

---

> Fill in values as you test each scenario. This layout makes it easy to directly compare results and highlight the gains from ORM tuning and SQL optimization.