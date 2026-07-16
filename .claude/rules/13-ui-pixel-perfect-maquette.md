# UI pixel-perfect vs la maquette (norme) — la justesse des calculs reste prioritaire

**Portée** : tout ticket touchant l'affichage des **statistiques** et l'UI en
général pendant l'epic refonte graphique. S'applique aux **nouveaux tickets** et
aux **écrans impactés**.

**Norme (depuis le ticket #23 / PR #33)** : l'UI doit viser le **pixel-perfect**
par rapport à la maquette du prototype UX, **dans la mesure du possible**. Le rendu
validé sur `WelcomeScreen` (pôle Accueil) fait **référence** : c'est le niveau de
finition attendu.

> Ceci **remplace** l'ancienne consigne « garder l'UI minimale / ne pas peaufiner »
> (qui supposait une refonte graphique ultérieure jetant le travail visuel). La
> refonte se fait désormais **écran par écran**, et le rendu visuel de chaque écran
> impacté est livré au niveau maquette.

## Exigé

- **Reproduire fidèlement le style de la maquette** : couleurs, espacements,
  typographies, rayons, dégradés, ombres, pastilles, icônes, états actifs… Extraire
  les valeurs depuis la **source du prototype** (`docs/prototype/stats-mkworld-5poles.html`
  — variables CSS `--…`, classes des cartes/segmented/pills… — et `docs/PROTOTYPE_UX.md`
  pour la structure/les libellés).
- **Modifications profondes autorisées** pour atteindre ce rendu : adapter en
  profondeur **ou créer** des composants, ajouter des couleurs (idéalement dans
  `ui/Colors.kt`, sinon locales au fichier), créer les **drawables/vecteurs**
  nécessaires (ex. `ic_flame` teintée). L'ancienne interdiction du polish ne
  s'applique plus.
- **« Dans la mesure du possible »** : quand un asset ou une police de la maquette
  n'existe pas dans le projet (ex. police Urbanist/MKPosition absente, icône
  manquante), utiliser l'équivalent projet le plus proche (police existante,
  drawable créé ou approximation) et **documenter l'écart résiduel** (résumé de PR
  / `docs/`). Viser le plus proche possible, pas l'impossible.

## Priorité inchangée : la justesse des calculs prime toujours

Le niveau de finition visuel ne dispense **jamais** de la correction des données :

- **Les valeurs produites** (`extension/`, `model/local/Stats.kt`, workers…) doivent
  rester **correctes et testables**. C'est un prérequis, indépendant du rendu.
- Ne **jamais** sacrifier la justesse d'un calcul au profit du visuel. En cas de
  tension, la donnée correcte passe avant le pixel.
- **Données réelles uniquement** : ne **jamais** coder en dur les valeurs de démo de
  la maquette (noms, scores, %) — elles illustrent seulement le type de donnée.

## Combinaison avec les autres rules

- **`15-ui-prototype-reference.md`** fixe *quoi* afficher et *où* (5 pôles, sections/
  onglets, libellés FR, navigation) **et** intègre désormais la fidélité visuelle à
  son critère de validation. Cette rule 13 fixe le *niveau de finition* (pixel-perfect)
  et la priorité calcul.
- Les autres rules UI (`10` clés de liste, `11` state, `12` roster, `14` back
  onglets) restent des **contraintes de correction** indépendantes du niveau de
  finition.
