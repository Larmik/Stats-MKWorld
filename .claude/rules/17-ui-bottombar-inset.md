# Écrans de pôle bottom-nav : réserver une marge basse pour ne pas être masqué par la bottombar

**Portée** : tout écran hébergé dans un **pôle de la bottom navigation** (`HomeScreen`,
routes `Home/Welcome` · `Home/WarList` · `Home/Stats` · `Home/Rankings` · `Home/Profile`),
c'est-à-dire tout contenu affiché **au-dessus** de la `NavigationBar` du `Scaffold` de
`HomeScreen`.

La `NavigationBar` (bottombar) est posée par le `Scaffold` de `HomeScreen`, mais son
`innerPadding` n'est **pas** propagé au contenu (`@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")`).
Sans compensation, le **bas du contenu scrollable est masqué** par la bottombar (dernières
lignes/boutons inaccessibles).

**Règle** : tout écran de pôle dont le contenu est **scrollable** (`LazyColumn`,
`Column.verticalScroll`, …) doit **réserver une marge basse** équivalente à la hauteur de
la bottombar (**≈ 90 dp**, valeur déjà utilisée par `StatsFullScreen`) :

- `LazyColumn` → `contentPadding = PaddingValues(bottom = 90.dp)` (n'ajoute pas d'espace en
  haut, laisse le dernier item défiler au-dessus de la barre) ;
- contenu non-lazy → `Modifier.padding(bottom = 90.dp)` (cf. `StatsFullScreen` en mode
  `showTabs`).

Points d'attention :

- **Composant de contenu mutualisé** entre un pôle (avec bottombar) et un écran du graphe
  racine (poussé **par-dessus** le pôle, **sans** bottombar) : appliquer la marge dans le
  contenu commun reste correct — sur l'écran racine elle n'ajoute qu'un peu d'espace de
  défilement inoffensif. Cf. `PlayerProfileContent` / `TeamProfileContent` (pôle Profil +
  fiches autonomes `Player/Profile/{id}` / `Team/Profile/{id}`).
- Ne pas confondre avec le **padding haut** de `BaseScreen` (statusbar) : ici il s'agit
  uniquement du **bas**.
- Les écrans du **graphe racine** (détails de war, fiches adversaire/circuit…) n'ont pas de
  bottombar → pas de marge basse requise (mais l'hériter d'un contenu mutualisé est toléré).
