# Pas de constante nommée pour une valeur utilisée une seule fois

**Portée** : tout code Kotlin du projet (toutes couches). Concerne l'introduction
d'une `const val` / `val` de premier niveau ou d'un `companion object` pour
extraire un littéral (nombre, chaîne, id…).

Ne **pas** extraire une valeur littérale en constante nommée si elle n'est
**référencée qu'à un seul endroit** : l'inliner sur le site d'usage, avec un
commentaire si le littéral mérite une explication. Une constante à usage unique
ajoute une indirection (aller-retour vers sa déclaration) sans bénéfice.

```kotlin
// Interdit (constante référencée une seule fois)
private const val MAX_PLAYERS = 6
if (selected.size == MAX_PLAYERS) { … }

// Attendu (littéral inliné + commentaire si le nombre mérite une explication)
if (selected.size == 6) { … } // composition complète d'une war 6v6
```

N'extraire une constante partagée que si le littéral est **réellement réutilisé
à ≥ 2 sites distincts**, surtout lorsque ces sites **doivent rester cohérents**
(ex. une même valeur employée à l'écriture *et* à un prédicat de lecture : la
constante partagée empêche alors la divergence). Le seuil du couple non-magique
(un même nombre magique répété) reste valable : la duplication d'un littéral à
plusieurs endroits justifie l'extraction ; l'usage unique ne la justifie pas.

Cette règle **généralise** à toutes les couches et aux **constantes** le
principe déjà posé pour les fonctions privées dans `30-repositories.md`
(« ne pas extraire de helper privé pour un seul appelant »).

## Corollaire — placement des fonctions d'extension dans `extension/`

Une nouvelle fonction d'extension va dans le **fichier existant** correspondant à
son récepteur, pas dans un nouveau fichier : une extension sur `List<…>` va dans
`extension/ListExtension.kt`, sur `String` dans `StringExtension.kt`, sur `War`
dans `WarExtension.kt`, etc. Ne créer un nouveau fichier `XxxExtension.kt` que
pour un **type de récepteur non encore couvert** par le dossier `extension/`.
