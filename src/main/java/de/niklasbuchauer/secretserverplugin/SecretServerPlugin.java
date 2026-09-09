package de.niklasbuchauer.secretserverplugin;

import de.niklasbuchauer.secretserverplugin.command.CustomItemCommand;
import de.niklasbuchauer.secretserverplugin.customitem.CustomItemService;
import de.niklasbuchauer.secretserverplugin.listener.BlindnessStickListener;
import de.niklasbuchauer.secretserverplugin.spyglass.SpyglassVisionService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SecretServerPlugin extends JavaPlugin {
    private SpyglassVisionService spyglassVisionService;

    @Override
    public void onEnable() {
        CustomItemService customItemService = new CustomItemService(this);

        PluginCommand customItemCommand = getCommand("customitem");
        if (customItemCommand == null) {
            throw new IllegalStateException("Command 'customitem' ist nicht in plugin.yml registriert.");
        }

        CustomItemCommand executor = new CustomItemCommand(customItemService);
        customItemCommand.setExecutor(executor);
        customItemCommand.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new BlindnessStickListener(customItemService), this);

        spyglassVisionService = new SpyglassVisionService(this, customItemService);
        getServer().getPluginManager().registerEvents(spyglassVisionService, this);
        spyglassVisionService.start();
    }

    @Override
    public void onDisable() {
        if (spyglassVisionService != null) {
            spyglassVisionService.stop();
        }
    }
}
