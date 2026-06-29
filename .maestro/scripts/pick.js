// Tirage aléatoire pour war 12p ET 24p : joueurs, positions (+ scores attendus selon le barème), circuits, intermissions.
function shuffle(a){ for (var i=a.length-1;i>0;i--){ var j=Math.floor(Math.random()*(i+1)); var t=a[i]; a[i]=a[j]; a[j]=t } return a }
var pts12={1:15,2:12,3:10,4:9,5:8,6:7,7:6,8:5,9:4,10:3,11:2,12:1}
var pts24={1:15,2:12,3:10,4:9,5:9,6:8,7:8,8:7,9:7,10:6,11:6,12:6,13:5,14:5,15:5,16:4,17:4,18:4,19:3,20:3,21:3,22:2,23:2,24:1}
var players=["Arthug","Flash","Guyal","Larii","Leviossah","Marss","Nosta","nsdive","Royal45","Spara","Phenyyx","Genki35","Quent'"]
// Circuits MK World dans l'ordre de l'enum Maps.kt (code -> label FR). GBR exclu de la sélection (le "?" casse les regex Maestro).
var MAPS=[
 {c:"MBC",l:"Circuit Mario Bros."},{c:"CC",l:"Trophéopolis"},{c:"WS",l:"Mont Tchou Tchou"},{c:"DKS",l:"Spatioport DK"},
 {c:"rDH",l:"Désert du Soleil"},{c:"rSGB",l:"Souk Maskass"},{c:"rWS",l:"Stade Wario"},{c:"rAF",l:"Bateau volant"},
 {c:"rDKP",l:"Alpes DK"},{c:"SP",l:"Pic de l'observatoire"},{c:"rSHS",l:"Cité Sorbet"},{c:"rWSh",l:"Galion de Wario"},
 {c:"rKTB",l:"Plage Koopa"},{c:"FO",l:"Savane sauvage"},{c:"PS",l:"Stade Peach"},{c:"rPB",l:"Plage Peach"},
 {c:"SSS",l:"Cité Fleur-de-Sel"},{c:"rDDJ",l:"Jungle Dino Dino"},{c:"GBR",l:"Bloc ? antique"},{c:"CCF",l:"Chutes Cheep Cheep"},
 {c:"DD",l:"Gouffre Pissenlit"},{c:"BCi",l:"Cinéma Boo"},{c:"DBB",l:"Fournaise osseuse"},{c:"rMMM",l:"Prairie Meuh Meuh"},
 {c:"rCM",l:"Montagne Choco"},{c:"rTF",l:"Usine Toad"},{c:"BC",l:"Château de Bowser"},{c:"AH",l:"Chemin du chêne"},
 {c:"MC",l:"Circuit Mario"},{c:"RR",l:"Route Arc-en-ciel"}
]
// Intermissions « depuis » (Maps.intermissionsFrom) : code -> codes atteignables.
var FROM={
 MBC:["rWS","rTF","rCM","CC","WS","rDH","rSGB"], CC:["rCM","rMMM","PS","FO","rKTB","DKS","WS","rDH","MBC","rWS"],
 WS:["rDH","MBC","rCM","CC","rKTB","DKS"], DKS:["WS","rDH","MBC","CC","PS","rKTB"], rDH:["rSGB","MBC","CC","rKTB","WS"],
 rSGB:["rAF","rWS","rCM","MBC","rDH"], rWS:["BC","DBB","rTF","rCM","CC","MBC","rSGB","rAF"], rAF:["BC","DBB","rTF","rWS","rSGB"],
 rDKP:["SP","rSHS","rWSh","SSS","CCF","rMMM","DD"], SP:["rSHS","rWSh","rDKP","CCF","DD","MC","BCi"],
 rSHS:["rWSh","SSS","rDKP","CCF","DD","SP"], rWSh:["rPB","SSS","CCF","rDKP","SP","rSHS"], rKTB:["DKS","CC","PS","FO","rDDJ"],
 FO:["CCF","SSS","rPB","GBR","rDDJ","rKTB","CC","PS"], PS:["rMMM","CCF","FO","rKTB","CC","rCM","rTF","RR"],
 rPB:["GBR","rDDJ","FO","SSS","rWSh"], SSS:["rDKP","rWSh","rPB","GBR","rDDJ","FO","CCF"], rDDJ:["rKTB","FO","SSS","rPB","GBR"],
 GBR:["rDDJ","rKTB","FO","SSS","rPB"], CCF:["DD","SP","rDKP","rWSh","SSS","FO","PS","rCM","rMMM"],
 DD:["BCi","SP","rSHS","rDKP","CCF","rMMM","rTF","MC","AH"], BCi:["SP","DD","MC","DBB","AH"],
 DBB:["AH","BCi","MC","rMMM","rTF","rWS","rAF","BC"], rMMM:["MC","DD","rDKP","CCF","PS","CC","rCM","rTF","DBB"],
 rCM:["rTF","rMMM","CCF","PS","CC","WS","MBC","rSGB","rWS","BC"], rTF:["DBB","AH","MC","DD","rMMM","PS","rCM","MBC","rWS","rAF","BC"],
 BC:["DBB","MC","rTF","rCM","rWS","rAF"], AH:["BCi","DD","MC","rTF","DBB"], MC:["AH","BCi","SP","DD","rMMM","PS","rTF","BC","DBB"], RR:[]
}
function labelOf(code){ for (var i=0;i<MAPS.length;i++) if (MAPS[i].c===code) return MAPS[i].l; return null }
function safe(label){ return label.indexOf("?")<0 } // labels regex-safe pour Maestro
// Maps.intermissionsTo(code) : tous les circuits X dont FROM[X] contient code.
function intermissionsTo(code){ var r=[]; for (var k in FROM) if (FROM[k].indexOf(code)>=0) r.push(k); return r }

