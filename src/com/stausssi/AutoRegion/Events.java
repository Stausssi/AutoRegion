package com.stausssi.AutoRegion;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import com.sk89q.worldguard.protection.util.DomainInputResolver;
import com.sk89q.worldguard.protection.util.DomainInputResolver.UserLocatorPolicy;
import com.sk89q.worldguard.util.profile.resolver.ProfileService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Events implements Listener {
    public static AutoRegion plugin;
    	
    // Get config identifiers
    public static String messagesIdentifier = AutoRegion.messagesIdentifier;
    
    public static String groupIdentifier = AutoRegion.groupIdentifier;
    
    public static String playerIdentifier = AutoRegion.playerIdentifier;
    public static String nameIdentifier = AutoRegion.nameIdentifier;
    public static String regionsIdentifier = AutoRegion.regionsIdentifier;
    public static String regionCountIdentifier = AutoRegion.regionCountIdentifier;
    public static String regionCreatorsReceivedIdentifier = AutoRegion.regionCreatorsReceivedIdentifier;
    
    public static String blocksIdentifier = AutoRegion.blocksIdentifier;
    public static String radiusIdentifier = AutoRegion.radiusIdentifier;
    
    
    public Events(AutoRegion main) {
        plugin = main;
    }

    @EventHandler
    public static void onBlockPlaced(BlockPlaceEvent e) {
        Block block = e.getBlockPlaced();
        Material m = block.getType();
        String blockName = m.name();
        Player p = e.getPlayer();
        UUID playerUUID = p.getUniqueId();
        
        // Check whether block has been defined in the config
        if (getMain().getBlockConfig().get(blocksIdentifier + "." + blockName) != null) {
            ItemStack item = new ItemStack(p.getInventory().getItemInMainHand());
            
            // Check whether item has a meta and is therefore maybe a RegionCreator
            if(item.hasItemMeta()) {
            	ItemMeta meta = item.getItemMeta();
            	
            	// Check whether item is a RegionCreator
            	if (meta.getLore().equals(getMain().getLore())) {
            		
            		int playerRegionCount, maxRegionsGroup;
            		
            		String group = "default";
            		
            		// Get the group
            		for(String s : getMain().getConfig().getConfigurationSection(groupIdentifier).getKeys(false)) {            			
            			if(p.hasPermission("autoregion.regions." + s)) {
            				group = s;
            				break;
            			}
            		}
            		
            		// Get max Regions
            		maxRegionsGroup = getMain().getConfig().getInt(groupIdentifier + "." + group);           		
            		
            		// Get player regions
            		playerRegionCount = getMain().getRegionCount(playerUUID);
            		
            		/* Check whether player has max regions already
            		 * -1 equals unlimited regions
            		 * OPs shouldn't be restricted
            		 */
            		if(playerRegionCount < maxRegionsGroup || maxRegionsGroup == -1 || p.isOp() || p.hasPermission("autoregion.regions.*")) {
            			
            			// Get coordinates
                        int x = block.getX();
                        int z = block.getZ();
                        
                        // Get radius
                        int radius = getMain().getRadius(blockName);
                        
                        // Create BlockVectors
                        BlockVector3 pos1 = BlockVector3.at(x + radius, 0, z + radius);
                        BlockVector3 pos2 = BlockVector3.at(x - radius, block.getWorld().getMaxHeight(), z - radius);
                        
    					RegionManager rMan = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(block.getWorld()));
                        ProtectedCuboidRegion pReg;
                        
                        /* Check whether the player already has a region
                         * If so, add a count next to the regions name
                         * Create RegionInstance
                         */ 
                        if (rMan.getRegion(p.getName()) != null) {
                            int i = playerRegionCount;
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
                        	DefaultDomain d = new DefaultDomain();
                            
                            // Convert name to WorldEdit UUID, 
                            // src: https://worldguard.readthedocs.io/en/latest/developer/regions/protected-region/
                            ListeningExecutorService executor =
                                    MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

                            String[] input = new String[] { p.getName() };
                            ProfileService profiles = WorldGuard.getInstance().getProfileService();
                            DomainInputResolver resolver = new DomainInputResolver(profiles, input);
                            resolver.setLocatorPolicy(UserLocatorPolicy.UUID_ONLY);
                            ListenableFuture<DefaultDomain> future = executor.submit(resolver);

                            // Add a callback using Guava
                            Futures.addCallback(future, new FutureCallback<DefaultDomain>() {

								@Override
								public void onFailure(Throwable throwable) {
									d.addPlayer(p.getName());
		                            d.addPlayer(playerUUID);
		                            pReg.setOwners(d);
								}

								@Override
								public void onSuccess(DefaultDomain result) {
									pReg.setOwners(result);
								}
                            	
                            });
                            
                            rMan.addRegion(pReg);
                            p.sendMessage(getMain().ColorMessage(getMain().getConfig().getString(messagesIdentifier + "." + "regionCreated").replaceAll("%OWNER%", p.getName())));
                            e.getBlockPlaced().setType(Material.AIR);
                            getMain().getConfig().set(playerIdentifier + "." + playerUUID + "." + regionsIdentifier + "." + regionCountIdentifier,  playerRegionCount + 1);
                            getMain().getConfig().set(playerIdentifier + "." + playerUUID + "." + nameIdentifier, p.getName());
                            getMain().saveConfig();
                        } else {
                            p.sendMessage(getMain().ColorMessage(getMain().getConfig().getString(messagesIdentifier + "." + "regionIntersecting")));
                            e.setCancelled(true);
                        }
            		} else {
            			p.sendMessage(getMain().ColorMessage(getMain().getConfig().getString(messagesIdentifier + "." + "maxRegionsReached").replaceAll("%REGIONS%", Integer.toString(maxRegionsGroup)).replaceAll("%GROUP%", group)));
            			e.setCancelled(true);
            			return;
            		}
            	}	
            }
        }
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID UUID = p.getUniqueId();
        if (!p.hasPlayedBefore() && getMain().getConfig().getBoolean("blockOnFirstJoin")) {
            String block = getMain().getConfig().getString("block").toUpperCase();
            
            int diameter = getMain().getDiameter(block);
            String itemName = getMain().getItemName(diameter);
            
            Material m = Material.getMaterial(block);
            ItemStack stack = new ItemStack(m);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(itemName);
            meta.setLore(getMain().getLore());
            stack.setItemMeta(meta);
            p.getInventory().addItem(new ItemStack[]{stack});
            getMain().getConfig().set(playerIdentifier + "." + UUID + "." + regionsIdentifier + "." + regionCreatorsReceivedIdentifier, getMain().getRegionCreatorsReceived(UUID) + 1);
            p.sendMessage(getMain().ColorMessage(getMain().getConfig().getString(messagesIdentifier + "." + "playerWelcomeMessage").replaceAll("%DIAMETER%", Integer.toString(diameter))));
        }

    }
    
    private static AutoRegion getMain() {
    	return plugin;
    }
}

