# Shiftora — audit koda i kvalitete (0.9.0)

Ovaj dokument sadrži pregled izvorne strukture, pronađene greške, stvarne izmjene i provjere. CI rezultat vrijedi samo za točan commit, a nije jamstvo odsutnosti svih nepoznatih grešaka.

## Pregledani dijelovi

| Područje | Rezultat |
|---|---|
| Android manifest / dozvole / offline pristup | Bez potrebe za INTERNET, lokacijom, oglašavanjem, kontaktima; `allowBackup=false`, cleartext promet onemogućen |
| Target SDK / AndroidX / Gradle | Migracija na API 36, AGP 8.10.1, Gradle 8.11.1 |
| Lokalna baza i rotacije | SQLite schema v2 s nedestruktivnom migracijom, recoverable archive, rotacije koje preskaču aktivne i arhivirane datume, serializirane operacije |
| Obračun minuta i dodataka | Jedinstveni `ShiftTimeline`, provjera preklapanja kategorija, DST i mjesečne granice |
| JSON / CSV datoteke | UTF-8, ograničenja veličine, zaštita od CSV formula, JSON v3 koji čuva arhivu; import je transakcijsko spajanje bez brisanja |
| Preferencije i postavke plaće | Popravak NaN/Infinity, pokvarenih tipova i raspona noćnog rada |
| UI niti i veliki dokumenti | SAF i dekodiranje sigurnosne kopije premješteni na IO dispatcher |
| Dead-code i duplikati | Uklonjen debug fixture iz glavne aktivnosti, neupotrijebljeni `icons`/Canvas/Stroke importi i nepotrebne UI tooling ovisnosti |
| Privatnost u aplikaciji | Dostupna lokalna pravila, nema praćenja ili oglasa |
| Objavljivanje | Preview debug APK + unsigned AAB; zaseban ručni workflow za potpisani Play AAB kada postoje secrets |

## Regresijski testovi

JVM: smjene i pauze, prelazak preko ponoći, promjene DST, hrvatski blagdani, obračun plaće, grafovi i indikatori, backup validacija, CSV formula injection. Android instrumentacijski testovi: navigacija, SQLite CRUD, rotacije, nevažeće preferencije i realne snimke zaslona na emulatoru.

Pokreću se:
`./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:bundleRelease`

## Granice trajnosti podataka

- Niti jedna automatska rotacija, pregled mjeseca ili JSON uvoz ne smije obrisati već spremljenu smjenu.
- Arhiviranje uklanja unos iz aktivnog kalendara **samo uz potvrdu**, ali ga zadržava u lokalnoj tablici za oporavak.
- Android može izbrisati lokalne podatke nakon brisanja podataka aplikacije, deinstalacije, resetiranja ili kvara uređaja. Bez ručne JSON kopije ne postoji jamstvo oporavka.
- Aktualno ograničenje JSON kopije je 16 MB / 20 000 ukupnih unosa; i dalje je potreban korisnički odabran vanjski medij za zaštitu nakon gubitka ili deinstalacije uređaja.
- Sigurnost obnove treba dodatno provjeravati automatiziranim testovima nakon svake buduće migracije sheme.

## Izričito otvoreno prije produkcijskog odobrenja

- Vlasnik mora dodati siguran upload ključ u GitHub Environment `google-play` i dovršiti Play App Signing.
- Potrebno je proći Play Console obrasce (privatnost, Data Safety, dobna kategorizacija, kontakt, app access), stvarni upload i Googleovu provjeru.
- Potrebno je pregledati na fizičkim uređajima: male ekrane, tablete, velika slova, tamnu temu, horizontalni prikaz i pristupačnost.
- Vizualnu podudarnost s referentnim slikama potrebno je dodatno potvrditi mjerenjem po ekranima; automatizirani UI smoke test nije pixel-diff.
- Iznosi plaća su **informativna procjena bruto iznosa**, ne službeni zakonski obračun niti neto obračun; korisnik unosi satnicu i dodatke.
- Treba nastaviti provjeravati ovisnosti i izvještaje o ranjivostima te održavati SDK cilj kako Play pravila napreduju.

Detaljne upute: [Google Play](GOOGLE_PLAY.md), [gradnja](BUILDING.md), [pravila privatnosti](../PRIVACY.md).

## Nastavak revizije — 19. 10. 2026. (PR, prije izdanja)

Proučeni su `ShiftStore`, `ShiftBackup`, `ShiftViewModel`, validator vremenskih preklapanja,
`WorkCalculator`, Android SAF izvoz, SQLite sheme v1/v2 i okidači CI-a. Ovaj dio
opisuje izmjene u radnoj grani, **ne potvrđuje objavu niti prolazak novih testova**.

- **Zaštita arhive:** nedestruktivni JSON uvoz preskače datume s arhiviranom
  smjenom. Prijašnja implementacija dopuštala je ponovno popunjavanje kalendara
  na takav datum i bez korisnikove radnje „Vrati”.
- **Veliki uvoz:** indeks po datumu i skup postojećih zapisa zamjenjuju ponovljene
  linearne usporedbe svih ranijih smjena; provjera zahvaća i prethodni/sljedeći
  datum kako bi se zadržalo otkrivanje preklapanja preko ponoći.
- **Sigurnosne kopije:** gornja granica povećana je s 4 MB na 16 MB uz postojeći
  limit 20 000 unosa i provjeru ulazne veličine. Priprema JSON i CSV sadržaja
  sada završava prije otvaranja odredišnog toka podataka.
- **Izolirani instrumentacijski testovi:** posebne baze za svaki novi test sprječavaju
  zadržavanje testnih arhiviranih smjena u korisničkoj/ranije dijeljenoj bazi.
  Dodani su scenariji arhiviranih datuma, idempotentnog uvoza, noćnih konflikata
  i stvarne nedestruktivne SQLite migracije v1 → v2.
- **CI:** Android emulator testovi (API 30 i 36) pokreću se i za promjene kroz PR,
  ne samo nakon spajanja u `main`.
- **Granice provjere:** ovi zahvati nisu cjelovit vizualni 1:1 audit zaslona.
  Potpuna provjera velikih fontova, stvarnih uređaja i produkcijskog potpisivanja
  ostaje zasebna obveza prije javnog izdanja.

Nakon GitHub CI-a treba unijeti rezultate po točnom SHA-u PR-a; sve prijavljene
padove treba riješiti prije mergea ili novog GitHub Releasea.
