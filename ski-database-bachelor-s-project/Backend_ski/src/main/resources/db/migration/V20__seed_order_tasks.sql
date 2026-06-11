-- Doplnění order_tasks a service_task_items pro objednávky, které je nemají
INSERT INTO order_tasks (order_id, ski_id, status, priority, estimated_time_hours, created_at, updated_at)
SELECT o.id, (SELECT id FROM skis ORDER BY id LIMIT 1), 'CEKA'::service_task_status, 'STREDNI'::service_task_priority, 2.0, o.created_at, o.updated_at
FROM orders o
WHERE NOT EXISTS (SELECT 1 FROM order_tasks ot WHERE ot.order_id = o.id) AND (SELECT COUNT(*) FROM skis) > 0;

INSERT INTO service_task_items (task_id, task_name, task_description, completed)
SELECT ot.id, t.name, NULL, FALSE
FROM order_tasks ot
CROSS JOIN (VALUES ('Broušení hran'), ('Voskování')) AS t(name)
WHERE NOT EXISTS (SELECT 1 FROM service_task_items sti WHERE sti.task_id = ot.id);
