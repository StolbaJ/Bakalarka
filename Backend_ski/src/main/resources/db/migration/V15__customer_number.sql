-- customer_number: us + 9 číslic (us000000001)

ALTER TABLE customers ADD COLUMN customer_number VARCHAR(12) UNIQUE;

UPDATE customers c SET customer_number = 'us' || LPAD(row_num::text, 9, '0')
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_num FROM customers
) n WHERE c.id = n.id;

ALTER TABLE customers ALTER COLUMN customer_number SET NOT NULL;

SELECT setval('customer_seq', (SELECT COUNT(*) FROM customers));

CREATE OR REPLACE FUNCTION set_customer_number()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.customer_number IS NULL OR NEW.customer_number = '' THEN
    NEW.customer_number := 'us' || LPAD(nextval('customer_seq')::text, 9, '0');
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customer_number
  BEFORE INSERT ON customers
  FOR EACH ROW
  EXECUTE FUNCTION set_customer_number();
