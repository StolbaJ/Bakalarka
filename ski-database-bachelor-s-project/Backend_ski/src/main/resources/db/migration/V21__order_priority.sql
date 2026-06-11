-- Priorita na úrovni objednávky
ALTER TABLE orders ADD COLUMN priority service_task_priority DEFAULT 'STREDNI';
UPDATE orders SET priority = 'STREDNI' WHERE priority IS NULL;
