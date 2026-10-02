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

## Combinaison avec les autres rules

Les autres rules UI (`10` clés de liste, `11` state, `12` roster, `14` back
onglets, `16` mutualisation des composants, `17` inset bottom bar) restent des
**contraintes de correction** indépendantes du niveau de finition.
