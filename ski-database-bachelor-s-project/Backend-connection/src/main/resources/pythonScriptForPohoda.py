import requests
import base64
import sys

# --- KONFIGURACE ---
WEB_API_URL = "https://tvoje-domena.cz/api/pohoda"
WEB_API_KEY = "moje-tajne-heslo-123"

POHODA_MSERVER_URL = "http://localhost:4444/xml"
POHODA_USER = "admin"
POHODA_PASS = "heslo"

# --- POMOCNÉ FUNKCE ---

def get_pohoda_auth_header():
    auth_str = f"{POHODA_USER}:{POHODA_PASS}"
    encoded = base64.b64encode(auth_str.encode('ascii')).decode('ascii')
    return {"STW-Authorization": f"Basic {encoded}"}

def sync_from_web_to_pohoda():
    """Vyzvedne data z webu a pošle je do lokální Pohody."""
    print("Kontrola objednávek pro export do Pohody...")
    headers = {"X-API-KEY": WEB_API_KEY}

    try:
        response = requests.get(f"{WEB_API_URL}/export", headers=headers)

        if response.status_code == 200:
            xml_to_pohoda = response.text
            print("Získány objednávky z webu, posílám do Pohody...")

            # Odeslání do mServeru
            p_res = requests.post(POHODA_MSERVER_URL,
                                  headers=get_pohoda_auth_header(),
                                  data=xml_to_pohoda.encode('utf-8'))

            if p_res.status_code == 200:
                print("Pohoda data přijala.")
                # Zde bys mohl zavolat confirm-export endpoint, pokud ho implementuješ
            else:
                print(f"Chyba mServeru: {p_res.status_code}")

        elif response.status_code == 204:
            print("Žádná data k exportu.")

    except Exception as e:
        print(f"Chyba při exportu do Pohody: {e}")

def sync_from_pohoda_to_web():
    """Vyzvedne data z Pohody a pošle je na web."""
    print("Kontrola nových objednávek v Pohodě...")

    # XML dotaz pro Pohodu (vyžádání seznamu objednávek)
    # V praxi doporučuji mít tento XML v souboru
    request_xml = """<?xml version="1.0" encoding="UTF-8"?>
    <base:requestPack xmlns:base="http://www.stormware.cz/schema/version_2/request.xsd"
                      xmlns:ord="http://www.stormware.cz/schema/version_2/order.xsd"
                      id="001" state="ok" version="2.0">
        <base:requestPackItem id="001" version="2.0">
            <ord:listOrderRequest version="2.0">
                <ord:requestOrder>
                    <ord:filter>
                        </ord:filter>
                </ord:requestOrder>
            </ord:listOrderRequest>
        </base:requestPackItem>
    </base:requestPack>"""

    try:
        # 1. Získání dat z Pohody
        p_res = requests.post(POHODA_MSERVER_URL,
                              headers=get_pohoda_auth_header(),
                              data=request_xml.encode('utf-8'))

        if p_res.status_code == 200:
            print("Data z Pohody získána, posílám na web...")

            # 2. Odeslání na Web API
            web_headers = {
                "X-API-KEY": WEB_API_KEY,
                "Content-Type": "application/xml"
            }
            w_res = requests.post(f"{WEB_API_URL}/import",
                                  headers=web_headers,
                                  data=p_res.content) # p_res.content zachová kódování

            if w_res.status_code == 200:
                print("Web data úspěšně přijal.")
            else:
                print(f"Web API chyba: {w_res.status_code} - {w_res.text}")

    except Exception as e:
        print(f"Chyba při importu z Pohody: {e}")

if __name__ == "__main__":
    # Spustíme obě strany synchronizace
    sync_from_web_to_pohoda()
    print("-" * 30)
    sync_from_pohoda_to_web()