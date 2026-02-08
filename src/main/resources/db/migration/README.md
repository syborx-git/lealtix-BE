Migration guidelines

- Never edit an existing versioned migration file (V__*.sql) that may have already been applied in any environment.
- To change schema or seeded data, create a new versioned migration (e.g. V010__add_new_column.sql).
- Use `spring.flyway.baseline-on-migrate=true` only in non-prod/dev environments when first adopting Flyway against an existing database.
- The Flyway Maven plugin is configured to reuse Spring datasource properties when invoked from Maven. Use the commands below with the appropriate profile when running validate/repair locally.

Recommended local commands:

- Start the application using the active profile (dev/local as needed):
  mvn -Dspring-boot.run.profiles=dev spring-boot:run

- Validate migrations (uses application-${spring.profiles.active}.properties via the properties-maven-plugin):
  mvn -Dspring.profiles.active=dev flyway:validate

- Repair metadata checksums (use with caution and only after reviewing changes):
  mvn -Dspring.profiles.active=dev flyway:repair

Notes:
- Avoid committing database credentials to version control. Prefer environment variables or secret management in CI.
- If you need a dev-only migration set, use a dev profile-specific location (e.g. application-dev.properties -> spring.flyway.locations=classpath:db/migration_clean) but avoid having two logical Flyway instances active at the same time.
