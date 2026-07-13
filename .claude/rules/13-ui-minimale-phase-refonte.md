# Phase refonte graphique : prioriser le calcul, garder l'UI minimale

**Portée** : tout ticket touchant l'affichage des **statistiques** (et l'UI en
général) pendant la phase de refonte graphique à venir. S'applique tant que les
tickets de refonte UI dédiés ne sont pas livrés.

Une refonte graphique globale est planifiée : inutile de peaufiner l'UI maintenant,
le travail visuel serait jeté.

**Exigé** :

- **La justesse des calculs prime.** Les valeurs produites (`extension/`,
  `model/local/Stats.kt`, workers…) doivent être **correctes** et testables. C'est
  là que va l'effort.
- **UI minimale et fonctionnelle** : afficher la donnée lisiblement, sans fignolage.
  **Réutiliser l'existant** (composants `MK*`, `ui/cells/`, `MKExpandableSection`,
  `MKStatRow`…) plutôt que créer des composants soignés ou ajuster finement
  espacements/couleurs/animations/typographies.
- Pas de nouveaux assets/thèmes/graphes élaborés pour une stat : une ligne
  « libellé → valeur » ou une cellule réutilisée suffit.

**À éviter** : polish visuel, micro-animations, pixel-perfect, variantes de style
(relèveront des tickets de refonte).

Cette règle **ne dispense pas** des autres règles UI (`10`/`11`/`12`) : clés de
liste correctes, bon choix de `State`, affichage roster restent des contraintes de
correction, indépendantes du niveau de finition.
