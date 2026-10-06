---
paths:
  - "app/src/main/java/fr/harmoniamk/statsmkworld/database/**"
  - "app/src/main/java/fr/harmoniamk/statsmkworld/datasource/local/**"
  - "app/schemas/**"
---

# Room : version, schémas, effacement

- Tout changement d'entité (`database/entities/`) incrémente `version` de `MKDatabase` (10
  actuellement) ; le schéma est exporté dans `app/schemas/`.
- Pas de migration : la base est en `fallbackToDestructiveMigration()`, une montée de version
  efface les données locales, ré-hydratées depuis Firebase / MKCentral. Ne pas écrire de
  migration sans demande.
- Ajouter un champ seulement pour une donnée absente de l'app (pas pour un asset embarqué).
- Les `clear*()` des DAO font `DELETE FROM …` sans filtre : cf. `data/repositories.md`
  (§ écriture destructive).
- Les convertisseurs (`database/converters/`) passent par Moshi ; tout type sérialisé reste
  couvert par les règles R8 (cf. `build/release-securite.md`).
