-- ski_number: sk + 9 číslic (sk000000001) - slouží jako ID lyže i obsah QR kódu

ALTER TABLE skis ADD COLUMN ski_number_new VARCHAR(12) UNIQUE;

UPDATE skis s SET ski_number_new = 'sk' || LPAD(row_num::text, 9, '0')
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_num FROM skis
) n WHERE s.id = n.id;

ALTER TABLE skis DROP COLUMN qr_code;
ALTER TABLE skis DROP COLUMN ski_number;
ALTER TABLE skis RENAME COLUMN ski_number_new TO ski_number;
ALTER TABLE skis ALTER COLUMN ski_number SET NOT NULL;

DROP INDEX IF EXISTS idx_skis_qr_code;
CREATE INDEX idx_skis_ski_number ON skis(ski_number);

SELECT setval('ski_seq', (SELECT COUNT(*) FROM skis));

CREATE OR REPLACE FUNCTION set_ski_number()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.ski_number IS NULL OR NEW.ski_number = '' THEN
    NEW.ski_number := 'sk' || LPAD(nextval('ski_seq')::text, 9, '0');
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ski_number
  BEFORE INSERT ON skis
  FOR EACH ROW
  EXECUTE FUNCTION set_ski_number();
