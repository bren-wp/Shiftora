# Povijest izdanja

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
