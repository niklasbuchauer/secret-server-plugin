package de.niklasbuchauer.secretserverplugin.customitem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class CustomItemService {
    private static final String CUSTOM_ITEM_KEY = "custom_item_id";

    private final NamespacedKey customItemIdKey;
    private final Map<String, Supplier<ItemStack>> itemFactories = new HashMap<>();

    public CustomItemService(JavaPlugin plugin) {
        this.customItemIdKey = new NamespacedKey(plugin, CUSTOM_ITEM_KEY);
        registerDefaults();
    }

    public void register(String customItemId, Supplier<ItemStack> factory) {
        itemFactories.put(customItemId, factory);
    }

    public Optional<ItemStack> createItem(String customItemId) {
        Supplier<ItemStack> factory = itemFactories.get(customItemId);
        if (factory == null) {
            return Optional.empty();
        }

        return Optional.of(factory.get());
    }

    public boolean isCustomItem(ItemStack item, String customItemId) {
        return getCustomItemId(item)
                .map(customItemId::equals)
                .orElse(false);
    }

    public Optional<String> getCustomItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(customItemIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    private void registerDefaults() {
        register(CustomItemConstants.SPYGLASS_ID, this::createCustomSpyglass);
        register(CustomItemConstants.BLINDNESS_STICK_ID, this::createCustomBlindnessStick);
    }

    private ItemStack createCustomSpyglass() {
        ItemStack item = new ItemStack(Material.SPYGLASS);
        tagItem(item, CustomItemConstants.SPYGLASS_ID);
        return item;
    }

    private ItemStack createCustomBlindnessStick() {
        ItemStack item = new ItemStack(Material.STICK);
        tagItem(item, CustomItemConstants.BLINDNESS_STICK_ID);
        return item;
    }

    private void tagItem(ItemStack item, String customItemId) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(customItemIdKey, PersistentDataType.STRING, customItemId);
        item.setItemMeta(meta);
    }
}
