---
paths:
  - "app/src/**/*.kt"
---

# Extraction, constantes et placement des extensions

## Principe : extraire à partir de 2 appelants / sites

Vaut pour une constante, un helper privé, une extension, une méthode de UseCase et un composable.

- **Un seul** appelant / site → inliner (avec un commentaire si le littéral mérite une explication).
  Un one-liner trivial ne justifie pas un helper même appelé deux fois (ex. lectures
  `mkcPlayer.firstOrNull()?.id ?: 0L` laissées inline).
- **≥ 2** appelants / sites distincts → un seul exemplaire partagé, surtout si les sites doivent
  rester cohérents (même valeur à l'écriture et à la lecture).
- Déclinaisons : composables → `ui/components.md` ; UseCase / repository → `data/repositories.md`.

```kotlin
if (selected.size == 6) { … } // composition complète d'une war 6v6 (pas de const MAX_PLAYERS)
```

## Littéraux métier : réutiliser l'ancre existante

Un littéral métier présent à ≥ 2 sites ne reçoit pas de nouvelle copie. Ancres existantes :

| Valeur | Ancre |
|---|---|
| Barème, scores de référence, id debug | `ScoringConstants` (`model/ScoringConstants.kt`) |
| Préfixe d'URL MKCentral | `String.mkcentralUrl` (`extension/StringExtension.kt`) |
| Filtre `game == "mkworld"` | `MKCTeam.mkWorldRosters()` (`extension/MKCTeamExtension.kt`) |
| Marge bottombar `90.dp` | `BottomBarInset` (`ui/Resources.kt`) |
| Adversaire irrésoluble | `TeamEntity.unknown(id)` (`database/entities/TeamEntity.kt`) |
| Seuil des classements | `Stats.MIN_RANKING_SAMPLE` |

- Sans ancre (sentinelle allié `"-1"`, rôles `0/1/2`, test de mode `teamOpponent.size > 1`) :
  créer la constante / l'enum / l'extension, y faire pointer les sites touchés par le ticket, et
  signaler les autres dans l'audit (G2, G6, D28, D30).

## Placement des extensions

- Une extension va dans le fichier de son récepteur (`List<…>` → `extension/ListExtension.kt`,
  `String` → `StringExtension.kt`, `War` → `WarExtension.kt`, `Int` → `IntegerExtension.kt`,
  `Double` → `DoubleExtension.kt`, `Flow` → `FlowExtension.kt`, `MKCTeam` → `MKCTeamExtension.kt`).
  Nouveau `XxxExtension.kt` seulement pour un récepteur non couvert.
- Interdit : une extension top-level dans un écran, un modèle ou un fichier d'un autre récepteur
  (ex. les `toPodiumEntry()` des écrans, audit D36).
- Usage unique → pas d'extension : inline, ou fonction membre / top-level privée (pas de fonction
  locale, cf. `kotlin/style.md`).
- Exception : extension membre privée d'une classe qui capture son état (`private fun
  WarDetails.outcome()` dans `Stats`).
