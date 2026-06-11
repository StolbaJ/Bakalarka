-- Přidání hodnoty CHANGE_OWN_PASSWORD do enum user_audit_action (změna vlastního hesla)
ALTER TYPE user_audit_action ADD VALUE 'CHANGE_OWN_PASSWORD';
