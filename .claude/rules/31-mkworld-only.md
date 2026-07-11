# Domaine exclusivement mkworld : aucun accès à un autre jeu (mk8dx)

**Portée** : tout accès aux données équipes/rosters MKCentral — `api/MKCentralApi.kt`,
`datasource/network/MKCentralDataSource.kt`, la synchro `usecase/FetchUseCase.kt`,
et **tout futur accès** aux équipes/rosters (endpoint, requête, cache local Room,
DataStore).

Le domaine métier de l'application est **exclusivement mkworld**. On ne doit
**jamais** accéder à, récupérer, ni stocker une équipe ou un roster d'un autre
jeu (`game=mk8dx` ou tout `game != "mkworld"`).

Interdictions fermes :

- **Ne pas ajouter/réintroduire** un endpoint ou un filtre ciblant `game=mk8dx`
  (ou tout jeu ≠ mkworld). L'endpoint historique `MKCentralApi.getMK8Teams`
  (`registry/teams?game=mk8dx…`) a été **supprimé** et ne doit **pas** revenir,
  pas plus que sa méthode de data source ou ses appels dans `FetchUseCase`.
- **Ne jamais persister** en cache local (Room `TeamEntity`, DataStore) une
  équipe/roster non-mkworld. `fetchTeams()` ne récupère et n'écrit que les
  équipes mkworld ; on conserve le filtre `TeamEntity.rosters.isNotEmpty()`
  (rosters mkworld) et l'équipe spéciale « 6v6 Squad ».
- **Toute récupération d'équipes filtre `game=mkworld`.** L'unique endpoint liste
  `getTeams` (utilisé par la synchro registre ET le diagnostic) fige
  `game=mkworld` côté URL, avec le filtre par défaut du site MKCentral
  (`is_active=true&is_historical=false&min_player_count=6` — équipes actives, non
  historiques, ≥ 6 joueurs) ; côté modèle, tout balayage de `rosters` filtre
  `it.game == "mkworld"`. (L'ancien endpoint `getAllTeams`, dédoublonné après
  convergence du filtre, a été supprimé — ne pas le réintroduire.)

Conséquence assumée pour le **diagnostic des adversaires « Équipe inconnue »**
(`FetchUseCase.diagnoseUnknownOpponents`) : un id d'adversaire d'origine mk8dx
pure, non couvert par la table d'override manuel `opponentOverrides`, tombe en
`NotFound` — c'est **voulu** (ces cas relèvent soit de l'override manuel, soit
d'une suppression de la war). L'override manuel et l'heuristique nom/tag ne
s'appuient que sur la liste mkworld.
