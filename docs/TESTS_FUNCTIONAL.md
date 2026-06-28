# Plan de tests fonctionnels — Stats MKWorld

> Catalogue exhaustif des cas d'usage utilisateur à valider manuellement sur appareil.
> Périmètre : **toutes les actions utilisateur** de l'application. **Hors écran Debug** (réservé).
> Voir aussi [FUNCTIONAL.md](FUNCTIONAL.md) (description des écrans) et [TECHNICAL.md](TECHNICAL.md).

## Conventions

- **Préc.** = préconditions · **Étapes** = actions à effectuer · **Attendu** = résultat attendu.
- ⚙️ = le cas exerce une opération **réseau / Firebase / base locale** récemment migrée en `suspend` (zone à surveiller en priorité pour les régressions de migration).
- 🔁 = comporte une écriture **temps réel** (war en cours écoutée via Firebase).
- Modes : **12p** = 1 adversaire (6v6) · **24p** = 3 adversaires (tournoi).
- Sauf mention contraire, tester chaque cas **en 12p ET en 24p**.
- 🤖 = cas couvert par un flow **Maestro** automatisé (`.maestro/flows/`).

---

## Couverture automatisée (Maestro)

Suite dans `.maestro/flows/` — exécuter : `maestro test .maestro/flows`.
Pré-requis : appareil connecté + app **déjà connectée** (login Discord manuel une fois). Build **debug** = environnement Firebase séparé (écritures sans risque).

| Flow | Cas couverts | Statut |
|---|---|---|
| `01_accueil_smoke` | 4.1, 4.4 | ✅ |
| `02_navigation` | 3.1 (3 onglets) | ✅ |
| `03_annuaire` | 14.4 (filtre équipes local), 14.2 (recherche joueurs — à durcir avec attente) | ✅ / ⚠️ |
| `04_stats` | 13.1.1 → 13.1.6 (ouverture des 5 catégories) | ✅ |
| `05_historique` | 4.9/4.10 (Voir plus), 10.1, 10.5, 11.1 | ✅ |
| `06_profils` | 15.8 (menu présent sur son profil), 16.1 | ✅ |
| `10_war_lifecycle` | **E2E dans 1 war** : 5.1/5.6/5.9 (création) + 7.x (saisie course, scoring « 61-21 ») + 8.1 (pénalité) + 8.2 (remplacement) + 9.x (édition) + 8.3 (annulation) — **idempotent** | ✅ |
| `16_add_ally_search` | 16.4, 16.5 (ouverture + recherche allié, **sans** ajout) | ✅ |
| `18_refresh` | 15.9 (rafraîchir → `fetchData` migré ⚙️) | ✅ |
| `19_pdf` | 11.2, 12.1, 12.2 (ouverture tableau + lignes) | ✅ |

**Sous-flows** (`.maestro/subflows/`) : `start_war_12p` (démarre une war, auto-nettoyant), `cancel_current_war`.

**Flows manuels non idempotents** (`.maestro/manual/`, à lancer explicitement) :
| Flow | Cas | Raison |
|---|---|---|
| `15_validate_war` | 6.3, 6.10 (12 courses → validation) | ajoute une war à l'historique |
| `20_logout` | 15.13 (déconnexion) | déconnecte → re-login Discord manuel — **à lancer en dernier** |

> **Conventions sélecteurs / pièges Maestro :**
> - `MKButton` et titres `BaseScreen` sont **affichés en MAJUSCULES** mais leur sémantique est en **casse naturelle** → sélectionner en casse naturelle.
> - **Ne pas utiliser `hideKeyboard`** sur l'écran de sélection d'adversaire : il réinitialise la sélection. Le clavier se ferme seul après le choix.
> - Carte « war en cours » : taper **« Courses restantes: N »** (la carte), pas le titre « War en cours ».
> - Mode 12p/24p : taper « 12 joueurs » / « 24 joueurs » sur l'Accueil avant de créer une war.

## Bugs trouvés en campagne

| ID | Sévérité | Cas | Description | Statut |
|---|---|---|---|---|
| BUG-A11Y-1 | 🟠 | (transverse) | `MKButton` faisait `clearAndSetSemantics { }` → libellé des boutons **invisible aux lecteurs d'écran** (TalkBack) et aux outils de test. | ✅ Corrigé (`contentDescription` exposé) |

---

