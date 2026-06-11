-- Možnosti struktur lyží (woodcore, sandwich, cap...) – výběr v objednávkách
CREATE TABLE struktura_options (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0
);

-- Nejčastější servisní úpravy/úkony – výběr při přidávání úkonu
CREATE TABLE common_modification_options (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_struktura_options_sort ON struktura_options(sort_order);
CREATE INDEX idx_common_modification_options_sort ON common_modification_options(sort_order);

-- Výchozí struktury
INSERT INTO struktura_options (name, sort_order) VALUES
    ('woodcore', 1),
    ('sandwich', 2),
    ('cap', 3),
    ('hybrid', 4);

-- Výchozí časté úpravy
INSERT INTO common_modification_options (name, description, sort_order) VALUES
    ('Běžná úprava', 'Standardní servis', 1),
    ('Voskování', NULL, 2),
    ('Broušení skluznice', NULL, 3),
    ('Oprava hran', NULL, 4),
    ('Měření lyží', NULL, 5),
    ('Nanesení vosku', NULL, 6);
