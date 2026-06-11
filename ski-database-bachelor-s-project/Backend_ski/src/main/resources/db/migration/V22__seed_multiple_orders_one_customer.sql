-- Pro test: více objednávek na jednoho zákazníka (Jan Novák, customer_id = 1, telefon +420 123 456 789)
-- Přidáme 2 další objednávky k jeho první (or000000001), aby měl celkem 3.

INSERT INTO orders (order_number, customer_id, due_date, notes, priority) VALUES
('TEMP-004', 1, CURRENT_DATE + 5, 'Druhá objednávka – broušení', 'STREDNI'),
('TEMP-005', 1, CURRENT_DATE + 7, 'Třetí objednávka – kompletní servis', 'VYSOKA');

-- Ke každé nové objednávce jeden order_task (lyže)
INSERT INTO order_tasks (order_id, ski_id, status, priority, estimated_time_hours, created_at, updated_at)
SELECT o.id, 1, 'CEKA'::service_task_status, 'STREDNI'::service_task_priority, 2.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM orders o
WHERE o.customer_id = 1
  AND o.order_number IN ('or000000004', 'or000000005');

-- Servisní úkony k novým taskům
INSERT INTO service_task_items (task_id, task_name, task_description, completed)
SELECT ot.id, t.name, NULL, FALSE
FROM order_tasks ot
JOIN orders o ON ot.order_id = o.id
CROSS JOIN (VALUES ('Broušení hran'), ('Voskování')) AS t(name)
WHERE o.customer_id = 1
  AND o.order_number IN ('or000000004', 'or000000005')
  AND NOT EXISTS (SELECT 1 FROM service_task_items sti WHERE sti.task_id = ot.id);

-- Testovací přístup: číslo objednávky např. or000000001 (nebo or000000004 / or000000005), telefon +420 123 456 789
