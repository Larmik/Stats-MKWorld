---
name: clean-branches
description: Nettoie les branches Git locales et distantes devenues inutiles (déjà mergées dans master ou dont la branche distante a disparu). Synchronise le dépôt, dresse la liste des branches supprimables en séparant local et remote, demande confirmation, puis supprime uniquement ce que l'utilisateur valide. Ne touche jamais master/main ni la branche courante. À utiliser après avoir mergé des PR pour faire le ménage.
disable-model-invocation: true
allowed-tools: Bash, AskUserQuestion
---

# Nettoyage des branches locales et distantes

Ton objectif : supprimer proprement les branches Git devenues inutiles, **en
local et sur `origin`**, sans jamais rien détruire que l'utilisateur n'ait
validé. Tu ne fais **aucune** suppression avant l'étape de confirmation (4).

## Garde-fous (non négociables)

- **Ne supprime JAMAIS** `master` ni `main`, ni la **branche courante** (celle sur
  laquelle on est). `master` est la branche par défaut du projet.
- N'utilise que la **suppression sûre** en local (`git branch -d`, qui refuse une
  branche non mergée). N'emploie `git branch -D` (forcé) **que** si l'utilisateur
  le demande explicitement pour une branche non mergée précise.
- La suppression distante (`git push origin --delete <b>`) est **irréversible** :
  elle exige une confirmation explicite (étape 4).

## 1. Synchroniser et se placer sur master

Avant tout diagnostic, garantis un état à jour (habitude projet) :

1. `git fetch --prune origin` — récupère l'état distant et **élague** les
   références distantes disparues (`--prune` est essentiel pour détecter les
   branches supprimées côté serveur).
2. Note la branche courante (`git rev-parse --abbrev-ref HEAD`). Si ce n'est pas
   `master`, bascule dessus : `git checkout master` puis `git pull --ff-only`.
   Si le working tree est sale et empêche le checkout, **arrête-toi** et signale-le
   à l'utilisateur (ne stash pas sans son accord).

## 2. Dresser la liste des branches supprimables

Distingue **deux catégories**, et présente-les séparément :

**A. Branches locales déjà mergées dans `master`** — sûres à supprimer :
```bash
git branch --merged master | grep -vE '^\*|^\s*(master|main)$'
```

**B. Branches locales dont l'upstream a disparu** (PR mergée + branche distante
supprimée sur GitHub) — repérables via le marqueur `: gone]` :
```bash
git branch -vv | grep ': gone]' | awk '{print $1}' | grep -vE '^\*'
```

**C. Branches distantes (`origin/*`) mergées dans `origin/master`** — candidates
à suppression distante :
```bash
git branch -r --merged origin/master | grep -vE 'origin/(master|main|HEAD)'
```

Compile ces listes. Écarte systématiquement `master`, `main` et la branche
courante. Si **aucune** branche n'est supprimable, dis-le et **arrête-toi**.

## 3. Présenter le diagnostic

Affiche un récapitulatif clair, par catégorie, par exemple :

- **Locales mergées / upstream disparu** (suppression locale sûre) : liste.
- **Distantes mergées sur `origin/master`** (suppression distante irréversible) :
  liste.

Pour chaque branche, tu peux indiquer le dernier commit (`git log -1 --format=…`)
si ça aide à décider. Signale toute branche **locale non mergée** qui traîne :
ne la propose pas à la suppression sûre, mentionne-la seulement.

## 4. Confirmer puis supprimer

Utilise `AskUserQuestion` pour faire choisir à l'utilisateur **ce qu'il veut
supprimer**, en séparant bien local et distant (la suppression distante est
irréversible). Propose des options du type : « Local uniquement », « Local +
distant », « Rien », ou une sélection.

Ensuite, et **seulement** ensuite :

- **Local** : `git branch -d <branche>` pour chaque branche validée. Si `-d`
  refuse (branche non mergée) et que l'utilisateur avait ciblé cette branche en
  connaissance de cause, propose `git branch -D <branche>` — sinon laisse-la.
- **Distant** : `git push origin --delete <branche>` pour chaque branche
  distante validée.

Traite les branches une par une et rapporte chaque suppression (ou échec).

## 5. Récapitulatif final

Affiche l'état après nettoyage : branches locales et distantes supprimées, celles
conservées, et éventuelles branches non mergées laissées de côté. Ne commite
rien, ne crée aucune branche : ce skill ne fait que du ménage.
