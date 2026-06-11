-- Všechny stávající (testovací) objednávky považujeme za „e-mail již odeslán“,
-- aby na ně cron neposílal notifikace.
UPDATE orders SET order_created_email_sent = true WHERE order_created_email_sent = false;
