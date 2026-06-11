-- Ukázkové servisní úkoly (order_id odkazuje na service_orders.id, ne na order_number)

INSERT INTO service_task_items (order_id, task_name, task_description, completed) VALUES
(1, 'Broušení hran', NULL, TRUE),
(1, 'Voskování', NULL, TRUE),
(1, 'Kontrola vázání', NULL, FALSE),
(2, 'Broušení hran', NULL, FALSE),
(2, 'Voskování', NULL, FALSE),
(3, 'Oprava vázání', 'Kontrola DIN', FALSE),
(3, 'Výměna vosku', NULL, FALSE);
