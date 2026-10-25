# Povijest izdanja

## 0.9.4 — 25. 10. 2026. | Dodatna vizualna preciznost i stvarna screenshot QA

- Ispravljen hrvatski **genitiv mjeseca** u punom datumu: npr. `24. listopada 2026.`, uz očuvanje nominativa zaglavlja `Listopad 2026.`.
- Povećan je S+satić znak u zajedničkim zaglavljima bez povećavanja sigurnosne zone Androidove ikone aplikacije.
- **Početna:** funkcionalan gumb za izbor datuma, pregled prošle/buduće smjene te posebno označavanje smjene koja se trenutno stvarno odvija. Sažeci i brze akcije sada koriste odabrani datum.
- **Kalendar:** glavne četiri boje/legende imaju prioritet kao na referenci; dodatne vrste smjena mogu se proširiti. Manje je nepotrebnog zauzimanja prostora prije detalja odabranog dana.
- **QA:** uklonjen je automatski upis demonstracijskih smjena iz običnog navigacijskog testa. Nove screenshot snimke eksplicitno pozivaju izolirani instrumentacijski fixture koji popunjava mjesec *isključivo na testnom emulatoru*. U produkciji nema testnih podataka.
- Dodani su regresijski testovi hrvatskih datuma, kalendarske legende i očuvanja korisničke baze.

**Granice:** Dizajnerski mockupovi nisu izravne snimke istih uređaja, razdoblja i podataka. CI provjerava izvedbu i snimke, ali sam po sebi ne dokazuje pikselnu 1:1 podudarnost. SQLite shema, postojeći rasporedi, povijest, sigurnosne kopije i paket nisu mijenjani.

## 0.9.3 — 25. 10. 2026. | Vizualno usklađivanje s referentnim zaslonima

- Redizajnirani su zajednički elementi: Shiftora znak, zaglavlja, zaobljene bijele površine ispod tamnih zaglavlja, kartice, natpisi i plavi/menta tonovi.
- Donja navigacija prikazuje zaobljenu podlogu cijele aktivne kartice (ikona i natpis), kao na priloženoj referenci; raspored i svih pet funkcionalnih odredišta ostaju sačuvani.
- Kalendar sada koristi nježne pastelne pločice, simboličke ikone za standardne smjene, plavo označen odabrani datum te dodatne oznake i opise za višestruke unose. Fond sati ostaje dostupan ispod dnevnih detalja.
- Statistika dobiva svjetlu zaobljenu radnu površinu, veći kružni graf s udjelima, čitljivije obojene KPI kartice, istaknut informativni bruto iznos i otvorenu karticu objašnjenja s stvarnim satima i postocima.
- Početna dobiva tri brze akcije u plavom/svijetloplavom/zelenom stilu te puni naziv smjene kad širina zaslona to dopušta.
- Onboarding zadržava tri funkcionalna koraka i može se preskočiti, ali ima ilustrirane preglede kalendara, rasporeda i procjene plaće izrađene kao nativne Compose komponente.
- Adaptivna ikona koristi osvježeni vlastiti S+satić simbol bez dodatnog rasteriziranog sadržaja; usklađena je i jednobojna varijanta za tematske ikone.
- Poboljšana je Android emulator skripta: upravlja stvarnim tekstnim/semantičkim elementima, ne samo fiksnim koordinatama.

**Granice:** dostavljene slike su dizajnerske reference, ne stvarne snimke izvođenja aplikacije. Podudarnost 1:1 zahtijeva usporedbu izvornih snimki uređaja i test na fizičkim telefonima. Izmišljeni neto iznosi i izmišljene smjene iz mockupa nisu uneseni u korisničke podatke. SQLite shema, paket aplikacije i pravila očuvanja podataka nisu mijenjani.

## 0.9.2 — 25. 10. 2026. | Poliranje korisničkog sučelja i pristupačnosti

