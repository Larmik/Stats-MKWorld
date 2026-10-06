---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/stats/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/**/*Screen.kt"
---

# Mise en page des cellules : hauteurs et alignements

## Cellule à hauteur fixe contenant du texte

- Calculer la hauteur pour le cas max : line-height × taille × nombre de lignes max + paddings.
- Nunito (toutes graisses) : line-height = 1,364 × taille.
- Exprimer la partie texte en sp (`x.sp.toDp()` via `LocalDensity`) et partager les tailles de
  police entre le `MKText` et le calcul. Cf. `TrackCellHeight` (`ui/cells/MKTrackCell.kt`, #101).
- Contenu à hauteur naturelle, centré verticalement (pas de lignes vides réservées).

## Cellules côte à côte d'une même ligne (podium, grille)

- Même hauteur : `IntrinsicSize.Min` sur la `Row` + `fillMaxHeight()` sur la cellule.
- Bloc d'identité (image, nom, tag) en haut, bloc de stats en bas (`Spacer(Modifier.weight(1f))`
  entre les deux). Cf. `PodiumCell` (#102).

## Blocs comparés avec un élément optionnel sous la valeur

- Ex. score + pénalité d'un seul côté, colonne centrale : colonnes alignées en haut, pas centrées.
- Aligner le centre visuel des éléments correspondants (logos entre eux, valeurs entre elles).
- La colonne centrale reprend la grille de rangées des côtés via des composables partagés ;
  aucune hauteur recopiée en dur ; textes en `resizable = false`. Cf. `WarScoreCard`
  (`ui/cells/WarSummaryCells.kt`, #103).
