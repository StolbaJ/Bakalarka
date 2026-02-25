-- Ukázkové servisní zakázky (customer_id a ski_id odkazují na .id)
-- order_number se přepíše v V17 na formát or000000001

INSERT INTO service_orders (order_number, customer_id, ski_id, status, priority, estimated_time_hours, assigned_to, due_date, notes) VALUES
('TEMP-001', 1, 1, 'PROBIHA', 'VYSOKA', 2.5, 2, CURRENT_DATE + 2, 'Zákazník potřebuje lyže do pátku'),
('TEMP-002', 2, 2, 'CEKA', 'STREDNI', 2.0, NULL, CURRENT_DATE + 3, NULL),
('TEMP-003', 3, 3, 'CEKA', 'KRITICKA', 3.0, NULL, CURRENT_DATE + 1, 'Naléhavá oprava pro víkend');
