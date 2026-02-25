-- order_number: or + 9 číslic (or000000001)

UPDATE service_orders o SET order_number = 'or' || LPAD(row_num::text, 9, '0')
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_num FROM service_orders
) n WHERE o.id = n.id;

SELECT setval('order_seq', (SELECT COUNT(*) FROM service_orders));

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
  BEFORE INSERT ON service_orders
  FOR EACH ROW
  EXECUTE FUNCTION set_order_number();
