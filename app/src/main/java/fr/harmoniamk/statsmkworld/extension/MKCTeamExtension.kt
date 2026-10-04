package fr.harmoniamk.statsmkworld.extension

import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeam
import fr.harmoniamk.statsmkworld.model.network.mkcentral.MKCTeamRoster

/** Rosters mkworld de l'équipe — domaine exclusivement mkworld (rule 31, audit D28). */
fun MKCTeam.mkWorldRosters(): List<MKCTeamRoster> = rosters.filter { it.game == "mkworld" }
