---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/datasource/network/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/usecase/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/worker/**"
  - "app/src/main/java/**/*ViewModel.kt"
---

# Réseau par élément d'une collection

- Parallèle (`coroutineScope { items.map { async { … } }.awaitAll() }`) seulement si l'API tient la
  rafale et pour un petit volume à la demande (`TeamProfileViewModel.resolveMembers`,
  `AddWarViewModel.resolvePlayerAvatars`).
- Séquentiel (ou lots de 3-4) dès que l'API throttle : symptôme = `successResponse == null` sans
  exception sur une partie des éléments. MKCentral throttle en synchro (#50 : `FetchUseCase.fetchTeam`).
- Chaque élément est tolérant à l'échec (`runCatching { … }.getOrNull()`) : il dégrade, les autres
  sont écrits.
- Peupler au fetch un champ persistant (ex. `PlayerEntity.avatar`) dès qu'un endpoint le fournit ;
  vérifier la réponse live avant de conclure qu'un endpoint ne l'a pas (`registry/teams/{id}` ne
  porte pas l'avatar des membres, `registry/players/{id}` si).
- Peupler tous les éléments d'un listing ou aucun (cf. `ui/roster-player-display.md`).
