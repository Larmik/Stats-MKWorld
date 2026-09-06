# Ordre d'initialisation des propriétés flow dans les ViewModels

**Portée** : tout `ViewModel` qui souscrit à un `Flow` (`launchIn`, `collect`,
`onEach {}.launchIn`, `stateIn`, `mergeWith`) dans un bloc `init` ou un initialiseur
de propriété.

Les initialiseurs et blocs `init` s'exécutent dans **l'ordre textuel**. Un `Flow`
DataStore/Room émet souvent une **valeur synchrone dès la souscription** : la lambda
(`onEach`, `collect`, `map`) peut s'exécuter **immédiatement**, pendant la construction.

**Interdit** : placer un `init { … .launchIn(...) }` (ou initialiseur souscrivant à
un flow) **avant** la déclaration d'un `StateFlow`/`MutableStateFlow` que sa lambda
**lit** (`_state.value`, `state.value`, ou tout flow exposé). À l'émission synchrone
la propriété vaut encore `null` → crash :

```
NullPointerException: Attempt to invoke interface method
'java.lang.Object kotlinx.coroutines.flow.StateFlow.getValue()' on a null object reference
```

**Règle** : déclarer `_state`/`state` (et tout flow lu par l'init) **avant** le bloc
`init`. Ordre recommandé :

1. propriétés mutables (`_state`, `_events`, caches `private var …`) ;
2. flow exposé (`val state = … .mergeWith(_state).stateIn(...)`) ;
3. bloc `init` (souscriptions `launchIn`) ;
4. fonctions.

Correction type = **réordonnancement uniquement**, sans changer la logique. Ne pas
masquer le symptôme par un accès null-safe si l'ordre est en cause.
