CREATE TABLE IF NOT EXISTS applydays_member_benefit (
    id UUID PRIMARY KEY,
    member_id UUID NOT NULL,
    benefit_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_applydays_member_benefit_member_type UNIQUE (member_id, benefit_type)
);
