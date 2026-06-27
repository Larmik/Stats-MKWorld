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

- [ ] 🔴 **A1 — Secrets en clair, versionnés.** [app/build.gradle.kts:42-46](../app/build.gradle.kts) : mot de passe keystore, alias et **chemin absolu** (`/Users/pascal/…`) en dur. Couplé à `DISCORD_API_SECRET`/`DISCORD_API_CLIENT` (`local.properties`) et `google-services.json`. Le chemin absolu casse tout build hors de la machine d'origine (CI, autre dev).
  → Externaliser (env/`local.properties`), chemin relatif. **Considérer le secret Discord comme compromis et le faire tourner.**
- [ ] 🔴 **A2 — Règles de sécurité Firebase RTDB à auditer.** L'app lit/écrit `users/`, `wars/`, `currentWars/`, `newAllies/`, `tags/`, `debug/` sans couche d'autorisation applicative (Discord OAuth ≠ Firebase Auth). Si les règles RTDB sont publiques, n'importe qui peut altérer les wars de n'importe quelle équipe. **À vérifier en console Firebase** (hors-repo).
- [ ] 🟠 **A3 — Build impossible sans `local.properties`.** Lu en phase de configuration Gradle → CI/analyse échouent d'emblée. Prévoir des valeurs de repli si le fichier est absent.

---

## 2. Bugs & correctness

