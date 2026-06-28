// Tirage aléatoire d'un circuit (visibles sans scroll) + 6 positions distinctes (1-12),
// et calcul du score 12p attendu (barème positionToPoints) pour assertion dynamique.
const pts = {1:15,2:12,3:10,4:9,5:8,6:7,7:6,8:5,9:4,10:3,11:2,12:1}
const circuits = ["Circuit Mario Bros.","Stade Wario","Désert du Soleil","Souk Maskass","Spatioport DK","Mont Tchou Tchou","Bateau volant","Trophéopolis"]
const all = [1,2,3,4,5,6,7,8,9,10,11,12]
for (let i = all.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); const t = all[i]; all[i] = all[j]; all[j] = t }
const chosen = all.slice(0, 6)
const host = chosen.reduce((s, p) => s + pts[p], 0)
output.circuit = circuits[Math.floor(Math.random() * circuits.length)]
output.p0 = String(chosen[0]); output.p1 = String(chosen[1]); output.p2 = String(chosen[2])
output.p3 = String(chosen[3]); output.p4 = String(chosen[4]); output.p5 = String(chosen[5])
output.score = host + " - " + (82 - host)
console.log("RANDOM TRACK -> circuit=" + output.circuit + " positions=" + chosen.join(",") + " score=" + output.score)
