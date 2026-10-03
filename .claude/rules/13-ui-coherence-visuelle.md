# UI cohérente avec l'existant — la justesse des calculs reste prioritaire

**Portée** : tout ticket touchant l'affichage des **statistiques** et l'UI en
général (nouveaux écrans comme écrans existants modifiés).

## Cohérence visuelle

Le style de l'app est désormais établi (rendu de référence : `WelcomeScreen`, pôle
Accueil). Tout nouvel écran ou composant doit s'**aligner sur ce style existant** :

- **Réutiliser/adapter d'abord** les composants partagés (`MK*` dans `ui/`,
  `ui/cells/`, `ui/stats/`) et les couleurs de `ui/Colors.kt` — cf. rule `16`.
- **Créer ou modifier en profondeur** un composant reste autorisé quand l'existant ne
  suffit pas (nouveau composant, couleur ajoutée dans `ui/Colors.kt`,
  drawable/vecteur), en restant cohérent avec le style établi (couleurs, espacements,
  typographies, rayons, pastilles, états).
- Si un asset ou une police manque, utiliser l'équivalent projet le plus proche et
  **documenter l'écart** (résumé de PR / `docs/`).

## Priorité inchangée : la justesse des calculs prime toujours

Le niveau de finition visuel ne dispense **jamais** de la correction des données :

- **Les valeurs produites** (`extension/`, `model/local/Stats.kt`, workers…) doivent
  rester **correctes et testables**. C'est un prérequis, indépendant du rendu.
- Ne **jamais** sacrifier la justesse d'un calcul au profit du visuel. En cas de
  tension, la donnée correcte passe avant le pixel.
- **Données réelles uniquement** : ne jamais coder en dur de valeurs de démo
  (noms, scores, %).

## Pourcentages : fonctions centrales, format compact, répartitions exactes

Tout pourcentage passe par les fonctions centrales de `extension/` (#99), jamais par un
`* 100 /` entier ni un `"$x%"` local :

- **Calcul isolé** (un winrate, un taux de participation, `% maps gagnées`) → `Int.percentOf(total)`
  (`Double` arrondi au centième, `0.0` si total nul).
- **Répartition** (plusieurs parts affichées ensemble d'un même total : Top 6 / Bot 6, parts de
  points ou de shocks d'un classement) → `List<Int>.percentShares(total)` (plus grand reste,
  10 000 unités) : les parts qui couvrent tout le total somment **exactement** à 100 %, jamais
  99,99 / 100,01. Si les parts ne couvrent pas tout (alliés exclus, positions 13-24), passer le
  vrai `total` : l'écart reste une part implicite et les parts visibles ne somment pas à 100.
- **Affichage** → `Double.toPercentString(signed)` : format **compact** (`50 %`, `33,5 %`,
  `33,33 %` — au plus 2 décimales, zéros finaux retirés), séparateur de la locale, espace
  insécable avant `%`, `+` pour les deltas. Les strings reçoivent la chaîne formatée (`%1$s`).
- Les champs de pourcentage sont des `Double` ; tris, seuils (`winrateColor`) et deltas portent
  sur ces valeurs arrondies.

## Cellule à hauteur fixe contenant du texte : dimensionner sur les line-heights réels, en sp

Une hauteur de cellule fixe (uniforme dans une grille) qui doit contenir du texte se calcule pour le
**cas max**, pas au jugé : line-height de la police × taille × nombre de lignes max, plus paddings.
Le contenu garde sa hauteur naturelle et est **centré verticalement** dans la cellule (pas de lignes
vides réservées en haut).
Nunito (toutes graisses) : line-height = **1,364 × taille** (hhea 1011/−353 pour 1000 unités).
Exprimer la partie texte en **sp** (`x.sp.toDp()` via `LocalDensity`) pour suivre l'échelle de
police système, et partager les tailles de police entre le `MKText` et le calcul. Cf.
`TrackCellHeight` (`MKTrackCell`, #101 : tag tronqué sous un nom sur 2 lignes).

## Course avec intermission : dernier circuit, sans tag

Une course 24p avec intermission porte deux circuits (`WarTrack.index` = `[intermission, circuit
choisi]`). Partout où une **course** est représentée (cellule, en-tête, aperçu, podium/classement
d'un `TrackStats`), afficher **toujours le dernier circuit** (l'arrivée) et **aucun tag** :
passer par `List<Maps>.displayedMap()` / `displayedTag()` (`extension/ListExtension.kt`), jamais
par un `firstOrNull()`/`.name` local. Une grille de **sélection** (un candidat = un circuit) n'est
pas une course : chaque cellule garde son tag. Cf. #101.

## Combinaison avec les autres rules

Les autres rules UI (`10` clés de liste, `11` state, `12` roster, `14` back
onglets, `16` mutualisation des composants, `17` inset bottom bar) restent des
**contraintes de correction** indépendantes du niveau de finition.
