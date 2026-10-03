# Rules du projet — pour le flux `/ticket`

Ce dossier contient les **règles** que l'agent `ticket-worker` doit respecter
quand il traite un ticket, et qu'il **enrichit** au fil des retours.

## Catégories

Les rules sont classées **par couche / thème**, avec un préfixe numérique par
dizaine (`10`, `20`, `30`…) laissant de la place pour en ajouter dans chaque
catégorie :

| Dizaine | Catégorie | Fichiers |
|---|---|---|
| `1x` | **UI / Compose** | `10-ui-compose` (clés de liste) · `11-compose-state` (type de State, switch, wizard) · `12-ui-roster-display` (roster vs équipe, médaillon) · `13-ui-coherence-visuelle` (style établi, justesse des calculs, pourcentages, hauteur de cellule texte, course avec intermission) · `14-ui-back-onglets` (retour bottom-nav) · `16-ui-mutualiser-composants` (composant partagé, chercher l'existant, `MKButton`/segmented uniques) · `17-ui-bottombar-inset` (marge basse des pôles) |
| `2x` | **ViewModels** | `20-viewmodels` (ordre d'init, recherche à la saisie, pas de Context statique) · `21-vm-offload-compute` (calcul hors thread UI) |
| `3x` | **Repositories / data sources / UseCases** | `30-repositories` (`suspend` vs `Flow`, clear hors boucle, pas d'UI, réseau par élément) · `31-mkworld-only` (domaine mkworld) · `32-usecase-vs-repository` (placement d'une logique) |
| `4x` | **Build / release** | `40-build-release` (R8/Moshi, secrets et backup côté client) |
| `5x` | **Process / documentation** | `50-process-doc` (doc `docs/` à jour, tenue de l'audit, références croisées) |
| `6x` | **Style / idiomes Kotlin** | `60-kotlin-style` (`?.let`, `var` capturée, saisie) · `61-no-single-use-constant` (constantes, littéraux métier, placement des extensions) · `62-fonctions-locales` · `63-noms-parametres` · `64-commentaires-documentaires` (commentaires, code commenté) |

La correspondance entre ces rules et les entrées de `docs/AUDIT.md` est tenue dans la
matrice du § 9 de l'audit.

Pour ajouter une rule, la ranger dans la catégorie qui correspond (ex.
`20-viewmodels.md` = 21, 22… ou une seconde rule VM dans un fichier voisin) et,
si une nouvelle catégorie émerge, ouvrir une nouvelle dizaine.

## Format d'une rule

Un fichier Markdown par règle, nommé `NN-slug-court.md` (le préfixe numérique
`NN` sert à l'ordre de lecture et à regrouper les rules par catégorie, cf.
ci-dessus) :

```markdown
# <Titre de la règle>

**Portée** : <quand elle s'applique — ex: tout ticket, uniquement les bugs war, l'UI…>

<Le contenu de la règle : ce qui est exigé, interdit, ou la convention à suivre.
Concis et actionnable. Donne un exemple si utile.>
```

## Comment les rules sont utilisées

- Au début de chaque ticket, l'agent lit **tous** les fichiers `*.md` de ce
  dossier (sauf ce `README.md`) et les traite comme des contraintes fermes.
- Sur un retour utilisateur :
  - si le retour recoupe une rule existante → l'agent la **met à jour** ;
  - si c'est une préférence **générale et durable** non couverte mais qui
    **rentre dans une catégorie existante** (cf. tableau ci-dessus) → l'agent
    l'**ajoute au fichier de cette catégorie**, sans créer de nouveau fichier ;
  - si l'agent juge qu'il s'agit d'une **nouvelle couche / catégorie** non
    couverte → il **demande confirmation** avant de créer une nouvelle dizaine ;
  - un retour **spécifique à un seul ticket** ne crée **pas** de rule.

## Conventions

- Français, ton factuel.
- Une règle = un fichier = une idée. Éviter les fichiers fourre-tout.
- En cas de conflit entre une rule et le ticket, l'agent le **signale** au lieu
  de trancher seul.
