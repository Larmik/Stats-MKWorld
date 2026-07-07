# Préférer `obj?.let { … }` à `if (obj == null) return`

**Portée** : tout code Kotlin du projet manipulant une valeur **nullable** dont
on veut ne rien faire quand elle vaut `null`.

Ne pas garder une valeur nullable avec un early-return :

```kotlin
// Interdit
fun handle(war: War?) {
    if (war == null) return
    doSomething(war)
}
```

Utiliser le scope function `?.let`, qui exprime directement « exécute ce bloc
seulement si non-null » et fournit un `it` déjà déballé (smart-cast garanti,
sans variable intermédiaire) :

```kotlin
// Attendu
fun handle(war: War?) {
    war?.let {
        doSomething(it)
    }
}
```

Points d'application :

- Vaut pour **toutes les couches** (ViewModels, repositories, data sources,
  extensions, UI) : c'est un idiome transverse, pas une règle de couche.
- **Fusionner les gardes suivantes dans le bloc** plutôt que d'enchaîner des
  `if (…) return` : à l'intérieur du `let`, combiner les conditions dans un seul
  `if` (ou un `takeIf`) au lieu de multiplier les sorties anticipées. Cf.
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

- Le early-return sur une **condition qui n'est pas une nullité** (ex.
  `if (list.isEmpty()) return`, `if (flag) return`) n'est **pas** concerné :
  `?.let` ne s'y applique pas.
- Dans une **coroutine / lambda** où l'on veut réellement sortir tôt d'un long
  bloc, l'`?: return@xxx` (`val x = obj ?: return@launch`) reste acceptable et
  idiomatique — le point est de ne pas écrire `if (x == null) return` sur une
  valeur qu'on va ensuite déballer.
- Ne pas imbriquer profondément les `?.let` : au-delà de deux niveaux,
  préférer un `?:` avec valeur par défaut ou décomposer la fonction.
