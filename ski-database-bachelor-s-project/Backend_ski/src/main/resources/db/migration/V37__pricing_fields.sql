-- Cena k servisní úpravě (ceník)
ALTER TABLE common_modification_options
    ADD COLUMN price NUMERIC(10, 2);

-- Cena konkrétního úkonu v zakázce (zkopíruje se z ceníku, lze upravit)
ALTER TABLE service_task_items
    ADD COLUMN price NUMERIC(10, 2);

-- Sleva na objednávce (v procentech, 0–100)
ALTER TABLE orders
    ADD COLUMN discount NUMERIC(5, 2) DEFAULT 0;
