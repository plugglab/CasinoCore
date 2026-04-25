# Setup licencji CasinoCore

Ten wariant zakłada:

- stronę lub panel admina, na którym tworzysz klucze,
- osobne API walidujące plugin,
- automatyczne przypięcie klucza do pierwszego serwera,
- automatyczną blokadę, gdy ten sam klucz pojawi się na innym serwerze.

## 1. Co już jest w pluginie

Plugin wysyła teraz do API:

- `licenseKey`
- `serverId`
- `instanceId`
- `machineFingerprint`
- `jarHash`
- `variant`
- `owner`

`instanceId` jest generowany lokalnie raz i zapisywany w:

- `plugins/CasinoCore/instance-id.txt`

`machineFingerprint` jest liczony z danych środowiska serwera i służy do wykrycia innej instalacji.

## 2. Co musisz mieć po stronie backendu

Minimalnie:

- domena lub subdomena, np. `api.twojadomena.pl`
- Node.js 20+ albo inny backend
- klucz RSA prywatny do podpisywania odpowiedzi
- baza danych albo plik JSON
- endpoint `POST /api/casinocore/validate`

Opcjonalnie:

- panel WWW do tworzenia i blokowania kluczy
- logi aktywacji
- webhook Discord przy auto-blokadzie

## 3. Struktura licencji w bazie

Minimalne pola:

```json
{
  "licenseKey": "cc_xxx",
  "owner": "Gabri",
  "status": "active",
  "variant": "protected",
  "createdAt": 1777132800,
  "expiresAt": 1779724800,
  "tokenLifetimeSeconds": 21600,
  "boundServerId": "",
  "boundInstanceId": "",
  "boundMachineFingerprint": "",
  "validationCount": 0,
  "blockReason": ""
}
```

Statusy:

- `active`
- `blocked`
- `expired`

## 4. Generowanie kluczy RSA

Windows OpenSSL:

```powershell
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem
```

`private.pem` trzymasz tylko na backendzie.

`public.pem` wrzucasz do `config.yml` pluginu.

## 5. Uruchomienie przykładowego backendu

Pliki:

- `examples/license-api-node.mjs`
- `examples/license-storage.json`

Start:

```powershell
$env:CASINOCORE_PRIVATE_KEY_PEM = Get-Content .\private.pem -Raw
$env:CASINOCORE_LICENSE_DB = ".\examples\license-storage.json"
node .\examples\license-api-node.mjs
```

Tworzenie licencji:

```powershell
$body = @{
  owner = "Gabri"
  licenseKey = "cc_test_001"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8080/api/casinocore/licenses" `
  -ContentType "application/json" `
  -Body $body
```

## 6. Jak podłączyć to do strony

Twoja strona nie powinna walidować pluginu bezpośrednio. Zrób to tak:

1. Panel WWW tworzy klucz w bazie.
2. Panel WWW pokazuje klientowi:
   klucz, datę wygaśnięcia, status.
3. Plugin łączy się tylko z endpointem walidacyjnym.
4. Panel WWW może ręcznie:
   blokować klucz, resetować przypięcie, przedłużać ważność.

Najważniejsze akcje panelu:

- `create license`
- `extend expiry`
- `block license`
- `reset binding`
- `show activations`

## 7. Jak działa pierwsza aktywacja

Przy pierwszym odpaleniu plugin:

1. wysyła klucz,
2. wysyła `serverId`,
3. wysyła `instanceId`,
4. wysyła `machineFingerprint`.

API zapisuje te wartości jako przypięcie licencji.

Od tego momentu ten sam klucz musi przychodzić z dokładnie tym samym zestawem danych.

## 8. Kiedy licencja jest blokowana automatycznie

API blokuje klucz, gdy:

- `serverId` się różni,
- `instanceId` się różni,
- `machineFingerprint` się różni.

To daje Ci trzy warstwy kontroli:

- ręczny identyfikator klienta,
- trwały identyfikator lokalnej instancji pluginu,
- fingerprint środowiska serwera.

## 9. Co klient ma ustawić w pluginie

W `plugins/CasinoCore/config.yml`:

```yaml
license:
  key: "cc_test_001"
  owner: "Gabri"
  server-id: "gabri-survival-01"
  instance:
    file: "instance-id.txt"
  api:
    validate-url: "https://api.twojadomena.pl/api/casinocore/validate"
    timeout-ms: 10000
    refresh-interval-minutes: 15
    offline-grace-minutes: 1440
    user-agent: "CasinoCore-License/1.0"
  cache:
    file: "license-cache.yml"
  public-key:
    lines:
      - "-----BEGIN PUBLIC KEY-----"
      - "TU_WKLEJ_TUTAJ_LINIE"
      - "-----END PUBLIC KEY-----"
```

## 10. Co warto dodać później

- HMAC nagłówka między pluginem a API
- rate limiting po IP
- webhook Discord przy `blocked`
- lista aktywacji z timestampami
- reset przypięcia tylko z panelu admina
- Cloudflare / reverse proxy przed API

## 11. Ważne ograniczenie

Tego typu ochrona nie jest niełamliwa. Da się ją obejść przy pełnej ingerencji w kod pluginu albo przy bardzo świadomym kopiowaniu środowiska.

Praktyczny cel jest inny:

- utrudnić zwykłe udostępnianie pluginu,
- szybko wykryć ponowne użycie klucza,
- mieć centralną możliwość blokady.
