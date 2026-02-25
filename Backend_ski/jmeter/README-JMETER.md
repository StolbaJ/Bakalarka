# JMeter testy – Ski Inventory API

Zatěžovací testy pro backend. Mám tři scénáře: smoke (rychlý), standardní load, dlouhý (10 min).

## Co nainstalovat

- **Java 11+**
- **JMeter 5.x** – na Macu: `brew install jmeter`

## Jak spustit

Backend musí běžet (např. `docker compose -f docker-compose.dev.yml up -d` v `docker/`). Pak z `Backend_ski/jmeter`:

```bash
# Rychlý smoke (pár sekund)
jmeter -n -t ski-inventory-smoke.jmx -l results.jtl -e -o report

# Standardní load (desítky sekund)
jmeter -n -t ski-inventory-load.jmx -l results.jtl -e -o report

# Dlouhý test – 10 minut zátěže
jmeter -n -t ski-inventory-long.jmx -l results.jtl -e -o report
```

`-l results.jtl` = kam se zapíšou vzorky, `-e -o report` = vygeneruje HTML report do složky `report/`.

Jiný host/port: `-JBASE_URL=1.2.3.4 -JPORT=8080`

## Jak se kouknout na výsledky

Lokálně po běhu:

```bash
open report/index.html
```

V prohlížeči máš grafy, tabulky a přehled chyb. `results.jtl` je surový výstup (CSV), můžeš z něj později znovu vygenerovat report.

## Výsledky z EC2 (nebo jiného serveru) k sobě

Když test běží na EC2 přes SSH, výsledky zůstanou na serveru. Stáhni si je k sobě:

```bash
# Stáhnout celou složku jmeter včetně report/ a results.jtl
scp -r uživatel@ec2-adresa:/cesta/k/Backend_ski/jmeter ./jmeter-from-ec2

# Pak u sebe otevři report
open jmeter-from-ec2/report/index.html
```

Příklad s konkrétní cestou a klíčem:

```bash
scp -i ~/.ssh/muj-key.pem -r ec2-user@ec2-xx-xx-xx-xx.eu-central-1.compute.amazonaws.com:~/BP/Backend_ski/jmeter ./jmeter-ec2
open jmeter-ec2/report/index.html
```

Stačí složka `report/` (a volitelně `results.jtl` pro archiv). Když na EC2 nemáš vygenerovaný report, stáhni jen `results.jtl` a vygeneruj report u sebe:

```bash
# Na EC2 po testu máš jen results.jtl
# Stáhneš ho:
scp -i ~/.ssh/muj-key.pem ec2-user@ec2-xxx:~/BP/Backend_ski/jmeter/results.jtl ./

# U sebe vygeneruješ report (potřebuješ JMeter):
jmeter -g results.jtl -o report
open report/index.html
```

`jmeter -g results.jtl -o report` vytvoří HTML report z existujícího .jtl souboru.

## Soubory

| Soubor | Popis |
|--------|--------|
| `ski-inventory-smoke.jmx` | 2 vlákna, 2 kola – ověří že API odpovídá |
| `ski-inventory-load.jmx` | 10+25 vláken, několik kol – střední zátěž |
| `ski-inventory-long.jmx` | 20+40 vláken, **10 minut** – dlouhá zátěž |

Connection refused = backend neběží. 401 na `/api/technician/*` = špatný login nebo chybějící JWT (ověř admin/admin123).
