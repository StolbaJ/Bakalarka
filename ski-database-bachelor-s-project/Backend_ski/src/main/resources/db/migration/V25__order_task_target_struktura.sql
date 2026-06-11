-- Cílová struktura lyže po dokončení objednávky (pokud se liší, zapíše se do skis.struktura)
ALTER TABLE order_tasks ADD COLUMN target_struktura VARCHAR(100);
