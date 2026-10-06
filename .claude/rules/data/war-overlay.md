---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/FirebaseRepository.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/usecase/FetchUseCase.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/ScoringConstants.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/extension/IntegerExtension.kt"
---

# Contrat RTDB avec l'overlay OBS WarOverlay

WarOverlay (dépôt `Larmik/WarOverlay`, Firebase Hosting `stats-mkworld-overlay`) est une page
statique ajoutée comme source navigateur dans OBS. Elle lit la RTDB `stats-mkworld` en lecture
seule, sans authentification, sans build ni tests : un changement de format côté app la casse
sans aucune erreur visible dans l'app.

## Ce que l'overlay lit

- `currentWars/{rosterId}`, écouté en temps réel (`onValue`). Le `rosterId` figure dans l'URL
  configurée par chaque streamer dans OBS : la clé du nœud doit rester stable.
- Champs lus : `teamHost`, `teamOpponent` (tableau de `rosterId`), `tracks[].positions[].position`,
  `penalties[].teamId`, `penalties[].amount`.
- La suppression du nœud (`deleteCurrentWar`) est reçue par l'overlay (fin de war).
- `tags/`, lu une fois au chargement : liste `[{tag, teamId}]` (`Tag`, écrite par `writeTags`
  depuis `FetchUseCase`). L'overlay y cherche le tag de l'hôte et des adversaires.
- `tags/` n'est pas une donnée interne : l'app ne le lit jamais, l'overlay est son seul
  consommateur. Son format est un contrat public.
- Barème 12 joueurs réimplémenté en JS : `positionToPoints(is24p = false)`,
  `MAX_POINTS_PER_TRACK_12P = 82`, 12 manches, victoire assurée = 40 × manches restantes.
  Le mode 24 joueurs n'est pas géré par l'overlay.

## Obligations

- Tout changement touchant ces nœuds, ces champs (renommage, type, suppression), la clé de
  `currentWars/` ou le barème 12p est **signalé** dans le résumé ou la description de PR.
- Il donne aussi lieu à une issue sur l'overlay, décrivant le nouveau format et l'adaptation
  attendue : `gh issue create -R Larmik/WarOverlay`, ajoutée au board « Stats MKWorld ».
- Préférer le rétrocompatible : ajouter un champ plutôt que renommer ou retyper un champ lu.
- Changement cassant inévitable → coordonner l'ordre de release de l'app et de déploiement de
  l'overlay, et l'écrire dans l'issue.
- Ajouter un champ non lu par l'overlay (ex. `scores`, `shocks`) ne demande rien de plus.
- Ce fichier décrit le contrat actuel : toute évolution du contrat le met à jour dans la même PR.
