-- Společné enum typy dle business domain modelu (PDF)

CREATE TYPE user_role AS ENUM ('ADMIN', 'TECHNICIAN', 'CUSTOMER');
CREATE TYPE ski_status AS ENUM ('DOSTUPNY', 'V_SERVISU', 'REZERVOVANO', 'NEDOSTUPNY');
CREATE TYPE ski_condition AS ENUM ('VYORNY', 'DOBRY', 'STREDNI', 'SPATNY');
CREATE TYPE service_task_status AS ENUM ('CEKA', 'PROBIHA', 'DOKONCENO', 'POZASTAVENO');
CREATE TYPE service_task_priority AS ENUM ('NIZKA', 'STREDNI', 'VYSOKA', 'KRITICKA');
