# Ordre d'initialisation des propriétés flow dans les ViewModels

**Portée** : tout `ViewModel` qui souscrit à un `Flow` (`launchIn`, `collect`,
`onEach {}.launchIn`, `stateIn`, `mergeWith`) dans un bloc `init` ou un
initialiseur de propriété.

En Kotlin, les initialiseurs de propriétés et les blocs `init` s'exécutent dans
**l'ordre textuel** de la classe. Un `Flow` issu de DataStore ou de Room émet
souvent une **valeur synchrone dès la souscription** : la lambda (`onEach`,
`collect`, `map`) peut donc s'exécuter **immédiatement**, pendant la construction
du ViewModel.

**Interdit** : placer un `init { … .launchIn(...) }` (ou un initialiseur qui
souscrit à un flow) **avant** la déclaration d'une propriété `StateFlow` /
`MutableStateFlow` que sa lambda **lit** (`_state.value`, `state.value`, ou tout
flow exposé). Au moment de l'émission synchrone, la propriété vaut encore `null`
→ crash :

```
NullPointerException: Attempt to invoke interface method
'java.lang.Object kotlinx.coroutines.flow.StateFlow.getValue()' on a null object reference
```

**Règle** : déclarer `_state` / `state` (et tout flow lu par l'init) **avant** le
bloc `init` et avant tout initialiseur qui les référence. Concrètement, l'ordre
recommandé dans la classe est :

1. propriétés mutables (`_state`, `_events`, caches `private var …`) ;
2. flow exposé (`val state = … .mergeWith(_state).stateIn(...)`) ;
3. bloc `init` (souscriptions `launchIn`) ;
4. fonctions.

Correction type = **réordonnancement uniquement** (déplacer les déclarations
avant l'init), sans changer la logique. Ne pas masquer le symptôme par un accès
null-safe si l'ordre est réellement en cause.
