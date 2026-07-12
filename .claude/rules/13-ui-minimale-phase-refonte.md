# Phase refonte graphique : prioriser le calcul, garder l'UI minimale

**Portée** : tout ticket touchant l'affichage des **statistiques** (et plus
largement l'UI) **pendant la phase de refonte graphique à venir**. S'applique
tant que des tickets de refonte UI dédiés n'ont pas été livrés.

Une refonte graphique globale est planifiée : elle retravaillera la présentation.
Inutile donc de peaufiner l'UI maintenant — le travail visuel serait jeté.

Ce qui est **exigé** :

- **La justesse des calculs de données prime.** L'essentiel d'un ticket de stats
  est que les valeurs produites (dans `extension/`, `model/local/Stats.kt`, les
  workers…) soient **correctes** et testables. C'est là que doit aller l'effort.
- **UI minimale et fonctionnelle.** Afficher la donnée de façon lisible, sans
  fignolage visuel : **réutiliser l'existant** (composants `MK*`, cellules
  `ui/cells/`, sections `MKExpandableSection`, lignes `MKStatRow`…) plutôt que de
  créer des composants soignés ou d'ajuster finement espacements, couleurs,
  animations, typographies.
- Pas de nouveaux assets/thèmes/graphes élaborés pour une stat : une ligne
  « libellé → valeur » ou une cellule réutilisée suffit.

Ce qui n'est **pas** demandé (à éviter) : polish visuel, micro-animations,
alignements pixel-perfect, variantes de style. Ces points relèveront des tickets
de refonte, pas des tickets de calcul de stats.

Cette règle **ne dispense pas** des autres règles UI (`10`/`11`/`12`) : clés de
liste correctes, bon choix de `State`, affichage roster — elles restent des
contraintes de correction, indépendantes du niveau de finition visuelle.
