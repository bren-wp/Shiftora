# Privatnost — Shiftora

Shiftora ne zahtijeva korisnički račun ni internetsku vezu. Nema analitiku, oglašivačke identifikatore, lokacijsko praćenje niti namjerno slanje osobnih podataka na poslužitelje.

## Pohrana

Raspored, arhivirane smjene, sati i postavke čuvaju se u lokalnoj SQLite bazi i postavkama aplikacije na uređaju. Ručno arhivirani unosi ostaju dostupni za oporavak; automatska rotacija i uvoz rasporeda ne brišu postojeće smjene. Automatsko spremanje u cloud putem Android backupa je isključeno. Deinstalacijom, brisanjem podataka, resetiranjem ili gubitkom uređaja podatci se mogu izgubiti ako korisnik nije izradio vlastitu sigurnosnu kopiju.

## Izvoz i vraćanje

CSV i JSON sigurnosne kopije nastaju isključivo kad korisnik odabere izvoz. Mjesto pohrane bira korisnik putem Android sustavnog dijaloga. Uvoz JSON sigurnosne kopije zahtijeva korisnikovu potvrdu i spaja podatke bez brisanja postojećih unosa. Konfliktni unosi se preskaču. JSON format v3 može sadržavati i arhivirane smjene.

## Dopuštenja

Manifest ne traži dozvolu za internet, lokaciju, kontakte ili pozive. Odabir datoteka obavlja se kroz Android sustavni odabir datoteka.

## Kontakt

Više informacija o brendu: https://brendigo.com
