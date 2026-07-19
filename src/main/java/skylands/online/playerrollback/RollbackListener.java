package skylands.online.playerrollback;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class RollbackListener implements Listener {
    private final PlayerRollbackPlugin plugin;

    public RollbackListener(PlayerRollbackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Create snapshot on join
        plugin.getSnapshotManager().createSnapshot(player, "JOIN");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Create snapshot immediately on death (capturing inventory before drops)
        plugin.getSnapshotManager().createSnapshot(player, "DEATH");
    }
}
