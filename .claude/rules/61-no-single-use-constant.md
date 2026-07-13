# Pas de constante nommée pour une valeur utilisée une seule fois

**Portée** : tout code Kotlin (toutes couches). Introduction d'une `const val`/`val`
de premier niveau ou d'un `companion object` pour extraire un littéral.

Ne **pas** extraire une valeur littérale en constante nommée si elle n'est
**référencée qu'à un seul endroit** : l'inliner, avec un commentaire si le littéral
mérite une explication.

```kotlin
// Interdit (constante référencée une seule fois)
private const val MAX_PLAYERS = 6
if (selected.size == MAX_PLAYERS) { … }
// Attendu (littéral inliné + commentaire)
if (selected.size == 6) { … } // composition complète d'une war 6v6
```

N'extraire une constante partagée que si le littéral est **réellement réutilisé à
≥ 2 sites distincts**, surtout s'ils **doivent rester cohérents** (ex. même valeur à
l'écriture *et* à un prédicat de lecture : la constante empêche la divergence).

Généralise à toutes les couches et aux constantes le principe « pas d'extraction pour
un seul appelant » (`30-repositories.md`).

## Corollaire — placement des fonctions d'extension dans `extension/`

Une nouvelle extension va dans le **fichier existant** correspondant à son récepteur
(`List<…>` → `extension/ListExtension.kt`, `String` → `StringExtension.kt`, `War` →
`WarExtension.kt`…). Ne créer un nouveau `XxxExtension.kt` que pour un **type de
récepteur non encore couvert**.
