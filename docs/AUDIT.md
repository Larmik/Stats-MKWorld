# Audit technique — Stats MKWorld

> Revue complète du projet : points bloquants, bugs/correctness, performance, duplication & refactoring, best practices, tests, dette technique.
> Établi par analyse statique du code (version 3.0.0, `versionCode` 23).

**Légende de sévérité** : 🔴 Bloquant/critique · 🟠 Important · 🟡 Moyen · 🟢 Cosmétique/confort.

Les numéros de ligne sont indicatifs (état au moment de l'audit) — à reconfirmer avant correction.

## Sommaire
1. [Bloquant & sécurité](#1-bloquant--sécurité)
2. [Bugs & correctness](#2-bugs--correctness)
3. [Performance](#3-performance)
4. [Duplication & refactoring](#4-duplication--refactoring)
5. [Best practices Android/Kotlin](#5-best-practices-androidkotlin)
6. [Tests & outillage](#6-tests--outillage)
7. [Dette technique & constantes magiques](#7-dette-technique--constantes-magiques)
8. [Feuille de route priorisée](#8-feuille-de-route-priorisée)

---

## 1. Bloquant & sécurité

- [x] ✅ **A1 — Secrets en clair, versionnés.** Keystore (chemin absolu + mots de passe) externalisé dans `local.properties`. `google-services.json` ajouté au `.gitignore` et désindexé. Secrets Discord déjà dans `local.properties`. **Reste : faire tourner le secret Discord si considéré comme compromis.**
- [ ] 🔴 **A2 — Règles de sécurité Firebase RTDB à auditer.** L'app lit/écrit `users/`, `wars/`, `currentWars/`, `newAllies/`, `tags/`, `debug/` sans couche d'autorisation applicative (Discord OAuth ≠ Firebase Auth). Si les règles RTDB sont publiques, n'importe qui peut altérer les wars de n'importe quelle équipe. **À vérifier en console Firebase** (hors-repo).
- [x] ✅ **A3 — Build sans `local.properties`.** `localProps` chargé conditionnellement en tête de `build.gradle.kts` (une seule lecture, valeurs de repli vides). Le build debug passe sans le fichier ; seule la fonctionnalité Discord est non-opérationnelle.

---

## 2. Bugs & correctness

- [x] ✅ **B1 — Crash potentiel `subList(0, 2)`.** Appliqué : `safeSubList(0, 2)` aux deux branches 24p de `withFullStats` ([ListExtension.kt:227–228](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)).
- [x] ✅ **B2 — Flag `is24p` perdu dans les classements d'adversaires.** Appliqué : `withFullTeamStats` accepte `is24p: Boolean = false` et le propage à `withFullStats` ; `InitStatsWorker` passe `is24p = is24PEnabled` aux deux appels `opponentRankList` / `playerOpponentRankList`.
- [x] ✅ **B3 — Shadowing trompeur de `is24p`.** Appliqué : le local renommé `warIs24p` dans `withFullStats` ([ListExtension.kt:90](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)) ; plus aucun masquage du paramètre.
- [x] ~~🟠 **B4 — `withTrackStats` : division incohérente de `teamScore`.**~~ **Analyse incorrecte.** Une intermission est une course unique (l'index double identifie 2 circuits joués simultanément, pas 2 courses). `teamScoreForTrack` est déjà le score d'une seule course et ne dépend pas de la taille du tableau d'index. Le comportement original (`teamScore = teamScoreForTrack` sans division) est correct.
- [x] ✅ **B5 — `WarTrack.diffScore` force le mode 12p.** Appliqué : `diffScore` est désormais une fonction `diffScore(is24p: Boolean = false)` ([WarTrack.kt:27](../app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/WarTrack.kt)) utilisant `positionToPoints(is24p)`. Les 3 appelants propagent le mode : `LineChartExtension` (via `WarTrackDetails.is24p`) et `withTrackStats` (via le `is24p` local). **Suite review** : le complément adverse (max points/manche) est lui aussi rendu dynamique via `when(is24p)` — `MAX_POINTS_PER_TRACK_24P` (144) en 24p, `MAX_POINTS_PER_TRACK_12P` (82) sinon — dans `WarTrack.diffScore` **et** `WarTrackDetails.opponentScore`. La même incohérence (host en `positionToPoints(is24p)` mais complément en dur sur 82) a aussi été corrigée dans `AddTrackViewModel` et `TrackDetailsViewModel`. Voir G2 pour les déclinaisons de constantes. Le bloc `/** 12 players */` de `WarDetails` reste en 12p (doublé d'un bloc `/** 24 players */` `scores`/`diffs`, basculé par `Stats` via `when(is24p)`).
- [x] ✅ **B6 — `PlayerEntity` est `class`, pas `data class`.** Appliqué : `PlayerEntity` est passé en `data class` ([entities/PlayerEntity.kt:10](../app/src/main/java/fr/harmoniamk/statsmkworld/database/entities/PlayerEntity.kt)) → `equals`/`hashCode` par valeur ; `withPlayersList` `groupBy { it.player }` regroupe maintenant par valeur.
- [x] ✅ **B7 — Erreurs réseau silencieuses.** Appliqué, puis **renforcé** par la migration Flow→suspend : toutes les méthodes réseau renvoient désormais `NetworkResponse<T>` (plus de `null` ambigu). La journalisation Crashlytics est **centralisée** dans `NetworkResponseCallAdapter` (`recordException(t)` sur exception, `log("HTTP <code> error: …")` avec `errorBody()` sur erreur HTTP) — un seul point pour tous les appels. Les appelants déballent via `.successResponse`.
- [x] ✅ **B8 — Modèles « source de vérité » mutables.** Appliqué : `WarScore`/`Shock` passent en `val` (aucune réassignation de champ dans le code, vérifié).
- [x] ✅ **B9 — Noms de méthodes erronés dans `WarScoreConverter`.** Appliqué : renommés en `fromWarScoreList`/`toWarScoreList` ([WarScoreConverter.kt](../app/src/main/java/fr/harmoniamk/statsmkworld/database/converters/WarScoreConverter.kt)).
- [ ] 🟠 **B11 — Adversaire d'une war stocké en `teamId`, pas en `rosterId`.** À la création d'une war ([AddWarViewModel.createWar()](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addWar/AddWarViewModel.kt)), `teamHost` reçoit le `rosterId` mkworld du joueur hôte, mais `teamOpponent` reçoit `teamSelected.map { it.id }` = le **`teamId`** de `TeamEntity`. Conséquence : pour un adversaire possédant **plusieurs rosters** mkworld, les wars ne sont pas rattachables à un roster précis → statistiques et résultats indistinguables en multi-roster. Asymétrie host(roster)/opponent(team) à la racine. **Correctifs** : (1) étape de sélection de roster à l'ajout de war (bottomSheet conditionnel si l'équipe a >1 roster) ; (2) adaptation des stats/affichage ; (3) migration RTDB des wars existantes (`opponentId` teamId → premier rosterId mkworld). Voir [TECHNICAL.md §6](TECHNICAL.md#6-modèle-de-données--les-trois-couches) (identité MKCentral). Tickets dédiés rédigés.
- [x] ✅ **B10 — Rôle d'un membre réinitialisé à 0 durant le cycle de war.** Les opérations de war (création, validation, annulation, remplacement de joueur) réécrivaient l'objet `User` complet via `setValue(user)` en repassant `role = it.role` issu de la `PlayerEntity` locale ; dès que ce rôle local était périmé/à 0, l'opération recopiait fidèlement ce `0` dans Firebase (le cycle de war était le **vecteur de propagation**, pas la source). Appliqué : `FirebaseRepository.updateUserCurrentWar`/`updateAllyCurrentWar` ne mettent à jour que le champ `currentWar` via `updateChildren` (fallback `setValue` si le nœud n'existe pas encore) ; les 5 emplacements du cycle de war ([AddWarViewModel](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addWar/AddWarViewModel.kt), [CurrentWarViewModel](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/CurrentWarViewModel.kt), [CurrentWarActionsViewModel](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/CurrentWarActionsViewModel.kt)) y basculent. Un **allié** ayant toujours `role = 0`, seuls les membres (nœud `users`) sont concernés. **Reste (basse priorité, ticket à part)** : `FetchUseCase.manageTransferts()` (≈ L206) traite *tous* les membres du roster, pas seulement les transférés, et pourrait remettre un `role > 0` à `0` lors d'une relance (déclenché uniquement depuis l'écran Debug — comportement métier membre↔allié par ailleurs voulu).

---

## 3. Performance

- [x] ✅ **P1 — Nouveau `OkHttpClient` + `Retrofit` à chaque appel réseau.** Appliqué (migration Flow→suspend) : `RetrofitUtils` expose un `baseClient` OkHttp, une `MoshiConverterFactory` et un `NetworkResponseCallAdapterFactory` **construits une seule fois** (`by lazy`). Les variations de timeout passent par `baseClient.newBuilder()` (réutilise pool de connexions/dispatcher). Les `Retrofit` sont mis en **cache** par clé `url|timeout|factory`. Plus aucune reconstruction par appel.
- [ ] 🟠 **P2 — Pagination MKCentral séquentielle bloquante.** [FetchUseCase.fetchTeams()](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt) enchaîne les pages via `firstOrNull()` dans des `while` (mkworld puis mk8dx, page par page). Lent sur grosse base. → Paralléliser (`async`/`flatMapMerge`).
- [ ] 🟠 **P3 — `InitStatsWorker` : N appels `getTeam(...).firstOrNull()`.** La résolution `TeamStats` interroge Room équipe par équipe, par joueur **et** par équipe. → Charger une seule `Map<id, TeamEntity>`.
- [ ] 🟡 **P4 — `shareIn(this, Eagerly)` inutile.** [FetchUseCase.kt:225-231](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt) crée des flux partagés *Eagerly* pour un usage one-shot (consommé par `firstOrNull`). → `firstOrNull()` direct.
- [ ] 🟡 **P5 — Gating de version dépendant du réseau au démarrage.** `MainViewModel` attend `remoteConfigRepository.minimumVersion()` (`fetch(0)`, cache 0 s) avant de router → démarrage retardé/risqué hors-ligne. → Cache court + repli offline.
- [ ] 🟡 **P6 — Recomputations dans `Stats`.** `maps.filter { it.totalPlayed >= 2 }` recalculé **5×** ([Stats.kt:25-33](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/Stats.kt)) ; `takeIf { isNotEmpty() }?.size ?: 1` répété 3× ([Stats.kt:34-41](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/Stats.kt)). → Base commune `val mapsAboveThreshold = …` + helper `sizeOrOne()`.

---

## 4. Duplication & refactoring

### 4.1 Couche données

- [x] ✅ **D1 — Pattern callbackFlow + Retrofit dupliqué ~9×.** Résolu par la **migration Flow→suspend** : les datasources réseau exposent des `suspend fun … : NetworkResponse<T>` et délèguent à des APIs Retrofit `suspend`. Le `callbackFlow { … enqueue … awaitClose }` est entièrement supprimé ; un `NetworkResponseCallAdapter` (Retrofit) centralise une fois pour toutes la conversion succès/erreur **et** la journalisation Crashlytics (voir B7). Chaque méthode de datasource tient en une expression.
- [ ] 🟡 **D2 — Convertisseurs Room quasi identiques (×5).** [database/converters/](../app/src/main/java/fr/harmoniamk/statsmkworld/database/converters/) : `WarPosition`/`WarScore`/`WarPenalty`/`String`Converter partagent à l'identique le bloc `adapter.toJson` / `try { fromJson } catch { arrayListOf() }`. → Un seul `MoshiListConverter<T>` générique (garder la spécificité `NumberToIntAdapterFactory` de `WarTrackConverter`).
- [x] ✅ **D3 — Wrappers `flow { emit(dao.x()) }` + passe-plats.** Résolu par la **migration Flow→suspend des repositories** : les mutations des 3 data sources locales et de `DatabaseRepository` sont désormais des `suspend fun` (plus de wrapper `flow { emit(dao.x()) }` ; `withContext(Dispatchers.IO)` au niveau repository). De même côté `FirebaseRepository`, les opérations one-shot (lectures `.get()` + écritures) sont `suspend`. Restent en `Flow` les seules lectures réellement réactives (Room streaming + `listenToCurrentWar`). La fusion data source locale ↔ repository n'a pas été faite (couches conservées).
- [ ] 🟡 **D4 — Boilerplate DI répété ~10×.** Interface + `@Module @Binds @Singleton` + impl pour chaque repo/datasource/usecase. Inhérent à Hilt, mais regroupable (modules par couche). À documenter comme convention plutôt qu'à supprimer.
- [ ] 🟡 **D5 — `FetchUseCase.fetchTeams` : mapping `MKCTeam → TeamEntity` dupliqué 4× + 2 boucles de pagination jumelles.** → `private fun MKCTeam.toEntity()` + `paginate(loader, mapper)` générique.
- [ ] 🟢 **D6 — Chaînes `firstOrNull()` répétées** dans `FetchUseCase` (sync roster, allies). Extraire `syncPlayersFromRoster(roster, teamId)`.

### 4.2 Moteur de statistiques (`extension/` + `model/local/`)

- [ ] 🟠 **D7 — Tables `MapStats` copiées-collées (~110 lignes).** [Stats.kt:146-262](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/Stats.kt) : `topsTable`/`bottomsTable`/`indivTopsTable`/`indivBottomsTable` = blocs `Pair("Top N", filter{…}.size)` répétés. → Générer par boucle `(2..6).map { … }`.
- [ ] 🟠 **D8 — `withFullStats` : 18 lignes identiques entre branches 12p/24p.** Le bloc `flowOf(Stats(...)).map { résoudre TeamStats }` et le calcul `mostPlayed/mostDefeated/lessDefeated` sont dupliqués. → Helper `List<Pair<String, …>>.toTeamStats(db)` + factorisation des 3 listes.
- [ ] 🟡 **D9 — `withTrackStats` : calcul commun + branches index simple/double redondantes** ([ListExtension.kt:306-346](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)). Extraire le calcul de scores et un builder `(indexes) → Pair(trackIndex, maps)` (corrige aussi B4).
- [ ] 🟡 **D10 — `warScoreToDiff` ≈ `trackScoreToDiff`** ([IntegerExtension.kt:121-145](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/IntegerExtension.kt)) : 95% identiques, seule la référence diffère (492 vs 41). → `private fun Int.scoreToDiff(midpoint: Int)`.
- [ ] 🟡 **D11 — `Datastore*` (×6) : boilerplate de conversion (~210 lignes).** Ctor(firebase) + ctor(proto) + getter `proto` mécaniques. → Fonctions d'extension de mapping ; à terme, génération.
- [ ] 🟡 **D12 — Constructeurs de conversion triviaux des modèles firebase (×12).** `War(entity)`, `WarTrack(track)`, etc. → Extensions `toX()` partagées.
- [ ] 🟢 **D13 — `positionToPoints`/`pointsToPosition`/`positionColor` : grandes tables `when`.** Lisibilité améliorable via `mapOf(...)` (lookup).
- [ ] 🟢 **D14 — `List<Int?>?.sum()` maison** ([ListExtension.kt:72-75](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)) redondant avec `sumOf { it ?: 0 }`. → Supprimer.
- [ ] 🟢 **D15 — Boucles manuelles dans `withPlayersList`** ([WarExtension.kt:47-82](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/WarExtension.kt)) remplaçables par `flatMap`/`groupBy`/`map`.

### 4.3 Couche UI (`ui/`, `ui/cells/`, `ui/stats/`)

- [ ] 🟠 **D16 — Rendu du logo d'équipe dupliqué 16×.** Bloc `when (logo) { null → Image(default_logo) ; else → AsyncImage("https://mkcentral.com$logo") }` répété dans `WarScoreView` (×8), `CurrentWarCell` (×4), `WarCell` (×3), `TeamCell`. → `@Composable TeamLogoImage(team, size)` + util `logoUrl(path)`.
- [ ] 🟠 **D17 — Bloc « label + valeur » des cellules de stats dupliqué (20+).** `MKWarDetailsStatsCell`, `MKWinTieLossCell`, `MKPlayerScoreCell`, `MKTopBottomCell` répètent `Column { MKText(label) ; MKText(value) }`. → `@Composable StatBlock(label, value, …)`.
- [ ] 🟡 **D18 — Style de carte répété (12+).** `.background(color, RoundedCornerShape(5.dp)).border(1.dp, white, RoundedCornerShape(5.dp))`. → `Modifier.mkCard()`.
- [ ] 🟡 **D19 — Pas de design-system.** [ui/Resources.kt](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/Resources.kt) centralise couleurs/fonts mais pas les **espacements** (100+ `dp` magiques), **formes**, ni **presets typographiques** (combinaisons font+taille+couleur répétées 50+). `textColor = Colors.white` répété 80+. → Objets `Spacing`/`Shapes`/`TextStyles`.
- [ ] 🟡 **D20 — `WarScoreView` monolithique (~456 lignes)** avec imbrication profonde `when/Column/Row/forEach`. → Découper (`WarScore24PDisplay`, `WarScore1v1Display`, `WarPenaltiesSection`, `TeamLogoImage`).
- [ ] 🟡 **D21 — `CurrentWarCellViewModel` ≈ `WarCellViewModel`.** Même logique (résolution des équipes adverses + nom/id de roster). → Base/util commune.
- [ ] 🟢 **D22 — `WarPlayersCell` : 2 `LazyColumn` quasi identiques** (gauche/droite, seul l'intervalle change). → `@Composable PlayersList(players, range)` appelé 2×.
- [ ] 🟢 **D23 — Cellules `PlayerCell`/`TeamCell`/`MapCell`** partagent la structure carte + section ranking optionnelle. Envisager un conteneur de base.

### 4.4 Écrans & ViewModels (`screen/`)

- [ ] 🟠 **D24 — Boilerplate ViewModel répété (15+ VMs).** `data class State` + `MutableStateFlow` + `SharedFlow` d'événements + `.mergeWith(_state).stateIn(scope, WhileSubscribed(5000), …)` identique. → `BaseViewModel<S>` ou extension `Flow<T>.mergedStateIn(scope, mutableState)`.
- [ ] 🟠 **D25 — Pagination recherche joueurs dupliquée.** Algo identique dans `RegistryViewModel` et `TeamProfileViewModel` (seul le filtre alliés diffère). → util `paginatePlayerSearch(term, excludeIds)`.
- [ ] 🟠 **D26 — Chaîne `fetchPlayer → fetchTeam → fetchAllies → fetchTeams → clearWars → fetchWars` dupliquée 3×** (`PlayerProfileViewModel.onRefresh`, `SignupViewModel`, `DebugViewModel.onMatrix`). → exposer `fetchFullTeamData(playerId)` dans `FetchUseCase` (et y rapatrier la logique).
- [ ] 🟡 **D27 — Écriture allié/user Firebase (when `rosterId == "-1"`) dupliquée 7×** (CurrentWarActions, CurrentWar, AddWar, PlayerProfile, TeamProfile). → `PlayerEntity.writeToFirebase(repo, teamId, currentWar)`.
- [ ] 🟡 **D28 — Filtre roster « mkworld » répété 17×** (`rosters?.firstOrNull { it.game == "mkworld" }`). → `MKCPlayer.mkWorldRoster()` / `MKCTeam.mkWorldRosters()`.
- [ ] 🟡 **D29 — Lectures DataStore répétées** (`mkcTeam`/`mkcPlayer`/`is24PEnabled` re-lus de façon identique dans 9+ VMs). → helpers suspendus `currentTeam()/currentPlayer()/is24pMode()`.
- [ ] 🟡 **D30 — Filtre de mode war dupliqué (5+)** (`(is24p && size>1) || (!is24p && size==1)`), parfois deux fois dans le même fichier ([WelcomeViewModel](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/WelcomeViewModel.kt)). → `War.matchesMode(is24p)`.
- [ ] 🟡 **D31 — Mapping rôle→libellé dupliqué** (`when { 1→admin ; 2→leader ; else→membre }`) dans PlayerProfile/Debug. → `Int.toRoleStringRes()` + un enum `Role`.
- [ ] 🟢 **D32 — Couleur de bordure de `MapCell` selon le diff dupliquée** (CurrentWarScreen ≈ WarDetailsScreen). → helper `mapCellBorderColor(is24p, diff)`.
- [ ] 🟢 **D33 — Plomberie de callbacks de navigation** : chaque écran réinvente des signatures de callbacks + `savedStateHandle` get/set. → événements de navigation typés / `NavController` partagé.

---

## 5. Best practices Android/Kotlin

- [ ] 🟡 **C1 — `fallbackToDestructiveMigration()`** : perte des données locales à chaque montée de schéma (acceptable car re-sync, mais à documenter et idéalement migrer les schémas).
- [ ] 🟡 **C2 — Désérialisation Firebase « à la main »** (cast `Map<*,*>` + parse champ par champ). Fragile/verbeux. → `GenericTypeIndicator` ou data classes typées.
- [x] ✅ **C3 — `kotlinCompilerExtensionVersion = "1.5.12"`** Résolu : la ligne est absente de `app/build.gradle.kts` ; le plugin `kotlin.compose.compiler` est déclaré via alias dans les deux `build.gradle.kts` et gère la version.
- [ ] 🟢 **C4 — `viewBinding` + `dataBinding`** activés pour 3 layouts PDF uniquement (coût de build). Évaluer un rendu Compose→bitmap.
- [ ] 🟢 **C5 — ProGuard `-dontoptimize`** : désactive l'optimisation R8 (taille/perf). À réévaluer.
- [ ] 🟢 **C6 — Opt-ins expérimentaux** (`@FlowPreview`, `@ExperimentalCoroutinesApi`) très répandus ; surveiller lors des montées de version.

---

## 6. Tests & outillage

- [~] 🟠 **T1 — Couverture de tests** : ~~aucune~~ → **suite E2E Maestro** ajoutée (`.maestro/`, cf. [TESTS_FUNCTIONAL.md](TESTS_FUNCTIONAL.md)) — lifecycles war 12p/24p (création, course, pénalité, remplacement, édition, annulation), navigation, garde-fous de création, profils, annuaire, stats, PDF ; tirages aléatoires + **scoring vérifié (property-based)** via `scripts/pick.js` ; flows idempotents/non destructifs sur l'env. Firebase **debug**. **Reste** : tests **unitaires JVM** du moteur pur (`positionToPoints`, `withFullStats`, `WarDetails`, `WarStats`) — meilleur ROI, complémentaires aux E2E (auraient attrapé B1, B2/B4).
- [ ] 🟡 **T2 — Pas de CI.** → Pipeline build + tests + `lint` (les flows Maestro nécessitent un device/émulateur).
- [ ] 🟡 **T3 — `versionCode` manuel** (risque d'incohérence avec `minimumVersion` Remote Config). → Bump automatisé.

---

## 7. Dette technique & constantes magiques

- [ ] 🟡 **G1 — Id joueur de référence `"18595"` codé en dur.** Partiellement traité : centralisé dans `ScoringConstants.DEBUG_PLAYER_ID` (tous les usages mis à jour dans `PlayerCell`, `PlayerProfileScreen`, `DebugViewModel`). Reste : migrer vers Remote Config ou `BuildConfig` pour résilience si le compte disparaît.
- [x] ✅ **G2 — Constantes de scoring magiques.** Partiellement appliqué : `ScoringConstants.kt` regroupe `MAX_POINTS_PER_TRACK_12P` (82), `MID_WAR_SCORE` (492), `MID_TRACK_SCORE` (41), `TOTAL_24P_SCORE` (1728), `DEBUG_PLAYER_ID`. **Suite review B5** : ajout des déclinaisons 24p manquantes — `MAX_POINTS_PER_TRACK_24P` (144), `MID_WAR_SCORE_24P` (864), `MID_TRACK_SCORE_24P` (72) — et sélection de la bonne valeur selon `is24p` dans `WarTrack.diffScore`, `WarTrackDetails.opponentScore`, `Int.warScoreToDiff(is24p)`, `Int.trackScoreToDiff(is24p)` (avec propagation aux appelants `Stats`, `MapCell`, `MKWarDetailsStatsCell`, `StatsRankingViewModel`), `AddTrackViewModel`, `TrackDetailsViewModel`. Reste : rôles `0/1/2` (→ enum `Role`) et équipe `"123456789"` (→ constante/`BuildConfig`).
- [ ] 🟢 **G5 — Homonyme `WarScore` (×2).** `model/firebase/WarScore(teamId, score)` (source de vérité 24p) et `model/local/Stats.kt → WarScore(war: WarDetails, score: Int)` (présentation, classements) partagent le même nom de classe. Risque d'`import` erroné lors des manipulations de scores. → Renommer la variante présentation (ex. `RankedWarScore`).
- [ ] 🟡 **G3 — Scraping `mkwrs.com` fragile** (`WorldRecordsRepository`, heuristiques regex) : cassera silencieusement si le HTML change. → tolérance + log/alerte.
- [ ] 🟢 **G4 — Bruit dans l'arbo.** Partiellement traité : `app/release/`, `*.aab`, `*.apk`, `*.jks`, `*.keystore` ajoutés au `.gitignore`. Reste : vérifier si `.kotlin/errors/` est ignoré ou absent du dépôt.

---

## 8. Feuille de route priorisée

### Lot 1 — Rapide & à fort impact (≈ 0,5–1 j) ✅ Appliqué
1. ✅ **B1** `safeSubList` (anti-crash, 1 ligne).
2. ✅ **B2+B3** propager `is24p` (stats 24p correctes).
3. ~~**B4**~~ analyse incorrecte — comportement original correct.
4. ✅ **G2** extraire les constantes de scoring (préalable utile aux tests).
5. ✅ **A1** sortir les secrets + chemin keystore relatif.
6. ✅ **A3** `local.properties` chargé conditionnellement (build CI sans le fichier).
7. ✅ **C3** `kotlinCompilerExtensionVersion` supprimé ; plugin `kotlin.compose.compiler` gère la version.

### Lot 2 — Filet de sécurité (≈ 1–2 j)
6. **T1** tests unitaires du moteur de scoring/stats (verrouille les corrections du Lot 1).
7. ✅ **P1** client OkHttp partagé + ✅ **D1** suppression du boilerplate callbackFlow — traités via la **migration Flow→suspend** des datasources réseau (`NetworkResponseCallAdapter`, cache `RetrofitUtils`).
8. **A2** audit des règles Firebase RTDB.

### Lot 3 — Refactoring structurel (itératif, non bloquant)
9. UI : **D16/D17/D18/D19** (TeamLogoImage, StatBlock, mkCard, design-system).
10. Stats : **D7/D8/D9/D10** (tables, branches `withFullStats`/`withTrackStats`).
11. ViewModels : **D24/D26/D28/D29** (base VM, `fetchFullTeamData`, extensions roster/DataStore).
12. Données : **D2/D3/D5** (converters génériques, allègement passe-plats, pagination factorisée).

---

*Audit non exhaustif sur le volet runtime (profiling, ANR, fuites) — à compléter par une passe avec Android Studio Profiler + LeakCanary. Voir aussi [TECHNICAL.md](TECHNICAL.md) et [FUNCTIONAL.md](FUNCTIONAL.md).*
