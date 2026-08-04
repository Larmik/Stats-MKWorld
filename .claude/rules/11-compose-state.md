# Choix du type de State Compose : `mutableStateOf` vs `derivedStateOf` vs `val` calculé

**Portée** : tout composable / fonction `@Composable` qui manipule ou dérive un
`State` Compose (`mutableStateOf`, `derivedStateOf`, `remember`, `rememberSaveable`,
valeur calculée en composition).

`mutableStateOf` (source de vérité qu'on écrit) et `derivedStateOf` (valeur
calculée en lecture seule) **ne sont pas interchangeables**. Choisir selon le rôle.

## `mutableStateOf` — état **possédé** par le composable

État local dont le composable est la source de vérité (champ de saisie, booléen
d'ouverture de dialog/popup, état d'expansion…). Toujours dans un
`remember { mutableStateOf(…) }`.

- S'il doit **survivre à rotation / mort du process** (recherche saisie, onglet
  sélectionné, popup ouverte) → `rememberSaveable`.
- Dans ce projet l'essentiel de l'état vit dans les **ViewModels** (`StateFlow` +
  `collectAsState`) : `mutableStateOf` est réservé au **pur état UI éphémère**, pas
  aux données métier.

## `derivedStateOf` — dérivation d'un `State`, avec filtrage

N'a d'intérêt que si **les deux** conditions sont réunies :

1. le calcul lit **un ou plusieurs `State` Compose** (pas de simples paramètres) ;
2. **l'entrée change plus souvent que la sortie** — il *filtre* des changements.

```kotlin
// firstVisibleItemIndex change à chaque pixel de scroll ; le booléen bascule 1×
val showButton by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

Typiquement : scroll, drag, saisie caractère par caractère, timer. Toujours dans
`remember`.

## `val` calculé — le cas par défaut

Dérivation simple dont **l'entrée change aussi souvent que la sortie** :
`derivedStateOf` n'apporte rien, un `val` en composition suffit.

```kotlin
val isEmpty = text.isEmpty()   // PAS de derivedStateOf
```

## Anti-patterns

- **`derivedStateOf` inutile** : l'envelopper autour d'une valeur dérivée de
  `state.value` **dans un bloc qui lit déjà `state.value` largement** ne réduit
  aucune recomposition (le bloc recompose à chaque émission). Utiliser un `val`,
  ou **extraire un sous-composable** ne lisant que le nécessaire (là
  `derivedStateOf` reprend son sens). Cas rencontré : `isComplete` /
  `takenPositions` dans `EditTrackScreen`/`AddTrackScreen`.
- **`derivedStateOf` sur un paramètre** (non-`State`) : il ne le voit pas → utiliser
  `remember(key) { … }`.
- **Dérivation à chaque frame sans `remember`** : annule tout bénéfice.

## Résumé décisionnel

| Besoin | Outil |
|---|---|
| État local possédé par le composable | `remember { mutableStateOf(…) }` |
| …qui doit survivre rotation / process death | `rememberSaveable { mutableStateOf(…) }` |
| Dérivation d'un `State` **qui change vite**, sortie rare | `remember { derivedStateOf { … } }` |
| Dérivation simple (entrée ≈ sortie en fréquence) | `val x = …` en composition |
| Dérivation d'un **paramètre** (non-`State`) | `remember(key) { … }` |
| Donnée métier / état d'écran | `StateFlow` (ViewModel) + `collectAsState` |

## Un switch/segmented/onglet met à jour l'affichage DYNAMIQUEMENT, jamais par re-navigation

**Portée** : tout contrôle qui **change le contenu du même écran** (segmented, chips,
onglets internes, toggle de vue/mode…).

Un tel contrôle doit modifier un **état** (local `mutableStateOf`/`rememberSaveable`
si purement UI, ou `StateFlow` du ViewModel si métier) et laisser l'UI se
**recomposer** ; il ne doit **jamais** re-naviguer vers la même route (ni
`popUpTo(self)`) pour « recharger » l'écran dans l'autre variante — cela remonte le
composable, recrée le ViewModel et déclenche des **transitions/slides parasites**.

- Si la variante dépend d'un **argument de nav** (ex. `is24p` de `Home/AddWar/{is24p}`),
  cet argument ne sert qu'à **semer la valeur initiale** ; le toggle bascule ensuite un
  **état interne réactif** (ex. `MutableStateFlow<Boolean>` dans le VM, exposé dans le
  `State`), et tout ce qui dépend du mode réagit à cet état. Cas rencontré :
  `AddWarViewModel.onModeChange` (12/24) — l'écran reste monté, la sélection est
  réinitialisée en interne, sans re-nav.
- La re-navigation reste réservée à un **vrai changement d'écran** (destination
  différente), pas à une variante d'affichage du même écran.

## Wizard/stepper à étapes : le retour en arrière ANNULE la sélection de l'étape rejointe

**Portée** : tout **wizard multi-étapes sur un seul écran** piloté par un `step` d'état
(ex. `AddWarViewModel.State.step` + `MKStepper` ; futur wizard de course/addtrack).

Dans un wizard, l'étape courante est un **état du VM** (pas de re-navigation, cf.
ci-dessus). La navigation **arrière** (bouton « Précédent », back système/`BackHandler`,
clic sur une étape antérieure du stepper) doit **réinitialiser la sélection faite à
l'étape que l'on rejoint** — on revient pour **refaire** ce choix :

- centraliser avant/arrière dans **une seule** fonction `onStepChange(step)` : aller
  **en avant** (ou rester : `step >= current`) ne réinitialise rien ; aller **en arrière**
  (`step < current`) déclenche le reset de l'étape cible ;
- **revenir à la PREMIÈRE étape = remise à zéro COMPLÈTE** : on ne réinitialise pas
  seulement la première étape, mais **toute la sélection du wizard** (on repart d'un choix
  vierge) ; revenir à une étape intermédiaire ne réinitialise que cette étape-là ;
- toutes les entrées (Précédent, `BackHandler`, clic stepper) passent par cette fonction
  → le reset est automatique et cohérent ;
- **mutualiser** le corps de reset s'il est partagé avec un autre déclencheur (ex. un
  changement de mode 12/24 qui réinitialise déjà la même étape → ≥ 2 appelants, extraction
  justifiée, rules 30/61). Cas rencontré : `AddWarViewModel.onStepChange` →
  `resetOpponentSelection()` (retour à la 1ʳᵉ étape Adversaire : vide `teamSelected`/rosters,
  restaure `teamList = teams`, replie le sélecteur inline, **ET** remet la line-up à zéro —
  remise à zéro complète ; **partagé avec `onModeChange`**, qui repasse aussi par `step = 0`) /
  `resetPlayerSelection()` (retour à l'étape Joueurs : tous les `isSelected = false`).

Le **gating du stepper** (une étape avancée n'est cliquable que si les précédentes sont
complètes) reste indépendant : après reset, l'étape avale redevient inaccessible tant
qu'on n'a pas re-complété — cohérent.
