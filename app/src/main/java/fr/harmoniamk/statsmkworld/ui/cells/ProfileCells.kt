package fr.harmoniamk.statsmkworld.ui.cells

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.harmoniamk.statsmkworld.R
import fr.harmoniamk.statsmkworld.ui.Colors
import fr.harmoniamk.statsmkworld.ui.Fonts
import fr.harmoniamk.statsmkworld.ui.MKText
import fr.harmoniamk.statsmkworld.ui.stats.StatCard
import fr.harmoniamk.statsmkworld.ui.stats.StatCardRadius

/**
 * Composants de présentation du **pôle Profil** (ticket #28), reproduisant au plus
 * près la maquette 5 pôles (écrans `profile` / `pplayer` / `pteam`). Mutualisés entre
 * le profil fusionné (`ProfileScreen`) et les fiches profil autonomes
 * (`PlayerProfileScreen` / `TeamProfileScreen`) — rule 16.
 *
 * Valeurs de style extraites de `docs/prototype/stats-mkworld-5poles.html`
 * (classes `.pcard`, `.role`, `.two`/`.b`, `.lrow`, `.setrow`, `.badge-mkc`).
 */

/** Rôle affiché dans une pastille (`.role` de la maquette). */
enum class ProfileRole(val labelRes: Int) {
    LEADER(R.string.leader),
    ADMIN(R.string.admin),
    MEMBER(R.string.membre),
    ALLY(R.string.profile_ally_role);

    companion object {
        /** Rôle depuis la valeur du nœud Firebase `users` (2 = Leader, 1 = Admin, 0 = Membre). */
        fun fromFirebaseRole(role: Int): ProfileRole = when (role) {
            2 -> LEADER
            1 -> ADMIN
            else -> MEMBER
        }
    }
}

/**
 * Pastille de rôle (`.role .lead/.adm/.mem`) : fond/texte/bordure teintés selon le
 * rôle. [text] permet de surcharger le libellé (ex. « TAG HM » réutilise le style
 * « membre » gris dans la carte équipe de la maquette).
 */
@Composable
fun RolePill(role: ProfileRole, text: String? = null) {
    val (bg, fg, border) = when (role) {
        // .role.lead : gold translucide + bordure gold.
        ProfileRole.LEADER -> Triple(Colors.gold.copy(alpha = 0.25f), Colors.gold, Colors.gold.copy(alpha = 0.55f))
        // .role.adm : blue translucide + bordure blue.
        ProfileRole.ADMIN -> Triple(Colors.blue.copy(alpha = 0.22f), Colors.blue, Colors.blue.copy(alpha = 0.5f))
        // .role.mem / ally : blanc translucide, texte blanc atténué, sans bordure.
        else -> Triple(Colors.white30, Colors.white85, Colors.transparent)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        MKText(
            text = (text ?: stringResource(role.labelRes)).uppercase(),
            font = Fonts.Urbanist,
            fontSize = 10,
            textColor = fg,
            resizable = false
        )
    }
}

/** Badge « Profil / Équipe MKCentral » (`.badge-mkc`) : bleu translucide, bordure bleue. */
@Composable
fun MkcBadge(labelRes: Int) {
    Box(
        Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Colors.blue.copy(alpha = 0.18f))
            .border(1.dp, Colors.blue.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        MKText(
            text = stringResource(labelRes).uppercase(),
            font = Fonts.Urbanist,
            fontSize = 10,
            textColor = Colors.blue,
            resizable = false
        )
    }
}

/**
 * Carte profil centrée (`.card > .pcard` de la maquette) : avatar rond 76dp, nom
 * (Bungee), ligne meta (pays/tag + rôle), bio en italique, badge MKCentral.
 *
 * @param avatarUrl URL MKCentral déjà préfixée (`https://mkcentral.com…`) → image ;
 *   `null` → pastille [avatarColor] avec [avatarFallback] (initiales / tag).
 * @param metaContent contenu de la ligne meta (pays + rôle joueur, ou tag + saison équipe).
 */
