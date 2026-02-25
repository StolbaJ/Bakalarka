-- Stav objednávky: Nové, Ve zpracování, Pozastavená, Ukončená, Stornovaná
CREATE TYPE order_status AS ENUM ('NOVE', 'VE_ZPRACOVANI', 'POZASTAVENA', 'UKONCENA', 'STORNOVANA');

ALTER TABLE orders ADD COLUMN status order_status NOT NULL DEFAULT 'NOVE';

CREATE INDEX idx_orders_status ON orders(status);
