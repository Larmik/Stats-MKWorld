package fr.harmoniamk.statsmkworld.model.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.harmoniamk.statsmkworld.R

enum class Maps(@StringRes val label: Int, @DrawableRes val picture: Int, @DrawableRes val cup: Int, @DrawableRes val background: Int) {

    MBC(R.string.mbc, R.drawable.mbc, R.drawable.mushroom, R.drawable.mbc_tab_bg),
    CC(R.string.cc1, R.drawable.cc1, R.drawable.mushroom, R.drawable.cc_tab_bg),
    WS(R.string.ws, R.drawable.ws, R.drawable.mushroom, R.drawable.ws_tab_bg),
    DKS(R.string.dks, R.drawable.dks, R.drawable.mushroom, R.drawable.dks_tab_bg),
    rDH(R.string.rdh, R.drawable.rdh, R.drawable.flower, R.drawable.rdh_tab_bg),
    rSGB(R.string.rsgb, R.drawable.rsgb, R.drawable.flower, R.drawable.rsgb_tab_bg),
    rWS(R.string.rws, R.drawable.rws, R.drawable.flower, R.drawable.rws_tab_bg),
    rAF(R.string.raf, R.drawable.raf, R.drawable.flower, R.drawable.af_tab_bg),
    rDKP(R.string.rdkp, R.drawable.rdkp, R.drawable.star, R.drawable.rdkp_tab_bg),
    SP(R.string.sp, R.drawable.sp, R.drawable.star, R.drawable.sp_tab_bg),
    rSHS(R.string.rshs, R.drawable.rshs, R.drawable.star, R.drawable.rshs_tab_bg),
    rWSh(R.string.rwsh, R.drawable.rwsh, R.drawable.star, R.drawable.rwsh_tab_bg),
    rKTB(R.string.rktb, R.drawable.rktb, R.drawable.shell, R.drawable.rktb_tab_bg),
    FO(R.string.fo, R.drawable.fo, R.drawable.shell, R.drawable.fo_tab_bg),
    PS(R.string.ps1, R.drawable.ps1, R.drawable.shell, R.drawable.ps_tab_bg),
    rPB(R.string.rpb, R.drawable.rpb, R.drawable.banana, R.drawable.rpb_tab_bg),
    SSS(R.string.sss, R.drawable.sss, R.drawable.banana, R.drawable.sss_tab_bg),
    rDDJ(R.string.rddj, R.drawable.rddj, R.drawable.banana, R.drawable.rddj_tab_bg),
    GBR(R.string.gbr, R.drawable.gbr, R.drawable.banana, R.drawable.gbr_tab_bg),
    CCF(R.string.ccf, R.drawable.ccf, R.drawable.leaf, R.drawable.ccf_tab_bg),
    DD(R.string.dd, R.drawable.dd, R.drawable.leaf, R.drawable.dd_tab_bg),
    BCi(R.string.bci, R.drawable.bci, R.drawable.leaf, R.drawable.bci_tab_bg),
    DBB(R.string.dbb, R.drawable.dbb, R.drawable.leaf, R.drawable.dbb_tab_bg),
    rMMM(R.string.rmmm, R.drawable.rmmm, R.drawable.lightling, R.drawable.rmmm_tab_bg),
    rCM(R.string.rcm, R.drawable.rcm, R.drawable.lightling, R.drawable.rcm_tab_bg),
    rTF(R.string.rtf, R.drawable.rtf, R.drawable.lightling, R.drawable.rtf_tab_bg),
    BC(R.string.bc, R.drawable.bc, R.drawable.lightling, R.drawable.bc_tab_bg),
    AH(R.string.ah, R.drawable.ah, R.drawable.special, R.drawable.ah_tab_bg),
    MC(R.string.mc, R.drawable.mc, R.drawable.special, R.drawable.mc_tab_bg),
    RR(R.string.rr, R.drawable.rr, R.drawable.special, R.drawable.rr_tab_bg);

    companion object {
        fun intermissionsFrom(map: Maps): List<Maps> = when (map) {
            MBC -> listOf(rWS, rTF, rCM, CC, WS, rDH, rSGB)
            CC -> listOf(rCM, rMMM, PS, FO, rKTB, DKS, WS, rDH, MBC, rWS)
            WS -> listOf(rDH, MBC, rCM, CC, rKTB, DKS)
            DKS -> listOf(WS, rDH, MBC, CC, PS, rKTB)
            rDH -> listOf(rSGB, MBC, CC, rKTB, WS)
            rSGB -> listOf(rAF, rWS, rCM, MBC, rDH)
            rWS -> listOf(BC, DBB, rTF, rCM, CC, MBC, rSGB, rAF)
            rAF -> listOf(BC, DBB, rTF, rWS, rSGB)
            rDKP -> listOf(SP, rSHS, rWSh, SSS, CCF, rMMM, DD)
            SP -> listOf(rSHS, rWSh, rDKP, CCF, DD, MC, BCi)
            rSHS -> listOf(rWSh, SSS, rDKP, CCF, DD, SP)
            rWSh -> listOf(rPB, SSS, CCF, rDKP, SP, rSHS)
            rKTB -> listOf(DKS, CC, PS, FO, rDDJ)
            FO -> listOf(CCF, SSS, rPB, GBR, rDDJ, rKTB, CC, PS)
            PS -> listOf(rMMM, CCF, FO, rKTB, CC, rCM, rTF, RR)
            rPB -> listOf(GBR, rDDJ, FO, SSS, rWSh)
            SSS -> listOf(rDKP, rWSh, rPB, GBR, rDDJ, FO, CCF)
            rDDJ -> listOf(rKTB, FO, SSS, rPB, GBR)
            GBR -> listOf(rDDJ, rKTB, FO, SSS, rPB)
            CCF -> listOf(DD, SP, rDKP, rWSh, SSS, FO, PS, rCM, rMMM)
            DD -> listOf(BCi, SP, rSHS, rDKP, CCF, rMMM, rTF, MC, AH)
            BCi -> listOf(SP, DD, MC, DBB, AH)
            DBB -> listOf(AH, BCi, MC, rMMM, rTF, rWS, rAF, BC)
            rMMM -> listOf(MC, DD, rDKP, CCF, PS, CC, rCM, rTF, DBB)
            rCM -> listOf(rTF, rMMM, CCF, PS, CC, WS, MBC, rSGB, rWS, BC)
            rTF -> listOf(DBB, AH, MC, DD, rMMM, PS, rCM, MBC, rWS, rAF, BC)
            BC -> listOf(DBB, MC, rTF, rCM, rWS, rAF)
            AH -> listOf(BCi, DD, MC, rTF, DBB)
            MC -> listOf(AH, BCi, SP, DD, rMMM, PS, rTF, BC, DBB)
            RR -> listOf()
        }

        fun intermissionsTo(map: Maps): List<Maps> = Maps.entries.filter { intermissionsFrom(it).contains(map) }

    }
}