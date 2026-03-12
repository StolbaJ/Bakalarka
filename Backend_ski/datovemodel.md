# Formální popis datového modelu – Backend Ski

Dokument popisuje datový model aplikace pro správu lyžařského servisu a skladu lyží. Model je implementován pomocí JPA/Hibernate; databázové tabulky odpovídají entitám níže.

---

## 1. Přehled entit (tabulky)

| Entita (tabulka) | Popis |
|------------------|--------|
| `customers` | Zákazníci |
| `users` | Uživatelé systému (přihlášení, role) |
| `orders` | Objednávky servisu |
| `order_tasks` | Úkoly (položky) objednávky – přiřazení lyží a servisních úkonů |
| `service_task_items` | Jednotlivé servisní úkony v rámci úkolu |
| `skis` | Lyže (sklad, stav, struktura) |
| `common_modification_options` | Běžné typy úprav (šablony úkonů) |
| `struktura_options` | Možnosti hodnot pro pole „struktura“ u lyží |
| `qr_scan_log` | Log naskenování QR kódů lyží |
| `user_audit_log` | Auditní záznamy změn uživatelských účtů |

---

## 2. Entity a atributy

### 2.1 customers (Zákazníci)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `customer_number` | VARCHAR(12) | N | ANO | Číslo zákazníka (generované) |
| `name` | VARCHAR(100) | N | | Jméno / název |
| `email` | VARCHAR(100) | A | | E-mail |
| `phone` | VARCHAR(20) | A | | Telefon |
| `address` | TEXT | A | | Adresa |
| `created_at` | TIMESTAMP | A | | Čas vytvoření záznamu |

---

### 2.2 users (Uživatelé)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `username` | VARCHAR(50) | N | ANO | Přihlašovací jméno |
| `password_hash` | VARCHAR / String | N | | Hash hesla |
| `role` | user_role (enum) | N | | Role: ADMIN, TECHNICIAN, CUSTOMER |
| `full_name` | VARCHAR(100) | A | | Celé jméno |
| `email` | VARCHAR(100) | A | | E-mail |
| `phone` | VARCHAR(20) | A | | Telefon |
| `active` | BOOLEAN / Boolean | N | | Zda je účet aktivní (default true) |
| `created_at` | TIMESTAMP | A | | Čas vytvoření |
| `updated_at` | TIMESTAMP | A | | Čas poslední aktualizace |

---

### 2.3 orders (Objednávky)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `order_number` | VARCHAR(12) | N | ANO | Číslo objednávky (generované) |
| `customer_id` | BIGINT / FK → customers | A | | Odkaz na zákazníka |
| `created_at` | TIMESTAMP | A | | Čas vytvoření |
| `updated_at` | TIMESTAMP | A | | Čas poslední aktualizace |
| `due_date` | DATE | A | | Termín splnění |
| `price` | DECIMAL(10,2) / BigDecimal | A | | Cena |
| `pohoda_id` | BIGINT / Long | A | | ID v externím systému Pohoda |
| `priority` | service_task_priority (enum) | A | | Priorita (default STREDNI) |
| `status` | order_status (enum) | A | | Stav objednávky (default NOVE) |
| `notes` | TEXT | A | | Poznámky |
| `order_created_email_sent` | BOOLEAN | N | | Zda byl odeslán e-mail o založení objednávky |

---

### 2.4 order_tasks (Úkoly objednávky)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `order_id` | BIGINT / FK → orders | N | | Objednávka |
| `ski_id` | BIGINT / FK → skis | N | | Lyže |
| `status` | service_task_status (enum) | A | | Stav úkolu (default CEKA) |
| `priority` | service_task_priority (enum) | A | | Priorita (default STREDNI) |
| `estimated_time_hours` | DECIMAL(5,2) / BigDecimal | A | | Odhadovaný čas (hodiny) |
| `actual_time_hours` | DECIMAL(5,2) / BigDecimal | A | | Skutečný čas (hodiny) |
| `assigned_to` | BIGINT / FK → users | A | | Přiřazený uživatel (technik) |
| `created_at` | TIMESTAMP | A | | Čas vytvoření |
| `updated_at` | TIMESTAMP | A | | Čas poslední aktualizace |
| `completed_at` | TIMESTAMP | A | | Čas dokončení |
| `target_struktura` | VARCHAR(100) | A | | Cílová struktura (pro lyži) |

---

### 2.5 service_task_items (Položky servisního úkolu)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `task_id` | BIGINT / FK → order_tasks | N | | Úkol objednávky |
| `task_name` | VARCHAR(100) | N | | Název úkonu |
| `task_instruction` | TEXT | A | | Návod / instrukce k úkonu |
| `task_description` | TEXT | A | | Výsledek / popis provedené práce |
| `completed` | BOOLEAN / Boolean | N | | Zda je úkon dokončen (default false) |
| `requires_work_description` | BOOLEAN | N | | Zda je povinný popis práce před dokončením |
| `completed_at` | TIMESTAMP | A | | Čas dokončení úkonu |
| `created_at` | TIMESTAMP | A | | Čas vytvoření |

---

