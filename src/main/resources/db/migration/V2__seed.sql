-- V2__seed.sql
select setseed(0.42); -- make random() deterministic for reproducible seeds

-- =========================
-- currencies
-- =========================
insert into currency(code, name)
values ('USD', 'US Dollar'),
       ('EUR', 'Euro'),
       ('COP', 'Colombian Peso')
on conflict (code) do nothing;

-- Companies
insert into company(legal_name, tax_id, country)
values ('Acme S.A.', '900123456', 'CO'),
       ('Globex LLC', 'US1234567', 'US'),
       ('Initech Ltd', 'GB7654321', 'GB')
on conflict (tax_id) do nothing;

-- 3 accounts per company
insert into company_account(company_id, account_number, bank_name, country, currency_code)
select c.id,
       'ACC-' || c.id || '-' || g,
       'Bank-' || g,
       c.country,
       (array ['USD','EUR','COP'])[1 + (random() * 2)::int]
from company c
         cross join generate_series(1, 3) g
on conflict do nothing;

-- Counter parties
insert into counterparty(display_name, tax_id, country, kyc_status)
values ('Provider One', 'CP001', 'CO', 'VERIFIED'),
       ('Provider Two', 'CP002', 'US', 'PENDING'),
       ('DIAN', 'GOV-CO', 'CO', 'VERIFIED'),
       ('Provider Three', 'CP003', 'CO', 'VERIFIED'),
       ('Provider Four', 'CP004', 'ES', 'PENDING')
on conflict (tax_id) do nothing;

-- Counterparty accounts
insert into counterparty_account(counterparty_id, account_number, bank_name, country)
select cp.id, 'CP-' || cp.id || '-' || g, 'BankX', cp.country
from counterparty cp
         cross join generate_series(1, 2) g
on conflict (counterparty_id, account_number) do nothing;

-- =========================
-- Exchange rates per day (COP report currency)
-- =========================
-- Fixed month window for stable baselines (change here if you want another month)
with period as (
    select make_date(2025, 8, 1) as start_date, make_date(2025, 9, 1) as end_date
),
days as (
    select generate_series((select start_date from period),
                           (select end_date   from period) - interval '1 day',
                           interval '1 day')::date as d
)
insert into exchange_rate(currency_code, valid_date, version, rate)
select cc,
       d                as valid_date,
       v                as version,
       case cc
           when 'USD' then 4200 + (date_part('day', d)::int * 3) + (v-1) * 5
           when 'EUR' then 4600 + (date_part('day', d)::int * 4) + (v-1) * 6
           else 1.00
       end::numeric(18,6) as rate
from (values ('USD'), ('EUR'), ('COP')) as c(cc)
cross join days
cross join generate_series(1, 2) v;

-- =========================
-- Tags
-- =========================
insert into movement_tag(name)
values ('PAYROLL'),
       ('SUPPLIER'),
       ('TAX')
on conflict (name) do nothing;

with period as (
    select make_date(2025, 8, 1) as start_date, make_date(2025, 9, 1) as end_date
),
-- deterministic enumerations of accounts to avoid picking the same one for all rows
acc as (
    select id, row_number() over(order by id) as rn from company_account
), acc_cnt as (
    select count(*)::int as cnt from acc
),
cpa as (
    select id, row_number() over(order by id) as rn from counterparty_account
), cpa_cnt as (
    select count(*)::int as cnt from cpa
),
params as (select 500000::int as total_rows),
rands as (
    select generate_series(1, (select total_rows from params)) as i,
           random() as r1,
           random() as r2
)
insert into movement(company_account_id, counterparty_account_id, currency_code, amount, booked_at, description)
select
       -- Distribute uniformly across company accounts using modulo on i
       (select a.id from acc a where a.rn = ((i - 1) % (select cnt from acc_cnt)) + 1) as company_account_id,
       -- Distribute across counterparty accounts with a phase shift so it doesn't align with company accounts
       (select p.id from cpa p where p.rn = ((i * 7 - 1) % (select cnt from cpa_cnt)) + 1)         as counterparty_account_id,
       case
         when r1 < 0.50 then 'COP'        -- ~50%
         when r1 < 0.80 then 'USD'        -- ~30%
         else               'EUR'         -- ~20%
       end::text as currency_code,
       round((random() * 10000)::numeric, 2)                           as amount,
       (
         (select start_date from period)
         + (
             floor((((select end_date from period) - (select start_date from period)) * 86400) * r2)::int
               || ' seconds'
           )::interval
       ) as booked_at,
       'seed movement'                                                 as description
from rands;

-- Assign 0–3 tags per movement (deterministic per movement to avoid same tag/count across all rows)
insert into movement_tags(movement_id, tag_id)
select m.id, mt.id
from movement m
join lateral (
  select t.id
  from movement_tag t
  -- deterministic pseudo-random order per movement using hash of (movement_id, tag_id)
  order by md5(m.id::text || '-' || t.id::text)
  limit (
    case
      when (m.id % 100) < 10 then 0   -- 10% no tags
      when (m.id % 100) < 75 then 1   -- 65% 1 tag
      when (m.id % 100) < 97 then 2   -- 22% 2 tags
      else 3                           -- 3% 3 tags
    end
  )
) mt on true;