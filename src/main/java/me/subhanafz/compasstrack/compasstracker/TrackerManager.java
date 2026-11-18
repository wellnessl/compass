package me.subhanafz.compasstrack.compasstracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TrackerManager {

    private static boolean tracking = false;
    /** avci UUID -> sectigi runner UUID */
    private static final Map<UUID, UUID> selected = new HashMap<>();
    /** runner UUID -> (dunya UUID -> son konum / girdigi portal) */
    private static final Map<UUID, Map<UUID, Location>> trail = new HashMap<>();

    private TrackerManager() {}

    public static boolean isTracking() {
        return tracking;
    }

    public static boolean toggleTracking() {
        tracking = !tracking;
        return tracking;
    }

    public static boolean isRunner(Player p) {
        return CompassTracker.Speedrunners.contains(p.getUniqueId());
    }

    public static void startTask(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }

    /** Runner portala girdiginde cagrilir: girdigi portalin konumunu o dunya icin kaydeder. */
    public static void recordPortal(Player runner, Location portal) {
        if (portal == null || portal.getWorld() == null) return;
        trail.computeIfAbsent(runner.getUniqueId(), k -> new HashMap<>())
                .put(portal.getWorld().getUID(), portal.clone());
    }

    public static List<Player> onlineRunners() {
        List<Player> list = new ArrayList<>();
        for (UUID id : CompassTracker.Speedrunners) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) list.add(p);
        }
        return list;
    }

    /** Avcinin takip ettigi runner (secim yoksa / cevrimdisiysa ilk cevrimici runner). */
    public static Player getSelected(Player hunter) {
        UUID sel = selected.get(hunter.getUniqueId());
        if (sel != null && CompassTracker.Speedrunners.contains(sel)) {
            Player p = Bukkit.getPlayer(sel);
            if (p != null) return p;
        }
        List<Player> online = onlineRunners();
        return online.isEmpty() ? null : online.get(0);
    }

    /** Pusula sag tiklaninca bir sonraki runner'a gec. */
    public static void cycle(Player hunter) {
        List<Player> online = onlineRunners();
        if (online.isEmpty()) {
            hunter.sendActionBar(Component.text("Takip edilecek runner yok.", NamedTextColor.RED));
            return;
        }
        Player current = getSelected(hunter);
        int idx = -1;
        for (int i = 0; i < online.size(); i++) {
            if (current != null && online.get(i).getUniqueId().equals(current.getUniqueId())) {
                idx = i;
                break;
            }
        }
        Player next = online.get((idx + 1) % online.size());
        selected.put(hunter.getUniqueId(), next.getUniqueId());

        Component msg = Component.text("Takip edilen runner: ", NamedTextColor.GRAY)
                .append(Component.text(next.getName(), NamedTextColor.GREEN))
                .append(Component.text(" (" + ((idx + 1) % online.size() + 1) + "/" + online.size() + ")",
                        NamedTextColor.GRAY));
        if (!tracking) {
            msg = msg.append(Component.text(" - takip kapali (/startTracking)", NamedTextColor.RED));
        }
        hunter.sendActionBar(msg);

        if (tracking) updateHunter(hunter);
    }

    private static void tick() {
        // Runner'larin her dunyadaki son konumunu guncelle
        for (UUID id : CompassTracker.Speedrunners) {
            Player r = Bukkit.getPlayer(id);
            if (r == null) continue;
            trail.computeIfAbsent(id, k -> new HashMap<>()).put(r.getWorld().getUID(), r.getLocation());
        }
        if (!tracking) return;
        for (Player hunter : Bukkit.getOnlinePlayers()) {
            if (isRunner(hunter)) continue;
            updateHunter(hunter);
        }
    }

    private static void updateHunter(Player hunter) {
        Player target = getSelected(hunter);
        if (target == null) return;

        World hw = hunter.getWorld();
        Location loc;
        boolean viaPortal;
        if (target.getWorld().getUID().equals(hw.getUID())) {
            loc = target.getLocation();
            viaPortal = false;
        } else {
            Map<UUID, Location> m = trail.get(target.getUniqueId());
            loc = m == null ? null : m.get(hw.getUID());
            viaPortal = true;
        }

        boolean holding = hunter.getInventory().getItemInMainHand().getType() == Material.COMPASS
                || hunter.getInventory().getItemInOffHand().getType() == Material.COMPASS;

        if (loc == null) {
            if (holding) {
                hunter.sendActionBar(Component.text(target.getName() + " baska bir boyutta, portal bilgisi yok",
                        NamedTextColor.RED));
            }
            return;
        }

        hunter.setCompassTarget(loc); // Overworld icin

        // Nether/End'de normal pusula donup durur; lodestone pusulasi her boyutta calisir.
        PlayerInventory inv = hunter.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != Material.COMPASS) continue;
            if (!(it.getItemMeta() instanceof CompassMeta meta)) continue;
            if (sameBlock(meta.getLodestone(), loc)) continue;
            meta.setLodestoneTracked(false);
            meta.setLodestone(loc);
            it.setItemMeta(meta);
            inv.setItem(i, it);
        }

        if (holding) {
            int dist = (int) hunter.getLocation().distance(loc);
            Component msg;
            if (viaPortal) {
                msg = Component.text("Takip: ", NamedTextColor.GRAY)
                        .append(Component.text(target.getName(), NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" | girdigi portal | " + dist + " blok", NamedTextColor.GRAY));
            } else {
                msg = Component.text("Takip: ", NamedTextColor.GRAY)
                        .append(Component.text(target.getName(), NamedTextColor.GREEN))
                        .append(Component.text(" | " + dist + " blok", NamedTextColor.GRAY));
            }
            hunter.sendActionBar(msg);
        }
    }

    private static boolean sameBlock(Location a, Location b) {
        return a != null && a.getWorld() != null && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
