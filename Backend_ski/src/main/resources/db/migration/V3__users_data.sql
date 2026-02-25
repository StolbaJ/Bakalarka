-- Naplnění users: výchozí admin a technik. Hashe Argon2 doplní DataInitializer.

INSERT INTO users (username, password_hash, role, full_name, email, active)
VALUES ('admin', '$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder', 'ADMIN', 'Administrátor', 'admin@ski-inventory.cz', TRUE);

INSERT INTO users (username, password_hash, role, full_name, email, active)
VALUES ('technician', '$argon2id$v=19$m=65536,t=3,p=4$placeholder$placeholder', 'TECHNICIAN', 'Technik', 'technician@ski-inventory.cz', TRUE);
