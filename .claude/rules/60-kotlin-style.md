# Préférer `obj?.let { … }` à `if (obj == null) return`

**Portée** : tout code Kotlin du projet manipulant une valeur **nullable** dont on
ne veut rien faire quand elle vaut `null`.

Ne pas garder une valeur nullable avec un early-return ; utiliser `?.let` (exécute
le bloc seulement si non-null, `it` déballé avec smart-cast) :

```kotlin
// Interdit
fun handle(war: War?) {
    if (war == null) return
    doSomething(war)
}
// Attendu
fun handle(war: War?) {
    war?.let { doSomething(it) }
}
```

- Vaut pour **toutes les couches** (idiome transverse).
- **Fusionner les gardes suivantes dans le bloc** plutôt qu'enchaîner des
  `if (…) return` : combiner les conditions dans un seul `if` (ou `takeIf`). Cf.
  `FirebaseRepository.restoreCurrentWarIfHost` :

  ```kotlin
  override suspend fun restoreCurrentWarIfHost(war: War?) {
      war?.let {
          val hasLocalWar = dataStoreRepository.war.firstOrNull() != null
          val playerId = dataStoreRepository.mkcPlayer.firstOrNull()?.id ?: 0L
          if (!hasLocalWar && playerId != 0L && it.playerHostId == playerId) {
              dataStoreRepository.setCurrentWar(it)
          }
      }
  }
  ```

Nuances :

- Early-return sur une **condition non-nullité** (`if (list.isEmpty()) return`,
  `if (flag) return`) : **pas** concerné.
- Dans une **coroutine/lambda** où l'on veut vraiment sortir tôt d'un long bloc,
  `val x = obj ?: return@launch` reste idiomatique — le point est de ne pas écrire
  `if (x == null) return` sur une valeur qu'on va ensuite déballer.
- Ne pas imbriquer les `?.let` au-delà de deux niveaux : préférer un `?:` avec
  valeur par défaut ou décomposer la fonction.
