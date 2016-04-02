package me.hqm.timezones;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.stream.Collectors;

public class TimezonesPlugin extends JavaPlugin implements Listener {
    private Map<String, Timezone> ZONES;

    @Override
    public void onEnable() {
        // Setup the config
        saveDefaultConfig();

        // Create the zones map
        Set<String> keys = getConfig().getValues(false).keySet();
        ZONES = new HashMap<>(keys.size());

        // Load the zones
        for(String key : keys) {
            Timezone zone = new Timezone(this, key);
            ZONES.put(key, zone);
        }

        // Register the listener
        getServer().getPluginManager().registerEvents(this, this);

        // Start the repeating task
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for(Player online : getServer().getOnlinePlayers()) {
                String region = findRegion(ZONES.keySet(), online.getLocation());
                if (region != null) {
                    Timezone zone = ZONES.get(region);
                    online.setPlayerTime(zone.getTime(), false);
                    online.setPlayerWeather(zone.getRain() ? WeatherType.DOWNFALL : WeatherType.CLEAR);
                }
            }
        }, 5, 5);

        // Alert the console
        getLogger().info("Up and ready!");
    }

    @Override
    public void onDisable() {
        // Clear the handler list
        HandlerList.unregisterAll((Plugin) this);

        // Cancel all tasks
        getServer().getScheduler().cancelTasks(this);

        // Alert the console
        getLogger().info("Shutting down...");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        // Get the current region
        String region = findRegion(ZONES.keySet(), event.getTo(), event.getFrom());

        // If a region exists grab it
        if(region != null) {
            // Get the status of the to and from
            boolean inTo = inRegion(region, event.getTo());
            boolean inFrom = inRegion(region, event.getFrom());
            Player player = event.getPlayer();

            // Only do stuff if they don't match
            if (inTo && !inFrom) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 255, false, false));
            } else if (!inTo && inFrom) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 255, false, false));
                player.resetPlayerTime();
                player.resetPlayerWeather();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if("timezone".equals(command.getName())) {
            if(sender.hasPermission("timezone.mod")) {
                if(args.length == 2 && "remove".equalsIgnoreCase(args[0])) {
                    if(ZONES.containsKey(args[1])) {
                        ZONES.remove(args[1]);
                        getConfig().set(args[1], null);
                        sender.sendMessage(ChatColor.YELLOW + "Timezone " + ChatColor.RED + args[1] + ChatColor.YELLOW +
                                " has been removed.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "That is not a valid timezone.");
                    }
                    return true;
                } else if(args.length > 2) {
                    try {
                        String name = args[1];
                        long time = Long.valueOf(args[2]);
                        boolean rain = args.length > 3 ? Boolean.valueOf(args[3].toLowerCase()) : false;
                        Timezone zone = new Timezone(this, name, time, rain);
                        zone.saveToConfig();
                        ZONES.put(name, zone);
                    } catch (Exception oops) {
                        sender.sendMessage(ChatColor.RED + "Invalid syntax.");
                        return false;
                    }
                    sender.sendMessage(ChatColor.YELLOW + "Timezone " + ChatColor.GREEN + args[1] +
                            ChatColor.YELLOW + " has been created.");
                    return true;
                } else {
                    sender.sendMessage("Not enough arguments.");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command.");
                return true;
            }
        }
        return false;
    }

    private String findRegion(Collection<String> names, Location... locations) {
        // If there are no locations to check return null
        if(locations.length < 1) {
            return null;
        }

        // Get the region manager
        RegionManager manager = WorldGuardPlugin.inst().getRegionManager(locations[0].getWorld());

        // Setup a list of all combined region names from each location
        List<String> regions = new ArrayList<>();
        for(Location location : locations) {
            regions.addAll(manager.getApplicableRegions(location).getRegions().stream().map(ProtectedRegion::getId).
                    collect(Collectors.toList()));
        }

        // Filter out any regions we aren't interested in
        regions.retainAll(names);

        // Return a relevant region, or null
        return regions.isEmpty() ? null : regions.get(0);
    }

    private boolean inRegion(String name, Location location) {
        // Get the regions for a location
        List<String> regions = WorldGuardPlugin.inst().getRegionManager(location.getWorld()).
                getApplicableRegions(location).getRegions().stream().map(ProtectedRegion::getId).
                collect(Collectors.toList());

        // Return if the name is included in the regions
        return regions.contains(name);
    }
}
