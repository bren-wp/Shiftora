# Povijest izdanja

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
