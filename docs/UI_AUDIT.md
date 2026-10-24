# Shiftora — pregled korisničkog sučelja (v0.9.2)

Ovaj dokument opisuje **stvarno promijenjena područja** u v0.9.2 i granice provjere.
Nijedna promjena ne dira SQLite tablice, shemu ni postojeće spremljene smjene.

| Zaslon / funkcionalnost | Izmjene | Preostala provjera |
|---|---|---|
| Ikona aplikacije | Usklađen S + sat vektor s izvornim `assets/brand/logo.svg`; adaptive safe-zone i themed monochrome | Ručno provjeriti kružnu/kvadratnu masku, Pixel Launcher i OEM launchere |
| Onboarding (3 koraka) | Skrolanje i prilagodba niskim / vodoravnim zaslonima; stanje koraka preživljava rotaciju | Povećani font i tablet |
| Početna | Sažeci tjedna i plaće slažu se okomito na uskim ekranima; kraći, konzistentni redci smjena | Na fizičkom malom telefonu i tabletu |
| Kalendar — mjesec | Vidljive oznake do dvije smjene, oznaka dodatnih unosa, istaknut današnji datum, opis za čitače zaslona, bolji kontrast vlastitih boja | 1:1 mjerenje prema izvornim slikama |
| Kalendar — tjedan | Strelice pomiču odabrani tjedan za sedam dana, opisi tipki odgovaraju prikazu | Rubni datumi mjeseca / godine |
| Uređivanje smjene | Brojčana tipkovnica za pauzu, ograničen unos, očuvane validacije | Manji ekrani s otvorenom tipkovnicom |
| Automatske rotacije | Veće dodirne površine, ograničena visina dijaloga, odabir datuma, numerički broj dana | Dugi prilagođeni ciklusi |
| Statistika | Razdoblje i tjedan se pamte; indikatori prilagođeni vrlo uskim karticama | Kontrast i povećani fontovi |
| Plaća | Odabrani mjesec ostaje spremljen kroz navigaciju; vrijednosti u sažecima ne preklapaju nazive | Veliki iznosi i ekran 320dp |
| Postavke plaće | Prikladna tipkovnica za decimalne iznose i satne intervale | Lokalizacija decimalnog zareza |
| Boje smjena | Veće tipke, oznake HEX boja za pristupačnost, potvrda odabrane boje | Različite kontrastne kombinacije |
| Fond sati | Brojčana tipkovnica i postojeća validacija 0–744 sati | Provjera s uvećanim fontom |
| Više / postavke | Puni red prekidača tamne teme i veće, semantički označene akcije | TalkBack i veliki font |
| Arhiva | LazyColumn umjesto renderiranja cijele arhive, dosljedne ikone i vraćanje po zapisu | Vrlo veliki broj arhiviranih smjena |
| Sigurnosne kopije | Postojeća potvrda spajanja i format ostaju nepromijenjeni | Ručni SAF test na fizičkom uređaju |
| Navigacija | Tematske boje aktivne kartice, bez duplicirane najave ikone i naziva | Tablet / geste / sustavna navigacija |

## Automated QA

- `ShiftoraNavigationTest` obuhvaća osnovnu navigaciju, fond, boje, arhivu.
- `ShiftoraCalendarUiTest` dodan za istovremene oznake smjena i pomicanje tjedna.
- Android CI: JVM, instrumentacijska kompilacija, Lint i izrada APK/AAB.
- Android emulatori: Pixel 2 API 30 i Pixel 4 API 36.
- GitHub Actions snima **stvarne** snimke onboarding, home, calendar, statistics, salary i more.

## Važna granica točnosti

U ovom razgovoru nisu dostupne izvorne referentne slike svih zaslona. Postojeći
GitHub screenshotovi prikazuju stvarno stanje aplikacije, **ne nužno ciljnu sliku**.
Zbog toga nije točno tvrditi da je dovršena provjera `pixel-by-pixel 1:1`. Za
potpunu usporedbu potrebni su izvorni screenshotovi, usporedba pri istoj gustoći,
rezoluciji, veličini fonta i stanju aplikacije te vizualni pregled svih modalnih
prozora. Izmjene u ovom ciklusu su funkcionalna i vizualna poboljšanja, a ne
certifikat podudarnosti.

Izrada potpisanog produkcijskog AAB-a ostaje zaseban proces s vlasnikovim
izdavačkim ključem. GitHub debug APK ne smije biti zamjena za potpisanu nadogradnju.
