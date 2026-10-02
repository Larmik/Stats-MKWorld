# Garder la documentation à jour

**Portée** : tout ticket qui modifie le comportement, l'architecture ou le fonctionnel.

À chaque changement destiné à une PR, mettre à jour la documentation du dossier
`docs/` pour refléter le changement :

- `docs/AUDIT.md` — état / points d'audit ;
- `docs/TECHNICAL.md` — architecture et détails techniques ;
- `docs/FUNCTIONAL.md` — comportement fonctionnel côté utilisateur.

Ne mettre à jour que les sections réellement impactées ; ne pas réécrire toute la
doc. Si le changement est purement interne et sans impact doc, l'indiquer dans le
résumé plutôt que de modifier les fichiers pour rien.

## Tenue de `docs/AUDIT.md`

- Une entrée **traitée est supprimée** (pas de `[x] ✅` conservé : l'historique est dans
  git). Une entrée partiellement traitée passe en `[~]` et ne décrit plus que le reste.
- Toute **nouvelle** entrée porte `chemin:ligne`, une sévérité et une ligne
  *Prévention : rule NN* (ou « — » si aucune), et la matrice du § 9 est complétée si une
  nouvelle catégorie apparaît.
- Un ticket qui découvre un problème hors périmètre l'ajoute à l'audit au lieu de le
  corriger en passant.

## Références croisées

Renommer, fusionner ou supprimer une rule → `rg "<ancien nom ou numéro>" docs .claude CLAUDE.md`
et corriger chaque référence. De même, une affirmation de `TECHNICAL.md` du type « X est
corrigé dans A, B, C » se vérifie dans le code avant d'être écrite. Cf. audit G8, P8.
