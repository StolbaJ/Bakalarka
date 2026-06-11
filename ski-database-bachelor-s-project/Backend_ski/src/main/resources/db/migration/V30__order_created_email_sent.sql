-- Flag: byl zákazníkovi již odeslán e-mail o založení objednávky (notifikace + odkaz na sledování)?
-- Cron projde objednávky s false a pošle e-mail tam, kde má zákazník e-mail.
ALTER TABLE orders ADD COLUMN order_created_email_sent BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_orders_created_email_sent ON orders(order_created_email_sent) WHERE order_created_email_sent = false;
