// Tirage aléatoire pour une war 12p : 6 joueurs, 6 positions (+ score 12p attendu),
// un remplaçant (hors sélection), et 2 circuits (saisie + édition). Tout est loggé.
function shuffle(a){ for (var i=a.length-1;i>0;i--){ var j=Math.floor(Math.random()*(i+1)); var t=a[i]; a[i]=a[j]; a[j]=t } return a }
var pts={1:15,2:12,3:10,4:9,5:8,6:7,7:6,8:5,9:4,10:3,11:2,12:1}
// Joueurs sûrs (sans caractère regex spécial ; "Zephyr | Manu" exclu)
var players=["Arthug","Flash","Guyal","Larii","Leviossah","Marss","Nosta","nsdive","Royal45","Spara","Phenyyx","Genki35","Quent'"]
// 29 circuits (exclu "Bloc ? antique" : '?' regex)
var circuits=["Circuit Mario Bros.","Trophéopolis","Mont Tchou Tchou","Spatioport DK","Désert du Soleil","Souk Maskass","Stade Wario","Bateau volant","Alpes DK","Pic de l'observatoire","Cité Sorbet","Galion de Wario","Plage Koopa","Savane sauvage","Stade Peach","Plage Peach","Cité Fleur-de-Sel","Jungle Dino Dino","Chutes Cheep Cheep","Gouffre Pissenlit","Cinéma Boo","Fournaise osseuse","Prairie Meuh Meuh","Montagne Choco","Usine Toad","Château de Bowser","Chemin du chêne","Circuit Mario","Route Arc-en-ciel"]
var pl=shuffle(players.slice())
for (var i=0;i<6;i++) output["player"+i]=pl[i]
output.subOut=pl[0]
output.subIn=pl[6]
var pos=shuffle([1,2,3,4,5,6,7,8,9,10,11,12]).slice(0,6)
for (var i=0;i<6;i++) output["p"+i]=String(pos[i])
var host=0; for (var i=0;i<6;i++) host+=pts[pos[i]]
output.score=host+" - "+(82-host)
var c=shuffle(circuits.slice())
output.circuit=c[0]; output.circuit2=c[1]
console.log("PICK -> players="+pl.slice(0,6).join(",")+" | subOut="+output.subOut+" subIn="+output.subIn+" | circuit="+output.circuit+" -> "+output.circuit2+" | pos="+pos.join(",")+" | score="+output.score)
