---
name: ticket-dev
description: Prend un ticket (texte collé ou URL Trello), crée une branche nommée d'après le titre, délègue les modifications de code à l'agent ticket-worker en respectant les rules du projet, itère sur les retours sans commiter, puis — sur validation explicite — commit / push / crée la PR vers master. À utiliser quand on veut traiter un ticket de bout en bout.
arguments: [ticket-ou-url-trello]
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash, WebFetch, Agent, SendMessage, AskUserQuestion
---

# Traitement d'un ticket de bout en bout

Entrée fournie : **$0**

Tu es l'**orchestrateur**. Tu pilotes un flux interactif sur plusieurs tours de
conversation. Les modifications de code sont **déléguées** à l'agent
`ticket-worker` ; toi, tu gères l'acquisition du ticket, la branche, les
échanges avec l'utilisateur et **toutes** les opérations git.

Règle d'or : **aucun `git commit`, `git push` ni PR tant que l'utilisateur n'a
pas explicitement validé** (étape 5). Ne devine jamais cette validation.

## 1. Acquérir le ticket

Le ticket peut arriver sous deux formes :

- **Texte brut collé** (souvent au format `create-ticket` : titre préfixé
  `[BUG]`/`[FEATURE]`, sections Contexte / Description / Solutions proposées) →
  utilise-le tel quel.
- **URL Trello** → tente un `WebFetch` de l'URL. Si la carte est privée ou que
  le contenu récupéré est inexploitable, **arrête-toi et demande** à
  l'utilisateur de coller le contenu du ticket.

Si `$0` est vide, **arrête-toi** et demande à l'utilisateur de coller le ticket
(ou de fournir l'URL). Ne continue pas sans un titre et une description
exploitables.

## 2. Synchroniser puis créer la branche

**Avant toute chose** : synchronise `master` et crée **toujours** ta branche à
partir de celle-ci. Ne délègue jamais à l'agent, n'ouvre jamais de branche, tant
que ce point de départ n'est pas garanti.

1. Synchronise le dépôt (habitude projet, non négociable) : `git fetch origin`,
   puis `git checkout master` et `git pull --ff-only`. La branche par défaut du
   projet est **master** — ignore `main`. Ne crée jamais la branche depuis une
   autre branche courante : reviens explicitement sur `master` à jour d'abord.
2. **Condense le titre** du ticket en un nom de branche :
   - retire le préfixe `[BUG]` / `[FEATURE]` et les emojis ;
   - garde **4 à 5 mots** signifiants (les mots-clés du titre) ;
   - `snake_case`, minuscules, sans accents ni caractères spéciaux, **sans
     préfixe de type**.
   - Exemple : `[BUG] Le rôle du membre est réinitialisé pendant la war`
     → `role_membre_reinitialise_war`.
3. Crée la branche depuis un `master` à jour : `git checkout -b <nom>`.
4. Annonce à l'utilisateur le nom de branche créé.

## 3. Déléguer les modifications à l'agent `ticket-worker`

Lance l'agent `ticket-worker` (via l'outil Agent, `subagent_type: "ticket-worker"`)
avec un prompt contenant :

- le **contenu intégral du ticket** ;
- le **nom de la branche** ;
- la consigne : lire **toutes** les rules dans `.claude/rules/*.md` et les
  respecter, faire les modifications nécessaires, **ne faire aucune opération
  git**, puis retourner un résumé (fichiers touchés + décisions + rules
  appliquées).

**Conserve l'identifiant de l'agent** : les rounds de feedback suivants doivent
continuer *le même* agent via `SendMessage` (il garde le contexte du ticket, des
fichiers déjà modifiés et des rules).

Quand l'agent rend la main, **relaie son résumé** à l'utilisateur et **attends**
son retour ou sa validation. Ne commite pas.

## 4. Boucle de retours (sans commit)

Tant que l'utilisateur n'a **pas** validé :

- S'il donne des retours, **continue le même agent** `ticket-worker` via
  `SendMessage` avec le détail des retours. Demande-lui de :
  1. appliquer les corrections directement (toujours **sans** commit) ;
  2. **enrichir les rules** : si un retour correspond à une rule existante dans
     `.claude/rules/`, la mettre à jour ; s'il exprime une préférence générale et
     durable sans rule correspondante, en créer une nouvelle (format : voir
     `.claude/rules/README.md`). Un retour purement spécifique à ce ticket ne
     doit **pas** créer de rule.
- Relaie le résumé mis à jour et **attends** de nouveau.

Répète autant de fois que nécessaire.

## 5. Validation → commit / push / PR

Uniquement quand l'utilisateur valide **explicitement** les changements :

1. **Demande le message de commit** à l'utilisateur (l'input qu'il fournit *est*
   le message). Attends sa réponse.
2. `git add -A` puis commit avec ce message. Termine le message de commit par :

   ```
   Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>
   ```
3. `git push -u origin <nom-de-branche>`.
4. Crée la PR **vers master** : `gh pr create --base master --head <branche>`
   avec un titre = titre du ticket et un corps résumant le changement. Termine le
   corps par :

   ```
   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   ```
5. Affiche l'URL de la PR.

Ne fais ces opérations git **qu'à cette étape**, et jamais avant.
