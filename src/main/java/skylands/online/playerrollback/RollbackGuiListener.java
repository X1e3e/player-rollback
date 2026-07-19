package skylands.online.playerrollback;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.util.UUID;

public class RollbackGuiListener implements Listener {
    private final PlayerRollbackPlugin plugin;

    private final int[] JOIN_SLOTS = {0, 1, 9, 10, 18, 19, 27, 28, 36, 37};
    private final int[] DEATH_SLOTS = {7, 8, 16, 17, 25, 26, 34, 35, 43, 44};

    public RollbackGuiListener(PlayerRollbackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if (inv == null) return;

        Inventory topInv = event.getInventory();

        // 1. Snapshot List GUI
        if (topInv.getHolder() instanceof SnapshotListHolder holder) {
            event.setCancelled(true); // Prevent picking up items

            if (inv.getHolder() instanceof SnapshotListHolder) {
                int slot = event.getRawSlot();
                Player viewer = (Player) event.getWhoClicked();

                if (slot == 49) { // Close button
                    viewer.closeInventory();
                    return;
                }

                // Check JOIN slots
                int joinIndex = getIndex(JOIN_SLOTS, slot);
                if (joinIndex != -1 && joinIndex < holder.getJoinSnapshots().size()) {
                    File file = holder.getJoinSnapshots().get(joinIndex);
                    Player target = Bukkit.getPlayer(holder.getTargetUuid());

                    if (event.isLeftClick()) {
                        if (target == null || !target.isOnline()) {
                            viewer.sendMessage("§cИгрок " + holder.getTargetName() + " должен быть в сети для восстановления.");
                            return;
                        }
                        boolean success = plugin.getSnapshotManager().restoreSnapshot(target, file);
                        viewer.closeInventory();
                        if (success) {
                            viewer.sendMessage("§aПрофиль игрока " + target.getName() + " успешно восстановлен к входу #" + (joinIndex + 1) + ".");
                            target.sendMessage("§aВаш профиль был восстановлен администратором к входу #" + (joinIndex + 1) + ".");
                        } else {
                            viewer.sendMessage("§cНе удалось восстановить профиль.");
                        }
                    } else if (event.isRightClick()) {
                        plugin.getGuiManager().openSnapshotPreview(viewer, holder.getTargetUuid(), holder.getTargetName(), file, holder.getJoinSnapshots(), holder.getDeathSnapshots());
                    }
                }

                // Check DEATH slots
                int deathIndex = getIndex(DEATH_SLOTS, slot);
                if (deathIndex != -1 && deathIndex < holder.getDeathSnapshots().size()) {
                    File file = holder.getDeathSnapshots().get(deathIndex);
                    Player target = Bukkit.getPlayer(holder.getTargetUuid());

                    if (event.isLeftClick()) {
                        if (target == null || !target.isOnline()) {
                            viewer.sendMessage("§cИгрок " + holder.getTargetName() + " должен быть в сети для восстановления.");
                            return;
                        }
                        boolean success = plugin.getSnapshotManager().restoreSnapshot(target, file);
                        viewer.closeInventory();
                        if (success) {
                            viewer.sendMessage("§aПрофиль игрока " + target.getName() + " успешно восстановлен к смерти #" + (deathIndex + 1) + ".");
                            target.sendMessage("§aВаш профиль был восстановлен администратором к смерти #" + (deathIndex + 1) + ".");
                        } else {
                            viewer.sendMessage("§cНе удалось восстановить профиль.");
                        }
                    } else if (event.isRightClick()) {
                        plugin.getGuiManager().openSnapshotPreview(viewer, holder.getTargetUuid(), holder.getTargetName(), file, holder.getJoinSnapshots(), holder.getDeathSnapshots());
                    }
                }
            }
        }

        // 2. Snapshot Preview GUI
        else if (topInv.getHolder() instanceof SnapshotPreviewHolder holder) {
            event.setCancelled(true); // Prevent picking up items

            if (inv.getHolder() instanceof SnapshotPreviewHolder) {
                int slot = event.getRawSlot();
                Player viewer = (Player) event.getWhoClicked();

                if (slot == 48) { // Back button
                    plugin.getGuiManager().openSnapshotList(viewer, holder.getTargetUuid(), holder.getTargetName());
                } else if (slot == 49) { // Restore button
                    Player target = Bukkit.getPlayer(holder.getTargetUuid());
                    if (target == null || !target.isOnline()) {
                        viewer.sendMessage("§cИгрок " + holder.getTargetName() + " должен быть в сети для восстановления.");
                        return;
                    }

                    boolean success = plugin.getSnapshotManager().restoreSnapshot(target, holder.getSnapshotFile());
                    viewer.closeInventory();

                    if (success) {
                        viewer.sendMessage("§aПрофиль игрока " + target.getName() + " успешно восстановлен из снимка.");
                        target.sendMessage("§aВаш профиль был восстановлен администратором.");
                    } else {
                        viewer.sendMessage("§cНе удалось восстановить профиль.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SnapshotListHolder ||
            event.getInventory().getHolder() instanceof SnapshotPreviewHolder) {
            event.setCancelled(true);
        }
    }

    private int getIndex(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == val) return i;
        }
        return -1;
    }
}
