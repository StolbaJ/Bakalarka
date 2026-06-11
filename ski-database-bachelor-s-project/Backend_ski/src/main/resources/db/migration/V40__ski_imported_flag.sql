-- Příznak, že lyže pochází z importované objednávky (random / ještě nebyla na prodejně)
ALTER TABLE skis
    ADD COLUMN IF NOT EXISTS is_from_imported_order BOOLEAN NOT NULL DEFAULT FALSE;

