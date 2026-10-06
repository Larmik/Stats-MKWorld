---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/home/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/welcome/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/warList/WarListScreen.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/full/StatsFullScreen.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/stats/ranking/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/profile/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/playerProfile/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/teamProfile/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/ui/Resources.kt"
---

# Bottom navigation : retour et marge basse des pôles

Pôles = routes `Home/Welcome`, `Home/WarList`, `Home/Stats`, `Home/Rankings`, `Home/Profile`
du `NavHost` imbriqué de `HomeScreen`.

## Retour système

- Depuis un pôle autre que la racine (`Home/Welcome`) : ← ramène à la racine, ne quitte pas.
- Depuis la racine : ← appelle `onBack()` (sortie de l'app).
- Lire la route courante au niveau de la fonction (`currentBackStackEntryAsState`) pour que le
  `BackHandler` la teste.
- Naviguer vers la racine avec le bloc des items de la barre : `popUpTo(findStartDestination())
  { saveState = true }` + `launchSingleTop = true` + `restoreState = true`.
- Un écran de pôle qui a son propre `BackHandler` (ex. `ProfileScreen`) reçoit en `onBack` la
  navigation « retour à la racine » (`backToWelcome`), jamais le `onBack()` qui quitte.

```kotlin
BackHandler {
    when (onWelcome) {
        true -> onBack()
        else -> backToWelcome()
    }
}
```

## Marge basse

Le `Scaffold` de `HomeScreen` ne propage pas son `innerPadding` : sans marge, le bas du contenu
scrollable d'un pôle est masqué par la `NavigationBar`.

- Réserver `BottomBarInset` (`ui/Resources.kt`) sous tout contenu scrollable de pôle :
  `contentPadding = PaddingValues(bottom = BottomBarInset)` sur un `LazyColumn`,
  `Modifier.padding(bottom = BottomBarInset)` sinon. Ne jamais recopier `90.dp`.
- Contenu mutualisé entre un pôle et un écran du graphe racine (`PlayerProfileContent`,
  `TeamProfileContent`) : la marge est tolérée.
- Écran du graphe racine (fiches adversaire/circuit, classements, détails de war) : pas de marge
  basse.
