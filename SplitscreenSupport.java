package de.splitscreen;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SplitscreenSupport extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<String, List<UUID>> consoleConnections = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info(ChatColor.GREEN + "[SplitscreenSupport] Plugin aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info(ChatColor.YELLOW + "[SplitscreenSupport] Plugin deaktiviert!");
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.getBoolean("splitscreen.enabled")) return;

        String ip = event.getAddress().getHostAddress();
        UUID uuid = event.getUniqueId();

        // Liste der Spieler von derselben Konsole
        consoleConnections.putIfAbsent(ip, new ArrayList<>());
        List<UUID> connected = consoleConnections.get(ip);

        int maxPlayers = config.getInt("splitscreen.max_per_console", 4);

        if (connected.size() >= maxPlayers) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.RED + "Maximale Splitscreen-Spieleranzahl (" + maxPlayers + ") erreicht.");
            return;
        }

        // Prüfen, ob UUID schon da (z.B. zweiter Spieler mit gleicher Konsole)
        if (connected.contains(uuid)) {
            if (config.getBoolean("splitscreen.allow_custom_names", true)) {
                // Neue UUID für Spieler erzeugen, um Kick zu vermeiden
                UUID fakeUuid = UUID.randomUUID();
                event.setUniqueId(fakeUuid);
                if (config.getBoolean("splitscreen.log_debug", false)) {
                    getLogger().log(Level.INFO, "Generierte Fake-UUID für Splitscreen-Spieler: " + event.getName());
                }
            }
        }

        connected.add(event.getUniqueId());
    }
}
