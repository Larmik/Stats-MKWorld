---
paths:
  - "app/src/main/java/**/*ViewModel.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/RootScreen.kt"
---

# ViewModels : construction, recherche, ressources, calcul hors thread UI

## Factory assistée

- Un VM paramétré par la navigation expose une `@AssistedFactory` (`interface Factory { fun
  create(…) }`) et un constructeur `@AssistedInject`.
- Le câblage se fait dans `RootScreen.kt` / `HomeScreen.kt` :
  `hiltViewModel(key = …, creationCallback = { factory: XxxViewModel.Factory -> factory.create(…) })`.
  La `key` inclut tous les arguments de route.

## Ordre d'initialisation

Les initialiseurs s'exécutent dans l'ordre du texte, et un `Flow` Room/DataStore peut émettre
dès la souscription.

- Ordre : 1. propriétés mutables (`_state`, `_events`, caches) ; 2. flow exposé (`val state =
  … .mergeWith(_state).stateIn(…)`) ; 3. `init { … launchIn(…) }` ; 4. fonctions.
- Un `init` placé avant le `StateFlow` qu'il lit provoque un NPE sur `StateFlow.getValue()` :
  corriger en réordonnant, pas par un accès null-safe.

## Recherche déclenchée à la saisie

- Pas de `viewModelScope.launch` par frappe. Terme dans un `MutableStateFlow` +
  `debounce(300)` + `distinctUntilChanged()` + `flatMapLatest { … }`, ou `Job` annulé avant
  relance. Cf. audit B30.

## Ressources et libellés

- Pas de `MainApplication.instance?.applicationContext` ni de texte UI en dur dans un VM :
  exposer un id `R.string` (+ arguments) résolu par l'écran, ou injecter `@ApplicationContext`
  si la résolution côté VM est indispensable (filtre sur un libellé). Cf. audit C10.
- Exception : `DebugViewModel` (cf. `ui/strings.md`).

## Agrégation de wars : `withContext(Dispatchers.Default)`

Le collecteur d'un `StateFlow` de VM tourne sur `Main.immediate` : un calcul dans un
`combine`/`map`/`flatMapLatest` s'exécute sur le thread UI, même dans une `suspend fun` (#73).

- Concerné : tout VM qui **agrège** des wars (stats, classements, dashboard, période). Un VM qui
  construit un seul `WarDetails` (ex. `CurrentWarCellViewModel`) n'est pas concerné.
- Deux temps :
  1. sur le collecteur : lectures Room / DataStore / Firebase, filtre saison, champs légers du
     `State` (`seasons`, `selectedSeasonNumber`), toujours renseignés ;
  2. dans `withContext(Dispatchers.Default) { … }` : uniquement le calcul CPU
     (`map { WarDetails(War(it)) }` compris, `withFullStats`, podiums, `groupBy`, tris).
- Une lecture de source ne se répète pas par war dans une boucle : la lire une fois avant.
- Ne pas mettre `flowOn(Dispatchers.Default)` sur la chaîne de calcul : il déplace aussi les
  lectures de sources et peut réordonner un `mergeWith` (merge non ordonné).

```kotlin
val seasons = databaseRepository.getSeasons().firstOrNull().orEmpty()   // collecteur
val stats = withContext(Dispatchers.Default) { wars.map { WarDetails(War(it)) }.withFullStats(…) }
State(seasons = seasons, stats = stats)
```
