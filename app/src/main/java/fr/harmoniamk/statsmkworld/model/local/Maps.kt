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
    RR(R.string.rr, R.drawable.rr, R.drawable.special, R.drawable.rr_tab_bg),
}