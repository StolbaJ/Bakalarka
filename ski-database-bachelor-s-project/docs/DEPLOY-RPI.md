# Deploy na Raspberry Pi 5 + GitHub Actions Runner + Cloudflare Tunnel

Návod: jak nasadit aplikaci na vlastní RPi5 pomocí **self-hosted GitHub Actions runneru** a vystavit ji přes **Cloudflare Tunnel** (bez otvírání portů v routeru).

---

## 1. Co se změní oproti EC2

| EC2 (teď) | RPi5 (nově) |
|-----------|--------------|
| Runner běží na GitHubu (`ubuntu-latest`) | Runner běží **na RPi5** |
| Workflow: checkout → SCP na server → SSH + docker na serveru | Workflow: checkout **na RPi5** → docker **lokálně na RPi5** |
| Přístup: veřejná IP / doména | Přístup: **Cloudflare Tunnel** → doména, bez otevřených portů |

**Ano – když je runner na Malině, workflow tam lokálně checkoutne kód a spustí Docker na té samé Malině.** SCP a SSH do serveru už nepotřebuješ.

---

## 2. Příprava Raspberry Pi 5

- Nainstalovaný **64bit OS** (např. Raspberry Pi OS 64-bit).
- Připojení k síti (Ethernet doporučeno).
- SSH přístup (aby sis mohl nastavit vše z počítače).

```bash
# Aktualizace systému
sudo apt update && sudo apt upgrade -y

# Docker
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
# Odhlásit se a znovu přihlásit (nebo restart), aby platila skupina docker
```

Ověření: `docker run hello-world`

---

## 3. Self-hosted GitHub Actions Runner na RPi5

### 3.1 Stažení a konfigurace runneru

Na GitHubu: **Repo → Settings → Actions → Runners → New self-hosted runner.**

Vyber **Linux** a **ARM64** (RPi5 je 64-bit ARM). GitHub ti ukáže přesné příkazy, typicky:

```bash
# Na RPi5 (v adresáři, kde chceš runnera, např. home)
mkdir -p ~/actions-runner && cd ~/actions-runner

# Stáhnout (použij odkaz z GitHubu pro tvůj repozitář!)
curl -o actions-runner-linux-arm64-2.321.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.321.0/actions-runner-linux-arm64-2.321.0.tar.gz
tar xzf ./actions-runner-linux-arm64-2.321.0.tar.gz

# Konfigurace – token a jméno z GitHubu
./config.sh --url https://github.com/TVOJE_ORGANIZACE/TVOJ_REPO --token TOKEN_Z_GITHUBU
# Jméno runnera: např. rpi5
```

**Token** vezmeš z té stránky „New self-hosted runner“ (jednorázový, po `./config.sh` už není potřeba).

### 3.2 Runner jako služba (autostart po startu RPi)

```bash
sudo ./svc.sh install
sudo ./svc.sh start
```

Kontrola: `sudo ./svc.sh status`

- Služba: `~/actions-runner` → běží pod uživatelem, který ji nainstaloval. Pro Docker musí mít tento uživatel v skupině `docker` (viz výše).

---

## 4. Úprava workflow – běh na self-hosted runneru

Workflow už **nekopíruje** soubory na server přes SCP/SSH. Na RPi5 se po spuštění workflow:

1. Provede **Checkout** (kód je v workspace runnera).
2. Vytvoří se **.env** a **docker/ssl/key.pem** ze secrets.
3. Spustí se **Docker Compose** v tom samém adresáři.

V repozitáři je upravený soubor `.github/workflows/main.yml`: `runs-on: self-hosted` a kroky bez SCP/SSH, přímo `docker compose` v workspace. Pokud máš víc runnerů a chceš cílit jen na RPi, můžeš použít `runs-on: [self-hosted, linux, ARM64]`. Pokud je uživatel, pod kterým runner běží, ve skupině `docker`, můžeš v workflow odstranit `sudo` u příkazů docker.

---

## 5. Cloudflare Tunnel – přístup z venku přes doménu

Lidé se na aplikaci připojují přes **doménu** (např. `bezkyservis.xyz`). Traffic jde: **Uživatel → Cloudflare → Tunnel (cloudflared na RPi5) → Nginx na RPi5**. Na routeru **nepotřebuješ otvírat porty 80/443**.

### 5.1 Tunnel v Cloudflare dashboardu

1. **Cloudflare Dashboard** → tvá doména → **Zero Trust** (nebo přímo **Networks → Tunnels**).
2. **Create a tunnel** → **Cloudflared**.
3. Pojmenuj tunnel (např. `rpi5-app`).
4. **Install connector**: vyber **Linux**, **Debian/Ubuntu** – zobrazí se příkaz na stažení a instalaci `cloudflared` na RPi.

### 5.2 Instalace cloudflared na RPi5

