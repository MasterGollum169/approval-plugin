package com.davis.approvalplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
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

    // In-memory sets: per-session approval/explicit block
    private final Set<String> approvedPlayers = new HashSet<>(); // lowercase names
    private final Set<String> blockedPlayers = new HashSet<>();  // explicit re-blocks by admin
    private final String adminName = "Davis1070"; // your username
    // permission nodes
    private final String PERM_UNBLOCK = "approvalplugin.unblock";
    private final String PERM_BLOCK = "approvalplugin.block";
    private final String PERM_LIST = "approvalplugin.list";

    @Override
    public void onEnable() {
        // register events
        getServer().getPluginManager().registerEvents(this, this);

        // /unblock - unified: unblock a player (acts as approve)
        if (getCommand("unblock") != null) {
            getCommand("unblock").setExecutor((sender, command, label, args) -> {
                if (!isAllowedToManage(sender, PERM_UNBLOCK)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Usage: /unblock <player>");
                    return true;
                }
                String target = args[0];
                String lower = target.toLowerCase();

                // Mark approved for this session and remove explicit block
                approvedPlayers.add(lower);
                blockedPlayers.remove(lower);

                Player p = Bukkit.getPlayerExact(target);
                if (p != null) {
                    unblockPlayer(p);
                }
                sender.sendMessage(ChatColor.GREEN + "Player " + target + " has been unblocked.");
                return true;
            });
        }

        // /block - explicitly block a player (admin)
        if (getCommand("block") != null) {
            getCommand("block").setExecutor((sender, command, label, args) -> {
                if (!isAllowedToManage(sender, PERM_BLOCK)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                if (args.length != 1) {
                    sender.sendMessage(ChatColor.RED + "Usage: /block <player>");
                    return true;
                }
                String target = args[0];
                String lower = target.toLowerCase();

                blockedPlayers.add(lower);
                approvedPlayers.remove(lower);

                Player p = Bukkit.getPlayerExact(target);
                if (p != null) {
                    blockPlayer(p);
                }
                sender.sendMessage(ChatColor.YELLOW + "Player " + target + " has been explicitly blocked.");
                return true;
            });
        }

        // /approvallist - show simple status for online players
        if (getCommand("approvallist") != null) {
            getCommand("approvallist").setExecutor((sender, command, label, args) -> {
                if (!isAllowedToManage(sender, PERM_LIST)) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "---- Approval Status (online) ----");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String name = p.getName();
                    String status;
                    if (name.equalsIgnoreCase(adminName)) {
                        status = ChatColor.GREEN + "" + ChatColor.BOLD + "ADMIN";
                    } else if (blockedPlayers.contains(name.toLowerCase())) {
                        status = ChatColor.RED + "" + ChatColor.BOLD + "BLOCKED";
                    } else if (approvedPlayers.contains(name.toLowerCase())) {
                        status = ChatColor.GREEN + "APPROVED";
                    } else {
                        status = ChatColor.YELLOW + "PENDING";
                    }
                    sender.sendMessage(
                        ChatColor.DARK_GRAY + "- " + ChatColor.WHITE + name + ChatColor.DARK_GRAY + " : " + status
                    );
                }
                sender.sendMessage(ChatColor.GOLD + "------------------------------------");
                return true;
            });
        }

        getLogger().info("ApprovalPlugin enabled. Admin: " + adminName);
    }

    @Override
    public void onDisable() {
        approvedPlayers.clear();
        blockedPlayers.clear();
        getLogger().info("ApprovalPlugin disabled.");
    }

    /**
     * Utility: check if sender (console or player) is allowed to manage actions.
     * Console allowed. AdminName / OP or permission node allowed.
     */
    private boolean isAllowedToManage(org.bukkit.command.CommandSender sender, String requiredPerm) {
        if (!(sender instanceof Player)) {
            return true; // console allowed
        }
        Player p = (Player) sender;
        if (p.getName().equalsIgnoreCase(adminName)) return true;
        if (p.isOp()) return true;
        return p.hasPermission(requiredPerm);
    }

    /**
     * On join: default behaviour is to block everyone except admin or explicitly approved.
     * blockedPlayers has precedence (explicit re-block).
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String lower = player.getName().toLowerCase();

        if (player.getName().equalsIgnoreCase(adminName)) {
            // admin always free
            return;
        }

        // If explicitly blocked -> block
        if (blockedPlayers.contains(lower)) {
            blockPlayer(player);
            notifyAdminJoin(player);
            return;
        }

        // If explicitly approved -> unblock
        if (approvedPlayers.contains(lower)) {
            unblockPlayer(player);
            return;
        }

        // Default: block (pending approval)
        blockPlayer(player);
        notifyAdminJoin(player);
    }

    private void notifyAdminJoin(Player joined) {
        // console-friendly message (no color codes)
        String consoleMsg = "[Approval] Player " + joined.getName() + " joined and is pending approval.";
        Bukkit.getLogger().info(consoleMsg);

        // colored message for admin in-game
        String chatMsg = ChatColor.GOLD + "" + ChatColor.BOLD + "[Approval] " 
                + ChatColor.RESET + ChatColor.YELLOW + "Player " 
                + ChatColor.AQUA + joined.getName() 
                + ChatColor.YELLOW + " is pending approval. Use " 
                + ChatColor.GREEN + "/unblock " + joined.getName() 
                + ChatColor.YELLOW + " to allow.";

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(adminName)) {
                p.sendMessage(chatMsg);
            }
        }
    }

    // Prevent movement / rotation for blocked players
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String lower = player.getName().toLowerCase();
        if (player.getName().equalsIgnoreCase(adminName)) return;
        if (approvedPlayers.contains(lower) && !blockedPlayers.contains(lower)) return;
        // not approved OR explicitly blocked => stop movement
        event.setTo(event.getFrom());
        player.setVelocity(new Vector(0, 0, 0));
    }

    // Prevent commands for non-approved players
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String lower = player.getName().toLowerCase();
        if (player.getName().equalsIgnoreCase(adminName)) return;
        if (approvedPlayers.contains(lower) && !blockedPlayers.contains(lower)) return;
        // blocked/pending: cancel commands
        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You cannot use commands until an admin approves you.");
    }

    // On quit: remove approvals so next reconnect always requires approval again (per-session)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String name = event.getPlayer().getName().toLowerCase();
        approvedPlayers.remove(name);
        // do NOT remove blockedPlayers - explicit blocks persist during server uptime
    }

    // helper: block player in-game (immobilize, set to adventure, invulnerable)
    private void blockPlayer(Player player) {
        try {
            player.setGameMode(GameMode.ADVENTURE);
            player.setInvulnerable(true);
            player.setVelocity(new Vector(0, 0, 0));

            player.sendMessage(
                ChatColor.RED + "" + ChatColor.BOLD + "You are currently BLOCKED\n" +
                ChatColor.RESET + ChatColor.GRAY + "An admin must approve you before you can move or use commands."
            );
            player.sendMessage(ChatColor.DARK_GRAY + "If you believe this is a mistake, ask an admin in chat.");
        } catch (Exception e) {
            getLogger().warning("Failed to block player " + player.getName() + ": " + e.getMessage());
        }
    }

    // helper: unblock player in-game (restore survival and vulnerability)
    private void unblockPlayer(Player player) {
        try {
            player.setGameMode(GameMode.SURVIVAL);
            player.setInvulnerable(false);
            player.sendMessage(
                ChatColor.GREEN + "" + ChatColor.BOLD + "You have been UNBLOCKED\n" +
                ChatColor.RESET + ChatColor.GRAY + "Enjoy the game!"
            );
        } catch (Exception e) {
            getLogger().warning("Failed to unblock player " + player.getName() + ": " + e.getMessage());
        }
    }
}
