# Affichage équipe vs roster : privilégier le roster dès qu'une distinction est possible

**Portée** : tout affichage UI (Compose) impliquant une **équipe** MKCentral et
ses **rosters** mkworld (war en cours, résumé/détail de war, cellules, sélection
d'adversaire, line-up, statistiques…).

Une équipe (`MKCTeam`, teamId) peut regrouper plusieurs **rosters**
(`MKCTeamRoster`, rosterId). Les wars sont rattachées au **rosterId** (hôte comme
adversaire). Règle : **dès qu'une distinction équipe/roster est possible, afficher
les infos du ROSTER**, pas de l'équipe principale.

- **Nom et tag** → ceux du **roster** (`MKCTeamRoster.name`/`.tag`, ou `RosterInfo`),
  pas de l'équipe (`MKCTeam.name`/`.tag`).
- **Avatar / logo** → celui de l'**équipe parente** (un roster n'a pas de logo
  propre). Donc : avatar = équipe, nom + tag = roster.
- Roster non identifiable (pas de roster mkworld résolvable, war legacy) →
  **retomber** sur le nom/tag de l'équipe (`roster?.name ?: team.name`).
- Nom de war, en-têtes de line-up, preview d'adversaire, libellés de stats suivent
  la même règle.

**Ne jamais faire disparaître silencieusement un adversaire non résolu — dégrader
plutôt qu'effacer.** Quand l'id (rosterId ou teamId legacy) ne résout AUCUNE
`TeamEntity` locale (équipe/roster disparu du cache, war jamais synchronisée), il
est **interdit** de le supprimer de la liste (pas de `mapNotNull` qui « avale »
l'entrée). **Retomber** sur une `TeamEntity` dégradée conservant l'id (pour
l'appariement score/pénalité), nom `« Équipe inconnue »`, tag `« ??? »`,
`logo = null`. Cf. `War.opponentTeams` dans `extension/WarExtension.kt` :
`getTeam(id)?.let { … } ?: TeamEntity(id = id, name = "Équipe inconnue", tag = "???", color = null, logo = null)`.

Corollaire **écriture/migration** : ne jamais réécrire un identifiant (ex.
migration teamId→rosterId) non résolvable à l'affichage — vérifier
`getTeam(nouvelId) != null` avant de remplacer.

Côté données : conserver le rosterId comme id d'appariement (score/pénalité) et
résoudre le nom/tag du roster via `TeamEntity.rosters : List<RosterInfo>`, sans
appel réseau supplémentaire.
