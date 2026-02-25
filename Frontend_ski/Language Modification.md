# Přidání dalšího jazyka (Language Modification)

Návod, jak do aplikace přidat nový jazyk (např. němčinu).

---

## 1. Vytvořit soubor překladů

Zkopírujte existující locale soubor (např. `src/locales/en.json`) do nového souboru s kódem jazyka:

- **Příklad pro němčinu:** `src/locales/de.json`

Doplňte všechny klíče odpovídajícími překlady. Struktura musí zůstat stejná jako v `cs.json` / `en.json` (sekce `common`, `status`, `priority`, `nav`, `home`, `orderLookup`, atd.).

---

## 2. Rozšířit LanguageContext

V souboru **`src/contexts/LanguageContext.tsx`**:

### 2.1 Rozšířit typ `Locale`

```ts
export type Locale = 'cs' | 'en' | 'de';
```

### 2.2 Přidat jazyk do pole `locales`

```ts
const locales: Locale[] = ['cs', 'en', 'de'];
```

### 2.3 Přidat načtení překladů v `loadTranslations`

V funkci `loadTranslations` přidejte větev pro nový jazyk a import příslušného JSON:

```ts
async function loadTranslations(locale: Locale): Promise<Translations> {
  if (locale === 'cs') {
    const mod = await import('@/locales/cs.json')
    return mod.default as Translations
  }
  if (locale === 'de') {
    const mod = await import('@/locales/de.json')
    return mod.default as Translations
  }
  const mod = await import('@/locales/en.json')
  return mod.default as Translations
}
```

### 2.4 (Volitelně) Úprava `loadLocale`

Funkce `loadLocale()` čte z `localStorage`; pokud chcete podporovat nový jazyk i při prvním načtení, upravte kontrolu:

```ts
if (stored === 'cs' || stored === 'en' || stored === 'de') return stored
```

---

## 3. Přidat vlajku do navigace

V souboru **`src/components/Navigation.tsx`** rozšířte objekt **`FLAGS`**:

```ts
const FLAGS: Record<Locale, string> = { cs: '🇨🇿', en: '🇬🇧', de: '🇩🇪' }
```

(Případně jiná vlajka podle potřeby.)

---

## 4. Překlad názvu jazyka v menu

V **`src/locales/cs.json`** a **`src/locales/en.json`** (a v novém `de.json`) přidejte do sekce `language` klíč pro název jazyka:

**cs.json:**
```json
"language": {
  "cs": "Čeština",
  "en": "English",
  "de": "Deutsch"
}
```

**en.json:**
```json
"language": {
  "cs": "Čeština",
  "en": "English",
  "de": "Deutsch"
}
```

V **`de.json`** stejnou strukturu s německými názvy podle potřeby.

---

## Shrnutí kroků

| Krok | Soubor / místo | Úprava |
|------|----------------|--------|
| 1 | `src/locales/de.json` | Nový soubor – zkopírovat z `en.json` a přeložit |
| 2a | `LanguageContext.tsx` | `Locale = 'cs' \| 'en' \| 'de'` |
| 2b | `LanguageContext.tsx` | `locales = ['cs', 'en', 'de']` |
| 2c | `LanguageContext.tsx` | V `loadTranslations` přidat `locale === 'de'` a import `@/locales/de.json` |
| 2d | `LanguageContext.tsx` | V `loadLocale` povolit `stored === 'de'` |
| 3 | `Navigation.tsx` | `FLAGS` doplnit o `de: '🇩🇪'` |
| 4 | `cs.json`, `en.json`, `de.json` | Sekce `language` doplnit klíč `"de": "Deutsch"` |

Po těchto úpravách se nový jazyk objeví v menu výběru jazyka (vlajka vpravo nahoře) a po výběru se aplikace zobrazí v novém jazyce.
