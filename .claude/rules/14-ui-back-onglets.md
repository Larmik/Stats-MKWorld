# Bouton retour dans un conteneur d'onglets : ← ramène à l'onglet racine, ne quitte que depuis la racine

**Portée** : tout conteneur d'onglets / bottom navigation (`NavigationBar` + `NavHost`
imbriqué, ex. `HomeScreen`). S'applique à toute future barre d'onglets.

Comportement attendu du bouton retour système / `BackHandler` sur un conteneur à
onglets :

- Depuis un onglet **autre** que l'onglet racine (celui du `startDestination`) → ←
  **navigue vers l'onglet racine**, il **ne quitte pas** l'app.
- Depuis l'onglet **racine** (Accueil) → ← exécute le comportement de sortie
  (`onBack()` qui `finish()` l'activité).
- Un onglet déjà arrivé sur la racine via ← : le ← suivant quitte de nouveau (pas
  d'empilement multiple de la racine).

Implémentation (cf. `HomeScreen`) :

- Lire la route courante **au niveau de la fonction** (`currentBackStackEntryAsState`),
  pas seulement dans le scope `bottomBar`, pour que le `BackHandler` puisse la tester.
- `BackHandler { if (onRacine) onBack() else navigate(racine){…} }`.
- Naviguer vers l'onglet racine avec le même bloc que les items de la barre —
  `popUpTo(findStartDestination()){ saveState = true }` + `launchSingleTop = true` +
  `restoreState = true` — pour éviter d'empiler plusieurs instances de la racine et
  conserver l'état des onglets.
- **Attention aux écrans-onglets portant leur propre `BackHandler`** (ex.
  `PlayerProfileScreen` hébergé dans le pôle Profil) : leur `BackHandler` intercepte
  le ← en priorité. Leur passer en `onBack` la **même** navigation « retour à
  l'onglet racine » (pas le `onBack()` qui quitte), sinon l'onglet quitterait l'app
  au lieu de revenir à la racine.

```kotlin
val backToWelcome: () -> Unit = {
    navController.navigate(BottomNavItem.WELCOME.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
BackHandler {
    when (onWelcome) {   // onWelcome = route courante == racine
        true -> onBack()
        else -> backToWelcome()
    }
}
```
