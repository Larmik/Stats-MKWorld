# Domaine exclusivement mkworld : aucun accès à un autre jeu (mk8dx)

**Portée** : tout accès aux données équipes/rosters MKCentral —
`api/MKCentralApi.kt`, `datasource/network/MKCentralDataSource.kt`,
`usecase/FetchUseCase.kt`, et **tout futur accès** (endpoint, requête, cache Room,
DataStore).

Le domaine métier est **exclusivement mkworld**. Ne **jamais** accéder à, récupérer,
ni stocker une équipe/roster d'un autre jeu (`game=mk8dx` ou tout `game != "mkworld"`).

Interdictions fermes :

- **Ne pas ajouter/réintroduire** un endpoint ou filtre ciblant `game=mk8dx` (ou
  tout jeu ≠ mkworld). L'endpoint historique `MKCentralApi.getMK8Teams`
  (`registry/teams?game=mk8dx…`) a été **supprimé** et ne doit **pas** revenir, ni
  sa méthode de data source, ni ses appels dans `FetchUseCase`.
- **Ne jamais persister** en cache local (Room `TeamEntity`, DataStore) une
  équipe/roster non-mkworld. `fetchTeams()` ne récupère/écrit que les équipes
  mkworld ; conserver le filtre `TeamEntity.rosters.isNotEmpty()` (rosters mkworld)
  et l'équipe spéciale « 6v6 Squad ».
- **Toute récupération d'équipes filtre `game=mkworld`.** L'unique endpoint liste
  `getTeams` (synchro registre ET diagnostic) fige `game=mkworld` côté URL, avec le
  filtre par défaut MKCentral
  (`is_active=true&is_historical=false&min_player_count=6` — actives, non
  historiques, ≥ 6 joueurs) ; côté modèle, tout balayage de `rosters` filtre
  `it.game == "mkworld"`. (L'ancien endpoint `getAllTeams` a été supprimé — ne pas
  le réintroduire.)

Conséquence assumée pour le diagnostic `FetchUseCase.diagnoseUnknownOpponents` : un
id d'adversaire mk8dx pur, non couvert par la table d'override manuel
`opponentOverrides`, tombe en `NotFound` — c'est **voulu** (relève de l'override
manuel ou d'une suppression de war). L'override manuel et l'heuristique nom/tag ne
s'appuient que sur la liste mkworld.
