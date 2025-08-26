# ApprovalPlugin

Questo repository contiene il sorgente del plugin *ApprovalPlugin* per Paper/Spigot (Minecraft 1.21.8).
Il workflow GitHub Actions compila automaticamente il .jar e lo pubblica come artifact.

File principali:
- `pom.xml`
- `src/main/java/com/davis/approvalplugin/Main.java`
- `src/main/resources/plugin.yml`
- `.github/workflows/maven.yml`

Procedura rapida:
1. Commit su `main` -> GitHub Actions compila e genera l'artifact `approval-plugin-jar`.
2. Scarica il .jar da Actions → Artifacts.
3. Caricalo nella cartella `plugins/` del server Paper su Falix e riavvia il server.
