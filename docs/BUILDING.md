# Shiftora — tehnička dokumentacija

Shiftora je izvorna Android aplikacija napisana u Kotlinu i Jetpack Composeu. U aplikaciji nisu potrebne registracija, dozvola za internet niti mrežna usluga.

## Razvoj i provjera

- JDK 17, Android SDK 36
- Gradle wrapper: `./gradlew` (8.11.1)
- Lokalno: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:bundleRelease`
- Automatizirani Android UI testovi: `.github/workflows/android-ui.yml`
- Lokalna SQLite baza; sadržaj ne napušta uređaj osim kada korisnik odabere izvoz/backup

## Izdanja

GitHub Actions stvara instalacijski debug APK i nepotpisani release AAB. APK potpisan debug ključem može se testno instalirati, ali **nije produkcijski potpisan APK**. Nepotpisani AAB **nije spreman za Google Play**. Ključevi za potpisivanje moraju biti pod kontrolom vlasnika aplikacije; ne smiju se pohraniti u javni repozitorij.

CI provjerava Kotlin/JVM unit testove, kompilaciju Compose instrumentacijskih testova, Android Lint, `assembleDebug` i `bundleRelease`. UI test na emulatoru provjerava glavnu navigaciju i sprema stvarne snimke ekrana. Testovi na emulatoru nisu zamjena za testiranje na fizičkim uređajima.

## Obračun

Obračun raspoređuje svaku minutu na stvarni lokalni datum, uz uvažavanje prelaska preko ponoći i promjene ljetnog/zimskog vremena. Dodatci (noćni, nedjeljni, blagdan i prekovremeni) mogu se preklapati i ne predstavljaju međusobno isključive kategorije sati. Postotci su korisnički podesivi; aplikacija ne utvrđuje automatski prava prema pojedinom ugovoru ili kolektivnom ugovoru.

## Snimke za javnu galeriju

Autentične snimke s emulatora pohranjuju se u Git granu `readme-media` pod `screenshots/`. To nisu generirani mockupovi. U screenshot testovima koriste se ilustrativni, lokalno uneseni rasporedi, ne osobni podatci korisnika.

## Licenca

MIT. Vizualni identitet: Shiftora / Brendigo.
