package com.stausssi.AutoRegion;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class Events implements Listener {
    public static AutoRegion plugin;

    public Events(AutoRegion main) {
        plugin = main;
    }

    public static WorldEditPlugin getWorldEdit() {
        Plugin we = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        return we != null && we instanceof WorldEditPlugin ? (WorldEditPlugin)we : null;
    }

    public static WorldGuardPlugin getWorldGuard() {
        Plugin wg = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        return wg != null && wg instanceof WorldGuardPlugin ? (WorldGuardPlugin)wg : null;
    }

    @EventHandler
    public static void onBlockPlaced(BlockPlaceEvent e) {
        Block block = e.getBlockPlaced();
        Material m = block.getType();
        String blockName = m.name();
        Player p = e.getPlayer();
        if (plugin.getBlockConfig().get("Blocks." + blockName) != null) {
            int id = p.getInventory().getHeldItemSlot();
            ItemStack item = new ItemStack(p.getInventory().getItem(id));
            ItemMeta meta = item.getItemMeta();
            if(meta != null) {
            	if (meta.getLore().equals(plugin.getLore())) {
                    int x = block.getX();
                    int z = block.getZ();
                    
                    int radius = plugin.getBlockConfig().getInt("Blocks." + blockName + ".radius");
                    
                    BlockVector pos1 = new BlockVector(x + radius, 0, z + radius);
                    BlockVector pos2 = new BlockVector(x - radius, block.getWorld().getMaxHeight(), z - radius);
                    
                    RegionManager rMan = getWorldGuard().getRegionManager(block.getWorld());
                    ProtectedCuboidRegion pReg;
                    
                    if (rMan.getRegion(p.getName()) != null) {
                        int i = plugin.getConfig().getInt("regions." + p.getName()) + 1;
                        pReg = new ProtectedCuboidRegion(p.getName() + i, pos1, pos2);
                    } else {
                        pReg = new ProtectedCuboidRegion(p.getName(), pos1, pos2);
                    }

                    Map<String, ProtectedRegion> regionList = rMan.getRegions();
                    List<ProtectedRegion> regions = new ArrayList<ProtectedRegion>();
                    Iterator<String> regionIterator = regionList.keySet().iterator();

                    while(regionIterator.hasNext()) {
                        String key = (String)regionIterator.next();
                        ProtectedRegion pr = (ProtectedRegion)regionList.get(key);
                        if (pr.getType() != RegionType.GLOBAL) {
                            regions.add(pr);
                        }
                    }

                    if (pReg.getIntersectingRegions(regions).isEmpty()) {
                        DefaultDomain d = new DefaultDomain();
                        d.addPlayer(p.getName());
                        d.addPlayer(p.getUniqueId());
                        rMan.addRegion(pReg);
                        pReg.setOwners(d);
                        p.sendMessage(plugin.ColorMessage(plugin.getConfig().getString("messages.regionCreated").replaceAll("%OWNER%", p.getName())));
                        e.getBlockPlaced().setType(Material.AIR);
                        plugin.getConfig().set("regions." + p.getName(), plugin.getConfig().getInt("regions." + p.getName()) + 1);
                        plugin.saveConfig();
                    } else {
                        p.sendMessage(plugin.ColorMessage(plugin.getConfig().getString("messages.regionIntersecting")));
                        e.setCancelled(true);
                    }
                }
            }
            
        }

    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPlayedBefore() && plugin.getConfig().getBoolean("blockOnFirstJoin")) {
            String block = plugin.getConfig().getString("block").toUpperCase();
            Material m = Material.getMaterial(block);
            ItemStack stack = new ItemStack(m);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(plugin.getItemName());
            meta.setLore(plugin.getLore());
            stack.setItemMeta(meta);
            p.getInventory().addItem(new ItemStack[]{stack});
        }

    }
}

