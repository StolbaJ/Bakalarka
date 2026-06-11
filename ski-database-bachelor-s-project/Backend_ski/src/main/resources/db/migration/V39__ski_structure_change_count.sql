-- Počet změn struktury (broušení) u lyže – zvyšuje se při každém zapsání nové struktury z objednávky
ALTER TABLE skis ADD COLUMN IF NOT EXISTS structure_change_count INTEGER NOT NULL DEFAULT 0;
