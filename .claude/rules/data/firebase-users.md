---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/FirebaseRepository.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/DiagnosticRepository.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/usecase/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/User.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/database/entities/PlayerEntity.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/teamProfile/TeamProfileViewModel.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/playerProfile/PlayerProfileViewModel.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/addWar/AddWarViewModel.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/debug/**"
---

# Membres, alliés et transferts (Firebase `users` / `newAllies`)

Détail technique : `docs/TECHNICAL.md` (tables Firebase, § synchro `FetchUseCase`).

## Rôles

- Un **membre** vit dans `users/{teamId}` et porte son rôle (Leader 2 / Admin 1 / Membre 0).
  Toute préservation du rôle vise les membres : `writeUser`, `updateUserCurrentWar` (qui ne
  touche que `currentWar` si le membre existe).
- Un **allié** (`rosterId == "-1"`, nœud `newAllies/{teamId}`) a **toujours** `role = 0`. Les
  écritures d'allié à 0 sont correctes : `writeAlly` avec `User(player)` à l'ajout,
  `updateAllyCurrentWar` (fallback `setValue`).

## Transferts (`FetchUseCase.manageTransferts`, lancé depuis l'écran Debug)

- Un membre qui quitte l'équipe MKCentral **devient allié** : il n'est jamais supprimé (ses wars
  et stats en dépendent).
- Un allié devenu membre rejoint le roster avec `role = 0`.
- Ce comportement est voulu. Problème connu : la condition « membre » matche tous les joueurs
  déjà dans le roster, donc relancer la fonction remet à 0 le rôle d'un leader / admin existant
  (audit B39, basse priorité).
