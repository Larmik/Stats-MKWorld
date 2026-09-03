---
name: edit-ticket
description: Re-rédige une issue GitHub existante du dépôt Larmik/Stats-MKWorld à partir de précisions/modifications fournies. À utiliser quand on veut affiner, compléter ou corriger un ticket déjà créé (numéro d'issue en entrée) sans le recréer — le skill fusionne l'existant avec les nouvelles précisions et met à jour l'issue.
arguments: [numero-issue, precisions-ou-modifications]
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash(git log *), Bash(git diff *), Bash(gh issue *), Bash(gh label *), Bash(gh api *), Agent, AskUserQuestion
---

# Édition d'une issue GitHub existante

Entrée : **$ARGUMENTS** (un **numéro d'issue** GitHub `#N`/`N` + les **précisions /
modifications** à apporter au ticket).

Ton objectif : **re-rédiger** une issue GitHub existante du dépôt
`Larmik/Stats-MKWorld` pour y intégrer les précisions fournies, en gardant le ticket
propre, précis et actionnable. Tu ne crées pas de nouvelle issue — tu **édites**
l'existante.

> Gestion des tickets sur **GitHub Issues** (Trello abandonné). Le dépôt est
> **public** : le corps est visible de tous — **aucun secret** (clé, token, chemin de
> keystore, id Discord réel…) dans le contenu.

## 1. Acquérir l'issue et comprendre la demande

1. Sépare l'entrée en **numéro d'issue** et **précisions**. Si le numéro d'issue
   **manque**, ou si les précisions sont **vides/inexploitables**, **arrête-toi et
   demande** ce qui manque avant d'aller plus loin.
2. Récupère l'issue existante :
   ```bash
   gh issue view <N> --json number,title,body,labels,milestone,state
   ```
   Si le numéro n'existe pas, **arrête-toi et signale-le**. Si l'issue est **fermée**,
   signale-le et demande confirmation avant d'éditer.
3. **Lis le corps existant** en entier : identifie sa structure (sections Contexte /
   Description / Pistes techniques / Solutions proposées / Notes), ce qui est déjà
   couvert, et **en quoi les précisions le modifient** : ajout d'info, correction d'une
   hypothèse, changement de périmètre, nouvelle solution retenue, requalification
   bug↔feature…
4. Si une précision est **ambiguë ou contredit** le corps existant (ex. « en fait le
   bug est aussi en 24p » alors que le ticket dit 12p only), **demande clarification**
   (`AskUserQuestion`) plutôt que de trancher seul.

## 2. Ré-enquêter dans le code si utile

Comme `create-ticket`, mais **seulement quand la modification le justifie** (les
pistes techniques doivent rester exactes) :

- Si les précisions touchent la **cause racine**, un **fichier**, ou une **solution
  technique** → relis le code concerné pour rafraîchir les pistes (`chemin:ligne`) et
  vérifier que les solutions proposées tiennent toujours.
- Pour une investigation large, délègue à un agent `Explore`.
- **Ne sur-investigue pas** : quelques pistes solides et à jour valent mieux qu'un
  audit. Si la précision est purement rédactionnelle/organisationnelle, saute cette
  étape.

## 3. Re-rédiger le corps entier

**Fusionne** le corps existant et les précisions, puis **réécris le corps complet** en
respectant **exactement** la structure `create-ticket` (ton concis et factuel, en
français) :

````markdown
## 🎯 Contexte
<2 à 4 phrases : où, dans quelles conditions, pourquoi ça compte (écran, mode war
12p/24p, étape du flux…).>

## 🐛 Description / Comportement attendu
<BUG : observé vs attendu, repro si connue, fréquence, données concrètes.
FEATURE : ce qu'on veut, besoin utilisateur, critères d'acceptation.>

## 🔍 Pistes techniques
<Optionnel (surtout bug) : fichiers/lignes suspectés (`chemin:ligne`), hypothèses de
cause racine, de la plus probable à la moins probable. Omettre si feature simple.>

## ✅ Solutions proposées
<Liste numérotée de solutions concrètes : ce qu'on change, où, le compromis
(effort/risque/portée). Mets en avant la solution recommandée.>

## 📌 Notes
<Optionnel : effets de bord, points à valider, liens, dépendances.>
````

Règles de fusion :

- **Intègre** les précisions dans les sections concernées — ne te contente pas de les
  coller en bloc à la fin. Un ticket édité doit se lire comme s'il avait été écrit
  correctement du premier coup.
- **Conserve** tout ce qui reste valable dans le corps d'origine ; **corrige/supprime**
  ce que les précisions invalident ; **enrichis** avec les nouvelles infos.
- GitHub rend le **Markdown** : titres, listes, **gras**, `code inline`, blocs de code,
  **task lists** `- [ ]` cliquables. Utilise `- [ ]` pour critères d'acceptation et
  étapes de solution actionnables. Paragraphes courts et aérés ; emojis de section
  conservés (issue scannable).
- Le corps d'origine est **récupérable** via l'historique d'édition GitHub — pas besoin
  de conserver une trace du texte remplacé dans le corps lui-même.

## 4. Ajuster titre / labels / milestone si pertinent

Si les précisions **changent la nature ou le rattachement** du ticket, ajuste-les
(sinon, laisse-les tels quels) — et **signale toujours** ce que tu changes :

- **Titre** : reformule si la modif le rend plus juste. **Conserve le préfixe**
  `[BUG]` / `[FEATURE]` (il sert au nommage de branche par `/ticket-dev`). Une
  requalification bug↔feature change le préfixe **et** le label de type.
- **Label de type** : `bug` ↔ `enhancement` selon la nature.
- **Epic refonte UX** : si la modif fait entrer/sortir le ticket de l'epic (nav 5
  pôles / stats-résultats — cf. `docs/PROTOTYPE_UX.md`), ajoute/retire le label
  `epic:refonte-ux`, le label de pôle (`pole:accueil` | `pole:wars` | `pole:stats` |
  `pole:classements` | `pole:profil`) et le **milestone** « Refonte UX — 5 pôles ».
- En cas de doute sur le pôle/rattachement, demande (`AskUserQuestion`).

## 5. Livraison — éditer l'issue (directement)

Applique la mise à jour **directement** (pas d'étape d'aperçu/validation : l'historique
d'édition GitHub reste récupérable). Passe le corps par un fichier temporaire ou un
here-doc pour préserver le Markdown :

```bash
gh issue edit <N> \
  --body-file - \
  --title "[BUG] Nouveau titre si changé" \
  --add-label enhancement --remove-label bug \
  --milestone "Refonte UX — 5 pôles (stats & résultats)" <<'BODY'
## 🎯 Contexte
…corps Markdown intégral re-rédigé…
BODY
```

- N'inclure `--title` / `--add-label` / `--remove-label` / `--milestone` **que** si tu
  changes réellement ces champs (sinon les omettre : `--body-file` seul suffit à
  réécrire le corps).
- **Ne crée aucun fichier `.md`** dans le dépôt : le ticket vit dans l'issue.
- Après édition, **affiche l'URL** de l'issue et un **résumé de ce qui a changé** (corps
  + éventuels titre/labels/milestone), en confirmant le numéro `#N`.
