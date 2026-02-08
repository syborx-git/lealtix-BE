DEPRECATED: db/migration_clean

This folder was used as a development-only set of migrations. The canonical Flyway location for this project is now `classpath:db/migration`.

Files here were copied into `src/main/resources/db/migration/` so the application and the Flyway Maven plugin operate on a single, canonical migration directory.

Recommended local cleanup (Windows cmd.exe):

1) Verify everything is committed and that `src/main/resources/db/migration/` contains the expected migration files.

2) Remove the development folder locally (use with caution):

   rmdir /S /Q src\main\resources\db\migration_clean

3) To also remove from Git, after deletion run:

   git rm -r --cached src/main/resources/db/migration_clean
   git commit -m "Remove legacy migration_clean folder; migrations live in db/migration"
   git push

If you prefer to keep a backup inside the repo, move the folder instead of deleting it (rename to migration_clean.archive or move to backups/). If you want me to perform repository edits instead, tell me and I will stage changes to remove the folder files from the project tree (note: I cannot run git commands from here; I can only edit files in the workspace).