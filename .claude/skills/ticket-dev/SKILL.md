---
name: ticket-dev
description: Prend un ticket (numéro/URL d'issue GitHub, ou texte collé), crée une branche nommée d'après le titre, délègue les modifications de code à l'agent ticket-worker en respectant les rules du projet, itère sur les retours sans commiter, puis — sur validation explicite — commit / push / crée la PR vers master en liant l'issue. À utiliser quand on veut traiter un ticket de bout en bout.
arguments: [numero-ou-url-issue-github-ou-texte]
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash, Agent, SendMessage, AskUserQuestion
---

# Traitement d'un ticket de bout en bout

Entrée fournie : **$0**

Tu es l'**orchestrateur**. Tu pilotes un flux interactif sur plusieurs tours de
conversation. Les modifications de code sont **déléguées** à l'agent
`ticket-worker` ; toi, tu gères l'acquisition du ticket, la branche, les
échanges avec l'utilisateur et **toutes** les opérations git.

Règle d'or : **dès que `ticket-worker` a rendu la main** (première passe), tu
**commits systématiquement** (message = **nom de la branche**), tu **push**, et tu
**crées la PR si elle n'existe pas encore** (étape 5), **puis** tu attends les
retours. Chaque round de retours ré-applique des modifs via le worker et **re-commit
+ push** sur la même branche (la PR se met à jour). La validation finale de
l'utilisateur sert à **fusionner** la PR, pas à autoriser le premier commit.

(Ceci est l'**exception assumée** à la règle générale « pas de git sans demande »
de `CLAUDE.md`, propre au flux `/ticket-dev` : l'utilisateur a explicitement demandé
ce commit/push/PR automatique après chaque passe du worker.)

## 1. Acquérir le ticket

Les tickets vivent sur **GitHub Issues** (dépôt `Larmik/Stats-MKWorld`). L'entrée
peut arriver sous trois formes :

- **Numéro d'issue** (`#42` ou `42`) ou **URL d'issue GitHub** → récupère-la avec
  `gh issue view <n> --json number,title,body,labels,milestone`. **Mémorise le
  numéro `#N`** : il servira à lier la PR à l'issue (étape 5). Si le numéro
  n'existe pas, **arrête-toi et demande**.
- **Texte brut collé** (souvent au format `create-ticket` : titre préfixé
  `[BUG]`/`[FEATURE]`/`[TECH]`, sections Contexte / Description / Solutions proposées) →
  utilise-le tel quel. Il n'y a alors pas d'issue à lier (sauf si l'utilisateur en
  fournit le numéro). Propose éventuellement de créer d'abord l'issue via
  `/create-ticket`.

Si `$0` est vide, **arrête-toi** et demande à l'utilisateur le numéro/URL de
l'issue (ou de coller le ticket). Ne continue pas sans un titre et une description
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
   - retire le préfixe `[BUG]` / `[FEATURE]` / `[TECH]` et les emojis ;
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
  git** (lecture `git diff`/`git status` permise), exécuter la **relecture finale
  anti-audit** de son § 4 sur son diff, puis retourner un résumé (fichiers touchés +
  décisions + rules appliquées + résultat de la relecture).

**Conserve l'identifiant de l'agent** : les rounds de feedback suivants doivent
continuer *le même* agent via `SendMessage` (il garde le contexte du ticket, des
fichiers déjà modifiés et des rules).

Quand l'agent rend la main (première passe), passe par l'**étape 4** (contrôle
anti-audit), puis **enchaîne directement sur l'étape 5** (commit = nom de branche +
push + PR si absente), **puis** relaie son résumé à l'utilisateur et **attends** ses
retours.

## 4. Contrôle anti-audit avant chaque commit

Objectif : ne pas réalimenter `docs/AUDIT.md`. Avant **chaque** commit (première passe
et rounds de retours) :

1. Vérifie que le résumé du worker contient le résultat de sa relecture anti-audit
   (§ 4 de `.claude/agents/ticket-worker.md`). S'il manque, renvoie-le via
   `SendMessage` pour qu'il l'exécute.
2. Relis toi-même `git diff master...HEAD` + `git diff` (non commité) au regard de la
   matrice § 9 de `docs/AUDIT.md`, en ciblant les patterns les plus récurrents :
   composable/helper recréé alors qu'il existe (`rg "fun <Nom>"`), littéral métier
   recopié (`"-1"`, rôles, `teamOpponent.size`, `https://mkcentral.com`, `90.dp`),
   calcul de wars hors `withContext`, one-shot en `Flow`, `clear*()` en boucle, code
   commenté, secret/token ajouté.
