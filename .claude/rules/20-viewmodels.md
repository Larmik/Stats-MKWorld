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

## Recherche déclenchée à la saisie : annuler la requête précédente

**Portée** : toute recherche réseau (ou calcul coûteux) lancée à chaque frappe.

Ne pas faire `viewModelScope.launch { … }` à chaque caractère : les requêtes s'empilent,
une réponse lente d'un terme ancien écrase celle du terme courant, et la rafale fait
throttler MKCentral (rule 30). Exposer le terme dans un `MutableStateFlow`, puis
`debounce(300)` + `distinctUntilChanged()` + `flatMapLatest { … }` (ou garder le `Job` et
l'annuler avant d'en relancer un). Cf. audit B30 (`RegistryViewModel`/`TeamProfileViewModel`).

## Pas de `Context` statique ni de libellé en dur dans un ViewModel

Ne pas lire `MainApplication.instance?.applicationContext` ni émettre de texte UI en dur
depuis un VM : exposer des ids `R.string` (avec arguments) résolus par l'écran, ou injecter
`@ApplicationContext` si une résolution côté VM est indispensable (filtre de recherche sur
un libellé). Les chaînes utilisateur restent en français dans `res/values*/`. Cf. audit C10.
