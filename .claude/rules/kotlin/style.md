---
paths:
  - "app/src/**/*.kt"
---

# Style Kotlin : nullables, état mutable, saisie, fonctions locales, noms

## Nullables

- Préférer `obj?.let { … }` à `if (obj == null) return` ; fusionner les gardes suivantes dans un
  seul `if` du bloc (cf. `FirebaseRepository.restoreCurrentWarIfHost`).
- Toléré : `val x = obj ?: return@launch` pour sortir tôt d'un long bloc de coroutine ; early-return
  sur une condition non nullable (`if (list.isEmpty()) return`).
- Pas plus de deux `?.let` imbriqués : `?:` avec valeur par défaut ou décomposer.
- Pas de `?.toString()` pour fabriquer un identifiant (`"null"`) : `team?.let { fetchAllies(it.id.toString()) }`.
  Cf. audit B33.
- Pas de `!!` : `?.let`, `?:` avec repli, `filterNotNull()` / `mapNotNull` en amont, ou smart-cast.

```kotlin
war?.let { doSomething(it) }   // au lieu de : if (war == null) return ; doSomething(war)
```

## État mutable et saisie

- Ne pas réassigner une `var` extérieure dans un `map` / `flatMap` / `forEach` pour la relire
  dans un opérateur suivant : porter la valeur dans l'élément (`Pair`, data class). Cf. audit B31.
- Texte saisi → `toIntOrNull()` / `toLongOrNull()` avec repli explicite, jamais `toInt()`. Cf.
  audit B32.

## Pas de fonction locale

Ne pas déclarer de fonction dans une fonction, un `when` ou un `init`. Par ordre de préférence :

1. inline du corps si l'usage est unique ;
2. fonction membre privée si l'appelant est membre d'une classe ;
3. fonction top-level privée à paramètre explicite sinon.

Une lambda passée à un opérateur n'est pas une fonction locale.

## Noms de paramètres explicites

- Le nom décrit le rôle (`score`, `teamId`, `warDetails`), jamais une lettre (`s`, `t`, `x`).
- Tolérés : `it`, et un index court (`i`, `n`) dans un scope minuscule.

```kotlin
private fun penaltyAdjustedScore(score: WarScore): Int = …   // pas (s: WarScore)
```