## 1. Démarrage, versionning & liens

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 1.1 ⚙️ | Aucun profil enregistré | Lancer l'app | Splash → écran **Signup** (onboarding) |
| 1.2 ⚙️ | Profil valide en local | Lancer l'app | Splash → écran **Accueil** directement |
| 1.3 ⚙️ | `minimumVersion` (Remote Config) > versionCode | Lancer l'app | Écran **« mise à jour requise »** bloquant, pas d'accès à l'app |
| 1.4 | `minimumVersion` ≤ versionCode | Lancer l'app | Démarrage normal |
| 1.5 ⚙️ | — | Ouvrir un lien `statsmkworld.com?...=code` (retour OAuth Discord) | L'app s'ouvre sur **Signup**, le `code` est injecté et la connexion se poursuit |
| 1.6 | App hors-ligne au démarrage | Lancer l'app | Démarrage sur données locales en cache (pas de crash) |

---

## 2. Onboarding & connexion (Signup)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 2.1 | Premier lancement | Parcourir le pager (START → OPEN_APP → NOTIFICATIONS → AUTH) | Navigation page à page fluide, contenu correct par page |
| 2.2 | Page OPEN_APP (Android 12+) | Cliquer « Ouvrir les réglages » | Ouvre les réglages d'ouverture de liens `statsmkworld.com` |
| 2.3 | Page NOTIFICATIONS (Android 13+) | Cliquer « Activer » | Demande la permission notifications (boîte système) |
| 2.4 ⚙️ | Page AUTH | Cliquer « Se connecter » | Ouvre l'autorisation Discord (OAuth) |
| 2.5 ⚙️ | Discord lié à MKCentral | Terminer l'OAuth | FIND_PLAYER (« récupération… ») → WELCOME (succès ~2 s) → **Accueil**. Profil enregistré, `User` Firebase créé (rôle correct si leader) |
| 2.6 ⚙️ | Discord **non lié** à MKCentral | Terminer l'OAuth | Page **ERROR** (Discord non lié / erreur) |
| 2.7 ⚙️ | Erreur serveur / réseau pendant le fetch | Terminer l'OAuth | Page **ERROR** |
| 2.8 | Page ERROR affichée | Cliquer « Réessayer » | Retour à la page **AUTH** |
| 2.9 ⚙️ | Token déjà présent (reconnexion) | Relancer le flux sans `code` | Token local réutilisé, enchaîne la récupération joueur |

---

## 3. Accueil & navigation (Home – 3 onglets)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 3.1 | Sur Accueil | Basculer entre les onglets Accueil / Stats / Annuaire | Changement d'onglet, **état conservé** par onglet (scroll, saisies) |
| 3.2 | Onglet Annuaire avec recherche en cours | Aller sur Stats puis revenir | La recherche/état de l'annuaire est restauré |
| 3.3 | — | Appuyer « retour » système depuis un sous-écran | Remonte d'un niveau de navigation correctement |

---

