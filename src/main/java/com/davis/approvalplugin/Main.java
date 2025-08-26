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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    private final Set<String> approvedPlayers = new HashSet<>();
    private final String adminName = "Davis1070"; // tuo username

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

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

        getLogger().info("ApprovalPlugin attivato!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getName().equalsIgnoreCase(adminName)) {
            return; // tu sei libero
        }

        if (approvedPlayers.contains(player.getName().toLowerCase())) {
            unblockPlayer(player);
            return;
        }

        blockPlayer(player);

        String msg = ChatColor.YELLOW + "Giocatore " + player.getName() + " vuole entrare! Usa /approve " + player.getName() + " per sbloccarlo.";
        Bukkit.getLogger().info(msg);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(adminName)) {
                p.sendMessage(msg);
            }
        }
    }

    // Blocca movimento, salto e comandi
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(adminName)) return;
        if (approvedPlayers.contains(player.getName().toLowerCase())) return;

        // Resetta posizione e blocca movimento
        event.setTo(event.getFrom());
        player.setVelocity(new Vector(0,0,0));
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(adminName)) return;
        if (approvedPlayers.contains(player.getName().toLowerCase())) return;

        // Blocca comandi finché non approvato
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "Non puoi usare comandi finché non sei approvato.");
    }

    private void blockPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.sendMessage(ChatColor.RED + "Sei bloccato! Attendi l'approvazione di un admin.");
    }

    private void unblockPlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        player.sendMessage(ChatColor.GREEN + "Sei stato approvato! Buon gioco.");
    }
}
