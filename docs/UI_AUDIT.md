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

## Revizija 0.9.4 — stvarne snimke i usporedba s pet korisničkih referenci

Pregledane su stvarne emulatorske slike prethodne verzije (`home`, `calendar`,
`statistics`, `salary`, `more`, `onboarding`) na Pixel 4 (1080 × 2280)
u odnosu na pet dizajnerskih referenci. Uočeni su premali znak zaglavlja,
nepravilni gramatički oblik datuma, dodatna visina višeredne legende
i screenshot provjera s praznim kalendarom i nulama u statistikama.

- **Zajednički znak:** istaknuti puni vektor S+satić u zaglavljima; launcher
  safe-zone i monochrome ikonice nisu mijenjane.
- **Početna:** gumb s kalendarom odabire datum i mijenja odgovarajući
  prikaz smjene, tjedni zbroj, procjenu plaće i budući popis. Za prethodni
  datum oznaka nije lažno „trenutna” smjena.
- **Kalendar:** četiri glavne legende odgovaraju referenci, ostalih šest
  statusa ostaje dostupno pomoću gumba „Ostale oznake”. Time dnevna kartica
  dobiva više prostora bez gubitka GO/BO/SD/PD/PR/X statusa.
- **Statistika/plaća:** ostaju stvarni zbrojevi; mockup vrijednosti i izmišljeni
  neto iznos nisu uvedeni.
- **Onboarding i Više:** zadržane su postojeće nativne komponente i
  responzivne kontrole, uključujući korisničke sigurnosne kopije i privatnost.
- **Testna baza:** raniji navigacijski UI test imao je upis ciklusa u zadanu
  bazu. Izoliran je u zaseban instrumentacijski fixture, aktivan samo preko
  izričitog parametra u skripti za snimanje testnog emulatora. Time obični
  test navigacije više ne upisuje demonstracijske rasporede.

Automatski CI i screenshot provjere nisu zamjena za mjerljivu vizualnu usporedbu
istih uređaja i stanja. Stvarna aplikacija treba zadržati stvarne podatke
korisnika, a ne preslikati izmišljene iznose s dizajnerskog mockupa.

## Revizija 0.9.5 — dovršavanje sistemskog UI-a i manjih zaslona

| Područje | Intervencija | Što ostaje otvoreno |
| --- | --- | --- |
| Sustavna statusna traka | Boja statusnih ikona ovisi o stvarnom svijetlom/tamnom zaglavlju, a ne samo o globalnoj temi | OEM interpretacija Android 16 edge-to-edge načina |
| Početna dobrodošlica | Zaseban tamni Compose welcome + 3 edukativna koraka; svi gumbi dostupni, bez interneta ili novih dopuštenja | Izvorna panoramska fotografija marketinške reference nije dio aplikacijskih resursa |
| Statistika | Dvije KPI kartice ostaju usporedne na širim ekranima, a na uskim se slažu okomito; kružni graf prelazi iz reda u stupac kad nema prostora za čitljive oznake | Provjera pri 200% veličini fonta na fizičkom uređaju |
| Uređivač | Puni red prekidača je semantički Switch i minimalno 56dp visok | TalkBack izgovor na stvarnom uređaju |
| Kalendar | Veće pločice dana i tipke za dodatne oznake / uređivanje | Usklađivanje s renderiranim mockupom zahtijeva jednako stanje i fizičku veličinu |
| QA početnih ekrana | GitHub AVD može resetirati samo vlastite podatke i snimiti dobrodošlicu te sva 3 koraka; lokalna skripta ne briše podatke bez opt-in varijable | Vizualni ručni pregled svih rezultata |

Ovo izdanje **ne** mijenja postojeću Androidovu lokalnu bazu, obračunske formule,
radne podatke ili korisničke boje. AAB ostaje nepotpisan sve dok se ne upotrijebi
vlasnikov produkcijski upload ključ.

## Revizija 0.9.6 — Kalendar kao korijenski ekran

Provjera navigacije: nakon splash zaslona i završenog onboardinga odabran je tab **Kalendar** (index 1). Android Natrag s drugih tabova vraća na Kalendar, ali Početna nije uklonjena. Provjera screenshot skripte počinje potvrdom oznake *Mjesec*, pa tek zatim otvara Početnu. Dnevna kartica ima vizualni okvir, jasnije ikone vremena/pauze/bilješke i automatsko pomicanje u vidljivi dio nakon korisnikova odabira. Dodano je responzivno ponašanje statistike i donje navigacije za Android font-scale veći od 130%.

Nisu promijenjene poslovne funkcije, korisnička baza, migracije, potpisni ključevi niti formule. Prije tvrdnje o strogom 1:1 poklapanju potreban je stvarni pixel-diff usporedivih snimki (ista gustoća, rezolucija, podaci i veličina fonta).

## Revizija 0.9.7 — pregled kontrole razmaka, selektora i dostupnosti

| Prikaz | Dodatna promjena | Test / vizualna granica |
| --- | --- | --- |
| Kalendar | Izravan klik na naslov mjeseca otvara DatePicker, naslovi i strelice imaju dovoljno prostora; legenda automatski bira četiri ili dva stupca; kartica smjene bira tri ili dva+jedan stupac | UI test potvrđuje prisutnost kontrole datuma; ručno provjeriti duge bilješke i TalkBack na stvarnom uređaju |
| Statistika | Gumb naziva razdoblja otvara odabir datuma; tjedan se normalizira na ponedjeljak, mjesec/godina prema izabranom datumu | UI test traži izravan odabir razdoblja; statistika ostaje izračunata iz stvarnih smjena |
| Plaća | Gumb naziva mjeseca otvara odabir mjeseca bez upisivanja izmišljenih iznosa | UI test provjerava postojanje gumba; obračunske formule se ne mijenjaju |
| Donja navigacija | Nema prisilnog smanjenja teksta na 9sp za povećane sistemske fontove; do dva retka i viša dodirna zona | Kompromis s marketinškom referencom je namjeran radi pristupačnosti |
| Boje smjena | Veće oznake, semantika izabrane vrste i 4/5 boja u retku na ovisnosti o širini | Kontrola 320dp / velika tipografija kroz emulator i stvarni uređaj |

Ova iteracija nije uklanjala stara izdanja niti mijenjala identitet aplikacije, potpisni ključ ili korisničke podatke. Referentne slike za strogi pixel-diff zahtijevaju istu rezoluciju, gustoću, font-scale i stanje aplikacije kao emulatorske snimke.
