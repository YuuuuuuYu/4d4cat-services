ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS payment_plan_id UUID;
ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS pending_billing_key TEXT;
ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS renewal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS charge_requested_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_applydays_payment_pending_member
ON applydays_payment (status, member_id);
