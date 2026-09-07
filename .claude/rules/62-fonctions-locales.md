# Éviter les fonctions locales / imbriquées

**Portée** : tout code Kotlin (toutes couches). Introduction d'une fonction
**locale** (déclarée à l'intérieur d'une autre fonction, d'un `when`, d'un `init`…).

Éviter les fonctions imbriquées (« pas très joli », lisibilité). Ce n'est **pas** un
interdit absolu, mais une préférence forte : par défaut, ne pas en écrire.

À la place, dans l'ordre de préférence :

1. **Inline complet** du corps si l'usage est unique et trivial (rules `30`/`61` :
   pas d'extraction pour un seul appelant) ;
2. **fonction membre privée** de la classe si l'appelant est membre (elle accède aux
   propriétés directement, sans paramètre superflu) ;
3. **fonction top-level privée** (paramètre explicite) si hors classe.

```kotlin
// À éviter — fonction locale imbriquée
fun scoreMargin(is24p: Boolean): Int = when (is24p) {
    true -> {
        fun scoreWithPenalties(s: WarScore) = /* … */   // imbriquée + param cryptique
        war.scores.maxOf { scoreWithPenalties(it) }
    }
    else -> …
}
// Attendu — fonction membre privée, paramètre nommé
fun scoreMargin(is24p: Boolean): Int = when (is24p) {
    true -> war.scores.maxOf { penaltyAdjustedScore(it) }
    else -> …
}
private fun penaltyAdjustedScore(score: WarScore): Int = /* … */
```

Se combine avec `61` (placement des extensions) : un helper mono-usage ne devient
**jamais** une fonction locale — on inline ou on en fait une fonction membre /
top-level privée. Une lambda passée à un opérateur (`map`/`filter`/`let`…) n'est
**pas** concernée (ce n'est pas une fonction locale nommée).
