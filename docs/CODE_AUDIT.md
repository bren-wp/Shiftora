# Shiftora — audit koda i kvalitete (0.7.0)

Ovaj dokument sadrži pregled izvorne strukture, pronađene greške, stvarne izmjene i provjere. CI rezultat vrijedi samo za točan commit, a nije jamstvo odsutnosti svih nepoznatih grešaka.

## Pregledani dijelovi

| Područje | Rezultat |
|---|---|
| Android manifest / dozvole / offline pristup | Bez potrebe za INTERNET, lokacijom, oglašavanjem, kontaktima; `allowBackup=false`, cleartext promet onemogućen |
| Target SDK / AndroidX / Gradle | Migracija na API 36, AGP 8.10.1, Gradle 8.11.1 |
| Lokalna baza i rotacije | SQLite transakcije, indeks datuma, izbjegavanje preklapanja, nedestruktivno dodavanje smjena |
| Obračun minuta i dodataka | Jedinstveni `ShiftTimeline`, provjera preklapanja kategorija, DST i mjesečne granice |
| JSON / CSV datoteke | UTF-8, gornje granice veličine, zaštita od formula u CSV-u, prethodna provjera uvoza |
| Preferencije i postavke plaće | Popravak NaN/Infinity, pokvarenih tipova i raspona noćnog rada |
| UI niti i veliki dokumenti | SAF i dekodiranje sigurnosne kopije premješteni na IO dispatcher |
| Dead-code i duplikati | Uklonjen debug fixture iz glavne aktivnosti, neupotrijebljeni `icons`/Canvas/Stroke importi i nepotrebne UI tooling ovisnosti |
| Privatnost u aplikaciji | Dostupna lokalna pravila, nema praćenja ili oglasa |
| Objavljivanje | Preview debug APK + unsigned AAB; zaseban ručni workflow za potpisani Play AAB kada postoje secrets |

## Regresijski testovi

JVM: smjene i pauze, prelazak preko ponoći, promjene DST, hrvatski blagdani, obračun plaće, grafovi i indikatori, backup validacija, CSV formula injection. Android instrumentacijski testovi: navigacija, SQLite CRUD, rotacije, nevažeće preferencije i realne snimke zaslona na emulatoru.

Pokreću se:
`./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:bundleRelease`

## Izričito otvoreno prije produkcijskog odobrenja

- Vlasnik mora dodati siguran upload ključ u GitHub Environment `google-play` i dovršiti Play App Signing.
- Potrebno je proći Play Console obrasce (privatnost, Data Safety, dobna kategorizacija, kontakt, app access), stvarni upload i Googleovu provjeru.
- Potrebno je pregledati na fizičkim uređajima: male ekrane, tablete, velika slova, tamnu temu, horizontalni prikaz i pristupačnost.
- Vizualnu podudarnost s referentnim slikama potrebno je dodatno potvrditi mjerenjem po ekranima; automatizirani UI smoke test nije pixel-diff.
- Iznosi plaća su **informativna procjena bruto iznosa**, ne službeni zakonski obračun niti neto obračun; korisnik unosi satnicu i dodatke.
- Treba nastaviti provjeravati ovisnosti i izvještaje o ranjivostima te održavati SDK cilj kako Play pravila napreduju.

Detaljne upute: [Google Play](GOOGLE_PLAY.md), [gradnja](BUILDING.md), [pravila privatnosti](../PRIVACY.md).