- Ažurirana je prilagodljiva ikona aplikacije prema službenom Shiftora S/sat logotipu. Dodana je jednobojna Android themed-icon varijanta.
- Kalendar čitljivije prikazuje **dvije smjene na istom datumu** i označuje dodatne unose. Uvedeni su opisi za čitače zaslona, posebna oznaka današnjeg datuma i odabranog dana te kontrastniji tekst na svim korisničkim bojama.
- Navigacija u tjednom kalendaru pomiče se točno **sedam dana**, umjesto da preskače cijeli mjesec.
- Odabrano statističko razdoblje, tjedan i mjesec plaće čuvaju se pri promjeni kartica i ponovno stvaranju prikaza.
- Početni sažeci sati i plaće prilagođeni su uskim zaslonima; redci smjena ne preklapaju vrijeme, datum, oznake i strelicu.
- Poboljšani su donja navigacija, ikone, dodirne površine, tamna tema i čitljivost statističkih indikatora.
- Dijalozi imaju preglednije kontrole, brojčane tipkovnice, veće tipke za uklanjanje dana iz ciklusa te odabir datuma za rotaciju.
- Uređen je prikaz uvođenja korisnika na manjim i vodoravnim zaslonima.
- Dodani su Android UI regresijski testovi za istovremene oznake smjena i tjednu navigaciju.

**Napomena:** izvorne korisničke referentne slike nisu dostupne u repozitoriju; zato ovo izdanje ne tvrdi potvrđenu piksel-po-piksel (1:1) podudarnost. Nisu mijenjane SQLite shema ni korisnički rasporedi.

## 0.9.1 — 19. 10. 2026. | Pouzdanije sigurnosne kopije i zaštita arhive

- JSON uvoz više ne vraća automatski smjene na datume koje je korisnik ranije arhivirao. Vraćanje arhive i dalje se pokreće izričito.
- Ubrzana je obrada dugoročnih sigurnosnih kopija bez narušavanja provjere konflikata i noćnih smjena.
- Povećana je dopuštena veličina JSON sigurnosne kopije na 16 MB uz ograničenje 20 000 unosa.
- CSV i JSON se prvo pripremaju u memoriji, a tek potom otvaraju odredišne datoteke.
- Dodani su regresijski testovi za migraciju SQLite baze v1 → v2, ponavljani uvoz, arhivu i noćne smjene.
- Android CI i emulator testovi (API 30 i API 36) provjeravaju i radne pull requestove.
- **Napomena:** rasporedi se čuvaju lokalno; deinstalacija ili brisanje podataka uklanja ih ako korisnik nije izvezao sigurnosnu kopiju.

## 0.9.0 — 18. 10. 2026. | Očuvanje rasporeda i stabilnost

- Sigurna migracija SQLite baze v1 → v2 bez brisanja povijesnih ili budućih smjena.
- Ručno uklanjanje smjene sada je arhiviranje u zasebnu lokalnu tablicu, s mogućnošću vraćanja u kalendar.
- Rotacije preskaču sve datume koji imaju aktivan ili arhiviran unos; nema automatskog prepisivanja.
- Uvoz JSON datoteke sada je isključivo **spajanje**: postojeće smjene i postavke ostaju netaknute, konfliktni i duplicirani unosi preskaču se bez gubitka podataka.
- JSON sigurnosna kopija v3 uključuje aktivne i arhivirane smjene, boje i fond sati; stare v1/v2 kopije ostaju podržane.
- UI pamti mjesec, datum i tjedni/mjesečni način pregleda i nakon promjene taba ili ponovnog stvaranja ekrana.
- Izbjegnuto privremeno prikazivanje praznog rasporeda tijekom učitavanja te serijalizirane operacije spremanja i uvoza.
- Dodan ekran Arhive, zaštitna kartica s brojem smjena i izravan izvoz JSON kopije.
- Poboljšano rukovanje brzom evidencijom rada na GO/BO/PD/SD danima i tekstovi potvrda arhiviranja.
- Regresijski JVM i Android testovi za arhivu, uvoz bez brisanja, očuvanje starih/budućih smjena i promjene navigacije.
- Lokalna pohrana **ne preživljava nužno deinstalaciju, brisanje podataka ili gubitak uređaja**: za to se preporučuje ručno izvezena JSON kopija.

## 0.8.1 — 18. 10. 2026. | Precizniji fond sati i pripravnost

