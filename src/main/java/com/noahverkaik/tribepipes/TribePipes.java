package com.noahverkaik.tribepipes;

import com.comphenix.packetwrapper.*;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.noahverkaik.tribemines.Mine;
import com.noahverkaik.tribemines.TribeMines;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.nowaha.infinitechests.InfiniteChests;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class TribePipes extends JavaPlugin implements Listener {

    //HashMap<Location, ItemStack> itemsInTransfer = new HashMap<>();

    public static TribePipes instance;

    HashMap<Integer, Integer> ids = new HashMap<>();
    HashMap<Location, ItemStack> itemsMoved = new HashMap<>();

    void playItemTransferAnimation(Directional dir, Block to, ItemStack old) {
        if (!to.getChunk().isLoaded()) return;

        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(to.getWorld())) {
                if (player.getLocation().distance(to.getLocation()) < 25) {
                    players.add(player);
                }
            }
        }

        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity();
        Integer entityID = (int)(Math.random() * Integer.MAX_VALUE);
        packet.setEntityID(entityID);
        packet.setType(78);

        Location loc = to.getRelative(dir.getFacing()).getLocation().clone().subtract(-.5, 1.5, -.5);
        if (dir.getFacing().equals(BlockFace.UP)) {
            loc.add(0, 2f, 0);
        } else if (dir.getFacing().equals(BlockFace.EAST)) {
            loc.add(1, 1.1f, 0);
        } else if (dir.getFacing().equals(BlockFace.WEST)) {
            loc.add(-1, 1.1f, 0);
        } else if (dir.getFacing().equals(BlockFace.NORTH)) {
            loc.add(0, 1.1f, -1);
        } else if (dir.getFacing().equals(BlockFace.SOUTH)) {
            loc.add(0, 1.1f, 1);
        }

        if (!old.getType().isBlock()) {
            loc.subtract(0, 0.35f, -0.2f);
        }

        packet.setX(loc.getX());
        packet.setY(loc.getY());
        packet.setZ(loc.getZ());
        for (Player player : players) {
            packet.sendPacket(player);
        }

        WrapperPlayServerEntityMetadata packet2 = new WrapperPlayServerEntityMetadata();
        packet2.setEntityID(entityID);
        WrappedDataWatcher dataWatcher = new WrappedDataWatcher(packet2.getMetadata());
        WrappedDataWatcher.WrappedDataWatcherObject noGravityIndex = new WrappedDataWatcher.WrappedDataWatcherObject(5, WrappedDataWatcher.Registry.get(Boolean.class));
        WrappedDataWatcher.WrappedDataWatcherObject invisibleIndex = new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
        WrappedDataWatcher.WrappedDataWatcherObject smallIndex = new WrappedDataWatcher.WrappedDataWatcherObject(11, WrappedDataWatcher.Registry.get(Byte.class));
        dataWatcher.setObject(invisibleIndex, (byte)0x20);
        dataWatcher.setObject(noGravityIndex, true);
        dataWatcher.setObject(smallIndex, (byte)0x01);
        packet2.setMetadata(dataWatcher.getWatchableObjects());

        for (Player player : players) {
            packet2.sendPacket(player);
        }

        ItemStack clone = old.clone();
        clone.setAmount(1);
        WrapperPlayServerEntityEquipment equipment = new WrapperPlayServerEntityEquipment();
        equipment.setEntityID(entityID);
        equipment.setSlot(EnumWrappers.ItemSlot.HEAD);
        equipment.setItem(clone);

        for (Player player : players) {
            equipment.sendPacket(player);
        }

        ids.put(entityID, Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            int index = 0;

            @Override
            public void run() {
                index++;

                WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport();
                teleport.setEntityID(entityID);
                if (dir.getFacing().equals(BlockFace.DOWN)) {
                    teleport.setX(loc.getX());
                    teleport.setY(loc.clone().getY() + index*(3.5f / 20f));
                    teleport.setZ(loc.getZ());
                } else if (dir.getFacing().equals(BlockFace.UP)) {
                    teleport.setX(loc.getX());
                    teleport.setY(loc.clone().getY() - index*(3.5f / 20f));
                    teleport.setZ(loc.getZ());
                } else if (dir.getFacing().equals(BlockFace.EAST)) {
                    teleport.setX(loc.clone().getX() - index*(3.5f / 20f));
                    teleport.setY(loc.getY());
                    teleport.setZ(loc.getZ());
                } else if (dir.getFacing().equals(BlockFace.WEST)) {
                    teleport.setX(loc.clone().getX() + index*(3.5f / 20f));
                    teleport.setY(loc.getY());
                    teleport.setZ(loc.getZ());
                } else if (dir.getFacing().equals(BlockFace.NORTH)) {
                    teleport.setX(loc.getX());
                    teleport.setY(loc.getY());
                    teleport.setZ(loc.clone().getZ() + index*(3.5f / 20f));
                } else if (dir.getFacing().equals(BlockFace.SOUTH)) {
                    teleport.setX(loc.getX());
                    teleport.setY(loc.getY());
                    teleport.setZ(loc.clone().getZ() - index*(3.5f / 20f));
                }

                for (Player player : players) {
                    teleport.sendPacket(player);
                }

                if (index >= 20) {
                    Bukkit.getScheduler().cancelTask(ids.get(entityID));
                    WrapperPlayServerEntityDestroy destroy = new WrapperPlayServerEntityDestroy();
                    destroy.setEntityIds(new int[] {entityID});
                    for (Player player : players) {
                        destroy.sendPacket(player);
                    }
                }
            }
        }, 0, 1));
    }

    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;

        getServer().getPluginManager().registerEvents(this, this);

        /*// Move items out of the pipe into their targets.
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (String uuid : getConfig().getConfigurationSection("data").getKeys(false)) {
                if (!uuid.equalsIgnoreCase("UUID")) {
                    Block to = decodeLocation(getConfig().getString("data." + uuid + ".to")).getBlock();
                    if (itemsInTransfer.containsKey(to.getLocation())) {
                        Directional dir = (Directional) to.getBlockData();
                        Block target = to.getRelative(dir.getFacing().getOppositeFace());
                        if (target.getType().equals(Material.CHEST)) {
                            Chest targetChest = (Chest) target.getState();
                            ItemStack item = itemsInTransfer.get(to.getLocation());
                            ItemStack old = item.clone();
                            if (targetChest.getInventory().firstEmpty() != -1) {
                                if (item.getAmount() > 8) {
                                    ItemStack clone = item.clone();
                                    clone.setAmount(8);
                                    targetChest.getInventory().addItem(clone);
                                    item.setAmount(item.getAmount() - 8);
                                    itemsInTransfer.put(to.getLocation(), item);
                                    playItemTransferAnimation(dir, to, old);
                                } else {
                                    targetChest.getInventory().addItem(item.clone());
                                    item.setAmount(0);
                                    itemsInTransfer.remove(to.getLocation());
                                    playItemTransferAnimation(dir, to, old);
                                }
                            }
                        } else if (target.getType().equals(Material.AIR)) {
                            ItemStack item = itemsInTransfer.get(to.getLocation());

                            Item drop = target.getLocation().getWorld().dropItem(target.getLocation().clone().add(0.5f, 0, 0.5f), item);
                            drop.setVelocity(new Vector(0, 0, 0));

                            itemsInTransfer.remove(to.getLocation());

                            if (dir.getFacing().equals(BlockFace.DOWN)) {
                                drop.setVelocity(new Vector(Math.random() * 0.1f, .3, Math.random() * 0.1f));
                            } else if (dir.getFacing().equals(BlockFace.UP)) {
                                drop.setVelocity(new Vector(0, -.5, 0));
                            } else if (dir.getFacing().equals(BlockFace.EAST)) {
                                drop.setVelocity(new Vector(-.1, 0, 0));
                            } else if (dir.getFacing().equals(BlockFace.WEST)) {
                                drop.setVelocity(new Vector(.1, 0, 0));
                            } else if (dir.getFacing().equals(BlockFace.NORTH)) {
                                drop.setVelocity(new Vector(0, 0, .1));
                            } else if (dir.getFacing().equals(BlockFace.SOUTH)) {
                                drop.setVelocity(new Vector(0, 0, -.1));
                            }

                            playItemTransferAnimation(dir, to, item);
                        } else if (target.getType().equals(Material.FURNACE)) {
                            Furnace targetChest = (Furnace) target.getState();
                            ItemStack item = itemsInTransfer.get(to.getLocation());
                            ItemStack old = item.clone();
                            if (targetChest.getInventory().getSmelting() == null) {
                                if (item.getAmount() > 8) {
                                    ItemStack clone = item.clone();
                                    clone.setAmount(8);
                                    targetChest.getInventory().setSmelting(clone);
                                    item.setAmount(item.getAmount() - 8);
                                    itemsInTransfer.put(to.getLocation(), item);
                                    playItemTransferAnimation(dir, to, old);
                                } else {
                                    targetChest.getInventory().setSmelting(item.clone());
                                    item.setAmount(0);
                                    itemsInTransfer.remove(to.getLocation());
                                    playItemTransferAnimation(dir, to, old);
                                }
                            } else if (targetChest.getInventory().getSmelting().getType().equals(item.getType())) {
                                if (targetChest.getInventory().getSmelting().getAmount() < item.getType().getMaxStackSize()) {
                                    int freeSpace = item.getType().getMaxStackSize() - targetChest.getInventory().getSmelting().getAmount();
                                    if (freeSpace >= 8) {
                                        if (item.getAmount() > 8) {
                                            targetChest.getInventory().getSmelting().setAmount(targetChest.getInventory().getSmelting().getAmount() + 8);
                                            item.setAmount(item.getAmount() - 8);
                                            itemsInTransfer.put(to.getLocation(), item);
                                            playItemTransferAnimation(dir, to, old);
                                        } else {
                                            targetChest.getInventory().getSmelting().setAmount(targetChest.getInventory().getSmelting().getAmount() + 8);
                                            item.setAmount(0);
                                            itemsInTransfer.remove(to.getLocation());
                                            playItemTransferAnimation(dir, to, old);
                                        }
                                    } else {
                                        if (item.getAmount() > freeSpace) {
                                            targetChest.getInventory().getSmelting().setAmount(targetChest.getInventory().getSmelting().getAmount() + freeSpace);
                                            item.setAmount(item.getAmount() - freeSpace);
                                            itemsInTransfer.put(to.getLocation(), item);
                                            playItemTransferAnimation(dir, to, old);
                                        } else {
                                            targetChest.getInventory().getSmelting().setAmount(item.getType().getMaxStackSize());
                                            item.setAmount(0);
                                            itemsInTransfer.remove(to.getLocation());
                                            playItemTransferAnimation(dir, to, old);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            for (String uuid : getConfig().getConfigurationSection("data").getKeys(false)) {
                if (!uuid.equalsIgnoreCase("UUID")) {
                    Block from = decodeLocation(getConfig().getString("data." + uuid + ".take-from")).getBlock();
                    Location to = decodeLocation(getConfig().getString("data." + uuid + ".to"));

                    if (from.getType().equals(Material.CHEST) || from.getType().equals(Material.HOPPER)) {
                        Container chest = (Container) from.getState();
                        Inventory inventory = chest.getInventory();

                        if (inventory.getName().contains(" Void Chest (") && inventory.getName().contains("§8")) {
                            Chest actualChest = (Chest) chest;
                            if (inventory.getName().contains("§c-")) {
                                return;
                            }

                            InfiniteChests instance = InfiniteChests.instance;

                            String typeName = ChatColor.stripColor(inventory.getName().split(" Void Chest")[0]).toUpperCase().replace(" ", "_");
                            Material type = Material.valueOf(typeName);

                            Integer amountStored = Integer.parseInt(inventory.getName().split("\\(")[1].split("\\)")[0]);
                            ItemStack itemInTransfer = itemsInTransfer.get(to);
                            if (itemInTransfer != null) {
                                if (itemInTransfer.getType().equals(type)) {
                                    if (amountStored >= 8) {
                                        amountStored -= 8;
                                        itemInTransfer.setAmount(itemInTransfer.getAmount() + 8);
                                        itemsInTransfer.put(to, itemInTransfer);

                                        actualChest.setCustomName(actualChest.getCustomName().split("\\(")[0] + "(" + amountStored + ")");
                                        actualChest.update();

                                        Hologram hologram = instance.holograms.get(actualChest.getLocation());
                                        TextLine line;
                                        if (hologram == null) {
                                            hologram = HologramsAPI.createHologram(instance, actualChest.getLocation().add(0.5f, 1.2f, 0.5f));
                                            instance.holograms.put(actualChest.getLocation(), hologram);
                                            line = hologram.appendTextLine("Void Chest");
                                        } else {
                                            line = (TextLine) hologram.getLine(0);
                                        }

                                        line.setText(instance.getFormattedChestLabel(actualChest.getLocation(), amountStored));
                                    } else if (amountStored > 0) {
                                        itemInTransfer.setAmount(itemInTransfer.getAmount() + amountStored);
                                        itemsInTransfer.put(to, itemInTransfer);

                                        amountStored = 0;
                                        actualChest.setCustomName(actualChest.getCustomName().split("\\(")[0] + "(" + amountStored + ")");
                                        actualChest.update();

                                        Hologram hologram = instance.holograms.get(actualChest.getLocation());
                                        TextLine line;
                                        if (hologram == null) {
                                            hologram = HologramsAPI.createHologram(instance, actualChest.getLocation().add(0.5f, 1.2f, 0.5f));
                                            instance.holograms.put(actualChest.getLocation(), hologram);
                                            line = hologram.appendTextLine("Void Chest");
                                        } else {
                                            line = (TextLine) hologram.getLine(0);
                                        }

                                        line.setText(instance.getFormattedChestLabel(actualChest.getLocation(), amountStored));
                                    }
                                }
                            } else {
                                if (amountStored >= 8) {
                                    amountStored -= 8;
                                    itemsInTransfer.put(to, new ItemStack(type, 8));

                                    actualChest.setCustomName(actualChest.getCustomName().split("\\(")[0] + "(" + amountStored + ")");
                                    actualChest.update();

                                    Hologram hologram = instance.holograms.get(actualChest.getLocation());
                                    TextLine line;
                                    if (hologram == null) {
                                        hologram = HologramsAPI.createHologram(instance, actualChest.getLocation().add(0.5f, 1.2f, 0.5f));
                                        instance.holograms.put(actualChest.getLocation(), hologram);
                                        line = hologram.appendTextLine("Void Chest");
                                    } else {
                                        line = (TextLine) hologram.getLine(0);
                                    }

                                    line.setText(instance.getFormattedChestLabel(actualChest.getLocation(), amountStored));
                                } else if (amountStored > 0) {
                                    itemsInTransfer.put(to, new ItemStack(type, amountStored));

                                    amountStored = 0;
                                    actualChest.setCustomName(actualChest.getCustomName().split("\\(")[0] + "(" + amountStored + ")");
                                    actualChest.update();

                                    Hologram hologram = instance.holograms.get(actualChest.getLocation());
                                    TextLine line;
                                    if (hologram == null) {
                                        hologram = HologramsAPI.createHologram(instance, actualChest.getLocation().add(0.5f, 1.2f, 0.5f));
                                        instance.holograms.put(actualChest.getLocation(), hologram);
                                        line = hologram.appendTextLine("Void Chest");
                                    } else {
                                        line = (TextLine) hologram.getLine(0);
                                    }

                                    line.setText(instance.getFormattedChestLabel(actualChest.getLocation(), amountStored));
                                }
                            }
                        } else if (inventory.getName().contains(" Mine")) {

                        } else {
                            for (ItemStack item : inventory.getStorageContents()) {
                                if (item != null) {
                                    if (item.getType() != Material.AIR) {
                                        if (itemsInTransfer.containsKey(to)) {
                                            if (itemsInTransfer.get(to).getType() == item.getType()) {
                                                ItemStack itemInTransfer = itemsInTransfer.get(to);
                                                int freeSpaces = itemInTransfer.getType().getMaxStackSize() - itemInTransfer.getAmount();
                                                if (freeSpaces > 8) {
                                                    freeSpaces = 8;
                                                }
                                                if (item.getAmount() >= freeSpaces) {
                                                    itemInTransfer.setAmount(itemInTransfer.getAmount() + freeSpaces);
                                                    item.setAmount(item.getAmount() - freeSpaces);
                                                } else {
                                                    itemInTransfer.setAmount(itemInTransfer.getAmount() + item.getAmount());
                                                    item.setAmount(0);
                                                }
                                                break;
                                            } else {
                                                continue;
                                            }
                                        } else {
                                            ItemStack clone = item.clone();
                                            if (item.getAmount() >= 8) {
                                                clone.setAmount(8);
                                                item.setAmount(item.getAmount() - 8);
                                            } else {
                                                clone.setAmount(item.getAmount());
                                                item.setAmount(0);
                                            }

                                            itemsInTransfer.put(to, clone);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } else if (from.getType().equals(Material.FURNACE)) {
                        Furnace chest = (Furnace) from.getState();
                        ItemStack item = chest.getInventory().getResult();
                        if (item != null) {
                            if (itemsInTransfer.containsKey(to)) {
                                if (itemsInTransfer.get(to).getType() == item.getType()) {
                                    ItemStack itemInTransfer = itemsInTransfer.get(to);
                                    int freeSpaces = itemInTransfer.getType().getMaxStackSize() - itemInTransfer.getAmount();
                                    if (freeSpaces > 8) {
                                        freeSpaces = 8;
                                    }
                                    if (item.getAmount() >= freeSpaces) {
                                        itemInTransfer.setAmount(itemInTransfer.getAmount() + freeSpaces);
                                        item.setAmount(item.getAmount() - freeSpaces);
                                    } else {
                                        itemInTransfer.setAmount(itemInTransfer.getAmount() + item.getAmount());
                                        item.setAmount(0);
                                    }
                                    break;
                                } else {
                                    continue;
                                }
                            } else {
                                ItemStack clone = item.clone();
                                if (item.getAmount() >= 8) {
                                    clone.setAmount(8);
                                    item.setAmount(item.getAmount() - 8);
                                } else {
                                    clone.setAmount(item.getAmount());
                                    item.setAmount(0);
                                }

                                itemsInTransfer.put(to, clone);
                                break;
                            }
                        }
                    }
                }
            }
        }, 0, 10);*/

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            itemsMoved.clear();

            for (String uuid : getConfig().getConfigurationSection("data").getKeys(false)) {
                try {
                    if (!uuid.equalsIgnoreCase("UUID")) {
                        Block from;
                        Block pipe;
                        Directional dir;
                        Block target;

                        try {
                            from = decodeLocation(getConfig().getString("data." + uuid + ".take-from")).getBlock();
                            pipe = decodeLocation(getConfig().getString("data." + uuid + ".to")).getBlock();
                            dir = (Directional) pipe.getBlockData();
                            target = pipe.getRelative(dir.getFacing().getOppositeFace());
                        } catch (Exception ex1) {
                            getConfig().set("data." + uuid, null);
                            saveConfig();
                            return;
                        }


                        if (from.getType().equals(Material.CHEST) || from.getType().equals(Material.HOPPER) || from.getType().equals(Material.TRAPPED_CHEST)) {
                            Container fromContainer = (Container) from.getState();
                            Inventory fromInventory = fromContainer.getInventory();

                            ItemStack itemMoving = null;
                            ItemStack old = null;
                            int slot = -1;
                            for (ItemStack item : fromContainer.getInventory().getStorageContents()) {
                                slot++;
                                if (item == null) continue;
                                if (item.getType() == Material.AIR) continue;
                                if (fromInventory.getName().contains(" Mine") && slot == 13) continue;

                                itemMoving = item.clone();
                                old = item.clone();
                                break;
                            }

                            if (itemMoving == null) continue;

                            if (itemsMoved.containsKey(from.getLocation()) && itemsMoved.get(from.getLocation()).equals(itemMoving)) {
                                continue;
                            }

                            if (fromInventory.getName().contains(" Void Chest (") && fromInventory.getName().contains("§8")) {
                                // It's taking from a void chest
                                Chest actualChest = (Chest) fromContainer;
                                Inventory inventory = actualChest.getInventory();
                                if (inventory.getName().contains("§c-")) {
                                    return;
                                }

                                if (target.getType().equals(Material.AIR)) {
                                    InfiniteChests instance = InfiniteChests.instance;

                                    String typeName = ChatColor.stripColor(inventory.getName().split(" Void Chest")[0]).toUpperCase().replace(" ", "_");
                                    Material type = Material.valueOf(typeName);

                                    Integer amountStored = Integer.parseInt(inventory.getName().split("\\(")[1].split("\\)")[0]);
                                    itemMoving = new ItemStack(type);
                                    if (amountStored >= 8) {
                                        itemMoving.setAmount(8);
                                        amountStored -= 8;
                                    } else if (amountStored > 0) {
                                        itemMoving.setAmount(amountStored);
                                        amountStored = 0;
                                    } else {
                                        continue;
                                    }

                                    actualChest.setCustomName(actualChest.getCustomName().split("\\(")[0] + "(" + amountStored + ")");
                                    actualChest.update();

                                    Hologram hologram = instance.holograms.get(actualChest.getLocation());
                                    TextLine line;
                                    if (hologram == null) {
                                        hologram = HologramsAPI.createHologram(instance, actualChest.getLocation().add(0.5f, 1.2f, 0.5f));
                                        instance.holograms.put(actualChest.getLocation(), hologram);
                                        line = hologram.appendTextLine("Void Chest");
                                    } else {
                                        line = (TextLine) hologram.getLine(0);
                                    }

                                    line.setText(instance.getFormattedChestLabel(actualChest.getLocation(), amountStored));
                                    playItemTransferAnimation(dir, pipe, itemMoving.clone());

                                    final ItemStack itemMovingClone = itemMoving.clone();

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                                        Item drop = target.getLocation().getWorld().dropItem(target.getLocation().clone().add(0.5f, 0, 0.5f), itemMovingClone);
                                        drop.setVelocity(new Vector(0, 0, 0));

                                        if (dir.getFacing().equals(BlockFace.DOWN)) {
                                            drop.setVelocity(new Vector(Math.random() * 0.1f, .3, Math.random() * 0.1f));
                                        } else if (dir.getFacing().equals(BlockFace.UP)) {
                                            drop.setVelocity(new Vector(0, -.5, 0));
                                        } else if (dir.getFacing().equals(BlockFace.EAST)) {
                                            drop.setVelocity(new Vector(-.1, 0, 0));
                                        } else if (dir.getFacing().equals(BlockFace.WEST)) {
                                            drop.setVelocity(new Vector(.1, 0, 0));
                                        } else if (dir.getFacing().equals(BlockFace.NORTH)) {
                                            drop.setVelocity(new Vector(0, 0, .1));
                                        } else if (dir.getFacing().equals(BlockFace.SOUTH)) {
                                            drop.setVelocity(new Vector(0, 0, -.1));
                                        }
                                    }, 20);
                                } else if (target.getState() instanceof Container) {
                                    Container container = (Container) target.getState();
                                    if (container.getInventory().firstEmpty() == -1) continue;
                                    if (!container.getInventory().getName().contains(" Void Chest (") && !container.getInventory().getName().contains(" Mine")) {
                                        InfiniteChests instance = InfiniteChests.instance;

                                        String typeName = ChatColor.stripColor(inventory.getName().split(" Void Chest")[0]).toUpperCase().replace(" ", "_");
                                        Material type = Material.valueOf(typeName);

                                        Integer amountStored = Integer.parseInt(inventory.getName().split("\\(")[1].split("\\)")[0]);
                                        itemMoving = new ItemStack(type);
                                        if (amountStored >= 8) {
                                            itemMoving.setAmount(8);
                                            amountStored -= 8;
                                        } else if (amountStored > 0) {
                                            itemMoving.setAmount(amountStored);
                                            amountStored = 0;
                                        } else {
                                            continue;
                                        }

                                        actualChest.setCustomName(actualChest.getCustomName().split("\\(")[0] + "(" + amountStored + ")");
                                        actualChest.update();

                                        Hologram hologram = instance.holograms.get(actualChest.getLocation());
                                        TextLine line;
                                        if (hologram == null) {
                                            hologram = HologramsAPI.createHologram(instance, actualChest.getLocation().add(0.5f, 1.2f, 0.5f));
                                            instance.holograms.put(actualChest.getLocation(), hologram);
                                            line = hologram.appendTextLine("Void Chest");
                                        } else {
                                            line = (TextLine) hologram.getLine(0);
                                        }

                                        line.setText(instance.getFormattedChestLabel(actualChest.getLocation(), amountStored));
                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());

                                        container.getInventory().addItem(itemMoving.clone());
                                    }
                                }
                            } else {
                                Mine mine = null;
                                if (fromInventory.getName().contains(" Mine")) {
                                    if (itemMoving.getType() != Material.GOLD_INGOT && itemMoving.getType() != Material.DIAMOND && itemMoving.getType() != Material.EMERALD && itemMoving.getType() != Material.IRON_INGOT && itemMoving.getType() != Material.REDSTONE  && itemMoving.getType() != Material.COAL && itemMoving.getType() != Material.LAPIS_LAZULI) continue;
                                    Location loc = fromInventory.getLocation();

                                    for (Mine testMine : TribeMines.instance.mines) {
                                        if (testMine.getChest().getLocation().equals(loc)) {
                                            mine = testMine;
                                            break;
                                        }
                                    }

                                    if (mine == null) continue;
                                }

                                // It's taking from a regular chest
                                if (target.getType().equals(Material.AIR)) {
                                    itemMoving = itemMoving.clone();

                                    if (old.getAmount() >= 8) {
                                        itemMoving.setAmount(8);
                                        old.setAmount(old.getAmount() - 8);

                                        fromInventory.setItem(slot, old);
                                    } else {
                                        fromInventory.setItem(slot, null);
                                    }

                                    if (mine != null) {
                                        mine.withdrawGold(itemMoving.getAmount());
                                    }

                                    playItemTransferAnimation(dir,
                                            pipe,
                                            itemMoving.clone());

                                    final ItemStack itemMovingClone = itemMoving.clone();

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                                        Item drop = target.getLocation().getWorld().dropItem(target.getLocation().clone().add(0.5f, 0, 0.5f), itemMovingClone);
                                        drop.setVelocity(new Vector(0, 0, 0));

                                        if (dir.getFacing().equals(BlockFace.DOWN)) {
                                            drop.setVelocity(new Vector(Math.random() * 0.1f, .3, Math.random() * 0.1f));
                                        } else if (dir.getFacing().equals(BlockFace.UP)) {
                                            drop.setVelocity(new Vector(0, -.5, 0));
                                        } else if (dir.getFacing().equals(BlockFace.EAST)) {
                                            drop.setVelocity(new Vector(-.1, 0, 0));
                                        } else if (dir.getFacing().equals(BlockFace.WEST)) {
                                            drop.setVelocity(new Vector(.1, 0, 0));
                                        } else if (dir.getFacing().equals(BlockFace.NORTH)) {
                                            drop.setVelocity(new Vector(0, 0, .1));
                                        } else if (dir.getFacing().equals(BlockFace.SOUTH)) {
                                            drop.setVelocity(new Vector(0, 0, -.1));
                                        }
                                    }, 20);
                                } else if (target.getType().equals(Material.CHEST) || target.getType().equals(Material.HOPPER) || target.getType().equals(Material.TRAPPED_CHEST)) {
                                    Container targetContainer = (Container) target.getState();
                                    Inventory targetInventory = targetContainer.getInventory();
                                    if (targetInventory.getName().contains(" Void Chest (")) {
                                        Chest actualChest = (Chest) targetContainer;

                                        ItemStack movingClone = itemMoving.clone();
                                        if (movingClone.getAmount() >= 8) {
                                            movingClone.setAmount(8);
                                        }

                                        if (mine != null) {
                                            mine.withdrawGold(itemMoving.getAmount());
                                        }

                                        ItemStack movingCloneClone = movingClone.clone();

                                        InventoryMoveItemEvent e = new InventoryMoveItemEvent(actualChest.getInventory(), movingClone, actualChest.getInventory(), true);

                                        int limit = InfiniteChests.instance.getLimit(actualChest.getLocation());
                                        int amountStored = Integer.parseInt(actualChest.getCustomName().split("\\(")[1].split("\\)")[0]);

                                        if (amountStored + movingClone.getAmount() > limit) continue;

                                        getServer().getPluginManager().callEvent(e);

                                        itemMoving.setAmount(itemMoving.getAmount() - 8);
                                        fromInventory.setItem(slot, itemMoving);
                                        itemsMoved.put(actualChest.getLocation(), movingClone);

                                        playItemTransferAnimation(dir, pipe, movingCloneClone.clone());

                                        continue;
                                    } else if (!targetContainer.getInventory().getName().contains(" Mine")) {
                                        if (targetInventory.firstEmpty() >= 0) {
                                            if (old.getAmount() >= 8) {
                                                itemMoving.setAmount(8);
                                                old.setAmount(old.getAmount() - 8);

                                                fromInventory.setItem(slot, old);
                                                targetInventory.addItem(itemMoving);
                                            } else {
                                                fromInventory.setItem(slot, null);
                                                targetInventory.addItem(itemMoving);
                                            }

                                            if (mine != null) {
                                                mine.withdrawGold(itemMoving.getAmount());
                                            }

                                            itemsMoved.put(target.getLocation(), itemMoving);

                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                        }
                                    }
                                } else if (target.getType().equals(Material.FURNACE)) {
                                    Furnace targetContainer = (Furnace) target.getState();
                                    FurnaceInventory targetInventory = targetContainer.getInventory();

                                    if (itemMoving.getType().name().contains("COAL") || itemMoving.getType().name().contains("WOOD") || itemMoving.getType().name().contains("PLANKS")) {
                                        ItemStack fuel = targetInventory.getFuel();
                                        if (fuel == null) {
                                            if (itemMoving.getAmount() >= 8) {
                                                itemMoving.setAmount(8);
                                                old.setAmount(old.getAmount() - 8);

                                                fromInventory.setItem(slot, old);
                                                targetInventory.setFuel(itemMoving);
                                                playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                            } else {
                                                fromInventory.setItem(slot, null);
                                                targetInventory.setFuel(itemMoving);
                                                playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                            }

                                            if (mine != null) {
                                                mine.withdrawGold(itemMoving.getAmount());
                                            }
                                        } else {
                                            if (fuel.getType().equals(itemMoving.getType())) {
                                                if (fuel.getAmount() < fuel.getType().getMaxStackSize()) {
                                                    if (fuel.getAmount() + 8 <= fuel.getType().getMaxStackSize()) {
                                                        if (itemMoving.getAmount() >= 8) {
                                                            itemMoving.setAmount(fuel.getAmount() + 8);
                                                            old.setAmount(old.getAmount() - 8);

                                                            fromInventory.setItem(slot, old);
                                                            targetInventory.setFuel(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                            fromInventory.setItem(slot, null);
                                                            itemMoving.setAmount(fuel.getAmount() + itemMoving.getAmount());
                                                            targetInventory.setFuel(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());

                                                            if (mine != null) {
                                                                mine.withdrawGold(itemMoving.getAmount());
                                                            }
                                                        }
                                                    } else {
                                                        int required = fuel.getType().getMaxStackSize() - fuel.getAmount();
                                                        if (required > 8) required = 8;

                                                        if (itemMoving.getAmount() >= required) {
                                                            itemMoving.setAmount(fuel.getAmount() + required);
                                                            old.setAmount(old.getAmount() - required);

                                                            fromInventory.setItem(slot, old);
                                                            targetInventory.setFuel(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());

                                                            if (mine != null) {
                                                                mine.withdrawGold(itemMoving.getAmount());
                                                            }
                                                        } else {
                                                            fromInventory.setItem(slot, null);
                                                            itemMoving.setAmount(fuel.getAmount() + itemMoving.getAmount());
                                                            targetInventory.setFuel(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());

                                                            if (mine != null) {
                                                                mine.withdrawGold(itemMoving.getAmount());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        ItemStack smelting = targetInventory.getSmelting();
                                        if (smelting == null) {
                                            if (itemMoving.getAmount() >= 8) {
                                                itemMoving.setAmount(8);
                                                old.setAmount(old.getAmount() - 8);

                                                fromInventory.setItem(slot, old);
                                                targetInventory.addItem(itemMoving);
                                                playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                            } else {
                                                fromInventory.setItem(slot, null);
                                                targetInventory.addItem(itemMoving);
                                                playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                            }
                                        } else {
                                            if (smelting.getType().equals(itemMoving.getType())) {
                                                if (smelting.getAmount() < smelting.getType().getMaxStackSize()) {
                                                    if (smelting.getAmount() + 8 <= smelting.getType().getMaxStackSize()) {
                                                        if (itemMoving.getAmount() >= 8) {
                                                            itemMoving.setAmount(8);
                                                            old.setAmount(old.getAmount() - 8);

                                                            fromInventory.setItem(slot, old);
                                                            targetInventory.addItem(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                        } else {
                                                            fromInventory.setItem(slot, null);
                                                            targetInventory.addItem(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                        }
                                                    } else {
                                                        int required = smelting.getType().getMaxStackSize() - smelting.getAmount();
                                                        if (required > 8) required = 8;
                                                        if (itemMoving.getAmount() >= required) {
                                                            itemMoving.setAmount(required);
                                                            old.setAmount(old.getAmount() - required);

                                                            fromInventory.setItem(slot, old);
                                                            targetInventory.addItem(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                        } else {
                                                            fromInventory.setItem(slot, null);
                                                            targetInventory.addItem(itemMoving);
                                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (from.getType().equals(Material.FURNACE)) {
                            Furnace fromContainer = (Furnace) from.getState();
                            FurnaceInventory fromInventory = fromContainer.getInventory();

                            ItemStack itemMoving = fromInventory.getResult();
                            ItemStack old = itemMoving.clone();

                            if (itemMoving == null) continue;

                            if (itemsMoved.containsKey(from.getLocation()) && itemsMoved.get(from.getLocation()).equals(itemMoving)) {
                                continue;
                            }

                            // It's taking from a regular chest
                            if (target.getType().equals(Material.AIR)) {
                                if (old.getAmount() >= 8) {
                                    itemMoving.setAmount(8);
                                    old.setAmount(old.getAmount() - 8);

                                    fromInventory.setResult(old);
                                } else {
                                    fromInventory.setResult(null);
                                }

                                playItemTransferAnimation(dir, pipe, itemMoving.clone());

                                final ItemStack itemMovingClone = itemMoving.clone();

                                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                                    Item drop = target.getLocation().getWorld().dropItem(target.getLocation().clone().add(0.5f, 0, 0.5f), itemMovingClone);
                                    drop.setVelocity(new Vector(0, 0, 0));

                                    if (dir.getFacing().equals(BlockFace.DOWN)) {
                                        drop.setVelocity(new Vector(Math.random() * 0.1f, .3, Math.random() * 0.1f));
                                    } else if (dir.getFacing().equals(BlockFace.UP)) {
                                        drop.setVelocity(new Vector(0, -.5, 0));
                                    } else if (dir.getFacing().equals(BlockFace.EAST)) {
                                        drop.setVelocity(new Vector(-.1, 0, 0));
                                    } else if (dir.getFacing().equals(BlockFace.WEST)) {
                                        drop.setVelocity(new Vector(.1, 0, 0));
                                    } else if (dir.getFacing().equals(BlockFace.NORTH)) {
                                        drop.setVelocity(new Vector(0, 0, .1));
                                    } else if (dir.getFacing().equals(BlockFace.SOUTH)) {
                                        drop.setVelocity(new Vector(0, 0, -.1));
                                    }
                                }, 20);
                            } else if (target.getType().equals(Material.CHEST) || target.getType().equals(Material.HOPPER) || target.getType().equals(Material.TRAPPED_CHEST)) {
                                Container targetContainer = (Container) target.getState();
                                Inventory targetInventory = targetContainer.getInventory();
                                if (!targetContainer.getInventory().getName().contains(" Void Chest (") && !targetContainer.getInventory().getName().contains(" Mine")) {
                                    if (targetInventory.firstEmpty() >= 0) {
                                        if (old.getAmount() >= 8) {
                                            itemMoving.setAmount(8);
                                            old.setAmount(old.getAmount() - 8);

                                            fromInventory.setResult(old);
                                            targetInventory.addItem(itemMoving);
                                        } else {
                                            fromInventory.setResult(null);
                                            targetInventory.addItem(itemMoving);
                                        }

                                        itemsMoved.put(target.getLocation(), itemMoving);

                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                    }
                                }
                            } else if (target.getType().equals(Material.FURNACE)) {
                                Furnace targetContainer = (Furnace) target.getState();
                                FurnaceInventory targetInventory = targetContainer.getInventory();

                                if (itemMoving.getType().name().contains("COAL") || itemMoving.getType().name().contains("WOOD") || itemMoving.getType().name().contains("PLANKS")) {
                                    ItemStack fuel = targetInventory.getFuel();
                                    if (fuel == null) {
                                        if (itemMoving.getAmount() >= 8) {
                                            itemMoving.setAmount(8);
                                            old.setAmount(old.getAmount() - 8);

                                            fromInventory.setResult(old);
                                            targetInventory.setFuel(itemMoving);
                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                        } else {
                                            fromInventory.setResult(null);
                                            targetInventory.setFuel(itemMoving);
                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                        }
                                    } else {
                                        if (fuel.getType().equals(itemMoving.getType())) {
                                            if (fuel.getAmount() < fuel.getType().getMaxStackSize()) {
                                                if (fuel.getAmount() + 8 <= fuel.getType().getMaxStackSize()) {
                                                    if (itemMoving.getAmount() >= 8) {
                                                        itemMoving.setAmount(fuel.getAmount() + 8);
                                                        old.setAmount(old.getAmount() - 8);

                                                        fromInventory.setResult(old);
                                                        targetInventory.setFuel(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    } else {
                                                        fromInventory.setResult(null);
                                                        itemMoving.setAmount(fuel.getAmount() + itemMoving.getAmount());
                                                        targetInventory.setFuel(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    }
                                                } else {
                                                    int required = fuel.getType().getMaxStackSize() - fuel.getAmount();
                                                    if (required > 8) required = 8;

                                                    if (itemMoving.getAmount() >= required) {
                                                        itemMoving.setAmount(fuel.getAmount() + required);
                                                        old.setAmount(old.getAmount() - required);

                                                        fromInventory.setResult(old);
                                                        targetInventory.setFuel(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    } else {
                                                        fromInventory.setResult(null);
                                                        itemMoving.setAmount(fuel.getAmount() + itemMoving.getAmount());
                                                        targetInventory.setFuel(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    ItemStack smelting = targetInventory.getSmelting();
                                    if (smelting == null) {
                                        if (itemMoving.getAmount() >= 8) {
                                            itemMoving.setAmount(8);
                                            old.setAmount(old.getAmount() - 8);

                                            fromInventory.setResult(old);
                                            targetInventory.addItem(itemMoving);
                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                        } else {
                                            fromInventory.setResult(null);
                                            targetInventory.addItem(itemMoving);
                                            playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                        }
                                    } else {
                                        if (smelting.getType().equals(itemMoving.getType())) {
                                            if (smelting.getAmount() < smelting.getType().getMaxStackSize()) {
                                                if (smelting.getAmount() + 8 <= smelting.getType().getMaxStackSize()) {
                                                    if (itemMoving.getAmount() >= 8) {
                                                        itemMoving.setAmount(8);
                                                        old.setAmount(old.getAmount() - 8);

                                                        fromInventory.setResult(old);
                                                        targetInventory.addItem(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    } else {
                                                        fromInventory.setResult(null);
                                                        targetInventory.addItem(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    }
                                                } else {
                                                    int required = smelting.getType().getMaxStackSize() - smelting.getAmount();
                                                    if (required > 8) required = 8;
                                                    if (itemMoving.getAmount() >= required) {
                                                        itemMoving.setAmount(required);
                                                        old.setAmount(old.getAmount() - required);

                                                        fromInventory.setResult(old);
                                                        targetInventory.addItem(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    } else {
                                                        fromInventory.setResult(null);
                                                        targetInventory.addItem(itemMoving);
                                                        playItemTransferAnimation(dir, pipe, itemMoving.clone());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {

                }
            }
        }, 0, 10);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType().equals(Material.END_ROD)) {
            try {
                if (e.getItemInHand().getItemMeta().getDisplayName().contains("§dTransport Pipe")) {
                    if (e.getBlockAgainst().getType() != Material.CHEST && e.getBlockAgainst().getType() != Material.HOPPER && e.getBlockAgainst().getType() != Material.FURNACE) {
                        e.getPlayer().sendMessage("§cYou can not place this here.");
                        e.setCancelled(true);
                    } else {
                        Directional directionalData = (Directional) e.getBlock().getBlockData();
                        Block block = e.getBlock().getRelative(directionalData.getFacing());

                        if (block.getType().equals(Material.AIR)) {
                            UUID id = UUID.randomUUID();
                            block.setType(Material.END_ROD);

                            Directional secondDirectionalData = (Directional) block.getBlockData();
                            secondDirectionalData.setFacing(directionalData.getFacing().getOppositeFace());
                            block.setBlockData(secondDirectionalData);
                            e.getPlayer().playSound(e.getBlock().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 1);

                            getConfig().set("data." + id.toString() + ".take-from", encodeLocation(e.getBlockAgainst().getLocation()));
                            getConfig().set("data." + id.toString() + ".through", encodeLocation(e.getBlock().getLocation()));
                            getConfig().set("data." + id.toString() + ".to", encodeLocation(block.getLocation()));
                            saveConfig();

                            e.getPlayer().sendMessage("§ePlaced a new pipe with as input a" + (e.getBlockAgainst().getType().equals(Material.END_ROD) ? "nother pipe" : " " + e.getBlockAgainst().getType().toString().toLowerCase()) + ".");
                        } else {
                            e.setCancelled(true);
                            e.getPlayer().sendMessage("§cThere is no room for that pipe here.");
                        }
                    }
                }
            } catch (Exception ex) {

            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent e) {
        if (e.getBlockClicked().getRelative(e.getBlockFace()).getType().equals(Material.END_ROD)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (e.getToBlock().getType().equals(Material.END_ROD)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType().equals(Material.END_ROD)) {
            Directional directionalData = (Directional) e.getBlock().getBlockData();
            Block block = e.getBlock().getRelative(directionalData.getFacing());

            if (block.getType().equals(Material.END_ROD)) {
                UUID id = isPipe(block.getLocation());
                if (id != null) {
                    e.setCancelled(true);
                    e.getBlock().setType(Material.AIR);
                    block.setType(Material.AIR);
                    getConfig().set("data." + id.toString(), null);
                    saveConfig();

                    ItemStack pipeItem = new ItemStack(Material.END_ROD);
                    ItemMeta pipeMeta = pipeItem.getItemMeta();
                    pipeMeta.setDisplayName("§dTransport Pipe");
                    pipeItem.setItemMeta(pipeMeta);

                    if (e.getPlayer().getInventory().firstEmpty() != -1) {
                        e.getPlayer().getInventory().addItem(pipeItem);
                    } else {
                        e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), pipeItem);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        try {
            if (e.getEntity().getType().equals(EntityType.DROPPED_ITEM)) {
                Item item = (Item) e.getEntity();
                if (item.getItemStack().getItemMeta().getDisplayName().toLowerCase().contains("pipe")) {
                    e.setCancelled(true);
                }
            }
        } catch (Exception ex) {

        }
    }

    public UUID isPipe(Location loc) {
        String encodedLoc = encodeLocation(loc);
        for (String uuid : getConfig().getConfigurationSection("data").getKeys(false)) {
            if (!uuid.equalsIgnoreCase("UUID")) {
                if (getConfig().getString("data." + uuid + ".through").equalsIgnoreCase(encodedLoc)) {
                    return UUID.fromString(uuid);
                } else if (getConfig().getString("data." + uuid + ".to").equalsIgnoreCase(encodedLoc)) {
                    return UUID.fromString(uuid);
                }
            }
        }
        return null;
    }

    String encodeLocation(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    Location decodeLocation(String string) {
        String[] split = string.split("\\|");
        return new Location(Bukkit.getWorld(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


}
