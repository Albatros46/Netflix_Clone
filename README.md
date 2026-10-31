# Netflix Clone

Streaming-Plattform nach dem Vorbild von Netflix: Benutzer registrieren sich, bestätigen ihre
E-Mail-Adresse, stöbern durch Filme nach Genre, suchen, führen eine Merkliste und schauen Videos im
Player. Admins laden Videos und Poster hoch, veröffentlichen sie und verwalten die Benutzer.

| Schicht   | Technik                                                                         |
|-----------|---------------------------------------------------------------------------------|
| Frontend  | React 18, React Router, Vite (Port **5175**)                                    |
| Backend   | Spring Boot 3.5 (Java 21+), Spring Security mit JWT, Spring Data JPA (Port **8086**) |
| Datenbank | PostgreSQL 16 in Docker auf **localhost:5433**, Datenbank `netflix_clone`       |
| E-Mail    | Mailpit in Docker fängt alle E-Mails ab (Web-UI Port **8027**, SMTP **1027**)   |

Die Ports kollidieren nicht mit den anderen lokalen Projekten (`Cafe_Management_System`,
`library-management`); Port 5432 bleibt für eine lokale PostgreSQL-Installation frei.

## Funktionen

- **Startseite**: E-Mail eingeben – vorhandenes Konto führt zur Anmeldung, sonst zur Registrierung.
- **Registrierung** mit Name, E-Mail und Passwort. Anmelden geht erst nach Klick auf den
  Bestätigungslink in der E-Mail (24 Stunden gültig); die E-Mail lässt sich erneut senden.
- **Anmeldung** mit JWT (30 Tage gültig). Deaktivierte Konten können sich nicht anmelden.
- **Passwort vergessen** (Link 1 Stunde gültig) und **Passwort ändern** unter „Konto“.
- **Browse**: wechselnder Hero-Film, Zeilen nach Genre, Suche in Titel und Beschreibung,
  Detail-Dialog, „+ Meine Liste“.
- **Meine Liste**: die eigene Merkliste.
- **Player**: Video-Streaming mit HTTP-Range-Anfragen (Spulen funktioniert).
- **Videos** (Admin): Statistik (Anzahl, veröffentlicht, Entwürfe, Gesamtlaufzeit), Suche, Seiten,
  anlegen/bearbeiten mit Upload von Video (MP4/WebM/…) und Poster mit Fortschrittsanzeige,
  veröffentlichen/zurückziehen, löschen (inkl. Dateien).
- **Benutzer** (Admin): Liste mit Suche, anlegen (bekommt Bestätigungs-E-Mail), bearbeiten,
  Rolle ändern, aktivieren/deaktivieren, löschen. Das eigene Konto und der letzte aktive Admin sind
  geschützt.

Rollen: **Admin** landet in der Video-Verwaltung, **Benutzer** auf der Startseite mit Filmen.

## Voraussetzungen

- Docker Desktop (PostgreSQL und Mailpit)
- JDK 21 oder neuer, Maven 3.9+
- Node.js 20+

## Starten

Alles mit einem Befehl (PowerShell, im Projektordner):

```powershell
.\start-all.ps1
```

Das Skript startet bei Bedarf Docker Desktop, dann PostgreSQL und Mailpit (`docker compose`), baut das
Backend mit Maven und startet Backend und Vite-Dev-Server im Hintergrund.

| Was           | Adresse                                      |
|---------------|----------------------------------------------|
| Anwendung     | http://localhost:5175                        |
| Backend-API   | http://localhost:8086/api                    |
| E-Mails       | http://localhost:8027                        |
| PostgreSQL    | `localhost:5433/netflix_clone` (netflix / netflix) |

Konten: **admin@netflix.local / admin123** (Admin), **user@netflix.local / user123** (Benutzer).

Optionen:

```powershell
.\start-all.ps1 -SkipBuild    # ohne Maven-Build (vorhandenes com.NetflixClone\target\app.jar)
.\start-all.ps1 -NoFrontend   # nur Backend
.\stop-all.ps1                # alles stoppen
.\stop-all.ps1 -KeepInfra     # nur Backend/Frontend stoppen, PostgreSQL und Mailpit laufen weiter
```

