---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/api/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/datasource/network/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/usecase/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/DiagnosticRepository.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/TournamentRepository.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/extension/MKCTeamExtension.kt"
---

# Domaine mkworld uniquement

- Ne jamais lire, récupérer ni stocker une équipe / roster d'un autre jeu (`game=mk8dx`, tout
  `game != "mkworld"`). Ne pas réintroduire `getMK8Teams` ni `getAllTeams` (supprimés).
- L'unique endpoint liste `MKCentralApi.getTeams` (synchro et diagnostic) fige dans l'URL
  `game=mkworld&mode=150cc&is_historical=false&is_active=true&min_player_count=6`.
- Côté modèle, filtrer les rosters avec `MKCTeam.mkWorldRosters()` (`extension/MKCTeamExtension.kt`),
  pas un nouveau `filter { it.game == "mkworld" }` (audit D28).
- Cache local (Room `TeamEntity`, DataStore) : uniquement des équipes mkworld ; garder le filtre
  `TeamEntity.rosters.isNotEmpty()` et l'équipe spéciale « 6v6 Squad ».
- Tournois officiels (#152) : les tournois mkworld d'une série d'origine mk8dx (Frontier
  `series_id=12`, EuroLeague `series_id=27`) sont permis, saison `game=mkworld` seule
  (`tournaments/list?game=mkworld&series_id=…` puis `tournaments/{id}`). Ne jamais lire
  `tournaments/series/{id}` d'une série mk8dx.
- Conséquence voulue : dans `DiagnosticRepository.diagnoseUnknownOpponents`, un id mk8dx pur non
  couvert par `opponentOverrides` tombe en `NotFound` (override manuel ou suppression de war).
