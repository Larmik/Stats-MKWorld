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
- Écouteur ou lecture Firebase posé sur le nœud lu (`child("currentWars").child(id)`), jamais sur
  la racine : la racine transfère toute la base à chaque écriture. Id nul ou vide → pas d'appel
  (`child("")` vise le parent). Cf. audit P9, B40.
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

## UseCase ou repository

- `usecase/` = orchestration consommée par ≥ 2 appelants distincts. Une logique à un seul
  consommateur va dans le repository mono-source adapté, ou dans un repository dédié si elle agrège
  plusieurs sources.
- Exemple : outils debug (`diagnoseUnknownOpponents`, `reattributeOpponent`, `deleteWar`,
  `diagnoseMissingPlayers`, `addMissingPlayerAsAlly`, `findOfficialWarCandidates`,
  `migrateOfficialWars`) dans `repository/DiagnosticRepository.kt`, pas dans `FetchUseCase`.
- Extraction de helpers privés : cf. `kotlin/constantes-extensions.md`.
- Appels réseau par élément d'une collection : cf. `data/network.md`.
