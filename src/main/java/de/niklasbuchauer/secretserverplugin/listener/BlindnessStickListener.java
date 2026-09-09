package de.niklasbuchauer.secretserverplugin.listener;

import de.niklasbuchauer.secretserverplugin.customitem.CustomItemConstants;
import de.niklasbuchauer.secretserverplugin.customitem.CustomItemService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BlindnessStickListener implements Listener {
    private static final int BLINDNESS_SECONDS = 5;

    private final CustomItemService customItemService;

    public BlindnessStickListener(CustomItemService customItemService) {
        this.customItemService = customItemService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        if (!attacker.getName().equalsIgnoreCase(CustomItemConstants.SPECIAL_PLAYER)) {
            return;
        }

        if (attacker.getUniqueId().equals(target.getUniqueId())) {
            return;
        }

        ItemStack heldItem = attacker.getInventory().getItemInMainHand();
        if (!customItemService.isCustomItem(heldItem, CustomItemConstants.BLINDNESS_STICK_ID)) {
            return;
        }

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                BLINDNESS_SECONDS * 20,
                0,
                true,
                true,
                true
        ));
    }
}
