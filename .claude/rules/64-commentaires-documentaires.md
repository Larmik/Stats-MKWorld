# Commentaires documentaires courts et non redondants

**Portée** : tout commentaire du code (KDoc `/** */`, commentaires de bloc/ligne `//`),
toutes couches. S'applique à l'écriture d'un nouveau commentaire **comme** à la relecture
d'un commentaire existant.

Style attendu : **documentaire, court, actionnable**. Un commentaire explique **ce que fait**
un élément (fonction / `val` / `var` / classe) et **pourquoi** un choix non évident a été fait
— jamais paraphraser le code ligne à ligne.

## Exigé

- **KDoc concis** sur les éléments publics non triviaux : une phrase de description, puis
  `@param` / `@return` **seulement s'ils apportent une info** que la signature ne donne pas.
  Ne pas documenter un paramètre dont le nom est déjà explicite (rule 63) au prix d'une
  paraphrase (`@param teamId L'identifiant de l'équipe`).
- **Garder l'info à valeur** : piège métier, invariant, raison d'un choix (« throttle
  MKCentral → séquentiel », « garde-fou anti-wipe », « idempotent »), référence de ticket
  (#NN) ou de rule. C'est **ça** qu'un commentaire doit porter.
- **Condenser** un commentaire verbeux : retirer la paraphrase du code, les répétitions, les
  reformulations d'une même idée, les phrases trop longues. Reformuler plus court **sans
  perdre** l'info utile — ne pas supprimer un commentaire porteur de sens, le raccourcir.

## À éviter

- **Paraphrase du code** : `// incrémente le compteur` au-dessus de `count++`.
- **Redondance** : répéter en prose ce que le nom de la fonction/variable dit déjà.
- **Pavés** : blocs de 8-10 lignes là où 2-3 suffisent ; en-têtes ASCII décoratifs
  (`// ----`), séparations verbeuses.
- **Sur-documentation** d'un trivial : un one-liner évident (`getters`, mapping direct) n'a
  pas besoin de KDoc.

```kotlin
// Trop verbeux (paraphrase + répétitions)
/**
 * Cette fonction prend en paramètre un identifiant d'équipe (teamId) et retourne
 * la liste des utilisateurs de cette équipe. Elle interroge Firebase pour récupérer
 * les utilisateurs. Si aucun utilisateur n'est trouvé, elle retourne une liste vide.
 */
suspend fun getUsers(teamId: String): List<User>

// Attendu (court, l'info utile seulement)
/** Utilisateurs de l'équipe (nœud Firebase `users/{teamId}`) ; liste vide si absent. */
suspend fun getUsers(teamId: String): List<User>
```

Se combine avec **63** (noms explicites → moins de commentaires nécessaires) et **62/61**
(pas de helper/fonction mono-usage à sur-documenter).