Na RPi5 (podle návodu z dashboardu, nebo např.):

```bash
# Stáhnout cloudflared pro ARM64
curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb
sudo dpkg -i cloudflared.deb
```

### 5.3 Přihlášení a vytvoření tunelu (jednorázově)

V dashboardu při vytváření tunelu můžeš zvolit:

- **Quick Tunnel** – Cloudflare ti dá náhodnou URL (pro test).
- **Nebo** „Install connector“ a pak **Public Hostname** – tam nastavíš svou doménu.

Doporučený postup pro produkci:

1. V kroku **Configure tunnel** přidej **Public Hostname**:
   - **Subdomain**: např. prázdné (pokud chceš `bezkyservis.xyz`) nebo např. `app` → `app.bezkyservis.xyz`.
   - **Domain**: tvá doména (např. `bezkyservis.xyz`).
   - **Service type**: **HTTPS** (nebo HTTP, viz níže).
   - **URL**: kde na RPi5 běží aplikace:
     - Pokud Nginx poslouchá na **443** (SSL): `https://localhost:443` a zaškrtni **No TLS Verify** (Cloudflare přijme tvůj Origin cert).
     - Pokud chceš posílat na **HTTP**: v Nginx můžeš mít jen port 80 a tady zvolit **HTTP** → `http://localhost:80`; v Cloudflare nastav SSL na **Flexible** (šifrování jen mezi uživatelem a Cloudflare).

2. Ulož a **Deploy** – dashboard ti ukáže příkaz na spuštění connectoru (token v URL). Ten můžeš spustit ručně nebo jako službu.

### 5.4 Cloudflared jako služba (autostart)

Po vytvoření tunelu v dashboardu můžeš stáhnout „install“ příkaz, který nainstaluje connector jako službu. Alternativně ručně:

```bash
# Spustit connector s tokenem z dashboardu (nahraď TOKEN)
sudo cloudflared service install TOKEN
sudo systemctl start cloudflared
sudo systemctl enable cloudflared
```

Token je jednorázově vygenerovaný v Cloudflare při vytváření tunelu („Run the connector“).

### 5.5 Shrnutí tunelu

- **Vstup**: Uživatel → `https://bezkyservis.xyz` (nebo tvá subdoména).
- **Cloudflare**: terminuje SSL, ochrana DDoS, cache dle pravidel.
- **Tunnel**: cloudflared na RPi5 posílá traffic na `https://localhost:443` (nebo `http://localhost:80`).
- **Nginx** na RPi5: stále používá tvůj stávající SSL (Origin cert) na 443, nebo jen HTTP na 80 – podle toho, jak jsi nastavil Public Hostname.

---

## 6. GitHub Secrets (beze změny)

Ponech v repozitáři tyto **Actions secrets** (bez EC2):

- `JWT_SECRET`
- `RESEND_API_KEY`
- `SSL_PRIVATE_KEY_B64` (base64 privátního klíče Cloudflare Origin certifikátu)

**Odstranit** (už nepotřebuješ): `EC2_HOST`, `EC2_USERNAME`, `EC2_SSH_KEY`.

---

## 7. Pořadí kroků (checklist)

1. Na RPi5: Docker + uživatel ve skupině `docker`.
2. Na RPi5: nainstalovat a zaregistrovat **GitHub Actions runner** (Linux ARM64), spustit jako službu.
3. V repu: workflow přepnutý na `runs-on: self-hosted` a bez SCP/SSH (viz upravený `main.yml`).
4. V GitHubu: smazat EC2 secrets, ostatní secrets nechat.
5. V Cloudflare: vytvořit **Tunnel**, přidat **Public Hostname** na tvou doménu → služba na RPi5 (HTTPS localhost:443 nebo HTTP localhost:80).
6. Na RPi5: nainstalovat **cloudflared** a spustit connector (token z dashboardu), ideálně jako službu.
7. Spustit workflow (např. **workflow_dispatch**) – deploy poběží na RPi5 a po dokončení bude aplikace dostupná přes doménu přes Cloudflare Tunnel.

---

## 8. Rychlé ověření

- **Runner**: v repo **Settings → Actions → Runners** by měl být runner „rpi5“ (nebo jak jsi ho pojmenoval) zelený.
- **Aplikace**: po deployi na RPi5 zkus lokálně: `curl -k https://localhost:443` nebo `curl http://localhost:80`.
- **Z venku**: v prohlížeči otevři `https://bezkyservis.xyz` (nebo tvá subdoména) – měl by jít traffic přes Cloudflare Tunnel.

Kdybys chtěl místo „workflow_dispatch“ znovu deploy při každém pushi na `main`, v `main.yml` odkomentuj `push: branches: [ main ]` a odeber nebo uprav `workflow_dispatch`.
