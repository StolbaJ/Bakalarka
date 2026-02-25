-- Audit log pro změny uživatelských účtů

CREATE TYPE user_audit_action AS ENUM (
    'CREATE',
    'UPDATE_ROLE',
    'RESET_PASSWORD',
    'DEACTIVATE',
    'REACTIVATE'
);

CREATE TABLE user_audit_log (
    id BIGSERIAL PRIMARY KEY,
    action user_audit_action NOT NULL,
    target_user_id BIGINT NOT NULL,
    target_username VARCHAR(50) NOT NULL,
    performed_by VARCHAR(50) NOT NULL,
    details VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_audit_target_user ON user_audit_log(target_user_id);
CREATE INDEX idx_user_audit_performed_by ON user_audit_log(performed_by);
CREATE INDEX idx_user_audit_created_at ON user_audit_log(created_at);
