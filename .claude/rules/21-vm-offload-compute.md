# Déporter le calcul d'un ViewModel hors du thread UI : PROUVER sur device d'abord (le calcul de stats a une affinité main non résolue)

**Portée** : tout `ViewModel` de stats/classements/dashboard (`StatsFullViewModel`,
`StatsRankingViewModel`, `MapDetailViewModel`, `OpponentDetailViewModel`,
`WelcomeViewModel`, `WarListViewModel`) dont on voudrait déporter le calcul lourd hors
du collecteur (`viewModelScope` = `Main.immediate`).

## Fait établi (#73) : déporter ce calcul CASSE le dropdown de saison

Le ticket #73 (perf/fluidité) a tenté de sortir le calcul de stats du thread UI. **Deux
approches ont été essayées et ont TOUTES DEUX régressé** l'affichage : le sélecteur de
saison (`MKSeasonDropdown`) **disparaissait de tous les headers** (Accueil, Wars, Stats,
Classements).

1. `.flowOn(Dispatchers.Default)` sur la branche de calcul.
2. `withContext(Dispatchers.Default)` autour de la **seule** portion de calcul CPU (en
   laissant les lectures de sources et `seasons` sur le collecteur).

Le fait que **même `withContext` ciblé** — qui préserve pourtant l'ordre d'émission et
garde `seasons` peuplé dans le `State` — casse l'affichage prouve qu'il existe une
**affinité au thread principal NON RÉSOLUE** quelque part dans la chaîne de calcul (une
dépendance qui *throw* hors du main thread → la branche `combine`/`map` throw avant
d'émettre → `stateIn` reste bloqué sur son seed vide → `seasons = []` → dropdown masqué,
uniformément sur tous les headers). La source exacte de l'affinité **n'a pas été
identifiée** (le calcul `withFullStats`/`computeState`/`computeRankings` paraît pur, mais
un throw runtime survient off-main).

## Règle

- **Ne pas déporter le calcul de ces VM hors du collecteur sans (a) avoir identifié et
  corrigé l'affinité main-thread, ET (b) l'avoir VÉRIFIÉ SUR DEVICE** (dropdown présent +
  fluidité). `./gradlew compileDebugKotlin` ne suffit pas : la régression est **runtime**,
  invisible à la compilation. Ni `flowOn(Default)` ni `withContext(Default)` ne sont
  sûrs tant que l'affinité n'est pas levée.
- **Corollaire général `flowOn` vs `withContext`** (vrai indépendamment de #73) : si un jour
  on déporte un calcul VM, préférer `withContext(Dispatchers.Default)` autour de la **seule**
  portion CPU à un `flowOn` sur toute la chaîne. `flowOn` relocalise **tout l'upstream**
  (lectures Room/Firebase/DataStore incluses) et, sur une chaîne passant par `mergeWith`
  (`extension/FlowExtension.kt` = `flowOf(this, flow).flattenMerge()`, merge **non ordonné**),
  transforme l'émission déterministe en course cross-thread où l'état vide peut survivre.
  `withContext` cible le CPU sans toucher aux I/O ni à l'ordre d'émission. **Mais** #73 montre
  que `withContext` seul ne suffit pas ici : l'affinité main sous-jacente doit être levée en
  premier.
- **Priorité correction runtime > gain perf** : un freeze est préférable à un écran cassé
  (dropdown disparu). En cas de doute non vérifiable sur device, **laisser le calcul sur le
  collecteur** (comportement d'origine).

## Ce qui RESTE sûr (appliqué en #73)

La mémoïsation en composition n'a **aucun** rapport avec le threading VM et reste acquise :
les tris + conversions dérivés d'un `State` (ex. `sortedByDescending` + `toPodiumEntry` dans
`PlayerMapsRankingScreen`/`PlayerOpponentsRankingScreen`) sont enveloppés dans
`remember(sortIndex, source)` (rule 11) pour ne pas être recalculés à chaque recomposition.
C'est la seule optimisation conservée de #73 ; le déport de calcul VM a été **reverté**.
