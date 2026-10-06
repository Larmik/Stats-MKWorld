---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/stats/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/extension/WarExtension.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/database/entities/TeamEntity.kt"
---

# Affichage roster vs équipe, adversaire inconnu, médaillon joueur

## Roster plutôt qu'équipe

Les wars sont rattachées au rosterId (hôte et adversaire). Dès que la distinction est possible :

- nom et tag = ceux du **roster** (`MKCTeamRoster`, `RosterInfo` de `TeamEntity.rosters`) ;
- avatar / logo = celui de l'**équipe parente** (un roster n'a pas de logo) ;
- roster non identifiable (war legacy) → nom/tag de l'équipe (`roster?.name ?: team.name`) ;
- vaut pour nom de war, en-têtes de line-up, preview d'adversaire, libellés de stats ;
- résoudre via `TeamEntity.rosters`, sans appel réseau ; garder le rosterId comme id d'appariement
  score/pénalité.

## Adversaire non résolu : dégrader, jamais effacer

- Un id qui ne résout aucune `TeamEntity` locale reste dans la liste : pas de `mapNotNull` sur une
  résolution.
- Repli = `TeamEntity.unknown(id)` (`database/entities/TeamEntity.kt`), unique fabrique : ne pas
  recopier `"Équipe inconnue"` / `"???"`. Cf. `War.opponentTeams` (`extension/WarExtension.kt`).
- Écriture / migration : ne jamais remplacer un identifiant par un id non résolvable (vérifier
  `getTeam(nouvelId) != null` avant).

## Médaillon joueur

- Composant unique `PlayerMedallion` (`ui/cells/PlayerMedallion.kt`) : pas de pastille d'initiales
  locale.
- Photo MKCentral si disponible (`PlayerEntity.avatar`, préfixe via `String.mkcentralUrl`),
  initiales sinon. Les initiales restent dessinées sous la photo pendant le chargement et en cas
  d'échec : pas de placeholder gris.
- Dans un même listing, tous les joueurs sont traités à l'identique, joueur courant compris : si
  les autres n'ont pas de photo, il affiche aussi ses initiales.
- Ne jamais régresser un écran où tous les joueurs ont une photo (`AddWar`, `TeamProfile`).
- Deux sections visuellement distinctes (Membres / Alliés) sont deux listings séparés.
- Côté données : peupler l'avatar de tous les joueurs d'un listing ou d'aucun (cf.
  `data/repositories.md`).
