package skylands.online.playerrollback;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SnapshotGuiManager {
    private final PlayerRollbackPlugin plugin;
    
    private final int[] JOIN_SLOTS = {0, 1, 9, 10, 18, 19, 27, 28, 36, 37};
    private final int[] DEATH_SLOTS = {7, 8, 16, 17, 25, 26, 34, 35, 43, 44};

    public SnapshotGuiManager(PlayerRollbackPlugin plugin) {
        this.plugin = plugin;
    }

    public void openSnapshotList(Player viewer, UUID targetUuid, String targetName) {
        List<File> joinFiles = plugin.getSnapshotManager().getSnapshots(targetUuid, "JOIN");
        List<File> deathFiles = plugin.getSnapshotManager().getSnapshots(targetUuid, "DEATH");

        SnapshotListHolder holder = new SnapshotListHolder(targetUuid, targetName, joinFiles, deathFiles);
        Inventory inv = Bukkit.createInventory(holder, 54, net.kyori.adventure.text.Component.text("Снимки: " + targetName));
        holder.setInventory(inv);

        // Fill background with gray glass panes
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Place Player Head in slot 4 (Row 1 center)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid));
            skullMeta.setDisplayName("§eСнимки: §f" + targetName);
            List<String> lore = new ArrayList<>();
            lore.add("§7Снимков JOIN: §a" + joinFiles.size() + " / 10");
            lore.add("§7Снимков DEATH: §c" + deathFiles.size() + " / 10");
            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
        }
        inv.setItem(4, head);

        // Place JOIN snapshots
        for (int i = 0; i < Math.min(JOIN_SLOTS.length, joinFiles.size()); i++) {
            File file = joinFiles.get(i);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String time = yaml.getString("timestamp", "Неизвестно");
            
            ItemStack item = new ItemStack(Material.LIME_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§aВход на сервер (JOIN) #" + (i + 1));
                List<String> lore = new ArrayList<>();
                lore.add("§7Время: §f" + time);
                lore.add("§7XP уровень: §f" + yaml.getInt("xp.level", 0));
                lore.add("§f");
                lore.add("§eЛКМ: §bВосстановить (откатить)");
                lore.add("§eПКМ: §bПросмотреть инвентарь");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(JOIN_SLOTS[i], item);
        }

        // Place DEATH snapshots
        for (int i = 0; i < Math.min(DEATH_SLOTS.length, deathFiles.size()); i++) {
            File file = deathFiles.get(i);
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String time = yaml.getString("timestamp", "Неизвестно");
            
            ItemStack item = new ItemStack(Material.RED_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§cСмерть игрока (DEATH) #" + (i + 1));
                List<String> lore = new ArrayList<>();
                lore.add("§7Время: §f" + time);
                lore.add("§7XP уровень: §f" + yaml.getInt("xp.level", 0));
                lore.add("§f");
                lore.add("§eЛКМ: §bВосстановить (откатить)");
                lore.add("§eПКМ: §bПросмотреть инвентарь");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(DEATH_SLOTS[i], item);
        }

        // Close button in slot 49
        inv.setItem(49, createGuiItem(Material.BARRIER, "§cЗакрыть меню"));

        viewer.openInventory(inv);
    }

    public void openSnapshotPreview(Player viewer, UUID targetUuid, String targetName, File snapshotFile, List<File> joinSnapshots, List<File> deathSnapshots) {
        SnapshotPreviewHolder holder = new SnapshotPreviewHolder(targetUuid, targetName, snapshotFile, joinSnapshots, deathSnapshots);
        Inventory inv = Bukkit.createInventory(holder, 54, net.kyori.adventure.text.Component.text("Просмотр: " + targetName));
        holder.setInventory(inv);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(snapshotFile);

        // Load inventory contents
        ItemStack[] storage = loadItemArray(yaml.getConfigurationSection("inventory.storage"), 36);
        ItemStack[] armor = loadItemArray(yaml.getConfigurationSection("inventory.armor"), 4);
        ItemStack[] extra = loadItemArray(yaml.getConfigurationSection("inventory.extra"), 1);

        // Slots 0-35: Storage inventory
        for (int i = 0; i < 36; i++) {
            if (storage[i] != null) {
                inv.setItem(i, storage[i]);
            }
        }

        // Fill row 5 (slots 36-44) with armor, offhand and spacers
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "§7");
        inv.setItem(36, filler);
        
        // Armor slots (Helmet, Chestplate, Leggings, Boots)
        // Saved order: index 0: Boots, 1: Leggings, 2: Chestplate, 3: Helmet
        if (armor[3] != null) inv.setItem(37, armor[3]); // Helmet
        else inv.setItem(37, createGuiItem(Material.BARRIER, "§7Пустой слот шлема"));

        if (armor[2] != null) inv.setItem(38, armor[2]); // Chestplate
        else inv.setItem(38, createGuiItem(Material.BARRIER, "§7Пустой слот нагрудника"));

        if (armor[1] != null) inv.setItem(39, armor[1]); // Leggings
        else inv.setItem(39, createGuiItem(Material.BARRIER, "§7Пустой слот поножей"));

        if (armor[0] != null) inv.setItem(40, armor[0]); // Boots
        else inv.setItem(40, createGuiItem(Material.BARRIER, "§7Пустой слот ботинок"));

        // Offhand slot
        if (extra[0] != null) inv.setItem(41, extra[0]); // Offhand
        else inv.setItem(41, createGuiItem(Material.BARRIER, "§7Пустой слот щита (оффхэнд)"));

        inv.setItem(42, filler);
        inv.setItem(43, filler);
        inv.setItem(44, filler);

        // Row 6 (slots 45-53) background
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Action buttons
        inv.setItem(48, createGuiItem(Material.RED_WOOL, "§c« Назад к списку"));
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a✔ Восстановить этот снимок"));

        viewer.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack[] loadItemArray(ConfigurationSection section, int size) {
        ItemStack[] items = new ItemStack[size];
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int index = Integer.parseInt(key);
                    if (index >= 0 && index < size) {
                        items[index] = section.getItemStack(key);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return items;
    }
}
