# UnserAlltag 💜

Eine Android-App, mit der ihr zu zweit euren Alltag organisiert – alles wird in Echtzeit zwischen euren Handys synchronisiert.

| Bereich | Was geht |
|---|---|
| **Heute** | Startseite mit den nächsten Terminen, Geburtstagen, fälligen Aufgaben, dem heutigen Essen, offenen Einkäufen und Kündigungsfristen |
| **Kalender** | Monatsansicht, Termine für „Beide“ oder eine Person (farbig), ganztägig, wiederkehrend (täglich/wöchentlich/monatlich/jährlich), Erinnerungen |
| **Einkauf** | Mehrere Listen (z. B. Supermarkt, Drogerie), Abhaken, Mengen („2 Milch“), Vorschläge aus bisherigen Einkäufen |
| **Aufgaben** | Wer macht was, Fälligkeitsdatum, wiederkehrende Aufgaben (z. B. „Müll rausbringen – wöchentlich“) |
| **Geburtstage & Jahrestage** | Sortiert nach dem nächsten Termin, mit Alter („wird 35“) und Erinnerung vorab |
| **Wunschlisten** | Pro Person. Der Partner kann Wünsche als „besorgt“ markieren und **geheime Geschenkideen** eintragen – beides sieht der Beschenkte nicht |
| **Essensplan** | Wochenplan, Zutaten mit einem Tipp auf die Einkaufsliste setzen, frühere Gerichte als Vorlage |
| **Notizen** | Gemeinsame Notizen (WLAN-Passwort, Rezepte, …), anheftbar und durchsuchbar |
| **Verträge & Fristen** | Kosten pro Monat/Jahr, Vertragsende, Kündigungsfrist → „spätestens kündigen am …“ plus Erinnerung |

Technik: Kotlin, Jetpack Compose (Material 3 mit dynamischen Farben und Dark Mode), Firebase Authentication (Google-Anmeldung) und Cloud Firestore (funktioniert auch offline).

---

## Einrichtung (einmalig, ca. 15 Minuten)

### 1. Firebase-Projekt anlegen
1. <https://console.firebase.google.com> öffnen → **Projekt hinzufügen** → Name z. B. `UnserAlltag` (Google Analytics kann aus bleiben).
2. Im Projekt auf das **Android-Symbol** klicken („App hinzufügen“):
   - **Android-Paketname:** `de.unseralltag.app`
   - **SHA-1-Zertifikat:** den SHA-1-Fingerabdruck deines Signaturschlüssels (steht in der Datei, die du von Claude bekommen hast)
   - Die Datei `google-services.json` **noch nicht** herunterladen (erst nach Schritt 2) – die restlichen Schritte des Assistenten kannst du überspringen.

### 2. Google-Anmeldung aktivieren
**Build → Authentication → Jetzt starten → Anbieter „Google“** → aktivieren, Support-E-Mail auswählen → Speichern.

### 3. Datenbank anlegen
1. **Build → Firestore Database → Datenbank erstellen**, Standort `europe-west3 (Frankfurt)`, **Produktionsmodus**.
2. Reiter **Regeln**: den Inhalt der Datei [`firestore.rules`](firestore.rules) aus diesem Repository einfügen → **Veröffentlichen**.

### 4. Konfiguration herunterladen
**Projekteinstellungen (Zahnrad) → Allgemein → Deine Apps → `google-services.json` herunterladen.**
(Wichtig: erst *nach* Schritt 2, sonst fehlt die Google-Anmeldung in der Datei.)

### 5. GitHub-Secrets hinterlegen
Im GitHub-Repository: **Settings → Secrets and variables → Actions → New repository secret**

| Name | Inhalt |
|---|---|
| `GOOGLE_SERVICES_JSON` | kompletter Inhalt der Datei `google-services.json` (mit einem Texteditor öffnen und kopieren) |
| `SIGNING_KEYSTORE_BASE64` | der lange Text aus der Schlüssel-Datei |
| `SIGNING_KEYSTORE_PASSWORD` | das Passwort aus der Schlüssel-Datei |

> 🔐 Bewahrt die Schlüssel-Datei sicher auf (z. B. im Passwortmanager). Geht sie verloren, muss die App für Updates einmal neu installiert werden.

### 6. App bauen & installieren
1. **Actions → „APK bauen“ → Run workflow** (oder einfach etwas pushen).
2. Nach ca. 5 Minuten erscheint unter **Releases** eine neue Version. Auf dem Handy die Datei `UnserAlltag-….apk` herunterladen und öffnen (Installation aus „unbekannten Quellen“ einmalig erlauben).
3. Für Updates einfach die neueste APK erneut installieren – die Daten bleiben erhalten.

### 7. Loslegen
1. Du: App öffnen → **Mit Google anmelden** → **Neu erstellen**.
2. Unter **Einstellungen** (Zahnrad auf „Heute“) steht euer **Einladungscode** → per **Teilen** an deine Frau schicken.
3. Deine Frau: App installieren → anmelden → **Beitreten** → Code eingeben. Fertig – ab jetzt seht ihr beide dieselben Daten.

---

## Erinnerungen
Die App prüft etwa alle 15 Minuten im Hintergrund auf anstehende Termine (Termine für „Beide“ oder für dich) und meldet sich ab 8 Uhr morgens mit Geburtstagen, fälligen Aufgaben und Kündigungsfristen. Falls Benachrichtigungen ausbleiben: In den Android-Einstellungen die Akku-Optimierung für UnserAlltag deaktivieren.

## Entwicklung
- Android Studio öffnen, `google-services.json` nach `app/` kopieren und starten.
- Tests: `./gradlew testDebugUnitTest`
- Die Datei `app/google-services.json` und Schlüssel sind per `.gitignore` vom Repository ausgeschlossen.
