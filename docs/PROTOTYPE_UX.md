# Prototype UX — Refonte navigation 5 pôles

Cette spécification décrit **fidèlement** le prototype UX navigable qui sert de référence pour l'epic de refonte de la navigation en 5 pôles. La maquette HTML navigable est disponible dans `docs/prototype/stats-mkworld-5poles.html` et en ligne sur l'artifact <https://claude.ai/code/artifact/58f3218f-3d51-4af4-8ff1-620dc3beac2d>.

Le prototype est une maquette fidèle à la charte (polices Bungee / Nunito / Urbanist / MKWorld, composants repris de l'app), avec des **données fictives**. Chaque écran actuel de l'app y est atteignable ; les nouveautés apportées par les tickets sont marquées `Nouveau`, les features secrètes existantes `Caché`. L'easter-egg Matrice est fonctionnel (taper 5× la version dans Profil).

> Conventions de lecture : les libellés et valeurs ci-dessous sont **recopiés tels quels** depuis la maquette (français). Les mentions `Nouveau` / `Caché` reproduisent les badges affichés. « → écran X » signale une navigation `data-nav`, « toast : … » une action factice `data-toast`.

---

## Navigation générale

Barre de navigation basse `[data-nav-bar]` à **5 pôles** :

| Pôle | Icône | Rôle (texte de présentation) |
|---|---|---|
| **Accueil** (`home`) | maison | Dashboard, war en cours, momentum, raccourcis. |
| **Wars** (`wars`) | drapeau | Créer/gérer une war, historique, détails course par course, PDF. |
| **Stats** (`stats`) | graphe | Individuelles vs Équipe, distinctes (composants app + nouveautés). |
| **Classements** (`classements`) | barres | Palmarès triables → fiches adv./circuit/joueur. Annuaire via la recherche 🔍. |
| **Profil** (`profile`) | utilisateur | Identité, réglages, alliés, rôles… et l'entrée cachée. |

Un clic sur un bouton de la barre appelle `goRoot` (réinitialise l'historique). Les navigations internes (`data-nav`) empilent l'historique ; `data-back` dépile. La barre basse met en surbrillance le pôle correspondant à l'écran courant selon la table `navHi`.

### Rattachement écran → pôle

| Écran (`data-view`) | Titre affiché | Pôle |
|---|---|---|
| `home` | ACCUEIL | Accueil |
| `wars` | WARS | Wars |
| `addwar` | NOUVELLE WAR | Wars |
| `currentwar` | WAR EN COURS | Wars |
| `waractions` | ACTIONS | Wars |
| `addtrack` | AJOUTER UNE COURSE | Wars |
| `trackdetails` | COURSE | Wars |
| `edittrack` | ÉDITER LA COURSE | Wars |
| `wardetails` | DÉTAILS DE LA WAR | Wars |
| `edittab` | TAB (PDF) | Wars |
| `stats` | STATISTIQUES | Stats |
| `statsfull` | STATISTIQUES | Stats |
| `classements` | CLASSEMENTS | Classements |
| `opp` | ADVERSAIRE | Classements |
| `map` | CIRCUIT | Classements |
| `registry` | ANNUAIRE | Classements |
| `pplayer` | PROFIL JOUEUR | Classements |
| `pteam` | PROFIL ÉQUIPE | Classements |
| `profile` | PROFIL | Profil |
| `debug` | DEBUG · MATRICE | Profil |
| `records` | RECORDS DU MONDE | Profil |
| `signup` | BIENVENUE | Profil |

**22 écrans** au total.

### Données de démonstration récurrentes

- **Mon équipe / moi** : équipe **Harmonia** (tag HM, `#5c6bc0`), joueur **Pascal** (tag PA, `#29b6f6`, 🇫🇷 France, Leader). Autres membres : **Larmik** (LK, Leader, 41 wars), **Juju** (Ju, Admin, 28 wars), **Max** (Ma, Membre, 35 wars). Allié : **Guest1** (G1, roster externe). Remplaçants cités : **Théo** (Th), **Kevin** (Ke).
- **Équipes adverses** : Plombiers du Coin (PC, `#ef5350`), Tortues Géniales (TG, `#66bb6a`), Fzero Squad (FZ, `#7e57c2`), Koopa Onslaught (KO, `#26a69a`), Wizzro (WZ, `#ffa726`), Rainbow Killers (RK, `#ec407a`).
- **Circuits** : Circuit Mario (CM, coupe Champignon), Plage Cheep Cheep (PC/CC, coupe Champignon), Château de Bowser (CB), Désert Sec-Sec, Route Arc-en-ciel (coupe Spéciale), Vallée Wario (coupe Étoile).

---

## Pôle Accueil

### Écran `home` — « ACCUEIL »

Barre d'app : titre **ACCUEIL** + icône loupe 🔍 (→ `registry`).

Sections dans l'ordre :

1. **Carte de salutation** (cliquable → `profile`) : pastille « PA » (`#29b6f6`), « **Salut, Pascal** », sous-titre « Harmonia · voir mon profil → ». Segmenté visuel `Moi` (actif) / `Équipe`.
2. **War en cours** (eyebrow) — bannière cliquable → `currentwar` : « **En direct · 12 joueurs** », « **vs Tortues Géniales** », « **512 – 486 · 7 courses jouées** ».
3. **Carte « Momentum »** :
   - Bande de forme (5 derniers) : `V V D V N` + libellé « 5 dernières ».
   - Sparkline (canvas) + indicateur « **+8 %** » (flèche haut), légende `Nouveau` « Forme 10 wars vs all-time ».
4. **Carte « Chiffres clés »** : `66 %` **Winrate** · `542` **Score moyen** · `4.8` **Position moy.**
5. **Bandeau highlight** (icône flamme) : « **Série de 4 victoires** », « En cours — record : 8 » `Nouveau`.
6. **Derniers résultats** (eyebrow) + lien « Voir tout → » (→ `wars`). Trois lignes de résultat, chacune cliquable → `wardetails` :
   - V · PC · « vs Plombiers du Coin » · « Hier · 12 joueurs » · **512–472** (+40)
   - V · TG · « vs Tortues Géniales » · « 28 juin · 12 joueurs » · **498–486** (+12)
   - D · KO · « vs Koopa Onslaught » · « 24 juin · 12 joueurs » · **455–529** (−74)

---

## Pôle Wars

### Écran `wars` — « WARS »

Barre d'app : titre **WARS** + sous-titre « **32 wars** ».

1. **Segmenté démo `[data-warstate]`** : `Démo : war en cours` (actif) / `Démo : aucune war`. Bascule l'affichage entre les blocs `ws-on` et `ws-off`.
   - **`ws-on`** : bannière → `currentwar` (« **En direct** », « **vs Tortues Géniales · 512–486** », « Reprendre — 7 courses jouées ») + hint « Règle métier : « Nouvelle war » est masqué tant qu'une war est en cours. »
   - **`ws-off`** (masqué par défaut) : bouton CTA « **Nouvelle war** » → `addwar`.
2. **Chips filtre `[data-filter]`** : `Tous` (actif) / `Victoires` / `Nuls` / `Défaites`. Filtrent la liste des résultats par `data-res`.
3. **Liste de résultats** (chacun → `wardetails`) :
   - V · PC · vs Plombiers du Coin · Hier · 12 joueurs · **512–472** (+40)
   - V · TG · vs Tortues Géniales · 28 juin · 12 joueurs · **498–486** (+12)
   - D · KO · vs Koopa Onslaught · 24 juin · 12 joueurs · **455–529** (−74)
   - V · WZ · vs Wizzro · 21 juin · **24 joueurs** · **534–450** (+84)
   - N · RK · vs Rainbow Killers · 18 juin · 12 joueurs · **492–492** (=)
   - V · FZ · vs Fzero Squad · 14 juin · 12 joueurs · **521–463** (+58)

### Écran `addwar` — « NOUVELLE WAR »

Barre d'app : ← retour + titre **NOUVELLE WAR**.

1. **Segmenté** : `12 joueurs` (actif) / `24 joueurs`.
2. **Stepper** : `1 · Adversaire` (actif) → `2 · Joueurs`.
3. Champ de recherche « **Rechercher une équipe / un tag** ».
4. **Sélectionner l'adversaire** (eyebrow) — liste cliquable (visuellement) :
   - PC · **Plombiers du Coin** · tag PC
   - TG · **Tortues Géniales** · tag TG
   - FZ · **Fzero Squad** · tag FZ
   - KO · **Koopa Onslaught** · tag KO
5. Hint : « Étape 2 : sélection des joueurs par roster (12 ou 24), puis « Commencer ». »
6. CTA « **Commencer la war** » → `currentwar`.

### Écran `currentwar` — « WAR EN COURS »

Barre d'app : ← retour + titre **WAR EN COURS**.

1. **Carte score** : Harmonia (HM) **512** (couleur victoire) — VS — Tortues Géniales (TG) **486** ; sous-texte « **+26 après 7 courses** ».
2. **Carte « Joueurs »** : Pascal `98 pts` · Larmik `104 pts` · Juju `86 pts` · Max `92 pts`.
3. CTA « **Course suivante** » → `addtrack`.
4. Deux boutons : « **Plus d'actions** » (→ `waractions`) · « **Valider la war** » (toast : « War validée ✓ » puis → `wars`).
5. Hint : « Règle métier : en 12 j, « Valider » directement · en 24 j, saisir d'abord les scores adverses. « Course suivante » disparaît à 12 courses jouées. »
6. **Courses jouées · 7** (eyebrow) — grille de circuits (chacun → `trackdetails`) :
   - Circuit Mario · **58** (+14)
   - Château de Bowser · **38** (−6) *(négatif)*
   - Plage Cheep Cheep · **54** (+10)
   - Désert Sec-Sec · **49** (+2)

### Écran `waractions` — « ACTIONS »

Barre d'app : ← retour + titre **ACTIONS**. Onglets `[data-tabs="wa"]` :

- **Pénalités** (`wa-pen`, actif) : hint « Appliquer une pénalité de points à une équipe. » Grille `pengrid` : `−10 Harmonia`, `−10 Tortues G.` (sélectionné), `−15 Harmonia`, `−15 Tortues G.`, `−20 Harmonia`, `−20 Tortues G.`. CTA « **Valider** » (toast : « Pénalité appliquée » + retour).
- **Remplacement** (`wa-sub`) : « **Joueur sortant** » (Pascal / Juju), « **Joueur entrant** » (Théo / Kevin). CTA « **Remplacer** » (icône swap ; toast : « Remplacement effectué » + retour).
- **Annuler** (`wa-cancel`) : carte « **Annuler la war** » + hint « Cette action supprime définitivement la war en cours et toutes ses courses. Êtes-vous sûr ? ». Bouton danger « **Supprimer la war** » (toast : « War annulée » + → `home`).

### Écran `addtrack` — « AJOUTER UNE COURSE »

Barre d'app : ← retour + titre **AJOUTER UNE COURSE**.

1. **Stepper** : `Circuit` (actif) → `Intermission` → `Positions` → `Résumé`.
2. Champ « **Rechercher un circuit** ».
3. Grille de circuits (choix) : Circuit Mario *(Coupe Champignon)*, Plage Cheep Cheep *(Coupe Champignon)*, Route Arc-en-ciel *(Coupe Spéciale)*, Vallée Wario *(Coupe Étoile)*.
4. **Étape Positions — Pascal** (eyebrow) : grille 1..12 (positions 2, 5, 8 marquées « taken »).
5. **Résumé & shocks** (eyebrow) : Pascal · P1 → « Shocks 1 » ; Larmik · P3 → « Shocks 0 ».
6. CTA « **Confirmer** » (toast : « Course enregistrée ✓ » + retour).

### Écran `trackdetails` — « COURSE »

Barre d'app : ← retour + titre **COURSE**.

1. **Carte en-tête** : pastille CM · « **Circuit Mario** » · « Course 1 · Score 58 (+14) ».
2. **Positions & shocks** (eyebrow) : Pascal `P1 · 1 shock` · Larmik `P3 · 0 shock` · Juju `P5 · 2 shocks` · Max `P6 · 0 shock`.
3. Bouton « **Éditer la course** » → `edittrack`.

### Écran `edittrack` — « ÉDITER LA COURSE »

Barre d'app : ← retour + titre **ÉDITER LA COURSE**. Onglets `[data-tabs="et"]` :

- **Circuit** (`et-c`, actif) : champ « Rechercher un circuit » + grille (Circuit Mario / Plage Cheep Cheep).
- **Positions** (`et-p`) : eyebrow « Pascal » + grille 1..12 (positions 2, 7, 10 « taken »).
- **Shocks** (`et-s`) : Pascal · P1 → « Shocks 1 » ; Juju · P5 → « Shocks 2 ».

Pied de page : boutons « **Annuler** » (retour) · « **Confirmer** » (toast : « Course modifiée ✓ » + retour).

### Écran `wardetails` — « DÉTAILS DE LA WAR »

Barre d'app : ← retour + titre **DÉTAILS DE LA WAR**.

1. **Carte score** : Harmonia (HM) **512** — VS — Plombiers du Coin (PC) **472**.
2. **Classement joueurs** (eyebrow) : Larmik `108 pts` · Pascal `96 pts` · Max `92 pts` · Juju `88 pts`.
3. Deux boutons :
   - « **Générer le Tab (PDF)** » (icône share) → `edittab`.
   - « **Voir l'adversaire** » (icône coupe) → `opp` (données : Plombiers du Coin, tag PC, `#ef5350`, wr 85%, 13 wars).
4. Hint : « Règle métier : « Tab » (PDF) n'apparaît qu'en 1v1 / 12 joueurs. »
5. **Courses jouées · 12** (eyebrow) — grille (chacun → `trackdetails`) :
   - Circuit Mario · **58** (+14)
   - Château de Bowser · **36** (−8) *(négatif)*
   - Plage Cheep Cheep · **54** (+10)
   - Désert Sec-Sec · **49** (+2)

### Écran `edittab` — « TAB (PDF) »

Barre d'app : ← retour + titre **TAB (PDF)**.

1. Hint : « Génère un tableau de résultats partageable (image). Saisir les scores adverses (6 à 9 lignes). »
2. **Chips** : `− ligne` / `6 lignes` (actif) / `+ ligne`.
3. Trois lignes de saisie (paires) : « Adversaire 1 » + « Score », « Adversaire 2 » + « Score », « Adversaire 3 » + « Score ».
4. CTA « **Tab classique & partager** » (icône share ; toast : « Tab généré — partage ouvert »).
5. Hint : « Contenu : logos, tags, scores finaux (pénalités incluses), top joueurs avec couronne/argent/bronze. « Tab détaillé » (circuits + courbe) présent mais désactivé dans l'app. »

---

## Pôle Stats

### Écran `stats` — « STATISTIQUES »

Barre d'app : titre **STATISTIQUES** + segmenté `12 j` (actif) / `24 j`.

Onglets `[data-tabs="scope"]` : **Individuelles** (`sc-moi`, actif) / **Équipe** (`sc-eq`).

#### Onglet Individuelles (`sc-moi`)

1. **En-tête** : pastille PA · « **Pascal** » · « Tes performances · 32 wars · 384 courses ».
2. **Ton bilan** (eyebrow) : `66 %` — « 21 V · 3 N · 8 D » — barre V/N/D (65,6 % / 9,4 % / 25 %).
3. **Tes indicateurs** (tuiles) : `98` **Points / war** · `4.8` **Position moy.** · `±2.1` **Régularité** *(tuile Nouveau)* · `1.4` **Shocks / war** · `15` **Meilleure course** (positif) · `2` **Pire course** (négatif).
4. **Ta contribution** `Nouveau` : icône coupe, « **23 % des points de l'équipe** », « 2ᵉ contributeur du roster ».
5. **Ta forme & tes séries** `Nouveau` : icône flamme, « **4 victoires d'affilée** », « Forme +8 % sur 10 wars · record 8 ».
6. **Ta distribution de positions** `Nouveau` : graphe barres P1→P12 (généré) ; pied : « **266** Top 6 · 69 % » / « **118** Bot 6 · 31 % ».
7. **Ton rythme de war** `Nouveau` : `4.2` **Courses 1–6** → `5.4` **Courses 7–12**.
8. **Tes circuits** `Nouveau` : Meilleur (winrate perso) « Circuit Mario 81% » ; Pire « Ch. Bowser 34% ».
9. **Comparatif 12J / 24J** `Nouveau` : 12 j → Winrate `66 %` / Pts/war `98` ; 24 j → Winrate `54 %` / Pts/war `91`.

#### Onglet Équipe (`sc-eq`)

1. **En-tête** : pastille HM · « **Harmonia** » · « Performances collectives · 32 wars ».
2. **Bilan équipe** : `69 %` — « 22 V · 3 N · 7 D » — barre (69 / 9 / 22 %).
3. **Détails équipe** (tuiles) : `551` **Score moyen** · `64 %` **Maps gagnées** · `+53` **Marge moy. V** *(tuile Nouveau)*.
4. **Forme & séries équipe** `Nouveau` : « **3 victoires d'affilée** », « Forme +5 % sur 10 wars · record 9 ».
5. **Contributeurs** `Nouveau` (liste classée) :
   - 1 · LK · **Larmik** · « 26 % des points » · **74%** winrate
   - 2 · PA · **Pascal** (toi) · « 23 % des points » · **66%** winrate
   - 3 · Ju · **Juju** · « 19 % des points » · **61%** winrate
6. **Adversaires** (tuiles) : « Tortues G. » **Le + joué** · « Plombiers » **Le + vaincu** · « Koopa O. » **Le − vaincu**.
7. **Circuits (équipe)** (tuiles) : « Circuit Mario » **Le + joué** · « Plage CC » **Meilleur** · « Ch. Bowser » **Pire**.
8. **Comparatif 12J / 24J** `Nouveau` : 12 j → Winrate `69 %` / Score `551` ; 24 j → Winrate `57 %` / Score `505`.

### Écran `statsfull` — « STATISTIQUES »

Barre d'app : ← retour + titre **STATISTIQUES** + sous-titre dynamique (nom du joueur, défaut « Pascal », injecté par `data-name`).

1. **Bilan** (eyebrow) + lien « Résultats → » (→ `wars`) : `66 %` — « 32 wars jouées / 384 courses » — barre (65,6 / 9,4 / 25 %) ; légende « V **21** · N **3** · D **8** ».
2. **Séries** `Nouveau` : « **4 victoires d'affilée** », « Invaincu depuis **6** wars (V+N) ». Sous-pilules : Record V `8` (S2 25) · Record D `3` (S4 24).
3. **Indicateurs clés** (tuiles) : `542` **Score moyen** · `+47` **Marge moy. V** *(Nouveau)* · `4.8` **Position moy.** · `±2.1` **Régularité** *(Nouveau)* · `23 %` **Part points éq.** *(Nouveau)* · `−14` **Pts pénalités** *(Nouveau)*.
4. **Distribution des positions** `Nouveau` : graphe barres P1→P12 ; pied « **266** Top 6 · 69 % » / « **118** Bot 6 · 31 % ».
5. **Rythme de war** `Nouveau` : `4.2` **Courses 1–6** → `5.4` **Courses 7–12**.
6. **Comparatif 12J / 24J** `Nouveau` : 12 joueurs → Winrate `66 %` / Score moy. `542` ; 24 joueurs → Winrate `54 %` / Score moy. `498`.
7. **Adversaires** : Le + joué « Tortues G. » · Le + vaincu « Plombiers ».
8. **Circuits** : Meilleur « Circuit Mario » · Pire « Ch. Bowser ».

---

## Pôle Classements

### Écran `classements` — « CLASSEMENTS »

Barre d'app : titre **CLASSEMENTS** + icône loupe 🔍 (→ `registry`).

Hint : « Palmarès triable. Touche une ligne pour ouvrir sa fiche statistique. » Onglets `[data-tabs="rk"]` : **Joueurs** / **Adversaires** / **Circuits**.

#### Onglet Joueurs (`joueurs`, actif)

Chips de tri : `Winrate` (actif) / `Score moy.` / `Wars`. Liste (chacun → `statsfull`, `data-name`) :
- 1 · LK · **Larmik** · 41 wars · **74%** winrate
- 2 · PA · **Pascal** (toi) · 32 wars · **66%** winrate
- 3 · Ju · **Juju** · 28 wars · **61%** winrate

#### Onglet Adversaires (`adv`)

1. **En bref** `Nouveau` (insight) : On domine « **Plombiers du Coin** » `85%` / Bête noire « **Koopa Onslaught** » `22%`.
2. Champ « **Rechercher une équipe** ».
3. Chips de tri : `Winrate` (actif) / `Score moy.` / `Occurrences`.
4. Liste (chacun → `opp` avec `data-name/tag/color/wr/wars`) :
   - PC · **Plombiers du Coin** · 13 confrontations · **85%** winrate
   - TG · **Tortues Géniales** · 18 confrontations · **78%** winrate
   - KO · **Koopa Onslaught** · 9 confrontations · **22%** winrate

#### Onglet Circuits (`circuits`)

1. **En bref** `Nouveau` (insight) : Meilleur « **Circuit Mario** » `81%` / Pire « **Château de Bowser** » `34%`.
2. Champ « **Rechercher un circuit** ».
3. Chips de tri : `Winrate` (actif) / `Score moy.` / `Fréquence`.
4. Liste (chacun → `map` avec `data-name/tag/color/wr/wars`) :
   - CM · **Circuit Mario** · joué 16 fois · **81%** winrate
   - CB · **Château de Bowser** · joué 15 fois · **34%** winrate

### Écran `opp` — « ADVERSAIRE »

Barre d'app : ← retour + titre **ADVERSAIRE**. Contenu **dynamique** (rempli via `data-name/tag/color/wr/wars` de la ligne d'origine ; valeurs par défaut = Plombiers du Coin).

1. **En-tête** : pastille (tag) · nom · « **13** confrontations · dernier : hier ».
2. **Bilan face à eux** : `85 %` — « de winrate sur **13** wars » — barre (77 / 8 / 15 %) ; légende « V **10** · N **1** · D **2** ».
3. **5 dernières face à eux** : `V V D V V`.
4. **Séries & scores** `Nouveau` : Série en cours `2 V` · Record `7 V` · Score moy. pour `512` · Score moy. contre `471`.
5. **Historique des wars** : ligne V « Course · 12 joueurs · Hier · **512–472** » (→ `wardetails`).

### Écran `map` — « CIRCUIT »

Barre d'app : ← retour + titre **CIRCUIT**. Contenu **dynamique** (valeurs par défaut = Circuit Mario).

1. **En-tête** : pastille CM · « **Circuit Mario** » · « joué **16** fois · coupe Champignon ».
2. **Performance** : `81 %` — « de winrate sur **16** passages » — barre (81 / 6 / 13 %).
3. **Scores moyens** : Score équipe `612` · Ton score `11.4`.
4. **Top 6 / Bot 6** `Nouveau` : Places Top 6 `28×` (positif) · Places Bot 6 `8×` (négatif).
5. **Meilleur pilote ici** : LK · **Larmik** · « score perso 12.8 » · **88%**.

### Écran `registry` — « ANNUAIRE »

Barre d'app : titre **ANNUAIRE**. Onglets `[data-tabs="rg"]` : **Joueurs** / **Équipes**.

#### Onglet Joueurs (`rg-p`, actif)

Champ « **Rechercher un joueur** ». Liste (chacun → `pplayer`, `data-name/tag/color`) :
- LK · **Larmik** · « Harmonia · 🇫🇷 »
- SH · **Shadow** · « Koopa Onslaught · 🇧🇪 »
- NO · **Nova** · « Fzero Squad · 🇨🇦 »

#### Onglet Équipes (`rg-t`)

Champ « **Rechercher une équipe** ». Liste (chacun → `pteam`, `data-name/tag/color`) :
- PC · **Plombiers du Coin** · « tag PC · 14 membres »
- TG · **Tortues Géniales** · « tag TG · 11 membres »

### Écran `pplayer` — « PROFIL JOUEUR »

Barre d'app : ← retour + titre **PROFIL JOUEUR**. Contenu **dynamique** (avatar/tag/nom injectés ; défaut = Larmik).

1. **Carte profil** : avatar (tag), nom, « 🇫🇷 France · Membre », badge « **Profil MKCentral** ».
2. **Informations** : Équipe `Harmonia` · Membre depuis `Janv. 2023` · Code ami `SW-9876-…` · Discord `@larmik`.
3. CTA « **Voir ses statistiques** » → `statsfull` (`data-name="Larmik"`).
4. Deux boutons : « **Ajouter en ally** » (toast : « Ajouté comme ally ✓ ») · « **Changer le rôle** » (toast : « Rôle modifié : Admin »).
5. Hint : « « Ajouter en ally » visible pour un non-membre · « Changer le rôle » (Membre ↔ Admin) visible pour un Leader. »

### Écran `pteam` — « PROFIL ÉQUIPE »

Barre d'app : ← retour + titre **PROFIL ÉQUIPE**. Contenu **dynamique** (défaut = Plombiers du Coin).

1. **Carte profil** : avatar (tag), nom, « TAG PC · créée 2022 », badge « **Équipe MKCentral** ».
2. **Membres** : Mario (MA, Leader) · Luigi (LU, Membre) — chacun → `pplayer`.
3. CTA « **Voir nos confrontations** » → `opp` (Plombiers du Coin, wr 85%, 13 wars).

---

## Pôle Profil

### Écran `profile` — « PROFIL »

Barre d'app : titre **PROFIL**. Onglets `[data-tabs="pf"]` : **Joueur** / **Équipe**.

#### Onglet Joueur (`pf-j`, actif)

1. **Carte profil** : avatar PA · « **Pascal** » · « 🇫🇷 France · **Leader** » · bio « « Toujours prêt pour une war. » » · badge « **Profil MKCentral** ».
2. **Informations** : Équipe `Harmonia HM` · Membre depuis `Mars 2024` · Code ami `SW-1234-…` · Discord `@pascal_mk` · Inscription `2022` · Rôle `Leader`.
3. CTA « **Voir mes statistiques** » → `stats` (sous-onglet `scope:sc-moi`).
4. **Réglages** (liste `setrow`) :
   - **Rafraîchir les données** — « Joueur · équipe · alliés · adversaires · wars » (toast : « Rafraîchissement… »).
   - **Notifications** — « Alertes push » — toggle (activé).
   - **Multi-roster** — « Stats sur tous les rosters (redémarrage requis) » — toggle (désactivé).
   - **Revoir l'onboarding** — « Tutoriel & connexion Discord » → `signup`.
   - **Debug / Matrice** `Caché` — « Réservé staff » → `debug`. *(Masqué par défaut ; débloqué en tapant 5× la version.)*
   - **Déconnexion** (danger, toast : « Déconnexion… »).
5. **Version** : « Stats MKWorld · v3.0.0 » (cliquable — easter-egg : 5 taps débloquent la ligne Debug, toast « Mode debug débloqué 🕶️ » ; aux 3e/4e taps, toast « N pour débloquer… »).

#### Onglet Équipe (`pf-e`)

1. **Carte profil équipe** : avatar HM · « **Harmonia** » · « TAG HM · Saison 5 » · bio « « Team compétitive FR. » » · badge « **Équipe MKCentral** ».
2. **Informations** : Membres `12` · Alliés `3` · Créée en `2023` · Bilan `21-3-8`.
3. Sous-onglets `[data-tabs="pf2"]` : **Membres** / **Alliés**.
   - **Membres** (`pf2-m`, actif) — chacun → `pplayer` : Larmik (LK, Leader, 41 wars) · Pascal (PA, Leader, toi, 32 wars) · Juju (Ju, Admin, 28 wars) · Max (Ma, Membre, 35 wars).
   - **Alliés** (`pf2-a`) : bouton « **Ajouter un ally** » (toast : « Recherche d'un ally… ») ; Guest1 (G1, Ally, « roster externe ») → `pplayer`.
4. CTA « **Voir les stats de l'équipe** » → `stats` (sous-onglet `scope:sc-eq`).

### Écran `debug` — « DEBUG · MATRICE » `Caché`

Barre d'app : ← retour + titre **DEBUG · MATRICE**.

Hint : « Écran réservé (joueur debug 18595 / mode matrice). Ici, à titre de démo. »

Liste d'actions (`setrow`) :
- **Mettre à jour les tags** (toast : « Tags mis à jour ✓ »).
- **Données LariisBot** — « Discord ID & noms » (toast : « Données LariisBot synchronisées »).
- **Gérer les transferts** — « membre ↔ allié » (toast : « Transferts traités »).
- **Test MKWR** — « Records du monde (mkwrs.com) » → `records`.
- **Test notification** (toast : « Notification envoyée »).

CTA « **Entrer dans la matrice** » (`#matrix-btn`) : bascule le mode matrice (dégradé en niveaux de gris) ; le libellé devient « Sortir de la matrice » ; toasts « Bienvenue dans la matrice » / « Retour à la réalité ».

### Écran `records` — « RECORDS DU MONDE » `Caché`

Barre d'app : ← retour + titre **RECORDS DU MONDE**.

Hint : « Source : mkwrs.com (scraping). Par circuit : temps, pilote, nation, perso, véhicule, splits. »

Lignes de records :
- CM · **Circuit Mario** · « 🇯🇵 Takeshi · Yoshi / Bolide · laps 27.1 / 26.8 / 26.9 » · **1:20.8** WR
- PC · **Plage Cheep Cheep** · « 🇫🇷 Alex · Peach / Sport · laps 25.4 / 25.1 / 25.3 » · **1:15.8** WR
- CB · **Château de Bowser** · « 🇺🇸 Mike · Bowser / Char · laps 30.2 / 29.9 / 30.0 » · **1:30.1** WR

### Écran `signup` — « BIENVENUE »

Barre d'app : ← retour + titre **BIENVENUE**.

1. **Stepper** : `Connexion` (actif) → `Notifs` → `App par défaut` → `Terminé`.
2. **Carte** : avatar « D » (Discord), « **Connexion** », bio « Identifie-toi via Discord pour récupérer ton profil et ton équipe MKCentral. ».
3. CTA « **Se connecter avec Discord** » (toast : « Redirection Discord… »).
4. Hint : « Étapes suivantes : autoriser les notifications, définir l'app par défaut pour les liens statsmkworld.com, puis accès à l'accueil. »
