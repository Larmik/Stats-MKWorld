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

## Médaillon joueur : photo si dispo, initiales sinon — ET cohérence intra-listing

**Portée** : tout affichage d'un **joueur** avec son « médaillon » (pastille
d'avatar). Composant unique `ui/cells/PlayerMedallion.kt` (rule 16 — ne pas
redupliquer une pastille d'initiales locale).

- **Photo si disponible, initiales sinon.** Afficher la photo de profil MKCentral
  (`PlayerEntity.avatar` / `userSettings.avatar`, préfixée `https://mkcentral.com`)
  quand elle existe ; sinon (null) **initiales** sur pastille colorée. Pendant le
  chargement async (Coil) : **initiales en fallback** (photo dessinée au-dessus de
  la couche d'initiales → elles transparaissent tant que rien n'est chargé et en
  cas d'échec). Pas de placeholder gris/vide.
- **Cohérence intra-listing (impératif).** Dans **un même listing de joueurs**,
  tous les joueurs sont traités **à l'identique** : **aucun cas spécial** (surtout
  pas pour le joueur courant). Si la photo n'est pas disponible pour les autres
  lignes (→ initiales), le joueur courant affiche **aussi** ses initiales dans ce
  listing. Corollaire côté données : ne pas enrichir l'avatar d'un seul joueur (ex.
  le courant depuis le DataStore) au fetch alors que les autres restent sans photo
  — soit on peuple l'avatar de **tous** les joueurs du listing (cf. rule 30 : fetch
  en parallèle), soit **aucun**. Ne **jamais** régresser un écran où **tous** les
  joueurs ont une photo (résolution réseau dédiée type `AddWar`/`TeamProfile`).
- Deux sous-listes visuellement distinctes (ex. section **Membres** vs section
  **Alliés** des Classements) sont des listings séparés : chacune doit être
  cohérente en interne, elles peuvent différer entre elles.
