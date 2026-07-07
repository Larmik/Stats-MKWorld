# Rules du projet — pour le flux `/ticket`

Ce dossier contient les **règles** que l'agent `ticket-worker` doit respecter
quand il traite un ticket, et qu'il **enrichit** au fil des retours.

## Catégories

Les rules sont classées **par couche / thème**, avec un préfixe numérique par
dizaine (`10`, `20`, `30`…) laissant de la place pour en ajouter dans chaque
catégorie :

| Fichier | Catégorie | Portée |
|---|---|---|
| `10-ui-compose.md` | **UI / Compose** | composants, LazyList/LazyGrid, recompositions |
| `20-viewmodels.md` | **ViewModels** | init des Flow, `StateFlow`, ordre des propriétés |
| `30-repositories.md` | **Repositories / data sources** | `suspend` vs `Flow`, accès Room/Firebase/réseau |
| `40-build-release.md` | **Build / release** | R8/ProGuard, DTO Moshi, signature |
| `50-process-doc.md` | **Process / documentation** | doc `docs/` à jour, workflow |
| `60-kotlin-style.md` | **Style / idiomes Kotlin** | idiomes transverses (`?.let` vs `if (x == null) return`…) |

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
