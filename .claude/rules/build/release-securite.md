---
paths:
  - "app/build.gradle.kts"
  - "app/proguard-rules.pro"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/res/xml/backup_rules.xml"
  - "app/src/main/res/xml/data_extraction_rules.xml"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/network/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/firebase/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/model/local/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/database/converters/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/serializers/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/datasource/network/DiscordDataSource.kt"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/repository/DataStoreRepository.kt"
---

# Build release : R8 et données sensibles

## R8 et modèles désérialisés par réflexion

Le release minifie (`isMinifyEnabled = true`), pas le debug : un modèle mal protégé marche en
debug et crashe en release (`JsonDataException`, NPE, adapter introuvable).

- Périmètre : tout modèle (dé)sérialisé par réflexion. Ça couvre `model.network` (Moshi/Retrofit),
  `model.firebase` (`getValue` RTDB), `model.local` (miroirs `Datastore*` et modèles convertis
  par Moshi) et les messages Protobuf.
- Dans `proguard-rules.pro` :
  - utiliser `.**`, jamais `.*` (`model.network.*` ne couvre pas `model.network.mkcentral`) ;
  - `-keep class <package>.** { *; }` (classe + membres), pas seulement `-keepclassmembers` ;
  - garder les adapters Moshi générés : `-keep class **JsonAdapter { <init>(...); <fields>; }` ;
  - garder constructeurs et champs des `@JsonClass` ;
  - un DTO sans `@JsonClass(generateAdapter = true)` passe par réflexion et doit être couvert par
    un `-keep` de package.
- Nouveau package de modèles → ajouter son `-keep … .**`.
- Rester ciblé : ne jamais désactiver la minification.
- Valider toute modif R8 ou DTO sur un vrai `./gradlew assembleRelease` + test du flux
  (`compileDebugKotlin` ne lance pas R8).

## Secrets et sauvegarde

- Aucun secret serveur dans l'APK : un `buildConfigField` ou une ressource se lit par
  décompilation. Seuls des identifiants publics (client id) sont embarqués ; un client secret relève
  d'un backend. Cf. audit A4.
- Tout nouveau stockage de token / identité est exclu de `backup_rules.xml` et
  `data_extraction_rules.xml` (ou `allowBackup=false`). Cf. audit A5.
- Ne jamais lire ni afficher `local.properties`, `google-services.json` ni un keystore.