### 2.6 skis (Lyže)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `ski_number` | VARCHAR(12) | N | ANO | Číslo lyže (generované) |
| `brand` | VARCHAR(50) | N | | Značka |
| `model` | VARCHAR(100) | N | | Model |
| `length` | VARCHAR(10) | N | | Délka |
| `year` | INTEGER | A | | Rok výroby |
| `ski_type` | VARCHAR(50) | A | | Typ lyží |
| `weight_kg` | DECIMAL(5,2) / BigDecimal | A | | Hmotnost (kg) |
| `condition` | ski_condition (enum) | A | | Stav lyže (default DOBRY) |
| `status` | ski_status (enum) | A | | Dostupnost (default DOSTUPNY) |
| `location` | VARCHAR(100) | A | | Umístění |
| `notes` | TEXT | A | | Poznámky |
| `last_service_date` | DATE | A | | Datum posledního servisu |
| `next_service_date` | DATE | A | | Datum příštího servisu |
| `struktura` | VARCHAR(100) | A | | Aktuální struktura |
| `struktura_recorded_at` | TIMESTAMP | A | | Čas záznamu struktury |
| `created_at` | TIMESTAMP | A | | Čas vytvoření |
| `updated_at` | TIMESTAMP | A | | Čas poslední aktualizace |

---

### 2.7 common_modification_options (Běžné typy úprav)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `name` | VARCHAR(200) | N | | Název úpravy |
| `description` | TEXT | A | | Popis |
| `sort_order` | INTEGER | N | | Pořadí pro zobrazení |
| `requires_work_description` | BOOLEAN | N | | Zda úkon vyžaduje popis práce před dokončením |

---

### 2.8 struktura_options (Možnosti struktury)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `name` | VARCHAR(100) | N | ANO | Název možnosti struktury |
| `sort_order` | INTEGER | N | | Pořadí pro zobrazení |

---

### 2.9 qr_scan_log (Log naskenování QR)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `user_id` | BIGINT / FK → users | N | | Uživatel, který naskenoval |
| `ski_id` | BIGINT / FK → skis | N | | Naskenované lyže |
| `scanned_at` | TIMESTAMP | N | | Čas naskenování |

---

### 2.10 user_audit_log (Audit uživatelů)

| Atribut | Datový typ (DB / Java) | Nullable | Unique | Popis |
|---------|-------------------------|----------|--------|--------|
| `id` | BIGINT / Long | N | PK | Primární klíč, auto-increment |
| `action` | user_audit_action (enum) | N | | Typ akce (CREATE, UPDATE_ROLE, …) |
| `target_user_id` | BIGINT / Long | N | | ID uživatele, kterého se změna týká |
| `target_username` | VARCHAR(50) | N | | Username cílového uživatele |
| `performed_by` | VARCHAR(50) | N | | Username toho, kdo akci provedl |
| `details` | VARCHAR(500) | A | | Detaily změny |
| `created_at` | TIMESTAMP | A | | Čas záznamu |

---

## 3. Výčtové typy (enum)

### order_status (Stav objednávky)

| Hodnota | Význam |
|---------|--------|
| NOVE | Nová |
| VE_ZPRACOVANI | Ve zpracování |
| POZASTAVENA | Pozastavená |
| UKONCENA | Ukončená |
| STORNOVANA | Stornovaná |

### user_role (Role uživatele)

| Hodnota | Význam |
|---------|--------|
| ADMIN | Administrátor |
| TECHNICIAN | Technik |
| CUSTOMER | Zákazník |

### ski_status (Stav lyže – dostupnost)

| Hodnota | Význam |
|---------|--------|
| DOSTUPNY | Dostupný |
| V_SERVISU | V servisu |
| REZERVOVANO | Rezervováno |
| NEDOSTUPNY | Nedostupný |

### ski_condition (Stav lyže – kondice)

| Hodnota | Význam |
|---------|--------|
| VYORNY | Výborný |
| DOBRY | Dobrý |
| STREDNI | Střední |
| SPATNY | Špatný |

### service_task_status (Stav servisního úkolu)

| Hodnota | Význam |
|---------|--------|
| CEKA | Čeká |
| PROBIHA | Probíhá |
| DOKONCENO | Dokončeno |
| POZASTAVENO | Pozastaveno |

### service_task_priority (Priorita úkolu)

| Hodnota | Význam |
|---------|--------|
| NIZKA | Nízká |
| STREDNI | Střední |
| VYSOKA | Vysoká |
| KRITICKA | Kritická |

### user_audit_action (Typ auditační akce)

| Hodnota | Význam |
|---------|--------|
| CREATE | Vytvoření uživatele |
| UPDATE_ROLE | Změna role/oprávnění |
| RESET_PASSWORD | Reset hesla |
| DEACTIVATE | Deaktivace účtu |
| REACTIVATE | Reaktivace účtu |
| CHANGE_OWN_PASSWORD | Změna vlastního hesla |

---

## 4. Vztahy mezi entitami (shrnutí)

- **Order** → **Customer** (N:1) – objednávka patří zákazníkovi.
- **Order** → **OrderTask** (1:N) – objednávka obsahuje úkoly.
- **OrderTask** → **Ski** (N:1) – úkol se týká jedněch lyží.
- **OrderTask** → **User** (N:1, assignedTo) – úkol může být přiřazen technikovi.
- **OrderTask** → **ServiceTaskItem** (1:N) – úkol obsahuje položky (jednotlivé úkony).
- **ServiceTaskItem** → **OrderTask** (N:1) – položka patří k úkolu.
- **QrScanLog** → **User** (N:1), **QrScanLog** → **Ski** (N:1) – log propojuje uživatele a lyže.

Entita **CommonModificationOption** a **StrukturaOption** jsou referenční (slovníkové) tabulky bez přímých FK v ostatních entitách; používají se při vytváření úkonů a při výběru struktury.

---

*Dokument vygenerován z JPA entit v `com.ski.inventory.model`.*
