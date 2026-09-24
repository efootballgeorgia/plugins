package com.roleplay.phone.gps;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.gps.model.GpsNode;
import com.roleplay.phone.gps.model.GpsSession;
import com.roleplay.phone.gps.pathfinding.GpsPathfinder;
import com.roleplay.phone.gps.storage.GpsStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GpsManager {
   private final PhonePlugin plugin;
   private final GpsStorage storage;
   private final Map<String, GpsNode> nodes = new ConcurrentHashMap();
   private final Map<UUID, GpsSession> activeSessions = new ConcurrentHashMap();
   private final AtomicInteger nodeSequence = new AtomicInteger(1000);
   private BukkitTask trackingTask;
   private double waypointReachRadiusSq;
   private double destinationReachRadiusSq;
   private double maxOffTrackDistanceSq;
   private double maxNodeSearchRadius;

   public GpsManager(PhonePlugin plugin) {
      this.plugin = plugin;
      this.storage = new GpsStorage(plugin);
      this.reloadRadii();
      this.load();
      this.startTrackingLoop();
   }

   public void reloadRadii() {
      double waypointRadius = this.plugin.getConfig().getDouble("gps.waypoint-radius", (double)4.5F);
      double arrivalRadius = this.plugin.getConfig().getDouble("gps.arrival-radius", (double)6.0F);
      double offTrackRadius = this.plugin.getConfig().getDouble("gps.off-track-reroute-distance", (double)18.0F);
      this.maxNodeSearchRadius = this.plugin.getConfig().getDouble("gps.max-node-search-radius", (double)150.0F);
      this.waypointReachRadiusSq = waypointRadius * waypointRadius;
      this.destinationReachRadiusSq = arrivalRadius * arrivalRadius;
      this.maxOffTrackDistanceSq = offTrackRadius * offTrackRadius;
   }

   public void load() {
      this.nodes.clear();
      this.nodes.putAll(this.storage.loadAll());

      for(String id : this.nodes.keySet()) {
         if (id.startsWith("node_")) {
            try {
               int num = Integer.parseInt(id.substring(5));
               if (num >= this.nodeSequence.get()) {
                  this.nodeSequence.set(num + 1);
               }
            } catch (NumberFormatException var4) {
            }
         }
      }

   }

   public void save() {
      this.storage.saveAll(this.nodes.values());
   }

   public String generateNextNodeId() {
      return "node_" + this.nodeSequence.incrementAndGet();
   }

   public Map<String, GpsNode> getNodes() {
      return Collections.unmodifiableMap(this.nodes);
   }

   public GpsNode getNode(String id) {
      return (GpsNode)this.nodes.get(id);
   }

   public void registerNode(GpsNode node) {
      this.nodes.put(node.getId(), node);
      this.save();
   }

   public void removeNode(String id) {
      GpsNode removed = (GpsNode)this.nodes.remove(id);
      if (removed != null) {
         for(GpsNode other : this.nodes.values()) {
            other.removeNeighbor(id);
         }

         this.save();
      }

   }

   public void connectNodes(String idA, String idB, boolean bidirectional) {
      GpsNode a = (GpsNode)this.nodes.get(idA);
      GpsNode b = (GpsNode)this.nodes.get(idB);
      if (a != null && b != null) {
         a.addNeighbor(idB);
         if (bidirectional) {
            b.addNeighbor(idA);
         }

         this.save();
      }

   }

   public void disconnectNodes(String idA, String idB) {
      GpsNode a = (GpsNode)this.nodes.get(idA);
      GpsNode b = (GpsNode)this.nodes.get(idB);
      if (a != null) {
         a.removeNeighbor(idB);
      }

      if (b != null) {
         b.removeNeighbor(idA);
      }

      this.save();
   }

   public List<GpsNode> getDestinations() {
      List<GpsNode> destinations = new ArrayList();

      for(GpsNode node : this.nodes.values()) {
         if (node.isDestination()) {
            destinations.add(node);
         }
      }

      return destinations;
   }

   public List<GpsNode> getDestinationsByCategory(String category) {
      List<GpsNode> results = new ArrayList();

      for(GpsNode node : this.nodes.values()) {
         if (node.isDestination() && node.getCategory().equalsIgnoreCase(category)) {
            results.add(node);
         }
      }

      return results;
   }

   public boolean isNavigating(UUID playerUuid) {
      return this.activeSessions.containsKey(playerUuid);
   }

   public GpsSession getSession(UUID playerUuid) {
      return (GpsSession)this.activeSessions.get(playerUuid);
   }

   public boolean startNavigation(Player player, GpsNode destinationNode) {
      Location destLoc = destinationNode.toLocation();
      if (destLoc == null) {
         player.sendMessage(Component.text("✖ Target world is currently unloaded.", NamedTextColor.RED));
         return false;
      } else {
         String title = destinationNode.getDestinationName().isBlank() ? destinationNode.getId() : destinationNode.getDestinationName();
         return this.startNavigation(player, title, destLoc, destinationNode);
      }
   }

   public boolean startNavigation(Player player, String title, Location targetLocation) {
      GpsNode closestTargetNode = GpsPathfinder.findClosestAccessibleNode(this.nodes, targetLocation, this.maxNodeSearchRadius);
      return this.startNavigation(player, title, targetLocation, closestTargetNode);
   }

   private boolean startNavigation(Player player, String title, Location targetLocation, GpsNode exitNode) {
      Location playerLoc = player.getLocation();
      if (!playerLoc.getWorld().equals(targetLocation.getWorld())) {
         player.sendMessage(Component.text("✖ Cannot navigate: destination is in another dimension.", NamedTextColor.RED));
         return false;
      } else {
         this.stopNavigation(player, GpsSession.Status.CANCELLED);
         double directDistSq = playerLoc.distanceSquared(targetLocation);
         List<GpsNode> route = new ArrayList();
         GpsNode entryNode = GpsPathfinder.findClosestAccessibleNode(this.nodes, playerLoc, this.maxNodeSearchRadius);
         boolean useDirectRoute = false;
         if (entryNode != null && exitNode != null) {
            double entryDistSq = playerLoc.distanceSquared(entryNode.toLocation());
            if (directDistSq < entryDistSq && GpsPathfinder.hasLineOfSight(playerLoc.clone().add((double)0.0F, 1.2, (double)0.0F), targetLocation.clone().add((double)0.0F, 1.2, (double)0.0F))) {
               useDirectRoute = true;
            } else {
               route = GpsPathfinder.findPath(this.nodes, entryNode, exitNode);
               if (route.isEmpty() && entryNode.equals(exitNode)) {
                  route.add(entryNode);
               } else if (route.isEmpty()) {
                  useDirectRoute = true;
               }
            }
         } else {
            useDirectRoute = true;
         }

         GpsSession session = new GpsSession(player.getUniqueId(), title, targetLocation, playerLoc, useDirectRoute ? Collections.emptyList() : route);
         this.activeSessions.put(player.getUniqueId(), session);
         player.sendMessage(Component.text("✔ Route calculated to: ", NamedTextColor.GREEN).append(Component.text(title, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)));
         player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.8F, 1.2F);
         return true;
      }
   }

   public void stopNavigation(Player player, GpsSession.Status endStatus) {
      GpsSession session = (GpsSession)this.activeSessions.remove(player.getUniqueId());
      if (session != null) {
         session.setStatus(endStatus);
         if (endStatus == GpsSession.Status.ARRIVED) {
            player.sendMessage(Component.text("✔ You have arrived at: ", NamedTextColor.GREEN).append(Component.text(session.getDestinationTitle(), NamedTextColor.GOLD)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
         } else if (endStatus == GpsSession.Status.CANCELLED) {
            player.sendMessage(Component.text("✖ Navigation cancelled.", NamedTextColor.YELLOW));
         }
      }

   }

   public void cancelAll() {
      if (this.trackingTask != null) {
         this.trackingTask.cancel();
      }

      for(UUID uuid : this.activeSessions.keySet()) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            this.stopNavigation(p, GpsSession.Status.CANCELLED);
         }
      }

      this.activeSessions.clear();
   }

   private void startTrackingLoop() {
      this.trackingTask = (new BukkitRunnable() {
         public void run() {
            for(Map.Entry<UUID, GpsSession> entry : GpsManager.this.activeSessions.entrySet()) {
               Player player = Bukkit.getPlayer((UUID)entry.getKey());
               GpsSession session = (GpsSession)entry.getValue();
               if (player != null && player.isOnline()) {
                  GpsManager.this.updateSession(player, session);
               } else {
                  GpsManager.this.activeSessions.remove(entry.getKey());
               }
            }

         }
      }).runTaskTimer(this.plugin, 6L, 6L);
   }

   private void updateSession(Player player, GpsSession session) {
      Location pLoc = player.getLocation();
      Location destLoc = session.getDestinationLocation();
      if (!pLoc.getWorld().equals(destLoc.getWorld())) {
         this.stopNavigation(player, GpsSession.Status.CANCELLED);
      } else if (pLoc.distanceSquared(destLoc) <= this.destinationReachRadiusSq) {
         this.stopNavigation(player, GpsSession.Status.ARRIVED);
      } else {
         GpsNode targetNode = session.getCurrentTargetNode();
         if (targetNode != null) {
            Location nodeLoc = targetNode.toLocation();
            if (nodeLoc != null && nodeLoc.getWorld().equals(pLoc.getWorld())) {
               boolean reachedByRadius = pLoc.distanceSquared(nodeLoc) <= this.waypointReachRadiusSq;
               boolean passedByVector = session.hasPassedWaypoint(pLoc);
               if (reachedByRadius || passedByVector) {
                  session.advanceNode(pLoc);
                  player.playSound(pLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.4F, 1.8F);
               }
            }
         }

         double crossTrackDistSq = session.getCrossTrackDistanceSquared(pLoc);
         if (crossTrackDistSq > this.maxOffTrackDistanceSq) {
            this.reroute(player, session);
         }

      }
   }

   private void reroute(Player player, GpsSession session) {
      Location pLoc = player.getLocation();
      GpsNode newEntry = GpsPathfinder.findClosestAccessibleNode(this.nodes, pLoc, this.maxNodeSearchRadius);
      GpsNode exitNode = GpsPathfinder.findClosestAccessibleNode(this.nodes, session.getDestinationLocation(), this.maxNodeSearchRadius);
      List<GpsNode> newRoute = Collections.emptyList();
      if (newEntry != null && exitNode != null) {
         newRoute = GpsPathfinder.findPath(this.nodes, newEntry, exitNode);
      }

      session.updateRoute(pLoc, newRoute);
      player.sendMessage(Component.text("↻ Off course: recalculating route...", NamedTextColor.YELLOW));
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.5F, 0.8F);
   }
}
