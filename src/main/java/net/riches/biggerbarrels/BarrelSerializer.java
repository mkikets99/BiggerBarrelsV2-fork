package net.riches.biggerbarrels;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;

public class BarrelSerializer {

    public static void saveBarrel(Block barrel, Inventory inv) {
        writeSave(barrel.getX()+","+barrel.getY()+","+barrel.getZ() + ".barrel", inv);
    }

    private static void startBelowHopperCycle(Block barrel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (barrel.getLocation().add(0, -1, 0).getBlock().getType() != Material.HOPPER || barrel.getType() != Material.BARREL) {
                    this.cancel();
                    return;
                }

                Inventory barrelInv = BiggerBarrels.getInstance().getBarrel(barrel);

                List<ItemStack> contents = new ArrayList<>();

                for (int i = 0; i < barrelInv.getContents().length; i++) {
                    if (barrelInv.getContents()[i] == null)
                        continue;

                    contents.add(barrelInv.getContents()[i].clone());
                }

                // System.out.println("Contents Array: " + contents);

                if (contents.isEmpty()) {
                    return;
                }

                // System.out.println("Contents: " + contents.size() + " items.");

                Hopper hopper = (Hopper) barrel.getLocation().add(0, -1, 0).getBlock().getState();
                // Add a single item to the hopper, making sure to account for if the itemstack has more than one item
                Optional<ItemStack> item = contents.stream().filter(Objects::nonNull).findFirst();

                if (item.isEmpty()) {
                    System.out.println("ERROR: Item is empty!");
                    return;
                }

                ItemStack newitem = item.get().clone();
                newitem.setAmount(1);

                hopper.getInventory().addItem(newitem);
                // Remove the item from the barrel
                barrelInv.removeItem(newitem);

                // refresh everyone looking at the barrel
                barrelInv.getViewers().forEach(player -> {
                    if (player == null)
                        return;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.openInventory(barrelInv);
                        }
                    }.runTask(BiggerBarrels.getInstance());
                });
            }
        }.runTaskTimer(BiggerBarrels.getInstance(), 0, 10);
    }

    private static void startAboveHopperCycle(Block barrel) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (barrel.getLocation().add(0, 1, 0).getBlock().getType() != Material.HOPPER || barrel.getType() != Material.BARREL) {
                    this.cancel();
                    return;
                }

                Hopper hopper = (Hopper) barrel.getLocation().add(0, 1, 0).getBlock().getState();

                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < hopper.getInventory().getContents().length; i++) {
                    if (hopper.getInventory().getContents()[i] == null)
                        continue;

                    contents.add(hopper.getInventory().getContents()[i].clone());
                }

                if (contents.size() == 0) {
                    return;
                }

                // Add a single item to the hopper, making sure to account for if the itemstack has more than one item
                Optional<ItemStack> item = contents.stream().filter(Objects::nonNull).findFirst();

                if (item.isEmpty()) {
                    System.out.println("ERROR: Item is empty!");
                    return;
                }

                ItemStack newitem = item.get().clone();
                newitem.setAmount(1);

                // TODO Is this wrong? Shouldnt it be BiggerBarrels.getInstance().getBarrel(barrel); ?
                Inventory inventory = ((Barrel) barrel.getState()).getInventory();
                inventory.addItem(newitem);

                // Remove the item from the hopper
                hopper.getInventory().removeItem(newitem);
            }
        }.runTaskTimer(BiggerBarrels.getInstance(), 0, 10);
    }

    public static Inventory loadBarrel(Block barrel) {
        if (barrel.getLocation().add(0, -1, 0).getBlock().getType() == Material.HOPPER) {
            startBelowHopperCycle(barrel);
        }
        else if (barrel.getLocation().add(0, 1, 0).getBlock().getType() == Material.HOPPER) {
            startAboveHopperCycle(barrel);
        }

        return loadSave(barrel.getX()+","+barrel.getY()+","+barrel.getZ() + ".barrel");
    }

    public static HashMap<Integer, Map<String, Object>> serialize(final Inventory inventory) {
        final HashMap<Integer, Map<String, Object>> barrelContents = new HashMap<>();
        final ItemStack[] inventoryContents = inventory.getContents();

        for (int i = 0; i < 54; ++i) {
            if (inventoryContents[i] == null) continue;

            barrelContents.put(i, inventoryContents[i].serialize());
        }

        return barrelContents;
    }

    // save the barrel contents to a file
    public static void writeSave(final String path, final Inventory inventory) {
        final HashMap<Integer, Map<String, Object>> barrelContents = BarrelSerializer.serialize(inventory);

        try {
            new File(BiggerBarrels.getInstance().getDataFolder(), path).createNewFile();

            final FileOutputStream f = new FileOutputStream(new File(BiggerBarrels.getInstance().getDataFolder(), path));
            final BukkitObjectOutputStream o = new BukkitObjectOutputStream(f);

            o.writeObject(barrelContents);
            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // delete a barrels file
    public static void deleteBarrel(Block barrel) {
        new File(BiggerBarrels.getInstance().getDataFolder(), barrel.getX()+","+barrel.getY()+","+barrel.getZ() + ".barrel").delete();
    }

    // load the barrel contents from a file
    public static Inventory loadSave(final String path) {
        try {
            final FileInputStream fi = new FileInputStream(new File(BiggerBarrels.getInstance().getDataFolder(), path));
            final BukkitObjectInputStream oi = new BukkitObjectInputStream(fi);
            final HashMap<Integer, Map<String, Object>> barrelContents = (HashMap<Integer, Map<String, Object>>) oi.readObject();

            oi.close();
            fi.close();

            final Inventory inventory = Bukkit.createInventory(null, 54, "Barrel");

            for (final Map.Entry<Integer, Map<String, Object>> entry : barrelContents.entrySet()) {
                inventory.setItem(entry.getKey(), ItemStack.deserialize(entry.getValue()));
            }

            return inventory;
        } catch (Exception ignored) {}

        return Bukkit.createInventory(null, 54, "Barrel");
    }
}
