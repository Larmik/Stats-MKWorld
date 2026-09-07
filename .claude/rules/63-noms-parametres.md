# Nommer les paramètres de façon explicite

**Portée** : tout code Kotlin (toutes couches). Déclaration d'un paramètre de
fonction / constructeur / lambda **nommée**.

Proscrire les noms cryptiques d'une lettre (`s`, `t`, `x`, `e`, `p`…) pour les
paramètres. Le nom doit **décrire le rôle** de la valeur (`score`, `teamId`,
`warDetails`, `penalties`…).

```kotlin
// Interdit
private fun penaltyAdjustedScore(s: WarScore): Int = …
// Attendu
private fun penaltyAdjustedScore(score: WarScore): Int = …
```

Nuances :

- `it` (paramètre implicite d'une lambda mono-argument passée à `map`/`filter`/`let`
  /`forEach`…) reste **idiomatique** et autorisé.
- Un **index / compteur d'itération** court (`i`, `n` dans `(1..6).map { n -> … }`)
  est toléré quand le rôle est évident et le scope minuscule ; préférer malgré tout
  un nom parlant dès que le corps grandit.
- Vaut pour **toutes les couches** (idiome transverse).
