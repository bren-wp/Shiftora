# Shiftora — vizualni audit prema korisničkim referencama (25. 10. 2026.)

## Opseg i izvor usporedbe

Referentne slike korisnika: Početna, Kalendar, Statistika i plaća, četiri uvodna prikaza, te grafički prikaz Shiftora brenda i komponenti. Testne snimke stvarne aplikacije na `main` (`e1fbba3`, 0.9.7) nastale su u GitHub Actions:

- [Android 30 UI snimke](https://github.com/bren-wp/Shiftora/actions/runs/40151895864), artifact `shiftora-pixel_2-screenshots`, 1080 × 1920.
- [Android 36 UI snimke](https://github.com/bren-wp/Shiftora/actions/runs/40151895864), artifact `shiftora-pixel_4-screenshots`, 1080 × 2280.
- Za svaki uređaj snimljeni su: welcome, tri onboarding stranice, Početna, Kalendar, Statistika, Plaća i Više.

Ovo su dvije različite visine uređaja; prikaz je iz rujna/listopada 2026., a referentna slika koristi ožujak 2025. i marketinški oblikovan uređaj. Razlike u stanju podataka i sustavnim trakama znače da sirovi globalni pixel-diff nije valjana metrika podudarnosti. Za ciljnu usporedbu najprije treba upariti screen content, width u dp, font-scale, Android verziju i podatke u zasebnoj emulator bazi.

## Provjera po ekranu

| Ekran | Opaženo na 0.9.7 | Intervencija u PR #8 | Što još treba provjeriti |
|---|---|---|---|
| Android launcher i splash | Mark S + sat postoji, ali plavi potezi su ravni i rub sata slabo odvaja plavu površinu | Dodatne refleksije poteza i tamni rub sata u vektoru, bez promjene maski | Maskirani ikonografski prikaz na Pixel/Samsung/Xiaomi launcherima; splash na API 30 i 36 |
| Welcome | Funkcionalni plavi početni ekran, referenca sadrži fotografiju planinske ceste koja ne postoji kao izvorni asset | Ostaje nativni offline Compose ekran; izvorna fotografija nije proizvoljno replicirana | Izvorni slikovni asset za fotografsku podudarnost, 320dp i TalkBack |
| Onboarding kalendar | Tjedni primjer prati četiri vrste smjena | Ikonica slobodnog dana usklađena s palmom | Snimke nakon ikonografske promjene i veliki font |
| Onboarding sati | Nativne kartice smjena, već postoje čitljive oznake vremena | Zajednička korekcija vektora | Renderiranje pri 200 % fonta |
| Onboarding plaća | Stupčani prikaz i informativna bruto procjena | Ikona procjene koristi grafikon umjesto novčanika | Veličina ilustracije naspram referentne kartice |
| Početna | Zaglavlje, datum, smjena, tjedni sati, bruto procjena, brze akcije i nadolazeće smjene postoje; na 360×640dp sadržaj je namjerno pomičan | Ikona procjene s grafikonom kao u referenci | Dugi iznosi, više smjena u danu, landscape, 320dp |
| Kalendar mjesec/tjedan | Mreža je čitljiva, ali na snimkama su na svim danima prikazane velike ikone jutarnje, popodnevne i slobodnog dana umjesto točkica/točne palme | Obojene točkice za neodabrane J/P, palma za SD, odabrani dan i dalje ima jasno istaknutu ikonu; pravilna semantika kartice Mjesec/Tjedan | Usporedba Android snimki nakon CI, 6 redova mjeseca, više unosa u istom danu |
| Detalj datuma | Na 360dp širokom emulatoru kartica dana djelomično izlazi ispod donje navigacije u početnom položaju, ali se može skrolati; ručnim odabirom datuma uključuje se BringIntoView | Zadržana automatska dostupnost nakon odabira i minimalna veličina dodira; nije žrtvovana pristupačnost radi umjetnog stiskanja mreže | Reproducirati odabir i klik na Uredi/Dodaj pri gestama, 320dp/360dp |
| Statistika | Kružni graf i četiri kategorije odgovaraju značenju. Na nižem zaslonu nisu sve KPI kartice vidljive bez skrolanja | Semantika izabranog razdoblja, postojeći responzivni prikaz; nema hardkodiranih vrijednosti iz slike | 150/200 % font, dugi lokalizirani iznosi, čitači zaslona |
| Plaća | Prikazuje se procjena *bruto*; referenca prikazuje i neto, ali sustav nema dovoljno podataka za pouzdanu neto procjenu | Umjesto novčanika, navigacija koristi kovanice prema referenci; obračun nije mijenjan | Provjeriti validnost korisničkih postotaka i prikaz velikih brojeva |
| Više/postavke | Lista funkcija je okomito pomična; reference ne specificiraju pojedinačni ekran | Zadržana funkcionalnost i sigurnosna kopija | Ručni prolaz kroz svaku stavku i potvrde |
| Uređivanje smjene | Više polja, odabir datuma/vremena, stanja prekidača | Visina skrolabilnog dijela ovisi o visini uređaja | Otvorena tipkovnica, rotacija, TalkBack |
| Rotacijski raspored | Uzorci i ručni ciklus dostupni | Prilagođena visina skrolabilnog sadržaja | Dugi 28-dnevni ciklus i 200 % font |
| Satnica i dodaci | Polja za bruto satnicu i dodatke | Sadržaj skrolabilan i dinamički ograničen po visini | Decimalni zarez, tipkovnica i vrlo veliki font |
| Boje smjena | 10 vrsta i HEX unos | Skrolabilno područje prilagođeno visini zaslona | Kontrast proizvoljnih HEX boja |
| Fond sati | Korisnički fond i izračun postoje | Sadržaj skrolabilan s ograničenjem visine | Minute 0–59, zadani fond |
| Arhiva | Lazy popis s povratkom datuma | Ograničenje visine popisa na malom zaslonu | 1000+ unosa, iznimke pri vraćanju |
| Donja navigacija | Početna, Kalendar, Statistika, Plaća, Više; Kalendar je nakon splasha odabrani tab | Ikona Plaće zamijenjena kovanicama; oznake ostaju uz ikonice | Gesture i 3-button navigacija, čitljivost na 320dp |

## Neizmijenjene poslovne vrijednosti

Nisu dirani SQLite schema, ID-evi smjena, arhiva, formule `WorkCalculator`, `MonthlyFund`, `SalarySettings`, JSON/CSV formati ni sigurnosni ključevi. U statistikama se prikazuju stvarno izračunate kategorije, a obračun bez poreznih parametara ostaje eksplicitno **bruto procjena**.

## Kriteriji dovršetka

- Android CI: `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`, `:app:lintRelease`, `:app:assembleDebug`, `:app:bundleRelease`.
- Emulator UI test API 30 i 36, uz test kompozicije za 320dp i font-scale 1.5.
- Nakon svake značajne izmjene napraviti nove snimke svih devet stanja, usporediti s ovim referencama i navesti neriješene razlike.
- Ne tvrditi vizualnu 1:1 certifikaciju bez normaliziranih slika i mjerljivog uspoređivanja.
- Ne spajati PR dok CI nije uspješan; novi release mora zadržati jasno označen **debug APK** i **unsigned AAB** ako produkcijski potpis nije postavljen.
