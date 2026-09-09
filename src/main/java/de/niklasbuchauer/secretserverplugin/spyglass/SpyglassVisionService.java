package de.niklasbuchauer.secretserverplugin.spyglass;

import de.niklasbuchauer.secretserverplugin.customitem.CustomItemConstants;
import de.niklasbuchauer.secretserverplugin.customitem.CustomItemService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpyglassVisionService implements Listener {
    private static final double MAX_DISTANCE = 48.0;
    private static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;
    private static final double CONE_ANGLE_DEGREES = 7.0;
    private static final double MIN_DOT_PRODUCT = Math.cos(Math.toRadians(CONE_ANGLE_DEGREES));
    private static final long UPDATE_INTERVAL_TICKS = 4L;
    private static final int GLOWING_DURATION_TICKS = 20;

    private final JavaPlugin plugin;
    private final CustomItemService customItemService;
    private final Map<UUID, UUID> glowingTargetsByViewer = new ConcurrentHashMap<>();

    private BukkitTask updateTask;

    public SpyglassVisionService(JavaPlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
    }

    public void start() {
        if (updateTask != null) {
            return;
        }

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, UPDATE_INTERVAL_TICKS);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        clearAllGlowStates();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quittingPlayer = event.getPlayer();

        if (quittingPlayer.getName().equalsIgnoreCase(CustomItemConstants.SPECIAL_PLAYER)) {
            clearViewerGlow(quittingPlayer);
            return;
        }

        Iterator<Map.Entry<UUID, UUID>> iterator = glowingTargetsByViewer.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            if (!entry.getValue().equals(quittingPlayer.getUniqueId())) {
                continue;
            }

            Player viewer = Bukkit.getPlayer(entry.getKey());
            if (viewer != null && viewer.isOnline()) {
                viewer.sendPotionEffectChangeRemove(quittingPlayer, PotionEffectType.GLOWING);
            }
            iterator.remove();
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getName().equalsIgnoreCase(CustomItemConstants.SPECIAL_PLAYER)) {
                continue;
            }

            if (!isUsingCustomSpyglass(player)) {
                clearViewerGlow(player);
                continue;
            }

            Player target = findTargetPlayer(player);
            updateGlowForViewer(player, target);
        }
    }

    private boolean isUsingCustomSpyglass(Player player) {
        if (!player.isHandRaised()) {
            return false;
        }

        ItemStack activeItem = player.getActiveItem();
        return customItemService.isCustomItem(activeItem, CustomItemConstants.SPYGLASS_ID);
    }

    private Player findTargetPlayer(Player viewer) {
        Location eyeLocation = viewer.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();

        Player bestTarget = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (Player candidate : viewer.getWorld().getPlayers()) {
            if (candidate.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }

            Vector toCandidate = candidate.getEyeLocation().toVector().subtract(eyeLocation.toVector());
            double distanceSquared = toCandidate.lengthSquared();
            if (distanceSquared <= 0 || distanceSquared > MAX_DISTANCE_SQUARED) {
                continue;
            }

            Vector toCandidateDirection = toCandidate.normalize();
            double dotProduct = lookDirection.dot(toCandidateDirection);
            if (dotProduct < MIN_DOT_PRODUCT) {
                continue;
            }

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private void updateGlowForViewer(Player viewer, Player newTarget) {
        UUID viewerId = viewer.getUniqueId();
        UUID previousTargetId = glowingTargetsByViewer.get(viewerId);

        if (previousTargetId != null && (newTarget == null || !previousTargetId.equals(newTarget.getUniqueId()))) {
            Player previousTarget = Bukkit.getPlayer(previousTargetId);
            if (previousTarget != null && previousTarget.isOnline()) {
                viewer.sendPotionEffectChangeRemove(previousTarget, PotionEffectType.GLOWING);
            }
        }

        if (newTarget == null) {
            glowingTargetsByViewer.remove(viewerId);
            return;
        }

        viewer.sendPotionEffectChange(newTarget, new PotionEffect(
                PotionEffectType.GLOWING,
                GLOWING_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));

        glowingTargetsByViewer.put(viewerId, newTarget.getUniqueId());
    }

    private void clearViewerGlow(Player viewer) {
        UUID previousTargetId = glowingTargetsByViewer.remove(viewer.getUniqueId());
        if (previousTargetId == null) {
            return;
        }

        Player previousTarget = Bukkit.getPlayer(previousTargetId);
        if (previousTarget != null && previousTarget.isOnline()) {
            viewer.sendPotionEffectChangeRemove(previousTarget, PotionEffectType.GLOWING);
        }
    }

    private void clearAllGlowStates() {
        for (Map.Entry<UUID, UUID> entry : glowingTargetsByViewer.entrySet()) {
            Player viewer = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue());
            if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
                continue;
            }

            viewer.sendPotionEffectChangeRemove(target, PotionEffectType.GLOWING);
        }

        glowingTargetsByViewer.clear();
    }
}
