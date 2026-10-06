---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/datasource/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/usecase/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/worker/**"
---

# Repositories, data sources, UseCases

## Injection

- Chaque repository / data source / UseCase = une **interface** + une impl `@Inject constructor` +
  un `@Module @InstallIn(SingletonComponent::class)` qui `@Binds` l'impl en `@Singleton`. Suivre
  ce patron pour toute nouvelle dépendance (cf. `FirebaseRepository`).

## `suspend` pour le one-shot, `Flow` pour les émissions multiples

- One-shot (auth, `get`, écriture, suppression, action) → `suspend fun` renvoyant le résultat. Ne
  pas emballer un one-shot dans `callbackFlow` / `flowOf` consommé par `firstOrNull()`.
- Source qui émet dans le temps (`listenToCurrentWar`, DataStore, Room streaming, événements) →
  `Flow`.
- Accès synchrone (`Firebase.auth.currentUser != null`) → ni `suspend` ni `Flow`.
- Pont `Task` Firebase : `kotlinx-coroutines-play-services` n'est pas déclaré → utiliser
  `suspendCancellableCoroutine` + `addOnSuccessListener` / `addOnFailureListener` (cf.
  `Task<DataSnapshot>.awaitSnapshot()`).

## Couche données sans UI

- Pas d'`Activity`, de launcher de permission ni de `currentActivity` dans un repository :
  l'interaction système se fait dans l'UI, le repository n'expose que l'état. Cf. audit B28.

## Écriture destructive

- `clear*()` vide toute la table : jamais dans une méthode qui traite un élément et qu'on appelle
  en boucle. Un seul clear avant la boucle, ou purge + réécriture en une passe (cf. `fetchTeams`).
  Cf. audit B27.
- `tags/` (RTDB, `setValue` intégral via `FetchUseCase.fetchTags`) n'est réécrit que par l'action
  manuelle Debug, jamais depuis une synchro (`fetchData`, `fetchTeams`, workers). Cf. #164.

## Réseau par élément d'une collection

- Parallèle (`coroutineScope { items.map { async { … } }.awaitAll() }`) seulement si l'API tient la
  rafale et pour un petit volume à la demande (`TeamProfileViewModel.resolveMembers`,
  `AddWarViewModel.resolvePlayerAvatars`).
- Séquentiel (ou lots de 3-4) dès que l'API throttle : symptôme = `successResponse == null` sans
  exception sur une partie des éléments. MKCentral throttle en synchro (#50 : `FetchUseCase.fetchTeam`).
- Chaque élément est tolérant à l'échec (`runCatching { … }.getOrNull()`) : il dégrade, les autres
  sont écrits.
- Peupler au fetch un champ persistant (ex. `PlayerEntity.avatar`) dès qu'un endpoint le fournit ;
  vérifier la réponse live avant de conclure qu'un endpoint ne l'a pas (`registry/teams/{id}` ne
  porte pas l'avatar des membres, `registry/players/{id}` si).
- Peupler tous les éléments d'un listing ou aucun (cf. `ui/roster-player-display.md`).

## UseCase ou repository

- `usecase/` = orchestration consommée par ≥ 2 appelants distincts. Une logique à un seul
  consommateur va dans le repository mono-source adapté, ou dans un repository dédié si elle agrège
  plusieurs sources.
- Exemple : outils debug (`diagnoseUnknownOpponents`, `reattributeOpponent`, `deleteWar`,
  `diagnoseMissingPlayers`, `addMissingPlayerAsAlly`, `findOfficialWarCandidates`,
  `migrateOfficialWars`) dans `repository/DiagnosticRepository.kt`, pas dans `FetchUseCase`.
- Extraction de helpers privés : cf. `kotlin/constantes-extensions.md`.
