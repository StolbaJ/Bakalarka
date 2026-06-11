# Analýza stop po AI asistenci v repozitáři BP

**Datum:** květen 2026  
**Rozsah:** kořen `BP/` (Backend_ski, Frontend_ski, Backend-connection, docker, docs) — bez duplicitní složky `ski-database-bachelor-s-project/` (obsah je stejný).

## Jak číst tento dokument

- **Pravděpodobnost** = odhad z komentářů, stylu dokumentace a konzistence jazyka — **ne důkaz** použití konkrétního nástroje.
- **Žádný soubor v repu neobsahuje** zmínku ChatGPT, Copilot, Cursor, Claude apod.
- Škála: **🔴 vysoká** (≈ 60–90 %) · **🟠 střední–vysoká** (≈ 40–60 %) · **🟡 střední** (≈ 20–40 %) · **🟢 nízká** (≈ 5–20 %) · **⚪ velmi nízká / lidský původ** (< 5 %)

---

## Shrnutí podle úrovní

| Úroveň | Počet položek (unikátní cesty) | Typické znaky |
|--------|-------------------------------|---------------|
| 🔴 Vysoká | 1 | Explicitní „vygenerováno“, systematické tabulky všech atributů |
| 🟠 Střední–vysoká | 6 | Tutorial / krok-za-krokem, směs CZ+EN v jednom souboru |
| 🟡 Střední | 12 | Formální README šablona, anglické testy/komentáře u českého projektu |
| 🟢 Nízká | 15+ | Doménové Javadoc, vysvětlení edge cases — může být AI i zkušený vývojář |
| ⚪ Velmi nízká (lidské) | 10+ | 1. osoba, překlepy, hovorová čeština, osobní pojmenování |

---

## 🔴 Vysoká pravděpodobnost asistence / generování

| Soubor | Důvod | Poznámka |
|--------|--------|----------|
| `Backend_ski/datovemodel.md` | Na konci: *„Dokument vygenerován z JPA entit…“*; uniformní tabulky Nullable/Unique u každého atributu | **Jediný explicitní „generated“ text v celém repu.** U obhajoby uvést nástroj/postup (skript, IDE, asistent). |

---

## 🟠 Střední–vysoká pravděpodobnost

| Soubor | Důvod | Indikátory |
|--------|--------|------------|
| `Frontend_ski/Language Modification.md` | Návod ve stylu tutorialu: kroky 1–4, hotové bloky kódu, tabulka „Shrnutí kroků“ | Typický výstup „jak přidat feature“ — AI nebo ruční how-to |
| `Frontend_ski/src/components/QRScanner.tsx` | ~15 komentářů krok za krokem („Získat stream“, „Spustit scan loop“, „Ignorovat chyby…“) | Často z tutoriálů nebo AI; vysvětluje každý řádek algoritmu |
| `Frontend_ski/src/app/page.tsx` | **Směs jazyků:** CZ komentář (ř. 63) + EN (ř. 94) ve stejném souboru | Nekonzistence naznačuje různý zdroj (asistence / copy-paste) |
| `Frontend_ski/src/app/users/page.tsx` | EN: `// error handled by api`, `// Keep modal open…`; hardcoded CZ text místo `t()` u generování hesla | Anglické „placeholder“ komentáře + nedodělaná i18n |
| `Frontend_ski/src/components/SetHtmlLang.tsx` | EN JSDoc: „for a11y and SEO“ | Běžný Next.js vzor; často z docs/AI |
| `Backend-connection/HELP.md` | Opakované „Ujistěte se, že…“, odrážkové shrnutí, odkazy na oficiální docs na konci | Formální šablona; může být rozšíření Spring Initializr + AI |

---

## 🟡 Střední pravděpodobnost

| Soubor | Důvod |
|--------|--------|
| `Frontend_ski/src/lib/api.ts` | Sekční komentáře (`// Auth endpoints`, `// Ski endpoints`); mix CZ JSDoc a EN `// ignore` |
| `Frontend_ski/src/components/PaginationControls.tsx` | Kompletní JSDoc u všech props (CZ) — dobrá praxe, občas AI doplní všechny props najednou |
| `Frontend_ski/src/components/SkiItem.tsx` | `// Default card variant - kompaktnější` (EN + CZ v jedné větě) |
| `Frontend_ski/src/components/ServiceTaskItem.tsx` | `// Table variant (default)` — anglicky |
| `Frontend_ski/src/contexts/AuthContext.tsx` | CZ JSDoc u metod session/login — kvalitní, mírně „učebnicový“ styl |
| `Backend_ski/src/main/java/com/ski/inventory/service/JwtService.java` | Javadoc u každé metody (13 bloků) — systematické, méně doménově specifické než jinde |
| `Backend_ski/src/main/java/com/ski/inventory/config/OpenApiConfig.java` | Dlouhé popisy security v OpenAPI + customizer z `@PreAuthorize` — složitá konfigurace, často asistovaná |
| `Backend_ski/src/test/java/com/ski/inventory/security/SecurityFilterChainIntegrationTest.java` | EN komentáře v testu + překlep „Integrace test“ v class Javadoc; dlouhý `@SpringBootTest` exclude list |
| `Backend_ski/src/test/java/com/ski/inventory/security/JwtAuthenticationFilterTest.java` | `// The existing authentication should be preserved (not overwritten)` |
| `Backend_ski/src/test/java/com/ski/inventory/monitoring/ServerErrorRecorderTest.java` | `// Most recent should be /path/24 (first in reversed list)` |
| `Backend_ski/src/test/java/com/ski/inventory/controller/UserControllerTest.java` | `// showPasswordOnReset defaults to false, so newPassword should be null` |
| `Backend-connection/src/main/java/.../service/PohodaOrderService.java` | Krokové komentáře u parsování XML (`// Hlavička`, `// Partner`) — částečně rozpracované (TODO) |

