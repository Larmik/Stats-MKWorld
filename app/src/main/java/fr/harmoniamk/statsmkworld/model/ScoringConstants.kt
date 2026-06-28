package fr.harmoniamk.statsmkworld.model

object ScoringConstants {
    const val MAX_POINTS_PER_TRACK_12P = 82   // somme des points positions 1-12
    const val MAX_POINTS_PER_TRACK_24P = 144  // somme des points positions 1-24 (= 1728 / 12 manches)
    const val MID_WAR_SCORE = 492             // 6 × 82 : milieu d'une war 12p (12 manches)
    const val MID_WAR_SCORE_24P = 864         // 1728 / 2 : milieu d'une war 24p
    const val MID_TRACK_SCORE = 41            // 82 / 2 : milieu d'une manche 12p
    const val MID_TRACK_SCORE_24P = 72        // 144 / 2 : milieu d'une manche 24p
    const val TOTAL_24P_SCORE = 1728          // total de points d'une war 24 joueurs
    const val DEBUG_PLAYER_ID = "18595"       // identifiant du joueur debug (mode matrix)
}
