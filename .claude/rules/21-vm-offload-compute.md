# Déporter un calcul CPU lourd d'un ViewModel : `withContext(Dispatchers.Default)`, PAS `flowOn`

**Portée** : tout `ViewModel` qui exécute un **calcul CPU lourd** (agrégation de
stats, tri/construction de gros modèles) dans la lambda d'un `combine`/`map`/
`flatMapLatest` d'une chaîne `Flow` exposée en `StateFlow` (typiquement les VM
stats/classements/dashboard : `StatsFullViewModel`, `StatsRankingViewModel`,
`MapDetailViewModel`, `OpponentDetailViewModel`, `WelcomeViewModel`,
`WarListViewModel`).

Le calcul lourd ne doit **jamais** bloquer le thread UI : le collecteur d'un
`StateFlow` de VM est `viewModelScope` = `Main.immediate`, donc la lambda de
`combine`/`map` s'exécute **sur le main thread** — même déclenchée par une `suspend
fun` (suspendre ne change PAS de dispatcher). Symptômes : jank/freezes au changement
de sélecteur, de saison, ou à la navigation (#73).

## Exigé : `withContext(Dispatchers.Default)` autour de la SEULE portion de calcul

Structurer la lambda en **deux temps** :

1. **Lire les sources et calculer les données légères sur le collecteur** (HORS
   `withContext`) : lectures Room/DataStore (`getSeasons`, `mkcPlayer`, `mkcTeam`…),
   appels Firebase (`getCurrentWar`, `listenToCurrentWar`), résolution du filtre
   saison, et **tout champ de `State` qui doit rester peuplé** (ex. `seasons`,
   `selectedSeasonNumber` du dropdown).
2. **Envelopper UNIQUEMENT le calcul CPU lourd** dans `withContext(Dispatchers.Default) { … }` :
   `withFullStats`/`withFullTeamStats`, `computeState`/`computeRankings`, construction
   des podiums/agrégats, gros `map`/`groupBy`/tris. Renvoyer le `State` construit avec
   les champs légers **toujours renseignés**, même si la partie stats est nulle/vide.

```kotlin
// Sources + filtre saison sur le collecteur (seasons TOUJOURS peuplé)
val seasons = databaseRepository.getSeasons().firstOrNull().orEmpty()
val activeSeason = /* … */
val currentWar = firebaseRepository.getCurrentWar(rosterId)   // Firebase : HORS withContext
// SEULE la partie CPU-lourde sur Default
val (teamStats, playerStats) = withContext(Dispatchers.Default) {
    wars.withFullStats(...).firstOrNull() to wars.withFullStats(userId = …).firstOrNull()
}
State(seasons = seasons, currentWar = currentWar, teamStats = teamStats, …)
```

## Interdit : `flowOn(Dispatchers.Default)` sur la chaîne de calcul d'un tel VM

Ne **pas** poser `.flowOn(Dispatchers.Default)` sur la branche de calcul. `flowOn`
relocalise **tout l'upstream** (change l'ordre ET le threading d'émission), ce qui
casse deux invariants :

- **Course sur un merge non ordonné.** Si la chaîne passe par `mergeWith`
  (`extension/FlowExtension.kt` = `flowOf(this, flow).flattenMerge()`, merge NON
  ordonné), `flowOn(Default)` transforme la séquence déterministe main-thread en une
  **course cross-thread** : l'état initial/vide (seed de `stateIn` ou `State()` de
  `_state`, `seasons = []`) peut **survivre** à l'état calculé complet → régression
  observée (#73) : **dropdown de saison disparu de tous les headers**.
- **Exception main-affine.** `flowOn(Default)` déplace aussi **toutes** les lectures
  de sources (Room, Firebase, DataStore) et appels potentiellement liés au main
  thread. Si l'un throw, la branche compute **meurt** → `stateIn` reste bloqué sur le
  seed vide.

`withContext` cible le **CPU** sans toucher aux I/O, à l'ordre ni au threading
d'émission du flow — c'est la seule voie sûre ici. Un `flowOn` reste acceptable sur
une chaîne **purement calcul, sans `mergeWith`/`flattenMerge` ni source main-affine
en upstream**, mais dans le doute (VM stats de ce projet), utiliser `withContext`.

## Corollaire UI (mémoïsation) — rule 11

Le déport du calcul VM ne remplace pas la mémoïsation en composition : les tris et
conversions dérivés d'un `State` (ex. `sortedByDescending` + `toPodiumEntry` dans
`PlayerMapsRankingScreen`/`PlayerOpponentsRankingScreen`) restent enveloppés dans
`remember(sortIndex, source)` (rule 11) pour ne pas être recalculés à chaque
recomposition.
