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