---

## 🟢 Nízká pravděpodobnost (spíš ruční práce, případně lehká asistence)

Doménově specifické, vysvětluje **proč** (edge cases), ne jen **co** kód dělá:

| Oblast | Příklady souborů |
|--------|------------------|
| Security / auth | `SecurityConfig.java`, `AuthCookieService.java`, `SwaggerCookieAuthenticationFilter.java`, `Argon2PasswordEncoder.java` |
| Monitoring | `MonitoringService.java`, `ServerErrorRecorder.java`, `ServerErrorRecordingFilter.java` |
| Objednávky / e-mail | `OrderViewTokenService.java`, `OrderCreatedNotificationService.java`, `EmailService.java` |
| Repozitáře (SQL popis) | `OrderTaskRepository.java`, `ServiceTaskItemRepository.java` |
| Inicializace | `DataInitializer.java`, `MailConfig.java`, `EnvFileLoader.java` |
| FE business | `orders/page.tsx` (velký, ale doménové UI), `api.ts` (typy a endpointy) |
| Migrace Flyway | `Backend_ski/src/main/resources/db/migration/V*.sql` — krátké CZ poznámky (cron, sync mobil↔PC) |
| Testy integrace | Většina `*Test.java` mimo výše uvedené EN komentáře |

---

## ⚪ Velmi nízká — silné znaky lidského původu

| Soubor | Proč působí lidsky |
|--------|---------------------|
| `README.md` (kořen) | Text zadání BP, překlep „s s napojením“ |
| `SPUSTENI.md` | Praktický provoz, tabulka GitHub secretů |
| `testing.md` | „NYNÍ ODSTRANĚNO VYUŽÍT TESTY Z BP“ — meta poznámka při přesunu |
| `Backend_ski/README.md` | Překlep `r` na konci souboru |
| `Backend_ski/jmeter/README-JMETER.md` | „**Mám** tři scénáře“ — 1. osoba |
| `docs/DEPLOY-RPI.md` | „**Malina**“, hovorová čeština |
| `docker/ssl/README.md` | „poznámky **pro mě**“, „**Jak jsem přidal**“ |
| `.github/workflows/main.yml` | Job „Malina Docker Deploy“, komentáře o RPi |
| `docker/README.md` | Stručný provozní přehled dev vs prod |
| `Backend-connection/.../SecurityConfig.java` | CZ komentáře k doméně („stroj už tebe nevolá“) |

---

## Šablony třetích stran (není AI autora projektu)

| Soubor | Text |
|--------|------|
| `docker/frontend/Dockerfile` | „Automatically leverage output traces…“ — **Next.js** Docker šablona |
| `Backend_ski/mvnw`, `Backend-connection/mvnw` | Komentáře z Apache Maven wrapperu |
| `Backend_ski/lombok.config` | Standardní JaCoCo + Lombok |

---

## Křížové vzory (celý repozitář)

| Vzor | Význam |
|------|--------|
| **CZ dokumentace + EN komentáře ve FE** | Nejsilnější nepřímý signál — spíš část frontendu než celý backend |
| **Explicitní „vygenerováno“ jen u datového modelu** | Dokumentace ano, aplikační kód ne |
| **Překlepy a 1. osoba v ops docs** | Proti teorii „vše napsal ChatGPT“ |
| **Doménová čeština** (struktura, Pohoda, Resend, Bezkyservis) | Konzistentní s ručním vývojem na reálném zadání |

---

## Doporučení před obhajobou (bez mazání historie)

1. **`datovemodel.md`** — doplnit jednu větu *jak* vznikl (ručně / export z IDE / asistent + ruční korekce).
2. **Sjednotit jazyk komentářů** ve FE (ideálně česky, nebo konzistentně anglicky) — zejména `page.tsx`, `users/page.tsx`, `QRScanner.tsx`.
3. **Dokončit i18n** — hardcoded řetězec v `users/page.tsx` („Heslo bude vygenerováno…“) přes `t()`.
4. **Opravit drobnosti** — `Backend_ski/README.md` (přebytečné `r`), Javadoc „Integrační test“ v `SecurityFilterChainIntegrationTest`.
5. **Nepředpokládat, že komise hledá AI** — spíš konzistenci a vlastnictví rozhodnutí; tento seznam je pro tebe, ne jako přiznání.

---

## Rychlý checklist pro revizi souboru

Při kontrole libovolného souboru se zeptej:

- [ ] Je v souboru směs CZ a EN komentářů?
- [ ] Vysvětlují komentáře zřejmý kód řádek po řádku?
- [ ] Je dokumentace tabulkově „příliš úplná“ bez projektové specificity?
- [ ] Je tam slovo *generated* / *vygenerováno* v meta smyslu (ne business logika hesla)?
- [ ] Sedí styl se zbytkem stejné složky (BE vs FE vs docker)?

---

*Tento dokument je interní analýza repozitáře; nenahrazuje akademickou etiku fakulty — při pochybnostech se řiď pravidly školy k použití AI.*
