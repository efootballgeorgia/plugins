package com.roleplay.phone.gps.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class GpsNode {
   private final String id;
   private final String worldName;
   private double x;
   private double y;
   private double z;
   private final Set<String> neighborIds;
   private boolean destination;
   private String destinationName;
   private String category;
   private Material iconMaterial;
   private String description;

   public GpsNode(String id, String worldName, double x, double y, double z) {
      this.id = id;
      this.worldName = worldName;
      this.x = x;
      this.y = y;
      this.z = z;
      this.neighborIds = new HashSet();
      this.destination = false;
      this.destinationName = "";
      this.category = "General";
      this.iconMaterial = Material.COMPASS;
      this.description = "";
   }

   public GpsNode(String id, Location location) {
      this(id, location.getWorld() != null ? location.getWorld().getName() : "world", location.getX(), location.getY(), location.getZ());
   }

   public String getId() {
      return this.id;
   }

   public String getWorldName() {
      return this.worldName;
   }

   public double getX() {
      return this.x;
   }

   public void setX(double x) {
      this.x = x;
   }

   public double getY() {
      return this.y;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getZ() {
      return this.z;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public Location toLocation() {
      World world = Bukkit.getWorld(this.worldName);
      return world == null ? null : new Location(world, this.x, this.y, this.z);
   }

   public double distanceSquared(Location loc) {
      if (loc != null && loc.getWorld() != null && loc.getWorld().getName().equals(this.worldName)) {
         double dx = this.x - loc.getX();
         double dy = this.y - loc.getY();
         double dz = this.z - loc.getZ();
         return dx * dx + dy * dy + dz * dz;
      } else {
         return Double.MAX_VALUE;
      }
   }

   public double distance(Location loc) {
      return Math.sqrt(this.distanceSquared(loc));
   }

   public double distance(GpsNode other) {
      if (other != null && other.getWorldName().equals(this.worldName)) {
         double dx = this.x - other.x;
         double dy = this.y - other.y;
         double dz = this.z - other.z;
         return Math.sqrt(dx * dx + dy * dy + dz * dz);
      } else {
         return Double.MAX_VALUE;
      }
   }

   public Set<String> getNeighborIds() {
      return Collections.unmodifiableSet(this.neighborIds);
   }

   public void addNeighbor(String neighborId) {
      if (!neighborId.equalsIgnoreCase(this.id)) {
         this.neighborIds.add(neighborId);
      }

   }

   public void removeNeighbor(String neighborId) {
      this.neighborIds.remove(neighborId);
   }

   public boolean isDestination() {
      return this.destination;
   }

   public void setDestination(boolean destination) {
      this.destination = destination;
   }

   public String getDestinationName() {
      return this.destinationName;
   }

   public void setDestinationName(String destinationName) {
      this.destinationName = destinationName != null ? destinationName : "";
   }

   public String getCategory() {
      return this.category;
   }

   public void setCategory(String category) {
      this.category = category != null ? category : "General";
   }

   public Material getIconMaterial() {
      return this.iconMaterial != null ? this.iconMaterial : Material.COMPASS;
   }

   public void setIconMaterial(Material iconMaterial) {
      this.iconMaterial = iconMaterial != null ? iconMaterial : Material.COMPASS;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String description) {
      this.description = description != null ? description : "";
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o instanceof GpsNode) {
         GpsNode gpsNode = (GpsNode)o;
         return Objects.equals(this.id, gpsNode.id);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.id});
   }
}
