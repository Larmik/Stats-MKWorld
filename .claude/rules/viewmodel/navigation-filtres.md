---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/RootScreen.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/home/HomeScreen.kt"
  - "app/src/main/java/**/*ViewModel.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/local/WarKindFilter.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/local/SeasonFilter.kt"
---

# Filtres d'écran propagés à tous les écrans enfants

Filtres concernés : **saison**, **Amicaux / Officiels** (`WarKindFilter`, #103) et **mode 12p**.

- Un écran qui ouvre un écran enfant (fiche, classement, « voir tout ») lui transmet ses filtres
  actifs par la route : segment `{season}` (`toSeasonNumber()`, `"all"` si aucun) et segment
  `{kind}` (`WarKindFilter.routeSegment` / `fromRouteSegment`).
- Ajouter un filtre ou un segment de route = l'ajouter à **toutes** les routes enfants qui
  affichent des données filtrées, pas seulement à une partie. Cf. audit B36.
- La `key` du `hiltViewModel` inclut chaque segment de filtre, sinon un VM en cache garde
  l'ancien filtre.
- Le VM enfant reçoit le filtre comme valeur initiale (Factory) ; un sélecteur local le fait
  ensuite évoluer en état interne (cf. `ui/compose.md`).
- Un écran du graphe racine ouvert sans parent filtré repart du filtre par défaut.
- Le filtre 12p (`teamOpponent.size == 1`) s'applique de la même façon au parent et à ses enfants.
