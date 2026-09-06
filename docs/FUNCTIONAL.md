# Guide de l'application Stats MKWorld

> **Ton app de suivi et d'analyse des *wars* de Mario Kart World.**
> Ce guide s'adresse à toi, joueur ou joueuse de Mario Kart World qui dispute des matchs d'équipe. Il t'explique, pas à pas et sans aucun terme compliqué, à quoi sert l'application, comment t'y retrouver et comment profiter de toutes ses fonctionnalités. Aucune connaissance technique n'est nécessaire : si tu sais jouer une war, tu sauras utiliser l'app.

---

## Sommaire

1. [Bienvenue — à quoi sert cette application](#1-bienvenue--à-quoi-sert-cette-application)
2. [Le vocabulaire à connaître](#2-le-vocabulaire-à-connaître)
3. [Premiers pas — installation et connexion](#3-premiers-pas--installation-et-connexion)
4. [Se repérer dans l'application](#4-se-repérer-dans-lapplication)
5. [Le pôle Accueil — ton tableau de bord](#5-le-pôle-accueil--ton-tableau-de-bord)
6. [Le pôle Wars — tes matchs](#6-le-pôle-wars--tes-matchs)
7. [Le pôle Stats — tes performances en détail](#7-le-pôle-stats--tes-performances-en-détail)
8. [Le pôle Classements — comparer joueurs, adversaires et circuits](#8-le-pôle-classements--comparer-joueurs-adversaires-et-circuits)
9. [Le pôle Profil — toi, ton équipe et tes réglages](#9-le-pôle-profil--toi-ton-équipe-et-tes-réglages)
10. [L'Annuaire — rechercher un joueur ou une équipe](#10-lannuaire--rechercher-un-joueur-ou-une-équipe)
11. [Enregistrer une war, pas à pas](#11-enregistrer-une-war-pas-à-pas)
12. [Comprendre tes statistiques](#12-comprendre-tes-statistiques)
13. [Les fiches détaillées (adversaire, circuit, joueur)](#13-les-fiches-détaillées-adversaire-circuit-joueur)
14. [Le Tab (PDF) — le tableau de résultats à partager](#14-le-tab-pdf--le-tableau-de-résultats-à-partager)
15. [Rôles, saisons et réglages](#15-rôles-saisons-et-réglages)
16. [Questions fréquentes](#16-questions-fréquentes)

---

## 1. Bienvenue — à quoi sert cette application

**Stats MKWorld** est faite pour les équipes qui jouent à **Mario Kart World** en compétition, sur Nintendo Switch 2. Le jeu peut réunir jusqu'à **24 pilotes** sur une même course, avec **30 circuits** répartis en **8 coupes** (Champignon, Fleur, Étoile, Carapace, Banane, Feuille, Éclair, Spéciale).

Quand ton équipe affronte une autre équipe dans un match organisé (une **war**), tu joues une série de courses et tu accumules des points. L'application te permet de :

- **Enregistrer chaque war**, course après course, pendant que tu joues ;
- **Retrouver l'historique** de tous tes matchs, avec le détail de chaque course ;
- **Analyser tes performances** et celles de ton équipe grâce à des statistiques claires ;
- **Comparer** les joueurs de ton équipe, les adversaires que tu rencontres et les circuits que tu joues ;
- **Générer un Tab (PDF)**, un tableau de résultats à partager avec ton équipe ou tes adversaires.

L'application ne suit **pas** ton classement solo ni tes parties classées : elle est entièrement centrée sur les **wars de ton équipe**.

**Ce qu'il te faut pour commencer :**

- Un **compte MKCentral** (le grand annuaire de la communauté Mario Kart, où sont enregistrés les joueurs et les équipes) ;
- Ton **compte Discord relié à MKCentral** : c'est par Discord que tu te connectes à l'application, qui retrouve alors automatiquement ton profil et ton équipe.

Si ces deux conditions sont remplies, tu es prêt·e. Sinon, crée d'abord ton compte MKCentral et relie-lui ton Discord.

---

## 2. Le vocabulaire à connaître

Voici les mots que tu croiseras dans l'application. Chacun est expliqué simplement. Tu peux revenir à cette section à tout moment.

| Mot | Ce que ça veut dire |
|---|---|
| **War** | Un match entre deux équipes organisées. Une war est composée d'une série de courses (12 en général). |
| **Course** | Une seule course, sur un circuit donné, à l'intérieur d'une war. On parle parfois de « manche ». |
| **12 joueurs** | Le format classique : **6 contre 6**. Ton équipe affronte **une** équipe adverse. C'est le mode principal de l'application. |
| **24 joueurs** | Le grand format : ton équipe affronte **trois** autres équipes en même temps. Les scores finaux se saisissent à la main. |
| **Position → points** | À chaque course, ta place d'arrivée rapporte des points. Une 1ʳᵉ place vaut plus qu'une 12ᵉ (voir le détail plus bas). |
| **Score** | Le total de points de ton équipe sur la war. On le compare à celui de l'adversaire. |
| **Écart** | La différence entre ton score et celui de l'adversaire. Positif (« +40 »), tu domines ; négatif (« -30 »), tu es mené·e. |
| **Pénalité** | Des points retirés à une équipe en sanction (par exemple −10, −15 ou −20). |
| **Shock / Éclair** | L'objet **Éclair** récupéré en course. Très puissant : il rétrécit et ralentit tous les adversaires. Dans l'app, le **nombre d'éclairs obtenus** mesure la qualité du *bagging* (voir ci-dessous). Ce n'est **pas** une faute et cela **n'entre pas** dans le calcul du score. |
| **Bagging** | Rester volontairement à l'arrière pour récupérer des objets puissants — surtout l'éclair — et marquer gros en fin de course. C'est une vraie stratégie, aussi importante que de rouler devant. |
| **Front-running** | L'inverse du bagging : rouler devant et défendre la tête de course. |
| **Host** | Le joueur qui héberge la partie en ligne. S'il se déconnecte, la partie tombe. |
| **Équipe** | Ton clan, tel qu'il est enregistré sur MKCentral. Une équipe peut regrouper plusieurs *rosters*. |
| **Roster** | Une composition précise inscrite sur MKCentral. Une même équipe peut avoir plusieurs rosters (par exemple une équipe principale et une académie). Les wars sont rattachées à un roster. |
| **Tag** | L'identifiant court d'une équipe (quelques lettres), placé devant le pseudo des joueurs. Il change rarement. |
| **Allié** | Un joueur de renfort qui ne fait pas partie du roster officiel mais qui peut jouer une war avec toi. |
| **Saison** | Une période de jeu. L'application découpe l'histoire de ton équipe en saisons, et tu peux filtrer tes stats saison par saison. |
| **Winrate** | Le pourcentage de wars gagnées. Par exemple, 60 % de winrate = 6 wars gagnées sur 10. |
| **All-time** | « Depuis toujours », c'est-à-dire sur tout ton historique, sans limite de temps. |

### Comment les points sont attribués

En **12 joueurs**, chaque place d'arrivée rapporte un nombre de points fixe. Au total, **82 points** sont distribués à chaque course.

| Place | Points | Place | Points |
|---|---|---|---|
| 1ʳᵉ | 15 | 7ᵉ | 6 |
| 2ᵉ | 12 | 8ᵉ | 5 |
| 3ᵉ | 10 | 9ᵉ | 4 |
| 4ᵉ | 9 | 10ᵉ | 3 |
| 5ᵉ | 8 | 11ᵉ | 2 |
| 6ᵉ | 7 | 12ᵉ | 1 |

Comme il y a 82 points par course, le score de l'adversaire se déduit tout seul : **score adverse = 82 × nombre de courses − ton score**. Sur une war de 12 courses, 984 points sont en jeu au total ; il faut donc dépasser **492** pour l'emporter (à égalité, c'est un match nul). Les **pénalités** viennent ensuite s'ajouter ou se retrancher.

En **24 joueurs**, comme trois équipes t'affrontent, l'application ne calcule pas les scores automatiquement : tu saisis à la fin le **score final de chaque équipe adverse**, et l'app établit le classement entre les équipes.

---

## 3. Premiers pas — installation et connexion

Au tout premier lancement, l'application t'accompagne à travers quelques écrans de bienvenue. Laisse-toi guider : chaque étape est courte.

1. **Présentation** — Un rappel de ce qu'il te faut : un compte MKCentral et un Discord relié.
2. **Autoriser l'ouverture des liens** — Sur certains téléphones récents, l'app te propose d'ouvrir un réglage pour qu'elle puisse gérer la connexion. Suis simplement l'indication.
3. **Autoriser les notifications** — Pour être prévenu·e quand tes données sont à jour. Tu peux accepter ou refuser ; tu changeras d'avis plus tard dans les réglages.
4. **Connexion Discord** — Appuie sur « Se connecter avec Discord ». L'app ouvre la page d'autorisation Discord habituelle. Tu autorises, et c'est tout.
5. **Récupération de ton profil** — L'application retrouve ton profil et ton équipe sur MKCentral à partir de ton Discord. Cette étape est automatique.
6. **Bienvenue !** — En cas de succès, tu es redirigé·e vers l'accueil au bout de quelques secondes.

**Si la connexion échoue :** l'app te propose de réessayer. Les causes les plus fréquentes sont : ton **Discord n'est pas relié à MKCentral**, un souci de **réseau**, ou un **serveur momentanément indisponible**. Vérifie d'abord que ton compte Discord est bien lié sur MKCentral, puis réessaie.

Tu n'as **rien d'autre à configurer** : ni mot de passe, ni saisie manuelle de ton pseudo. Tout est récupéré depuis MKCentral.

---

## 4. Se repérer dans l'application

L'application est organisée en **cinq pôles**, accessibles par la **barre en bas de l'écran**. Chaque pôle regroupe un type d'usage :

| Pôle | Ce que tu y trouves |
|---|---|
| **Accueil** | Ton tableau de bord : l'essentiel en un coup d'œil (forme du moment, war en cours, derniers résultats). |
| **Wars** | Tous tes matchs : créer une war, suivre celle en cours, consulter l'historique et les détails. |
| **Stats** | Tes performances et celles de ton équipe, en profondeur. |
| **Classements** | Comparer les joueurs de ton équipe, les adversaires et les circuits. |
| **Profil** | Ton identité, ton équipe, les rôles et tous les réglages. |

**Passer d'un pôle à l'autre** : appuie sur l'icône correspondante dans la barre du bas. L'application garde en mémoire où tu en étais dans chaque pôle.

**La loupe 🔍 (Annuaire)** : en haut des écrans **Accueil** et **Classements**, une icône de recherche ouvre l'**Annuaire**, qui te permet de chercher n'importe quel joueur ou n'importe quelle équipe de MKCentral (voir le [chapitre 10](#10-lannuaire--rechercher-un-joueur-ou-une-équipe)).

**Le bouton retour** : sur les écrans de détail (une war, un circuit, un profil…), une flèche **←** en haut à gauche te ramène à l'écran précédent. Le bouton retour de ton téléphone fait la même chose. Depuis un pôle autre que l'Accueil, le retour te ramène d'abord à l'Accueil ; depuis l'Accueil, il ferme l'application.

**Le filtre par saison** : en haut de plusieurs écrans (Accueil, Wars, Stats, Classements), un **menu déroulant** te laisse choisir la saison à afficher. L'option « Tout l'historique » montre tout ; par défaut, c'est la **saison en cours** qui est sélectionnée. Changer de saison met à jour l'écran immédiatement.

---

## 5. Le pôle Accueil — ton tableau de bord

L'**Accueil** est ta page d'entrée : l'essentiel y est rassemblé pour te donner le pouls de ton équipe en quelques secondes. **Comment y accéder :** icône maison dans la barre du bas (c'est aussi l'écran affiché au lancement).

De haut en bas, tu y trouves :

1. **La carte de salutation** — « Salut, [ton prénom] », avec ta photo (ou tes initiales) et le nom de ton équipe. Appuie dessus pour ouvrir ton **Profil**. Juste en dessous, un sélecteur **« Moi » / « Équipe »** te laisse choisir si le tableau de bord parle de **tes** performances ou de celles de **l'équipe**. Le basculement est instantané.

2. **La war en cours** — Si une war est en cours, une bannière verte **« En direct »** s'affiche avec l'adversaire et le score actuel. Appuie dessus pour **reprendre** la war là où tu l'avais laissée. (S'il n'y a aucune war en cours, cette bannière n'apparaît pas.)

3. **Le Momentum (ta dynamique)** — Un aperçu de ta forme récente. Tu peux choisir la fenêtre **« 5 dernières » ou « 10 dernières »** wars. Tu y vois :
   - une **bande de pastilles** vertes (victoire), blanches (nul) et rouges (défaite) qui résume tes derniers résultats ;
   - une **petite courbe** de l'évolution de ton score (verte si ça monte, rouge si ça descend) ;
   - un **indicateur d'évolution** de ta forme (une flèche ↗ ou ↘ avec un pourcentage), qui compare ta forme récente à ta moyenne de toujours.

4. **Les chiffres clés** — Trois valeurs essentielles : ton **winrate**, ton **score moyen** et une troisième donnée (ta **position moyenne** en vue « Moi », ou le **pourcentage de courses gagnées** en vue « Équipe »).

5. **La série en cours** — Si tu es sur une série de victoires (ou de défaites), un bandeau avec une **flamme** l'annonce : « Série de 4 victoires », avec le rappel de ton record. La flamme est verte pour une série de victoires, rouge pour une série de défaites.

6. **Les derniers résultats** — Tes **3 dernières wars**, chacune cliquable pour ouvrir son détail. Un lien **« Voir tout »** t'emmène vers l'historique complet (pôle Wars).

En haut de l'écran, tu retrouves la **loupe** (vers l'Annuaire) et le **menu de saison** (pour filtrer tout le tableau de bord sur une saison précise).

---

## 6. Le pôle Wars — tes matchs

Le pôle **Wars** rassemble tout ce qui concerne tes matchs. **Comment y accéder :** icône drapeau dans la barre du bas.

En haut de l'écran, le titre indique le **nombre total de wars**. À droite, un bouton **« + » (Créer une war)** te permet de lancer un nouveau match — il disparaît tant qu'une war est déjà en cours (on ne peut suivre qu'une war à la fois). Le détail de la création est expliqué au [chapitre 11](#11-enregistrer-une-war-pas-à-pas).

> **Où reprendre une war en cours ?** Ce n'est pas sur cet écran, mais depuis l'**Accueil** (la bannière verte « En direct »). L'historique des Wars ne montre que les wars **terminées**.

L'écran propose :

- **Des filtres de résultat** : « Tous » (par défaut), « Victoires », « Nuls », « Défaites ». Ils affinent la liste sans quitter l'écran.
- **L'historique complet** de tes wars, **regroupé par mois** et trié de la plus récente à la plus ancienne. Les deux formats (12 et 24 joueurs) apparaissent ensemble. Appuie sur une war pour ouvrir son **détail**.
- **Un menu de saison** en haut pour n'afficher que les wars d'une saison donnée. Le compteur de wars s'ajuste à ta sélection.

### Voir par période

En tête de l'historique, un bouton **« Voir par période »** ouvre un écran d'aide à la composition des équipes. Tu choisis **deux dates** (« Du » et « Au »), et l'app te montre, pour cette plage :

- l'onglet **Wars** : toutes les wars de la période ;
- l'onglet **Joueurs** : le classement des joueurs ayant joué au moins une war sur la période, avec le **nombre de wars jouées et le taux de participation**, le **score moyen** et le **nombre d'éclairs** de chacun.

Par défaut, la période couvre la **saison en cours**, mais tu peux choisir n'importe quelles dates. C'est pratique pour décider qui aligner en fonction de qui a le plus joué récemment.

### Le détail d'une war

Depuis l'historique, appuie sur une war pour ouvrir son détail :

- **La carte de score** : ton équipe face à l'adversaire, avec la **différence de score** au centre (verte si tu gagnes, rouge sinon), les **pénalités** éventuelles de chaque camp et le **total d'éclairs** de la war.
- **Le classement des joueurs** : chaque joueur avec ses points, **classés du meilleur au moins bon**, et son nombre d'éclairs le cas échéant.
- **Deux boutons** : **« Générer le Tab (PDF) »** (uniquement en 12 joueurs) pour créer un tableau de résultats à partager, et **« Voir l'adversaire »** pour ouvrir sa fiche détaillée.
- **Les courses jouées** : la liste des courses de la war, chacune cliquable pour voir le détail (circuit, positions, éclairs).

### Le détail d'une course

Depuis le détail d'une war, appuie sur une course pour l'ouvrir :

- **Le circuit** joué, avec le score de la course et l'écart.
- **Les positions de chaque joueur**, avec le nombre d'éclairs obtenus si applicable.

Tant qu'une war **n'est pas encore validée**, tu peux **éditer** n'importe laquelle de ses courses (bouton « Éditer la course ») : corriger le circuit, une position, ou le nombre d'éclairs. Une fois la war validée, les courses ne sont plus modifiables.

---

## 7. Le pôle Stats — tes performances en détail

Le pôle **Stats** est le cœur analytique de l'application. **Comment y accéder :** icône graphe dans la barre du bas. Il présente tes performances de façon détaillée, avec deux angles de lecture au choix : **« Individuelles »** (toi) ou **« Équipe »** (le collectif).

> Toutes les statistiques portent aujourd'hui sur les **wars 12 joueurs**. Le mode 24 joueurs sera couvert plus tard.

En haut de l'écran, tu disposes de deux outils qui s'appliquent à **tout** l'écran :

- **Le sélecteur de période** : « Tout l'historique » / « 5 dernières » / « 10 dernières ». Il définit sur quelles wars les stats sont calculées.
- **Le menu de saison** : pour restreindre le calcul à une saison précise.

Ces deux réglages sont complémentaires : la saison choisit **quelles** wars comptent, la période prend les **N dernières** parmi elles. L'écran se met à jour immédiatement.

Voici ce que tu y trouves (le contenu s'adapte selon que tu es en « Individuelles » ou en « Équipe ») :

- **Le bilan** : ton **winrate** en grand, le décompte **Victoires / Nuls / Défaites** et une barre proportionnelle.
- **Les indicateurs** : une grille de tuiles (score par war, position moyenne, régularité, marges, pénalités, éclairs, taux de participation…). Beaucoup affichent une **flèche d'évolution** par rapport à ta moyenne de toujours. Chaque indicateur est expliqué au [chapitre 12](#12-comprendre-tes-statistiques).
- **Ta contribution** (en vue Individuelles) : la part des points de l'équipe que tu apportes, et ta part d'éclairs (ton poids dans le bagging collectif).
- **Forme & séries** puis **Records & séries** : ta dynamique du moment, tes meilleurs et pires passages, tes courses parfaites (Top 6) et complètement ratées (Bot 6).
- **La répartition des positions** : un histogramme montrant à quelles places tu (ou l'équipe) finis le plus souvent.
- **Les podiums Circuits et Adversaires** : le Top 3 et le Flop 3 de tes circuits et adversaires, avec un choix de tri (par nombre de fois joué, par winrate ou par score).
- **Contributeurs** et **Meilleurs baggeurs** (en vue Équipe) : le classement des joueurs de l'équipe selon leur part de points, puis selon leur part d'éclairs.

Sur beaucoup d'indicateurs, un petit **bouton d'information (ⓘ)** ouvre une explication en une phrase, pour ne jamais rester bloqué sur le sens d'une stat.

Un lien **« Résultats → »** te renvoie vers l'historique des wars filtré sur le joueur concerné.

---

## 8. Le pôle Classements — comparer joueurs, adversaires et circuits

Le pôle **Classements** te permet de comparer entre eux les joueurs, les adversaires et les circuits. **Comment y accéder :** icône barres dans la barre du bas.

L'écran propose trois onglets :

1. **Joueurs** — La liste de tous les joueurs, séparée en **Membres** (ceux de ton équipe) et **Alliés**. Pour chacun : nombre de wars jouées, **taux de participation**, winrate et score moyen. Appuie sur un joueur pour ouvrir ses **statistiques individuelles**.
2. **Adversaires** — La liste des équipes que tu as affrontées, avec le nombre de confrontations, le winrate face à elles et le score moyen. Appuie sur une équipe pour ouvrir sa **fiche détaillée**.
3. **Circuits** — La liste des circuits joués, avec le nombre de fois joué, le winrate et le score moyen. Appuie sur un circuit pour ouvrir sa **fiche détaillée**.

Sur chaque onglet, tu disposes de :

- **une recherche par nom** ;
- **un tri à trois choix** (par exemple : par nombre de fois joué, par winrate, par score) ;
- **un curseur « occurrences minimum »** : il masque les entrées jouées moins de N fois, pour ne garder que celles où tu as assez de recul. Fais glisser le curseur pour choisir ton seuil ;
- **un menu de saison** en haut, pour restreindre le classement à une saison.

Toutes les listes se recalculent instantanément quand tu changes un réglage.

---

## 9. Le pôle Profil — toi, ton équipe et tes réglages

Le pôle **Profil** regroupe ton identité, celle de ton équipe et tous les réglages. **Comment y accéder :** icône utilisateur dans la barre du bas. Deux onglets : **« Joueur »** et **« Équipe »**.

### Onglet Joueur

- **Ta carte d'identité** : ta photo (ou tes initiales), ton pseudo, ton pays, ta **pastille de rôle** (Membre, Admin ou Leader) et ta bio.
- **Tes informations** : ton équipe et son tag, ta date d'arrivée dans l'équipe, ton code ami, ton Discord, ta date d'inscription et ton rôle.
- **Tes réglages** :
  - **Rafraîchir les données** — Relance la récupération de tes infos (joueur, équipe, alliés, adversaires, wars) depuis MKCentral.
  - **Notifications** — Un interrupteur pour être prévenu·e quand tes données sont à jour.
  - **Multi-roster** — Un interrupteur qui étend le calcul des stats à **tous** les rosters de l'équipe, plutôt qu'au tien seul (nécessite un redémarrage de l'app).
  - **Déconnexion** — Te déconnecte et efface tes données locales.
- **La version de l'application** et la date de la dernière synchronisation, en bas.

### Onglet Équipe

- **La carte d'identité de l'équipe** : logo (ou tag), nom, tag, date de création et bio.
- **Les informations** : nombre de membres, d'alliés et date de création.
- **Deux sous-onglets « Membres » / « Alliés »** :
  - **Membres** : la liste des joueurs de l'équipe, avec leur **rôle réel** (Leader, Admin, Membre). Regroupés par roster si l'équipe en a plusieurs. Appuie sur un membre pour ouvrir sa fiche.
  - **Alliés** : la liste des renforts, plus un bouton **« Ajouter un ally »** pour en enregistrer un nouveau.
- Si tu es **Leader**, un bouton **« Démarrer une nouvelle saison »** apparaît (voir le [chapitre 15](#15-rôles-saisons-et-réglages)).

Les **pastilles de rôle** ont chacune une couleur : **Leader** en or, **Admin** en bleu, **Membre / Allié** en blanc.

---

## 10. L'Annuaire — rechercher un joueur ou une équipe

L'**Annuaire** te permet de chercher n'importe quel joueur ou équipe enregistré sur MKCentral, même en dehors de ton équipe. **Comment y accéder :** la **loupe 🔍** en haut des écrans Accueil et Classements.

Deux onglets :

- **Joueurs** — Tape un nom (à partir de **3 caractères**) pour lancer la recherche. Appuie sur un résultat pour ouvrir son profil. Depuis le profil d'un joueur qui n'est pas dans ton équipe, tu peux (selon ton rôle) l'**ajouter comme allié**.
- **Équipes** — Cherche une équipe par nom ou par tag. Appuie sur un résultat pour ouvrir son profil (rosters et joueurs).

---

## 11. Enregistrer une war, pas à pas

C'est la fonctionnalité centrale de l'application : suivre une war en direct. Voici le parcours complet.

> **Qui peut créer une war ?** Les joueurs ayant le rôle Admin ou Leader (voir le [chapitre 15](#15-rôles-saisons-et-réglages)). Le bouton n'apparaît que s'il n'y a **aucune war en cours**.

### Lancer une nouvelle war

Depuis le pôle **Wars**, appuie sur le bouton **« + » (Créer une war)** en haut à droite. Tu arrives sur un assistant en **trois étapes**, indiquées par une barre en haut : **1 · Adversaire → 2 · Joueurs → 3 · Récap**.

> Pour l'instant, la création se fait uniquement en **12 joueurs**. Le choix du format 24 joueurs sera réactivé plus tard.

### Étape 1 — Choisir l'adversaire

- Cherche l'équipe adverse dans le champ **« Rechercher une équipe / un tag »**. La recherche fonctionne aussi sur les noms de rosters.
- Appuie sur l'équipe voulue.
  - Si elle n'a **qu'un seul roster**, il est retenu automatiquement et tu passes à l'étape suivante.
  - Si elle a **plusieurs rosters**, un petit sélecteur s'ouvre juste sous son nom : choisis le bon roster.

### Étape 2 — Composer ton équipe

- Sélectionne **exactement 6 joueurs** parmi ton roster (et tes alliés). Une pastille verte ✓ apparaît sur chaque joueur choisi ; un compteur « n / 6 » suit ta progression.
- Dès que les 6 joueurs sont sélectionnés, l'assistant passe **automatiquement** à l'étape suivante. (Retirer un joueur te ramène en arrière.)

### Étape 3 — Récapitulatif

- Vérifie l'**adversaire** et ta **composition**.
- Appuie sur **« Démarrer la war »** pour lancer le match.

> **Bon à savoir :** si tu reviens en arrière dans l'assistant, la sélection de l'étape que tu rejoins est **remise à zéro** (revenir tout au début efface l'adversaire *et* la composition). C'est voulu : quand tu recules, c'est pour refaire ton choix.

### La war en cours

Une fois la war lancée, tu arrives sur l'écran de suivi :

- **La carte de score** : ton équipe face à l'adversaire, la différence au centre (colorée), les pénalités éventuelles, le total d'éclairs et le **nombre de courses restantes**.
- **Les scores des joueurs** : chaque joueur avec ses points cumulés (et une pastille d'éclair si applicable).
- **Les actions** : un bouton principal **« Course suivante »** et un bouton **« Plus d'actions »**.
- **Les courses déjà jouées** : une grille de tuiles, une par course, avec le circuit, le score et les éclairs. Appuie sur une course pour la revoir ou la corriger.

> **Tu as changé de téléphone, réinstallé l'app ou été déconnecté·e en pleine war ?** Pas de panique : si tu es bien la personne qui a créé la war, l'application **retrouve automatiquement** tes droits d'édition et tu peux reprendre la saisie.

### Saisir une course

Appuie sur **« Course suivante »**. Un assistant t'accompagne :

1. **Circuit** — Cherche et choisis le circuit joué parmi les 30 disponibles.
2. **Positions** — Saisis la position d'arrivée **joueur par joueur** : pour chaque joueur, appuie sur sa place (de 1 à 12). Les places déjà prises sont verrouillées. Après la dernière position, tu passes au résumé.
3. **Résumé** — L'app affiche le **score de la course** (calculé automatiquement) et te permet, pour chaque joueur, d'**ajouter ou retirer des éclairs** avec les boutons « − » et « + ». Appuie sur **« Confirmer »** pour enregistrer la course.

> En 24 joueurs, une étape **Intermission** s'ajoute (un second circuit enchaîné, optionnel).

### Corriger une course déjà saisie

Tu t'es trompé·e sur une place ou sur un nombre d'éclairs ? Tant que la war **n'est pas encore validée**, tu peux revenir sur n'importe quelle course enregistrée pour la corriger. Depuis la war en cours (ou depuis le détail d'une course), ouvre la course concernée puis appuie sur **« Éditer la course »**. Tu peux alors ajuster :

- le **circuit** joué ;
- les **positions** d'arrivée de chaque joueur (les places doivent rester toutes différentes) ;
- le **nombre d'éclairs** de chaque joueur.

Appuie sur **« Confirmer »** pour enregistrer tes corrections : le score de la war est recalculé automatiquement. Une fois la war validée, les courses ne sont plus modifiables.

### Plus d'actions : pénalités, remplacement, annulation

Le bouton **« Plus d'actions »** ouvre trois onglets :

- **Pénalités** — Applique une pénalité (−10, −15 ou −20) à ton équipe ou à l'adversaire. Choisis le montant et l'équipe visée, puis valide : les points sont déduits du score de l'équipe concernée.
- **Remplacement** — Fais entrer un joueur du banc à la place d'un titulaire, en cours de war. Sélectionne le sortant et l'entrant, puis valide.
- **Annuler la war** — Supprime définitivement la war en cours et toutes ses courses (avec une confirmation). À utiliser si la war n'a finalement pas eu lieu.

### Valider la war

Quand les **12 courses** sont jouées :

- **En 12 joueurs**, le bouton principal devient **« Valider la war »** : appuie dessus pour l'enregistrer dans ton historique.
- **En 24 joueurs**, une carte apparaît pour **saisir le score final de chaque équipe adverse** ; le bouton devient **« Saisir & valider »**. L'app vérifie que le total des scores est cohérent avant d'enregistrer.

Une fois validée, la war rejoint ton **historique** (pôle Wars) et l'écran de war en cours se libère : tu peux en créer une nouvelle.

---

## 12. Comprendre tes statistiques

Cette section explique, en langage simple, **ce que veut dire chaque statistique**, comment la lire et comment t'en servir. Toutes portent sur les wars 12 joueurs. Rappels : une **war** = un match, une **course** = une course d'une war, le **winrate** = le pourcentage de wars gagnées, **all-time** = sur tout l'historique.

Beaucoup de stats existent en deux versions selon l'angle choisi (« Individuelles » = **toi**, « Équipe » = **le collectif**). Quand la lecture diffère, c'est précisé.

### Les stats de bilan

- **Wars jouées** — Le nombre de wars prises en compte. Plus il est grand, plus tes autres stats sont fiables. Une stat calculée sur 3 wars est moins parlante que sur 40.
- **Winrate** — Le pourcentage de wars gagnées. 50 % = autant de victoires que de défaites ; au-dessus, tu gagnes plus que tu ne perds. Les matchs nuls comptent dans le total mais pas comme des victoires.
- **Victoires / Nuls / Défaites (V/N/D)** — Le décompte de tes résultats. Le résultat d'une war est déterminé par le **score final** (pénalités comprises), pas seulement par les places.
- **Points / war** (toi) — Tes points moyens par war. Plus c'est haut, mieux c'est.
- **Score moyen** (équipe) — Ici, c'est l'**écart de score moyen** : positif (« +34 »), l'équipe domine en moyenne ; négatif, elle subit.

### Les stats de course

- **Position moyenne** (toi) — Ta place moyenne sur une course. Plus tu es **proche de la 1ʳᵉ place**, mieux c'est. C'est le seul cas où un chiffre **plus bas** est meilleur.
- **Score moyen par course** (équipe) — Les points moyens marqués par l'équipe sur une course, exprimés en écart. Plus c'est haut, mieux c'est.
- **Maps gagnées** — La part des **courses** remportées par l'équipe. C'est utile pour repérer une équipe qui gagne les courses mais perd les wars serrées (ou l'inverse).
- **Répartition des positions** — Un histogramme montrant à quelles places tu finis le plus souvent. Concentré près de la 1ʳᵉ place = joueur de tête ; étalé = résultats variables.

### Les stats de régularité et de marge

- **Régularité** — À quel point tes scores sont constants d'une war à l'autre. Affichée en « ± X » : plus le chiffre est **bas**, plus tu es régulier·e ; plus il est haut, plus tu fais des hauts et des bas.
- **Amplitude du score (min – max)** — Ton pire et ton meilleur score sur la période. Un grand écart signale des performances variables.
- **Marge moyenne de victoire** — De combien de points tu gagnes en moyenne **quand tu gagnes** (ex. « +45 »).
- **Marge moyenne de défaite** — De combien tu perds en moyenne **quand tu perds** (ex. « -30 »). L'idéal : gagner large et perdre serré.
- **Points perdus en pénalités** — Le total des points retirés à ton équipe par les pénalités. Un repère du « coût » cumulé des sanctions.

### Les séries

Toutes les séries sont calculées dans l'**ordre réel des wars** (par date).

- **Série en cours** — Combien de wars d'affilée tu as gagnées **ou** perdues, à partir de la plus récente. Un match nul ou un résultat inverse remet le compteur à zéro.
- **Record de victoires** — Ta plus longue série de victoires consécutives (ton meilleur passage).
- **Record de défaites** — Ta plus longue série de défaites consécutives (ton pire passage).
- **Invaincu depuis** — Depuis combien de wars tu n'as plus perdu, **nuls compris**. Se remet à zéro à la première défaite.

### Top 6 et Bot 6

- **Top 6** — Une course où **les 6 joueurs de l'équipe occupent les 6 premières places** (1 à 6). C'est une course parfaite. Le compteur indique combien de fois c'est arrivé.
- **Bot 6** — L'inverse : les 6 joueurs occupent les 6 dernières places (7 à 12). Une course complètement dominée par l'adversaire.

Ce sont des cas **exacts** : une course répartie entre le haut et le bas du classement n'est ni un Top 6 ni un Bot 6.

### Le bagging et les éclairs

- **Shocks / war** — Le nombre moyen d'**éclairs obtenus** par war. C'est la mesure de ton *bagging* (rester à l'arrière pour farmer des objets puissants). Un éclair obtenu est compté même si tu ne l'utilises pas.

  **À lire avec nuance :** un grand nombre d'éclairs veut dire que tu bagges beaucoup, ce qui est **précieux** pour l'équipe — mais ce n'est **pas** relié à ta position finale, et il n'y a **pas** de « baggeur attitré » : les rôles changent en pleine course (un joueur devant peut se mettre à bagger, un baggeur qui tire un bon objet peut remonter). C'est pourquoi l'évolution de cette stat s'affiche **sans couleur** : ni « plus » ni « moins » n'est forcément « mieux ». Et rappel : les éclairs **n'entrent pas** dans le calcul du score.

### La contribution

- **Ta contribution** (toi) — La part des points de l'équipe que tu apportes toi-même. Dans une équipe de 6, une contribution « moyenne » tourne autour de 16-17 % ; nettement au-dessus, tu es un moteur de l'équipe.
- **Contributeurs** (équipe) — Le classement des joueurs par part de points apportés.
- **Meilleurs baggeurs** (équipe) — Le même principe, mais classé par part d'éclairs obtenus.

### La forme récente et son évolution

Plusieurs écrans comparent ta forme **récente** à ta moyenne **de toujours**, sous forme d'une flèche colorée avec un chiffre :

- pour le **winrate**, le **% de courses gagnées** et le **score** : une hausse est **bonne** (affichée en vert) ;
- pour la **position moyenne** : une **baisse** est bonne (couleur inversée), puisqu'être plus proche de la 1ʳᵉ place est meilleur ;
- pour les **éclairs** : l'évolution est affichée **sans couleur** (voir plus haut).

**Petit échantillon :** si tu as joué moins de wars que la fenêtre demandée (par exemple 3 wars pour une fenêtre de 10), l'app affiche ce qu'elle a et **n'invente pas** d'évolution trompeuse.

---

## 13. Les fiches détaillées (adversaire, circuit, joueur)

Au-delà des chiffres globaux, l'application propose des **fiches dédiées** pour analyser un adversaire, un circuit ou un joueur en profondeur.

### La fiche d'un adversaire

**Comment y accéder :** depuis le pôle **Classements** (onglet Adversaires), ou depuis le détail d'une war (bouton « Voir l'adversaire »).

Tu y trouves tout ce que tu dois savoir avant de les affronter : le **nombre de confrontations** et la date de la dernière, le **bilan face à eux** (winrate coloré : rouge si tu es en dessous de 50 %, vert au-dessus), tes **5 dernières wars** contre eux, tes **séries** face à eux, ton **score / écart moyen**, tes **éclairs** contre eux, ainsi que les **circuits qui te réussissent le mieux (ou le moins)** contre cette équipe. En vue Équipe, tu vois aussi quels **pilotes** et quels **baggeurs** de ton équipe performent le mieux face à eux. Tout en bas, l'**historique** de tes wars contre cet adversaire.

Un sélecteur **« Joueur » / « Équipe »** te laisse voir ces stats de ton point de vue personnel ou de celui du collectif.

### La fiche d'un circuit

**Comment y accéder :** depuis le pôle **Classements** (onglet Circuits).

Elle rassemble tout sur un circuit : le **nombre de fois joué**, ta **performance** (winrate de course, coloré selon un seuil), le **score moyen** de l'équipe, ta **position moyenne**, tes **éclairs**, la **répartition des positions** sur ce circuit, et — en vue Équipe — quels **pilotes** et **baggeurs** de ton équipe brillent sur ce circuit, ainsi que les **adversaires** rencontrés dessus. Utile pour décider quels circuits privilégier ou éviter.

### La fiche statistique d'un joueur

**Comment y accéder :** depuis le pôle **Classements** (onglet Joueurs) en appuyant sur un joueur.

Elle affiche les **mêmes statistiques que l'onglet « Individuelles » du pôle Stats**, mais pour le joueur choisi : son bilan, ses indicateurs, ses séries, sa répartition de positions, ses meilleurs circuits et adversaires. Un lien **« Résultats → »** ouvre l'historique des wars de ce joueur.

> **Le filtre de saison suit :** si tu ouvres une fiche depuis un classement filtré sur une saison, la fiche reste cohérente avec cette saison.

---

## 14. Le Tab (PDF) — le tableau de résultats à partager

À la fin d'une war en **12 joueurs**, tu peux générer le **Tab (PDF)**, un tableau récapitulatif de résultats à partager (par exemple sur Discord).

**Comment y accéder :** depuis le détail d'une war 12 joueurs, appuie sur **« Générer le Tab (PDF) »**. (Ce bouton n'existe pas en 24 joueurs.)

- Tu peux ajuster le **nombre de lignes adverses** (de 6 à 9) avec les boutons « − ligne » et « + ligne », utile s'il y a eu des remplaçants côté adverse.
- Renseigne le **nom et le score** de chaque joueur adverse.
- Appuie sur **« Tab classique & partager »** : l'app vérifie que les scores sont cohérents, génère le Tab (logos, tags, scores de chaque camp — pénalités comprises, meilleurs joueurs avec médailles) et ouvre le menu de partage.

Un joueur qui a été **remplacé** (il n'a pas joué toutes les courses) voit son nombre de courses jouées indiqué entre parenthèses à côté de son nom (ex. « Pseudo (7) »).

---

## 15. Rôles, saisons et réglages

### Les rôles

Chaque joueur a un **rôle** qui définit ce qu'il peut faire :

| Rôle | Ce qu'il permet |
|---|---|
| **Leader** | Tout : créer des wars, gérer les rosters et les alliés, changer les rôles des membres, démarrer une nouvelle saison. |
| **Admin** | Créer et gérer des wars, changer le rôle d'un membre. |
| **Membre** | Consulter et participer aux wars (mais pas en créer). |

Un **allié** est toujours considéré comme un simple membre : ne faisant pas partie de l'équipe, il ne peut pas la gérer.

**Important :** créer, valider, annuler une war ou remplacer un joueur ne change **jamais** le rôle de qui que ce soit.

Concrètement :

- Le bouton **« Créer une war »** n'apparaît que pour les Admins et Leaders (et seulement si aucune war n'est en cours).
- Le bouton **« Ajouter en allié »** apparaît sur le profil d'un joueur qui n'est pas déjà dans ton équipe.
- Le bouton **« Changer le rôle »** est réservé aux Leaders, sur le profil d'un membre.
- **« Démarrer une nouvelle saison »** est réservé aux **Leaders** (pas aux Admins).

### Les saisons

Une **saison** est une période de jeu de ton équipe. L'application connaît déjà l'historique des saisons passées et en cours, et te laisse **filtrer toutes tes stats** saison par saison (via le menu de saison en haut des écrans).

Si tu es **Leader**, tu peux **démarrer une nouvelle saison** depuis l'onglet Équipe de ton Profil. Une confirmation t'avertit que l'action est **importante et définitive** : elle **clôt** la saison en cours (à la fin de la journée) et en **démarre** une nouvelle (le lendemain). Une saison close ne peut plus être rouverte.

### Les autres réglages

- **Rafraîchir les données** — Met à jour manuellement tes infos (joueur, équipe, alliés, adversaires, wars) depuis MKCentral. L'app le fait aussi automatiquement en tâche de fond environ une fois par jour.
- **Notifications** — Te préviennent quand une mise à jour de tes données est terminée.
- **Multi-roster** — Étend le calcul des stats à tous les rosters de l'équipe. Le changement s'applique après un redémarrage de l'app.
- **Déconnexion** — Te déconnecte, efface tes données locales et te ramène à l'écran de connexion.
- **Fonctionnement sans connexion** — Tes données restent consultables hors ligne (elles sont enregistrées sur ton téléphone). La war en cours, elle, se met à jour en temps réel dès que tu es connecté·e, ce qui permet à toute l'équipe de la suivre.

---

## 16. Questions fréquentes

**Puis-je suivre plusieurs wars à la fois ?**
Non. L'application ne gère qu'**une war en cours à la fois**. Il faut valider ou annuler la war courante avant d'en créer une nouvelle.

**Je ne vois pas le bouton pour créer une war. Pourquoi ?**
Soit une war est déjà en cours (le bouton est alors caché), soit ton rôle ne le permet pas (il faut être Admin ou Leader).

**Un adversaire s'affiche « Équipe inconnue ». C'est grave ?**
Non. Cela arrive quand une équipe adverse a disparu du registre ou n'a jamais été synchronisée. La war reste valable et ses scores sont intacts ; seul le nom ne peut plus être retrouvé.

**Les éclairs (shocks) me font-ils gagner ?**
Pas directement : ils **n'entrent pas** dans le calcul du score. Ils mesurent la qualité de ton *bagging*, une stratégie précieuse, mais indépendante de ta position finale.

**Pourquoi certaines statistiques ne s'affichent pas ?**
Beaucoup de stats demandent un **minimum de wars ou de courses** pour être fiables. Si l'échantillon est trop petit, l'app préfère ne rien afficher plutôt que de donner un chiffre trompeur. Utilise le filtre de période ou le curseur d'occurrences pour ajuster.

**Le mode 24 joueurs est-il pris en charge ?**
Tu peux enregistrer et consulter des wars 24 joueurs, mais les **statistiques détaillées** ne couvrent aujourd'hui que le mode 12 joueurs. Le 24 joueurs sera enrichi plus tard.

---

*Bon jeu, et bonnes wars !*
