-- U úpravy lze nastavit, že před označením úkonu jako dokončený musí být vyplněn popis práce (výsledek měření apod.)
ALTER TABLE common_modification_options
    ADD COLUMN requires_work_description BOOLEAN NOT NULL DEFAULT FALSE;
