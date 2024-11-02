package net.riches.biggerbarrels;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BarrelEvents implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        for (BlockState tileEntity : e.getChunk().getTileEntities()) {
            if (!(tileEntity instanceof Barrel barrel)) {
                return;
            }

            BiggerBarrels.getInstance().getBarrel(barrel.getBlock());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onHopperItemMove(InventoryMoveItemEvent e) {
        if (!e.getSource().getType().equals(InventoryType.HOPPER))
            return;
        if (!e.getDestination().getType().equals(InventoryType.BARREL))
            return;

        BiggerBarrels.getInstance().getBarrel(e.getDestination().getLocation().getBlock()).addItem(e.getItem());

        new BukkitRunnable() {
            @Override
            public void run() {
                e.getDestination().removeItem(e.getItem());
            }
        }.runTask(BiggerBarrels.getInstance());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Block barrel;
        Inventory inventory;

        if (e.getBlock().getType() == Material.BARREL) {
            if (e.getBlock().getLocation().clone().subtract(0, 1, 0).getBlock().getType() != Material.HOPPER)
                return;

            barrel = e.getBlock().getLocation().clone().subtract(0, 1, 0).getBlock();

            inventory = BiggerBarrels.getInstance().getBarrel(e.getBlock());
            if (inventory == null) return;
        }
        else if (e.getBlock().getType() == Material.HOPPER) {
            if (e.getBlock().getLocation().clone().add(0, 1, 0).getBlock().getType() != Material.BARREL)
                return;

            barrel = e.getBlock().getLocation().clone().add(0, 1, 0).getBlock();

            inventory = BiggerBarrels.getInstance().getBarrel(barrel);
            if (inventory == null) return;
        }
        else return;

        Inventory finalInventory = inventory;
        Block finalBarrel = barrel;

        new BukkitRunnable() {
            @Override
            public void run() {
                if ((finalBarrel.getType() != Material.BARREL || e.getBlock().getType() != Material.HOPPER) && (finalBarrel.getType() != Material.HOPPER || e.getBlock().getType() != Material.BARREL)) {
                    this.cancel();
                    return;
                }

                List<ItemStack> contents = new ArrayList<>();
                for (int i = 0; i < finalInventory.getContents().length; i++) {
                    if (finalInventory.getContents()[i] == null)
                        continue;

                    contents.add(finalInventory.getContents()[i].clone());
                }

                if (contents.isEmpty())
                    return;

                Optional<ItemStack> item = contents.stream().filter(Objects::nonNull).findFirst();

                if (item.isEmpty()) {
                    System.out.println("ERROR: Item is empty!");
                    return;
                }

                ItemStack newitem = item.get().clone();
                newitem.setAmount(1);

                if (e.getBlock().getState() instanceof Hopper hopper)
                    hopper.getInventory().addItem(newitem);
                else if (e.getBlock().getState() instanceof Barrel barrel)
                    barrel.getInventory().addItem(newitem);
                else
                    return;

                System.out.println(newitem);

                // Remove the item from the barrel
                finalInventory.removeItem(newitem);
            }
        }.runTaskTimer(BiggerBarrels.getInstance(), 0, 10);
    }

    @EventHandler
    public void onBarrelOpened(PlayerInteractEvent e) {
        if (!e.hasBlock()) return;

        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(e.getClickedBlock().getLocation(), true, null);

            if (claim != null && claim.getOwnerID() != null && !claim.getOwnerID().equals(e.getPlayer().getUniqueId()) && !claim.hasExplicitPermission(e.getPlayer(), ClaimPermission.Inventory)) {
                return;
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Lands")) {
            LandsIntegration lands = LandsIntegration.of(BiggerBarrels.getInstance());
            Area area = lands.getArea(e.getClickedBlock().getLocation());

            if (area != null && !area.isTrusted(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp())
                return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("ChestShop")) {
            Sign connectedSign = uBlock.getConnectedSign(e.getClickedBlock());

            if (connectedSign != null && !ChestShopSign.canAccess(e.getPlayer(), connectedSign)) {
                return;
            }
        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK | e.getClickedBlock().getType() != Material.BARREL) return;

        if (e.getPlayer().isSneaking() && e.getItem() != null && (e.getItem().getType().isBlock() || e.getItem().getType() == Material.ITEM_FRAME)) return;

        e.getPlayer().playSound(e.getClickedBlock().getLocation(), Sound.BLOCK_BARREL_OPEN, 1.0f, 1.0f);
        e.setCancelled(true);

        // load barrel from e.getClickedBlock()
        Inventory barrelInv = BiggerBarrels.getInstance().getBarrel(e.getClickedBlock());
        e.getPlayer().openInventory(barrelInv);
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!event.getView().getTitle().contains("Barrel"))
            return;

        ((Player)event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.BLOCK_BARREL_CLOSE, 1.0f, 1.0f);
    }

    @EventHandler
    public void onBarrelBroke(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.BARREL) return;

        Inventory barrelInv = BiggerBarrels.getInstance().getBarrel(e.getBlock());
        for (ItemStack item : barrelInv.getContents()) {
            if (item == null) continue;

            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item);
        }

        BiggerBarrels.getInstance().deleteBarrel(e.getBlock());
    }
}
