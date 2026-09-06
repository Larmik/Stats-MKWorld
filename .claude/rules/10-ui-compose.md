# Clés de LazyList/LazyGrid : types stockables dans un Bundle uniquement

**Portée** : tout `items(...)` / `itemsIndexed(...)` d'un `LazyColumn`, `LazyRow`,
`LazyVerticalGrid` ou `LazyHorizontalGrid`.

La valeur du lambda `key = { ... }` doit être **stockable dans un `Bundle`** :
`String`, `Int`, `Long`, `Boolean`, `Float`, `Char`… (ou `Parcelable`/`Serializable`).
**Jamais** un objet, une `data class` métier ou une `sealed class` → crash à la
sauvegarde d'état :

```
java.lang.IllegalArgumentException: Type of the key <X> is not supported.
On Android you can only use types which can be stored inside the Bundle.
```

Règles :

- Utiliser un **identifiant primitif stable** : `key = { it.id }`,
  `key = { it.war.id }`, `key = { it.track.id }`, `key = { it.name }` pour un enum.
- Modèle **sans id primitif stable** (ex. `WarPenalty`, positions de saisie, lignes
  de formulaire) : **ne pas mettre de clé** (l'index par défaut convient tant que la
  liste n'est pas réordonnable).
- `items(count: Int)` (grille indexée à plage fixe, ex. positions 1..12 / 1..24) :
  **ne pas ajouter de clé** — une clé `it + 1` peut régresser la sélection/le clic.
- Identifiant composite → construire une `String` : `key = { "${it.teamId}-${it.type}-${it.amount}" }`.
- Ne jamais utiliser une clé recalculée à chaque frame (ex. `UUID.randomUUID()`).
