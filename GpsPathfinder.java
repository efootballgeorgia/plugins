package com.roleplay.phone.gps.pathfinding;

import com.roleplay.phone.gps.model.GpsNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class GpsPathfinder {
   private GpsPathfinder() {
   }

   public static List<GpsNode> findPath(Map<String, GpsNode> graph, GpsNode start, GpsNode goal) {
      if (graph != null && start != null && goal != null) {
         if (start.equals(goal)) {
            return List.of(start);
         } else if (!start.getWorldName().equals(goal.getWorldName())) {
            return Collections.emptyList();
         } else {
            PriorityQueue<NodeRecord> openQueue = new PriorityQueue();
            Set<String> openSetIds = new HashSet();
            Map<String, String> cameFrom = new HashMap();
            Map<String, Double> gScore = new HashMap();

            for(String id : graph.keySet()) {
               gScore.put(id, Double.MAX_VALUE);
            }

            gScore.put(start.getId(), (double)0.0F);
            double initialF = start.distance(goal);
            openQueue.add(new NodeRecord(start, initialF));
            openSetIds.add(start.getId());

            while(!openQueue.isEmpty()) {
               NodeRecord currentRecord = (NodeRecord)openQueue.poll();
               GpsNode current = currentRecord.node;
               openSetIds.remove(current.getId());
               if (current.equals(goal)) {
                  return reconstructPath(graph, cameFrom, current.getId());
               }

               double currentG = (Double)gScore.getOrDefault(current.getId(), Double.MAX_VALUE);

               for(String neighborId : current.getNeighborIds()) {
                  GpsNode neighbor = (GpsNode)graph.get(neighborId);
                  if (neighbor != null && neighbor.getWorldName().equals(current.getWorldName())) {
                     double tentativeG = currentG + current.distance(neighbor);
                     if (tentativeG < (Double)gScore.getOrDefault(neighborId, Double.MAX_VALUE)) {
                        cameFrom.put(neighborId, current.getId());
                        gScore.put(neighborId, tentativeG);
                        double f = tentativeG + neighbor.distance(goal);
                        if (!openSetIds.contains(neighborId)) {
                           openQueue.add(new NodeRecord(neighbor, f));
                           openSetIds.add(neighborId);
                        }
                     }
                  }
               }
            }

            return Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   private static List<GpsNode> reconstructPath(Map<String, GpsNode> graph, Map<String, String> cameFrom, String currentId) {
      List<GpsNode> path = new ArrayList();

      for(String step = currentId; step != null; step = (String)cameFrom.get(step)) {
         GpsNode node = (GpsNode)graph.get(step);
         if (node != null) {
            path.add(node);
         }
      }

      Collections.reverse(path);
      return path;
   }

   public static GpsNode findClosestAccessibleNode(Map<String, GpsNode> graph, Location location, double maxSearchRadius) {
      if (graph != null && location != null && location.getWorld() != null) {
         String worldName = location.getWorld().getName();
         Location playerEye = location.clone().add((double)0.0F, 1.2, (double)0.0F);
         double maxDistSq = maxSearchRadius * maxSearchRadius;
         GpsNode closestVisible = null;
         double minVisibleDistSq = maxDistSq;
         GpsNode fallbackClosest = null;
         double minFallbackDistSq = maxDistSq;

         for(GpsNode node : graph.values()) {
            if (node.getWorldName().equals(worldName)) {
               double distSq = node.distanceSquared(location);
               if (!(distSq > maxDistSq)) {
                  if (distSq < minFallbackDistSq) {
                     minFallbackDistSq = distSq;
                     fallbackClosest = node;
                  }

                  Location nodeTarget = node.toLocation();
                  if (nodeTarget != null) {
                     Location nodeEye = nodeTarget.clone().add((double)0.0F, 0.6, (double)0.0F);
                     if (hasLineOfSight(playerEye, nodeEye) && distSq < minVisibleDistSq) {
                        minVisibleDistSq = distSq;
                        closestVisible = node;
                     }
                  }
               }
            }
         }

         return closestVisible != null ? closestVisible : fallbackClosest;
      } else {
         return null;
      }
   }

   public static boolean hasLineOfSight(Location from, Location to) {
      if (from != null && to != null && from.getWorld() != null && to.getWorld() != null) {
         if (!from.getWorld().equals(to.getWorld())) {
            return false;
         } else {
            World world = from.getWorld();
            Vector direction = to.toVector().subtract(from.toVector());
            double distance = direction.length();
            if (distance < 0.4) {
               return true;
            } else {
               direction.normalize();
               RayTraceResult result = world.rayTraceBlocks(from, direction, distance, FluidCollisionMode.NEVER, true);
               if (result != null && result.getHitBlock() != null) {
                  Block hitBlock = result.getHitBlock();
                  return isPassableObstacle(hitBlock.getType());
               } else {
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isPassableObstacle(Material material) {
      if (material.isAir()) {
         return true;
      } else if (!material.isSolid()) {
         return true;
      } else {
         String name = material.name();
         return name.endsWith("_CARPET") || name.endsWith("_DOOR") || name.endsWith("_GATE") || name.endsWith("_PRESSURE_PLATE") || name.endsWith("_BANNER");
      }
   }

   private static class NodeRecord implements Comparable<NodeRecord> {
      private final GpsNode node;
      private final double fScore;

      public NodeRecord(GpsNode node, double fScore) {
         this.node = node;
         this.fScore = fScore;
      }

      public int compareTo(NodeRecord other) {
         return Double.compare(this.fScore, other.fScore);
      }
   }
}
