package net.kothplugin.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Jednoduchá reprezentace kvádrové (cuboid) oblasti definované dvěma rohy.
 * Používá se jako capture zóna KOTH bodu - nevyžaduje žádnou závislost na WorldGuard.
 */
public class Cuboid {

    private final String world;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;

    public Cuboid(String world, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static Cuboid fromLocations(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null) {
            return null;
        }
        return new Cuboid(a.getWorld().getName(), a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(world)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public String getWorld() {
        return world;
    }

    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }

    public double getCenterX() { return (minX + maxX) / 2.0; }
    public double getCenterY() { return (minY + maxY) / 2.0; }
    public double getCenterZ() { return (minZ + maxZ) / 2.0; }

    public void saveTo(ConfigurationSection section) {
        section.set("world", world);
        section.set("minX", minX);
        section.set("minY", minY);
        section.set("minZ", minZ);
        section.set("maxX", maxX);
        section.set("maxY", maxY);
        section.set("maxZ", maxZ);
    }

    public static Cuboid loadFrom(ConfigurationSection section) {
        if (section == null || !section.contains("world")) return null;
        String world = section.getString("world");
        double minX = section.getDouble("minX");
        double minY = section.getDouble("minY");
        double minZ = section.getDouble("minZ");
        double maxX = section.getDouble("maxX");
        double maxY = section.getDouble("maxY");
        double maxZ = section.getDouble("maxZ");
        return new Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public World bukkitWorld() {
        return org.bukkit.Bukkit.getWorld(world);
    }
}