@Composable
fun ProfilePersonCard(
    name: String,
    avatarUrl: String?,
    avatarColor: Color,
    avatarFallback: String,
    badgeRes: Int,
    bio: String? = null,
    metaContent: @Composable () -> Unit
) {
    StatCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
                    .border(3.dp, Colors.white85, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when (avatarUrl) {
                    null -> MKText(text = avatarFallback, font = Fonts.Urbanist, fontSize = 23, textColor = Colors.white, resizable = false)
                    else -> AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(76.dp).clip(CircleShape)
                    )
                }
            }
            MKText(text = name, font = Fonts.Bungee, fontSize = 20, textColor = Colors.white, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                metaContent()
            }
            bio?.takeIf { it.isNotBlank() }?.let {
                MKText(
                    text = "« $it »",
                    font = Fonts.NunitoIT,
                    fontSize = 12,
                    textColor = Colors.white.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp),
                    resizable = false
                )
            }
            Spacer(Modifier.height(8.dp))
            MkcBadge(badgeRes)
        }
    }
}

/** Une entrée « clé → valeur » d'une carte Informations. [valueSmall] = suffixe gris (tag). */
class ProfileInfo(val key: String, val value: String, val valueSmall: String? = null)

/**
 * Carte « Informations » (`.card > .eyebrow + .two`) : grille 2 colonnes de tuiles
 * translucides libellé/valeur (`.b > .k + .v`).
 */
@Composable
fun ProfileInfoCard(infos: List<ProfileInfo>) {
    StatCard(title = stringResource(R.string.profile_information)) {
        infos.chunked(2).forEach { rowInfos ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                rowInfos.forEach { info ->
                    Column(
                        Modifier.weight(1f)
                            .background(Colors.white30, StatCardRadius)
                            .padding(11.dp)
                    ) {
                        MKText(text = info.key.uppercase(), font = Fonts.NunitoBD, fontSize = 10, textColor = Colors.white66, textAlign = TextAlign.Start)
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 5.dp)) {
                            MKText(text = info.value, font = Fonts.Urbanist, fontSize = 15, textColor = Colors.white, textAlign = TextAlign.Start, maxLines = 1)
                            info.valueSmall?.let {
                                Spacer(Modifier.width(5.dp))
                                MKText(text = it, font = Fonts.NunitoBD, fontSize = 11, textColor = Colors.white.copy(alpha = 0.6f), textAlign = TextAlign.Start, maxLines = 1)
                            }
                        }
                    }
                }
                repeat(2 - rowInfos.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Ligne de membre / allié (`.lrow`) : pastille ronde (photo MKCentral [avatarUrl] si
 * disponible, sinon [initials] sur fond [color]), nom + pastille de rôle, sous-texte
 * optionnel (roster externe pour un allié), chevron. Délègue au composant partagé
 * [MKListRow] (rule 16 : un seul exemplaire de la `.lrow`).
 */
@Composable
fun ProfileMemberRow(
    initials: String,
    color: Color,
    name: String,
    role: ProfileRole? = null,
    avatarUrl: String? = null,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    MKListRow(
        modifier = Modifier.fillMaxWidth(),
        initials = initials,
        avatarColor = color,
        name = name,
        avatarUrl = avatarUrl,
        subtitle = subtitle,
        onClick = onClick,
        titleTrailing = { role?.let { RolePill(it) } },
        trailing = { MKListRowChevron() }
    )
}

/**
 * Ligne de réglage (`.setrow`) : icône de tête (`.si`, carré 32dp arrondi translucide)
 * + titre (gras) + sous-titre optionnel, contenu de fin (toggle / chevron). [danger]
 * colore l'icône et le titre en rouge (Déconnexion). Séparateur inférieur optionnel
 * ([divider]).
 *
 * @param leadingIcon drawable de l'icône `.si` (refresh/bell/rank/book/cog/logout de
 *   la maquette).
 */
@Composable
fun ProfileSettingRow(
    title: String,
    leadingIcon: Int,
    subtitle: String? = null,
    danger: Boolean = false,
    divider: Boolean = true,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Colors.white30),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    tint = if (danger) Colors.red else Colors.white,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                MKText(
                    text = title,
                    font = Fonts.NunitoBD,
                    fontSize = 14,
                    textColor = if (danger) Colors.red else Colors.white,
                    textAlign = TextAlign.Start
                )
                subtitle?.let {
                    MKText(text = it, font = Fonts.NunitoRG, fontSize = 11, textColor = Colors.white55, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 2.dp))
                }
            }
            trailing?.invoke() ?: Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Colors.white.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
        if (divider) Spacer(Modifier.fillMaxWidth().height(1.dp).background(Colors.white.copy(alpha = 0.14f)))
    }
}
