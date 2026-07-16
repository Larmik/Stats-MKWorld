# Prototype UX « 5 pôles » : référence de navigation/écrans pour l'epic refonte

**Portée** : tout ticket de l'**epic refonte UX (navigation 5 pôles + stats/résultats)**
touchant la navigation, la structure d'un écran, le placement d'une stat/section, ou
les libellés utilisateur. S'applique tant que l'epic est en cours.

Un **prototype navigable** fait foi pour la cible UX. Avant d'implémenter un écran ou
un flux de cet epic, se référer à la maquette :

- Spécification écran par écran : **`docs/PROTOTYPE_UX.md`** (5 pôles, 22 écrans,
  sections/onglets/libellés/valeurs de démo, et la navigation entre écrans).
- Maquette HTML navigable (ouvrable hors-ligne dans un navigateur) :
  **`docs/prototype/stats-mkworld-5poles.html`**.
- Source d'origine (artifact) :
  `https://claude.ai/code/artifact/58f3218f-3d51-4af4-8ff1-620dc3beac2d`.

Règles d'usage :

- **Le prototype décrit la CIBLE, pas le pixel-perfect — la fidélité n'est PAS un
  objectif.** Il fixe l'intention : les 5 pôles et leur ordre (Accueil · Wars · Stats ·
  Classements · Profil), le rattachement de chaque écran à son pôle, les sections/onglets
  présents, l'ordre de ces sections, les **libellés FR** et la navigation (quel élément
  mène à quel écran). Suivre cette structure et ces libellés. En revanche, **il est
  attendu et acceptable que le rendu diverge de la maquette** : ne pas chercher à
  reproduire la maquette à l'identique.
- **Ne JAMAIS recréer de zéro un élément graphique qui existe déjà.** Réutiliser
  systématiquement les composants/écrans existants (`MK*`, `ui/cells/`, `ui/stats/`,
  sections déjà en place…) : on ne crée du neuf que pour ce qui n'existe pas.
  (Complète `13-ui-minimale-phase-refonte.md`.)
- **En revanche, ADAPTER un composant existant est autorisé** pour le rapprocher de la
  maquette et gagner en cohérence :
  - Faire le **minimum** — on n'est pas obligé de tout réadapter, seulement ce qui est
    nécessaire pour se rapprocher de la maquette.
  - **Conserver TOUS les éléments visuels existants** du composant : on n'en retire aucun.
  - On peut **ajouter des choses à l'intérieur** d'un composant, à condition d'adapter
    correctement l'UI (l'ajout doit s'intégrer proprement, pas casser la mise en page).
  - Adapter/étendre ≠ recréer : rester dans le composant existant, ne pas le remplacer par
    un nouveau.
- **Ne pas reproduire le polish visuel du prototype** (couleurs, espacements, typos,
  animations) : la règle `13-ui-minimale-phase-refonte.md` prime — UI minimale,
  réutiliser les composants `MK*`/`ui/cells/`. Le prototype sert à cadrer *quoi* afficher
  et *où*, pas *avec quel style*.
- Les **valeurs affichées dans la maquette sont des données de démo** (noms, scores, %) :
  ne pas les coder en dur ; elles illustrent seulement le type de donnée attendu.
- **Divergence prototype ↔ ticket** : si un ticket demande un écran/flux qui contredit le
  prototype, le **signaler** (comme pour un conflit rule ↔ ticket) au lieu de trancher
  seul.
- La justesse des calculs de stats reste prioritaire (`13-…`) ; le prototype ne préjuge
  pas des formules, seulement de leur présentation.

## Critère de validation (Definition of Done)

Un ticket qui touche un **écran décrit dans `docs/PROTOTYPE_UX.md`** n'est **validé**
(passé « Terminé » / fermé sur le board) que si **les deux** conditions sont réunies :

1. **Conformité maquette** : l'écran comporte les sections/onglets/insights prévus par le
   prototype, dans le bon rattachement de pôle, avec les libellés FR attendus et la
   navigation décrite. Un écart (section/onglet/insight manquant, mauvaise destination)
   = **pas validé** → rester « À faire ».
2. **Respect des règles composants** (`13`/`15`) : réutiliser/adapter l'existant, **ne pas
   recréer** ; UI minimale ; on peut adapter/étendre un composant en gardant tous ses
   éléments visuels.

« Fonctionnellement présent » ne suffit pas. Au moment de valider un ticket (cf. étape de
validation de `/ticket-dev`), **vérifier explicitement la conformité maquette écran par
écran** et lister les écarts éventuels avant de proposer la clôture.

**Exemption** : un ticket **purement technique / sans écran** (calculs, perfs, migration)
n'a pas de critère de conformité maquette — il est validé sur ses critères fonctionnels.
