# Placement UseCase ↔ Repository : une logique mono-consommateur ne va pas dans un UseCase partagé

**Portée** : ajout d'une méthode d'orchestration/logique métier dans un `usecase/`
(ex. `FetchUseCase`) ou un `repository/`.

Un `usecase/` modélise une **orchestration réutilisée** : justifié seulement si
consommé par **≥ 2 appelants distincts** (plusieurs ViewModels/Workers). Une logique
appelée par **un seul** consommateur **ne doit pas** vivre dans le UseCase partagé.

Où la placer :

- Si un **repository mono-source** approprié existe (Firebase, Room, DataStore,
  réseau…) → y placer la méthode.
- Si la logique **agrège plusieurs sources** sans repository naturel → **créer un
  repository dédié** (interface + objet `@Module @InstallIn(SingletonComponent::class)`
  qui `@Binds` l'impl `@Singleton`, cf. `FirebaseRepository`), plutôt qu'étendre le
  UseCase. Le repository dédié injecte les sources et porte la logique + helpers privés.

Généralise au couple UseCase↔Repository la règle « pas d'extraction pour un seul
appelant » (`30-repositories.md`, `61-no-single-use-constant.md`).

**Exemple appliqué** : les outils de diagnostic debug (`diagnoseUnknownOpponents`,
`reattributeOpponent`, `deleteWar`, `diagnoseMissingPlayers`, `addMissingPlayerAsAlly`
+ helpers privés `fetchAllMkworldTeams`, `resolveOpponentId`, `mkworldCandidate`,
table `opponentOverrides`), consommés uniquement par `DebugViewModel`, ont été
**déplacés de `FetchUseCase` vers un `DiagnosticRepository` dédié** (interface +
module `@Binds @Singleton` injectant `FirebaseRepositoryInterface`,
`MKCentralDataSourceInterface`, `DatabaseRepositoryInterface`,
`DataStoreRepositoryInterface`). `FetchUseCase` ne garde que la synchro réellement
partagée (`fetchData`/`fetchTeams`/`fetchWars`/`fetchTags`/`manageTransferts`/
`migrateOpponentsToRoster`).
