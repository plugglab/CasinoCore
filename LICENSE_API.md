# CasinoCore Protected License API

Ten dokument opisuje kontrakt API dla builda `protected`.

## Co wysyła plugin

Plugin wysyła `POST` na `license.api.validate-url` z JSON:

```json
{
  "product": "CasinoCore",
  "variant": "protected",
  "serverId": "srv-001",
  "owner": "Gabri",
  "licenseKey": "YOUR-LICENSE-KEY",
  "jarHash": "sha256-of-current-jar",
  "instanceId": "persistent-local-instance-id",
  "machineFingerprint": "sha256-of-server-environment",
  "nonce": "random-base64url",
  "pluginVersion": "1.0-protected",
  "timestamp": 1777132800
}
```

## Co ma zwrócić API

HTTP `200` z JSON:

```json
{
  "ok": true,
  "payload": "BASE64_OF_PLAIN_TEXT_PAYLOAD",
  "signature": "BASE64_RSA_SIGNATURE"
}
```

Przy odrzuceniu:

```json
{
  "ok": false,
  "error": "license expired"
}
```

## Format `payload` przed Base64

```text
product=CasinoCore
owner=Gabri
serverId=srv-001
variant=protected
jarHash=sha256-of-current-jar
nonce=the-same-nonce-from-request
licenseKeyHash=sha256-of-license-key
issuedAt=1777132800
refreshAfter=1777136400
expiresAt=1777140000
```

## Jak działa blokada przy użyciu gdzie indziej

Rekomendowany model:

1. Tworzysz klucz na swojej stronie.
2. Klucz ma status `active`, ale nie ma jeszcze przypiętego `serverId`, `instanceId` ani `machineFingerprint`.
3. Przy pierwszej poprawnej aktywacji API zapisuje:
   `boundServerId`, `boundInstanceId`, `boundMachineFingerprint`
4. Każde kolejne sprawdzenie musi mieć dokładnie te same wartości.
5. Jeżeli ten sam klucz przyjdzie z innym `serverId`, `instanceId` albo `machineFingerprint`, API automatycznie zmienia status licencji na `blocked`.
6. Plugin przy następnym odświeżeniu dostaje błąd i sam się wyłącza.

To jest prostsze i mocniejsze niż ręczne przypinanie tylko do `server-id`.

## Podpis

Podpisuj oryginalny string `payload` zakodowany w Base64 algorytmem `SHA256withRSA`.

Plugin ma tylko klucz publiczny i lokalnie weryfikuje podpis.

## Minimalna logika backendu

1. Odbierz request.
2. Sprawdź `product == CasinoCore`.
3. Znajdź licencję po `licenseKey`.
4. Sprawdź `status == active`.
5. Sprawdź, czy licencja nie wygasła.
6. Jeśli to pierwsze użycie, przypnij:
   `serverId`, `instanceId`, `machineFingerprint`
7. Jeśli to nie pierwsze użycie, porównaj te pola z zapisanymi.
8. Przy różnicy ustaw status `blocked`.
9. Zbuduj `payload`.
10. Podpisz `payload`.
11. Zwróć `ok=true`.

## Co ustawiasz w configu pluginu

```yaml
license:
  key: "YOUR-LICENSE-KEY"
  owner: "Gabri"
  server-id: "srv-001"
  instance:
    file: "instance-id.txt"
  api:
    validate-url: "https://twoja-domena.pl/api/casinocore/validate"
    timeout-ms: 10000
    refresh-interval-minutes: 15
    offline-grace-minutes: 1440
    user-agent: "CasinoCore-License/1.0"
  cache:
    file: "license-cache.yml"
  public-key:
    lines:
      - "-----BEGIN PUBLIC KEY-----"
      - "YOUR_BASE64_PUBLIC_KEY_LINE_1"
      - "YOUR_BASE64_PUBLIC_KEY_LINE_2"
      - "-----END PUBLIC KEY-----"
```

## Offline

Po poprawnej walidacji plugin zapisuje podpisany token do `license-cache.yml`.

Jeżeli API chwilowo nie działa, plugin akceptuje cache do:

`expiresAt + offline-grace-minutes`

Po tym czasie build `protected` się wyłączy.
