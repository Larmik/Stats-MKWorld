---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/extension/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/local/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/WarTrack.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/warList/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/addTrack/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTrack/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/stats/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/**"
---

# Calcul et affichage des statistiques

## Justesse avant tout

- Les valeurs produites (`extension/`, `model/local/Stats.kt`, workers) restent correctes et
  testables ; ne jamais sacrifier un calcul au rendu.
- Données réelles uniquement, aucune valeur de démo.

## Périmètre des stats

- Nouvelle stat = **12p uniquement** (`teamOpponent.size == 1`) ; le support 24p existant reste
  intact, toute extension 24p relève d'un ticket dédié.
- Top 6 / Bot 6 (12p) : manche avec `teamScore == 61` / `teamScore == 21`, égalité exacte, pas un
  seuil.

## Pourcentages (#99)

- Calcul isolé (winrate, participation) → `Int.percentOf(total)` (`Double` au centième, `0.0` si
  total nul). Jamais de `* 100 /` entier.
- Répartition de plusieurs parts d'un même total (Top 6 / Bot 6, parts de points ou de shocks) →
  `List<Int>.percentShares(total)` : les parts couvrant tout le total somment à 100 %. Parts
  partielles (alliés exclus) : passer le vrai `total`.
- Affichage → `Double.toPercentString(signed)` (`50 %`, `33,5 %`, espace insécable, `+` pour un
  delta). Jamais de `"$x%"` local.
- Champs en `Double` ; tris, seuils (`winrateColor`) et deltas portent sur ces valeurs.

## Course avec intermission (#101)

- Une course 24p avec intermission porte `[intermission, circuit choisi]`. Partout où une course
  est représentée (cellule, en-tête, podium d'un `TrackStats`) : dernier circuit, sans tag, via
  `List<Maps>.displayedMap()` / `displayedTag()`. Jamais de `firstOrNull()` / `.name` local.
- Grille de sélection de circuits : chaque cellule garde son tag.

## Classements et positions moyennes (#102)

- Position moyenne = vraie moyenne des positions (`Double`, `Double.toCompactString()`), jamais
  `pointsToPosition` d'une moyenne de points. En vue joueur, ne moyenner que les manches courues
  (`WarTrack.hasPlayer`).
- Top/Flop de performance : seuil `Stats.MIN_RANKING_SAMPLE` partout ; tri sur la valeur affichée
  (vue joueur → `sortedByTrackScore`).
- Circuits et adversaires : Top et Flop disjoints (`flopExcludingTop()`) ; sous 3 entrées,
  message (`PodiumSectionCard(completeRowsOnly = true)`), jamais de podium tronqué. Podiums
  pilotes / baggeurs : podium partiel, flop masqué si vide.
- Le libellé suit le critère (tri « score » en vue joueur = « Position »). Un tri de fréquence se
  libelle « Plus / Moins joués » (ou « affrontés »), pas « Top / Flop ».
