# Shiftora

**Shiftora** — *Moje smjene. Moji sati.* • Android aplikacija za zaposlenike, Brendigo.

Nativna Android aplikacija na hrvatskom jeziku inspirirana vizualnim referencama premium brenda Shiftora. Nema prijave, pozadinskog servera, analitike ni dopuštenja za internet.

## Značajke (razvoj 0.2.0)

- Početna nadzorna ploča s današnjom i nadolazećim smjenama te pregledom tjednih sati
- Mjesečni i tjedni kalendar, više smjena na dan, unos, uređivanje i brisanje
- Jutarnja, popodnevna, noćna, slobodan dan i prilagođena vrsta
- Unos plaćene/neplaćene pauze, oznake odrađene smjene i prekovremenoga rada
- Automatsko ponavljanje ciklusa (bez prepisivanja postojećih zapisa)
- Statistika radnih sati i procjena bruto plaće
- Postavke bruto satnice i postotaka dodataka za noćni rad, nedjelje, blagdane i prekovremeni rad
- Ispravan obračun noćnih smjena koje prelaze u sljedeći tjedan ili mjesec
- Lokalne hrvatske blagdanske datume, ljetno/zimsko računanje vremena i CSV izvoz
- Ručni izbor datuma i 24-satnog vremena kroz sistemske Android dijaloge
- Prilagođeni smjenski ciklusi do 28 dana
- JSON sigurnosna kopija i vraćanje svih smjena i satnice uz provjeru valjanosti i izričitu potvrdu korisnika
- Svijetla/tamna tema, bez korisničkog računa i mrežnog pristupa

## Tehnologija

Kotlin 2.0.21, Jetpack Compose + Material 3, Android SDK 35 (minSdk 26), SQLite i ViewModel/StateFlow.

## Pokretanje

1. Otvori repozitorij u Android Studiju s instaliranim Android SDK 35 i JDK 17.
2. Sinkroniziraj Gradle i pokreni modul **app**.
3. GitHub Actions / **Android CI** pokreće JVM testove, provjerava kompilaciju UI testova i izrađuje debug APK koji je dostupan kao workflow artifact nakon uspješnog builda.
4. Dodatni **Android UI smoke test** workflow pokreće emulator pri ručnom pokretanju preko GitHub Actions i sprema stvarnu snimku zaslona (ne generiranu sliku).

Za CLI koristi Gradle 8.10.2: `gradle :app:testDebugUnitTest :app:assembleDebug`. Projekt trenutno nema uključen Gradle wrapper; Android Studio može koristiti lokalnu podržanu distribuciju. Produkcijski APK/AAB zahtijeva vlastito sigurno potpisivanje.

## Obračun

Svi datumi i vremena pohranjeni su lokalno. Radni sati računaju se minutnom preciznošću. Kod prijelaza na ljetno/zimsko vrijeme koristi se vremenska zona uređaja. Dodaci su *preklapajuće* kategorije (npr. nedjelja i noćni rad), stoga se ukupni radni sati ne zbrajaju dvaput.

**Procjena plaće nije službeni obračun.** Postoci u početnim postavkama samo su primjeri. Korisnik treba unijeti vlastitu satnicu, dodatke i pravila svojega radnog odnosa. Aplikacija ne prikazuje neutemeljenu neto procjenu.

## Privatnost

Svi osobni unosi ostaju na uređaju i ne koriste se za analitiku. Sigurnosni cloud backup je isključen kako se evidencija rada ne bi slučajno slala u cloud. Izvoz CSV-a pokreće korisnik.

## Licenca

MIT © 2026 Brendigo. Razvoj Shiftore je originalan; repozitorij se ne oslanja na kopiranje neprovjereno licenciranog izvornog koda trećih strana.
