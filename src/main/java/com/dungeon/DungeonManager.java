package com.dungeon;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class DungeonManager implements Listener {
    private final JavaPlugin plugin;
    private Location dungeonSpawn;
    private Location lobby;
    private final Set<UUID> inDungeon = new HashSet<>();
    private Breeze boss;
    private Location bossCenter;

    public DungeonManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void createDungeon(Player p) {
        World world = p.getWorld();
        lobby = p.getLocation();
        Location base = new Location(world, p.getLocation().getX() + 100, 60, p.getLocation().getZ());
        dungeonSpawn = base.clone().add(2, 1, 2);
        for (int room = 0; room < 4; room++) buildRoom(world, base.getBlockX() + room * 12, 60, base.getBlockZ());
        int bossX = base.getBlockX() + 4 * 12 + 5;
        buildBossRoom(world, bossX, 60, base.getBlockZ());
        p.sendMessage("§aDa tao Dungeon! Boss co 4 SKILL!");
        startBossSkills();
    }

    private void spawnMob(World world, Location loc, EntityType type, String name, double hp) {
        Entity e = world.spawnEntity(loc, type);
        if (e instanceof Mob mob) {
            mob.setPersistent(true);
            mob.setRemoveWhenFarAway(false);
            if (name != null) { mob.setCustomName(name); mob.setCustomNameVisible(true); }
            if (hp > 0) { mob.setMaxHealth(hp); mob.setHealth(hp); }
        }
    }

    private void buildRoom(World world, int x, int y, int z) {
        for (int dx = 0; dx < 10; dx++) {
            for (int dz = 0; dz < 10; dz++) {
                Material floor = (dx + dz) % 2 == 0 ? Material.TUFF_BRICKS : Material.COPPER_BLOCK;
                world.getBlockAt(x + dx, y, z + dz).setType(floor);
                world.getBlockAt(x + dx, y + 4, z + dz).setType(Material.TUFF_BRICKS);
                if (dx == 0 || dx == 9 || dz == 0 || dz == 9) {
                    for (int h = 1; h <= 3; h++) world.getBlockAt(x + dx, y + h, z + dz).setType(Material.TUFF_BRICKS);
                } else {
                    for (int h = 1; h <= 3; h++) world.getBlockAt(x + dx, y + h, z + dz).setType(Material.AIR);
                }
            }
        }
        for (int h = 1; h <= 2; h++) {
            world.getBlockAt(x + 9, y + h, z + 5).setType(Material.AIR);
            world.getBlockAt(x, y + h, z + 5).setType(Material.AIR);
        }
        world.getBlockAt(x + 5, y + 3, z + 5).setType(Material.COPPER_BULB);
        Location spawn = new Location(world, x + 5, y + 1, z + 5);
        spawnMob(world, spawn, EntityType.ZOMBIE, "§aQuai Dungeon", 30);
        spawnMob(world, spawn.clone().add(2, 0, 2), EntityType.SKELETON, "§aQuai Dungeon", 30);
        spawnMob(world, spawn.clone().add(-2, 0, -2), EntityType.BOGGED, "§aQuai Dungeon", 30);
        spawnMob(world, spawn.clone().add(2, 0, -2), EntityType.BREEZE, "§aQuai Dungeon", 30);
    }

    private void buildBossRoom(World world, int x, int y, int z) {
        int S = 30, H = 8;
        for (int dx = 0; dx < S; dx++) {
            for (int dz = 0; dz < S; dz++) {
                boolean isEdge = dx < 2 || dx >= S - 2 || dz < 2 || dz >= S - 2;
                if (isEdge) world.getBlockAt(x + dx, y, z + dz).setType(Material.MAGMA_BLOCK);
                else {
                    double cx = dx - S/2.0, cz = dz - S/2.0;
                    double dist = Math.sqrt(cx*cx + cz*cz);
                    world.getBlockAt(x + dx, y, z + dz).setType(dist < 6 ? Material.POLISHED_TUFF : Material.TUFF_BRICKS);
                }
                world.getBlockAt(x + dx, y + H, z + dz).setType(Material.TUFF_BRICKS);
                if (dx == 0 || dx == S-1 || dz == 0 || dz == S-1) {
                    for (int h = 1; h < H; h++) world.getBlockAt(x + dx, y + h, z + dz).setType(Material.TUFF_BRICKS);
                } else {
                    for (int h = 1; h < H; h++) world.getBlockAt(x + dx, y + h, z + dz).setType(Material.AIR);
                }
            }
        }
        for (int h = 1; h <= 3; h++) for (int w = -1; w <= 1; w++) world.getBlockAt(x, y + h, z + S/2 + w).setType(Material.AIR);
        int[][] pillars = {{4,4},{4,S-5},{S-5,4},{S-5,S-5}};
        for (int[] p : pillars) {
            for (int h = 1; h < H; h++) {
                world.getBlockAt(x + p[0], y + h, z + p[1]).setType(Material.CHISELED_TUFF);
                world.getBlockAt(x + p[0]+1, y + h, z + p[1]).setType(Material.TUFF_BRICKS);
            }
            world.getBlockAt(x + p[0], y + H, z + p[1]).setType(Material.COPPER_BULB);
        }
        world.getBlockAt(x + S/2, y + H - 1, z + S/2).setType(Material.COPPER_BULB);
        bossCenter = new Location(world, x + S/2, y + 1, z + S/2);
        // Spawn Boss va luu lai de dung skill
        Entity e = world.spawnEntity(bossCenter, EntityType.BREEZE);
        if (e instanceof Breeze b) {
            boss = b;
            boss.setPersistent(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCustomName("§c§lBOSS - VUA GIO");
            boss.setCustomNameVisible(true);
            boss.setMaxHealth(300);
            boss.setHealth(300);
        }
        spawnMob(world, bossCenter.clone().add(8, 0, 8), EntityType.BOGGED, "§eLinh Boss", 60);
        spawnMob(world, bossCenter.clone().add(-8, 0, 8), EntityType.BOGGED, "§eLinh Boss", 60);
        world.getBlockAt(x + S/2, y + 1, z + S - 4).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(x + S/2, y + 1, z + S - 4).getState();
        chest.getInventory().addItem(new ItemStack(Material.MACE));
        chest.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
        chest.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 32));
        chest.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));
        chest.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
        chest.update();
    }

    // ===== BOSS SKILL =====
    private void startBossSkills() {
        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (boss == null || boss.isDead()) { cancel(); return; }
                tick++;
                World world = boss.getWorld();
                Location bl = boss.getLocation();
                // Tim player gan Boss
                List<Player> targets = new ArrayList<>();
                for (Player p : world.getPlayers()) if (p.getLocation().distance(bl) < 35) targets.add(p);
                if (targets.isEmpty()) return;
                boolean enraged = boss.getHealth() < 150; // Duoi 50% mau danh nhanh hon
                int interval = enraged ? 60 : 100; // 3 giay hoac 5 giay

                if (tick % interval == 0) {
                    int skill = new Random().nextInt(4);
                    Player target = targets.get(new Random().nextInt(targets.size()));
                    switch (skill) {
                        case 0 -> { // SKILL 1: Bao Gio - Ban 8 Wind Charge
                            Bukkit.broadcastMessage("§b§lBOSS dung BAO GIO!");
                            for (int i = 0; i < 8; i++) {
                                double angle = i * Math.PI / 4;
                                Vector dir = new Vector(Math.cos(angle), 0.3, Math.sin(angle));
                                WindCharge wc = (WindCharge) world.spawnEntity(bl.clone().add(0,1,0), EntityType.WIND_CHARGE);
                                wc.setVelocity(dir.multiply(1.5));
                            }
                            world.playSound(bl, Sound.ENTITY_BREEZE_SHOOT, 2, 1);
                        }
                        case 1 -> { // SKILL 2: Trieu Hoi
                            Bukkit.broadcastMessage("§e§lBOSS TRIEU HOI LINH!");
                            spawnMob(world, bl.clone().add(3,0,0), EntityType.BOGGED, "§eLinh Duoc Trieu Hoi", 40);
                            spawnMob(world, bl.clone().add(-3,0,0), EntityType.BREEZE, "§eLinh Duoc Trieu Hoi", 40);
                            world.spawnParticle(Particle.CLOUD, bl, 30, 2, 2, 2, 0.1);
                        }
                        case 2 -> { // SKILL 3: Tele + Set
                            Bukkit.broadcastMessage("§c§lBOSS DICH CHUYEN!");
                            Location behind = target.getLocation().add(target.getLocation().getDirection().multiply(-3));
                            boss.teleport(behind);
                            world.strikeLightningEffect(target.getLocation());
                            target.damage(4);
                            world.playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
                        }
                        case 3 -> { // SKILL 4: Soc Gio - Hat vang
                            Bukkit.broadcastMessage("§d§lBOSS dung SOC GIO! CAN THAN!");
                            for (Player p : targets) {
                                Vector push = p.getLocation().toVector().subtract(bl.toVector()).normalize().multiply(2);
                                push.setY(1.2);
                                p.setVelocity(push);
                                p.damage(3);
                            }
                            world.spawnParticle(Particle.EXPLOSION, bl, 10, 3, 1, 3, 0.1);
                            world.playSound(bl, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.8f);
                        }
                    }
                    if (enraged) Bukkit.broadcastMessage("§4§lBOSS DANG NOI GIAN! Danh nhanh hon!");
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Chay moi giay
    }

    public void joinDungeon(Player p) {
        if (dungeonSpawn == null) { p.sendMessage("§cChua co dungeon! /dungeon create truoc."); return; }
        inDungeon.add(p.getUniqueId());
        p.teleport(dungeonSpawn);
        p.sendMessage("§aDa vao Dungeon! Can than Boss co Skill!");
    }
    public void leaveDungeon(Player p) {
        inDungeon.remove(p.getUniqueId());
        if (lobby != null) p.teleport(lobby);
        p.sendMessage("§eDa roi khoi Dungeon.");
    }
    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (e.getEntity().getCustomName() != null && e.getEntity().getCustomName().contains("BOSS")) {
            Bukkit.broadcastMessage("§6§lBOSS DUNGEON DA BI HA! Mo ruong lay Mace!");
            if (e.getEntity().equals(boss)) boss = null;
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) { inDungeon.remove(e.getPlayer().getUniqueId()); }
                                     }
