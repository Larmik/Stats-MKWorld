---
paths:
  - "app/src/**/*.kt"
---

# Commentaires : courts, documentaires, sans code mort

- Un commentaire décrit le **code actuel** : aucune mention de fichier, code ou feature supprimé
  (« remplace l'ancien X », « ex-Y », « fusionné depuis », « retiré en #NN »), aucun historique.
  git garde le passé.
- Un commentaire dit ce que fait un élément et pourquoi un choix non évident a été fait ; il ne
  paraphrase pas le code.
- KDoc concis sur les éléments publics non triviaux : une phrase, puis `@param` / `@return`
  seulement s'ils apportent une info absente de la signature.
- Garder l'info à valeur : piège métier, invariant, raison d'un choix (« throttle MKCentral →
  séquentiel », « garde-fou anti-wipe »).
- Référencer un **ticket** (`#NN`) ou une entrée d'audit, **jamais une rule** (ni numéro ni
  chemin) : une rule évolue, le renvoi se périme.
- Condenser un commentaire verbeux plutôt que le supprimer s'il porte du sens.
- À éviter : paraphrase (`// incrémente le compteur`), redondance avec le nom, pavés de 8-10
  lignes, en-têtes décoratifs (`// ----`), KDoc sur un trivial.

```kotlin
/** Utilisateurs de l'équipe (nœud Firebase `users/{teamId}`) ; liste vide si absent. */
suspend fun getUsers(teamId: String): List<User>
```

## Pas de code commenté ni orphelin

- Ne pas laisser de code commenté « pour plus tard » : git garde l'historique.
- Exception : une désactivation assumée avec le ticket de réactivation (ex. segmenté 12/24
  d'`AddWarScreen`, #91).
- Un composant remplacé est supprimé dans le même ticket (hors code 24p différé). Cf. audit C14, D34.
