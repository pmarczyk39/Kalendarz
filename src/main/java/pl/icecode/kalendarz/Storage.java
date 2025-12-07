package pl.icecode.kalendarz;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Storage {

    private final CalendarPlugin plugin;
    private final File dataFolder;

    public Storage(CalendarPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private FileConfiguration getConfig(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Nie mozna utworzyc pliku danych dla gracza: " + uuid);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveConfig(UUID uuid, FileConfiguration cfg) {
        try {
            cfg.save(getPlayerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().warning("Nie mozna zapisac pliku danych dla gracza: " + uuid);
        }
    }

    public boolean hasClaimed(OfflinePlayer player, int year, int day) {
        UUID uuid = player.getUniqueId();
        FileConfiguration cfg = getConfig(uuid);
        String path = year + ".claimed";
        Set<Integer> set = new HashSet<>(cfg.getIntegerList(path));
        return set.contains(day);
    }

    public void markClaimed(OfflinePlayer player, int year, int day) {
        UUID uuid = player.getUniqueId();
        FileConfiguration cfg = getConfig(uuid);
        String path = year + ".claimed";
        Set<Integer> set = new HashSet<>(cfg.getIntegerList(path));
        if (!set.contains(day)) {
            set.add(day);
            cfg.set(path, set.stream().sorted().toArray(Integer[]::new));
            saveConfig(uuid, cfg);
        }
    }
}
