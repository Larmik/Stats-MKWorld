# Audit technique — Stats MKWorld

> Revue du projet : sécurité, bugs/correctness, performance, duplication, best practices, tests/outillage, dette technique.
> **Audit post-epic refonte UX** (#53, `versionName` 4.0.0 / `versionCode` 24, travail en cours sur la 4.0.2), par analyse statique de `app/src/main/java/fr/harmoniamk/statsmkworld/`.

**Légende de sévérité** : 🔴 Bloquant/critique · 🟠 Important · 🟡 Moyen · 🟢 Cosmétique/confort.
**Statut** : `[ ]` ouvert · `[~]` partiellement traité (le reste est décrit).

Conventions de maintenance (rule `50-process-doc.md`) :

- les numéros de ligne sont indicatifs (état au moment de l'audit) — à reconfirmer avant correction ;
- une entrée **traitée est supprimée** (pas de liste ✅ : l'historique vit dans git et les PR) ;
- chaque entrée porte une ligne **Prévention** : la rule / l'étape de process qui empêche sa réapparition (matrice complète au § 9).

## Sommaire
1. [Bloquant & sécurité](#1-bloquant--sécurité)
2. [Bugs & correctness](#2-bugs--correctness)
3. [Performance](#3-performance)
4. [Duplication & refactoring](#4-duplication--refactoring)
5. [Best practices Android/Kotlin](#5-best-practices-androidkotlin)
6. [Tests & outillage](#6-tests--outillage)
7. [Dette technique & constantes magiques](#7-dette-technique--constantes-magiques)
8. [Feuille de route priorisée](#8-feuille-de-route-priorisée)
9. [Matrice audit ↔ prévention](#9-matrice-audit--prévention)

---

## 1. Bloquant & sécurité

- [~] 🟠 **A2 — Règles de sécurité Firebase RTDB.** Volet applicatif fait (Firebase Anonymous Auth : `FirebaseRepository.signInAnonymously()`, re-tentée par `MainViewModel`). **Reste hors-repo (console Firebase)** : activer *Anonymous* et durcir les règles RTDB (`auth != null`, idéalement scopé par équipe). Tant que les règles restent ouvertes, l'auth anonyme ne protège rien. *Prévention : rule 40 (secrets & données côté client).*
- [ ] 🟠 **A4 — Secret OAuth Discord embarqué dans l'APK.** `BuildConfig.DISCORD_API_SECRET` sert au `Credentials.basic(...)` de l'échange de code et de la révocation ([DiscordDataSource.kt:36,52](../app/src/main/java/fr/harmoniamk/statsmkworld/datasource/network/DiscordDataSource.kt)). Sorti du dépôt (`local.properties`), mais un `BuildConfig` se lit par décompilation : le secret est extractable de tout APK publié. → Déplacer l'échange `code → token` côté serveur (Cloud Function), puis faire tourner le secret. *Prévention : rule 40.*
- [ ] 🟡 **A5 — Sauvegarde Android non filtrée.** `android:allowBackup="true"` avec des règles d'exemple vides (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`) : le token Discord (`access_token`, [DataStoreRepository.kt:77](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/DataStoreRepository.kt)), les DataStore proto et la base Room partent en sauvegarde cloud et se restaurent sur un autre appareil. → Exclure les DataStore/token (ou `allowBackup=false`, les données étant re-synchronisables). *Prévention : rule 40.*

---

## 2. Bugs & correctness

- [ ] 🟠 **B27 — Synchro des wars : seule la dernière roster hôte est conservée.** `FetchUseCase.fetchWars(teamId)` fait `clearWars()` (vide **toute** la table, [WarDao.kt:19](../app/src/main/java/fr/harmoniamk/statsmkworld/database/dao/WarDao.kt)) puis écrit les wars d'**une** roster ([FetchUseCase.kt:164-167](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt)). Or les 4 appelants l'invoquent **en boucle par roster mkworld** ([FetchUseCase.kt:72](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt), [PlayerProfileViewModel.kt:238](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/playerProfile/PlayerProfileViewModel.kt), [SignupViewModel.kt:135](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/signup/SignupViewModel.kt), [DebugViewModel.kt:113](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/debug/DebugViewModel.kt)) : une équipe hôte à ≥ 2 rosters ne garde que les wars de la dernière. → Sortir le `clearWars()` de `fetchWars` (un seul clear avant la boucle, ou clear + écriture de toutes les rosters en une passe). *Prévention : rule 30 (écriture destructive hors boucle).*
- [ ] 🟠 **B28 — Permission de notification jamais demandée (Android 13+).** `MainApplication` implémente `ActivityLifecycleCallbacks` mais **n'appelle jamais** `registerActivityLifecycleCallbacks(this)` ([MainApplication.kt:14-60](../app/src/main/java/fr/harmoniamk/statsmkworld/application/MainApplication.kt)) → `currentActivity` toujours `null` → `NotificationRepository.permissionCheck()` renvoie `Denied` et le `notificationPermissionLauncher` n'est jamais lancé ([NotificationRepository.kt:53,60](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/NotificationRepository.kt)). Cause de fond : un repository dépend d'une `Activity`. → Lancer la demande depuis l'UI (`rememberLauncherForActivityResult`) et laisser au repository le seul état de permission. *Prévention : rule 30 (pas de dépendance Activity/UI dans un repository).*
- [ ] 🟠 **B29 — Écran « mise à jour requise » masqué par le splash (à confirmer sur device).** `setKeepOnScreenCondition { true }` n'est relâché que dans la branche `startDestination` ([MainActivity.kt:44,72-80](../app/src/main/java/fr/harmoniamk/statsmkworld/activity/MainActivity.kt)) ; la branche `needUpdate` pose le `MKDialog` sans libérer le splash → l'app reste figée sur le splash. → Relâcher le splash dans les deux branches. *Prévention : checklist anti-audit (toutes les branches d'un état terminal).*
- [ ] 🟡 **B30 — Recherche de joueurs : résultats concurrents.** Chaque frappe (≥ 3 caractères) lance un nouveau `viewModelScope.launch` qui parcourt **toutes les pages** MKCentral, sans annuler la recherche précédente ni debounce ([RegistryViewModel.kt:46-61](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/registry/RegistryViewModel.kt), [TeamProfileViewModel.kt:88-105](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/teamProfile/TeamProfileViewModel.kt)) : une réponse lente d'un terme ancien peut écraser le résultat du terme courant, et la rafale d'appels expose au throttle MKCentral. → `MutableStateFlow` du terme + `debounce` + `flatMapLatest` (ou `Job` annulé). *Prévention : rule 20 (recherche à la saisie).*
- [ ] 🟡 **B31 — `withTrackStats` : mode 12/24 partagé entre wars.** `var is24p` réassigné dans le `flatMap` puis lu dans le `mapNotNull` suivant ([ListExtension.kt:207-215](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)) : c'est le mode de la **dernière** war qui s'applique à toutes les manches. Latent (les appelants filtrent un seul mode aujourd'hui), faux dès qu'une liste mixte arrive. → Porter le mode par manche (comme `warIs24p` dans `withFullStats`). *Prévention : rule 60 (pas de `var` capturée mutée dans un opérateur).*
- [ ] 🟡 **B32 — Génération du Tab : crash et adversaire perdu.** `scores[index].toInt()` lève une `NumberFormatException` si un champ est non numérique alors que la somme (`toIntOrNull`) concorde ; `teamOpponents.mapNotNull { getTeam(...) }` efface un adversaire non résolu → `teamWin`/`teamLose` `null` dans le PDF ([EditTabViewModel.kt:73-91](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTab/EditTabViewModel.kt)). → `toIntOrNull() ?: 0` + repli dégradé (rule 12). *Prévention : rules 60 (saisie utilisateur) et 12.*
- [ ] 🟢 **B33 — Id `"null"` passé à la synchro des alliés.** `fetchAllies(team?.id.toString())` envoie la chaîne `"null"` si l'équipe n'a pas été récupérée ([FetchUseCase.kt:69](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt), [PlayerProfileViewModel.kt:232](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/playerProfile/PlayerProfileViewModel.kt)). → `team?.let { … }`. *Prévention : rule 60.*
- [~] 🟢 **B11 — Adversaires multi-rosters historiques restés en `teamId`.** Création de war en `rosterId`, affichage/classements par roster et migration RTDB des équipes mono-roster faits. **Limite assumée** : les wars historiques contre une équipe multi-rosters restent en `teamId` (roster joué inconnu) → un item de classement « niveau équipe » subsiste à côté des rosters. Pas d'action prévue sans source de vérité. *Prévention : rule 12.*

---

## 3. Performance

- [ ] 🟠 **P8 — Calcul de stats encore sur le thread UI dans 3 ViewModels (rule 21 partiellement appliquée).** (a) [OpponentDetailViewModel.kt:127-149](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/opponent/OpponentDetailViewModel.kt) : `map { WarDetails(War(it)) }` dans le `combine` **et** `withFullStats` dans le `flatMapLatest`, tous deux hors `withContext` ; (b) [MapDetailViewModel.kt:114-121](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/map/MapDetailViewModel.kt) : construction des `WarDetails` hors `withContext` ; (c) [PeriodViewModel.kt:63-134](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/warList/period/PeriodViewModel.kt) : tout le calcul sur le collecteur, et `withPlayersList` relit `getPlayers()` Room **à chaque war**. Le § « Calcul de stats hors du thread UI » de `TECHNICAL.md` présente (a)/(b) comme corrigés à tort. → Appliquer le découpage en deux temps de la rule 21 ; hisser la lecture des joueurs hors de la boucle. *Prévention : rule 21.*
- [ ] 🟡 **P2 — Pagination MKCentral séquentielle.** [FetchUseCase.fetchTeams()](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt) (L131-142) enchaîne les pages une par une. Le parallélisme complet est exclu (throttle MKCentral, rule 30) : au mieux des **lots bornés de 3-4 pages**, à valider sur l'API réelle. En arrière-plan (`UpdateDataWorker`), la latence actuelle reste acceptable. *Prévention : rule 30.*
- [ ] 🟡 **P5 — Gating de version dépendant du réseau au démarrage.** `MainViewModel` attend `minimumVersion()` (`fetch(0)`, intervalle minimal 0 s, sans timeout — [RemoteConfigRepository.kt:32-47](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/RemoteConfigRepository.kt)) avant de router → démarrage retardé hors-ligne ou sur réseau lent. → Intervalle de cache raisonnable + `activate()` des valeurs en cache + timeout. *Prévention : checklist anti-audit (I/O bloquant le démarrage).*
- [ ] 🟢 **P9 — `withFullTeamStats` reconstruit les `WarDetails` par adversaire.** `map { WarDetails(War(it)) }` refait pour chaque roster adverse ([ListExtension.kt:168-180](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)) → O(adversaires × wars) de reparse. → Construire les `WarDetails` une fois et filtrer. *Prévention : rule 21.*

---

## 4. Duplication & refactoring

### 4.1 Couche données

- [ ] 🟡 **D2 — Convertisseurs Room quasi identiques (×6).** [database/converters/](../app/src/main/java/fr/harmoniamk/statsmkworld/database/converters/) : `WarPosition`/`WarScore`/`WarPenalty`/`String`/`RosterInfo`Converter répètent `adapter.toJson` / `try { fromJson } catch { arrayListOf() }`. → `MoshiListConverter<T>` générique (garder `NumberToIntAdapterFactory` de `WarTrackConverter`). *Prévention : rule 16 (généraliser par paramètre, pas par copie).*

### 4.2 Moteur de statistiques (`extension/` + `model/local/`)

- [ ] 🟡 **D9 — `withTrackStats` : branches index simple / double redondantes** ([ListExtension.kt:240-262](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)) : seuls `map`/`trackIndex`/`teamScore` diffèrent. → Un seul `TrackStats(...)` paramétré par la liste d'index. *Prévention : rule 16.*
- [ ] 🟡 **D10 — `warScoreToDiff` ≈ `trackScoreToDiff`** ([IntegerExtension.kt:162-202](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/IntegerExtension.kt)) : identiques hormis le point milieu. → `private fun Int.scoreToDiff(midpoint: Int)`. *Prévention : rule 16.*
- [ ] 🟡 **D11 — `Datastore*` (×6) : boilerplate de conversion** (ctor firebase + ctor proto + getter `proto`). → Fonctions de mapping ; à terme génération. *Prévention : —.*
- [ ] 🟡 **D12 — Constructeurs de conversion triviaux des modèles firebase.** `War(entity)`, `WarTrack(track)`… → extensions `toX()` partagées. *Prévention : —.*
- [ ] 🟢 **D13 — Tables `when` de `positionToPoints`/`pointsToPosition`/`positionColor`** ([IntegerExtension.kt:64-239](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/IntegerExtension.kt)). → Lookup `mapOf(...)`. *Prévention : —.*
- [ ] 🟢 **D14 — `List<Int?>?.sum()` maison** ([ListExtension.kt:72-75](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)), redondant avec `sumOf { it ?: 0 }`. → Supprimer. *Prévention : —.*
- [ ] 🟢 **D15 — Boucles manuelles dans `withPlayersList`** ([WarExtension.kt:41-90](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/WarExtension.kt)) remplaçables par `flatMap`/`groupBy`. *Prévention : —.*

### 4.3 Couche UI (`ui/`, `ui/cells/`, `ui/stats/`)

- [ ] 🟠 **D16 — Logo/écusson d'équipe et préfixe d'URL MKCentral dupliqués.** `"https://mkcentral.com$…"` est concaténé à **28** endroits (VM et écrans) et l'écusson existe en **5 variantes** privées ou publiques : `Crest` ([WelcomeScreen.kt:253](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/WelcomeScreen.kt)), `OpponentCrest`/`TeamLogo` ([CurrentWarCell.kt:130,148](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/CurrentWarCell.kt)), `WarTeamCrest` ([WarSummaryCells.kt:217](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/WarSummaryCells.kt)), `TeamCrestSmall` ([CurrentWarScreen.kt:323](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/CurrentWarScreen.kt)). → `fun String?.mkcentralUrl()` (`StringExtension.kt`) + un composable `TeamCrest` partagé dans `ui/cells/`. *Prévention : rule 16 (chercher l'existant avant de créer).*
- [ ] 🟡 **D35 — Composants UI recopiés malgré la rule 16.** `OutcomeChip` identique dans [WarCell.kt:101](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/WarCell.kt) et [WelcomeScreen.kt:359](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/WelcomeScreen.kt) (+ `OutcomeChipSmall`, [OpponentDetailScreen.kt:202](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/opponent/OpponentDetailScreen.kt)) ; `Eyebrow` privé de [WelcomeScreen.kt:194](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/WelcomeScreen.kt) identique au public [MKStatCard.kt:72](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/stats/MKStatCard.kt) ; `initialsOf` privé ×2 ([WelcomeScreen.kt:483](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/WelcomeScreen.kt), [AddWarScreen.kt:150](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addWar/AddWarScreen.kt)) alors que [MKPodiumCell.kt:147](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/stats/MKPodiumCell.kt) l'expose ; `ProgressCard` ×2 ([AddWarScreen.kt:347](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addWar/AddWarScreen.kt), [AddTrackScreen.kt:435](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addTrack/AddTrackScreen.kt)). → Un exemplaire partagé chacun. *Prévention : rule 16.*
- [ ] 🟡 **D18 — Style de carte répété.** 81 `RoundedCornerShape(N.dp)` littéraux, souvent avec `.background(…).border(…)`. → `Modifier.mkCard()` / `StatCard`. *Prévention : rule 16.*
- [ ] 🟡 **D19 — Pas de design-system.** [ui/Resources.kt](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/Resources.kt) centralise couleurs/fonts mais pas les espacements (686 littéraux `N.dp`), formes ni presets typographiques (`textColor = Colors.white` ×98). Absorbe l'ancien D17 (blocs « label + valeur »). → Objets `Spacing`/`Shapes`/`TextStyles`. *Prévention : rules 13/16.*
- [ ] 🟡 **D21 — `CurrentWarCellViewModel` ≈ `WarCellViewModel`** ([ui/cells/](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/)) : même résolution des adversaires + nom/id de roster. → Logique commune. *Prévention : rule 16.*
- [~] 🟠 **D34 — Code mort UI conservé.** `ui/WarScoreView.kt` (453 lignes, 0 appelant) volontairement gardé pour le rendu 24p (« 12p first, 24p deferred ») → à trancher au ticket de réactivation 24p. **Nouveau** : `ui/cells/MapCell.kt` (267 lignes) n'est plus appelé que par ses `@Preview` (remplacé par `PodiumCell`). → Supprimer `MapCell` (hors 24p). *Prévention : checklist anti-audit (supprimer ce qui devient orphelin).*

### 4.4 Écrans & ViewModels (`screen/`)

- [ ] 🟠 **D24 — Boilerplate ViewModel répété (15 VM).** `data class State` + `MutableStateFlow` + `.mergeWith(_state).stateIn(scope, WhileSubscribed(5000), …)`. → Extension `Flow<T>.mergedStateIn(scope, mutableState)`. *Prévention : —.*
- [ ] 🟠 **D25 — Pagination de recherche joueurs dupliquée** (`RegistryViewModel` / `TeamProfileViewModel`, même algorithme — cf. B30). → Une seule implémentation (repository ou data source). *Prévention : rules 16/32.*
- [ ] 🟠 **D26 — Chaîne de synchro complète dupliquée 4×** (`FetchUseCase.fetchData`, `PlayerProfileViewModel.onRefresh`, `SignupViewModel`, `DebugViewModel.onMatrix`) — le bug B27 est répliqué dans chacune. → Un seul point d'entrée (`fetchData` paramétré pour la progression). *Prévention : rule 32.*
- [ ] 🟡 **D27 — Écriture allié/membre Firebase (`when (rosterId) { "-1" -> … }`) dupliquée 5×** ([AddWarViewModel.kt:357](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addWar/AddWarViewModel.kt), [CurrentWarActionsViewModel.kt:146,171,216](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/CurrentWarActionsViewModel.kt), [CurrentWarViewModel.kt:159](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/CurrentWarViewModel.kt)). → Méthode `FirebaseRepository.updatePlayerCurrentWar(player, war)`. *Prévention : rule 16 (logique) / 61.*
- [ ] 🟡 **D28 — Filtre roster « mkworld » répété (37 occurrences de `game == "mkworld"`).** → `MKCPlayer.mkWorldRoster()` / `MKCTeam.mkWorldRosters()`. *Prévention : rule 61.*
- [ ] 🟡 **D29 — Lectures DataStore répétées** (`mkcTeam`/`mkcPlayer`/`is24PEnabled.firstOrNull()` ×39). → Helpers suspendus `currentTeam()`/`currentPlayer()`. *Prévention : —.*
- [ ] 🟡 **D30 — Test de mode war `teamOpponent.size > 1` / `== 1` répété (~15 sites).** Ex. [WarDetails.kt:22](../app/src/main/java/fr/harmoniamk/statsmkworld/model/local/WarDetails.kt), [WarExtension.kt:69](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/WarExtension.kt), [StatsRankingViewModel.kt:218](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/ranking/StatsRankingViewModel.kt), [EditTrackViewModel.kt:180](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTrack/EditTrackViewModel.kt). → `val War.is24p` / `WarEntity.is24p` dans `WarExtension.kt`. *Prévention : rule 61.*
- [ ] 🟡 **D36 — Extensions `toPodiumEntry` posées dans des fichiers d'écran.** 14 extensions top-level dans `screen/**` ; `TrackStats.toPodiumEntry` est définie **deux fois** ([PlayerMapsRankingScreen.kt:104](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/full/PlayerMapsRankingScreen.kt), [OpponentDetailScreen.kt:330](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/opponent/OpponentDetailScreen.kt)). → Fonctions privées à paramètre explicite si mono-usage, sinon un seul exemplaire dans `extension/`. *Prévention : rule 61 (corollaire extensions).*
- [ ] 🟢 **D33 — Plomberie de callbacks de navigation** (14 `savedStateHandle` get/set dans `RootScreen.kt`). → Événements de navigation typés. *Prévention : —.*

---

## 5. Best practices Android/Kotlin

- [ ] 🟡 **C1 — `fallbackToDestructiveMigration()`** : perte des données locales à chaque montée de schéma (acceptable car re-sync, documenté dans `CLAUDE.md`). *Prévention : `CLAUDE.md` (pièges).*
- [ ] 🟡 **C2 — Désérialisation Firebase à la main** (cast `Map<*, *>` + parse champ par champ, [FirebaseRepository.kt:114-152,249-275](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/FirebaseRepository.kt)). → `getValue(Class)` / data classes typées. *Prévention : —.*
- [ ] 🟡 **C9 — One-shots emballés dans un `Flow` (contraire à la rule 30).** `PDFRepository.write` ([PDFRepository.kt:45,72](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/PDFRepository.kt)), `FetchUseCase.manageTransferts`/`migrateOpponentsToRoster` ([FetchUseCase.kt:41-42](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt)), `withFullStats` qui renvoie `flowOf(Stats)` consommé par `firstOrNull()` et reçoit un `databaseRepository` **inutilisé** ([ListExtension.kt:81,135](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)). → `suspend fun` à résultat direct ; retirer le paramètre mort. *Prévention : rule 30.*
- [ ] 🟡 **C10 — `Context` statique et libellés en dur dans les ViewModels.** `MainApplication.instance?.applicationContext?.getString(...)` ([AddTrackViewModel.kt:137](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/addTrack/AddTrackViewModel.kt), [EditTrackViewModel.kt:104](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTrack/EditTrackViewModel.kt)) ; toasts en dur ([EditTabViewModel.kt:97-102](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTab/EditTabViewModel.kt), `DebugViewModel`) ; 14 libellés en dur dans `DebugScreen`. → `@ApplicationContext` injecté ou ids `R.string` résolus en UI. *Prévention : rule 20.*
- [ ] 🟢 **C11 — Fonction locale imbriquée** `addRankingItem` dans `withFullTeamStats` ([ListExtension.kt:176](../app/src/main/java/fr/harmoniamk/statsmkworld/extension/ListExtension.kt)). → Fonction top-level privée. *Prévention : rule 62.*
- [ ] 🟢 **C12 — `CoroutineScope` implémenté sans usage** par `FetchUseCase` ([FetchUseCase.kt:63,239](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt)), `RegistryViewModel` ([RegistryViewModel.kt:24](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/registry/RegistryViewModel.kt)) et `UpdateDataWorker` (aucun `launch` sur ce scope, `Job` absent). → Retirer. *Prévention : —.*
- [ ] 🟢 **C13 — Worker périodique : pas de retry et re-planification à chaque lancement.** `MKCoroutineWorker.doWork()` renvoie toujours `success` sans capturer d'exception ([MKCoroutineWorker.kt:52-55](../app/src/main/java/fr/harmoniamk/statsmkworld/worker/MKCoroutineWorker.kt)) ; `RootScreen` ré-enfile en `CANCEL_AND_REENQUEUE` à chaque démarrage ([RootScreen.kt:67-69](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/RootScreen.kt)), ce qui annule une synchro en cours. → `Result.retry()` sur échec réseau, politique `UPDATE`. *Prévention : —.*
- [ ] 🟢 **C14 — Code commenté laissé en place.** `generateDetailedPdf` commenté avec `StrictMode.permitAll()` ([EditTabViewModel.kt:111-154](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTab/EditTabViewModel.kt)), blocs commentés de [PDFRepository.kt:47,231](../app/src/main/java/fr/harmoniamk/statsmkworld/repository/PDFRepository.kt). (Le segmenté 12/24 commenté d'`AddWarScreen` est une désactivation assumée #91.) → Supprimer, git garde l'historique. *Prévention : rule 64.*
- [ ] 🟢 **C4 — `viewBinding` + `dataBinding`** activés pour 3 layouts PDF ([build.gradle.kts:73-78](../app/build.gradle.kts)). → Évaluer un rendu Compose→bitmap. *Prévention : —.*
- [ ] 🟢 **C5 — ProGuard `-dontoptimize`** ([proguard-rules.pro:10](../app/proguard-rules.pro)) désactive l'optimisation R8. → À réévaluer avec un build release testé. *Prévention : rule 40.*
- [ ] 🟢 **C6 — Opt-ins expérimentaux** (`@FlowPreview`, `@ExperimentalCoroutinesApi`, ~74 occurrences, souvent sans API expérimentale réellement utilisée). → Retirer les superflus, surveiller aux montées de version. *Prévention : —.*
- [~] 🟢 **C8 — Collecte d'état Compose.** `rememberSaveable` désormais généralisé (35) et `derivedStateOf` inutiles retirés. **Reste** : 9 `collectAsState()` non liés au cycle de vie (vs 51 `collectAsStateWithLifecycle`). → Aligner. *Prévention : rule 11.*

---

## 6. Tests & outillage

- [~] 🟠 **T1 — Couverture de tests.** Suite **E2E Maestro** présente (`.maestro/`, cf. [TESTS_FUNCTIONAL.md](TESTS_FUNCTIONAL.md)). **Correction** : le test JVM `StatsEngineTest` annoncé par l'audit précédent n'existe pas dans le dépôt — seul `ExampleUnitTest` est présent (`app/src/test`). **Reste** : tests unitaires du moteur (`withFullStats`, `withTrackStats`, `WarStats`, scoring 12p/24p), selon les modalités que l'utilisateur fixera (`CLAUDE.md` : pas de tests spontanés). *Prévention : hors config (décision utilisateur).*
- [ ] 🟡 **T2 — Pas de CI.** → Pipeline build + `lint` (+ tests JVM quand ils existeront). *Prévention : —.*
- [ ] 🟡 **T3 — `versionCode` manuel** ([build.gradle.kts:28](../app/build.gradle.kts)), risque d'incohérence avec `minimumVersion` Remote Config. → Bump automatisé. *Prévention : `CLAUDE.md` (pièges).*

---

## 7. Dette technique & constantes magiques

- [ ] 🟡 **G2 — Rôles et équipe synthétique en littéraux.** Rôles `0/1/2` et le mapping « leader → 2 » répétés ([PlayerEntity.kt:44](../app/src/main/java/fr/harmoniamk/statsmkworld/database/entities/PlayerEntity.kt), [SignupViewModel.kt:124](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/signup/SignupViewModel.kt), [DebugViewModel.kt:99](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/debug/DebugViewModel.kt), [TeamProfileViewModel.kt:128,150](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/teamProfile/TeamProfileViewModel.kt), [PlayerProfileViewModel.kt:93,112-119](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/playerProfile/PlayerProfileViewModel.kt)) alors qu'un `ProfileRole` existe côté UI ([ProfileCells.kt:40](../app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/ProfileCells.kt)) ; id `"123456789"` de la « 6v6 Squad » ([FetchUseCase.kt:155](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt)). Absorbe l'ancien D31. → Enum `Role` (valeur Firebase + libellé) + constante. *Prévention : rule 61.*
- [ ] 🟡 **G6 — Sentinelle allié `rosterId = "-1"` en littéral (~15 sites).** [PlayerEntity.kt:32](../app/src/main/java/fr/harmoniamk/statsmkworld/database/entities/PlayerEntity.kt), [FetchUseCase.kt:184](../app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt), [OpponentDetailViewModel.kt:266,305](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/opponent/OpponentDetailViewModel.kt), [MapDetailViewModel.kt:193,233](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/map/MapDetailViewModel.kt), [TeamProfileViewModel.kt:117,180](../app/src/main/java/fr/harmoniamk/statsmkworld/screen/teamProfile/TeamProfileViewModel.kt), D27. Écriture et lecture doivent rester cohérentes. → Constante partagée + `val PlayerEntity.isAlly`. *Prévention : rule 61.*
- [ ] 🟢 **G7 — Marge bottombar `90.dp` en littéral (13 sites)**, parfois appliquée à des écrans du graphe racine sans bottombar (`Opponent*RankingScreen`, `Map*RankingScreen`). → Constante partagée (`ui/`), appliquée aux seuls contenus de pôle. *Prévention : rules 17/61.*
- [ ] 🟡 **G1 — Id joueur debug `"18595"` codé en dur** (`ScoringConstants.DEBUG_PLAYER_ID`). → Remote Config ou `BuildConfig`. *Prévention : —.*
- [ ] 🟡 **G3 — Scraping `mkwrs.com` fragile** (`WorldRecordsRepository`, regex ; seul consommateur : écran Debug). Champs du modèle `WorldRecord` peuplés mais jamais lus. → Tolérance + log, ou retrait si la feature est abandonnée. *Prévention : —.*
- [ ] 🟢 **G4 — `.kotlin/` non ignoré.** Dossier présent à la racine, non suivi mais absent du `.gitignore` (risque de commit accidentel via `git add -A`). → Ajouter `.kotlin/`. *Prévention : —.*
- [ ] 🟢 **G5 — Homonyme `WarScore`.** `model/firebase/WarScore` et `model/local/Stats.kt:343 → WarScore(war, score)` partagent le nom (risque d'import erroné). → Renommer la variante présentation (`RankedWarScore`). *Prévention : rule 63.*
- [ ] 🟢 **G8 — Documentation : références à des rules supprimées.** `TECHNICAL.md` (l. 102, 104, 115, 186, 188, 190) et `PROTOTYPE_UX.md` citent `15-ui-prototype-reference.md` / « rules 13/15 » (pixel-perfect), rules retirées à la fin de l'epic. → Remplacer par les rules 13/16 actuelles. *Prévention : rule 50 (références croisées).*

---

## 8. Feuille de route priorisée

### Lot 1 — Correctness & sécurité (≈ 1-2 j)
1. **B27** + **D26** : un seul `clearWars()` hors boucle, chaîne de synchro unifiée.
2. **B28** : demande de permission depuis l'UI (retirer la dépendance `Activity` du repository).
3. **B29** : relâcher le splash sur la branche « mise à jour requise ».
4. **B30** + **D25** : recherche joueurs annulable/debouncée, une seule implémentation.
5. **B31**, **B32**, **B33** : correctifs ponctuels.
6. **A4** (backend d'échange OAuth + rotation du secret), **A5** (règles de backup), **A2** (règles RTDB, console).

### Lot 2 — Fluidité (≈ 1 j)
7. **P8** (rule 21 sur `OpponentDetail`/`MapDetail`/`Period`) puis **P9**, **P5**.

### Lot 3 — Hygiène à faible coût (≈ 1 j)
8. **D34** (suppression `MapCell`), **C14**, **C11**, **C12**, **D14**, **G4**, **G8**.
9. Constantes : **G6**, **G2**, **G7**, **D30**, **D28**.

### Lot 4 — Refactoring structurel (itératif)
10. UI : **D16**, **D35**, **D18**, **D19**, **D21**, **D36**.
11. Données / VM : **C9**, **C10**, **D24**, **D27**, **D29**, **D2**, **D9**, **D10**.
12. Reste : **C2**, **C13**, **C4**, **C5**, **C6**, **C8**, **D11-D13**, **D15**, **D33**, **G1**, **G3**, **G5**.

### Lot 5 — Filet de sécurité (selon décision utilisateur)
13. **T1** (tests JVM du moteur), **T2** (CI), **T3** (bump automatisé).

---

## 9. Matrice audit ↔ prévention

Chaque catégorie d'entrée est rattachée à la cause qui la produit et à ce qui l'empêche de revenir. La relecture finale anti-audit (`.claude/agents/ticket-worker.md` § 4, `.claude/skills/ticket-dev/SKILL.md` § 4) parcourt cette table sur le diff avant livraison.

| Catégorie (entrées) | Cause générative | Prévention |
|---|---|---|
| Secrets / données sensibles côté client (A2, A4, A5) | Secret placé dans `BuildConfig`, sauvegarde par défaut | rule 40 § secrets ; `settings.json` (deny lecture des secrets) |
| Écriture destructive mal placée (B27, D26) | `clear*()` dans une méthode appelée par élément | rule 30 § écriture destructive |
| Dépendance UI dans la couche données (B28) | Repository qui manipule une `Activity` | rule 30 § couche données sans UI |
| Branches d'état incomplètes (B29) | Seule la branche nominale traitée | checklist anti-audit (correctness) |
| Requêtes concurrentes à la saisie (B30, D25) | `launch` par frappe sans annulation | rule 20 § recherche à la saisie |
| État mutable partagé, saisie, nullables (B31, B32, B33) | `var` capturée, `toInt()`, `?.toString()` | rule 60 |
| Adversaire effacé (B32, B11) | `mapNotNull` sur une résolution | rule 12 |
| Calcul sur le thread UI (P8, P9) | Construction `WarDetails` / agrégats hors `withContext` | rule 21 |
| Rafales réseau (P2, B30) | Parallélisme non borné vers MKCentral | rule 30 § résolution réseau |
| I/O au démarrage (P5) | Appel réseau bloquant le routage | checklist anti-audit (perf) |
| Composants UI dupliqués (D16, D18, D19, D21, D35) | Composable privé recréé sans chercher l'existant | rule 16 § chercher avant de créer ; rule 13 |
| Logique dupliquée (D2, D9, D10, D24-D29) | Copier-coller d'une branche ou d'un VM voisin | rules 16, 32, 61 ; checklist (duplication) |
| Extensions mal placées (D36) | Extension posée dans le fichier qui l'utilise | rule 61 § corollaire extensions |
| One-shots en `Flow`, paramètres morts (C9) | Signature calquée sur un ancien patron | rule 30 |
| Contexte statique / libellés en dur (C10) | Raccourci `MainApplication.instance` | rule 20 § ressources dans les VM |
| Fonctions locales (C11) | Helper déclaré dans la fonction appelante | rule 62 |
| Code mort / commenté (D34, C14) | Remplacement sans suppression de l'ancien | rule 64 § code commenté ; checklist (orphelins) |
| Constantes magiques (G1, G2, G6, G7, D28, D30) | Littéral métier recopié à chaque site | rule 61 § littéraux métier partagés ; rule 17 |
| Documentation obsolète (G8, P8) | Rule ou comportement modifié sans grep des références | rule 50 § références croisées |
| Tests / CI / version (T1-T3) | Décision utilisateur en attente | hors config — `CLAUDE.md` (pas de tests spontanés) |
| Sans prévention dédiée (D11-D15, D33, C2, C4-C6, C12, C13, G3-G5) | Dette historique, pas reproduite par le flux actuel | checklist anti-audit (ne pas aggraver) |

*Audit statique : le volet runtime (profiling, ANR, fuites) reste à compléter avec Android Studio Profiler + LeakCanary, et l'inspection « Unused declaration » de Studio pour un balayage définitif du code mort. Voir aussi [TECHNICAL.md](TECHNICAL.md) et [FUNCTIONAL.md](FUNCTIONAL.md).*