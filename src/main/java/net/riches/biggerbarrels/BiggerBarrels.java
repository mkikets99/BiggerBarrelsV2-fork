package net.riches.biggerbarrels;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class BiggerBarrels extends JavaPlugin {

    public static BiggerBarrels instance;
    private HashMap<Block, Inventory> barrels;

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }

        instance = this;

        barrels = new HashMap<>();

        this.getServer().getPluginManager().registerEvents(new BarrelEvents(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        for (Block barrel : barrels.keySet()) {
            BarrelSerializer.saveBarrel(barrel, barrels.get(barrel));
        }
    }

    public Inventory getBarrel(Block barrel) {
        if (!barrels.containsKey(barrel)) {
            barrels.put(barrel, BarrelSerializer.loadBarrel(barrel));
        }

        return barrels.get(barrel);
    }

    public static BiggerBarrels getInstance() {
        return instance;
    }

    // delete a barrel
    public void deleteBarrel(Block barrel) {
        this.barrels.remove(barrel);
        BarrelSerializer.deleteBarrel(barrel);
    }
}
