package me.hqm.timezones;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class Timezone {
    private final transient Plugin plugin;

    private String name;
    private long time;
    private boolean rain;

    public Timezone(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(name);
        this.time = section.getLong("time", 0);
        this.rain = section.getBoolean("rain", false);
    }

    public Timezone(Plugin plugin, String name, long time, boolean rain) {
        this.plugin = plugin;
        this.name = name;
        this.time = time;
        this.rain = rain;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRain(boolean rain) {
        this.rain = rain;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public boolean getRain() {
        return rain;
    }

    public long getTime() {
        return time;
    }

    public void saveToConfig() {
        Configuration config = plugin.getConfig();
        config.set(name + ".time", time);
        config.set(name + ".rain", rain);
        plugin.saveConfig();
    }
}
