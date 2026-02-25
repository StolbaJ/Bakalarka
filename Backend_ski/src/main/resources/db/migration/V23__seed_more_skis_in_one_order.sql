-- Na zkoušku: do jedné objednávky (or000000001) přidáme další lyže – jeden order_task = jedna lyže
-- Objednávka už má lyži 1 (Salomon), přidáme lyže 2 (Rossignol) a 3 (Atomic)

INSERT INTO order_tasks (order_id, ski_id, status, priority, estimated_time_hours, created_at, updated_at)
SELECT o.id, s.id, 'CEKA'::service_task_status, 'STREDNI'::service_task_priority, 2.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM orders o
CROSS JOIN skis s
WHERE o.order_number = 'or000000001'
  AND s.id IN (2, 3)
  AND NOT EXISTS (SELECT 1 FROM order_tasks ot WHERE ot.order_id = o.id AND ot.ski_id = s.id);

-- Servisní úkony k novým taskům (každá lyže má Broušení hran + Voskování)
INSERT INTO service_task_items (task_id, task_name, task_description, completed)
SELECT ot.id, t.name, NULL, FALSE
FROM order_tasks ot
JOIN orders o ON ot.order_id = o.id
CROSS JOIN (VALUES ('Broušení hran'), ('Voskování')) AS t(name)
WHERE o.order_number = 'or000000001'
  AND NOT EXISTS (SELECT 1 FROM service_task_items sti WHERE sti.task_id = ot.id);
