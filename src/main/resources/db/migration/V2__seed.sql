-- V2__seed.sql

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
-- Taxes per day
-- =========================
insert into exchange_rate(currency_code, valid_date, version, rate)
select cc,
       d::date                         as valid_date,
       v                               as version,
       case cc
           when 'USD' then 1.00 + (v - 1) * 0.01
           when 'EUR' then 1.10 + (v - 1) * 0.01
           else 4000 + (v - 1) * 5 end as rate
from (values ('USD'), ('EUR'), ('COP')) as c(cc)
         cross join generate_series(current_date - interval '60 day', current_date, interval '1 day') d
         cross join generate_series(1, 2) v;

-- =========================
-- Tags
-- =========================
insert into movement_tag(name)
values ('PAYROLL'),
       ('SUPPLIER'),
       ('TAX')
on conflict (name) do nothing;

-- =========================
-- Movements
-- =========================
with params as (select 200000::int as total_rows)
insert
into movement(company_account_id, counterparty_account_id, currency_code, amount, booked_at, description)
select (select id from company_account order by random() limit 1)      as company_account_id,
       (select id from counterparty_account order by random() limit 1) as counterparty_account_id,
       (array ['USD','EUR','COP'])[1 + floor(random() * 3)]::text      as currency_code,
       round((random() * 10000)::numeric, 2)                           as amount,
       (current_timestamp - (random() * 60 || ' days')::interval)      as booked_at,
       'seed movement'                                                 as description
from params, generate_series(1, (select total_rows from params));

insert into movement_tags(movement_id, tag_id)
select m.id, t.id
from movement m
         join lateral (
    select id
    from movement_tag
    where random() < 0.3
    order by random()
    limit 1
    ) t on true
where random() < 0.3;