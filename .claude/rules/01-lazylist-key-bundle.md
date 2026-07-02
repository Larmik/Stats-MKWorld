# Clés de LazyList/LazyGrid : types stockables dans un Bundle uniquement

**Portée** : tout `items(...)` / `itemsIndexed(...)` d'un `LazyColumn`, `LazyRow`,
`LazyVerticalGrid` ou `LazyHorizontalGrid` en Jetpack Compose.

La valeur retournée par le lambda `key = { ... }` doit être un type **stockable
dans un `Bundle`** : `String`, `Int`, `Long`, `Boolean`, `Float`, `Char`… (ou un
`Parcelable`/`Serializable`). **Jamais** un objet quelconque, une `data class`
métier ou une `sealed class` : cela provoque le crash au moment de la
sauvegarde d'état :

```
java.lang.IllegalArgumentException: Type of the key <X> is not supported.
On Android you can only use types which can be stored inside the Bundle.
```

Règles :

- Utiliser un identifiant **primitif stable** de l'élément :
  `key = { it.id }` (String/Long), `key = { it.war.id }`, `key = { it.track.id }`,
  `key = { it.name }` pour un enum.
- Si le modèle **n'a pas d'id primitif stable** (ex. `WarPenalty`,
  positions de saisie, lignes de formulaire) : **ne pas mettre de clé** — l'index
  par défaut convient tant que la liste n'est pas réordonnable.
- Pour un `items(count: Int)` (grille indexée à plage fixe, ex. grilles de
  positions 1..12 / 1..24) : **ne pas ajouter de clé**. L'index par défaut suffit,
  la liste n'est ni réordonnable ni filtrée ; une clé `it + 1` n'apporte aucun
  bénéfice et peut au contraire régresser la sélection/le clic (l'état de
  composition étant alors mémorisé par clé au lieu d'être recyclé par slot).
- Si un identifiant composite est nécessaire, construire une `String` :
  `key = { "${it.teamId}-${it.type}-${it.amount}" }`.
- Ne jamais utiliser une clé recalculée à chaque frame (ex. `UUID.randomUUID()`) :
  cela invalide l'intérêt de la clé et aggrave les recompositions.
