# Règles R8/ProGuard pour les DTO réseau Moshi (build release)

**Portée** : tout DTO de `model/network/**` (Moshi/Retrofit) et toute évolution
de `app/proguard-rules.pro`. Concerne les bugs « fonctionne en debug, crashe en
release ».

Le build **release** active `isMinifyEnabled = true` (R8) ; le debug non. Un DTO
mal protégé se désérialise donc correctement en debug mais **crashe en release**
(`JsonDataException`, NPE, ou adapter introuvable) — typiquement au moment du
premier appel réseau concerné (ex. recherche de joueurs MKCentral au 3ᵉ
caractère → `MKCPlayerResponse`).

Exigences dans `proguard-rules.pro` :

- **Utiliser `.**` (double étoile), jamais `.*`** pour couvrir les
  **sous-packages** : `model.network.*` ne matche PAS `model.network.mkcentral`.
  Règle correcte :
  `-keep class fr.harmoniamk.statsmkworld.model.network.** { *; }`.
- **`-keep` (classe + membres), pas seulement `-keepclassmembers`** : les champs
  seuls ne suffisent pas si la classe DTO ou son adapter est renommé/strippé.
- **Garder les adapters Moshi générés** (`<Fqcn>JsonAdapter`), résolus par
  réflexion via leur nom : `-keep class **JsonAdapter { <init>(...); <fields>; }`.
- **Garder les constructeurs/champs des classes `@JsonClass`** :
  `-keepclassmembers @com.squareup.moshi.JsonClass class * { <init>(...); <fields>; }`.
- Un DTO **sans** `@JsonClass(generateAdapter = true)` est désérialisé par
  réflexion → il DOIT être gardé explicitement (le `-keep … model.network.**`
  ci-dessus le couvre).

Rester **ciblé** : garder les DTO/adapters concernés, ne jamais désactiver la
minification globalement. Toute modification touchant R8 ou un DTO réseau doit
être **validée sur un vrai build release** (`./gradlew assembleRelease` + test du
flux réseau), car `compileDebugKotlin` ne déclenche pas R8.
