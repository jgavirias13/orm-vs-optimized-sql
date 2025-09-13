create table if not exists bootstrap_check
(
    id         bigserial primary key,
    created_at timestamp not null default now()
);