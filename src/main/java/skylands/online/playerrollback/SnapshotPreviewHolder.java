package skylands.online.playerrollback;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class SnapshotPreviewHolder implements InventoryHolder {
    private final UUID targetUuid;
    private final String targetName;
    private final File snapshotFile;
    private final List<File> joinSnapshots;
    private final List<File> deathSnapshots;
    private Inventory inventory;

    public SnapshotPreviewHolder(UUID targetUuid, String targetName, File snapshotFile, List<File> joinSnapshots, List<File> deathSnapshots) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.snapshotFile = snapshotFile;
        this.joinSnapshots = joinSnapshots;
        this.deathSnapshots = deathSnapshots;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public File getSnapshotFile() {
        return snapshotFile;
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
