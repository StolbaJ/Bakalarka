# Návod pro testování – Ski Inventory Management

Dokument slouží testerům k manuálnímu testování aplikace a může být použit v bakalářské práci jako popis testovacích scénářů.

---

## 1. Předpoklady a prostředí

- **Docker a Docker Compose** nainstalované a funkční
- **Prohlížeč** (Chrome, Firefox, Edge) s povoleným JavaScriptem
- Porty **3000**, **8080**, **5432** volné

Spuštění aplikace viz **SPUSTENI.md**. Pro testování postačí dev režim:

```bash
cd /cesta/k/BP
docker compose -f docker/docker-compose.dev.yml up -d --build
```

- **Frontend:** http://localhost:3000  
- **API:** http://localhost:8080  

---

## 2. Testovací účty

| Role       | Uživatelské jméno | Heslo   | Poznámka                          |
|-----------|-------------------|--------|-----------------------------------|
| Admin     | `admin`           | `admin123` | Plný přístup včetně statistik a uživatelů |
| Technik   | `technician`      | `tech123`  | Objednávky, scanner, databáze lyží      |
| Zákazník  | přes vyhledání objednávky | číslo objednávky + telefon | Jen „Moje objednávky“ |

---

## 3. Obecné zásady testování

- **Klikání:** Používejte levé tlačítko myši na tlačítka, odkazy a položky v seznamech. Na mobilu tapnutí.
- **Formuláře:** Vyplňte povinná pole (označená nebo s chybou po odeslání), pak klikněte na tlačítko pro odeslání („Přihlásit“, „Uložit“, „Přidat“ apod.).
- **Načítání:** Po akci může chvíli trvat načtení; zobrazí se indikátor (např. točící se ikona). Po dokončení by měla stránka zobrazit aktuální data.
- **Chyby:** Při neplatných údajích nebo selhání API se zobrazí chybová hláška (červený text nebo upozornění). Zkontrolujte, že zpráva odpovídá situaci.
- **Navigace:** Horní lišta obsahuje odkazy podle role. Po přihlášení ověřte, že vidíte správné položky menu.

---

## 4. Testovací scénáře

Níže jsou scénáře popsány tak, aby je tester mohl provést krok za krokem a výsledek zapsat (prospěch/neúspěch, popř. poznámka). Stejné scénáře lze v bakalářce uvést jako „Scénáře manuálního testování“.

---

### 4.1 Přihlášení administrátora / technika

**Cíl:** Ověřit přihlášení zaměstnance (admin nebo technik).

**Kroky:**

1. Otevřete http://localhost:3000.
2. V horní liště klikněte na tlačítko **„Přihlášení“**.
3. V modálním okně vyplňte:
   - Uživatelské jméno: `admin`
   - Heslo: `admin123`
4. Klikněte na **„Přihlásit“**.

**Očekávaný výsledek:** Modál se zavře, v horní liště je jméno uživatele (např. „admin“), v menu jsou položky Domů, QR Scanner, Databáze lyží, Objednávky a dle role i Statistiky, Struktury a úpravy, Správa uživatelů (pouze admin).

**Varianta – neplatné údaje:** Zadejte špatné heslo a klikněte „Přihlásit“. Očekává se chybová zpráva „Neplatné přihlašovací údaje“ a zůstáváte na přihlašovacím formuláři.

---

### 4.2 Přihlášení zákazníka (vyhledání objednávky)

**Cíl:** Ověřit vstup zákazníka přes číslo objednávky a telefon.

**Předpoklad:** V systému existuje objednávka a znáte její číslo a telefonní číslo zákazníka (např. z databáze nebo z předchozího vytvoření objednávky).

**Kroky:**

1. Na úvodní stránce (bez přihlášení zaměstnance) vyplňte v sekci „Vyhledání objednávky“:
   - Číslo objednávky
   - Telefonní číslo
2. Klikněte na tlačítko pro vyhledání („Vyhledat“ / „Zobrazit objednávky“).

