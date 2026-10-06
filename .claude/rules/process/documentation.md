---
paths:
  - "docs/**"
  - ".claude/**"
  - "CLAUDE.md"
---

# Documentation : tenue de l'audit et références croisées

Le rappel « mettre à jour `docs/` à chaque changement » vit dans `CLAUDE.md`.

## Mise à jour de `docs/`

- La doc et la config Claude (`docs/`, `CLAUDE.md`, `.claude/`) décrivent le **code actuel** : pas
  de mention de fichier, code ou feature supprimé, pas de récit historique (git le garde). Une
  interdiction toujours utile se formule au présent sur le code actuel.
- Ne modifier que les sections impactées de `docs/AUDIT.md`, `docs/TECHNICAL.md` et
  `docs/FUNCTIONAL.md`.
- Changement purement interne : le dire dans le résumé plutôt que toucher la doc.

## `docs/AUDIT.md`

- Une entrée traitée est **supprimée** (pas de `[x] ✅`, l'historique est dans git). Une entrée
  partiellement traitée passe en `[~]` et ne décrit plus que le reste.
- Toute nouvelle entrée porte `chemin:ligne`, une sévérité et une ligne *Prévention :* (fichier de
  rule, étape de checklist, ou « — ») ; compléter la matrice du § 9 si une catégorie apparaît.
- Un problème découvert hors périmètre d'un ticket s'ajoute à l'audit au lieu d'être corrigé en
  passant.
- Toute entrée ajoutée à l'audit reçoit immédiatement son issue GitHub, créée via `/create-ticket`,
  et son numéro est reporté dans l'entrée (*Suivi : #NN*). Pas d'entrée « Suivi : à créer ».

## Références croisées

- Dans `docs/` et `.claude/`, désigner une rule par son chemin relatif à `.claude/rules/` (ex.
  `ui/compose.md`) ; le code n'en cite aucune (cf. `kotlin/commentaires.md`).
- Renommer, fusionner ou supprimer une rule → `rg "<chemin>" docs .claude CLAUDE.md` et corriger
  chaque renvoi, puis mettre à jour `.claude/rules-index.md`.
- Une affirmation de doc (« X est corrigé dans A, B, C ») se vérifie dans le code avant d'être
  écrite.