- PR (pripravnost) može postojati uz radnu smjenu istog datuma bez udvostručenja radnih ili plaćenih sati.
- GO, BO, PD i SD ostaju cjelodnevni statusi koji ne mogu dijeliti dan s drugim unosima.
- Mjesečni fond podržava zaseban unos sati i minuta (npr. 151 h 30 min), uz strogu validaciju raspona.
- Odvojeni pokazatelji stvarno odrađenih, planiranih i GO/BO/PD sati; dodana projekcija koliko nedostaje do fonda ako se planirane smjene odrade.
- Novi predložak automatskog rasporeda D–N–SD–SD za smjenski turnus.
- Regresijski unit i Android instrumentacijski testovi za istodobnu pripravnost i rad, isključivost godišnjeg odmora, minute fonda te planirane i odrađene sate.

## 0.8.0 — 18. 10. 2026. | Smjene, boje i mjesečni fond sati

- Nove oznake u kalendaru: D (dnevna), N (noćna), J (jutarnja), P (popodnevna), PR (pripravnost), GO (godišnji), BO (bolovanje), SD (slobodni dan), PD (plaćeni dopust) te X (prilagođena).
- Pločični mjesečni pregled koristi cijelu obojenu ćeliju i veliko slovo/kod, uz označen odabrani datum.
- Korisničke boje za svaku oznaku zasebno: 15 ponuđenih boja ili vlastiti HEX kod; izmjene ostaju spremljene.
- Mjesečni fond sati: standardni izračun za 8 h po radnom danu bez vikenda i hrvatskih blagdana, uz vlastiti fond za svaki mjesec.
- Razdvojeni odrađeni sati, ukupna evidencija, evidentirani izostanci GO/BO/PD i preostali fond.
- PR može imati vrijeme početka i završetka, ali se ne pribraja automatski odrađenom radu niti plaći.
- GO/BO/SD/PD ne povećavaju obračunate sate ili bruto procjenu. Krediti izostanka za fond nisu zakonski obračun plaće.
- JSON sigurnosne kopije v2 čuvaju boje i mjesečne fondove; stare v1 kopije mogu se učitati bez gubitka novih kategorija.
- Prošireni regresijski testovi za fond sati, dane izostanka, prilagođene boje, stare JSON kopije i nove UI kontrole.
- CSV izvoz sada sadrži i skraćenu oznaku svake smjene.

## 0.7.0 — 18. 10. 2026. | Android 16 & Google Play hardening

- `targetSdk` / `compileSdk` 36 (Android 16), odgovarajući Android Gradle Plugin i Gradle wrapper.
- Dodatni Android Lint (release), integracijske provjere te emulator na API 30 i 36.
- Siguran ručni workflow za **potpisani** Google Play AAB i APK — aktivira se tek kada vlasnik postavi tajne za ključ.
- Uklonjen testni unos smjena iz produkcijske aktivnosti; testni raspored ostaje samo u instrumentacijskim testovima.
- Izvoz CSV-a, JSON sigurnosne kopije i provjera uvezenih datoteka više ne blokiraju glavnu UI nit.
- Neispravne spremljene postavke plaće i nevažeći brojevi ne mogu srušiti početni zaslon.
- Sigurnosne kopije provjeravaju vremenska preklapanja prije upozorenja o zamjeni podataka.
- Objedinjeno računanje vremenskih granica smjena; optimizirana provjera blagdana i obračun minuta.
- Uklonjeni nekorišteni Compose tooling ovisnosti, razvojni simboli i dupli importi.
- Pravila privatnosti mogu se pročitati i izvan mreže unutar aplikacije.
- Poboljšane poruke o pogrešnim vrijednostima obračuna te dodani regresijski testovi.

## 0.6.0 — 18. 10. 2026.

- Statistika: kružni graf sada koristi četiri stvarne kategorije prema referentnom dizajnu — redovne, prekovremene, noćne i nedjeljne/blagdanske sate.
- Podjela kruga matematički je međusobno isključiva; zasebni obračunski indikatori i dalje pravilno prate preklapajuće dodatke.
- Stvarni postotci svakog segmenta i 2×2 pregledne kartice statistike.
- Usporedba tjedna/mjeseca/godine s prethodnim ekvivalentnim razdobljem; nema izmišljenih postotaka rasta kad prethodno razdoblje iznosi nula.
- Procjena bruto plaće: stvarna osnovica, dodaci i trend, bez netočnih neto iznosa.
- Objašnjenje obračuna proširivo na dodir; bolja podrška za male ekrane i tamnu temu.
- Početna: aktivna smjena, stvarno preostalo vrijeme preko ponoći, osvježavanje svakih 60 sekundi i proporcionalne tjedne trake.
- Jedinični i Android UI regresijski testovi za statistiku, datume, indikatore i promjenu razdoblja.

