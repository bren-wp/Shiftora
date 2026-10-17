# Shiftora — objava na Google Playu

## Android i potpis

- Paket: `com.brendigo.shiftora`. **Ne mijenjati ID nakon prve objave.**
- Android 16 / API 36 je ciljna i kompilacijska razina za podnošenje 2026. godine.
- Google Play za novu Android aplikaciju zahtijeva potpisan Android App Bundle (`.aab`) i aktiviranu opciju Play App Signing.
- Automatski GitHub **preview** uvijek sadrži debug APK i **nepotpisani** AAB. Nijedan od tih paketa nije namijenjen izravnoj produkcijskoj objavi.
- GitHub Actions -> **Google Play signed bundle** izrađuje **potpisani** AAB i APK kada vlasnik doda tajne te ručno pokrene workflow.
- Upload ključ mora ostati isti pri sljedećim ažuriranjima, osim postupka reseta putem Play Console. Čuvaj zasebnu sigurnosnu kopiju ključa i lozinki.

### Sigurna konfiguracija (obavlja vlasnik)

U GitHub Settings -> Environments kreiraj `google-play`, ograniči tko može pokretati produkcijski workflow i dodaj sljedeće secrets u taj environment (ili repozitorij):

| Secret | Sadržaj |
|---|---|
| `SHIFTORA_UPLOAD_KEYSTORE_BASE64` | Base64 cijele Java upload-keystore datoteke |
| `SHIFTORA_UPLOAD_STORE_PASSWORD` | Lozinka spremišta |
| `SHIFTORA_UPLOAD_KEY_ALIAS` | Alias ključa |
| `SHIFTORA_UPLOAD_KEY_PASSWORD` | Lozinka ključa |

Koristi lokalno generirani, vlastiti Java upload ključ. **Ne šalji ga u chat, issue, log, pull request ni repozitorij.** Pokreni workflow ručno, pričekaj testove i preuzmi artefakt `shiftora-play-signed-bundle-and-apk`. Nije postavljen automatski upload u Play Console jer nije odobren Play Console pristup i nema produkcijskih ključeva.

### Play Console deklaracije

- Kategorija: Productivnost / Productivity; funkcija: osobni raspored radnih smjena i informativna bruto procjena.
- Pravila privatnosti: `https://github.com/bren-wp/Shiftora/blob/main/PRIVACY.md`. Politika je dostupna i lokalno u aplikaciji (**Više → Pravila privatnosti**).
- Data Safety: ručno provjeri obrazac u Play Console prema konačnoj implementaciji. Shiftora po zadanom nema analitiku, oglašavanje, račune ni internet dozvolu; korisnik sam bira i pokreće izvoz u druge datoteke ili aplikacije.
- Ispuni Content Rating, App Access, Target Audience, Ads, Financial Features (ako se pojavi pitanje zbog procjene plaće), kontakt i ostala obvezna pitanja stvarnim podacima izdavača.
- Koristi stvarne snimke aplikacije te vlastiti store icon i feature graphic. Ne predstavljaj procijenjenu bruto plaću kao službeni obračun ili neto iznos.
- Ako Play Console traži zatvoreno testiranje za novi developerski račun, ono se mora zasebno dovršiti prije produkcije.
- Validiraj bundle u Play Console internom testnom izdanju; provjeri Android 16 i tablet prilagodbu, edge-to-edge, sustavnu navigaciju, tamnu temu i povećana slova.
- Dok se sve to ne dovrši, **ne označavati izdanje kao odobreno od Google Playa**.

## Lokalni build

`./gradlew :app:testDebugUnitTest :app:lintDebug :app:lintRelease :app:assembleDebug :app:bundleRelease`

Za lokalno potpisivanje postavi privremene environment varijable `SHIFTORA_UPLOAD_KEYSTORE_FILE`, `SHIFTORA_UPLOAD_KEYSTORE_PASSWORD`, `SHIFTORA_UPLOAD_KEY_ALIAS`, `SHIFTORA_UPLOAD_KEY_PASSWORD` i izradi `assembleRelease bundleRelease`.
