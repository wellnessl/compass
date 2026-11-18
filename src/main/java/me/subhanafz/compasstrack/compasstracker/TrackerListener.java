package me.subhanafz.compasstrack.compasstracker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TrackerListener implements Listener {

    private static boolean cfg(String key) {
        return CompassTracker.plugin.getConfig().getBoolean(key);
    }

    private static boolean hasCompass(Player p) {
        return p.getInventory().contains(Material.COMPASS);
    }

    private static void giveCompassIfMissing(Player p) {
        if (!hasCompass(p)) p.getInventory().addItem(new ItemStack(Material.COMPASS));
    }

    private static void autoTrack(Player p) {
        String auto = CompassTracker.plugin.getConfig().getString("TrackPlayerAuto");
        if (auto != null && !auto.isEmpty() && auto.equalsIgnoreCase(p.getName())) {
            CompassTracker.Speedrunners.add(p.getUniqueId());
        }
    }

    // ---- Pusula sag tik: runner degistir ----
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        EquipmentSlot hand = e.getHand();
        if (hand == null) return;
        // Cift tetiklenmeyi onle: ana elde de pusula varsa off-hand olayini yok say
        if (hand == EquipmentSlot.OFF_HAND
                && p.getInventory().getItemInMainHand().getType() == Material.COMPASS) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.COMPASS) return;
        if (TrackerManager.isRunner(p)) return;

        // Sandik, kapi vb. etkilesimli bloklara tiklaninca runner degistirme
        if (a == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
                && e.getClickedBlock().getType().isInteractable()
                && e.useInteractedBlock() != Event.Result.DENY
                && !p.isSneaking()) return;

        TrackerManager.cycle(p);
    }

    // ---- Runner portala girince portali kaydet ----
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        Player p = e.getPlayer();
        if (!TrackerManager.isRunner(p)) return;

        Location from = e.getFrom();
        TrackerManager.recordPortal(p, from);

        String dest = "baska bir boyuta";
        Location to = e.getTo();
        if (to != null && to.getWorld() != null) {
            World.Environment env = to.getWorld().getEnvironment();
            dest = env == World.Environment.NETHER ? "Nether'a"
                    : env == World.Environment.THE_END ? "End'e" : "Overworld'e";
        }
        Component msg = Component.text(p.getName(), NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(" portala girdi ve " + dest + " gecti! Pusula portali gosteriyor.",
                        NamedTextColor.GRAY));
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!TrackerManager.isRunner(other)) other.sendMessage(msg);
        }
    }

    // ---- Eski ozellikler ----
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        autoTrack(p);
        if (cfg("giveCompassOnSpawn")) giveCompassIfMissing(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        autoTrack(p);
        if (cfg("giveCompassOnSpawn")) {
            Bukkit.getScheduler().runTask(CompassTracker.plugin, () -> giveCompassIfMissing(p));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (cfg("RemoveCompassOnHunterDeath") && !TrackerManager.isRunner(p)) {
            e.getDrops().removeIf(i -> i.getType() == Material.COMPASS);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (cfg("ForceHuntersNoDropCompass") && !TrackerManager.isRunner(p)
                && e.getItemDrop().getItemStack().getType() == Material.COMPASS) {
            e.setCancelled(true);
        }
    }
}
