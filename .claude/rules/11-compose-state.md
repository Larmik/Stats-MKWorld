# Choix du type de State Compose : `mutableStateOf` vs `derivedStateOf` vs `val` calculé

**Portée** : tout composable ou fonction `@Composable` qui manipule ou dérive un
`State` Compose (`mutableStateOf`, `derivedStateOf`, `remember`,
`rememberSaveable`, valeur calculée en composition).

`mutableStateOf` et `derivedStateOf` **ne sont pas interchangeables** : l'un est
une source de vérité qu'on écrit, l'autre une valeur calculée en lecture seule.
Choisir selon le rôle, pas par réflexe.

## `mutableStateOf` — état **possédé** par le composable

Pour l'état local dont le composable est la source de vérité (champ de saisie,
booléen d'ouverture de dialog/popup, état d'expansion…). Toujours dans un
`remember { mutableStateOf(…) }`.

- Si l'état doit **survivre à une rotation / mort du process** (recherche saisie,
  onglet sélectionné, popup ouverte), préférer `rememberSaveable`.
- Dans ce projet, l'essentiel de l'état vit dans les **ViewModels**
  (`StateFlow` + `collectAsState`) : `mutableStateOf` reste réservé au **pur état
  UI éphémère**, pas aux données métier (qui remontent du ViewModel).

## `derivedStateOf` — valeur **dérivée** d'autres `State`, avec filtrage

N'a d'intérêt que si **les deux** conditions sont réunies :

1. le calcul lit **un ou plusieurs `State` Compose** (pas de simples paramètres) ;
2. **l'entrée change plus souvent que la sortie** — il *filtre* des changements,
   donc évite des recompositions.

Cas d'école (l'entrée est bruyante, la sortie rare) :

```kotlin
// firstVisibleItemIndex change à chaque pixel de scroll ; le booléen bascule 1×
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

Typiquement : scroll, drag, saisie caractère par caractère, timer. Toujours
enveloppé dans `remember`.

## `val` calculé — le cas par défaut

Pour une dérivation simple dont **l'entrée change aussi souvent que la sortie**,
`derivedStateOf` n'apporte **rien** : un `val` calculé en composition est
équivalent, plus court et plus lisible.

```kotlin
val isEmpty = text.isEmpty()   // suffisant — PAS de derivedStateOf
```

## Anti-patterns à éviter

- **`derivedStateOf` inutile.** L'envelopper autour d'une valeur dérivée de
  `state.value` (un `StateFlow` collecté) **à l'intérieur d'un bloc qui lit déjà
  `state.value` largement** ne réduit aucune recomposition : le bloc recompose de
  toute façon à chaque émission. Utiliser un `val`, ou **extraire un
  sous-composable** qui ne lit que le strict nécessaire — et là `derivedStateOf`
  reprend son sens. (Cas rencontré : `isComplete` / `takenPositions` dans
  `EditTrackScreen`/`AddTrackScreen`.)
- **`derivedStateOf` sur un paramètre** (valeur non-`State`) : il ne le « voit »
  pas. Pour dépendre d'un paramètre, utiliser `remember(key) { … }`.
- **Clé recalculée / dérivation à chaque frame** sans `remember` : annule tout
  bénéfice et aggrave les recompositions.

## Résumé décisionnel

| Besoin | Outil |
|---|---|
| État local possédé par le composable | `remember { mutableStateOf(…) }` |
| …qui doit survivre rotation / process death | `rememberSaveable { mutableStateOf(…) }` |
| Dérivation d'un `State` **qui change vite**, sortie rare | `remember { derivedStateOf { … } }` |
| Dérivation simple (entrée ≈ sortie en fréquence) | `val x = …` en composition |
| Dérivation d'un **paramètre** (non-`State`) | `remember(key) { … }` |
| Donnée métier / état d'écran | `StateFlow` (ViewModel) + `collectAsState` |
