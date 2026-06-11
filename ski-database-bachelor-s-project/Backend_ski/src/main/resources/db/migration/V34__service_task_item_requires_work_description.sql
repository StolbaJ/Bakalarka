-- U položky úkolu přebíráme z typu úpravy, zda je povinný popis práce před dokončením
ALTER TABLE service_task_items
    ADD COLUMN requires_work_description BOOLEAN NOT NULL DEFAULT FALSE;
