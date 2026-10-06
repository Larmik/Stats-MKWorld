---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/**"
---

# Composants UI : style établi, composant partagé unique

## Cohérence visuelle

- Aligner tout écran ou composant sur le style établi (référence : `WelcomeScreen`, pôle
  Accueil) : couleurs de `Colors` (`ui/Resources.kt`), polices de `Fonts`, espacements, rayons.
- Réutiliser ou adapter d'abord les composants `MK*` de `ui/`, `ui/cells/`, `ui/stats/`. Créer un
  composant, une couleur ou un drawable reste permis si l'existant ne suffit pas.
- Asset ou police manquant : prendre l'équivalent projet le plus proche et documenter l'écart.
- Image déjà embarquée en drawable : l'afficher directement, sans charger l'équivalent d'une API
  (ni mapping DTO, ni cache, ni `AsyncImage`). Cf. #152 (`Tournament.logo`).
- Ne jamais coder en dur de valeur de démo (noms, scores, %).

## Chercher l'existant avant de créer

- Avant d'écrire un composable (même `private`) ou un helper d'affichage :
  `rg "fun <Nom>\(" app/src/main` et un nom voisin (`Chip`, `Crest`, `Logo`, `Eyebrow`, `Card`,
  `Tile`, `initialsOf`…), dans `ui/**` et dans les autres écrans.
- Public → le réutiliser. Privé dans un autre écran → l'extraire (règle suivante). Cf. audit D35.

## Mutualiser dès le 2ᵉ écran consommateur

- Un seul consommateur : rester local. Dès ≥ 2 écrans : extraire en public dans le package
  approprié (`ui/cells/` cellule, `ui/stats/` carte de stats, `ui/` transverse préfixé `MK`).
- Un seul exemplaire : supprimer la copie d'origine et la faire pointer sur la version partagée.
- Généraliser par paramètres optionnels (`onClick: (() -> Unit)? = null`, `onDark`…), pas par fork.
  Cf. `PodiumCell` (`ui/stats/MKPodiumCell.kt`, #26).

## Composants uniques imposés

Ne jamais recréer d'équivalent local ; un besoin non couvert = un paramètre optionnel en plus.

- **`MKButton`** (`ui/MKButton.kt`, #50) : seul bouton de l'app, un seul style (fond
  `Colors.white30`, sans bordure, libellé et icône blancs en majuscules Urbanist, coins 10 dp).
  Pas de bordure, de fond opaque, de dégradé ni de variante primaire/secondaire.
  - Paramètres : `icon: Int?`, `textColor` (`Colors.black` sur surface claire comme `MKDialog`).
  - Container Material transparent à l'état actif et désactivé (`disabledContainerColor =
    Color.Transparent`, `disabledElevation = 0.dp`).
  - Plusieurs boutons sur une `Row` : `Modifier.weight(1f)` chacun + `Arrangement.spacedBy(9.dp)`.
    Un bouton seul garde sa largeur intrinsèque.
  - Deux actions de même niveau : même largeur et même présence d'icône (cf. #105).
- **`MKSegmentedSelector`** (`ui/MKSegmentedSelector.kt`) : seul segmented, stateless (`page` +
  `onClick`) ; `onDark = true` sur carte sombre, `false` sur le dégradé de `BaseScreen`.
- **`MKStepper`** (`ui/MKStepper.kt`) : seul stepper de wizard, stateless.
