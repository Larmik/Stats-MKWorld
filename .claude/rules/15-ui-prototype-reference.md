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

- **Le prototype décrit la CIBLE, et la fidélité visuelle EST désormais un objectif
  (pixel-perfect dans la mesure du possible, cf. `13-ui-pixel-perfect-maquette.md`).**
  Il fixe à la fois : (a) la **structure** — les 5 pôles et leur ordre (Accueil · Wars ·
  Stats · Classements · Profil), le rattachement de chaque écran à son pôle, les
  sections/onglets présents, leur ordre, les **libellés FR** et la navigation (quel
  élément mène à quel écran) ; (b) le **rendu visuel** — couleurs, espacements,
  typographies, rayons, dégradés, pastilles, états… à reproduire au plus près. Suivre
  cette structure, ces libellés **et** ce style.
- **Réutiliser/adapter l'existant reste le premier réflexe** (cohérence, moins de
  code) : partir des composants/écrans en place (`MK*`, `ui/cells/`, `ui/stats/`,
  sections déjà présentes) et les **adapter** au style de la maquette. Ce n'est plus
  une interdiction de créer : **créer ou modifier en profondeur un composant est
  autorisé** dès que c'est nécessaire pour atteindre le pixel-perfect (nouveaux
  composants, nouvelles couleurs `ui/Colors.kt`, drawables/vecteurs…). Préférer
  l'adaptation quand elle suffit ; créer quand elle ne suffit pas.
- **Reproduire le polish visuel du prototype** (couleurs, espacements, typos, rayons,
  dégradés, animations légères) au plus près : c'est l'objet de
  `13-ui-pixel-perfect-maquette.md`. Extraire les valeurs de style depuis la source du
  prototype (`docs/prototype/stats-mkworld-5poles.html`). Le prototype cadre à la fois
  *quoi* afficher, *où*, **et** *avec quel style*.
- Les **valeurs affichées dans la maquette sont des données de démo** (noms, scores, %) :
  ne pas les coder en dur ; elles illustrent seulement le type de donnée attendu.
- **Divergence prototype ↔ ticket** : si un ticket demande un écran/flux qui contredit le
  prototype, le **signaler** (comme pour un conflit rule ↔ ticket) au lieu de trancher
  seul.
- **Écarts visuels résiduels** (asset/police absents du projet, contrainte technique) :
  acceptables « dans la mesure du possible » — viser l'équivalent le plus proche et les
  **documenter** (cf. `13`).
- La justesse des calculs de stats reste prioritaire (`13`) ; le prototype ne préjuge
  pas des formules, seulement de leur présentation.

## Critère de validation (Definition of Done)

Un ticket qui touche un **écran décrit dans `docs/PROTOTYPE_UX.md`** n'est **validé**
(passé « Terminé » / fermé sur le board) que si **les trois** conditions sont réunies :

1. **Conformité structurelle** : l'écran comporte les sections/onglets/insights prévus par
   le prototype, dans le bon rattachement de pôle, avec les libellés FR attendus et la
   navigation décrite. Un écart (section/onglet/insight manquant, mauvaise destination)
   = **pas validé** → rester « À faire ».
2. **Fidélité visuelle (pixel-perfect dans la mesure du possible)** : le rendu reproduit
   le style de la maquette (couleurs, espacements, typographies, rayons, pastilles,
   états…) au niveau du rendu de référence validé (`WelcomeScreen`, cf.
   `13-ui-pixel-perfect-maquette.md`). Les seuls écarts admis sont ceux « dans la mesure
   du possible » (asset/police absents), **documentés**. Un rendu « fonctionnel mais loin
   de la maquette » = **pas validé**.
3. **Respect des règles composants** : réutiliser/adapter l'existant en priorité ;
   création/modification profonde autorisée quand nécessaire pour la fidélité ; ne pas
   régresser l'existant.

« Fonctionnellement présent » ne suffit pas. Au moment de valider un ticket (cf. étape de
validation de `/ticket-dev`), **vérifier explicitement la conformité structurelle ET la
fidélité visuelle écran par écran** et lister les écarts éventuels avant de proposer la
clôture.

**Exemption** : un ticket **purement technique / sans écran** (calculs, perfs, migration)
n'a pas de critère de conformité maquette — il est validé sur ses critères fonctionnels.
