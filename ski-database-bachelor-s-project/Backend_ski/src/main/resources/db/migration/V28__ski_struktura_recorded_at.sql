-- Kdy byla u lyže naposledy zapsána struktura (z objednávky nebo úpravy)
ALTER TABLE skis ADD COLUMN IF NOT EXISTS struktura_recorded_at TIMESTAMP;
