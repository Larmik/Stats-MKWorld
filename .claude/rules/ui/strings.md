---
paths:
  - "app/src/main/res/values/strings.xml"
  - "app/src/main/res/values-fr/strings.xml"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/**"
---

# Chaînes utilisateur : ressources bilingues, pas de libellé en dur

- Toute chaîne affichée passe par une ressource `R.string` (`stringResource` dans un composable,
  id + arguments exposés par le VM, cf. `viewmodel/viewmodels.md`).
- Ajouter chaque nouvelle string dans les deux fichiers, avec la même clé :
  `res/values/strings.xml` en **anglais** (défaut) et `res/values-fr/strings.xml` en **français**.
- Un texte formaté (pourcentage, nombre) arrive déjà formaté en argument `%1$s`.
- Pas de libellé utilisateur en dur dans un composable, un VM ou une extension.
- **Exception** : l'écran debug (`screen/debug/DebugScreen.kt`, `DebugViewModel.kt`) peut garder
  ses libellés et toasts en dur.
