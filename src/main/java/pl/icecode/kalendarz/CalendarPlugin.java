package pl.icecode.kalendarz;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.ZoneId;

public class CalendarPlugin extends JavaPlugin {

    private Storage storage;
    private CalendarGUI calendarGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.storage = new Storage(this);
        this.calendarGUI = new CalendarGUI(this, storage);

        Bukkit.getPluginManager().registerEvents(calendarGUI, this);
        getLogger().info("Kalendarz Adventowy wlaczony.");
        getLogger().info("Created: cyan_tm for: IceCode.pl");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        getLogger().info("Kalendarz Adventowy wylaczony.");
        getLogger().info("Created: cyan_tm for: IceCode.pl");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kalendarz")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Ta komenda jest tylko dla graczy.");
                return true;
            }
            Player player = (Player) sender;
            // Blokuj otwarcie poza grudniem
            LocalDate now = LocalDate.now(ZoneId.systemDefault());
            if (now.getMonthValue() != 12) {
                String serverName = getConfig().getString("server-name", "Serwer");
                String prefix = colorize(getConfig().getString("messages.prefix", "&a" + serverName + " &7>>"));
                String msg = getConfig().getString("messages.only-december", "&7Kalendarz jest dostepny tylko w grudniu.");
                msg = msg.replace("$name", serverName).replace("%player", player.getName());
                player.sendMessage(prefix + " " + colorize(msg));
                return true;
            }
            calendarGUI.open(player);
            return true;
        }
        return false;
    }

    private String colorize(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
