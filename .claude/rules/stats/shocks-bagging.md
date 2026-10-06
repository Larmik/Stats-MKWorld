---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/Shock.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/WarTrack.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/local/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/extension/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/warList/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/currentWar/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/addTrack/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/editTrack/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/stats/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/cells/**"
  - "app/src/main/res/values/strings.xml"
  - "app/src/main/res/values-fr/strings.xml"
---

# Shocks (éclairs) et bagging

Détail fonctionnel : `docs/FUNCTIONAL.md` (glossaire, § « Le bagging et les éclairs »).

- Un `Shock` (`model/firebase/Shock.kt`, `count` par joueur et par course) est un objet **Éclair
  obtenu**. Ce n'est pas une pénalité (≠ `WarPenalty`) et il n'entre jamais dans le score.
- Libellés utilisateur : un éclair est « obtenu », jamais « joué » (#87). Les identifiants
  techniques (`shock`, `shockCount`…) restent inchangés.
- Bagging = rester derrière pour farmer des objets ; le nombre d'éclairs mesure la qualité du
  bagging, activité aussi importante que rouler devant.
- Le bagging est **situationnel** : pas de baggeur attitré, les rôles changent en course. Ne
  jamais modéliser de rôle fixe (baggeur / front-runner) d'un joueur.
- Aucune corrélation shocks ↔ position finale : ne jamais construire de stat ni d'insight qui la
  suppose. Position moyenne, Bot 6 et shocks ne se lisent pas « bon / mauvais » pour un joueur qui
  bag ; l'évolution des shocks s'affiche sans couleur.
