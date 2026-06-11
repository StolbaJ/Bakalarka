-- Atribut struktura u lyže (např. woodcore, sandwich, cap)
ALTER TABLE skis ADD COLUMN IF NOT EXISTS struktura VARCHAR(100);
