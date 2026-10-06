# Index des rules du projet

Ce fichier est **hors** de `.claude/rules/` : il n'est pas chargé automatiquement. Il sert au flux
`/ticket-dev` (agent `ticket-worker`) pour choisir quelles rules lire et où enrichir une rule.

## Chargement

- Un `.md` de `.claude/rules/` (sous-répertoires compris) **sans** frontmatter `paths` est chargé à
  chaque session ; **avec** `paths` (globs), il n'est chargé qu'au `Read` / `Write` / `Edit` d'un
  fichier correspondant, dans la session principale comme dans un sous-agent.
  La lecture via `cat`/`rg` dans Bash ne déclenche rien.
- `paths` est le seul champ lu ; un YAML invalide fait charger la rule sans condition.
- Aujourd'hui toutes les rules ont des `paths`. Le transverse (workflow, doc, langue) vit dans
  `CLAUDE.md`.

## Rules

Racine des globs Kotlin : `app/src/main/java/fr/harmoniamk/statsmkworld/` (abrégée `…/`).

| Fichier | Sujet | `paths` principaux |
|---|---|---|
| `ui/compose.md` | clés de liste, type de State, `collectAsStateWithLifecycle`, switch, wizard | `…/screen/**`, `…/ui/**` |
| `ui/components.md` | style établi, chercher l'existant, mutualiser, `MKButton` / `MKSegmentedSelector` / `MKStepper` uniques | `…/ui/**`, `…/screen/**` |
| `ui/cell-layout.md` | hauteur de cellule texte, alignements podium / blocs comparés | `…/ui/cells/**`, `…/ui/stats/**`, `…/screen/**/*Screen.kt` |
| `ui/roster-player-display.md` | roster vs équipe, `TeamEntity.unknown`, médaillon | `…/screen/**`, `…/ui/cells/**`, `…/ui/stats/**`, `WarExtension.kt`, `TeamEntity.kt` |
| `ui/bottom-nav.md` | retour dans les pôles, `BottomBarInset` | `…/screen/home/**` + écrans de pôle, `ui/Resources.kt` |
| `ui/strings.md` | strings bilingues, pas de libellé en dur (sauf debug) | `res/values*/strings.xml`, `…/screen/**`, `…/ui/**` |
| `stats/calculs.md` | justesse, 12p first, Top6/Bot6, pourcentages, intermission, classements | `…/extension/**`, `…/model/local/**`, `…/screen/stats/**`, `…/screen/welcome/**`, `…/screen/warList/**`, `…/ui/stats/**`, `…/ui/cells/**`… |
| `stats/shocks-bagging.md` | shock = éclair obtenu (hors score), bagging situationnel, pas de corrélation avec la position | `…/model/firebase/Shock.kt`, `…/model/local/**`, `…/extension/**`, `…/screen/stats/**`, `…/screen/currentWar/**`, `…/ui/stats/**`, `…/ui/cells/**`, `res/values*/strings.xml`… |
| `viewmodel/viewmodels.md` | Factory assistée, ordre d'init, recherche, ressources, `withContext(Default)` | `app/src/main/java/**/*ViewModel.kt`, `RootScreen.kt` |
| `viewmodel/navigation-filtres.md` | saison / `WarKindFilter` / 12p propagés aux enfants | `RootScreen.kt`, `HomeScreen.kt`, `**/*ViewModel.kt`, filtres `model/local/` |
| `data/repositories.md` | DI, `suspend` vs `Flow`, pas d'UI, `clear*`, réseau par élément, UseCase vs repository | `…/repository/**`, `…/datasource/**`, `…/usecase/**`, `…/worker/**` |
| `data/mkworld-only.md` | domaine mkworld, tournois officiels, endpoint liste sans joueurs, migration `teamId` → `rosterId` | `…/api/**`, `…/datasource/network/**`, `…/usecase/**`, `DiagnosticRepository.kt`, `TournamentRepository.kt`, `MKCTeamExtension.kt` |
| `data/firebase-users.md` | rôles membres / alliés (`role = 0`), `manageTransferts` | `FirebaseRepository.kt`, `DiagnosticRepository.kt`, `…/usecase/**`, `User.kt`, `PlayerEntity.kt`, VM profils / war en cours / AddWar, `…/screen/debug/**` |
| `data/room.md` | version, schémas, destructive migration | `…/database/**`, `…/datasource/local/**`, `app/schemas/**` |
| `build/release-securite.md` | R8 des modèles par réflexion, secrets, backup | `app/build.gradle.kts`, `proguard-rules.pro`, manifest, `res/xml/*rules.xml`, `…/model/**`, `…/serializers/**`… |
| `kotlin/style.md` | `?.let`, pas de `!!`, `var` capturée, saisie, fonctions locales, noms | `app/src/**/*.kt` |
| `kotlin/constantes-extensions.md` | principe « ≥ 2 sites », ancres de littéraux, placement des extensions | `app/src/**/*.kt` |
| `kotlin/commentaires.md` | commentaires courts, `#NN` et jamais de n° de rule, pas de code commenté | `app/src/**/*.kt` |
| `process/documentation.md` | tenue de l'audit (issue `/create-ticket` par entrée), références croisées | `docs/**`, `.claude/**`, `CLAUDE.md` |

## Format d'une rule

```markdown
---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/screen/**"
---

# <Titre>

- Consigne prescriptive (« Faire X », « Ne pas faire Y »), une idée par puce.
- Référence courte : `#NN` (ticket) ou audit `X12`, pas de récit.
```

- Français, ton neutre, moins de 60 lignes par fichier, au plus un exemple court par consigne.
- Globs : un par ligne, entre guillemets, chemins complets, **sans accolades**, vérifiés sur
  l'arborescence réelle.
- Un sujet par fichier ; un sujet lié aux mêmes fichiers rejoint le fichier existant.

## Enrichir une rule (retour utilisateur)

- Le retour recoupe une rule existante → la mettre à jour.
- Préférence générale et durable non couverte, dans une catégorie existante (`ui/`, `stats/`,
  `viewmodel/`, `data/`, `build/`, `kotlin/`, `process/`) → l'ajouter au fichier le plus proche ;
  créer un nouveau fichier dans le répertoire seulement si le sujet ou les `paths` diffèrent.
- Nouvelle catégorie (nouveau répertoire) → demander confirmation à l'utilisateur avant.
- Retour propre à un seul ticket → aucune rule.
- Toute création / renommage → mettre à jour cet index et `docs/AUDIT.md` § 9.
- Le code ne cite aucune rule (cf. `kotlin/commentaires.md`).
- En cas de conflit entre une rule et un ticket, le signaler au lieu de trancher.
