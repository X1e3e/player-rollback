package skylands.online.playerrollback;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SnapshotManager {
    private final PlayerRollbackPlugin plugin;
    private final File snapshotsFolder;
    private static final int MAX_SNAPSHOTS = 10;
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SnapshotManager(PlayerRollbackPlugin plugin) {
        this.plugin = plugin;
        this.snapshotsFolder = new File(plugin.getDataFolder(), "snapshots");
        if (!snapshotsFolder.exists()) {
            snapshotsFolder.mkdirs();
        }
    }

    public void createSnapshot(Player player, String type) {
        UUID uuid = player.getUniqueId();
        File playerDir = new File(snapshotsFolder, uuid.toString());
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }

        LocalDateTime now = LocalDateTime.now();
        String filename = now.format(FILE_DATE_FORMAT) + "_" + type + ".yml";
        File file = new File(playerDir, filename);

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("type", type);
        yaml.set("timestamp", now.format(DISPLAY_DATE_FORMAT));

        // Save location
        Location loc = player.getLocation();
        yaml.set("location.world", loc.getWorld().getName());
        yaml.set("location.x", loc.getX());
        yaml.set("location.y", loc.getY());
        yaml.set("location.z", loc.getZ());
        yaml.set("location.yaw", loc.getYaw());
        yaml.set("location.pitch", loc.getPitch());

        // Save inventory
        saveItemArray(yaml.createSection("inventory.storage"), player.getInventory().getStorageContents());
        saveItemArray(yaml.createSection("inventory.armor"), player.getInventory().getArmorContents());
        saveItemArray(yaml.createSection("inventory.extra"), player.getInventory().getExtraContents());

        // Save attributes
        yaml.set("xp.level", player.getLevel());
        yaml.set("xp.exp", player.getExp());
        yaml.set("food", player.getFoodLevel());
        yaml.set("gamemode", player.getGameMode().name());
        yaml.set("allow-flight", player.getAllowFlight());
        yaml.set("flying", player.isFlying());

        // Save potion effects
        List<Map<String, Object>> effectsList = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            Map<String, Object> effectMap = new HashMap<>();
            effectMap.put("type", effect.getType().getName());
            effectMap.put("duration", effect.getDuration());
            effectMap.put("amplifier", effect.getAmplifier());
            effectMap.put("ambient", effect.isAmbient());
            effectMap.put("particles", effect.hasParticles());
            effectMap.put("icon", effect.hasIcon());
            effectsList.add(effectMap);
        }
        yaml.set("potion-effects", effectsList);

        try {
            yaml.save(file);
            cleanOldSnapshots(playerDir, type);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить снимок для игрока " + player.getName() + ": " + e.getMessage());
        }
    }

    private void cleanOldSnapshots(File playerDir, String type) {
        File[] files = playerDir.listFiles((dir, name) -> name.endsWith("_" + type + ".yml"));
        if (files == null || files.length <= MAX_SNAPSHOTS) {
            return;
        }

        // Sort files by name (which sorts them chronologically because of the yyyyMMdd_HHmmss prefix)
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.sort(Comparator.comparing(File::getName));

        // Delete the oldest files until we are within the limit
        int filesToDelete = fileList.size() - MAX_SNAPSHOTS;
        for (int i = 0; i < filesToDelete; i++) {
            File oldestFile = fileList.get(i);
            if (oldestFile.delete()) {
                plugin.getLogger().fine("Удален старый снимок: " + oldestFile.getName());
            }
        }
    }

    public List<File> getSnapshots(UUID uuid, String type) {
        File playerDir = new File(snapshotsFolder, uuid.toString());
        if (!playerDir.exists()) {
            return Collections.emptyList();
        }

        File[] files = playerDir.listFiles((dir, name) -> name.endsWith("_" + type + ".yml"));
        if (files == null) {
            return Collections.emptyList();
        }

        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        // Sort chronologically descending (latest first)
        fileList.sort((f1, f2) -> f2.getName().compareTo(f1.getName()));
        return fileList;
    }

    public boolean restoreSnapshot(Player player, File file) {
        if (!file.exists()) {
            return false;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        // Teleport to saved location
        String worldName = yaml.getString("location.world");
        if (worldName != null) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = yaml.getDouble("location.x");
                double y = yaml.getDouble("location.y");
                double z = yaml.getDouble("location.z");
                float yaw = (float) yaml.getDouble("location.yaw");
                float pitch = (float) yaml.getDouble("location.pitch");
                player.teleport(new Location(world, x, y, z, yaw, pitch));
            }
        }

        // Restore attributes
        player.setLevel(yaml.getInt("xp.level", 0));
        player.setExp((float) yaml.getDouble("xp.exp", 0.0));
        player.setFoodLevel(yaml.getInt("food", 20));
        player.setSaturation(20.0f);

        // Restore health to full/max health to prevent dying again
        var maxHealthAttribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;
        player.setHealth(maxHealth);

        String gmStr = yaml.getString("gamemode");
        if (gmStr != null) {
            try {
                player.setGameMode(GameMode.valueOf(gmStr));
            } catch (IllegalArgumentException ignored) {}
        }

        player.setAllowFlight(yaml.getBoolean("allow-flight", false));
        player.setFlying(yaml.getBoolean("flying", false));

        // Restore inventory
        player.getInventory().clear();
        ItemStack[] storage = loadItemArray(yaml.getConfigurationSection("inventory.storage"), 36);
        ItemStack[] armor = loadItemArray(yaml.getConfigurationSection("inventory.armor"), 4);
        ItemStack[] extra = loadItemArray(yaml.getConfigurationSection("inventory.extra"), 1);
        player.getInventory().setStorageContents(storage);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setExtraContents(extra);

        // Restore potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        List<?> effectsList = yaml.getList("potion-effects");
        if (effectsList != null) {
            for (Object obj : effectsList) {
                if (obj instanceof Map) {
                    Map<?, ?> effectMap = (Map<?, ?>) obj;
                    String typeStr = (String) effectMap.get("type");
                    PotionEffectType type = PotionEffectType.getByName(typeStr);
                    if (type != null) {
                        int duration = ((Number) effectMap.get("duration")).intValue();
                        int amplifier = ((Number) effectMap.get("amplifier")).intValue();
                        boolean ambient = (Boolean) effectMap.get("ambient");
                        boolean particles = (Boolean) effectMap.get("particles");
                        boolean icon = effectMap.containsKey("icon") ? (Boolean) effectMap.get("icon") : true;
                        player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, particles, icon));
                    }
                }
            }
        }

        return true;
    }

    private void saveItemArray(ConfigurationSection section, ItemStack[] items) {
        if (items == null || section == null) return;
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                section.set(String.valueOf(i), items[i]);
            }
        }
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