## 0.5.0 — 18. 10. 2026.

- Kalendar: kompaktniji raspored dana, manji razmaci i jasnije istaknut odabrani dan na manjim telefonima.
- Gumbi za uređivanje i dodavanje smjena prikazani su jedan uz drugi, kao na vizualnim referencama.
- Više smjena u jednom danu dobiva zasebne vizualne oznake.
- Početna: smanjene kartice i razmaci kako bi nadolazeće smjene bile bolje vidljive.
- Uvodni ekrani: svijetli premium izgled s prikazima kalendara, smjena i obračuna.
- Uređivanje smjena: pogreške preklapanja sada su vidljive prije spremanja podataka.
- Spriječen istovremeni unos slobodnog dana i radne smjene na isti datum.
- Prošireni regresijski testovi za konflikte rasporeda.

## 0.4.0 — 18. 10. 2026.

- Novi premium vizualni identitet i pregledan marketinški README s ikonama, bannerom i stvarnim snimkama Android aplikacije
- Optimiziran prikaz nadzorne ploče, kalendara, statistike i plaće
- Izračuni za tjedan, mjesec i godinu memorirani radi smanjenja ponovnih obračuna tijekom rada sučelja
- Usklađeni sistemska statusna traka, format EUR i navigacija
- Proširene automatizirane provjere na Android emulatorima različitih veličina
- APK i AAB isporučuju se putem GitHub izdanja

> AAB zahtijeva produkcijsko potpisivanje za Google Play. Ova aplikacija daje informativni obračun bruto plaće.

## 0.3.1 (2026-10-17) – hotfix i verifikacija
- Na svim podržanim Android verzijama prikazuje se simbol € umjesto starog HRK ili ISO EUR
- Tamna brendirana statusna traka s čitljivim sistemskim ikonama
- Snapshot testovi svih glavnih ekrana na dva profila emulatora
- Potpuno novo preview izdanje s novim versionCode 4

## 0.3.0 (2026-10-17) – stabilizacija i QA
- Sprječavanje preklapajućih smjena i dvostrukog uračunavanja sati
- Ograničenja trajanja pauze, zero-duration smjena i noćnog intervala
- Podesivo razdoblje noćnog rada, dodatna testna pokrivenost
- Ispravna valuta EUR čak i na starijim Android uređajima s HRK lokalizacijom
- Tamne statusne ikone na svijetloj podlozi, responzivnije akcije i kalendar
- Uveden standardni Gradle wrapper 8.10.2 i reproducibilne CI provjere
- Snimke stvarnih ekrana s emulatora, umjesto ilustrativnih mockupova
- Android lint, debug APK i nepotpisani release AAB u CI-u

## 0.2.0 (2026-10-17) – razvojno izdanje
- Validirano JSON sigurnosno kopiranje i atomsko vraćanje rasporeda
- Prihod i radni sati pravilno podijeljeni između mjeseci i tjedana za noćne smjene
- Dodani testovi za granice razdoblja, ljetno/zimsko računanje vremena i sigurnosne kopije
- Praktični Android dijalozi za datum i početak/završetak smjene
- Prilagođeni rasporedi rotacije do 28 dana
- Kompilacija Compose UI testova u CI-u i zaseban ručni emulator workflow
- Ograničenje sadržaja pri uvozu, validacija prije zamjene i pristup bez interneta

## 0.1.0 (2026-10-17) – početni prototip
- Kotlin i Jetpack Compose, početni vizualni identitet
- Početna, Kalendar, Statistika, Plaća, Više
- Smjene, lokalna SQLite baza, osnovni bruto obračun i CSV izvoz

> UI/UX podudarnost 1:1 s referentnim slikama, potpuna pravna validacija obračuna,
> testiranje na različitim dijagonalama zaslona i produkcijsko potpisivanje
> još nisu završeni. Prikazani iznos plaće je samo procjena bruto plaće.
