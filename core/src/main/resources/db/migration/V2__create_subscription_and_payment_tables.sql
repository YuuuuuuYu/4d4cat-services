ALTER TABLE applydays_subscription_plan 
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS applydays_subscription (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL UNIQUE,
    plan_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATE,
    end_date DATE,
    next_billing_date DATE,
    
    -- BaseEntity
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS applydays_payment_method (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL UNIQUE,
    billing_key VARCHAR(255) NOT NULL,
    card_company VARCHAR(100),
    card_number_masked VARCHAR(50),
    is_default BOOLEAN NOT NULL DEFAULT TRUE,

    -- BaseEntity
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS applydays_payment (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    member_id UUID NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    portone_imp_uid VARCHAR(255) UNIQUE,
    portone_merchant_uid VARCHAR(255),
    fail_reason VARCHAR(2000),
    receipt_url VARCHAR(1000),
    approval_no VARCHAR(255),
    
    -- BaseEntity
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