- [ ] 🔴 **B1 — Crash potentiel `subList(0, 2)`.** [extension/ListExtension.kt:227-228](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt) (branche 24p de `withFullStats`) appelle `subList(0, 2)` sur les `war.scores` **bruts**. Si une war 24p a < 2 scores (scores non saisis → liste vide) ⇒ `IndexOutOfBoundsException`. [Stats.kt:78,84](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/Stats.kt) utilise `safeSubList`, lui. → Utiliser `safeSubList` partout.
- [ ] 🟠 **B2 — Flag `is24p` perdu dans les classements d'adversaires.** [ListExtension.kt:285](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt) : `withFullTeamStats` appelle `.withFullStats(databaseRepository, userId)` **sans** `is24p` (défaut `false`). Utilisé par `InitStatsWorker` pour `opponentRankList`/`playerOpponentRankList` → en mode 24p, les classements d'adversaires sont calculés avec les formules 12p (victoire/défaite via `displayedDiff` basé sur `82 × nbTracks`, faux en 24p). Le `StatsScreen` direct, lui, passe bien `is24p` ([StatsViewModel.kt:107-110](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/StatsViewModel.kt)). → Propager `is24p` dans `withFullTeamStats` et son appel.
- [ ] 🟠 **B3 — Shadowing trompeur de `is24p`.** Dans `withFullStats` ([ListExtension.kt:78-136](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)), le paramètre `is24p` est masqué par un `val is24p = …teamOpponent.size > 1` local (scoring), mais le `when (is24p)` qui choisit la branche utilise le **paramètre**. Source directe de B2. → Unifier sur `teamOpponent.size`.
- [x] ~~🟠 **B4 — `withTrackStats` : division incohérente de `teamScore`.**~~ **Analyse incorrecte.** Une intermission est une course unique (l'index double identifie 2 circuits joués simultanément, pas 2 courses). `teamScoreForTrack` est déjà le score d'une seule course et ne dépend pas de la taille du tableau d'index. Le comportement original (`teamScore = teamScoreForTrack` sans division) est correct.
- [ ] 🟡 **B5 — `82` codé en dur dans des calculs réutilisés.** [WarDetails.kt:28](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/WarDetails.kt) (`82 * size − scoreHost`) et `WarTrack.diffScore` (force `positionToPoints(false)`) ne valent qu'en 12p ; fragile dès qu'un appel 24p y passe (cf. B2).
- [ ] 🟡 **B6 — `PlayerEntity` est `class`, pas `data class`.** [entities/PlayerEntity.kt:10](../app/src/main/java/fr/harmoniamk/statsmkworld/database/entities/PlayerEntity.kt) : pas d'`equals`/`hashCode`. `WarExtension.withPlayersList` fait `groupBy { it.player }` → regroupement par **référence**. Fonctionne aujourd'hui (instances partagées) mais piège latent. → `data class`.
- [ ] 🟡 **B7 — Erreurs réseau silencieuses.** Les datasources MKCentral/Discord font `trySend(null)` sur échec : un `null` est indistinct d'un « aucun résultat », sans log Crashlytics. → Propager `NetworkResponse.Error`, journaliser.
- [ ] 🟢 **B8 — Modèles « source de vérité » mutables.** `WarScore(var…)`, `Shock(var…)` exposent des `var`. → `val` + `copy`.
- [ ] 🟢 **B9 — Noms de méthodes erronés dans `WarScoreConverter`** (`fromWarPositionList`/`toWarPositionList`). **Inoffensif** (Room associe les `@TypeConverter` par type, pas par nom) mais trompeur. → Renommer.

---

## 3. Performance

- [ ] 🔴 **P1 — Nouveau `OkHttpClient` + `Retrofit` à chaque appel réseau.** [api/RetrofitUtils.kt:11-33](../app/src/main/java/fr/harmoniamk/statsmkworld/api/RetrofitUtils.kt) est invoqué dans **chaque** méthode des datasources (≈12 sites). Aucun pool de connexions/DNS/cache réutilisé, Moshi recréé à chaque fois. → Client OkHttp **singleton** partagé ; timeout par appel via `Call.timeout()` ou interceptor ; Retrofit mis en cache par base URL.
- [ ] 🟠 **P2 — Pagination MKCentral séquentielle bloquante.** [FetchUseCase.fetchTeams()](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt) enchaîne les pages via `firstOrNull()` dans des `while` (mkworld puis mk8dx, page par page). Lent sur grosse base. → Paralléliser (`async`/`flatMapMerge`).
- [ ] 🟠 **P3 — `InitStatsWorker` : N appels `getTeam(...).firstOrNull()`.** La résolution `TeamStats` interroge Room équipe par équipe, par joueur **et** par équipe. → Charger une seule `Map<id, TeamEntity>`.
- [ ] 🟡 **P4 — `shareIn(this, Eagerly)` inutile.** [FetchUseCase.kt:225-231](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt) crée des flux partagés *Eagerly* pour un usage one-shot (consommé par `firstOrNull`). → `firstOrNull()` direct.
- [ ] 🟡 **P5 — Gating de version dépendant du réseau au démarrage.** `MainViewModel` attend `remoteConfigRepository.minimumVersion()` (`fetch(0)`, cache 0 s) avant de router → démarrage retardé/risqué hors-ligne. → Cache court + repli offline.
- [ ] 🟡 **P6 — Recomputations dans `Stats`.** `maps.filter { it.totalPlayed >= 2 }` recalculé **5×** ([Stats.kt:25-33](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/Stats.kt)) ; `takeIf { isNotEmpty() }?.size ?: 1` répété 3× ([Stats.kt:34-41](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/Stats.kt)). → Base commune `val mapsAboveThreshold = …` + helper `sizeOrOne()`.

---

## 4. Duplication & refactoring

### 4.1 Couche données

- [ ] 🟠 **D1 — Pattern callbackFlow + Retrofit dupliqué ~9×.** Dans `MKCentralDataSource` (6 méthodes) et `DiscordDataSource` (3) : `callbackFlow { … enqueue(Callback { onResponse/onFailure → trySend }) ; awaitClose {} }` identique. → Helper générique `Call<T>.asFlow()` (réduit chaque méthode à 1 ligne).
- [ ] 🟡 **D2 — Convertisseurs Room quasi identiques (×5).** [database/converters/](../app/src/main/java/fr/harmoniamk/statsmkworld/database/converters/) : `WarPosition`/`WarScore`/`WarPenalty`/`String`Converter partagent à l'identique le bloc `adapter.toJson` / `try { fromJson } catch { arrayListOf() }`. → Un seul `MoshiListConverter<T>` générique (garder la spécificité `NumberToIntAdapterFactory` de `WarTrackConverter`).
- [ ] 🟡 **D3 — Wrappers `flow { emit(dao.x()) }` + passe-plats.** Les 3 data sources locales wrappent les DAO ; `DatabaseRepository` ([DatabaseRepository.kt:64-84](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/DatabaseRepository.kt)) est ~18 méthodes de **pure délégation** (ajoutant un `flowOn(IO)` parfois oublié). → Extension `(() -> T).asFlow()` ; envisager de fusionner data source locale + repository (la couche n'apporte que le dispatcher).
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
- [ ] 🟡 **C3 — `kotlinCompilerExtensionVersion = "1.5.12"`** ([build.gradle.kts:81](../app/build.gradle.kts)) figé alors que le plugin `kotlin.compose.compiler` pilote la version → ligne morte/trompeuse.
- [ ] 🟢 **C4 — `viewBinding` + `dataBinding`** activés pour 3 layouts PDF uniquement (coût de build). Évaluer un rendu Compose→bitmap.
- [ ] 🟢 **C5 — ProGuard `-dontoptimize`** : désactive l'optimisation R8 (taille/perf). À réévaluer.
- [ ] 🟢 **C6 — Opt-ins expérimentaux** (`@FlowPreview`, `@ExperimentalCoroutinesApi`) très répandus ; surveiller lors des montées de version.

---

## 6. Tests & outillage

- [ ] 🟠 **T1 — Aucune couverture de tests** (seulement `ExampleUnitTest`/`ExampleInstrumentedTest`). Le moteur de scoring (`positionToPoints`, `withFullStats`, `WarDetails`, `WarStats`) est **pur et trivial à tester en JVM** : meilleur ROI immédiat, aurait attrapé B1 et B2/B4. → Suite JUnit sur `extension/` + `model/local/`.
- [ ] 🟡 **T2 — Pas de CI.** → Pipeline build + tests + `lint`.
- [ ] 🟡 **T3 — `versionCode` manuel** (risque d'incohérence avec `minimumVersion` Remote Config). → Bump automatisé.

---

## 7. Dette technique & constantes magiques

- [ ] 🟡 **G1 — Id joueur de référence `"18595"` codé en dur** (gating debug + sortie de mode matrix) : [PlayerCell.kt:177,193](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/PlayerCell.kt), [PlayerProfileScreen.kt:384](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/playerProfile/PlayerProfileScreen.kt), [DebugViewModel.kt:98](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/debug/DebugViewModel.kt). Si ce compte disparaît, le mode matrix casse. → Constante/Remote Config/flag de build.
- [ ] 🟡 **G2 — Constantes de scoring magiques** (`82`, `492`, `41`, `144`, `1728`, rôles `0/1/2`, équipe `"123456789"`). → constantes nommées + enum `Role`.
- [ ] 🟡 **G3 — Scraping `mkwrs.com` fragile** (`WorldRecordsRepository`, heuristiques regex) : cassera silencieusement si le HTML change. → tolérance + log/alerte.
- [ ] 🟢 **G4 — Bruit dans l'arbo** : `.kotlin/errors/`, `app/release/` traînent. Vérifier `.gitignore`.

---

## 8. Feuille de route priorisée

### Lot 1 — Rapide & à fort impact (≈ 0,5–1 j) ✅ Appliqué
1. ✅ **B1** `safeSubList` (anti-crash, 1 ligne).
2. ✅ **B2+B3** propager `is24p` (stats 24p correctes).
3. ~~**B4**~~ analyse incorrecte — comportement original correct.
4. ✅ **G2** extraire les constantes de scoring (préalable utile aux tests).
5. ✅ **A1** sortir les secrets + chemin keystore relatif.

### Lot 2 — Filet de sécurité (≈ 1–2 j)
6. **T1** tests unitaires du moteur de scoring/stats (verrouille les corrections du Lot 1).
7. **P1** client OkHttp partagé + **D1** helper `Call<T>.asFlow()`.
8. **A2** audit des règles Firebase RTDB.

### Lot 3 — Refactoring structurel (itératif, non bloquant)
9. UI : **D16/D17/D18/D19** (TeamLogoImage, StatBlock, mkCard, design-system).
10. Stats : **D7/D8/D9/D10** (tables, branches `withFullStats`/`withTrackStats`).
11. ViewModels : **D24/D26/D28/D29** (base VM, `fetchFullTeamData`, extensions roster/DataStore).
12. Données : **D2/D3/D5** (converters génériques, allègement passe-plats, pagination factorisée).

---

*Audit non exhaustif sur le volet runtime (profiling, ANR, fuites) — à compléter par une passe avec Android Studio Profiler + LeakCanary. Voir aussi [TECHNICAL.md](TECHNICAL.md) et [FUNCTIONAL.md](FUNCTIONAL.md).*
