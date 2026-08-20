-- Baseline schema for the bike-leasing application.
-- Matches the JPA entities in adapter/outbound/db so Hibernate `ddl-auto: validate` passes.
-- Flyway owns these tables; the CIB seven engine manages its own ACT_* tables separately.

CREATE TABLE bike_portfolio (
    bike_id VARCHAR(255) NOT NULL,
    model   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_bike_portfolio PRIMARY KEY (bike_id)
);

CREATE TABLE leasing_application (
    application_id     UUID         NOT NULL,
    customer_name      VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    age                INTEGER      NOT NULL,
    monthly_net_income FLOAT8       NOT NULL,
    bike_id            VARCHAR(255) NOT NULL,
    status             VARCHAR(255) NOT NULL,
    order_id           VARCHAR(255),
    contract_id        VARCHAR(255),
    created_at         TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_leasing_application PRIMARY KEY (application_id)
);
