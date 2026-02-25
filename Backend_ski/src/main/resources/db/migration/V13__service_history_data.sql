-- Ukázková historie servisu (ski_id a order_id odkazují na .id, ne na ski_number/order_number)

INSERT INTO service_history (ski_id, order_id, service_type, technician_id, duration_minutes, notes) VALUES
(1, 1, 'Broušení hran', 2, 45, 'Pravidelné broušení'),
(1, 1, 'Voskování', 2, 30, 'Univerzální vosk');
