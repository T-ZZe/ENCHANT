package fr.enchantlimit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerPortalEvent;

/** Regles du monde (PvP, dimensions) et equilibrage des degats. */
public class RulesListener implements Listener {

    private final LimitManager limits;

    public RulesListener(LimitManager limits) {
        this.limits = limits;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Entity damager = e.getDamager();

        Player attacker = null;
        if (damager instanceof Player direct) {
            attacker = direct;
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        // PvP global
        if (attacker != null && !attacker.equals(victim)
                && limits.getSetting("pvp_enabled", 1) == 0
                && !attacker.hasPermission("enchantlimit.bypass")) {
            e.setCancelled(true);
            attacker.sendActionBar(Component.text("Le PvP est désactivé.", NamedTextColor.RED));
            return;
        }

        // Equilibrage des degats
        int pct = 100;
        if (damager instanceof EnderCrystal) {
            pct = limits.getSetting("dmg_crystal", 100);
        } else if (damager instanceof Trident) {
            pct = limits.getSetting("dmg_trident", 100);
        } else if (damager instanceof Player melee) {
            Material m = melee.getInventory().getItemInMainHand().getType();
            if (m == Material.MACE) pct = limits.getSetting("dmg_mace", 100);
            else if (m.name().endsWith("_SPEAR")) pct = limits.getSetting("dmg_spear", 100);
            else if (m == Material.TRIDENT) pct = limits.getSetting("dmg_trident", 100);
        }
        if (pct != 100) e.setDamage(e.getDamage() * pct / 100.0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        Location to = e.getTo();
        if (to == null || to.getWorld() == null) return;
        World.Environment env = to.getWorld().getEnvironment();
        String key = null;
        String name = null;
        if (env == World.Environment.NETHER) {
            key = "nether_open";
            name = "Le Nether";
        } else if (env == World.Environment.THE_END) {
            key = "end_open";
            name = "L'End";
        }
        if (key == null || limits.getSetting(key, 1) == 1) return;
        if (e.getPlayer().hasPermission("enchantlimit.bypass")) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(Component.text(name + " est fermé pour le moment.", NamedTextColor.RED));
    }
}
