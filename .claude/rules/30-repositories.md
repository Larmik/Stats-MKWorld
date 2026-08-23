# Repositories/data sources : `suspend` pour le one-shot, `Flow` pour les émissions multiples

**Portée** : toute fonction exposée par un `repository/` ou `datasource/` (local
ou réseau).

Choisir le type de retour selon la **nature de la source** :

- **One-shot** (une seule valeur puis terminé) → **`suspend fun`** renvoyant
  directement le résultat. Concerne : login/auth, lecture ponctuelle (`get`),
  écriture/suppression (`write`/`delete`/`setValue`/`removeValue`), action
  utilisateur. **Ne pas emballer un one-shot dans un `Flow`** (`callbackFlow`/
  `flowOf`) consommé aussitôt via `firstOrNull()`.
- **Source qui émet dans le temps** (observation continue) → **`Flow`**. Concerne :
  observation temps réel Firebase (`listenToCurrentWar` via `ValueEventListener`),
  flux DataStore, lectures Room streaming, `SharedFlow`/`StateFlow` d'événements.

**Nuance avec `CLAUDE.md`** (« Toujours retourner des `Flow` … ; ne pas bloquer ») :
l'intention réelle est **« ne pas bloquer »**. Une `suspend fun` ne bloque pas le
thread (elle suspend la coroutine). Le `Flow` n'est requis que pour les **émissions
multiples**. Le dépôt applique déjà ce raffinement (cf. `FirebaseRepository` :
lectures `.get()`/écritures en `suspend`, seul `listenToCurrentWar` en `Flow`).

```kotlin
// Avant (one-shot emballé dans un Flow, consommé par firstOrNull)
fun signInAnonymously(): Flow<Boolean> = callbackFlow {
    Firebase.auth.signInAnonymously()
        .addOnSuccessListener { trySend(true) }
        .addOnFailureListener { trySend(false) }
    awaitClose {}
}
// Après (one-shot suspend, résultat direct)
suspend fun signInAnonymously(): Boolean = suspendCancellableCoroutine { cont ->
    Firebase.auth.signInAnonymously()
        .addOnSuccessListener { cont.resume(true) }
        .addOnFailureListener { cont.resume(false) }
}
```

**Pont vers les APIs Firebase basées sur `Task`** : voie idiomatique `Task.await()`
(dépendance `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`, import
`kotlinx.coroutines.tasks.await`). Si cette dépendance n'est **pas déclarée
explicitement** (ne pas s'appuyer sur une résolution transitive fragile), utiliser
`suspendCancellableCoroutine { cont -> … }` avec `addOnSuccessListener`/
`addOnFailureListener`, à l'image du helper `Task<DataSnapshot>.awaitSnapshot()`.

Un accès purement **synchrone** (ex. `Firebase.auth.currentUser != null`) reste
**non-suspend** : ni `suspend`, ni `Flow`.

## Ne pas extraire de fonction privée pour une logique à un seul appelant

Dans un `repository/`/`datasource/`, **ne pas extraire** de helper privé pour une
logique **appelée une seule fois** : l'**inliner**. N'extraire que si **réellement
réutilisé (≥ 2 appelants distincts)** ou si l'extraction clarifie nettement un bloc
long/complexe. Un one-liner trivial (ex. `dataStoreRepository.mkcPlayer
.firstOrNull()?.id ?: 0L`) ne justifie pas un helper même appelé deux fois.

## Résolution réseau par élément d'une collection : parallèle SI l'API tient la charge, sinon séquentiel

**Portée** : synchro/repository/UseCase (`FetchUseCase`, VM) devant récupérer une
donnée réseau **pour chaque élément** d'une liste (ex. avatar de chaque membre via
`getPlayer(id)`).

Deux modes selon la **tolérance de l'API aux rafales** :

- **Parallèle** (latence minimale) tant que l'API **ne throttle pas** les rafales :
  `coroutineScope { items.map { async { fetch(it) } }.awaitAll() }`. Adapté à un petit
  volume affiché à la demande (ex. `TeamProfileViewModel.resolveMembers`,
  `AddWarViewModel.resolvePlayerAvatars`).
- **Séquentiel** (ou petits lots bornés, 3-4 max) dès que l'API **throttle** une rafale
  d'appels simultanés au même host — symptôme : réponses `successResponse == null`
  **sans exception** sur une partie des éléments alors que le même appel isolé réussit.
  Cf. **#50** : la résolution des avatars membres dans `FetchUseCase.fetchTeam`, d'abord
  en rafale parallèle, ne peuplait **aucun** avatar (throttle MKCentral) ; repassée en
  **séquentiel** (`forEach`, aligné sur `fetchAllies` qui, lui, marchait), elle les
  peuple correctement. En arrière-plan (synchro `UpdateDataWorker`), la latence
  séquentielle est acceptable.

Dans les deux modes :

- **Tolérance aux échecs par élément** (`runCatching { … }.getOrNull()`) : un élément en
  échec ne doit jamais faire échouer les autres (en parallèle, `awaitAll()` est fail-fast :
  une exception annule tout le lot ; en séquentiel, elle interromprait la boucle). L'élément
  en échec dégrade (valeur nulle → repli d'affichage), les autres sont écrits.
- **Enrichir au fetch si l'API le permet.** Puisqu'un champ persistant existe (ex.
  `PlayerEntity.avatar`, Room), le peupler au fetch **dès qu'un endpoint le fournit** —
  c'est le but d'avoir migré le schéma. Ne renoncer (et documenter) **que** si le coût est
  réellement prohibitif (données absentes de tous les endpoints, volume ingérable).
- **Vérifier la source réelle avant de conclure.** Avant d'affirmer qu'un endpoint ne
  fournit pas un champ, **inspecter la réponse live** (l'endpoint *détail* peut porter des
  champs absents de l'endpoint *liste*, et inversement). Cf. #50 : `registry/teams/{id}`
  ne porte PAS l'avatar des membres (vérifié), seul `registry/players/{id}` le fait.
- Cohérence d'affichage : peupler **tous** les éléments d'un même listing ou **aucun**
  (cf. rule 12, cohérence intra-listing) — pas d'enrichissement d'un seul élément privilégié.
