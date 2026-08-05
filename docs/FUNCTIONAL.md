# Documentation fonctionnelle — Stats MKWorld

> Application mobile de suivi des statistiques de *wars* (matchs d'équipe) pour **Mario Kart World**.
> Ce document décrit l'application du point de vue de l'utilisateur, écran par écran, avec les règles métier encodées. Volet architecture : [TECHNICAL.md](TECHNICAL.md).

## Sommaire

1. [À qui s'adresse l'application](#1-à-qui-sadresse-lapplication)
2. [Concepts clés & vocabulaire](#2-concepts-clés--vocabulaire)
3. [Rôles & permissions](#3-rôles--permissions)
4. [Onboarding & connexion](#4-onboarding--connexion)
5. [Accueil & navigation](#5-accueil--navigation)
6. [Cycle de vie d'une war](#6-cycle-de-vie-dune-war)
7. [Historique & détails](#7-historique--détails)
8. [Statistiques](#8-statistiques)
9. [Annuaire & profils](#9-annuaire--profils)
10. [Tableau partageable (PDF)](#10-tableau-partageable-pdf)
11. [Paramètres, données & mode debug](#11-paramètres-données--mode-debug)
12. [Récapitulatif des règles métier](#12-récapitulatif-des-règles-métier)

---

## 1. À qui s'adresse l'application

Aux **équipes compétitives de Mario Kart World** qui disputent des *wars* et veulent en conserver une trace structurée et des statistiques détaillées (par joueur, équipe, adversaire, circuit).

Prérequis utilisateur :
- Être inscrit sur **MKCentral** (registre communautaire des joueurs/équipes MK).
- Avoir un **compte Discord lié à MKCentral** (utilisé pour l'identification).

---

## 2. Concepts clés & vocabulaire

| Terme | Définition |
|---|---|
| **War** | Un match entre équipes, composé d'une série de courses (12 en général). |
| **12 joueurs** | Format 6 v 6 : l'équipe affronte **un** adversaire. |
| **24 joueurs** | Format tournoi : l'équipe affronte **trois** équipes adverses ; les scores finaux sont saisis manuellement. |
| **Course / Track** | Une course sur un circuit, avec les positions d'arrivée des joueurs. |
| **Intermission** | En 24 joueurs, segment alternatif du monde ouvert enchaîné à un circuit (un track peut alors porter 2 circuits). |
| **Position → points** | Chaque place d'arrivée rapporte des points (1ʳᵉ = 15 pts). |
| **Pénalité** | Retrait de points (−10, −15, −20) imputé à une équipe. |
| **Shock** | L'objet **éclair** récupéré en jeu, stratégiquement décisif. Compté par joueur sur une course pour produire des statistiques dédiées (n'affecte pas le calcul du score). |
| **Équipe** | Entité MKCentral complète (identifiant `teamId`), pouvant regrouper plusieurs rosters. Les joueurs y sont rattachés. |
| **Roster** | Composition inscrite sur MKCentral (identifiant `rosterId`) ; une équipe peut en avoir plusieurs. C'est à ce niveau que se rattachent les wars. |
| **Allié** | Joueur de renfort hors roster officiel (rosterId interne `-1`), pouvant participer aux wars. |
| **Multi-roster** | Option : calculer les stats sur tous les rosters de l'équipe ou seulement le sien. |
| **Mode matrix** | Mode debug permettant de simuler les données d'un autre joueur. |

**Scoring synthétique :**
- **12 joueurs** : 82 points distribués par course ; score adverse = 82 − score équipe ; total war 12 courses = 984 (équilibre à 492). L'affichage met en avant l'**écart** (`+/−`) par rapport à l'équilibre.
- **24 joueurs** : 144 points par course ; total war 12 courses = **1728** (valeur de contrôle à la saisie des scores). L'affichage montre les **scores absolus** (pas d'écart `+/−`), le classement se faisant entre 4 équipes.

Détails du barème position→points : voir [TECHNICAL.md §8](TECHNICAL.md#8-algorithmes-de-scoring).

---

## 3. Rôles & permissions

Le rôle est un entier stocké par joueur :

| Rôle (`role`) | Niveau | Peut faire |
|---|---|---|
| **2** | Leader / Manager | Tout : créer des wars, gérer rosters & alliés, changer les rôles des membres |
| **1** | Admin | Créer/gérer des wars, basculer le rôle d'un membre |
| **0** | Membre | Consulter, participer aux wars (pas de création) |

Le rôle ne dépend que de la gestion explicite des rôles (et du statut leader/manager détecté dans le roster MKCentral) : **le cycle de vie d'une war — création, validation, annulation, remplacement de joueur — ne le modifie jamais** (cf. audit B10). Un **allié** (joueur hors équipe) a toujours le rôle `0` : ne faisant pas partie de l'équipe, il ne peut pas la modérer.

Gating concret :
- Le bouton **« Nouvelle war »** n'apparaît que si `role > 0` **ou** le mode matrix est actif (et qu'aucune war n'est en cours).
- Le bouton **« Ajouter en allié »** apparaît si le joueur consulté n'est pas dans l'équipe, n'est pas soi-même, et n'est pas déjà allié.
- Le bouton **« Basculer le rôle »** apparaît si un leader consulte un membre de l'équipe qui n'est ni lui-même ni déjà leader.
- L'accès à l'**écran debug** est réservé (utilisateur de référence id `18595` ou mode matrix actif).

---

## 4. Onboarding & connexion

Écran `Signup` : un pager de 7 pages (`TutorialItem`) piloté par `SignupViewModel` (état `currentPage`, `code`).

| Page | Contenu | Action |
|---|---|---|
| **START** | Présentation de l'app et des prérequis (MKCentral + Discord lié) | Continuer |
| **OPEN_APP** | Autoriser l'ouverture des liens `statsmkworld.com` (Android 12+) | Ouvrir les réglages |
| **NOTIFICATIONS** | Autoriser les notifications (Android 13+) | Activer (déclenche la demande de permission) |
| **AUTH** | Connexion Discord (OAuth2) | Se connecter (ouvre l'autorisation Discord) |
| **FIND_PLAYER** | « Récupération de ton profil… » (fetch MKCentral) | Auto |
| **WELCOME** | Succès — redirection auto vers l'accueil | Auto (~2 s) |
| **ERROR** | Échec : Discord non lié à MKCentral / erreur serveur / réseau | Réessayer (revient à AUTH) |

**Flux technique** : au retour OAuth (deep link portant `code`), l'app échange le code contre un token, récupère l'utilisateur Discord, retrouve le joueur sur MKCentral via son `discord_id`, effectue une **connexion anonyme Firebase** (UID technique pour autoriser l'accès RTDB, transparent pour l'utilisateur ; échec réseau non bloquant), puis enchaîne `fetchData` (joueur → équipe → alliés → équipes → wars). Le joueur est enregistré en DataStore et un `User` Firebase est créé (role 0, sauf si leader détecté dans le roster). La connexion anonyme est aussi re-tentée à chaque démarrage si l'UID a été perdu (ex. après réinstallation).

---

## 5. Accueil & navigation

`HomeScreen` = conteneur à **cinq pôles** (barre du bas — `Accueil · Wars · Stats · Classements · Profil`), avec conservation d'état entre onglets (`saveState`/`restoreState`). Chaque pôle est une destination du `NavHost` imbriqué de `HomeScreen`. L'**Annuaire** n'est plus un onglet : il est accessible via une **icône recherche** (loupe) dans l'app bar des écrans Accueil et Classements (route `Home/Registry` du graphe racine). Le graphe racine (`RootScreen`) conserve `startDestination = Signup` et les deep links Discord (`statsmkworld.com?...=code`) inchangés.

### Pôle 1 — Accueil (`WelcomeScreen`) — tableau de bord
État : `teamName/teamLogo`, `playerName/playerLogo`, `currentWar`, `playerStats` + `teamStats` (les **deux** vues 12p, calculées d'emblée par le VM), `recentResults` (3 dernières wars 12p).

L'accueil est un **dashboard** qui met l'essentiel à portée immédiate. **Rendu pixel-perfect** : `WelcomeScreen` reproduit fidèlement le style de la maquette du prototype UX (`docs/prototype/stats-mkworld-5poles.html`, vue `home`) — cartes sombres translucides (fond `rgba(60,64,67,.5)`, bordure blanche, radius 6), eyebrows majuscules, pastilles V/N/D (vert/blanc/rouge), sparkline teintée selon la tendance à aire dégradée, segmentés stylés, bannière « En direct » verte, bandeau série avec flamme colorée, pastilles adversaire colorées. Ce rendu est la **norme** de la refonte (rules 13/15 : pixel-perfect exigé pour tout écran impacté, `WelcomeScreen` = rendu de référence). Les données affichées restent **réelles** (aucune valeur de démo codée en dur). Sections dans l'ordre (calcul 12p uniquement ; le support 24p relèvera d'un ticket dédié) :

1. **Carte de salutation** (cliquable → **Profil**) : pastille/avatar du joueur, « Salut, <prénom> », sous-titre « <équipe> · voir mon profil → ». Sous la carte, un **segmenté `Moi` / `Équipe`** (`Moi` actif par défaut, état UI `rememberSaveable`) **pilote la vue** des stats du dashboard (Momentum + Chiffres clés). Les deux jeux de stats étant précalculés par le VM (`playerStats` avec `userId` = id MKCentral du joueur courant ; `teamStats` avec `userId = null`), le basculement ne déclenche **aucun recalcul**.
2. **War en cours** — bannière « En direct · N joueurs » (dégradé vert, bordure verte) cliquable (→ reprend la war courante), affichée seulement si `currentWar != null`. Le corps réutilise `CurrentWarCell` (données réelles).
3. **Momentum** (reflète le profil sélectionné) : segmenté **`5 dernières` / `10 dernières`** pilotant la fenêtre ; bande de forme en **pastilles V/N/D** (`Stats.chronologicalOutcomes.takeLast(n)`) ; **sparkline** à aire dégradée des scores de la fenêtre (`Stats.scoreTimeline.takeLast(n)`, tracé Compose `Canvas`), **teintée selon la tendance** (vert si delta ≥ 0, rouge sinon) + delta de forme (flèche ↗/↘, winrate de la fenêtre `recentForm5`/`recentForm10` vs all-time), coloré vert/rouge.
4. **Chiffres clés** (reflète le profil sélectionné) : winrate (`allTimeForm.winrate`) · score moyen · 3ᵉ colonne, toutes valeurs en blanc. En vue **Moi** : score = score brut du joueur (`averagePoints`), 3ᵉ = position moyenne (`averagePlayerPosLabel`). En vue **Équipe** : score = écart moyen (`averagePointsLabel`), 3ᵉ = % de manches gagnées (`mapsWon`).
5. **Bandeau highlight — série en cours** (affiché si `currentStreak != 0`) : « Série de N victoires/défaites » + « En cours — record : M » (`bestWinStreak` / `worstLossStreak`). Icône flamme (`ic_flame`) teintée **verte** (série de victoires) ou **rouge** (série de défaites), dans un cercle assorti.
6. **Derniers résultats** : 3 wars 12p (`recentResults`, cliquables → détail de war) + lien **« Voir tout »** → pôle Wars (historique). Réutilise la **cellule `WarCell` unifiée** (voir ci-dessous).

- **Icône recherche** (app bar) → Annuaire.
- Le **sélecteur 12/24 joueurs** et le bouton **« Nouvelle war »** ne figurent plus sur l'accueil : ils vivent désormais dans le **pôle Wars** — le CTA « Nouvelle war » sur `WarListScreen`, le segmenté 12/24 en tête de `AddWarScreen` (cf. §6). Les destinations de navigation `Home/AddWar/{is24p}` restent inchangées.

> **Cellule `WarCell` unifiée** : la cellule de résultat (`ui/cells/WarCell.kt`) est **partagée** par l'Accueil, l'historique (`WarListScreen`) et les stats (`StatsScreen`), signature publique inchangée. Elle rend **12p** avec le style pixel-perfect de l'Accueil (pastille V/N/D, pastille adversaire, « vs … » + date, score + écart + **maps gagnées**) et **24p** avec le podium des 3 équipes (style minimal, non régressé). L'ancienne implémentation dédoublée a été supprimée.

### Pôle 2 — Wars (`WarListScreen`)
Point d'entrée unifié du domaine « match ». Barre d'app : titre **WARS** + sous-titre **« N wars »** (total affiché). L'écran enchaîne, de haut en bas :

1. **War en cours / création** (règle métier existante) :
   - si une war est en cours (écoutée en temps réel via `FirebaseRepository.listenToCurrentWar`, semée par `getCurrentWar`) → **bannière « En direct »** cliquable (composant partagé `CurrentWarBanner`, cf. ci-dessous) portant l'appel à l'action **« Reprendre — N courses jouées »** → `CurrentWarScreen` ;
   - sinon → bouton **« Nouvelle war »** → `AddWarScreen` (le CTA est masqué tant qu'une war est en cours).
2. **Chips filtre de résultat** : `Tous` (actif par défaut) / `Victoires` / `Nuls` / `Défaites`. Filtre purement UI (état `rememberSaveable`), sur le signe de la marge de score (`WarDetails.scoreMargin`, 12j comme 24j).
3. **Historique complet**, groupé par mois (en-têtes collants), triés du plus récent au plus ancien. **Tous les modes sont mélangés (12j ET 24j)** — l'ancien filtrage par mode (`is24PEnabled`) a été retiré. Clic sur une war → détail de war. Réutilise la cellule `WarCell` unifiée.

> **Composant `CurrentWarBanner` partagé** (`ui/cells/CurrentWarBanner.kt`) : la bannière « War en cours » (dégradé vert, pastille « En direct », corps = `CurrentWarCell`) est extraite de l'Accueil et **partagée** par l'Accueil et le pôle Wars. Un paramètre `withPlayers` choisit le libellé de pastille (« En direct · N joueurs » côté Accueil, « En direct » côté Wars) et un `callToAction` optionnel affiche « Reprendre — N courses jouées » côté Wars.
>
> **Cellule `CurrentWarCell` restylée** (`ui/cells/CurrentWarCell.kt`) : alignée sur le style des cellules de résultat (`WarCell12p`) — carte `blackAlphaed` + bordure, pastille adversaire (avatar équipe ou tag), « vs … », score + écart — et affiche en sous-ligne le **nombre de courses restantes** (`12 − courses jouées`). En 24p, podium des 3 logos + score de l'hôte + courses restantes. Le « courses restantes » (cellule) et le « N courses jouées » du `callToAction` (bannière, côté Wars) sont complémentaires et cohérents (jouées + restantes = 12).

### Pôle 3 — Stats (`StatsFullScreen`)
Écran riche à **onglets Individuelles / Équipe**, au niveau maquette (pôle Stats du prototype UX). **12p uniquement** pour l'instant : le sélecteur 12 j / 24 j et le comparatif 12/24 sont **temporairement retirés** (réintégration prévue au ticket #37).

- **En-tête** : **photo de profil du joueur** (Individuelles) ou **logo de l'équipe** (Équipe) — vignette MKCentral, fallback initiales/`default_logo` — + nom + sous-titre (« Tes performances · N wars » / « Performances collectives · N wars »). Pas de nombre de courses.
- **Bilan** : gros winrate + V/N/D + barre proportionnelle. Le décompte V/N/D **et** le nombre de wars sont calculés sur la portée affichée : en Individuelles seules les wars où le joueur a joué, en Équipe toutes les wars de l'équipe.
- **Indicateurs** (Individuelles) / **Détails équipe** (Équipe) : **grille régulière** de tuiles **toutes de la même taille** (la place de la ligne de progression **et** du libellé sur 2 lignes est réservée en permanence → aucune tuile ne change de taille, ni au changement de fenêtre ni selon la longueur du libellé), avec **sélecteur all-time / 5 dernières / 10 dernières** qui recalcule la section, et **progression en %** (delta vs all-time, flèche ↗/↘ colorée) sur les métriques comparables. Distinction stricte : Individuelles = **points/war** + position ; Équipe = **écart de points** (le « Score moyen » affiche une différence, pas le total) + score moyen/manche. Tuiles communes : winrate, maps gagnées, régularité, marges V/D, **pénalités** (points perdus), shocks/war. **Position moyenne** et **pénalités** se mettent à jour avec la fenêtre. Valeurs en blanc, seuls les deltas colorés.
- **Contribution** (Individuelles) : % des points de l'équipe + rang de contributeur.
- **Forme & séries** (série en cours + forme sur 10 wars) puis **Records & séries** : **grille 3 lignes × 2 colonnes** avec son propre **sélecteur all-time / 5 / 10** — ligne 1 amplitude (**score min | score max**), ligne 2 (**record V | record D**), ligne 3 (**Top 6 | Bot 6**).
- **Répartition des positions** : **sélecteur all-time / 5 / 10** (recalcul par fenêtre) + barres P1→P12 **ancrées sur une ligne de base commune** (labels alignés) + pied Top6/Bot6 avec %.
- **Podium circuits** et **Podium adversaires** : Top 3 / Flop 3 (**chacun sur une ligne**, 3 cellules) + sélecteur **Occurrences (défaut) / Winrate / Score**. « Occurrences » classe par nombre de fois joué (circuit) / de confrontations (adversaire) — Top 3 = les plus joués, Flop 3 = les moins joués. Les deux podiums partagent la **même cellule**, qui reprend **toutes** les infos des cellules historiques (image, nom, puis *nb de fois joué / confrontations*, *winrate*, *score équipe ou position/score joueur*) ; seule l'image change (illustration de circuit vs logo d'équipe). Perspective joueur (score du joueur) en Individuelles, équipe (écart) en Équipe.
- **Contributeurs** (Équipe) : **sélecteur all-time / 5 / 10** + mini-classement du roster recalculé par fenêtre (% de points + winrate, « toi » mis en évidence).

> Les sections « Rythme de war », « Comparatif 12/24 » et l'accordéon « Indicateurs avancés » ont été retirés : leurs indicateurs sont surfacés ailleurs (régularité, marges, pénalités → Indicateurs ; amplitude, records, invaincu → Records), **sauf la position moyenne 1ʳᵉ/2ᵉ moitié de war** qui disparaît avec le rythme (choix produit assumé).

> **`statsfull` — vue « pour un joueur donné »** : même rendu que l'onglet Individuelles, paramétré par `userId` (`StatsFullScreen(showTabs = false)`, route `Statsfull/{userId}`), avec barre de retour et sous-titre = nom du joueur. Mutualisé avec la vue Individuelles. Les **points d'entrée** (Classements onglet Joueurs #26, fiche joueur « Voir ses statistiques ») relèvent d'autres tickets ; la route réutilisable est déjà en place.
>
> **Saisons masquées** : les libellés de saison (« Record 8 · S2 25 ») dépendent du ticket #30 (non livré) → non affichés (« record 8 » sans suffixe).

### Pôle 4 — Classements (`StatsRankingScreen`)
Écran **unique à sous-onglets** `Joueurs / Adversaires / Circuits` (`MKSegmentedSelector`), **sans menu intermédiaire** (l'ancien `StatsMenuScreen` a été supprimé). Titre **CLASSEMENTS**, hint « Palmarès triable. Touche une ligne pour ouvrir sa fiche statistique. » Chaque onglet propose : **recherche par nom**, **tri à 3 chips** (le chip d'**occurrences en 1ʳᵉ position et sélectionné par défaut**, tri décroissant), **curseur « occurrences minimum »**, et une **grille de cellules podium** (`PodiumCell` mutualisée avec le pôle Stats, **texte noir** sur cet écran clair) — avatar + nom + 3 lignes (occurrences / winrate / score moyen) :
1. **Joueurs** — chips `Wars (défaut) / Winrate / Score moy.`. Liste **sectionnée** en **Membres** (joueurs de l'équipe) et **Alliés** (deux en-têtes). Cellule = pastille d'initiales + nom. Ligne → stats joueur (`StatsType.PlayerStats`).
2. **Adversaires** — chips `Occurrences (défaut) / Winrate / Score moy.`. Champ « Rechercher une équipe ». Les wars étant rattachées au **rosterId** adverse, le classement compte **un item par roster** (nom/tag du roster, avatar de l'équipe) ; les wars legacy restent sous un item de niveau équipe. Ligne → **fiche détail adversaire** (`Opponent/{teamId}`, cf. plus bas).
3. **Circuits** — chips `Fréquence (défaut) / Winrate / Score moy.`. Champ « Rechercher un circuit ». Cellule = illustration du circuit + nom. Ligne → **fiche détail circuit** (`Map/{trackIndex}`, cf. plus bas).

**Curseur « occurrences minimum »** (`Slider`, état réactif) : filtre la liste sur le nombre de matchs (**wars** pour Joueurs/Adversaires, **maps jouées** pour Circuits). Min = 1, max = le plus haut compteur de l'onglet courant ; seules les entrées à `occurrences ≥ valeur` sont affichées. Ce filtre utilisateur **remplace, pour l'affichage**, l'ancien seuil fixe : il n'y a plus de carte « En bref » ni de relégation automatique — l'utilisateur choisit lui-même l'échantillon minimum. Le curseur est masqué s'il n'y a rien à filtrer (max ≤ 1). La constante `Stats.MIN_RANKING_SAMPLE` reste utilisée par les **podiums du pôle Stats** (calculs de biais), pas par cet écran.

**Divergence assumée vs prototype** : la maquette prévoit une carte « En bref » (On domine / Bête noire ; Meilleur / Pire) sur les onglets Adversaires et Circuits. Elle a été **retirée sur décision explicite de l'utilisateur** (remplacée par le curseur d'occurrences), au profit d'un contrôle direct de l'échantillon.

Perspective : Joueurs = par joueur ; Adversaires / Circuits = **winrate global de l'équipe** (le prototype n'a pas de switch individuel/équipe sur les Classements). Depuis une ligne Adversaire/Circuit, la navigation ouvre désormais la **fiche dédiée** correspondante (#27, cf. ci-dessous) ; la ligne Joueur ouvre l'écran `Stats` (`StatsType.PlayerStats`).

#### Fiches détail Adversaire & Circuit (#27)

Fiches profil « page équipe » (pattern apps sportives), atteintes depuis les Classements. Rendu **pixel-perfect** de la maquette (écrans `opp` / `map`), cartes translucides mutualisées (`ui/stats/MKStatCard.kt` : `StatCard`, `StatHeaderCard`, `BalanceCard`, `WinTieLossBar`, `StatTiles` — extraites de `StatsFullScreen`, rule 16). **12p uniquement**, données réelles. Chaque fiche présente **toutes les données détaillées** de l'écran Statistiques scopées à son entité.

**Sélecteur Indiv / Équipe** (les deux fiches) — `MKSegmentedSelector` partagé (rule 15), libellés courts **« Joueur » / « Équipe »**, état **réactif** du ViewModel (rule 11 : `MutableStateFlow` basculé par `onModeChange`, l'écran reste monté, pas de re-navigation). **Mode initial semé par le contexte d'ouverture** : `OpponentStats`/`MapStats` portent un `userId` (nullable) passé dans la route (`…/{userId}`, arg **nullable** — le littéral « null » est parsé en `null` par `StringType` ⇒ mode Équipe). Les sections réagissent au mode, **sauf** celles explicitement figées ci-dessous.

- **Fiche adversaire** (`OpponentDetailScreen`, route `Opponent/{teamId}/{userId}`, `OpponentDetailViewModel`) : en-tête (nom/tag du roster + avatar de l'équipe, rule 12 ; nb de confrontations + dernière rencontre), **Bilan face à eux** (winrate **coloré selon seuil** — rouge < 50 %, blanc = 50 %, vert > 50 % — + V/N/D + barre), **5 dernières face à eux** (pastilles V/N/D), **Séries & scores** — grille **3 lignes × 2 cellules** : L1 = *Score/diff* (mode-dépendant : Équipe = différence moyenne signée pour − contre ; Indiv = score moyen du joueur) · *Série en cours* ; L2 = *Record série de victoires* · *Record série de défaites* ; L3 = *Shocks joués* · *Shocks/War* (ratio), les deux cellules de la L3 portant l'**illustration shock** à gauche (centrée verticalement) ; **Circuits contre eux** (podium Top3/Flop3 + **sélecteur Occurrences / Winrate / Score moy.** + « **Voir le classement en entier** » → `OpponentTracksRankingScreen`), **sections détaillées** (répartition des positions, Top/Bot 2→6), **Historique des wars** (`WarCell` → `WarDetailsScreen`). Réutilise `withFullStats(teamId=…, userId=…)`.
- **Fiche circuit** (`MapDetailScreen`, route `Map/{trackIndex}/{userId}`, `MapDetailViewModel`) : en-tête (illustration + nom + « joué N fois » ; **plus d'icône/label de coupe**), **Performance** (winrate de manche **coloré selon seuil** + V/N/D + barre), **Scores moyens** : *Score équipe* et *Ta position moyenne* **FIXES** (indépendants du mode — toujours score d'équipe + position du joueur courant) + *Shocks joués* **DYNAMIQUE** (suit le mode), **sections détaillées** (répartition des positions, Top/Bot 2→6, mode-scopées), **Pilotes sur ce circuit** (podium Top3/Flop3 **trié ET affiché par score perso moyen** — critère trié = critère affiché ; position moyenne et nb de manches jouées en infos secondaires ; **membres uniquement — alliés exclus** ; **seuil `MIN_RANKING_SAMPLE`** de manches sur le circuit + « **Voir le classement en entier** » → `MapPilotsRankingScreen`) — **affiché en mode Équipe uniquement** (masqué en Indiv). S'appuie sur `MapStats`.

**Sections détaillées communes** (`ui/stats/MapStatsSections.kt`, `mapStatsDetailSections(MapStats)`) — mêmes calculs/rendus que `StatsFullScreen`, scopés à l'entité ET au mode : **Répartition des positions** (histogramme P1→P12 — positions du joueur en Indiv, de l'ÉQUIPE en Équipe — + pied Top6/Bot6 %, `DistributionChart`/`DistributionFooter`) et **Top / Bot** (compteurs Top 2→6 et Bot 2→6, `TopBottomColumns`). Masquées si vides.

**Classements complets** (« Voir le classement en entier », **texte des cellules en noir** sur les deux) : `OpponentTracksRankingScreen` (circuits contre l'adversaire, **même sélecteur de tri** Occurrences / Winrate / Score moy. que la fiche et l'écran Classements) et `MapPilotsRankingScreen` (pilotes membres sur le circuit, tri ET affichage par score perso moyen, seuil `MIN_RANKING_SAMPLE`). Rendus via la **grille de podiums mutualisée** `ui/stats/PodiumGrid.kt` (`podiumRows`, extraite de `StatsRankingScreen`, rule 16), en réutilisant le **même ViewModel** que la fiche (mêmes données, même mode, même tri).

Le routage par type se fait dans `RootScreen` (`onStats` dispatche `OpponentStats`→`Opponent/{teamId}/{userId}`, `MapStats`→`Map/{trackIndex}/{userId}`, autres→`Stats`).

**Note calcul** : distinction stricte **score vs position** pour les circuits — la « position moyenne » est la position réelle (1..12) moyenne (du joueur via `MapStats.averagePlayerPosLabel`, de l'équipe via `MapStats.teamAveragePosition`), jamais un score. Les **shocks** sont les objets éclair **joués** (`Shock.playerId` = joueur qui joue l'éclair ; filtrés par joueur en Indiv, tous les joueurs de l'équipe hôte en Équipe). Le winrate coloré selon seuil réutilise `winrateColor()` de `ui/stats/MKStatCard.kt`.

**Écart résiduel documenté** : pas de flèche `←` visible dans l'app bar (`BaseScreen` n'en propose pas) : retour par geste/bouton système (`BackHandler`), comme `StatsFullScreen`/`WarDetailsScreen`.

### Pôle 5 — Profil (`ProfileScreen`, onglets fusionnés Joueur / Équipe)
Profil unique du joueur courant / de mon équipe (`me`), **à onglets fusionnés** (écran `profile` du prototype, ticket #28), **rendu pixel-perfect** vis-à-vis de la maquette (rules 13/15). Un seul `BaseScreen` (titre **Profil**), un **segmented partagé** `Joueur` / `Équipe` (`MKSegmentedSelector`) qui bascule l'onglet **dynamiquement** (état interne réactif, sans re-navigation — rule 11). Le contenu de chaque onglet **réutilise** le contenu existant des fiches profil, restylé au niveau maquette via des **composants profil mutualisés** (`ui/cells/ProfileCells.kt` : `ProfilePersonCard`, `ProfileInfoCard`, `ProfileMemberRow`, `ProfileSettingRow`, `RolePill`, `MkcBadge`) :

- **Onglet Joueur** (`PlayerProfileContent`) : **carte identité** centrée (avatar rond 76dp / pastille d'initiales, nom Bungee, pays + **pastille de rôle** Membre/Admin/Leader, bio italique, **badge « Profil MKCentral »**) ; **carte Informations** (grille 2 colonnes : Équipe + tag, Membre depuis **[date exacte jj/mm/aaaa]**, Code ami, Discord, Inscription **[date exacte]**, Rôle) ; boutons de règles métier (fiche d'un autre joueur — « Ajouter en ally », « Changer le rôle ») **en largeur intrinsèque, centrés** ; **carte Réglages** (lignes `setrow` avec **icône de tête** : Rafraîchir, Notifications + toggle, Multi-roster + toggle si ≥ 2 rosters, entrée **Debug** si joueur 18595 / mode matrice, **Déconnexion** en rouge) ; **ligne version** « Stats MKWorld · vX » + dernière synchro.
- **Onglet Équipe** (`TeamProfileContent`) : **carte identité** équipe (logo / pastille de tag, nom, `TAG XX · créée le jj/mm/aaaa`, bio, **badge « Équipe MKCentral »**) ; **carte Informations** (Membres, Alliés, Créée le **[date exacte]**) ; **sous-onglets `MKSegmentedSelector`** Membres / Alliés (remplaçant l'ancien `MKSelectorViewPager`, conformément au style pill de la maquette) ; **lignes membres** (`ProfileMemberRow` : **photo de profil MKCentral** du membre — fallback initiales colorées par roster — nom, **pastille de rôle réel** [nœud Firebase `users` : Leader=2 / Admin=1 / Membre=0], chevron → fiche joueur), **regroupées par roster** (un en-tête par roster) **si l'équipe a ≥ 2 rosters** mkworld, sinon liste plate ; onglet Alliés = bouton « **Ajouter un ally** » **en largeur intrinsèque, centré** (sheet hébergé par l'écran) + lignes alliés (« roster externe »).

Pastilles de rôle (couleurs de la maquette) : **Leader** = or (`gold`), **Admin** = bleu, **Membre / Ally** = blanc translucide. Le **rôle réel** provient du nœud Firebase `users` (un allié vaut toujours 0) ; pour une équipe publique (sans nœud `users`), repli sur les indicateurs MKCentral leader/manager.

Le contenu scrollable réserve une **marge basse** (≈ 90 dp) pour ne pas être masqué par la bottombar (cf. rule `.claude/rules/17-ui-bottombar-inset.md`).

**Fiches profil autonomes** (atteintes depuis l'Annuaire / résultats, graphe racine) : `PlayerProfileScreen(id)` et `TeamProfileScreen(id)` restent des écrans à barre de titre propre, réutilisant le **même** contenu (`PlayerProfileContent` / `TeamProfileContent`, rule 16 — un seul exemplaire, généralisé par paramètres). La **fiche équipe publique** (`id != "me"`, `pteam`) est en lecture seule : membres → fiche joueur (regroupés par roster si ≥ 2 rosters), sans CTA supplémentaire.

### Annuaire (`RegistryScreen`, via icône recherche)
Sélecteur **Joueurs / Équipes** :
- **Joueurs** : recherche déclenchée à partir de **3 caractères** ; pagination de toutes les pages MKCentral. Clic → profil joueur.
- **Équipes** : filtre local par nom/tag (insensible à la casse). Clic → profil équipe.

---

## 6. Cycle de vie d'une war

```mermaid
flowchart LR
    A["Nouvelle war"] --> B["Choix adversaire(s)"]
    B --> C["Composition (6 joueurs)"]
    C --> D["War en cours"]
    D -->|Course suivante| E["Saisie d'une course"]
    E --> D
    D -->|Plus d'actions| F["Pénalités / Remplacement / Annuler"]
    F --> D
    D -->|12 courses faites, 12p| G["Valider la war"]
    D -->|12 courses faites, 24p| H["Saisie scores adverses"]
    H --> G
    G --> I["Historique"]
```

**Wizard 3 étapes sur un seul écran** (`AddWarScreen`, rendu **pixel-perfect** vs la maquette du prototype UX — écran `addwar`, rules 13/15). En tête : le **segmenté 12/24** (`MKSegmentedSelector`) puis le **stepper cliquable** `1 · Adversaire` → `2 · Joueurs` → `3 · Récap` (composant partagé `MKStepper`). Les étapes basculent **dynamiquement** (état `step` du ViewModel, **aucune re-navigation** ni transition slide, rules 11/14) ; l'étape « Joueurs » n'est accessible qu'une fois l'adversaire complet, et « Récap » qu'une fois les 6 joueurs sélectionnés. Le bouton retour replie d'abord le sélecteur de roster inline, sinon recule d'une étape (3→2→1), sinon retire la dernière équipe, sinon quitte.

> **Le retour en arrière annule la sélection de l'étape rejointe** (demande utilisateur) : revenir à **Adversaire** (== 1ʳᵉ étape, via « Précédent », back système ou clic stepper) fait une **remise à zéro complète** — **désélectionne l'adversaire** (les adversaires en 24p), **réaffiche la liste complète** des équipes **et remet la line-up à zéro** (aucun joueur coché) ; revenir aux **Joueurs** depuis le Récap remet **seulement** la line-up à zéro. Aller **en avant** ne réinitialise rien. Le changement de mode 12/24 (qui repasse par la 1ʳᵉ étape) applique aussi cette remise à zéro complète. Le gating du stepper empêche alors de re-remonter au Récap sans re-compléter la sélection.

> **Divergence assumée vs la maquette** (demande utilisateur explicite, PR #54 / audit B23) : la maquette d'origine ne décrit **que 2 étapes** (Adversaire, Joueurs, avec le CTA « Commencer la war » en pied d'étape Joueurs). À la demande de l'utilisateur, une **3ᵉ étape « Récap »** a été ajoutée : le CTA de lancement est **retiré de l'étape Joueurs** et **déplacé sur l'étape Récap** ; le passage d'étape se fait par la **logique de complétion** (adversaire complet → Joueurs ; 6 joueurs → Récap) et/ou le stepper. Le style reste conforme à la maquette.

### Étape 1 — Choix de l'adversaire
- **Segmenté 12/24 joueurs** : point d'entrée du choix de mode (déménagé de l'Accueil). Le changer bascule le mode **dynamiquement sur le même écran** : l'argument de nav ne fait que semer la valeur initiale, puis `AddWarViewModel.onModeChange` met à jour l'état réactif, **revient à l'étape 1** et **réinitialise la sélection d'adversaires** (1 équipe en 12p, 3 en 24p).
- **12 joueurs** : sélectionner **1** équipe. **24 joueurs** : sélectionner **3** équipes.
- Champ « **Rechercher une équipe / un tag** » (insensible à la casse) ; le texte est comparé au nom et au tag de l'équipe **ainsi qu'au nom et au tag de chacun de ses rosters** — chercher le nom d'un roster fait donc remonter l'équipe parente (affichée une seule fois, avec son avatar). Aucune requête réseau (rosters déjà en local). Sous le champ, une **liste d'équipes** (`MKListRow` : logo de l'équipe, nom + sous-texte `roster · TAG` ou `N rosters mkworld`, chevron), suivie d'un **hint** rappelant « Logo = équipe, nom + tag = roster… ». Un pied de liste réserve de la marge pour ne pas coller au CTA.
- **Sélection du roster adverse** (rule 12) : au clic sur une équipe, l'app récupère ses rosters MK World.
  - **Un seul roster** → il est retenu automatiquement et l'écran passe directement à l'étape 2.
  - **Plusieurs rosters** → un **sélecteur inline** se déplie **sous la ligne** de l'équipe (cadre translucide `roster-pick` de la maquette) : une ligne par roster (nom — tag, avatar hérité de l'équipe). Choisir un roster le retient et passe à l'étape 2. En 24p, l'opération se répète pour chaque équipe adverse.
- Nom de war construit avec les **tags des rosters** : `TAG_rosterHôte - TAG_rosterAdv1 - TAG_rosterAdv2 …`.

### Étape 2 — Composition
- **Carte de progression** : compteur `n / 6` + barre verte + hint « Sélectionne les 6 joueurs… ».
- **Ton roster** : joueurs **groupés par roster** (un eyebrow par roster ; eyebrow « Allies » pour les alliés). Chaque ligne (`MKListRow`) porte la **photo de profil MKCentral** du joueur (résolue en parallèle ; **initiales colorées en repli** tant que la photo n'est pas là, rule 12) et une **pastille de sélection ✓** verte qui bascule au clic.
- **Roster adverse** (indicatif, non saisi) : lignes des joueurs du roster adverse retenu (`OpponentPreview.players`, issus du détail MKCentral), atténuées, en **initiales** (photo non résolue — voir écart ci-dessous), précédées d'un hint « L'adversaire est indicatif… ».
- **Aucun CTA** sur cette étape : dès que **exactement 6** joueurs sont sélectionnés, l'écran **bascule automatiquement** sur l'étape 3 « Récap » (retirer un joueur y ramène). C'est `AddWarViewModel.onPlayerSelected` qui pose `step = 2` quand la composition est complète, `step = 1` sinon.

### Étape 3 — Récap
- **Accessible seulement** quand **adversaire complet ET line-up complète** (les 6 joueurs) : le stepper conditionne l'accès à `nextButtonEnabled && buttonEnabled`.
- Rappel de l'**adversaire (des adversaires en 24p)** : `MKListRow` avec **nom + tag du roster** et **avatar de l'équipe** (rule 12).
- Rappel de la **line-up** : les 6 joueurs sélectionnés (`MKListRow` avec **photo de profil MKCentral** / initiales en repli + pastille ✓).
- Pied : bouton **« Précédent »** (→ étape 2) + **le seul CTA de lancement**, **« Démarrer la war »** (actif tant que la composition reste à 6 joueurs) → appelle `createWar()`.
- À la création : `War(id = now, teamHost = rosterId, teamOpponent = [rosterId…], scores = [WarScore(0)…])` — `teamOpponent` contient le(s) **rosterId** choisi(s) à l'étape 1 (et non le teamId) ; le `currentWar` de chaque joueur est mis à jour en DB **et** Firebase (les alliés `rosterId = -1` vont dans `newAllies`, les autres dans `users`).

> **Écart mineur assumé** : les **photos de profil** ne sont résolues que pour **ton roster** (étape 2 + line-up du Récap), pas pour les joueurs adverses **indicatifs** (qui restent en initiales). Les résoudre imposerait jusqu'à 18 appels `getPlayer` supplémentaires en 24p pour une information seulement indicative — non justifié.

### Étape 4 — War en cours (`CurrentWarScreen`)
> Hôte comme adversaires sont affichés avec le **nom et le tag de leur roster** (l'**avatar** reste celui de l'équipe principale). Idem sur le résumé/détail de war et les cellules de war. Si un adversaire ne peut plus être résolu localement (équipe/roster disparu du cache, war ancienne jamais synchronisée), il n'est **plus effacé** de l'affichage : il apparaît **en dégradé** (« Équipe inconnue » / tag `???`, sans logo) au lieu de disparaître.

**Écran unique scrollable** (`CurrentWarScreen`, ticket #43), **pixel-perfect** vs la maquette du prototype UX (écran `currentwar`, rules 13/15). Plus de pager : un seul `LazyColumn` (marge basse `90.dp` pour la bottombar, rule 17) au style « cartes dashboard » (fond `blackAlphaed`, bordure blanche, radius 6 — même style que l'Accueil). Le **segmenté « Démo : 12/24 joueurs »** de la maquette est un **contrôle de démo** (non reproduit, rule 15) : le mode réel est **déterminé par la war** (`teamOpponent.size > 1`), pas un choix utilisateur.

De haut en bas :
- **Carte « Score du match »** (`.warscore`) : côté hôte VS côté adversaire, chacun avec pastille (avatar de l'équipe ou initiales du tag sur couleur), **nom du roster** et score (en **blanc**). La **différence de score seule** est affichée **au centre**, entre les deux scores, **colorisée** (vert si > 0, rouge si < 0, blanc si = 0). Sous-titre : **« N courses restantes »** (12 − courses jouées), en **blanc** (non colorisé). En 24p, les côtés adverses sont empilés (pas de score chiffré au niveau de la carte tant que non saisi).
- **Carte « Scores des joueurs »** : cellules en **ligne compacte** — nom à gauche (+ nb de courses jouées si le joueur n'a pas tout joué, + pastille de shock si applicable), « **N pts** » à droite (`Arrangement.SpaceBetween`) — disposées sur **deux colonnes** (6 joueurs → 2 × 3 lignes).
- **Actions** : « **Course suivante** » (CTA dégradé) → `AddTrack` (masqué à 12 courses) **et** « **Plus d'actions** » (→ `Actions`) **sur une même ligne**, côte à côte.
- **Validation selon la variante** (uniquement à 12 courses jouées, `isOver`) :
  - **12 j** : CTA « **Valider la war** » (validation directe, pleine largeur — plus de libellé « 12 joueurs : … ») ;
  - **24 j** : carte « **Scores des équipes adverses** » (une ligne de saisie par équipe adverse : pastille + nom + champ numérique) + hint + CTA « **Saisir & valider** ». Le **total doit valoir 144 × N courses** (soit 1728 sur 12 courses), pénalités exclues ; toast d'erreur indiquant les points manquants/en trop ; les pénalités sont déduites avant écriture.
- **Section « Courses jouées »** (`.trackgrid`) : **enveloppée dans le même cadre** que les autres sections (`DashboardCard` — `blackAlphaed` + bordure blanche, mêmes radius/padding), eyebrow « Courses jouées · N » puis grille 2 colonnes. Chaque cellule, de gauche à droite : **(1) bande colorée verticale** au bord gauche (vert si diff > 0, rouge si < 0, blanc si = 0) ; **(2) score « hôte-adverse »** (ex. « 44-38 », `WarTrackDetails.displayedResult`) **+ diff colorisée** dessous, centrés verticalement ; **(3) colonne centrale** = image du circuit (rectangle arrondi) + **nom** (`Maps.label`) en dessous ; **(4) zone shocks** = icônes éclair de la manche (`WarTrack.shocks`), de **largeur toujours réservée** (placeholder invisible si aucun shock) pour aligner toutes les cellules. Clic → détail de la course. Grille en lignes chunkées (pas de `LazyVerticalGrid` imbriqué dans le `LazyColumn`).

`isOver` = `tracks.size == 12`. `buttonsVisible` = une war en cours existe en DataStore.

> **Écart résiduel assumé** (rule 13) : la carte score des variantes **24 j** n'affiche pas encore le podium des 3 scores en direct comme un rendu final (les scores sont saisis manuellement en fin de war, pas calculés par manche) ; le focus pixel-perfect porte sur la variante 12 j (cas principal), la 24 j réutilise le même style de cartes.

**Récupération des droits d'édition (DataStore vidé).** Le DataStore local peut être vide alors que la war existe encore côté Firebase (logout, réinstallation, autre appareil) : les boutons d'édition disparaîtraient. Pour éviter cela, la war courante enregistre son **créateur** via un `playerHostId` (id MKCentral) sur Firebase. À la lecture de la war courante (accueil comme écran de war en cours), si le DataStore est vide **et** que le joueur courant est le créateur (`playerHostId == mkcPlayer.id`), le DataStore est **réhydraté automatiquement** et l'édition redevient possible. Un utilisateur qui n'est pas le créateur (ou dont le profil MKCentral est lui aussi vidé) ne déclenche pas cette réhydratation. Les wars antérieures à ce mécanisme (sans `playerHostId`) restent lisibles (valeur par défaut, pas de réhydratation).

### Étape 4 — Saisie d'une course (`AddTrackScreen`, pager 4 pages)
1. **Circuit** : grille des 30 circuits, recherche par nom/label.
2. **Intermission (24p)** : choisir un circuit alternatif parmi `Maps.intermissionsTo(...)` ; resélectionner le même circuit l'annule.
3. **Positions** : pour chaque joueur (cycle auto), sa place (1–12 ou 1–24) ; les positions déjà prises sont masquées. Cellules plus petites en 24p.
4. **Récapitulatif** : positions + score de la course. En 12p : `score - adverse` (sur 2 chiffres) et `+/−diff`. En 24p : progression `score actuel -> nouveau score`. Possibilité d'**ajouter/retirer des shocks** par joueur. « Confirmer » écrit le `WarTrack` (indices du/des circuit(s), positions, shocks) dans Firebase et met à jour les scores.

### Étape 5 — Plus d'actions (`CurrentWarActionsScreen`, 3 onglets)
- **Pénalités** : grille `−10 / −15 / −20` par équipe ; **une seule pénalité par équipe** à la fois ; « Valider » applique et déduit du score de l'équipe visée.
- **Remplacement** : sélectionner 1 joueur sortant (composition actuelle) + 1 entrant (banc) ; « Remplacer » met à jour `currentWar` des deux joueurs (DB + Firebase).
- **Annuler la war** : abandon complet (réinitialise `currentWar` de tous les joueurs, supprime `currentWars/{rosterId}`, vide le DataStore) ; confirmation requise.

### Étape 6 — Validation finale
`onValidateWar()` : écrit la war dans l'historique Firebase (`wars/{rosterId}/{id}`), réinitialise les `currentWar`, supprime la war en cours, revient à l'accueil. En 24p, la validation des scores (`onValidateScore`) précède.

---

## 7. Historique & détails

### Liste des wars (`WarListScreen`)
- Bannière « War en cours » / CTA « Nouvelle war » + chips filtre de résultat (cf. §5, Pôle 2).
- Wars **groupées par mois** (`Pair("Mois AAAA", [WarDetails])`), triées du plus récent au plus ancien, en-tête collant avec compte (recalculé après filtrage).
- **Tous les modes (12j ET 24j) mélangés** : seul le filtre multi-roster subsiste (si désactivé : seulement `teamHost == rosterId`). L'ancien filtre par mode a été retiré.
- Filtre de résultat V/N/D purement UI (chips), appliqué par mois : un mois sans war correspondant au filtre est masqué.
- Clic → détail.

### Détail d'une war (`WarDetailsScreen`)
- `WarScoreView` (scores finaux), `WarPlayersCell` (composition, scores par joueur), `roster`.
- Bouton **« Tab »** (12p uniquement) → génération du tableau partageable.
- Grille des courses (bordure colorée 12p / transparente 24p) ; clic → détail de la course.

### Détail d'une course (`TrackDetailsScreen`)
- Circuit, score (`points - adverse` en 12p, `trackScore` en 24p), `+/−diff` (12p), positions de chaque joueur + shocks.
- Bouton **« Éditer »** si la course appartient à la war en cours (`editing` et war en cours existante).

### Édition d'une course (`EditTrackScreen`, 3 onglets)
- **Circuit** / **Positions** / **Shocks**. Le bouton de validation s'active si le circuit, les positions ou les shocks ont changé. Logique conditionnelle : mise à jour du circuit seul, des positions (exactement 6 saisies), des shocks seuls, ou combinaison ; le score de la war est recalculé sur l'ensemble des courses puis réécrit.
- Onglet **Positions** : ré-attribution joueur par joueur (nom du joueur courant + grille de positions cliquables). Une fois **toutes les positions ré-attribuées** (autant de positions saisies que de joueurs, dernier joueur inclus), la grille est remplacée par un **récapitulatif** en lecture seule (`VerticalGrid` de `PlayerCell` : joueur, position, shocks existants), réutilisant les mêmes composants que la page « Résumé » de l'ajout d'une course. La `MapCell` n'y est pas affichée (le circuit se modifie dans l'onglet dédié) et les shocks restent en lecture seule (édités dans l'onglet Shocks). Les boutons Confirmer / Annuler, communs aux onglets, restent en bas.

---

## 8. Statistiques

Les stats se déclinent par **format** (12/24) et souvent par **Individuel / Équipe**. Type porté par la classe scellée `StatsType` : `PlayerStats`, `TeamStats`, `OpponentStats`, `MapStats`.

### Écran de stats (`StatsScreen`)
Sections affichées selon le type :
- Cellule joueur / équipe / circuit en tête.
- `MKWarStatsView` : bilan global de wars (nombre de wars, V/N/D, winrate, courbe).
- `MKWarDetailsStatsView` : détails (score moyen, score moyen par manche / position
  moyenne, maps gagnées, shocks) — affiché pour la vue **circuit (`MapStats`)** et
  pour l'**écran détail d'un adversaire (`OpponentStats`)**. En vue **individuelle**
  (un joueur ciblé), ces valeurs sont celles **du joueur** (son score moyen, sa
  position moyenne par manche) ; en vue équipe, celles de l'équipe. Pour les récaps
  globaux joueur/équipe, ces indicateurs sont couverts par « Forme récente ».
- Historique des 5 dernières wars (pour `OpponentStats`).
- **Sections enrichies repliables** (accordéon animé, hors `MapStats`) :
  - **Forme récente** (`MKRecentFormCell`) — **vue de référence des stats** :
    compare **trois fenêtres** (all-time, 5 dernières, 10 dernières wars) sur les
    mêmes indicateurs, une ligne par indicateur avec ses trois valeurs. Indicateurs :
    **winrate**, **score moyen** par war, **position moyenne** (vue joueur) OU
    **score moyen par manche** (vue équipe/adversaire), **% de manches gagnées**,
    **shocks/war** (avec l'icône éclair). Les deux fenêtres récentes affichent un
    **delta** vs l'all-time (chiffre + flèche ↗/↘ + couleur) dont le sens dépend de
    l'indicateur : winrate & % manches gagnées → plus haut = mieux ; position moyenne
    → plus **bas** = mieux (couleur inversée) ; shocks/war → direction ambiguë, donc
    **valeur neutre** sans couleur. Si moins de wars que demandé, la fenêtre le
    signale sans delta trompeur.
  - **Records & séries** (`MKRecordsCell`) : série de victoires/défaites en cours,
    records historiques de séries de victoires et de défaites, comptes Top6/Bot6
    (affichés si > 0).
  - **Indicateurs avancés** (`MKAdvancedStatsCell`) : contribution du joueur aux
    points de l'équipe (vue joueur), régularité du score (écart-type + amplitude
    min/max), marge moyenne de victoire et de défaite (séparées), position moyenne
    en 1ʳᵉ vs 2ᵉ moitié de war (vue joueur), série d'invincibilité (V+N) en cours,
    points perdus en pénalités.
  - **Distribution des positions** (`MKPositionDistributionCell`, vue joueur) :
    mini-histogramme du nombre de fois où le joueur a fini à chaque position.
  - **Meilleurs / pires circuits** (`MKMapsRankingCell`) : top 3 / flop 3 des
    circuits, présentés par lignes, avec **winrate ET score moyen** affichés
    simultanément. Un circuit n'apparaît qu'à partir de 3 matchs joués.
  - **Meilleurs / pires adversaires** (`MKOpponentsRankingCell`, vues **équipe ET
    joueur/individuelle**) : top 3 / flop 3 des adversaires par « winrate face à »
    et « score moyen face à » (double critère). Chaque adversaire est présenté sur
    une **ligne pleine largeur** (cellules empilées) afin d'afficher ses trois
    valeurs — wars jouées, winrate et score moyen — sans les tronquer, seuil de 3
    matchs. En vue joueur, les adversaires sont ceux affrontés **du point de vue de
    ce joueur**.
- `MKTopBottomCell` : tops/bottoms d'équipe et positions individuelles.

> Ces sections enrichies restent **affichées en 12p uniquement** pour l'instant
> (l'affichage 24p — histogramme des positions P1→P24, comparatif 12p vs 24p —
> viendra dans un ticket dédié). Le **moteur de calcul** sous-jacent est en revanche
> déjà prêt pour le 24p (résultat victoire/nul/défaite et marges corrects en 24p) :
> lorsque l'affichage 24p sera livré, les valeurs seront justes. Les anciens blocs
> à une valeur créés au départ ont été retirés car en doublon avec les nouvelles
> sections top3/flop3 : le bloc « circuits » (circuit le plus joué, meilleur/pire
> circuit) et le bloc « adversaires » (adversaire le plus joué, le plus vaincu, le
> moins vaincu).
>
> `MKPlayerScoreCell` (pire/meilleur score, plus large victoire / plus lourde
> défaite) a été **retiré** : ces deux indicateurs sont abandonnés car ils n'entrent
> pas dans une comparaison de moyennes. Les indicateurs récurrents (score moyen,
> position moyenne / score par manche, % manches gagnées, shocks/war) sont désormais
> dans « Forme récente » sur trois fenêtres **pour les récaps globaux joueur/équipe**.
> `MKWarDetailsStatsView` reste utilisé pour la vue **circuit** et l'**écran détail
> d'un adversaire** (où, en vue individuelle, il montre le score moyen et la position
> moyenne **du joueur**).

### Statistiques enrichies — que veut dire chaque stat ?

Cette section explique, en langage clair, le **sens** de chaque statistique
enrichie ajoutée à l'écran de stats. Sauf mention contraire, elles se lisent du
point de vue de **ton équipe** (ou de **toi** en vue joueur), et portent
uniquement sur les **wars 12 joueurs** (le mode 24 joueurs arrivera plus tard).

Rappels de vocabulaire : une **war** = un match ; une **manche** (ou « track ») =
une course dans la war ; le **winrate** = pourcentage de wars gagnées ;
« **all-time** » = sur tout l'historique.

#### Séries

- **Série en cours** — Ta dynamique du moment : combien de wars d'affilée tu as
  gagnées (ex. « 3 victoires ») **ou** perdues (ex. « 2 défaites ») en comptant à
  partir de la war la plus récente. Une seule war nulle ou un résultat inverse
  remet la série à zéro. Affiche « Aucune » s'il n'y a pas de série nette.
- **Record de victoires** — La plus longue série de victoires consécutives que tu
  aies jamais réalisée (meilleur historique).
- **Record de défaites** — À l'inverse, la plus longue série de défaites
  consécutives subie (pire historique).
- **Invaincu depuis** — Depuis combien de wars tu n'as plus perdu, en comptant
  aussi les matchs nuls (victoires **et** nuls). Se réinitialise à la première
  défaite. Utile pour visualiser une bonne passe même avec quelques nuls.

Toutes les séries sont calculées dans l'**ordre chronologique** réel des wars
(par date), condition indispensable pour qu'elles aient un sens.

#### Top6 / Bot6

Une manche est un **Top6** quand les **6 joueurs de l'équipe occupent les
positions 1 à 6** (score d'équipe de la manche = 61) et un **Bot6** quand ils
occupent les **positions 7 à 12** (score d'équipe = 21). C'est un cas **exact** :
une manche où l'équipe est répartie entre le haut et le bas n'est ni l'un ni
l'autre.

- **Nombre de Top6 / Bot6** — Le **compte brut** de manches Top6 et Bot6 sur
  l'historique. Chaque ligne n'apparaît que si son compte est supérieur à 0.
- **Top6/Bot6 par circuit** — La même idée déclinée circuit par circuit (série de
  bonnes/mauvaises manches sur chaque map), pour repérer les circuits où l'équipe
  performe ou coince.

#### Meilleurs / pires circuits

Deux classements de circuits, présentés **par lignes** (top 3 et flop 3), avec les
**deux critères affichés côte à côte** :

- par **winrate** : les circuits que tu gagnes le plus souvent (top 3) et le moins
  souvent (flop 3) ;
- par **score moyen** : les circuits où l'équipe marque le plus / le moins de
  points en moyenne.

**Condition** : un circuit n'apparaît dans ces classements que s'il a été joué
**au moins 3 fois** — en dessous, l'échantillon est trop faible pour être fiable,
le circuit est donc exclu.

#### Meilleurs / pires adversaires

Deux classements d'adversaires (top 3 / flop 3), toujours avec **winrate ET score
moyen** affichés ensemble sur chaque cellule (wars jouées + winrate + score moyen) :

- **Meilleur / Pire winrate face à** — Les équipes contre lesquelles on gagne le
  plus / le moins souvent.
- **Meilleur / Pire score moyen face à** — Les équipes contre lesquelles on marque
  le plus / le moins de points en moyenne.

Le libellé est volontairement « **face à** » (et non « meilleur/pire
adversaire »), pour bien dire de quel angle on parle. **Condition** : seuls les
adversaires rencontrés **au moins 3 fois** entrent dans ces classements.

Ces classements apparaissent dans les **stats d'équipe** comme dans les **stats de
joueur / individuelles** — dans ce dernier cas, ce sont les adversaires affrontés
**du point de vue du joueur** concerné (winrate/score calculés sur ses wars).

#### Forme récente

**Vue de référence** des stats : compare **trois fenêtres** — ta moyenne de
toujours (**all-time**), tes **5 dernières** et tes **10 dernières** wars — sur les
mêmes indicateurs, affichés une ligne par indicateur avec ses trois valeurs.

Indicateurs :

- **Winrate** — pourcentage de wars gagnées.
- **Score moyen** par war (points du joueur en vue joueur ; écart d'équipe « +X/-X »
  en vue équipe/adversaire).
- **Position moyenne** (vue joueur) — ta place moyenne sur une manche.
- **Score moyen par manche** (vue équipe/adversaire) — remplace la position moyenne.
- **% de manches gagnées**.
- **Shocks/war** — nombre moyen d'objets éclair pris par war (icône éclair).

Lecture du **delta** (sur les fenêtres 5 et 10 dernières, vs l'all-time) : un
**chiffre signé + une flèche + une couleur**. Le **sens** dépend de l'indicateur :

- winrate, % manches gagnées, score → **plus haut = mieux** (hausse en vert).
- **position moyenne → plus BAS = mieux** : une baisse de la position est affichée
  en **vert** (couleur inversée).
- **shocks/war → direction ambiguë** : valeur affichée **sans couleur** (neutre),
  pour ne pas suggérer à tort que plus/moins est mieux.

Le delta du **score moyen** est exprimé sur la même échelle que la valeur affichée :
en vue joueur c'est un delta de points bruts, en vue équipe c'est un delta d'écart de
score (cohérent avec l'affichage « +X / -X »).

**Petit échantillon** : si tu as joué moins de wars que la fenêtre demandée (ex.
seulement 3 wars pour la fenêtre « 10 dernières »), l'app affiche ce qui est
disponible, le signale, et **n'affiche pas de delta trompeur**.

#### Contribution du joueur (vue joueur uniquement)

Le **pourcentage moyen des points de l'équipe que tu apportes toi-même**, calculé
war par war puis moyenné. Exemple : 20 % signifie que tu marques en moyenne un
cinquième des points de l'équipe. Utile pour situer son poids dans le collectif.
Visible **seulement en vue joueur**.

#### Régularité

Mesure la **constance** de tes performances, de deux façons complémentaires :

- **Écart-type** — Plus il est **bas**, plus tes scores sont réguliers d'une war à
  l'autre ; plus il est **haut**, plus ils sont irréguliers (des hauts et des
  bas). Affiché « ± X ».
- **Amplitude (min – max)** — Simplement ton **pire** et ton **meilleur** score sur
  l'historique, pour visualiser l'écart entre les deux extrêmes.

#### Distribution des positions (vue joueur uniquement)

Un **mini-histogramme** qui montre, pour chaque position de 1 à 12, **combien de
fois** tu as terminé une manche à cette place. Permet de voir en un coup d'œil si
tu finis souvent devant, ou plutôt dispersé. Visible **seulement en vue joueur**.

#### Marge moyenne de victoire / de défaite

Au-delà du simple bilan Victoires/Nuls/Défaites, indique **de combien** on gagne
ou on perd en moyenne :

- **Marge moyenne de victoire** — L'écart de score moyen **quand on gagne** (ex.
  « +45 »).
- **Marge moyenne de défaite** — L'écart de score moyen **quand on perd** (ex.
  « -30 »).

Les deux sont **séparées** : gagner souvent de justesse mais perdre lourdement
raconte une autre histoire qu'un simple winrate.

#### Performance 1ʳᵉ vs 2ᵉ moitié de war (vue joueur)

Compare ta **position moyenne** sur la **première moitié** de la war (premières
manches) et sur la **seconde moitié** (dernières manches). Permet de voir si tu
commences fort et faiblis, ou si tu montes en puissance en fin de match.

#### Points perdus en pénalités

Le **total des points retirés à l'équipe par des pénalités** sur tout
l'historique. Un repère du « coût » cumulé des pénalités.

### Statistiques individuelles (joueur)
Bilan V/N/D, taux de victoire, score moyen/circuit, position moyenne, circuit le plus joué, meilleur/pire circuit, plus grosse victoire, meilleur/pire score, pire défaite, nombre de wars et de circuits, meilleurs/pires résultats face aux adversaires, tableau par circuit, historique.

### Statistiques d'équipe
Mêmes agrégats au niveau équipe + détail par joueur.

### Classements (`StatsRankingScreen`)
Écran unique à **sous-onglets** `Joueurs / Adversaires / Circuits` (`RankingTab`, `MKSegmentedSelector`), pilotés par un **état interne réactif** (pas de re-navigation, cf. rule 11). Chaque onglet : recherche par nom + tri à **3 chips** (`SortType`, `MKSegmentedSelector`) + **curseur d'occurrences minimum** (`Slider`). Cellules = `PodiumCell` **mutualisée** (extraite de `StatsFullScreen` vers `ui/stats/MKPodiumCell.kt`, param `contentColor` = **noir** ici, blanc côté Stats), rendue en lignes de 3.
- **Joueurs** : liste **sectionnée** Membres / Alliés (`PlayerSection`), cellule avatar-initiales + nom, tri `Wars (défaut) / Winrate / Score moy.`. Clic → stats individuelles (`StatsType.PlayerStats`). La distinction membre/allié vient du cache `playersRankList` (clé `Pair(0, roster)` = membre, `Pair(1, "Allies")` = allié).
- **Adversaires** : cellule logo d'équipe + nom (rule 12), tri `Occurrences (défaut) / Winrate / Score moy.`. Clic → **fiche détail adversaire** (`Opponent/{teamId}`, #27).
- **Circuits** : cellule illustration + nom, tri `Fréquence (défaut) / Winrate / Score moy.`. Clic → **fiche détail circuit** (`Map/{trackIndex}`, #27).
- **Tri** (`SortType`, ordre = ordre des chips) : `COUNT` (**défaut**, nb de matchs, desc), `WINRATE` (desc), `AVERAGE` (score moyen, desc). L'ancien tri `NAME` (chip « Nom ») a été **retiré** (absent du prototype).
- **Curseur d'occurrences** : `Slider` **continu** (piste sans graduations, pouce cercle plein blanc), `minOccurrences` (état réactif) filtre par `sampleSize ≥ min` ; `maxOccurrences` = plus haut compteur de l'onglet (borne haute). Remplace, à l'affichage, le seuil fixe ; **plus de carte « En bref »** (retrait décidé par l'utilisateur). `Stats.MIN_RANKING_SAMPLE` n'est plus utilisé par cet écran (il reste employé par les podiums du pôle Stats).

Sources : les classements sont pré-calculés par `InitStatsWorker` et lus depuis le cache. Perspective **équipe** pour adversaires/circuits (`opponentRankList` / `trackRankList`, pas de switch individuel/équipe) ; `playersRankList` (groupé membre/allié) pour les joueurs.

---

## 9. Annuaire & profils

### Profil joueur (`PlayerProfileScreen`)
- Avatar, drapeau pays, nom, bio (MKCentral) ; date d'inscription, friend code, tag Discord, équipe + date d'arrivée, **badge de rôle**.
- **Bouton « Ajouter en allié »** (selon gating §3) ; message « Allié » si déjà allié ; **bouton « Basculer le rôle »** (leader uniquement).
- **Menu visible uniquement sur son propre profil** (`id == "me"`) :
  - **Rafraîchir** (relance `fetchData`, met à jour `lastUpdate` affiché `dd/MM/yyyy - HH:mm`).
  - **Notifications** (interrupteur ; demande la permission au besoin).
  - **Multi-roster** (interrupteur ; **nécessite un redémarrage** pour s'appliquer).
  - **Déconnexion** (confirmation → vide DB/DataStore, révoque le token Discord, retour à `Signup`).
  - **Debug** (visible si id `18595` ou matrix).

### Profil équipe (`TeamProfileScreen`)
Si c'est **sa** propre équipe (`id == "me"`), 3 contenus :
- **Membres** : groupés par roster (en-têtes si plusieurs), clic → profil joueur.
- **Alliés** : bouton « Ajouter un allié » (si role > 0) ouvrant une bottom sheet de recherche (≥ 3 caractères, pagination MKCentral, exclut les alliés déjà présents).
- **Stats** d'équipe.

Pour une autre équipe : vue simple (rosters + joueurs), sans gestion d'alliés.

---

## 10. Tableau partageable (PDF)

Écran `EditTabScreen` (depuis le détail d'une war 12p, bouton « Tab ») : génère une **image de tableau récapitulatif**.

- Texte d'explication ; **+/−** pour ajuster le nombre de lignes adverses (**6 à 9**, défaut 6) ; pour chaque ligne, un champ nom + un champ score.
- **« Tab classique »** : valide que la somme des scores adverses saisis = `scoreOpponent` calculé (sinon toast d'écart), puis génère le PDF (détails de war, scores équipe/joueur/adversaire, pénalités incluses) et émet l'`Uri` pour partage.
- **« Tab détaillé »** (ajoute courses, shocks, courbe de progression) — **commenté dans le code** (`EditTabScreen`), donc **absent de l'UI** actuelle.

Le rendu et l'enregistrement (galerie / partage) sont décrits dans [TECHNICAL.md §15](TECHNICAL.md#15-génération-pdf).

---

## 11. Paramètres, données & mode debug

- **Notifications** : informent de la fin des traitements (ex. « Données mises à jour »). Activables depuis le profil.
- **Multi-roster** : périmètre de calcul des stats (tous les rosters vs le sien).
- **Synchronisation** : tâche de fond quotidienne (~4 h) rafraîchissant les données ; rafraîchissement manuel depuis le profil.
- **Fonctionnement hors-ligne** : données en cache local (Room/DataStore), wars synchronisées via Firebase (la war en cours est écoutée en temps réel).

### Écran debug (`DebugScreen`) — réservé
1. **Update Tags** — pousse les tags d'équipes vers Firebase.
2. **Update LariisBot Data** — rafraîchit pour chaque utilisateur d'équipe ses infos Discord/nom depuis MKCentral.
3. **Update Transferts** — réconcilie les rosters (entrées/sorties de joueurs, alliés).
4. **Migrer les adversaires (teamId → roster)** — action manuelle et idempotente : réécrit dans l'historique Firebase le `teamId` d'un adversaire en `rosterId`, **uniquement** pour les équipes possédant un seul roster mkworld (cas non ambigu) **et dont le roster est résolvable localement** (sinon la migration s'abstient, pour ne pas rendre l'adversaire non affichable). Fusionne alors le doublon « équipe legacy + roster » d'une même équipe mono-roster en un seul item du classement adverse (wars anciennes et récentes réunies). Les équipes multi-rosters et la war en cours ne sont pas touchées.
5. **Diagnostiquer les adversaires inconnus** — outil **non destructif** d'arbitrage des wars dont un adversaire s'affiche « Équipe inconnue » / tag `???` (id de `teamOpponent` non résolu localement). Balaye les wars de chaque roster hôte, liste chaque war concernée (id, date, score, hôte) et, pour chaque id d'adversaire non résolu, tente une résolution MKCentral parmi les **équipes mkworld actives ayant plus de 6 joueurs** (miroir du filtre par défaut du site MKCentral : actives, non historiques, effectif ≥ 6). Le domaine étant exclusivement mkworld, un adversaire qui a **recréé une équipe mkworld** avec un nom/tag proche est retrouvé : le diagnostic identifie l'**équipe source** mkworld, puis **rebondit sur son nom/tag** pour proposer les **équipes mkworld candidates** (correspondance de tag ou de nom). Un **mapping manuel expert** (correspondances exactes relevées à la main dans les données historiques de l'équipe) **prime** sur cette recherche automatique pour certains adversaires : il propose alors directement l'équipe mkworld cible et ses rosters. Issues par id : **source retrouvée avec candidats mkworld** → un bouton **« Réattribuer »** **par roster mkworld candidat** (réécrit `teamOpponent` vers le rosterId choisi, uniquement s'il est résolvable localement) — le choix reste **humain** (0, 1 ou plusieurs candidats, jamais automatique) ; **source retrouvée sans candidat** (irrécupérable en l'état) ; **introuvable** (adversaire dissous/historique/à faible effectif ou d'origine mk8dx pure, hors mapping — à supprimer) ; **erreur réseau** (réessayer). Chaque war offre aussi **« Supprimer la war »** (avec confirmation) pour le paquet irrécupérable. Les actions destructives/réécritures restent manuelles, war par war.
6. **Diagnostiquer les joueurs manquants** — outil **non destructif** (miroir du diagnostic des adversaires) qui répertorie les joueurs présents dans des wars mais absents du cache local (membres + alliés) — typiquement des joueurs ayant quitté l'équipe. Balaye les wars de chaque roster hôte, collecte les `playerId` (via `WarPosition`), retient ceux qui ne correspondent à **aucun** joueur du cache, dédoublonne, et affiche pour chacun : nom, pays et **nombre de wars** où il apparaît (nom/pays résolus via MKCentral ; « Joueur inconnu » si non résolu, sans faire disparaître l'entrée). Chaque joueur offre un bouton **« Ajouter en ally »** qui l'enregistre comme allié **en local ET sur Firebase `newAllies`** (durable — sinon la resynchro l'effacerait) ; le joueur ajouté disparaît alors de la liste.
7. **Test MKWR** — charge les records du monde (scraping `mkwrs.com`).
8. **Test Notif** — envoie une notification de test (si activées).
9. **Mode Matrix** — simule un autre joueur : entrée par id (charge ses données et passe `matrixMode = true`), sortie (recharge le joueur de référence `18595`, `matrixMode = false`).

---

## 12. Récapitulatif des règles métier

| Domaine | Règle |
|---|---|
| **Format** | `teamOpponent.size == 1` ⇒ 12 joueurs ; `> 1` (3) ⇒ 24 joueurs. Sélecteur dans le pôle Wars (création) et le menu stats. |
| **Composition** | Exactement **6 joueurs** sélectionnés pour démarrer ; substitutions possibles en cours de war. |
| **Adversaires** | 1 équipe (12p) ou 3 équipes (24p) à sélectionner. |
| **Scoring 12p** | Position→points (1ʳᵉ = 15) ; score adverse = 82 − score équipe ; pénalités déduites des totaux. |
| **Scoring 24p** | 144 pts/course ; scores finaux **saisis manuellement**, total de contrôle **1728** ; victoire = top 2 des scores. |
| **Pénalités** | −10, −15, −20 ; une par équipe ; déduites avant l'écriture finale. |
| **Shocks** | Objet éclair obtenu en jeu (item stratégique majeur) ; compté par joueur/course pour des statistiques dédiées, sans impact sur le calcul du score. |
| **Alliés** | `rosterId = -1` ; stockés dans `newAllies` Firebase ; ajoutables depuis l'annuaire ou le profil d'équipe. |
| **Rôles** | 2 = leader/manager, 1 = admin, 0 = membre ; gating des actions (cf. §3). |
| **Multi-roster** | Active/désactive l'agrégation des stats sur tous les rosters ; redémarrage requis. |
| **War en cours** | Une seule à la fois, écoutée en temps réel ; terminée à 12 courses. |
| **Équipe « 6v6 Squad »** | Équipe synthétique (`SQ`, id `123456789`) injectée localement pour les matchs amicaux. |

---

*Détails techniques (modèles, algorithmes, intégrations) : [TECHNICAL.md](TECHNICAL.md).*
