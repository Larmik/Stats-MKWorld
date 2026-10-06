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

## Agrégation de wars : hors thread UI, annulable

Par défaut (`viewModelScope` = `Main.immediate`), toute la chaîne d'un `StateFlow` de VM
s'exécute sur le thread UI, `suspend fun` comprises : tout calcul CPU d'agrégation doit en sortir
via `withContext(Dispatchers.Default)` (#73).

- Concerné : tout VM qui **agrège** des wars (stats, classements, dashboard, période). Un VM qui
  construit un seul `WarDetails` (ex. `CurrentWarCellViewModel`) n'est pas concerné.
- Sur le collecteur : lectures Room / DataStore / Firebase (non bloquantes : executors propres ou
  callbacks), filtre saison, champs légers du `State` (`seasons`, `selectedSeasonNumber`),
  toujours renseignés.
- Dans `withContext(Dispatchers.Default) { … }` : uniquement le calcul CPU
  (`map { WarDetails(War(it)) }` compris, `withFullStats`, podiums, `groupBy`, tris).
- La branche qui porte le calcul utilise un opérateur `*Latest` (`mapLatest`, `flatMapLatest`,
  `collectLatest`) : une nouvelle émission (saison, filtre, mode) annule le calcul obsolète.
- `combine` : combiner les sources en valeur légère (`Pair`/`Triple`, data class privée
  `Sources`), puis `.mapLatest { … withContext(Default) { calcul } … }`. Jamais de calcul dans la
  lambda du `combine` (elle n'est pas annulée).
- L'annulation n'agit qu'aux points de suspension : à la sortie du `withContext`, un résultat
  obsolète n'est pas émis. Un très long calcul en boucle peut appeler `ensureActive()` / `yield()`.
- Une lecture de source ne se répète pas par war dans une boucle : la lire une fois avant.
- `withContext` plutôt que `flowOn(Dispatchers.Default)`, qui déplace aussi les lectures de
  sources et peut réordonner un `mergeWith` (merge non ordonné).

```kotlin
val state = combine(getWars(), _seasonFilter, getSeasons(), _kindFilter, ::Sources)
    .mapLatest { (wars, seasonFilter, seasons, kindFilter) ->
        val season = seasonFilter.resolve(seasons)                       // collecteur
        val stats = withContext(Dispatchers.Default) { wars.map { WarDetails(War(it)) }.withFullStats(…) }
        State(seasons = seasons, stats = stats)
    }
```
