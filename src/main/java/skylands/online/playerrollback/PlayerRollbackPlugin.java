package skylands.online.playerrollback;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerRollbackPlugin extends JavaPlugin {
    private SnapshotManager snapshotManager;
    private SnapshotGuiManager guiManager;

    @Override
    public void onEnable() {
        // Initialize managers
        snapshotManager = new SnapshotManager(this);
        guiManager = new SnapshotGuiManager(this);

        // Register command
        RollbackCommand command = new RollbackCommand(this);
        var cmd = getCommand("rollback");
        if (cmd != null) {
            cmd.setExecutor(command);
            cmd.setTabCompleter(command);
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new RollbackListener(this), this);
        getServer().getPluginManager().registerEvents(new RollbackGuiListener(this), this);

        getLogger().info("Плагин PlayerRollback успешно включен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин PlayerRollback выключен.");
    }

    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
    }

    public SnapshotGuiManager getGuiManager() {
        return guiManager;
    }
}
