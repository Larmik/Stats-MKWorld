# Mutualiser un composant UI dès un 2ᵉ écran consommateur (pas de duplication)

**Portée** : tout composant Compose de présentation réutilisable — cellule de
liste/podium, carte, en-tête, tuile, sélecteur… (`ui/`, `ui/cells/`, `ui/stats/`).
S'applique dès qu'un composant défini/privé dans un écran est **réutilisé par un
second écran**.

Un composant de rendu défini **localement** à un écran (fonction `private
@Composable` dans `XxxScreen.kt`) ne doit **pas être dupliqué** (copier/coller,
réécriture équivalente) quand un **2ᵉ écran** en a besoin. Dès qu'il y a **≥ 2
écrans consommateurs distincts**, l'**extraire** dans le package UI partagé
approprié et le rendre **public** :

- cellule de liste / résultat / profil → `ui/cells/` ;
- cellule/carte de statistiques (podium, ranking, forme…) → `ui/stats/` ;
- composant transverse (bouton, texte, dialog, champ, sélecteur…) → `ui/`,
  préfixe `MK` (cf. `MKButton`, `MKSegmentedSelector`).

Généralise le principe « pas d'extraction pour un seul appelant » (`61`, `30`) :
**un seul** consommateur → rester **local/inline** ; **≥ 2** consommateurs → **un
composant partagé unique**, jamais deux copies qui divergeront.

Exigences à l'extraction :

- **Un seul exemplaire** : supprimer la version locale d'origine et faire pointer
  l'écran initial sur la version partagée (pas de doublon résiduel).
- **Généraliser par paramètres**, pas par fork : couvrir les besoins des deux écrans
  via des **paramètres optionnels** (`onClick: (() -> Unit)? = null`, variantes de
  contenu, `onDark`…) plutôt que dupliquer une variante. Cf. `MKSegmentedSelector`
  (param `onDark`), `PodiumCell` (support initiales joueur + `onClick` optionnel).
- **Rester cohérent avec les rules composants** (`15` : réutiliser/adapter l'existant,
  segmented partagé unique ; `13` : pixel-perfect ; `12` : roster/avatar). L'extraction
  ne doit pas régresser l'écran d'origine.

**Exemple appliqué (#26)** : `PodiumCell` (initialement `private` dans
`StatsFullScreen.kt`) réutilisé par l'écran Classements pour les cellules
Joueurs/Adversaires/Circuits → **extrait vers `ui/stats/MKPodiumCell.kt`**
(`PodiumEntry`/`PodiumRow`/`PodiumCell`/`initialsOf` publics, ajout initiales +
`onClick`), la version locale de `StatsFullScreen` supprimée au profit de l'import
partagé.

## `MKButton` : style UNIQUE dans toute l'app (pas de variante primaire/secondaire)

**Portée** : tout `MKButton` et tout nouveau bouton d'action.

`MKButton` est le **SEUL** composant bouton de l'app, avec **un seul style** : fond
**blanc translucide** (`Colors.white30`), **SANS bordure**, libellé **et icône blancs**
en majuscules (Urbanist), coins 10 dp — sur **demande utilisateur** (#50 : garder le fond
blanc transparent du `.btn2` mais **retirer la bordure** `whiteBorderSoft` jugée
disgracieuse — c'était le seul vrai souci). Ne **pas** réintroduire la bordure, ni un fond
plein/opaque, ni de variante de style (l'ancien `MKButtonStyle` `Gradient`/`.cta` +
`Minor`/`.btn2` supprimé), ni de second composant bouton (`WarActionButton` **fusionné
dans `MKButton`**). Params variables (pas des variantes de style) : `textColor` et
`icon: Int?` (drawable de tête optionnel — couvre « Générer le Tab » / « Voir
l'adversaire »). **`textColor` s'adapte au fond hôte** : blanc par défaut (sur le dégradé
`BaseScreen` / cartes sombres), **`Colors.black` sur une surface CLAIRE** (les 2 boutons
de `MKDialog`, fond blanc) où le blanc serait illisible ; l'état désactivé atténue la
couleur demandée (`textColor.copy(alpha = 0.4f)`), lisible quel que soit le fond.

- **Divergence assumée vs maquette (rules 13/15)** : le prototype propose un CTA dégradé
  (`.cta`) et un secondaire translucide bordé (`.btn2`) ; l'app retient **un unique bouton
  translucide sans bordure** (ni dégradé, ni bordé) — hiérarchie primaire/secondaire
  aplatie. Ne pas « rétablir » la bordure, le dégradé ni la hiérarchie au nom du
  pixel-perfect.
- **Besoin non couvert par `MKButton`** (icône, largeur, contenu…) → **généraliser
  `MKButton` par un paramètre optionnel** (ex. `icon`), **jamais** créer un second
  composant bouton (ce fut l'erreur corrigée avec `WarActionButton`).
- **Plusieurs boutons sur une même ligne (`Row`) → largeurs ÉGALES** : chaque bouton
  reçoit `Modifier.weight(1f)` (+ `Arrangement.spacedBy(9.dp)` pour l'espace), afin qu'ils
  occupent une largeur identique et remplissent la ligne. Vaut dans **toute** l'app (pieds
  de wizard Précédent/Suivant, Confirmer/Annuler, détails de war, dialog à 2 boutons,
  panneau « Annuler la war »…). Un **bouton seul** sur sa ligne n'est **pas** concerné
  (garde sa largeur intrinsèque/centrée). Un bouton « danger » ad hoc (Box rouge) partageant
  la ligne suit la même règle (`weight(1f)`).
- **État désactivé sans boîte Material** : `MKButton` pose son fond sur son `Row` interne,
  le container Material doit être transparent **à l'état actif ET désactivé**. Forcer dans
  `ButtonDefaults.buttonColors(...)` **`disabledContainerColor = Color.Transparent`** (et
  `containerColor`/`disabledContentColor` transparents) + `buttonElevation(disabledElevation
  = 0.dp)` : sinon le `disabledContainerColor` par défaut (gris `onSurface .12`) réaffiche
  une boîte/halo disgracieux derrière le bouton désactivé.
