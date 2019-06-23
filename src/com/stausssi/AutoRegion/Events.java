package com.stausssi.AutoRegion;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
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

    // Get WorldEdit 
    public static WorldEditPlugin getWorldEdit() {
        Plugin we = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        return we != null && we instanceof WorldEditPlugin ? (WorldEditPlugin)we : null;
    }

    // Get WorldGuard
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
        
        // Check whether block has been defined in the config
        if (plugin.getBlockConfig().get("Blocks." + blockName) != null) {
            ItemStack item = new ItemStack(p.getInventory().getItemInMainHand());
            
            // Check whether item has a meta and is therefore maybe a RegionCreator
            if(item.hasItemMeta()) {
            	ItemMeta meta = item.getItemMeta();
            	
            	// Check whether item is a RegionCreator
            	if (meta.getLore().equals(plugin.getLore())) {
            		// Get coordinates
                    int x = block.getX();
                    int z = block.getZ();
                    
                    // Get radius
                    int radius = plugin.getBlockConfig().getInt("Blocks." + blockName + ".radius");
                    
                    // Create BlockVectors
                    BlockVector3 pos1 = BlockVector3.at(x + radius, 0, z + radius);
                    BlockVector3 pos2 = BlockVector3.at(x - radius, block.getWorld().getMaxHeight(), z - radius);
                    
					RegionManager rMan = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(block.getWorld()));
                    ProtectedCuboidRegion pReg;
                    
                    // Check whether the player already has a region
                    // If so, add a count next to the regions name
                    // Create Region
                    if (rMan.getRegion(p.getName()) != null) {
                        int i = plugin.getConfig().getInt("regions." + p.getName()) + 1;
                        pReg = new ProtectedCuboidRegion(p.getName() + i, pos1, pos2);
                    } else {
                        pReg = new ProtectedCuboidRegion(p.getName(), pos1, pos2);
                    }

                    Map<String, ProtectedRegion> regionList = rMan.getRegions();
                    List<ProtectedRegion> regions = new ArrayList<ProtectedRegion>();
                    Iterator<String> regionIterator = regionList.keySet().iterator();
                    
                    // Get all regions
                    while(regionIterator.hasNext()) {
                        String key = (String)regionIterator.next();
                        ProtectedRegion pr = (ProtectedRegion)regionList.get(key);
                        if (pr.getType() != RegionType.GLOBAL) {
                            regions.add(pr);
                        }
                    }

                    // Check whether there are intersecting regions
                    if (pReg.getIntersectingRegions(regions).isEmpty()) {
                    	// Add Player as owner of the region
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

