package skylands.online.playerrollback;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class SnapshotListHolder implements InventoryHolder {
    private final UUID targetUuid;
    private final String targetName;
    private final List<File> joinSnapshots;
    private final List<File> deathSnapshots;
    private Inventory inventory;

    public SnapshotListHolder(UUID targetUuid, String targetName, List<File> joinSnapshots, List<File> deathSnapshots) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.joinSnapshots = joinSnapshots;
        this.deathSnapshots = deathSnapshots;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public List<File> getJoinSnapshots() {
        return joinSnapshots;
    }

    public List<File> getDeathSnapshots() {
        return deathSnapshots;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
