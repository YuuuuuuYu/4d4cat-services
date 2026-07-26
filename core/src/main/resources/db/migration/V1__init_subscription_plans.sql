CREATE TABLE IF NOT EXISTS applydays_subscription_plan (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL,
    billing_cycle_months INT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

INSERT INTO applydays_subscription_plan (id, name, price, billing_cycle_months, created_at, updated_at)
SELECT 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'ApplyDays Premium', 16500, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM applydays_subscription_plan WHERE name = 'ApplyDays Premium'
);
