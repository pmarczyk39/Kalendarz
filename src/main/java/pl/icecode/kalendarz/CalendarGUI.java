package pl.icecode.kalendarz;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CalendarGUI implements Listener {

    private final CalendarPlugin plugin;
    private final Storage storage;
    private final String title;
    private final boolean onlyToday;
    private final boolean allowPreviousDays;
    private final String serverName;
    private final String msgPrefix;
    private final String msgClaimSuccess;
    private final String msgAlreadyClaimed;
    private final String msgNotAvailable;
    private final String msgNoReward;
    private final Material matAvailable;
    private final Material matLocked;
    private final Material matClaimed;
    private final boolean glowToday;
    private final int guiSize;
    private final boolean borderEnabled;
    private final Material borderMat;
    private final Material fillerMat;
    private final ItemStack infoItemTemplate;
    private final Integer closeSlot; // nullable, computed for 54-size
    private final ItemStack closeItem;
    private final Sound openSound;
    private final float openSoundVol;
    private final float openSoundPitch;

    public CalendarGUI(CalendarPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.serverName = Objects.toString(plugin.getConfig().getString("server-name"), "Serwer");
        this.msgPrefix = colorize(Objects.toString(plugin.getConfig().getString("messages.prefix"), "&a" + this.serverName + " &7>>"));
        this.msgClaimSuccess = Objects.toString(plugin.getConfig().getString("messages.claim-success"), "&7Gratulacje &e%player&7, odebrales nagrode za dzien &e%day&7!");
        this.msgAlreadyClaimed = Objects.toString(plugin.getConfig().getString("messages.already-claimed"), "&cJuz odebrales dzien &e%day&c.");
        this.msgNotAvailable = Objects.toString(plugin.getConfig().getString("messages.not-available"), "&7Ten dzien nie jest dostepny teraz.");
        this.msgNoReward = Objects.toString(plugin.getConfig().getString("messages.no-reward"), "&cBrak nagrody skonfigurowanej dla dnia &e%day&c. Powiadom administracje.");

        String rawTitle = Objects.toString(plugin.getConfig().getString("gui.title"), "&aKalendarz adwentowy - $name");
        rawTitle = rawTitle.replace("$name", serverName);
        this.title = ChatColor.translateAlternateColorCodes('&', rawTitle);

        this.onlyToday = plugin.getConfig().getBoolean("only-today", true);
        this.allowPreviousDays = plugin.getConfig().getBoolean("allow-previous-days", false);
        this.matAvailable = parseMaterial(plugin.getConfig().getString("gui.material.available"), Material.LIME_STAINED_GLASS_PANE);
        this.matLocked = parseMaterial(plugin.getConfig().getString("gui.material.locked"), Material.GRAY_STAINED_GLASS_PANE);
        this.matClaimed = parseMaterial(plugin.getConfig().getString("gui.material.claimed"), Material.RED_STAINED_GLASS_PANE);

        this.glowToday = plugin.getConfig().getBoolean("gui.glow-today", true);
        this.guiSize = validateSize(plugin.getConfig().getInt("gui.size", 54));
        this.borderEnabled = plugin.getConfig().getBoolean("gui.border.enabled", true);
        this.borderMat = parseMaterial(plugin.getConfig().getString("gui.border.material"), Material.RED_STAINED_GLASS_PANE);
        this.fillerMat = parseMaterial(plugin.getConfig().getString("gui.filler.material"), Material.BLACK_STAINED_GLASS_PANE);

        Material infoMat = parseMaterial(plugin.getConfig().getString("gui.items.info.material"), Material.PAPER);
        String infoName = colorize(Objects.toString(plugin.getConfig().getString("gui.items.info.name"), "§eKalendarz §f{month} {year}"));
        List<String> infoLore = colorizeList(plugin.getConfig().getStringList("gui.items.info.lore"));
        this.infoItemTemplate = simpleItem(infoMat, infoName, infoLore, false);

        Material closeMat = parseMaterial(plugin.getConfig().getString("gui.items.close.material"), Material.BARRIER);
        String closeName = colorize(Objects.toString(plugin.getConfig().getString("gui.items.close.name"), "§cZamknij"));
        this.closeItem = simpleItem(closeMat, closeName, null, false);
        this.closeSlot = (guiSize == 54) ? 49 : null;

        this.openSound = parseSound(plugin.getConfig().getString("gui.sounds.open.name"), Sound.BLOCK_CHEST_OPEN);
        this.openSoundVol = (float) getDouble(plugin.getConfig().getDouble("gui.sounds.open.volume", 1.0));
        this.openSoundPitch = (float) getDouble(plugin.getConfig().getDouble("gui.sounds.open.pitch", 1.0));
    }

    private Material parseMaterial(String name, Material def) {
        if (name == null) return def;
        try { return Material.valueOf(name); } catch (IllegalArgumentException ex) { return def; }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(player, guiSize, title);
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        int today = now.getDayOfMonth();
        int month = now.getMonthValue();
        int year = now.getYear();

        if (guiSize == 54 && borderEnabled) {
            fillBorder(inv, borderMat);
        }

        List<Integer> daySlots = computeDaySlots(guiSize);

        int claimedCount = 0;
        for (int day = 1; day <= 24; day++) {
            ItemStack item;
            List<String> lore = new ArrayList<>();
            String display = ChatColor.GOLD + "Dzien " + day;
            boolean claimed = storage.hasClaimed(player, year, day);
            if (claimed) {
                item = new ItemStack(matClaimed);
                lore.add(ChatColor.RED + "Odebrano");
                claimedCount++;
            } else if (isDayClaimable(month, today, day)) {
                item = new ItemStack(matAvailable);
                if (allowPreviousDays) {
                    lore.add(ChatColor.GREEN + "Dostepne");
                } else {
                    lore.add(ChatColor.GREEN + "Dostepne dzisiaj!");
                }
            } else {
                item = new ItemStack(matLocked);
                if (month != 12) {
                    lore.add(ChatColor.GRAY + "Dostepne w grudniu");
                } else {
                    lore.add(ChatColor.GRAY + "Niedostepne dzisiaj");
                }
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(display);
                meta.setLore(lore);
                if (glowToday && (month == 12 && day == today) && !meta.hasEnchants()) {
                    meta.addEnchant(Enchantment.LUCK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);
            }
            inv.setItem(daySlots.get(day - 1), item);
        }

        if (guiSize == 54) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                if (inv.getItem(slot) == null || inv.getItem(slot).getType() == Material.AIR) {
                    inv.setItem(slot, new ItemStack(fillerMat));
                }
            }
            ItemStack info = infoItem(year, now.getMonthValue(), claimedCount, 24);
            inv.setItem(4, info);
            if (closeSlot != null) {
                inv.setItem(closeSlot, closeItem.clone());
            }
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), openSound, openSoundVol, openSoundPitch);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        if (!title.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (closeSlot != null && slot == closeSlot) {
            player.closeInventory();
            return;
        }

        List<Integer> daySlots = computeDaySlots(event.getInventory().getSize());
        int index = daySlots.indexOf(slot);
        if (index == -1) return;
        int day = index + 1;

        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        int today = now.getDayOfMonth();
        int month = now.getMonthValue();
        int year = now.getYear();

        if (storage.hasClaimed(player, year, day)) {
            msg(player, msgAlreadyClaimed, day);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        if (!isDayClaimable(month, today, day)) {
            msg(player, msgNotAvailable, day);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        List<String> cmds = plugin.getConfig().getStringList("rewards." + day);
        if (cmds == null || cmds.isEmpty()) {
            msg(player, msgNoReward, day);
        } else {
            for (String cmd : cmds) {
                String parsed = cmd.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }

        storage.markClaimed(player, year, day);
        msg(player, msgClaimSuccess, day);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        ItemStack item = new ItemStack(matClaimed);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Dzien " + day);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.RED + "Odebrano");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        event.getInventory().setItem(slot, item);
    }

    private boolean isDayClaimable(int month, int today, int day) {
        if (month != 12) return false;
        if (allowPreviousDays) {
            return day <= today;
        }
        if (onlyToday) return day == today;
        return day == today;
    }

    private int validateSize(int size) {
        return (size == 27 || size == 54) ? size : 54;
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', Objects.toString(s, ""));
    }

    private void msg(Player p, String template, int day) {
        String txt = Objects.toString(template, "");
        txt = txt.replace("$name", serverName)
                .replace("%player", p.getName())
                .replace("%day", String.valueOf(day));
        String full = msgPrefix + " " + colorize(txt);
        p.sendMessage(full);
    }

    private List<String> colorizeList(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list != null) {
            for (String l : list) {
                out.add(colorize(l));
            }
        }
        return out;
    }

    private ItemStack simpleItem(Material mat, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            if (name != null) m.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) m.setLore(lore);
            if (glow) {
                m.addEnchant(Enchantment.LUCK, 1, true);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            it.setItemMeta(m);
        }
        return it;
    }

    private void fillBorder(Inventory inv, Material mat) {
        int size = inv.getSize();
        int width = 9;
        int rows = size / width;
        ItemStack pane = new ItemStack(mat);
        for (int x = 0; x < width; x++) {
            inv.setItem(x, pane);
            inv.setItem((rows - 1) * width + x, pane);
        }
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * width, pane);
            inv.setItem(r * width + (width - 1), pane);
        }
    }

    private List<Integer> computeDaySlots(int size) {
        List<Integer> slots = new ArrayList<>(24);
        if (size == 27) {
            for (int i = 0; i < 24; i++) slots.add(i);
            return slots;
        }
        int width = 9;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                int slot = row * width + col;
                slots.add(slot);
                if (slots.size() == 24) return slots;
            }
        }
        return slots;
    }

    private Sound parseSound(String name, Sound def) {
        if (name == null) return def;
        try { return Sound.valueOf(name); } catch (IllegalArgumentException ex) { return def; }
    }

    private double getDouble(double v) { return v; }

    private ItemStack infoItem(int year, int month, int claimed, int total) {
        ItemStack it = infoItemTemplate.clone();
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            String name = m.getDisplayName();
            if (name != null) {
                name = name.replace("{year}", String.valueOf(year))
                        .replace("{month}", String.valueOf(month))
                        .replace("{progress}", claimed + "/" + total);
                m.setDisplayName(name);
            }
            List<String> lore = m.getLore();
            if (lore != null && !lore.isEmpty()) {
                List<String> nl = new ArrayList<>();
                for (String l : lore) {
                    nl.add(l.replace("{year}", String.valueOf(year))
                            .replace("{month}", String.valueOf(month))
                            .replace("{progress}", claimed + "/" + total));
                }
                m.setLore(nl);
            }
            it.setItemMeta(m);
        }
        return it;
    }
}
