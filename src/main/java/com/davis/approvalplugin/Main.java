package com.davis.approvalplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private final Set<String> approvedPlayers = new HashSet<>();
    private final String adminName = "Davis1070"; // tuo username

    @Override
    public void onEnable() {
        // registra gli eventi
        getServer().getPluginManager().registerEvents(this, this);

        // registra il comando /approve (controlla null per sicurezza)
        if (getCommand("approve") != null) {
            getCommand("approve").setExecutor((sender, command, label, args) -> {
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Uso corretto: /approve <player>");
                    return true;
                }
                String target = args[0];
                approvedPlayers.add(target.toLowerCase());
                Player p = Bukkit.getPlayerExact(target);
                if (p != null) {
                    unblockPlayer(p);
                }
                sender.sendMessage(ChatColor.GREEN + "Hai approvato " + target);
                return true;
            });
        } else {
            getLogger().warning("Comando /approve non registrato (plugin.yml?).");
        }

        getLogger().info("ApprovalPlugin attivato!");
    }

    @Override
    public void onDisable() {
        approvedPlayers.clear();
        getLogger().info("ApprovalPlugin disattivato.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getName().equalsIgnoreCase(adminName)) {
            return; // tu sei libero
        }

        if (approvedPlayers.contains(player.getName().toLowerCase())) {
            // già approvato in questa sessione
            unblockPlayer(player);
            return;
        }

        // blocca il giocatore
        blockPlayer(player);

        // notifica admin in console e in chat (se online)
        String msg = ChatColor.YELLOW + "Giocatore " + player.getName() + " vuole entrare! Usa /approve " + player.getName() + " per sbloccarlo.";
        Bukkit.getLogger().info(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(adminName)) {
                p.sendMessage(msg);
            }
        }
    }

    // Blocca movimento, salto e comandi (impossibile muoversi o ruotare)
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(adminName)) return;
        if (approvedPlayers.contains(player.getName().toLowerCase())) return;

        // Resetta posizione e rotazione al valore precedente
        event.setTo(event.getFrom());
        // Azzeriamo eventuale velocity residua
        player.setVelocity(new Vector(0, 0, 0));
    }

    // Blocca i comandi finché non approvato
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(adminName)) return;
        if (approvedPlayers.contains(player.getName().toLowerCase())) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Non puoi usare comandi finché non sei approvato.");
    }

    // Rimuove l'approvazione quando il giocatore esce (così al reconnect sarà bloccato di nuovo)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        approvedPlayers.remove(event.getPlayer().getName().toLowerCase());
    }

    private void blockPlayer(Player player) {
        // Metti in adventure per proteggere il mondo, rendilo invulnerabile e fermo
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.setVelocity(new Vector(0, 0, 0));
        player.sendMessage(ChatColor.RED + "Sei bloccato! Attendi l'approvazione di un admin.");
    }

    private void unblockPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        player.sendMessage(ChatColor.GREEN + "Sei stato approvato! Buon gioco.");
    }
}