Logs liegen in `logs\` (`backend.log`, `frontend.log`). Hochgeladene Dateien liegen in
`com.NetflixClone\upload\` (nicht im Git).

## Beispieldaten

Beim Start legt das Backend (`DataSeeder`) automatisch an, was noch fehlt:

- den Admin und den Demo-Benutzer (beide mit bestätigter E-Mail),
- 12 frei erfundene Filme in verschiedenen Genres mit generierten SVG-Postern, alle veröffentlicht.

Das ist wiederholbar: Vorhandene Konten (E-Mail) und Filme (Titel) werden übersprungen, fehlende Poster
neu geschrieben. Die Beispielfilme haben **keine Videodatei** – zum Abspielen im Admin-Bereich bei einem
Film eine MP4/WebM-Datei hochladen. Abschalten mit `SEED_ENABLED=false`.

Alles zurücksetzen: `.\stop-all.ps1`, dann `docker compose down -v` (löscht die Datenbank) und den
Ordner `com.NetflixClone\upload\` löschen.

### IntelliJ IDEA

Den Ordner `com.NetflixClone` öffnen. Unter *Run Configurations* gibt es `infra (postgres + mailpit)`,
`backend`, `frontend` und `All`.

## Konfiguration

Umgebungsvariablen des Backends (Standardwerte für die lokale Entwicklung):

| Variable          | Standard                                              |
|-------------------|-------------------------------------------------------|
| `DB_URL`          | `jdbc:postgresql://localhost:5433/netflix_clone`      |
| `DB_USERNAME` / `DB_PASSWORD` | `netflix` / `netflix`                     |
| `JWT_SECRET`      | Entwicklungs-Schlüssel – **in Produktion ändern** (mind. 32 Byte) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@netflix.local` / `admin123`     |
| `SEED_ENABLED`    | `true`                                                |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1027` (Mailpit)                |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS` | für einen echten SMTP-Server, z. B. Gmail mit App-Passwort |
| `MAIL_FROM`       | `no-reply@netflix-clone.local`                        |
| `FRONTEND_URL` / `CORS_ORIGINS` | `http://localhost:5175`                 |
| `UPLOAD_VIDEO_DIR` / `UPLOAD_IMAGE_DIR` | `upload/videos` / `upload/images` |

Zugangsdaten wie ein Gmail-App-Passwort gehören in Umgebungsvariablen, nie in
`application.properties`.

## Projektstruktur

```
com.NetflixClone/                 (Repository)
├── com.NetflixClone/             Spring Boot (Maven)
│   └── src/main/java/com/NetflixClone/
│       ├── controller/           Auth, User, Video, File
│       ├── service/, serviceImpl/
│       ├── entity/, dao/, dto/   JPA-Entitäten, Repositories, Request/Response
│       ├── security/             JWT
│       ├── config/               Security, CORS, DataSeeder
│       └── exception/            Fehlerbehandlung
├── frontend/                     React + Vite
│   └── src/pages/                Landing, Login, Signup, VerifyEmail, ForgotPassword, ResetPassword,
│                                 Browse, Watchlist, Watch, Account, admin/AdminVideos, admin/AdminUsers
├── docker-compose.yml            PostgreSQL + Mailpit
├── start-all.ps1 / stop-all.ps1
└── .run/                         IntelliJ-Run-Konfigurationen
```

## REST-API (Auszug)

Alle Pfade unter `/api`; außer Auth-Endpunkten und Datei-Abruf mit `Authorization: Bearer <token>`.

| Methode | Pfad                                                        | Wer        |
|---------|-------------------------------------------------------------|------------|
| POST    | `/auth/signup`, `/auth/login`, `/auth/resend-verification`  | alle       |
| GET     | `/auth/validate-email?email=`, `/auth/verify-email?token=`  | alle       |
| POST    | `/auth/forgot-password`, `/auth/reset-password`             | alle       |
| GET     | `/auth/current-user` · POST `/auth/change-password`         | angemeldet |
| GET     | `/videos/published`, `/videos/featured`, `/videos/{id}`     | angemeldet |
| GET/POST/DELETE | `/videos/watchlist[/{videoId}]`                     | angemeldet |
| GET/POST | `/videos/admin`, `/videos/admin/stats`                     | Admin      |
| PUT/DELETE | `/videos/admin/{id}` · PATCH `/videos/admin/{id}/publish?value=` | Admin |
| POST    | `/files/upload/video`, `/files/upload/image`                | Admin      |
| GET     | `/files/video/{uuid}` (Range), `/files/image/{uuid}`        | alle       |
| GET/POST | `/users`, `/users/create-user`                             | Admin      |
| PUT/DELETE | `/users/{id}` · PUT `/users/{id}/toggle-status`, `/users/{id}/change-role` | Admin |
