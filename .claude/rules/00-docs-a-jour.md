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
