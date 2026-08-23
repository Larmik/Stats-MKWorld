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

`MKButton` est le **SEUL** composant bouton de l'app, avec **un seul style** (aligné
`.btn2` maquette : fond blanc translucide `Colors.white30`, bordure
`Colors.whiteBorderSoft`, libellé majuscule Urbanist), sur **demande utilisateur
d'harmonisation** (#50). Ne **pas** réintroduire de variante de style (l'ancien
`MKButtonStyle` `Gradient`/`.cta` + `Minor` a été supprimé) **ni** de second composant
bouton (l'ancien `WarActionButton` de `WarDetailsScreen` a été **fusionné dans
`MKButton`**). Params variables (pas des variantes de style) : `textColor` (contraste
selon le fond : blanc par défaut ; `Colors.black` sur surface claire type `MKDialog`) et
`icon: Int?` (drawable de tête optionnel — couvre les boutons à icône « Générer le Tab »
/ « Voir l'adversaire »).

- **Divergence assumée vs maquette (rules 13/15)** : le prototype distingue un CTA
  primaire dégradé d'un bouton secondaire ; cette hiérarchie est **volontairement
  aplatie**. Ne pas la « rétablir » au nom du pixel-perfect.
- **Besoin non couvert par `MKButton`** (icône, largeur, contenu…) → **généraliser
  `MKButton` par un paramètre optionnel** (ex. `icon`), **jamais** créer un second
  composant bouton (ce fut l'erreur corrigée avec `WarActionButton`).
