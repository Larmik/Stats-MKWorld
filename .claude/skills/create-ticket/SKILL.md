---
name: create-ticket
description: Crée une issue GitHub structurée (Contexte / Description / Solutions proposées) sur le dépôt Larmik/Stats-MKWorld à partir d'une description de bug ou de feature. À utiliser quand on veut transformer une idée de bug/feature en ticket actionnable sur GitHub Issues (remplace l'ancien flux Trello).
arguments: [description-bug-ou-feature]
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash(git log *), Bash(git diff *), Bash(gh issue *), Bash(gh label *), Bash(gh api *), Agent, AskUserQuestion
---

# Création d'une issue GitHub

Description fournie en entrée : **$0**

Ton objectif : créer **une issue GitHub** propre et actionnable sur le dépôt
`Larmik/Stats-MKWorld`, décrivant le bug ou la feature donné(e) en entrée.

> La gestion des tickets se fait sur **GitHub Issues** (le board Trello est
> abandonné). Le dépôt est **public** : le contenu de l'issue est visible de tous
> — pas de secret (clé, token, chemin de keystore, id Discord réel…) dans le corps.

## 1. Comprendre la demande

1. Si `$0` est vide, **arrête-toi** et demande à l'utilisateur la description du bug ou de la feature.
2. Détermine s'il s'agit d'un **bug** ou d'une **feature** (en cas de doute, demande, ou déduis-le du ton de la description).
3. **Enquête dans le code** avant d'écrire le ticket (sauf si la demande est purement organisationnelle) :
   - Pour un **bug** : localise le ou les fichiers concernés, comprends le flux de données, et formule des hypothèses sur la cause racine. Cite les fichiers en `chemin:ligne`.
   - Pour une **feature** : identifie où elle s'intègrerait (écran, ViewModel, repository…) et les contraintes existantes.
   - Pour une investigation large, délègue à un agent `Explore`.
4. Ne sur-investigue pas : l'objectif est un ticket actionnable, pas un audit complet. Quelques pistes solides valent mieux qu'une analyse exhaustive.

## 2. Rédiger le corps de l'issue

Respecte **exactement** cette structure (c'est le corps de l'issue, sans titre H1 — le titre part dans le champ titre de l'issue). Ton concis et factuel, en français.

````markdown
## 🎯 Contexte
<2 à 4 phrases : où ça se passe dans l'app, dans quelles conditions, et pourquoi
ça compte. Donne l'environnement utile (écran, mode war 12p/24p, étape du flux…).>

## 🐛 Description / Comportement attendu
<Pour un BUG : comportement observé vs comportement attendu, étapes de
reproduction si connues, fréquence (systématique / intermittent), données
concrètes (ex: id joueur, valeurs).
Pour une FEATURE : ce qu'on veut, le besoin utilisateur, les critères
d'acceptation.>

## 🔍 Pistes techniques
<Optionnel mais recommandé pour un bug : fichiers et lignes suspectés
(`chemin:ligne`), hypothèses de cause racine classées de la plus probable à la
moins probable. Omettre cette section pour une feature simple.>

## ✅ Solutions proposées
<Liste numérotée de solutions concrètes. Pour chacune : ce qu'on change, où, et
le compromis (effort / risque / portée). Mets en avant la solution recommandée.>

## 📌 Notes
<Optionnel : effets de bord, points à valider, liens, dépendances.>
````

- GitHub rend le **Markdown** : titres, listes, **gras**, `code inline`, blocs de code, et **task lists** `- [ ]` (cases cliquables). Utilise `- [ ]` pour les critères d'acceptation et les étapes de solution actionnables.
- Paragraphes courts et aérés. Les emojis de section sont volontaires (issue scannable).

## 3. Titre & labels

- **Titre de l'issue** = titre court et explicite **préfixé `[BUG]` ou `[FEATURE]`**
  (le préfixe sert au nommage de branche par `/ticket-dev`).
- **Labels de type** : `bug` (bug) ou `enhancement` (feature).
- **Rattachement à l'epic refonte** : si le ticket concerne la refonte UX (navigation
  5 pôles / stats-résultats — cf. `docs/PROTOTYPE_UX.md`), ajoute le label `epic:refonte-ux`,
  le label de pôle concerné (`pole:accueil` | `pole:wars` | `pole:stats` |
  `pole:classements` | `pole:profil`), et le **milestone** « Refonte UX — 5 pôles ».
  Sinon, ne mets ni milestone ni label de pôle.
- En cas de doute sur le pôle/rattachement epic, demande à l'utilisateur (`AskUserQuestion`).

## 4. Livraison — créer l'issue

Crée l'issue via `gh` (le corps passe par un fichier temporaire ou un here-doc pour préserver le Markdown). **Toute issue est ajoutée au board `Stats MKWorld`** (projet qui représente l'app entière — tous les tickets y vont) via `--project "Stats MKWorld"` :

```bash
gh issue create \
  --title "[FEATURE] Titre court" \
  --body-file - \
  --project "Stats MKWorld" \
  --label enhancement --label epic:refonte-ux --label pole:stats \
  --milestone "Refonte UX — 5 pôles (stats & résultats)" <<'BODY'
## 🎯 Contexte
…corps Markdown intégral…
BODY
```

- `--project "Stats MKWorld"` est **systématique** (tout ticket, epic ou non).
- Pour un ticket **hors epic**, retire `--milestone` et les labels `epic:*`/`pole:*` (mais garde `--project` et le label de type).
- La nouvelle carte arrive sans **Status** (colonne « No Status » du board) : c'est normal, elle sera classée dans une colonne au démarrage.
- Après création, **affiche l'URL de l'issue** renvoyée par `gh` et confirme le numéro `#N`.
- **Ne crée aucun fichier `.md` dans le dépôt** : le ticket vit dans l'issue GitHub, pas dans un fichier versionné.
