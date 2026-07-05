# Affichage équipe vs roster : privilégier le roster dès qu'une distinction est possible

**Portée** : tout affichage UI (Compose) impliquant une **équipe** MKCentral et
ses **rosters** mkworld (war en cours, résumé/détail de war, cellules de war,
sélection d'adversaire, line-up, statistiques…).

Une équipe (`MKCTeam`, teamId) peut regrouper plusieurs **rosters**
(`MKCTeamRoster`, rosterId). Les wars sont rattachées au **rosterId** (hôte comme
adversaire). Règle : **dès qu'une distinction est possible entre l'équipe et l'un
de ses rosters, afficher les informations du ROSTER**, pas celles de l'équipe
principale.

Concrètement :

- **Nom et tag** → ceux du **roster** (ex. `MKCTeamRoster.name` / `.tag`, ou
  `RosterInfo`), pas ceux de l'équipe (`MKCTeam.name` / `.tag`).
- **Avatar / logo** → reste celui de l'**équipe parente** (un roster n'a pas de
  logo propre). Donc : avatar = équipe, nom + tag = roster.
- Si le roster n'est pas identifiable (équipe sans roster mkworld résolvable,
  war legacy), **retomber** sur le nom/tag de l'équipe (`roster?.name ?: team.name`).
- Le nom de war, les en-têtes de line-up, la preview d'adversaire, les libellés
  de statistiques suivent la même règle.

**Ne jamais faire disparaître silencieusement un adversaire non résolu —
dégrader plutôt qu'effacer.** Quand l'id (rosterId ou teamId legacy) ne se
résout à AUCUNE `TeamEntity` locale (équipe/roster disparu du cache, war jamais
synchronisée), il est **interdit** de le supprimer de la liste (pas de
`mapNotNull` qui « avale » l'entrée) : nom + logo disparaîtraient et l'adversaire
s'évanouirait de la cellule/du détail. **Retomber** sur une `TeamEntity` dégradée
conservant l'id (pour l'appariement score/pénalité) avec un libellé explicite —
nom `« Équipe inconnue »`, tag `« ??? »`, `logo = null` (logo vide assumé, une
équipe disparue n'a pas de logo local). Cf. `War.opponentTeams` dans
`extension/WarExtension.kt` : `getTeam(id)?.let { … } ?: TeamEntity(id = id,
name = "Équipe inconnue", tag = "???", color = null, logo = null)`.

Corollaire côté **écriture/migration** : ne jamais réécrire dans les données un
identifiant (ex. migration teamId→rosterId) qui ne serait pas résolvable à
l'affichage. Vérifier `getTeam(nouvelId) != null` avant de remplacer.

Côté données, ne pas perdre le lien vers l'équipe parente : conserver le rosterId
comme identifiant d'appariement (score/pénalité) et résoudre le nom/tag du roster
via les métadonnées locales (`TeamEntity.rosters : List<RosterInfo>`), sans appel
réseau supplémentaire.
