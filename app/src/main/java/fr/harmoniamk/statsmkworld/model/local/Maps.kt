package fr.harmoniamk.statsmkworld.model.local

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.harmoniamk.statsmkworld.R

enum class Maps(@StringRes val label: Int, @DrawableRes val picture: Int, @DrawableRes val cup: Int) {

    MBC(R.string.mbc, R.drawable.mbc, R.drawable.mushroom),
    CC(R.string.cc1, R.drawable.cc1, R.drawable.mushroom),
    WS(R.string.ws, R.drawable.ws, R.drawable.mushroom),
    DKS(R.string.dks, R.drawable.dks, R.drawable.mushroom),
    rDH(R.string.rdh, R.drawable.rdh, R.drawable.flower),
    rSGB(R.string.rsgb, R.drawable.rsgb, R.drawable.flower),
    rWS(R.string.rws, R.drawable.rws, R.drawable.flower),
    rAF(R.string.raf, R.drawable.raf, R.drawable.flower),
    rDKP(R.string.rdkp, R.drawable.rdkp, R.drawable.star),
    SP(R.string.sp, R.drawable.sp, R.drawable.star),
    rSHS(R.string.rshs, R.drawable.rshs, R.drawable.star),
    rWSh(R.string.rwsh, R.drawable.rwsh, R.drawable.star),
    rKTB(R.string.rktb, R.drawable.rktb, R.drawable.shell),
    FO(R.string.fo, R.drawable.fo, R.drawable.shell),
    PS(R.string.ps1, R.drawable.ps1, R.drawable.shell),
    rPB(R.string.rpb, R.drawable.rpb, R.drawable.banana),
    SSS(R.string.sss, R.drawable.sss, R.drawable.banana),
    rDDJ(R.string.rddj, R.drawable.rddj, R.drawable.banana),
    GBR(R.string.gbr, R.drawable.gbr, R.drawable.banana),
    CCF(R.string.ccf, R.drawable.ccf, R.drawable.leaf),
    DD(R.string.dd, R.drawable.dd, R.drawable.leaf),
    BCi(R.string.bci, R.drawable.bci, R.drawable.leaf),
    DBB(R.string.dbb, R.drawable.dbb, R.drawable.leaf),
    rMMM(R.string.rmmm, R.drawable.rmmm, R.drawable.lightling),
    rCM(R.string.rcm, R.drawable.rcm, R.drawable.lightling),
    rTF(R.string.rtf, R.drawable.rtf, R.drawable.lightling),
    BC(R.string.bc, R.drawable.bc, R.drawable.lightling),
    AH(R.string.ah, R.drawable.ah, R.drawable.special),
    MC(R.string.mc, R.drawable.mc, R.drawable.special),
    RR(R.string.rr, R.drawable.rr, R.drawable.special),

}