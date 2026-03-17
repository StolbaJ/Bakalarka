-- Příznak, že objednávka byla založena v servisním GUI (ne přímo z e‑shopu)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS is_created_in_servis_gui BOOLEAN NOT NULL DEFAULT FALSE;

