package skylands.online.playerrollback;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RollbackCommand implements CommandExecutor, TabCompleter {
    private final PlayerRollbackPlugin plugin;

    public RollbackCommand(PlayerRollbackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("rollback.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /rollback list <игрок> ИЛИ /rollback restore <игрок> <join/death> <номер>");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];
        
        @SuppressWarnings("deprecation")
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = targetOffline.getUniqueId();

        if (subCommand.equals("list")) {
            if (sender instanceof Player playerSender) {
                // Open GUI for players
                plugin.getGuiManager().openSnapshotList(playerSender, uuid, targetOffline.getName());
                return true;
            } else {
                // Console output fallback
                List<File> joinFiles = plugin.getSnapshotManager().getSnapshots(uuid, "JOIN");
                List<File> deathFiles = plugin.getSnapshotManager().getSnapshots(uuid, "DEATH");
                if (joinFiles.isEmpty() && deathFiles.isEmpty()) {
                    sender.sendMessage("§cСнимки состояния для игрока " + targetName + " не найдены.");
                    return true;
                }

                sender.sendMessage("§eСнимки состояния для игрока §f" + targetName + "§e:");
                sender.sendMessage(" §a[Входы (JOIN)]:");
                int index = 1;
                for (File file : joinFiles) {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    sender.sendMessage("  §a[#" + index + "] §f" + yaml.getString("timestamp"));
                    index++;
                }
                sender.sendMessage(" §c[Смерти (DEATH)]:");
                index = 1;
                for (File file : deathFiles) {
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                    sender.sendMessage("  §c[#" + index + "] §f" + yaml.getString("timestamp"));
                    index++;
                }
                return true;
            }
        } 
        
        if (subCommand.equals("restore")) {
            if (args.length < 4) {
                sender.sendMessage("§cИспользование: /rollback restore <игрок> <join/death> <номер>");
                return true;
            }

            String type = args[2].toUpperCase();
            if (!type.equals("JOIN") && !type.equals("DEATH")) {
                sender.sendMessage("§cУкажите верный тип: join или death.");
                return true;
            }

            Player targetOnline = Bukkit.getPlayer(targetName);
            if (targetOnline == null || !targetOnline.isOnline()) {
                sender.sendMessage("§cИгрок " + targetName + " должен быть в сети для восстановления профиля.");
                return true;
            }

            List<File> snapshots = plugin.getSnapshotManager().getSnapshots(uuid, type);
            if (snapshots.isEmpty()) {
                sender.sendMessage("§cУ игрока " + targetName + " нет сохраненных снимков типа " + type + ".");
                return true;
            }

            int id;
            try {
                id = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверный номер снимка: " + args[3]);
                return true;
            }

            if (id < 1 || id > snapshots.size()) {
                sender.sendMessage("§cНомер снимка должен быть в диапазоне от 1 до " + snapshots.size() + ".");
                return true;
            }

            File snapshotFile = snapshots.get(id - 1);
            boolean success = plugin.getSnapshotManager().restoreSnapshot(targetOnline, snapshotFile);

            if (success) {
                sender.sendMessage("§aПрофиль игрока " + targetOnline.getName() + " успешно восстановлен к снимку " + type + " #" + id + ".");
                targetOnline.sendMessage("§aВаш профиль был восстановлен администратором к снимку " + type + " #" + id + ".");
            } else {
                sender.sendMessage("§cНе удалось восстановить профиль.");
            }
            return true;
        }

        sender.sendMessage("§cНеизвестная подкоманда. Используйте: list или restore.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("rollback.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String query = args[0].toLowerCase();
            if ("list".startsWith(query)) completions.add("list");
            if ("restore".startsWith(query)) completions.add("restore");
            return completions;
        }

        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            String query = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(query)) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("restore")) {
            List<String> completions = new ArrayList<>();
            String query = args[2].toLowerCase();
            if ("join".startsWith(query)) completions.add("join");
            if ("death".startsWith(query)) completions.add("death");
            return completions;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("restore")) {
            String targetName = args[1];
            String type = args[2].toUpperCase();
            if (type.equals("JOIN") || type.equals("DEATH")) {
                @SuppressWarnings("deprecation")
                OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
                List<File> snapshots = plugin.getSnapshotManager().getSnapshots(targetOffline.getUniqueId(), type);
                if (!snapshots.isEmpty()) {
                    List<String> completions = new ArrayList<>();
                    for (int i = 1; i <= snapshots.size(); i++) {
                        completions.add(String.valueOf(i));
                    }
                    return completions;
                }
            }
        }

        return Collections.emptyList();
    }
}
