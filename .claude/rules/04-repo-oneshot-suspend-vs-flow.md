# Repositories/data sources : `suspend` pour le one-shot, `Flow` pour les émissions multiples

**Portée** : toute fonction exposée par un `repository/` ou un `datasource/`
(local ou réseau).

Choisir le type de retour selon la **nature de la source**, pas par réflexe :

- **Opération one-shot** (une seule valeur produite puis terminée) → **`suspend fun`**
  renvoyant directement le résultat. Concerne : login/auth, lecture ponctuelle
  (`get`), écriture/suppression (`write`/`delete`/`setValue`/`removeValue`),
  action déclenchée par l'utilisateur. Ne pas emballer un one-shot dans un `Flow`
  (`callbackFlow`/`flowOf`) que l'appelant consommerait aussitôt via
  `firstOrNull()` : c'est du bruit.
- **Source qui émet dans le temps** (plusieurs valeurs, observation continue) →
  **`Flow`**. Concerne : observation temps réel Firebase
  (`listenToCurrentWar` via `ValueEventListener`), flux DataStore, lectures Room
  streaming, `SharedFlow`/`StateFlow` d'événements.

**Nuance avec `CLAUDE.md`** — la convention y est formulée « Toujours retourner
des `Flow` depuis repositories/data sources ; ne pas bloquer ». L'intention réelle
est **« ne pas bloquer »** : une `suspend fun` ne bloque pas le thread appelant
(elle suspend la coroutine). Cette rule **raffine** donc la convention sans la
contredire : le `Flow` n'est requis que pour les **émissions multiples** ; un
one-shot non bloquant se modélise par une `suspend fun`. Le dépôt applique déjà
ce raffinement (cf. `FirebaseRepository` : lectures `.get()`/écritures en
`suspend`, seul `listenToCurrentWar` reste en `Flow`).

**Exemple avant/après** :

```kotlin
// Avant (one-shot emballé dans un Flow, consommé par firstOrNull)
fun signInAnonymously(): Flow<Boolean> = callbackFlow {
    Firebase.auth.signInAnonymously()
        .addOnSuccessListener { trySend(true) }
        .addOnFailureListener { trySend(false) }
    awaitClose {}
}
// appelant : firebaseRepository.signInAnonymously().firstOrNull()

// Après (one-shot suspend, résultat direct)
suspend fun signInAnonymously(): Boolean = suspendCancellableCoroutine { cont ->
    Firebase.auth.signInAnonymously()
        .addOnSuccessListener { cont.resume(true) }
        .addOnFailureListener { cont.resume(false) }
}
// appelant : firebaseRepository.signInAnonymously()
```

**Pont vers les APIs Firebase basées sur `Task`** : la voie idiomatique est
`Task.await()` (dépendance `org.jetbrains.kotlinx:kotlinx-coroutines-play-services`,
import `kotlinx.coroutines.tasks.await`). Si cette dépendance n'est **pas
déclarée explicitement** dans le projet (ne pas s'appuyer sur une résolution
transitive fragile), utiliser `suspendCancellableCoroutine { cont -> … }` avec
`addOnSuccessListener`/`addOnFailureListener`, à l'image du helper existant
`Task<DataSnapshot>.awaitSnapshot()`.

Un accès purement **synchrone** (ex. `Firebase.auth.currentUser != null`) reste
une fonction **non-suspend** classique : ni `suspend`, ni `Flow`.
