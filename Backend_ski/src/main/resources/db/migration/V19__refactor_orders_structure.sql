-- Refaktorování: Order 1:N lyží přes OrderTask
-- Objednávka -> Tasky (1 task = 1 lyže) -> Servisní úkony

-- 1. Nová tabulka orders (hlavní objednávka)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    due_date DATE,
    notes TEXT,
    price DECIMAL(10,2),
    pohoda_id BIGINT
);

-- 2. Tabulka order_tasks (1 task = 1 lyže v objednávce)
CREATE TABLE order_tasks (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    ski_id BIGINT NOT NULL REFERENCES skis(id) ON DELETE CASCADE,
    status service_task_status DEFAULT 'CEKA',
    priority service_task_priority DEFAULT 'STREDNI',
    estimated_time_hours DECIMAL(5,2),
    actual_time_hours DECIMAL(5,2),
    assigned_to BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_tasks_order_id ON order_tasks(order_id);
CREATE INDEX idx_order_tasks_ski_id ON order_tasks(ski_id);
CREATE INDEX idx_order_tasks_status ON order_tasks(status);
CREATE INDEX idx_order_tasks_assigned_to ON order_tasks(assigned_to);

-- 3. Migrace dat: každý service_order -> 1 order + 1 order_task
INSERT INTO orders (order_number, customer_id, created_at, updated_at, due_date, notes)
SELECT order_number, customer_id, created_at, updated_at, due_date, notes FROM service_orders;

INSERT INTO order_tasks (order_id, ski_id, status, priority, estimated_time_hours, actual_time_hours, assigned_to, created_at, updated_at, completed_at)
SELECT o.id, so.ski_id, so.status, so.priority, so.estimated_time_hours, so.actual_time_hours, so.assigned_to, so.created_at, so.updated_at, so.completed_at
FROM service_orders so
JOIN orders o ON o.order_number = so.order_number;

-- 4. Přidat task_id do service_task_items a migrovat
ALTER TABLE service_task_items ADD COLUMN task_id BIGINT REFERENCES order_tasks(id) ON DELETE CASCADE;

UPDATE service_task_items sti
SET task_id = ot.id
FROM service_orders so
JOIN orders o ON o.order_number = so.order_number
JOIN order_tasks ot ON ot.order_id = o.id AND ot.ski_id = so.ski_id
WHERE sti.order_id = so.id;

ALTER TABLE service_task_items ALTER COLUMN task_id SET NOT NULL;
ALTER TABLE service_task_items DROP COLUMN order_id;

-- 5. service_history: změnit order_id na order_task_id
ALTER TABLE service_history ADD COLUMN order_task_id BIGINT REFERENCES order_tasks(id) ON DELETE SET NULL;

UPDATE service_history sh
SET order_task_id = ot.id
FROM service_orders so
JOIN orders o ON o.order_number = so.order_number
JOIN order_tasks ot ON ot.order_id = o.id AND ot.ski_id = so.ski_id
WHERE sh.order_id = so.id;

ALTER TABLE service_history DROP COLUMN order_id;
CREATE INDEX idx_service_history_order_task_id ON service_history(order_task_id);

-- 6. Smazat trigger a funkci pro service_orders, pak smazat service_orders
DROP TRIGGER IF EXISTS trg_order_number ON service_orders;

-- Trigger pro novou tabulku orders
CREATE OR REPLACE FUNCTION set_order_number()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.order_number IS NULL OR NEW.order_number = '' OR NEW.order_number LIKE 'TEMP-%' THEN
    NEW.order_number := 'or' || LPAD(nextval('order_seq')::text, 9, '0');
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_number
  BEFORE INSERT ON orders
  FOR EACH ROW
  EXECUTE FUNCTION set_order_number();

-- 7. Odstranit starou tabulku
DROP TABLE service_orders;

-- 8. Doplnění tasků pro objednávky, které je nemají (fallback)
INSERT INTO order_tasks (order_id, ski_id, status, priority, estimated_time_hours, created_at, updated_at)
SELECT o.id, (SELECT id FROM skis ORDER BY id LIMIT 1), 'CEKA'::service_task_status, 'STREDNI'::service_task_priority, 2.0, o.created_at, o.updated_at
FROM orders o
WHERE NOT EXISTS (SELECT 1 FROM order_tasks ot WHERE ot.order_id = o.id) AND (SELECT COUNT(*) FROM skis) > 0;

INSERT INTO service_task_items (task_id, task_name, task_description, completed)
SELECT ot.id, t.name, NULL, FALSE
FROM order_tasks ot
CROSS JOIN (VALUES ('Broušení hran'), ('Voskování')) AS t(name)
WHERE NOT EXISTS (SELECT 1 FROM service_task_items sti WHERE sti.task_id = ot.id);