var pl=shuffle(players.slice())
for (var i=0;i<6;i++) output["player"+i]=pl[i]
output.subOut=pl[0]; output.subIn=pl[6]
// 12p : positions 1-12 + score "host - opp"
var p12=shuffle([1,2,3,4,5,6,7,8,9,10,11,12]).slice(0,6)
for (var i=0;i<6;i++) output["p"+i]=String(p12[i])
var h12=0; for (var i=0;i<6;i++) h12+=pts12[p12[i]]
output.score=h12+" - "+(82-h12)
// 24p : positions 1-24 + score d'équipe (progression "0 -> host")
var a24=[]; for (var k=1;k<=24;k++) a24.push(k)
var p24=shuffle(a24).slice(0,6)
for (var i=0;i<6;i++) output["q"+i]=String(p24[i])
var h24=0; for (var i=0;i<6;i++) h24+=pts24[p24[i]]
output.score24=String(h24)

// Circuits : on tire parmi les labels regex-safe (GBR exclu).
var circuits=MAPS.filter(function(m){return safe(m.l)}).map(function(m){return m.l})
var c=shuffle(circuits.slice()); output.circuit=c[0]; output.circuit2=c[1]

// Intermission 24p ALÉATOIRE : on cherche le code du circuit tiré, on liste ses intermissions possibles
// (regex-safe), puis ~50% du temps on en sélectionne une, sinon on l'ignore (= re-sélection du même circuit).
var circuitCode=null; for (var i=0;i<MAPS.length;i++) if (MAPS[i].l===output.circuit) circuitCode=MAPS[i].c
var inters=intermissionsTo(circuitCode).map(labelOf).filter(function(l){return l && safe(l)})
if (inters.length>0 && Math.random()<0.5){ output.intermission=shuffle(inters)[0]; output.intermissionMode="select" }
else { output.intermission=output.circuit; output.intermissionMode="ignore" }

console.log("PICK -> players="+pl.slice(0,6).join(",")+" | circuit="+output.circuit+"->"+output.circuit2+" | p12="+p12.join(",")+" score="+output.score+" | p24="+p24.join(",")+" score24="+output.score24+" | intermission["+output.intermissionMode+"]="+output.intermission)
