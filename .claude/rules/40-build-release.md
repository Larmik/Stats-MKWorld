# Règles R8/ProGuard pour les DTO réseau Moshi (build release)

**Portée** : tout DTO de `model/network/**` (Moshi/Retrofit) et toute évolution de
`app/proguard-rules.pro`. Bugs « fonctionne en debug, crashe en release ».

Le build **release** active `isMinifyEnabled = true` (R8), pas le debug. Un DTO mal
protégé marche en debug mais **crashe en release** (`JsonDataException`, NPE, adapter
introuvable) au premier appel réseau concerné (ex. recherche joueurs MKCentral au
3ᵉ caractère → `MKCPlayerResponse`).

Exigences dans `proguard-rules.pro` :

- **Utiliser `.**` (double étoile), jamais `.*`** pour couvrir les sous-packages
  (`model.network.*` ne matche PAS `model.network.mkcentral`) :
  `-keep class fr.harmoniamk.statsmkworld.model.network.** { *; }`.
- **`-keep` (classe + membres), pas seulement `-keepclassmembers`** : les champs
  seuls ne suffisent pas si la classe/adapter est renommé/strippé.
- **Garder les adapters Moshi générés** (`<Fqcn>JsonAdapter`, résolus par réflexion
  via leur nom) : `-keep class **JsonAdapter { <init>(...); <fields>; }`.
- **Garder constructeurs/champs des classes `@JsonClass`** :
  `-keepclassmembers @com.squareup.moshi.JsonClass class * { <init>(...); <fields>; }`.
- Un DTO **sans** `@JsonClass(generateAdapter = true)` est désérialisé par réflexion
  → DOIT être gardé explicitement (couvert par le `-keep … model.network.**` ci-dessus).

Rester **ciblé** : garder les DTO/adapters concernés, ne jamais désactiver la
minification globalement. Toute modif R8 ou DTO réseau doit être **validée sur un
vrai build release** (`./gradlew assembleRelease` + test du flux réseau) —
`compileDebugKotlin` ne déclenche pas R8.
