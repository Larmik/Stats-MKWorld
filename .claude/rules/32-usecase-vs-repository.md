# Placement UseCase ↔ Repository : une logique mono-consommateur ne va pas dans un UseCase partagé

**Portée** : ajout d'une méthode d'orchestration/logique métier dans un
`usecase/` (ex. `FetchUseCase`) ou un `repository/`. Concerne le choix de la
couche où placer une nouvelle fonction.

Un `usecase/` modélise une **orchestration réutilisée** : il ne se justifie que
si sa logique est consommée par **≥ 2 appelants distincts** (plusieurs
ViewModels/Workers). Une logique ajoutée à un UseCase mais appelée par **un seul**
consommateur (typiquement un unique ViewModel) **ne doit pas** vivre dans le
UseCase partagé : elle l'alourdit sans bénéfice de réutilisation.

Où la placer alors :

- Si un **repository mono-source** approprié existe (Firebase, Room, DataStore,
  réseau…), y placer la méthode.
- Si la logique **agrège plusieurs sources** (orchestration multi-repositories /
  data sources) sans repository naturel, **créer un repository dédié** — interface
  + objet `@Module @InstallIn(SingletonComponent::class)` qui `@Binds`
  l'implémentation `@Singleton` (patron DI habituel du projet, cf.
  `FirebaseRepository`) — plutôt que d'étendre le UseCase. Le repository dédié
  injecte les sources nécessaires et porte la logique + ses helpers privés.

Ce principe **généralise au couple UseCase↔Repository** la règle « pas
d'extraction pour un seul appelant » déjà posée pour les fonctions privées
(`30-repositories.md`) et les constantes (`61-no-single-use-constant.md`) : on
n'introduit pas une indirection (méthode de UseCase partagé) pour un usage unique.

**Exemple concret (appliqué)** : les outils de **diagnostic debug** —
`diagnoseUnknownOpponents`, `reattributeOpponent`, `deleteWar`,
`diagnoseMissingPlayers`, `addMissingPlayerAsAlly` et leurs helpers privés
(`fetchAllMkworldTeams`, `resolveOpponentId`, `mkworldCandidate`, table
`opponentOverrides`) — n'étaient consommés que par `DebugViewModel`. Ils ont été
**déplacés de `FetchUseCase` vers un `DiagnosticRepository` dédié** (interface +
module `@Binds @Singleton`, injectant `FirebaseRepositoryInterface`,
`MKCentralDataSourceInterface`, `DatabaseRepositoryInterface`,
`DataStoreRepositoryInterface`). `FetchUseCase` ne conserve que l'orchestration de
synchro réellement partagée (`fetchData`/`fetchTeams`/`fetchWars`/`fetchTags`/
`manageTransferts`/`migrateOpponentsToRoster`).
