Istruzioni rapide:


1) Crea un nuovo repository su GitHub (privato o pubblico) e imposta la branch `main`.
2) Crea i file come da questo documento (usa "Add file > Create new file" su GitHub):
- `pom.xml`
- `src/main/java/com/davis/approvalplugin/Main.java`
- `src/main/resources/plugin.yml`
- `.github/workflows/maven.yml`
3) Commit e push sul ramo `main`.
4) Vai su Actions nella pagina del repo e attendi che il workflow finisca. Troverai un artifact chiamato `approval-plugin-jar` scaricabile (il .jar).
5) Scarica il .jar e caricalo nella cartella `plugins/` del tuo server Paper su Falix. Riavvia il server.


Se vuoi, posso preparare un "copy&paste checklist" passo-passo ancora più semplice per ciascuno dei punti sopra.
