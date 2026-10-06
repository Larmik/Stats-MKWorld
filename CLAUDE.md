# CLAUDE.md

Guide de référence pour travailler sur **Stats MKWorld** — application Android de suivi de statistiques pour les *wars* (matchs d'équipe) de Mario Kart World.

## Présentation

App Android native (Kotlin + Jetpack Compose) qui permet à des équipes compétitives de Mario Kart World d'enregistrer leurs *wars* course par course, de les synchroniser en temps réel via Firebase, et d'en tirer des statistiques détaillées (joueurs, équipes, circuits, adversaires). L'identité du joueur et les rosters proviennent de **MKCentral** ; la connexion se fait via **Discord OAuth2**.

- **Package / applicationId** : `fr.harmoniamk.statsmkworld`
- **Version** : `versionName` 4.1.0 / `versionCode` 26 (bump manuel à la release)
- **minSdk** 28 · **targetSdk / compileSdk** 37 · **Java 17**
- **Éditeur** : Harmonia
- Documentation détaillée : [docs/TECHNICAL.md](docs/TECHNICAL.md) (architecture) · [docs/FUNCTIONAL.md](docs/FUNCTIONAL.md) (fonctionnel)

## Commandes

```bash
# Build debug (applicationId suffixé .debug, label « Stats MKWorld (Dev) »)
./gradlew assembleDebug

# Build release (signé, minifié R8) — nécessite le keystore + local.properties
./gradlew assembleRelease

# Installer sur un appareil/émulateur connecté
./gradlew installDebug

# Tests unitaires (JVM) / instrumentés (device requis)
./gradlew test
./gradlew connectedAndroidTest

# Compilation rapide sans packaging
./gradlew compileDebugKotlin

# Nettoyage
./gradlew clean
```

> Le projet utilise le wrapper Gradle (`./gradlew`). Ne pas exiger un Gradle global.

### Prérequis de build

Le build **échoue sans `local.properties`** (lu à la configuration, pas seulement le SDK) :

```properties
sdk.dir=/chemin/vers/Android/sdk
DISCORD_API_SECRET="..."   # injecté dans BuildConfig.DISCORD_API_SECRET
DISCORD_API_CLIENT="..."   # injecté dans BuildConfig.DISCORD_API_CLIENT
```

Le build **release** référence aussi un keystore en chemin absolu (`signingConfigs`) — voir `app/build.gradle.kts`. Firebase nécessite `app/google-services.json` (+ variante debug). Ces fichiers ne sont pas versionnés en clair et contiennent des secrets : **ne jamais les exposer dans une sortie**.

## Workflow & process

Règles de collaboration à respecter systématiquement (pour l'agent comme pour l'humain) :

- **Pas de git sans demande explicite** : ne jamais `commit` / `push` / créer de PR tant que l'utilisateur ne l'a pas explicitement demandé. Appliquer les changements dans le working tree et attendre sa validation.
- **Synchroniser avant de commencer** : avant toute nouvelle tâche, `git fetch origin` puis se placer sur `master` à jour (`git pull --ff-only`). Créer les branches de travail **depuis `master`**.
- **Branche par défaut = `master`** : `master` est la branche de base du dépôt (PRs, syncs). Ignorer `main` (obsolète), même si l'outillage local la mentionne.
- **Documentation à la racine** : la doc fonctionnelle/technique reste dans `docs/` à la racine (`AUDIT.md`, `TECHNICAL.md`, `FUNCTIONAL.md`), séparée de `.claude/`. À chaque changement de comportement ou d'architecture, mettre à jour les sections `docs/` impactées (tenue de l'audit : `.claude/rules/process/documentation.md`).
- **Rules à portée** : les consignes par couche vivent dans `.claude/rules/**` (frontmatter `paths`, chargées à l'ouverture d'un fichier correspondant) ; index dans `.claude/rules-index.md`. Code, docs et config décrivent le code actuel, sans historique (git le garde).
- **Relecture anti-audit avant livraison** : avant de rendre la main / de créer une PR, relire le diff avec la checklist du § 4 de `.claude/agents/ticket-worker.md` (matrice audit ↔ rules : § 9 de `docs/AUDIT.md`). Un problème découvert hors périmètre est ajouté à `docs/AUDIT.md` plutôt que corrigé en passant, avec son issue créée via `/create-ticket` (*Suivi : #NN*).
- **Pas de tests unitaires spontanés** : ne pas créer de tests unitaires tant que l'utilisateur n'a pas indiqué comment il souhaite les écrire (la suite de tests actuelle est volontairement squelettique).
- **Périmètre des PR décidé par l'utilisateur** : c'est lui qui choisit ce que contient une PR. Ne pas commenter/remettre en question le titre ou la cohérence du périmètre d'une PR, ne jamais proposer de scinder des changements dans une PR dédiée. Regrouper les changements sur la branche en cours et créer **uniquement** les PR explicitement demandées.

## Stack technique

- **UI** : Jetpack Compose (Material3), Navigation Compose, Accompanist Pager, Coil (images), Lottie (animations), MPAndroidChart (graphes), vues XML résiduelles (ViewBinding/DataBinding) pour le rendu PDF.
- **DI** : Hilt (Dagger). Repository/data source = interface + module `@Binds` (cf. `.claude/rules/data/repositories.md`) ; VM paramétrés via `@AssistedInject` Factory (cf. `.claude/rules/viewmodel/viewmodels.md`) ; workers via `@HiltWorker` + `@AssistedInject`.
- **Async** : Coroutines (pas d'appels bloquants) : `suspend` pour un one-shot, `Flow` pour une source qui émet dans le temps.
- **Persistance locale** : Room (SQLite, version 10) pour players/teams/wars/seasons/tournaments ; Proto DataStore (Protobuf) pour le profil/équipe MKCentral et la war en cours ; Preferences DataStore pour les flags/token.
- **Réseau** : Retrofit + OkHttp + Moshi. APIs Discord et MKCentral. Jsoup pour le scraping des records du monde.
- **Backend** : Firebase Realtime Database (source de vérité des wars), Remote Config (gating de version), Crashlytics, Analytics.
- **Background** : WorkManager (`InitStatsWorker`, `UpdateDataWorker`).

## Architecture

MVVM en couches, câblé par Hilt. Flux de données :

```
UI (Compose) → ViewModel → UseCase / Repository → DataSource → Room | Retrofit | Firebase
```

### Repère des packages (`app/src/main/java/fr/harmoniamk/statsmkworld/`)

| Package | Rôle |
|---|---|
| `activity/` | `MainActivity` (Compose host, splash, deep links) + `MainViewModel` (choix du startDestination, gating de version) |
| `application/` | `MainApplication` (`@HiltAndroidApp`, init WorkManager) |
| `screen/` | Un sous-package par écran : `Screen.kt` (Compose) + `ViewModel.kt`. `RootScreen.kt` = graphe de navigation |
| `ui/` | Composants réutilisables (`MKButton`, `MKText`, `MKDialog`…), `ui/cells/`, `ui/stats/`, `Resources.kt` |
| `repository/` | Logique métier / orchestration des données (DataStore, Database, Firebase, PDF, RemoteConfig, Worker, WorldRecords, Notification, Season, Tournament, Translation, Lorenzi, Diagnostic) |
| `datasource/` | `local/` (wrap des DAO Room) et `network/` (wrap Retrofit Discord/MKCentral) |
| `model/selectors/` | Modèles de sélection de la saisie (`PlayerSelector`, `PenaltySelector`) |
| `usecase/` | `FetchUseCase` : chaîne de synchro multi-étapes (player → team → allies → teams → wars) |
| `database/` | Room : `MKDatabase`, `dao/`, `entities/`, `converters/` (Moshi) |
| `model/firebase/` | Modèles « source de vérité » (`War`, `WarTrack`, `WarPosition`, `WarScore`, `WarPenalty`, `Shock`, `User`, `Tag`) |
| `model/local/` | Modèles de présentation/calcul (`WarDetails`, `Stats`, `Maps`, `PlayerScore`) + miroirs `Datastore*` (Protobuf) |
| `model/network/` | DTO MKCentral (`MKCPlayer`, `MKCTeam`, rosters…), Discord et Lorenzi (tab HLorenzi) |
| `serializers/` | Sérialiseurs Proto DataStore (`MKCPlayerSerializer`, `MKCTeamSerializer`, `WarSerializer`) |
| `worker/` | Workers WorkManager + `MKCoroutineWorker` / `MKWorkerBuilder` |
| `extension/` | Extensions Kotlin — **`WarExtension.kt` contient le cœur du calcul de stats** |
| `api/` | Interfaces Retrofit (MKCentral, Discord, Lorenzi) + `RetrofitUtils` |

Schémas Protobuf hors package : `app/src/main/proto/` (`mkc_player.proto`, `mkc_team.proto`, `war.proto`).

### Concepts métier clés

- **War** : un match. `War → List<WarTrack> → List<WarPosition>`. Deux modes : **12 joueurs** (1v1, 6 contre 6) et **24 joueurs** (3 équipes adverses, scores saisis via `WarScore`).
- **Scoring 12p** : position → points (`Int.positionToPoints`), score adverse = `82×nbTracks − scoreHost`, ajusté des `WarPenalty`. Logique dans `extension/WarExtension.kt` + `extension/IntegerExtension.kt`.
- **Maps** : enum `model/local/Maps.kt` énumérant les circuits MK World (label, image, coupe), avec règles d'intermissions.
- **Stats** : calculées à la demande en mémoire par les VM (`model/local/Stats.kt`, `extension/ListExtension.kt`, `WarExtension`), sans cache. `InitStatsWorker` n'hydrate plus que saisons et tournois officiels.
- **Firebase RTDB** : `currentWars/{rosterId}` (war live, écoutée en temps réel), `wars/{teamId}/{warId}` (historique), `users/{teamId}/{userId}`, `newAllies/{teamId}`, `tags/`, `debug/`. `currentWars/` et `tags/` sont lus par l'overlay OBS WarOverlay : contrat à préserver, cf. `.claude/rules/data/war-overlay.md`.

### Flux de démarrage (`MainViewModel`)

1. Si `RemoteConfig.minimumVersion > versionCode` → écran « mise à jour requise ».
2. Sinon, si un `mkcPlayer` valide (`id != 0`) existe en DataStore → `Home`.
3. Sinon → `Signup` (onboarding + Discord OAuth). Un deep link `statsmkworld.com?...=code` injecte le `code` OAuth.

## Conventions de code

- Code, commentaires et doc en **français**. Chaînes utilisateur bilingues : `res/values/` (anglais) + `res/values-fr/` (français), cf. `.claude/rules/ui/strings.md`.
- Un écran = un dossier `screen/<feature>/` avec `<Feature>Screen.kt` + `<Feature>ViewModel.kt`.
- Préfixe `MK` pour les composants UI maison (`MKButton`, `MKText`, `MKDialog`, `MKTextField`…).
- Repositories / data sources : ne jamais bloquer ; `suspend` pour un one-shot, `Flow` réservé aux émissions multiples (cf. `.claude/rules/data/repositories.md`).
- Modèles : ne pas confondre les trois couches — `model/firebase/*` (réseau/RTDB), `model/local/Datastore*` (Protobuf), `model/local/War­Details`/`Stats` (présentation/calcul). Les conversions se font via constructeurs dédiés.

## Pièges connus

- **Room sans migration** (`fallbackToDestructiveMigration`) : cf. `.claude/rules/data/room.md`.
- **Secrets en clair** dans `app/build.gradle.kts` (mot de passe keystore) et `google-services.json` / `local.properties` — ne pas les divulguer ; idéalement les externaliser.
- **`WorldRecordsRepository`** scrape `mkwrs.com` : fragile aux changements de HTML (heuristiques regex + cache d'en-têtes).
- Build impossible sans `local.properties` même pour une simple analyse — il est lu dès la phase de configuration Gradle.
- `versionCode` à incrémenter manuellement à chaque release (et cohérence avec `minimumVersion` de Remote Config).
- **`BuildConfig` n'est pas un coffre-fort** et `clear*()` vide toute la table : cf. `.claude/rules/build/release-securite.md` et `.claude/rules/data/repositories.md`.

## Tests

Tests squelettiques uniquement (`ExampleUnitTest`, `ExampleInstrumentedTest`). Pas de suite de tests significative à ce jour — privilégier la vérification par build + exécution sur device pour les changements UI/flux.
