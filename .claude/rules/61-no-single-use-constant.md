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
`WarExtension.kt`, `WarScore` → `WarScoreExtension.kt`…). Ne créer un nouveau
`XxxExtension.kt` que pour un **type de récepteur non encore couvert**.

**Interdit ferme** : ne **jamais** définir une extension dans un `.kt` qui n'est pas
celui correspondant à son type récepteur — ni comme extension top-level d'un autre
fichier, ni comme extension locale dans un écran / un modèle / une fonction non liés
au récepteur (ex. `fun WarScore.withPenalties()` posée dans `WarDetails.kt`). Deux
issues seulement :

- **usage unique** (ou capture du contexte local) → **ne pas en faire une extension**.
  Préférer, dans l'ordre : **inline complet** ; à défaut une **fonction membre privée**
  (si l'appelant est membre d'une classe, elle accède aux propriétés directement) ou
  une **fonction top-level privée à paramètre explicite**. **Pas de fonction locale
  imbriquée** (cf. `62-fonctions-locales.md`).
- **réellement réutilisée** (≥ 2 appelants) → la **déplacer** dans le fichier
  d'extension du récepteur (existant, sinon nouveau `XxxExtension.kt` si le type
  n'est pas couvert).

Exception : une extension **membre privée d'une classe** (ex. `private fun
WarDetails.outcome()` dans `Stats`, qui capture l'état de `Stats`) reste licite —
elle est scopée à la classe, pas posée en top-level hors de son fichier.
