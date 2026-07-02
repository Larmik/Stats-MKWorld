---
name: create-ticket
description: Génère un ticket prêt à coller sur Trello à partir d'une description de bug ou de feature. Le ticket contient toujours trois sections — Contexte, Description, et Solutions proposées — avec une mise en page Markdown lisible. À utiliser quand on veut transformer une idée de bug/feature en ticket structuré pour le board Trello du projet.
arguments: [description-bug-ou-feature]
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash(git log *), Bash(git diff *), Bash(pbcopy *), Task
---

# Création d'un ticket Trello

Description fournie en entrée : **$0**

Ton objectif : produire **un seul bloc Markdown final, prêt à être collé tel quel dans une carte Trello**, qui décrit proprement le bug ou la feature donné(e) en entrée.

## 1. Comprendre la demande

1. Si `$0` est vide, **arrête-toi** et demande à l'utilisateur la description du bug ou de la feature.
2. Détermine s'il s'agit d'un **bug** ou d'une **feature** (en cas de doute, demande, ou déduis-le du ton de la description).
3. **Enquête dans le code** avant d'écrire le ticket (sauf si la demande est purement organisationnelle) :
   - Pour un **bug** : localise le ou les fichiers concernés, comprends le flux de données, et formule des hypothèses sur la cause racine. Cite les fichiers en `chemin:ligne`.
   - Pour une **feature** : identifie où elle s'intègrerait (écran, ViewModel, repository…) et les contraintes existantes.
   - Pour une investigation large, délègue à un agent `Explore`.
4. Ne sur-investigue pas : l'objectif est un ticket actionnable, pas un audit complet. Quelques pistes solides valent mieux qu'une analyse exhaustive.

## 2. Rédiger le ticket

Respecte **exactement** cette structure (c'est le livrable). Garde un ton concis et factuel, en français.

````markdown
# <Titre court et explicite — préfixé par [BUG] ou [FEATURE]>

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

## 3. Règles de mise en page Trello

- Trello rend le **Markdown** : titres `#`/`##`, listes, **gras**, `code inline`, blocs de code triple-backtick, et cases à cocher `- [ ]`.
- Utilise des **cases à cocher** `- [ ]` pour les critères d'acceptation et les étapes de solution actionnables — elles deviennent cliquables dans Trello.
- Garde des paragraphes courts et aérés. Pas de tableaux complexes (Trello les rend mal).
- Les emojis de section sont volontaires : ils rendent la carte scannable.

## 4. Livraison

Affiche le ticket final **dans un bloc de code** (fenced ```` ``` ````) pour que l'utilisateur puisse le copier d'un seul geste.

**Copie systématiquement le ticket dans le presse-papiers** (macOS) une fois rédigé : passe son contenu intégral et brut (le Markdown, sans les backticks d'entourage) à `pbcopy`, par exemple via un here-doc :

```bash
pbcopy <<'TICKET'
<contenu Markdown intégral du ticket>
TICKET
```

Confirme ensuite à l'utilisateur que le ticket a été copié dans le presse-papiers.

**Ne crée JAMAIS de fichier** (`.md` ou autre) : la sortie reste exclusivement dans le chat. Écrire un fichier polluerait le dépôt inutilement.
