# Rules du projet — pour le flux `/ticket`

Ce dossier contient les **règles** que l'agent `ticket-worker` doit respecter
quand il traite un ticket, et qu'il **enrichit** au fil des retours.

## Format d'une rule

Un fichier Markdown par règle, nommé `NN-slug-court.md` (le préfixe numérique
`NN` sert uniquement à l'ordre de lecture) :

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
  - si c'est une préférence **générale et durable** non couverte → l'agent
    **crée** une nouvelle rule ;
  - un retour **spécifique à un seul ticket** ne crée **pas** de rule.

## Conventions

- Français, ton factuel.
- Une règle = un fichier = une idée. Éviter les fichiers fourre-tout.
- En cas de conflit entre une rule et le ticket, l'agent le **signale** au lieu
  de trancher seul.