3. Un écart → renvoie-le au worker (même agent) avant de commiter. Un écart
   **assumé** (hors périmètre, décision utilisateur) → il doit figurer dans
   `docs/AUDIT.md` avec sa ligne *Prévention* et être signalé à l'utilisateur.

## 5. Commit / push / PR (systématique, dès la fin du worker)

**À faire dès que le worker a rendu la main (première passe), sans attendre de
validation** :

1. `git add -A` puis commit avec pour **message le nom de la branche**. Termine le
   message par :

   ```
   Co-Authored-By: Claude Opus 5.5 (1M context) <noreply@anthropic.com>
   ```
2. `git push -u origin <nom-de-branche>`.
3. **Crée la PR si elle n'existe pas encore** (`gh pr view <branche>` pour vérifier).
   Base = **`master`** (toujours). Titre = titre du ticket ; corps = résumé du
   changement + `Closes #N` (si issue GitHub). Termine le corps par :

   ```
   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   ```
4. Habitude doc (rule 50) : mettre à jour les sections **impactées** de
   `docs/AUDIT.md` / `docs/TECHNICAL.md` / `docs/FUNCTIONAL.md`, puis re-commit + push
   sur la branche de la PR.
5. Affiche l'URL de la PR (et rappelle le `#N` de l'issue liée), puis **attends les
   retours** de l'utilisateur.

## 6. Boucle de retours (chaque round re-commit + push)

Tant que l'utilisateur donne des retours :

- **Continue le même agent** `ticket-worker` via `SendMessage` avec le détail des
  retours (il garde son contexte : rules et fichiers déjà lus). Demande-lui de :
  1. appliquer les corrections directement ;
  2. **enrichir les rules** : si un retour correspond à une rule existante dans
     `.claude/rules/`, la mettre à jour ; s'il exprime une préférence générale et
     durable sans rule correspondante, l'ajouter au fichier de la catégorie
     existante (tableau de `.claude/rules/README.md`) — une **nouvelle dizaine** ne
     se crée qu'après confirmation de l'utilisateur. Un retour purement spécifique à
     ce ticket ne doit **pas** créer de rule.
  3. refaire la relecture anti-audit (§ 4 du worker) sur le nouveau diff.
- Puis **contrôle anti-audit (étape 4)**, **re-commit (message = nom de branche) + push** sur la même branche (la PR se
  met à jour automatiquement), relaie le résumé et **attends** de nouveau.

> **Périmètre du commit.** Les **rules enrichies pendant le ticket** (`.claude/rules/`)
> sont committées **sur la branche du ticket** (dans sa PR) — elles ont évolué à cause
> de ce ticket, c'est cohérent. Ne PAS les isoler ailleurs (pas de ballet de branches).
> Seuls les **artefacts fondationnels pré-existants** non liés au ticket
> (skills eux-mêmes, docs de référence transverses) se committent hors de la PR ticket.

## 7. Validation / fusion

La **validation finale** de l'utilisateur sert à **fusionner** la PR. Avant de la
proposer comme « fait », vérifier que le ticket est couvert (critères d'acceptation de
l'issue) et que les rules sont respectées — notamment la cohérence visuelle avec
l'existant et la justesse des calculs (`13`), la réutilisation des composants
partagés (`16`) — et que le dernier contrôle anti-audit (étape 4) est propre. Lister les écarts éventuels : tant qu'il en reste, rester en boucle
de retours.

### Après fusion (obligatoire, ne jamais oublier)

1. `gh pr merge <n> --merge`, puis vérifier que l'issue `#N` est bien **fermée**
   (`Closes #N`) ; sinon `gh issue close <N>`.
2. **Passer l'issue dans la colonne « Terminé »** du board « Stats MKWorld »
   (projet `2`, owner `Larmik`) — la fermeture ne la déplace PAS automatiquement :
   ```bash
   ITEM=$(gh project item-list 2 --owner Larmik --limit 300 --format json \
     -q '.items[] | select(.content.number==<N>) | .id')
   gh project item-edit --id "$ITEM" --project-id PVT_kwHOAi0L9s4BdjcN \
     --field-id PVTSSF_lAHOAi0L9s4BdjcNzhYELEw --single-select-option-id 5348c84d
   ```
   Vérifier ensuite que le statut lu vaut bien `Terminé`.
3. Revenir sur `master` à jour (`git checkout master && git pull --ff-only`).
