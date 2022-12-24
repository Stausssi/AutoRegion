package io.stausssi.plugins.autoregion;

import com.google.common.util.concurrent.*;
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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Executors;

public class Events implements Listener {
    public static AutoRegion plugin;

    private static final ConfigHandler configHandler = ConfigHandler.getInstance();
    private static final MessageHandler messageHandler = MessageHandler.getInstance();

    public Events(AutoRegion main) {
        plugin = main;
    }


    @EventHandler
    public static void onBlockPlaced(BlockPlaceEvent e) {
        // Get the placed Block
        Block block = e.getBlockPlaced();

        // Convert Block to Material
        Material m = block.getType();

        // Get the blocks name
        String blockIdentifier = BlockNameConverter.toIdentifier(m.name());

        // Get the player that triggered the Event
        Player p = e.getPlayer();
        UUID playerID = p.getUniqueId();

        // Get the held item
        ItemStack item = new ItemStack(p.getInventory().getItemInMainHand());

        // Check whether block has been defined in the config
        // Check whether item has a meta and therefore maybe is a RegionCreator
        if (!configHandler.isRegionCreator(blockIdentifier) || !item.hasItemMeta() || !Objects.equals(plugin.getLore(item), plugin.getLore())) {
            return;
        }

        // Create Integers for the players regionCount and max region per group
        int playerRegionCount, maxRegionsGroup;

        // Create the group string, by default the group is set to "default"
        String group = "default";

        // Get the group
        for (String s : configHandler.getGroups()) {
            // Check whether the player has the permission corresponding to a group
            if (p.hasPermission("autoregion.regions." + s)) {
                group = s;
                break;
            }
        }

        // Get max regions of the group
        maxRegionsGroup = configHandler.getRegionCount(group, true);

        // Get players regionCount
        playerRegionCount = configHandler.getRegionCount(playerID);

        /* Check whether player has max regions already
         * -1 equals unlimited regions
         * OPs shouldn't be restricted
         */
        if (playerRegionCount >= maxRegionsGroup && maxRegionsGroup != -1 && !p.isOp() && !p.hasPermission("autoregion.regions.*")) {
            HashMap<String, String> replacements = new HashMap<>();
            replacements.put("%REGIONS%", Integer.toString(maxRegionsGroup));
            replacements.put("%GROUP%", group);

            messageHandler.sendMessage(p, "maxRegionsReached", replacements);
            e.setCancelled(true);
            return;
        }

        // Get coordinates of the placed block
        int x = block.getX();
        int z = block.getZ();

        // Get the radius of the RegionCreator
        int radius = configHandler.getRadius(blockIdentifier);

        // Create BlockVectors for each corner and from 0 to the world's maximum build height
        BlockVector3 pos1 = BlockVector3.at(x + radius, 0, z + radius);
        BlockVector3 pos2 = BlockVector3.at(x - radius, block.getWorld().getMaxHeight(), z - radius);

        // Get the RegionManager of the world
        RegionManager rMan = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(block.getWorld()));
        assert rMan != null;

        // Create the Region Instance
        ProtectedCuboidRegion pReg;

        /* Check whether the player already has a region
         * If so, add a count next to the regions name
         * Create RegionInstance
         */
        if (rMan.getRegion(p.getName()) != null) {
            pReg = new ProtectedCuboidRegion(p.getName() + playerRegionCount, pos1, pos2);
        } else {
            pReg = new ProtectedCuboidRegion(p.getName(), pos1, pos2);
        }

        Map<String, ProtectedRegion> regionList = rMan.getRegions();
        List<ProtectedRegion> regions = new ArrayList<>();

        // Get all regions in the world
        for (String key : regionList.keySet()) {
            // Get the region with the name of key
            ProtectedRegion pr = regionList.get(key);
            // Exclude the global region ("__global__")
            if (pr.getType() != RegionType.GLOBAL) {
                // Add region to the list
                regions.add(pr);
            }
        }

        // Check whether there are intersecting regions
        if (!pReg.getIntersectingRegions(regions).isEmpty()) {
            messageHandler.sendMessage(p, "regionIntersecting");
            e.setCancelled(true);
            return;
        }


        DefaultDomain d = new DefaultDomain();

        // Convert name to WorldEdit UUID,
        // src: https://worldguard.readthedocs.io/en/latest/developer/regions/protected-region/
        ListeningExecutorService executor =
                MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        String[] input = new String[]{p.getName()};
        ProfileService profiles = WorldGuard.getInstance().getProfileService();
        DomainInputResolver resolver = new DomainInputResolver(profiles, input);
        resolver.setLocatorPolicy(UserLocatorPolicy.UUID_ONLY);
        ListenableFuture<DefaultDomain> future = executor.submit(resolver);

        // Add a callback using Guava
        Futures.addCallback(
                future,
                new FutureCallback<DefaultDomain>() {
                    @Override
                    public void onFailure(@NotNull Throwable throwable) {
                        d.addPlayer(p.getName());
                        d.addPlayer(playerID);
                        pReg.setOwners(d);
                    }

                    @Override
                    public void onSuccess(DefaultDomain result) {
                        pReg.setOwners(result);
                    }

                },
                Runnable::run
        );

        // Add region to the RegionManager
        rMan.addRegion(pReg);

        // Replace RegionCreator with air
        e.getBlockPlaced().setType(Material.AIR);

        configHandler.addRegion(p);
        messageHandler.sendMessage(p, "regionCreated", Collections.singletonMap("%OWNER%", p.getName()));
    }

    @EventHandler
    public static void onPlayerJoin(PlayerJoinEvent e) {
        // Get the joined player and their UUID
        Player p = e.getPlayer();

        // Get the RegionCreator which will be given to the player
        String blockIdentifier = configHandler.blockOnFirstJoin();

        // Check whether this is the first time the player has played and blockOnFirstJoin is enabled
        if (!p.hasPlayedBefore() && blockIdentifier != null) {
            // Get the name of the item
            String itemName = configHandler.createItemName(blockIdentifier);

            // Create the item
            Material m = Material.getMaterial(blockIdentifier);
            assert m != null;
            ItemStack stack = new ItemStack(m);

            // Get the itemMeta
            ItemMeta meta = AutoRegion.getMetaSafe(stack);

            // Set ItemName and ItemLore
            meta.setDisplayName(itemName);
            meta.setLore(plugin.getLore());

            // Set the Meta
            stack.setItemMeta(meta);

            // Add item to the players inventory
            p.getInventory().addItem(stack);

            // Add 1 to the RegionCreatorsReceived count 
            configHandler.addReceivedRegionCreator(p);

            messageHandler.sendMessage(p, "playerWelcomeMessage", Collections.singletonMap("%DIAMETER%", String.valueOf(configHandler.getDiameter(blockIdentifier))));
        }
    }
}

