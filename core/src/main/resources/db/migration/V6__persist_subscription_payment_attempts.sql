ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS payment_plan_id UUID;
ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS pending_billing_key TEXT;
ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS renewal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE applydays_payment ADD COLUMN IF NOT EXISTS charge_requested_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_applydays_payment_pending_member
ON applydays_payment (status, member_id);

UPDATE applydays_subscription s
SET status = 'EXPIRED', next_billing_date = NULL
WHERE s.status IN ('ACTIVE', 'CANCELED')
  AND EXISTS (SELECT 1 FROM member m WHERE m.id = s.member_id AND m.deleted = true);

UPDATE applydays_payment_method p
SET deleted = true
WHERE p.deleted = false
  AND EXISTS (SELECT 1 FROM member m WHERE m.id = p.member_id AND m.deleted = true);