**Očekávaný výsledek:** Při správných údajích se uživatel „přihlásí“ jako zákazník; v menu se zobrazí „Domů“ a „Moje objednávky“. Po kliknutí na „Moje objednávky“ se zobrazí seznam jeho objednávek.

**Neplatné údaje:** Špatné číslo objednávky nebo telefon → zpráva typu „Objednávka nebyla nalezena nebo telefonní číslo nesouhlasí“.

---

### 4.3 Domovská stránka po přihlášení (admin/technik)

**Cíl:** Ověřit zobrazení dashboardu a rychlých akcí.

**Kroky:**

1. Přihlaste se jako admin nebo technik (scénář 4.1).
2. Klikněte v menu na **„Domů“** (nebo otevřete http://localhost:3000).

**Očekávaný výsledek:** Nadpis „Ski Inventory Management“, čtyři statistické karty (Celkem lyží, V servisu, Dokončeno dnes, Průměrná doba) a sekce „Rychlé akce“ se čtyřmi kartami: Skenovat QR kód, Databáze lyží, Servisní úkony (odkaz na objednávky), Statistiky. Kliknutím na kartu dojde k přechodu na příslušnou stránku.

---

### 4.4 Objednávky – seznam a filtrování

**Cíl:** Ověřit načtení seznamu objednávek a filtrů.

**Kroky:**

1. Přihlaste se jako admin nebo technik.
2. V menu klikněte na **„Objednávky“**.
3. Po načtení zkontrolujte seznam objednávek (tabulka nebo karty).
4. Do vyhledávacího pole zadejte část čísla objednávky nebo zákazníka a ověřte, že se seznam filtruje.
5. Zapněte/vypněte filtr „Zobrazit dokončené“ (pokud je k dispozici) a ověřte změnu seznamu.

**Očekávaný výsledek:** Seznam se načte bez chyby; vyhledávání a přepínač dokončených mění zobrazené položky.

---

### 4.5 Objednávky – vytvoření nové objednávky

**Cíl:** Ověřit vytvoření objednávky včetně lyží a úkolů.

**Kroky:**

1. Na stránce Objednávky klikněte na tlačítko **„Nová objednávka“** (nebo podobně).
2. Vyplňte povinná pole (např. zákazník, kontakt, lyže – struktura, úpravy, počet).
3. Přidejte alespoň jeden servisní úkol (pokud formulář vyžaduje).
4. Odešlete formulář („Vytvořit“, „Uložit“).

**Očekávaný výsledek:** Objednávka se vytvoří, modal/formulář se zavře a v seznamu objednávek se objeví nová položka. Po rozkliknutí objednávky jsou vidět zadané lyže a úkoly.

---

### 4.6 Objednávky – rozbalení objednávky a úkoly

**Cíl:** Ověřit zobrazení detailu objednávky a úkolů.

**Kroky:**

1. Na stránce Objednávky klikněte na řádek nebo na ikonu pro rozbalení (šipka/chevron) u jedné objednávky.
2. Zkontrolujte, že se zobrazí detail: lyže, úkoly, stavy.
3. Rozbalte jeden úkol (pokud je rozbalovací) a ověřte zobrazení položek úkolu.
4. (Volitelně) Změňte stav úkolu nebo položky, pokud je to v rozhraní umožněno.

**Očekávaný výsledek:** Detail se zobrazí bez chyby; stavy a údaje odpovídají datům v systému. Změna stavu (pokud je k dispozici) se projeví po obnovení nebo automaticky.

---

### 4.7 QR Scanner

**Cíl:** Ověřit skenování QR kódu a ruční zadání kódu.

**Kroky:**

1. Přihlaste se jako admin nebo technik.
2. V menu klikněte na **„QR Scanner“**.
3. Klikněte na tlačítko pro spuštění kamery/skenování (např. „Skenovat“).
4. Povolte přístup ke kameře (pokud prohlížeč vyzve) a naskenujte QR kód lyže, nebo použijte možnost **ručního zadání kódu**: vyplňte kód a potvrďte („Přidat“, „Vyhledat“).

**Očekávaný výsledek:** Po úspěšném skenu/ručním zadání se zobrazí informace o lyži (nebo záznam o skenu). Při neexistujícím kódu může být chybová hláška; aplikace by neměla spadnout.

---

### 4.8 Databáze lyží

**Cíl:** Ověřit prohlížení, přidání, editaci a smazání lyže.

**Kroky:**

1. V menu klikněte na **„Databáze lyží“**.
2. Po načtení seznamu klikněte na jednu lyži (nebo „Zobrazit“) a ověřte zobrazení detailu.
3. Klikněte na **„Přidat lyži“** (nebo podobně), vyplňte povinná pole a uložte. Ověřte, že se lyže objeví v seznamu.
4. Otevřete lyži k editaci („Upravit“), změňte údaj a uložte. Ověřte, že změna zůstane.
5. U vybrané lyže (ideálně testovací) zvolte **„Smazat“** a v potvrzovacím dialogu smazání potvrďte. Lyže by měla ze seznamu zmizet.

**Očekávaný výsledek:** Všechny operace proběhnou bez chyby; seznam a detaily odpovídají provedeným změnám.

---

### 4.9 Statistiky (pouze admin)

**Cíl:** Ověřit zobrazení statistik a změnu období.

**Kroky:**

1. Přihlaste se jako **admin**.
2. V menu klikněte na **„Statistiky“**.
3. Zkontrolujte, že se zobrazí grafy/čísla (např. lyže v objednávkách – Čeká, Probíhá, Dokončeno; průměrný čas).
4. V rozbalovacím seznamu „Období“ změňte období (např. Posledních 7 dní, 30 dní, 90 dní, rok) a ověřte, že se data přenačtou.

**Očekávaný výsledek:** Stránka se načte bez chyby; po změně období se obsah aktualizuje. Technik by neměl mít v menu položku Statistiky (nebo po přímém vstupu na URL dostane odmítnutí).

---

### 4.10 Nastavení – struktury a úpravy (pouze admin)

**Cíl:** Ověřit správu možností „Struktura“ a „Úpravy“ pro lyže/objednávky.

**Kroky:**

1. Přihlaste se jako **admin**.
2. V menu klikněte na **„Struktury a úpravy“** (Nastavení).
3. V sekci struktur přidejte novou strukturu: zadejte název a klikněte na „Přidat“ (nebo podobně). Ověřte, že se položka objeví v seznamu.
4. V sekci úprav přidejte novou úpravu (název, popis dle formuláře) a uložte. Ověřte zobrazení v seznamu.
5. (Volitelně) Smažte testovací položku a ověřte, že zmizí ze seznamu.

**Očekávaný výsledek:** Přidání a smazání se projeví v seznamech; chybové stavy zobrazí srozumitelnou hlášku.

---

### 4.11 Správa uživatelů (pouze admin)

**Cíl:** Ověřit výpis uživatelů, vytvoření uživatele a základní akce.

**Kroky:**

1. Přihlaste se jako **admin**.
2. V menu klikněte na **„Správa uživatelů“**.
3. Zkontrolujte seznam uživatelů (admin, technician, popř. další).
4. Klikněte na **„Přidat uživatele“** (nebo podobně), vyplňte uživatelské jméno, heslo (nebo „Vygenerovat heslo“), roli (Technik/Admin), jméno, e-mail dle potřeby a uložte.
5. Ověřte, že nový uživatel je v seznamu. (Volitelně: změna role, reset hesla, deaktivace, pokud jsou v UI k dispozici.)
6. (Volitelně) Otevřete audit log a ověřte, že se zobrazí záznamy.

**Očekávaný výsledek:** Seznam se načte; nový uživatel se vytvoří a zobrazí. Akce (reset, role, audit) fungují dle popisu v rozhraní.

---

### 4.12 Zákazník – Moje objednávky

**Cíl:** Ověřit zobrazení objednávek přihlášeného zákazníka.

**Kroky:**

1. Přihlaste se jako zákazník (scénář 4.2 – vyhledání objednávky).
2. V menu klikněte na **„Moje objednávky“** (nebo na odkaz z úvodní stránky).
3. Zkontrolujte seznam objednávek; rozbalte jednu objednávku a ověřte zobrazení lyží a stavů úkolů.

**Očekávaný výsledek:** Zákazník vidí pouze své objednávky; detail objednávky je čitelný a odpovídá datům.

---

### 4.13 Odhlášení

**Cíl:** Ověřit odhlášení a návrat do anonymního stavu.

**Kroky:**

1. Přihlaste se jako admin, technik nebo zákazník.
2. Klikněte na své jméno (nebo ikonu uživatele) v horní liště.
3. V rozbalené nabídce klikněte na **„Odhlásit“**.

**Očekávaný výsledek:** Uživatel je odhlášen, menu obsahuje pouze „Domů“ a tlačítko „Přihlášení“. U zákazníka může na úvodní stránce zůstat sekce pro vyhledání objednávky.

---

### 4.14 Nastavení účtu a API dokumentace (admin/technik)

**Cíl:** Ověřit přístup k nastavení profilu a (pro admina) k API dokumentaci.

**Kroky:**

1. Přihlaste se jako admin nebo technik.
2. Klikněte na jméno v horní liště a v menu zvolte **„Nastavení“**. Ověřte, že se otevře formulář pro změnu hesla nebo profilu (pokud je implementován); zavřete ho.
3. Jako **admin** v uživatelském menu klikněte na **„API dokumentace“**. Ověřte, že se otevře nová záložka s dokumentací API (Swagger UI).

**Očekávaný výsledek:** Nastavení se otevře bez chyby; API dokumentace je dostupná pouze po přihlášení admina a zobrazí rozhraní Swagger.

---

## 5. Shrnutí scénářů pro bakalářku

Pro popis v bakalářce lze uvést např. tabulku:

| Č. | Scénář                              | Role     | Hlavní kroky |
|----|-------------------------------------|----------|----------------|
| 1  | Přihlášení admin/technik            | -        | Přihlášení → menu, jméno v liště |
| 2  | Přihlášení zákazníka                | -        | Číslo objednávky + telefon → Moje objednávky |
| 3  | Domovská stránka                    | A / T    | Dashboard, rychlé akce |
| 4  | Objednávky – seznam a filtry        | A / T    | Seznam, vyhledávání, dokončené |
| 5  | Objednávky – vytvoření               | A / T    | Nová objednávka, lyže, úkoly |
| 6  | Objednávky – detail a úkoly          | A / T    | Rozbalení, stavy úkolů |
| 7  | QR Scanner                          | A / T    | Skenování / ruční kód |
| 8  | Databáze lyží                       | A / T    | Seznam, přidat, upravit, smazat |
| 9  | Statistiky                          | A       | Grafy, změna období |
| 10 | Struktury a úpravy                  | A       | Přidat/smazat strukturu a úpravu |
| 11 | Správa uživatelů                    | A       | Seznam, přidat uživatele, audit |
| 12 | Zákazník – Moje objednávky          | Z       | Seznam a detail objednávek |
| 13 | Odhlášení                           | Vše     | Odhlásit → anonymní úvodní stránka |
| 14 | Nastavení a API dokumentace         | A / T   | Profil, Swagger (admin) |

*A = Admin, T = Technik, Z = Zákazník*

---

## 6. Hlášení chyb

Při nalezení chyby uveďte:

- **Scénář** (číslo nebo název)
- **Kroky** k reprodukci
- **Očekávané chování**
- **Skutečné chování** (včetně chybové hlášky nebo screenshotu)
- **Prohlížeč a verze** (volitelně)

Tím se zajistí konzistentní testování a využitelnost scénářů v bakalářské práci.
