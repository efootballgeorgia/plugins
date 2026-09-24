package com.roleplay.phone.gps.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class GpsSession {
   private final UUID playerUuid;
   private final String destinationTitle;
   private final Location destinationLocation;
   private final List<GpsNode> route;
   private int currentRouteIndex;
   private Status status;
   private final long startTimeMillis;
   private Location segmentStartLocation;
   private UUID arrowDisplayUuid;

   public GpsSession(UUID playerUuid, String destinationTitle, Location destinationLocation, Location originLocation, List<GpsNode> calculatedRoute) {
      this.playerUuid = playerUuid;
      this.destinationTitle = destinationTitle;
      this.destinationLocation = destinationLocation.clone();
      this.route = new ArrayList(calculatedRoute != null ? calculatedRoute : Collections.emptyList());
      this.currentRouteIndex = 0;
      this.status = GpsSession.Status.NAVIGATING;
      this.startTimeMillis = System.currentTimeMillis();
      this.segmentStartLocation = originLocation != null ? originLocation.clone() : destinationLocation.clone();
      this.arrowDisplayUuid = null;
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public String getDestinationTitle() {
      return this.destinationTitle;
   }

   public Location getDestinationLocation() {
      return this.destinationLocation.clone();
   }

   public List<GpsNode> getRoute() {
      return Collections.unmodifiableList(this.route);
   }

   public synchronized void updateRoute(Location currentPlayerLoc, List<GpsNode> newRoute) {
      this.route.clear();
      if (newRoute != null) {
         this.route.addAll(newRoute);
      }

      this.currentRouteIndex = 0;
      this.segmentStartLocation = currentPlayerLoc != null ? currentPlayerLoc.clone() : this.destinationLocation.clone();
      this.status = GpsSession.Status.NAVIGATING;
   }

   public int getCurrentRouteIndex() {
      return this.currentRouteIndex;
   }

   public GpsNode getCurrentTargetNode() {
      return !this.route.isEmpty() && this.currentRouteIndex < this.route.size() ? (GpsNode)this.route.get(this.currentRouteIndex) : null;
   }

   public Location getSegmentStartLocation() {
      return this.segmentStartLocation.clone();
   }

   public boolean advanceNode(Location playerLocation) {
      if (this.currentRouteIndex < this.route.size()) {
         GpsNode passedNode = (GpsNode)this.route.get(this.currentRouteIndex);
         Location passedLoc = passedNode.toLocation();
         if (passedLoc != null) {
            this.segmentStartLocation = passedLoc.clone();
         } else if (playerLocation != null) {
            this.segmentStartLocation = playerLocation.clone();
         }

         ++this.currentRouteIndex;
         return true;
      } else {
         return false;
      }
   }

   public boolean isLastNode() {
      return this.route.isEmpty() || this.currentRouteIndex >= this.route.size();
   }

   public Status getStatus() {
      return this.status;
   }

   public void setStatus(Status status) {
      this.status = status;
   }

   public long getStartTimeMillis() {
      return this.startTimeMillis;
   }

   public UUID getArrowDisplayUuid() {
      return this.arrowDisplayUuid;
   }

   public void setArrowDisplayUuid(UUID arrowDisplayUuid) {
      this.arrowDisplayUuid = arrowDisplayUuid;
   }

   public double getCrossTrackDistanceSquared(Location playerLoc) {
      if (playerLoc != null && playerLoc.getWorld().equals(this.destinationLocation.getWorld())) {
         GpsNode node = this.getCurrentTargetNode();
         Location targetLoc;
         if (node != null && node.toLocation() != null) {
            targetLoc = node.toLocation();
         } else {
            targetLoc = this.destinationLocation;
         }

         Vector start = this.segmentStartLocation.toVector();
         Vector end = targetLoc.toVector();
         Vector point = playerLoc.toVector();
         Vector segment = end.clone().subtract(start);
         double segmentLengthSq = segment.lengthSquared();
         if (segmentLengthSq < 0.001) {
            return point.distanceSquared(start);
         } else {
            double t = point.clone().subtract(start).dot(segment) / segmentLengthSq;
            t = Math.max((double)0.0F, Math.min((double)1.0F, t));
            Vector projection = start.add(segment.multiply(t));
            return point.distanceSquared(projection);
         }
      } else {
         return Double.MAX_VALUE;
      }
   }

   public boolean hasPassedWaypoint(Location playerLoc) {
      GpsNode targetNode = this.getCurrentTargetNode();
      if (targetNode == null) {
         return false;
      } else {
         Location targetLoc = targetNode.toLocation();
         if (targetLoc != null && targetLoc.getWorld().equals(playerLoc.getWorld())) {
            Vector segment = targetLoc.toVector().subtract(this.segmentStartLocation.toVector());
            if (segment.lengthSquared() < 0.001) {
               return false;
            } else {
               Vector toPlayer = playerLoc.toVector().subtract(targetLoc.toVector());
               return segment.dot(toPlayer) > (double)0.0F;
            }
         } else {
            return false;
         }
      }
   }

   public double getRemainingDistance(Location playerLocation) {
      if (playerLocation != null && playerLocation.getWorld().equals(this.destinationLocation.getWorld())) {
         if (!this.route.isEmpty() && this.currentRouteIndex < this.route.size()) {
            GpsNode currentNode = this.getCurrentTargetNode();
            Location currentLoc = currentNode != null ? currentNode.toLocation() : null;
            if (currentLoc == null) {
               return playerLocation.distance(this.destinationLocation);
            } else {
               double total = playerLocation.distance(currentLoc);

               for(int i = this.currentRouteIndex; i < this.route.size() - 1; ++i) {
                  GpsNode a = (GpsNode)this.route.get(i);
                  GpsNode b = (GpsNode)this.route.get(i + 1);
                  total += a.distance(b);
               }

               GpsNode lastNode = (GpsNode)this.route.get(this.route.size() - 1);
               Location lastLoc = lastNode.toLocation();
               if (lastLoc != null) {
                  total += lastLoc.distance(this.destinationLocation);
               }

               return total;
            }
         } else {
            return playerLocation.distance(this.destinationLocation);
         }
      } else {
         return (double)0.0F;
      }
   }

   public static enum Status {
      NAVIGATING,
      RECALCULATING,
      ARRIVED,
      CANCELLED;

      // $FF: synthetic method
      private static Status[] $values() {
         return new Status[]{NAVIGATING, RECALCULATING, ARRIVED, CANCELLED};
      }
   }
}
