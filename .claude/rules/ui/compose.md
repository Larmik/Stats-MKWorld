---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/**"
---

# Compose : clés de liste, State, collecte, switch et wizard

## Clés de `LazyList` / `LazyGrid`

- La valeur de `key = { … }` est stockable dans un `Bundle` : `String`, `Int`, `Long`… Jamais
  un objet, une `data class` ni une `sealed class` (crash `Type of the key … is not supported`).
- Clé = identifiant primitif stable (`it.id`, `it.war.id`, `it.name` pour un enum) ; composite →
  `String` (`"${it.teamId}-${it.type}"`).
- Modèle sans id stable (`WarPenalty`, lignes de formulaire) ou `items(count)` à plage fixe
  (positions 1..12) : pas de clé.
- Jamais de clé recalculée à chaque frame (`UUID.randomUUID()`).

## Type de State

| Besoin | Outil |
|---|---|
| État UI éphémère possédé par le composable | `remember { mutableStateOf(…) }` |
| …qui doit survivre rotation / mort du process | `rememberSaveable { mutableStateOf(…) }` |
| Dérivation d'un `State` qui change plus souvent que sa sortie (scroll, saisie) | `remember { derivedStateOf { … } }` |
| Dérivation simple (entrée ≈ sortie) | `val x = …` en composition |
| Dérivation d'un paramètre (non-`State`) | `remember(key) { … }` |
| Donnée métier / état d'écran | `StateFlow` du VM |

- Ne pas envelopper de `derivedStateOf` une valeur dérivée de `state.value` dans un bloc qui lit
  déjà `state.value` : utiliser un `val` ou extraire un sous-composable.
- Tri/conversion dérivé d'un `State` (ex. `sortedByDescending` + `toPodiumEntry`) : dans
  `remember(sortIndex, source)`.

## Collecte d'un `StateFlow`

- Utiliser `collectAsStateWithLifecycle()`, pas `collectAsState()` (audit C8).

## Switch, segmented, onglet interne

- Un contrôle qui change le contenu du même écran modifie un état (local ou VM) ; ne jamais
  re-naviguer vers la même route pour « recharger » la variante.
- Un argument de nav (ex. `is24p` de `Home/AddWar/{is24p}`) ne fait que semer la valeur initiale ;
  le toggle bascule ensuite l'état interne du VM (`AddWarViewModel.onModeChange` : champ privé
  `is24p` + `State.is24p`).

## Wizard à étapes sur un seul écran

- L'étape courante est un état du VM (`State.step` + `MKStepper`), pas une destination.
- Centraliser avant/arrière dans une seule `onStepChange(step)` appelée par Précédent,
  `BackHandler` et le clic stepper : `step >= current` → rien à réinitialiser ; `step < current`
  → réinitialiser la sélection de l'étape rejointe.
- Revenir à la première étape = remise à zéro complète du wizard.
- Mutualiser le corps de reset s'il a ≥ 2 déclencheurs (cf. `kotlin/constantes-extensions.md`).
- Implémentations de référence : `AddWarViewModel.onStepChange` (`resetOpponentSelection`,
  partagé avec `onModeChange` ; `resetPlayerSelection`) et `AddTrackViewModel.onStepChange`
  (`resetTrack`, `resetIntermission`, `resetPositions`).
- Le gating du stepper (étape cliquable si les précédentes sont complètes) reste indépendant.
