# SSL – poznámky pro mě (Cloudflare Origin)

- **cert.pem** – sem dávám Cloudflare Origin Certificate, pojmenovaný přesně cert.pem (celý cert včetně řetězce). Tenhle soubor můžu mít v repu.
- **key.pem** – privátní klíč sem neukládám. Předávám ho jen přes GitHub Actions Secret, při deployi se zapisuje na server.

## Jak jsem přidal privátní klíč do GitHub Actions

1. V repu na GitHubu: Settings → Secrets and variables → Actions.
2. New repository secret, název: `SSL_PRIVATE_KEY_B64`.
3. Jako hodnotu jsem vložil klíč zakódovaný v base64 (celý obsah .pem souboru s klíčem).

   Na Macu v terminálu:
   ```bash
   base64 -i tvuj-cloudflare-origin-key.pem | pbcopy
   ```
   (nebo bez pbcopy a výstup zkopírovat ručně)

   Ten řetězec jsem dal jako hodnotu secretu `SSL_PRIVATE_KEY_B64`.

4. Při každém deployi se z toho secretu vytvoří na serveru soubor `docker/ssl/key.pem`. Certifikát je v repu, takže při změně serveru stačí znovu nasadit – klíč se doplní ze secretu.
