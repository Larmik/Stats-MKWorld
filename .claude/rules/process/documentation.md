---
paths:
  - "docs/**"
  - ".claude/**"
  - "CLAUDE.md"
---

# Documentation : tenue de l'audit et références croisées

Le rappel « mettre à jour `docs/` à chaque changement » vit dans `CLAUDE.md`.

## Mise à jour de `docs/`

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

## Références croisées

- Désigner une rule par son chemin relatif à `.claude/rules/` (ex. `ui/compose.md`). Les anciens
  numéros (`rule 11`) se résolvent par la table de `.claude/rules-index.md`.
- Renommer, fusionner ou supprimer une rule → `rg "<ancien chemin ou numéro>" docs .claude CLAUDE.md`
  et corriger chaque renvoi, puis mettre à jour l'index.
- Une affirmation de doc (« X est corrigé dans A, B, C ») se vérifie dans le code avant d'être
  écrite. Cf. audit G8, P8.
