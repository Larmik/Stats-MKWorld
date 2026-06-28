// Tirage aléatoire pour war 12p ET 24p : joueurs, positions (+ scores attendus selon le barème), circuits.
function shuffle(a){ for (var i=a.length-1;i>0;i--){ var j=Math.floor(Math.random()*(i+1)); var t=a[i]; a[i]=a[j]; a[j]=t } return a }
var pts12={1:15,2:12,3:10,4:9,5:8,6:7,7:6,8:5,9:4,10:3,11:2,12:1}
var pts24={1:15,2:12,3:10,4:9,5:9,6:8,7:8,8:7,9:7,10:6,11:6,12:6,13:5,14:5,15:5,16:4,17:4,18:4,19:3,20:3,21:3,22:2,23:2,24:1}
var players=["Arthug","Flash","Guyal","Larii","Leviossah","Marss","Nosta","nsdive","Royal45","Spara","Phenyyx","Genki35","Quent'"]
var circuits=["Circuit Mario Bros.","Trophéopolis","Mont Tchou Tchou","Spatioport DK","Désert du Soleil","Souk Maskass","Stade Wario","Bateau volant","Alpes DK","Pic de l'observatoire","Cité Sorbet","Galion de Wario","Plage Koopa","Savane sauvage","Stade Peach","Plage Peach","Cité Fleur-de-Sel","Jungle Dino Dino","Chutes Cheep Cheep","Gouffre Pissenlit","Cinéma Boo","Fournaise osseuse","Prairie Meuh Meuh","Montagne Choco","Usine Toad","Château de Bowser","Chemin du chêne","Circuit Mario","Route Arc-en-ciel"]
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
var c=shuffle(circuits.slice()); output.circuit=c[0]; output.circuit2=c[1]
console.log("PICK -> players="+pl.slice(0,6).join(",")+" | circuit="+output.circuit+"->"+output.circuit2+" | p12="+p12.join(",")+" score="+output.score+" | p24="+p24.join(",")+" score24="+output.score24)
