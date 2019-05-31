package com.stausssi.AutoRegion;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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

public class EventsOLD implements Listener {
    public static AutoRegionOLD plugin;

    public EventsOLD(AutoRegionOLD main) {
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
        String bname = m.name();
        Player p = e.getPlayer();
        if (plugin.getBlockConfig().get("Blocks." + bname) != null) {
            int id = p.getInventory().getHeldItemSlot();
            ItemStack item = new ItemStack(p.getInventory().getItem(id));
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(plugin.getConfig().getString("lore"));
            if (meta.getLore().size() == lore.size()) {
                int x = block.getX();
                int z = block.getZ();
                int radius = plugin.getBlockConfig().getInt("Blocks." + bname + ".radius");
                BlockVector pos1 = new BlockVector(x + radius, 0, z + radius);
                BlockVector pos2 = new BlockVector(x - radius, block.getWorld().getMaxHeight(), z - radius);
                RegionManager rman = getWorldGuard().getRegionManager(block.getWorld());
                ProtectedCuboidRegion preg;
                if (rman.getRegion(p.getName()) != null) {
                    int i = plugin.getConfig().getInt("regions." + p.getName()) + 1;
                    preg = new ProtectedCuboidRegion(p.getName() + i, pos1, pos2);
                } else {
                    preg = new ProtectedCuboidRegion(p.getName(), pos1, pos2);
                }

                Map<String, ProtectedRegion> regionList = rman.getRegions();
                List<ProtectedRegion> regions = Lists.newArrayList();
                Iterator<String> var19 = regionList.keySet().iterator();

                while(var19.hasNext()) {
                    String key = (String)var19.next();
                    ProtectedRegion pr = (ProtectedRegion)regionList.get(key);
                    if (pr.getId() != "__gloabl__") {
                        regions.add(pr);
                    }
                }

                if (preg.getIntersectingRegions(regions).isEmpty()) {
                    DefaultDomain d = new DefaultDomain();
                    d.addPlayer(p.getName());
                    d.addPlayer(p.getUniqueId());
                    rman.addRegion(preg);
                    preg.setOwners(d);
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

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPlayedBefore() && plugin.getConfig().getBoolean("blockOnFirstJoin")) {
            String block = plugin.getConfig().getString("block");
            Material m = Material.getMaterial(block);
            ItemStack stack = new ItemStack(m);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("§b" + block.toLowerCase().replace("_", " ") + " - Creates a region of " + (2 * plugin.getBlockConfig().getInt("Blocks." + block + ".radius") + 1) + "x" + (2 * plugin.getBlockConfig().getInt("Blocks." + block + ".radius") + 1));
            meta.setLore(plugin.lore);
            stack.setItemMeta(meta);
            p.getInventory().addItem(new ItemStack[]{stack});
        }

    }
}

