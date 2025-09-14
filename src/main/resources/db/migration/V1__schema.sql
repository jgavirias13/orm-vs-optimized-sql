-- =========================
-- Core catalogs
-- =========================
create table company
(
    id         bigserial primary key,
    legal_name varchar(120) not null,
    tax_id     varchar(32)  not null unique,
    country    char(2),
    created_at timestamp    not null default now()
);

create table company_account
(
    id             bigserial primary key,
    company_id     bigint      not null references company (id),
    account_number varchar(40) not null,
    iban           varchar(40),
    bank_name      varchar(80),
    currency_code  varchar(3),
    country        char(2),
    created_at     timestamp   not null default now(),
    constraint uq_company_account unique (company_id, account_number)
);
create index idx_company_account_company on company_account (company_id);

create table counterparty
(
    id           bigserial primary key,
    display_name varchar(120) not null,
    tax_id       varchar(32) unique,
    country      char(2),
    kyc_status   varchar(20),
    created_at   timestamp    not null default now()
);

create table counterparty_account
(
    id              bigserial primary key,
    counterparty_id bigint    not null references counterparty (id),
    account_number  varchar(40),
    iban            varchar(40),
    bank_name       varchar(80),
    country         char(2),
    created_at      timestamp not null default now(),
    constraint uq_counterparty_account unique (counterparty_id, account_number)
);
create index idx_cp_account_counterparty on counterparty_account (counterparty_id);

create table currency
(
    code varchar(3) primary key,
    name varchar(50) not null
);

create table exchange_rate
(
    id            bigserial primary key,
    currency_code varchar(3)     not null references currency (code),
    valid_date    date           not null,
    version       int            not null,
    rate          numeric(18, 6) not null,
    created_at    timestamp      not null default now()
);
create index idx_rate_cc_date on exchange_rate (currency_code, valid_date);
create index idx_rate_cc_date_version on exchange_rate (currency_code, valid_date, version desc);

-- =========================
-- Movements + tags
-- =========================
create table movement
(
    id                      bigserial primary key,
    company_account_id      bigint         not null references company_account (id),
    counterparty_account_id bigint         not null references counterparty_account (id),
    currency_code           varchar(3)     not null references currency (code),
    amount                  numeric(18, 2) not null,
    booked_at               timestamp      not null,
    description             varchar(255),
    created_at              timestamp      not null default now()
);
create index idx_mv_company_booked on movement (company_account_id, booked_at);

create table movement_tag
(
    id   bigserial primary key,
    name varchar(40) unique not null
);

create table movement_tags
(
    movement_id bigint not null references movement (id),
    tag_id      bigint not null references movement_tag (id),
    primary key (movement_id, tag_id)
);

-- =========================
-- FX declaration
-- =========================
create table fx_declaration
(
    id                 bigserial primary key,
    company_account_id bigint      not null references company_account (id),
    period_year        int         not null,
    period_month       int         not null,
    status             varchar(20) not null,
    created_at         timestamp   not null default now(),
    constraint uq_fx_decl unique (company_account_id, period_year, period_month)
);

create table fx_declaration_summary
(
    id                bigserial primary key,
    fx_declaration_id bigint    not null unique references fx_declaration (id),
    total_local       numeric(18, 2),
    total_usd         numeric(18, 2),
    items_json        jsonb,
    created_at        timestamp not null default now()
);