## 4. Onglet Accueil (Welcome)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 4.1 ⚙️ | Profil + équipe chargés | Ouvrir l'Accueil | Cartes **joueur** et **équipe** (nom, logo) affichées |
| 4.2 | — | Cliquer la carte **joueur** | Ouvre le profil joueur (soi) |
| 4.3 | — | Cliquer la carte **équipe** | Ouvre le profil équipe (sienne) |
| 4.4 | — | Basculer le sélecteur **12p / 24p** | Le contenu (derniers résultats, point d'entrée stats) se filtre selon le mode |
| 4.5 ⚙️ | role > 0 (ou matrix) **et** aucune war en cours | Observer | Bouton **« Nouvelle war »** visible |
| 4.6 | role = 0 | Observer | Bouton « Nouvelle war » **masqué** |
| 4.7 | Une war en cours existe | Observer | Bouton « Nouvelle war » masqué, **carte war en cours** affichée |
| 4.8 🔁 | War en cours | Cliquer la carte war en cours | Reprend la **war en cours** (live) |
| 4.9 ⚙️ | Wars en historique | Observer « Derniers résultats » | **5 dernières** wars du mode courant ; bouton « voir plus » |
| 4.10 | — | Cliquer « voir plus » | Ouvre l'**historique** |
| 4.11 ⚙️ | 0 war pour le mode | Observer | Liste des derniers résultats vide (pas de crash) |

---

## 5. Création d'une war (AddWar)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 5.1 | Bouton « Nouvelle war » (12p) | Choisir adversaire | Sélection de **1** équipe requise ; « Suivant » actif à 1 |
| 5.2 | Mode 24p | Choisir adversaires | Sélection de **3** équipes requise ; « Suivant » actif à 3 |
| 5.3 | Page 1 | Rechercher une équipe par nom/tag | Filtrage de la liste |
| 5.4 | ≥1 équipe sélectionnée | Cliquer « retour » | Retire la **dernière** équipe sélectionnée |
| 5.5 | Équipes choisies | Observer le nom de war | `TAG - TAGadv1 - TAGadv2 …` |
| 5.6 ⚙️ | Page 2 (composition) | Observer | Joueurs **groupés par roster** (en-têtes collants ; en-tête vide = alliés) |
| 5.7 | Page 2 | Sélectionner/désélectionner des joueurs | Mise en évidence ; « Commencer » actif **uniquement à 6** sélectionnés |
| 5.8 | < 6 ou > 6 sélectionnés | Observer | « Commencer » **inactif** |
| 5.9 ⚙️🔁 | 6 joueurs sélectionnés | Cliquer « Commencer » | War créée (`teamHost`, `teamOpponent`, scores à 0) ; `currentWar` mis à jour en **DB + Firebase** (alliés → `newAllies`, autres → `users`) ; navigation vers la war en cours |

---

## 6. War en cours (CurrentWar)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 6.1 🔁 | War en cours | Ouvrir la war en cours | Scores, pénalités, composition affichés ; mise à jour **temps réel** si modif externe |
| 6.2 | War non terminée (< 12 courses) | Observer le bouton principal | **« Course suivante »** |
| 6.3 | 12p, 12 courses faites | Observer | Bouton **« Valider la war »** |
| 6.4 | 24p, 12 courses faites | Observer | Bouton **« Scores adversaires »** |
| 6.5 | — | Observer la grille des courses jouées | Bordure **verte** si diff +, **rouge** si − (12p) ; **transparente** en 24p |
| 6.6 | — | Cliquer une course de la grille | Ouvre le **détail de la course** |
| 6.7 | — | Cliquer « Plus d'actions » | Ouvre l'écran d'actions (pénalités / remplacement / annulation) |
| 6.8 | 24p, page scores adverses | Saisir un score par équipe, total **≠ 1728** | Toast d'erreur indiquant les points manquants / en trop |
| 6.9 ⚙️🔁 | 24p, total = 1728 | Valider les scores | Pénalités déduites avant écriture ; war validée |
| 6.10 ⚙️🔁 | 12p, 12 courses | Cliquer « Valider la war » | War écrite dans l'**historique** (`wars/{rosterId}`), `currentWar` réinitialisés, war en cours supprimée, retour Accueil |
| 6.11 | Changer de page (pager) | Naviguer entre pages de la war en cours | Pas de perte d'état |

---

## 7. Saisie d'une course (AddTrack)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 7.1 | War en cours | Page **Circuit** : parcourir la grille des 30 circuits | Liste complète, images correctes |
| 7.2 | Page Circuit | Rechercher un circuit par nom/label | Filtrage |
| 7.3 | 12p | Sélectionner un circuit | Passe directement à la saisie des positions |
| 7.4 | 24p | Sélectionner un circuit | Affiche la page **Intermission** (`intermissionsTo`) |
| 7.5 | 24p, page Intermission | Sélectionner une intermission | Combo enregistré (track à 2 circuits) ; passe aux positions |
| 7.6 | 24p, page Intermission | Re-sélectionner le **même** circuit | Annule l'intermission |
| 7.7 | Page **Positions** | Attribuer les places joueur par joueur | Cycle automatique de joueur ; positions déjà prises **masquées** |
| 7.8 | 24p | Observer les cellules de position | Échelle 1–24, cellules plus petites |
| 7.9 | 12p | Observer | Échelle 1–12 |
| 7.10 | Saisie en cours | Cliquer « retour » après ≥1 position | Retire la **dernière** position saisie, revient au joueur précédent |
| 7.11 | Page **Récap** 12p | Observer | Score `host - adverse` (2 chiffres) + `+/−diff` |
| 7.12 | Page Récap 24p | Observer | Progression `score actuel -> nouveau score` |
| 7.13 | Page Récap | **Ajouter** des shocks à un joueur (+) | Compteur de shocks du joueur incrémenté |
| 7.14 | Page Récap | **Retirer** des shocks (−) | Compteur décrémenté (pas en dessous de 0) |
| 7.15 ⚙️🔁 | Page Récap | Cliquer « Confirmer » | `WarTrack` (indices circuit(s), positions, shocks) écrit en Firebase ; scores mis à jour ; retour war en cours |
| 7.16 | — | Vérifier le **barème de points** | 12p : 1ʳᵉ=15…12ᵉ=1 · 24p : 1ʳᵉ=15…24ᵉ=1 (paliers) |

---

## 8. Plus d'actions (CurrentWarActions)

### 8.1 Pénalités
| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 8.1.1 | Onglet Pénalités | Observer la grille | `−10 / −15 / −20` par équipe (hôte + adverses) |
| 8.1.2 | — | Sélectionner une pénalité | Sélection exclusive : **une seule par équipe** à la fois |
| 8.1.3 ⚙️🔁 | Pénalité sélectionnée | Cliquer « Valider » | Pénalité appliquée et **déduite du score** de l'équipe visée ; war en cours mise à jour |

### 8.2 Remplacement
| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 8.2.1 | Onglet Remplacement | Observer | Liste composition actuelle + banc |
| 8.2.2 | — | Sélectionner 1 sortant + 1 entrant | Sélections mises en évidence |
| 8.2.3 ⚙️🔁 | Sortant + entrant choisis | Cliquer « Remplacer » | `currentWar` des **deux** joueurs mis à jour (DB + Firebase) ; retour |
| 8.2.4 | Allié impliqué (rosterId −1) | Remplacer | Écriture côté `newAllies` correcte |

### 8.3 Annulation
| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 8.3.1 | Onglet Annuler | Cliquer « Annuler la war » | Demande de **confirmation** |
| 8.3.2 ⚙️🔁 | Confirmation | Confirmer | `currentWar` de tous réinitialisés, `currentWars/{rosterId}` supprimé, DataStore vidé, retour Accueil |
| 8.3.3 | Confirmation | Annuler la confirmation | Aucune modification, reste sur l'écran |

---

## 9. Détail & édition d'une course

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 9.1 | Une course | Ouvrir le détail | Circuit, score (`points-adverse` 12p / `trackScore` 24p), `+/−diff` (12p), positions + shocks par joueur |
| 9.2 | Course **d'une war en cours** | Observer | Bouton **« Éditer »** visible |
| 9.3 | Course d'une war **historique** | Observer | Bouton « Éditer » **masqué** |
| 9.4 | Édition – onglet Circuit | Changer de circuit | Bouton de validation actif |
| 9.5 | Édition – onglet Positions | Modifier (exactement 6 saisies) | Validation conditionnelle correcte |
| 9.6 | Édition – onglet Shocks | Modifier les shocks | Validation activée |
| 9.7 ⚙️🔁 | Édition validée | Confirmer | Score de la war **recalculé sur l'ensemble** des courses et réécrit (Firebase + DataStore) ; retour war en cours |
| 9.8 | Édition sans changement | Observer | Bouton de validation **inactif** |

---

## 10. Historique (WarList)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 10.1 ⚙️ | Wars existantes | Ouvrir l'historique | Wars **groupées par mois**, triées récent→ancien, en-tête collant + compte |
| 10.2 | — | Basculer 12p/24p | Liste filtrée par mode |
| 10.3 | Multi-roster **désactivé** | Observer | Seules les wars `teamHost == rosterId` |
| 10.4 | Multi-roster **activé** | Observer | Wars de tous les rosters |
| 10.5 | — | Cliquer une war | Ouvre le détail de la war |
| 10.6 ⚙️ | 0 war | Ouvrir l'historique | Liste vide, pas de crash |

---

## 11. Détail d'une war (WarDetails)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 11.1 ⚙️ | Une war historique | Ouvrir le détail | Scores finaux, composition + scores par joueur, roster |
| 11.2 | War **12p** | Observer | Bouton **« Tab »** visible |
| 11.3 | War **24p** | Observer | Bouton « Tab » **masqué** |
| 11.4 | — | Cliquer une course de la grille | Détail de la course |
| 11.5 | — | Observer la grille | Bordures colorées (12p) / transparentes (24p) |

---

## 12. Tableau partageable (EditTab / PDF) — 12p uniquement

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 12.1 | Détail war 12p → « Tab » | Ouvrir l'écran | Champs adverses (nom + score), nombre de lignes par défaut 6 |
| 12.2 | — | `+` / `−` sur le nombre de lignes | Borné **6 à 9** |
| 12.3 | Somme des scores adverses **≠** scoreOpponent | Cliquer « Tab classique » | Toast d'écart, **pas** de génération |
| 12.4 | Somme = scoreOpponent | Cliquer « Tab classique » | PDF/image généré (scores, pénalités), partage proposé (`Intent.ACTION_SEND`) |
| 12.5 | — | Vérifier l'enregistrement | Image en galerie (Android 10+) ou via FileProvider |
| 12.6 | — | Observer « Tab détaillé » | Présent mais **désactivé** (comportement attendu) |
| 12.7 | Joueurs nombreux + pénalités | Générer | Mise en page/hauteur correcte (pas de coupe) |

---

## 13. Statistiques

### 13.1 Menu (StatsMenu)
| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 13.1.1 | Onglet Stats | Basculer 12p/24p | Sélecteur fonctionnel |
| 13.1.2 | — | Ouvrir **Statistiques individuelles** | `PlayerStats(currentPlayer, is24p)` |
| 13.1.3 | — | Ouvrir **Statistiques de l'équipe** | `TeamStats(is24p)` |
| 13.1.4 | — | Ouvrir **Statistiques des joueurs** | Classement des joueurs |
| 13.1.5 | — | Ouvrir **Statistiques des adversaires** | Classement adversaires |
| 13.1.6 | — | Ouvrir **Statistiques des circuits** | Classement circuits |

### 13.2 Écran de stats (StatsScreen)
| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 13.2.1 ⚙️ | PlayerStats | Observer | Bilan V/N/D, taux, score/position moyens, meilleur/pire circuit, plus grosse victoire/pire défaite, tableau par circuit, historique |
| 13.2.2 | PlayerStats / TeamStats | Basculer **Individuel / Équipe** (`onIndivSwitch`) | Bascule des agrégats |
| 13.2.3 | OpponentStats | Observer | Historique des 5 dernières wars face à l'adversaire |
| 13.2.4 | MapStats | Observer | Tops/bottoms d'équipe + positions individuelles ; cellules masquées non pertinentes |
| 13.2.5 | **12p** | Observer affichage des écarts | Format `+/−` (relatif à l'équilibre) |
| 13.2.6 | **24p** | Observer | **Scores absolus** (pas d'écart `+/−`) ; barème 24p (144 pts/course) |
| 13.2.7 ⚙️ | Données vides (joueur sans war) | Ouvrir les stats | Pas de crash, valeurs neutres |

### 13.3 Classements (StatsRanking)
| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 13.3.1 | Adversaires/Circuits | Basculer **Individuel / Équipe** | Recalcul du classement |
| 13.3.2 | — | Rechercher (nom équipe / circuit) | Filtrage |
| 13.3.3 | — | Trier : **Occurrences** | Tri par wars jouées, desc |
| 13.3.4 | — | Trier : **Nom** | Tri alpha, asc |
| 13.3.5 | — | Trier : **Winrate** | Tri par taux, desc |
| 13.3.6 | — | Trier : **Moyenne** | Tri par points/position moyens, desc (cohérent 12p **et** 24p) |
| 13.3.7 | Joueurs | Observer | Liste pré-calculée groupée par roster (en-têtes collants) |
| 13.3.8 | — | Cliquer une cellule | Ouvre l'écran de stats détaillé correspondant |

---

## 14. Annuaire (Registry)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 14.1 | Onglet Joueurs | Saisir **< 3** caractères | Aucune recherche déclenchée |
| 14.2 ⚙️ | Onglet Joueurs | Saisir **≥ 3** caractères | Recherche MKCentral, **toutes les pages** paginées |
| 14.3 ⚙️ | Résultats joueurs | Cliquer un joueur | Ouvre le profil joueur |
| 14.4 | Onglet Équipes | Filtrer par nom/tag | Filtrage **local** (insensible à la casse) |
| 14.5 | — | Cliquer une équipe | Ouvre le profil équipe |
| 14.6 ⚙️ | Recherche réseau en échec | Saisir un terme | Pas de crash (résultat vide) |

---

## 15. Profil joueur (PlayerProfile)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 15.1 ⚙️ | Profil d'un autre joueur | Ouvrir | Avatar, pays, nom, bio, date d'inscription, friend code, Discord, équipe + date, **badge de rôle** |
| 15.2 | Joueur hors équipe, pas soi, pas déjà allié | Observer | Bouton **« Ajouter en allié »** visible |
| 15.3 | Joueur déjà allié | Observer | Mention « Allié » (pas de bouton) |
| 15.4 ⚙️🔁 | Bouton « Ajouter en allié » | Cliquer | Ajout en `newAllies` (Firebase) + DB ; bouton disparaît, devient « Allié » |
| 15.5 | Leader consulte un membre (ni soi ni leader) | Observer | Bouton **« Basculer le rôle »** visible |
| 15.6 ⚙️🔁 | Bouton « Basculer le rôle » | Cliquer | Rôle basculé (membre↔admin) en DB + Firebase ; libellé du bouton mis à jour |
| 15.7 | Profil **d'un autre** | Observer | **Pas** de menu (refresh/notif/multiroster/logout) |
| 15.8 | Profil **soi** (`me`) | Observer | Menu présent |
| 15.9 ⚙️ | Menu soi | « Rafraîchir » | Relance `fetchData` (player→team→allies→teams→wars), `lastUpdate` mis à jour ; dialogue de progression par étape |
| 15.10 | Menu soi | « Notifications » (toggle) | Active/désactive ; demande la permission au besoin |
| 15.11 | Menu soi | « Multi-roster » (toggle) | Bascule ; mention **redémarrage requis** |
| 15.12 | Menu soi | « Déconnexion » | Demande de confirmation |
| 15.13 ⚙️ | Confirmation déconnexion | Confirmer | DB/DataStore vidés, **token Discord révoqué**, retour **Signup** |
| 15.14 | Confirmation déconnexion | Annuler | Aucun changement |
| 15.15 | Retour sur le profil (onResume) | Quitter/revenir | État notifications recalculé correctement |

---

## 16. Profil équipe (TeamProfile)

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 16.1 ⚙️ | Sa propre équipe (`me`) | Ouvrir | 3 contenus : **Membres**, **Alliés**, **Stats** |
| 16.2 | Membres | Observer | Groupés par roster (en-têtes si plusieurs) |
| 16.3 | — | Cliquer un membre | Profil joueur |
| 16.4 | role > 0 | Onglet Alliés | Bouton **« Ajouter un allié »** visible |
| 16.5 ⚙️ | « Ajouter un allié » | Ouvrir la bottom sheet, saisir ≥ 3 car. | Recherche MKCentral paginée, **alliés déjà présents exclus** |
| 16.6 ⚙️🔁 | Résultat | Sélectionner un joueur | Ajout en allié (Firebase + DB), bottom sheet fermée, liste alliés mise à jour |
| 16.7 ⚙️ | **Autre** équipe | Ouvrir | Vue simple (rosters + joueurs), **pas** de gestion d'alliés |
| 16.8 | role = 0 sur sa propre équipe | Onglet Alliés | Bouton « Ajouter un allié » masqué |

---

## 17. Cas transverses

| # | Préc. | Étapes | Attendu |
|---|---|---|---|
| 17.1 🔁 | War en cours, 2 appareils | Modifier la war sur l'appareil A | L'appareil B (war en cours ouverte) se met à jour **en temps réel** |
| 17.2 ⚙️ | Hors-ligne | Naviguer (accueil, historique, stats) | Données en cache affichées, pas de crash |
| 17.3 ⚙️ | Hors-ligne | Tenter une action réseau (créer war, ajouter allié) | Comportement gracieux (pas de crash ; échec silencieux toléré) |
| 17.4 | Permission notifications refusée | Déclencher une notif (fin de synchro) | Pas de notification, pas de crash |
| 17.5 | Permission notifications accordée | Fin de synchro (rafraîchir) | Notification « Données mises à jour » |
| 17.6 | Rôles | Vérifier le gating pour role 0 / 1 / 2 | Actions autorisées conformes (création war, gestion rôles/alliés) |
| 17.7 | Synchro quotidienne (~4 h) | Laisser tourner | `UpdateDataWorker` rafraîchit les données en arrière-plan |
| 17.8 | Équipe synthétique | Créer une war contre **« 6v6 Squad »** | War amicale possible (équipe locale `SQ`) |
| 17.9 ⚙️ | Bascule de mode 12p↔24p | Changer le sélecteur | Stats recalculées (`InitStatsWorker`), affichage cohérent |
| 17.10 | Rotation / changement de configuration | Pivoter l'écran sur divers écrans | Pas de perte d'état ni crash |

---

## Suivi d'exécution

| Légende statut | |
|---|---|
| ✅ | OK |
| ❌ | Bug (à reporter dans la branche `fix-bugs`) |
| ⏭️ | Non testé / non applicable |

> Reporter chaque ❌ avec : n° du cas, étapes exactes, résultat observé, capture/log si possible.

---

*Document de test à compléter au fil des campagnes. Hors périmètre : écran Debug (Update Tags, LariisBot, Transferts, MKWR, Test Notif, Mode Matrix) — réservé.*
