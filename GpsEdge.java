package com.roleplay.phone.gps.model;

import java.util.Objects;

public class GpsEdge {
   private final String nodeA;
   private final String nodeB;
   private final double weight;
   private final boolean bidirectional;

   public GpsEdge(String nodeA, String nodeB, double weight, boolean bidirectional) {
      this.nodeA = nodeA;
      this.nodeB = nodeB;
      this.weight = weight;
      this.bidirectional = bidirectional;
   }

   public static GpsEdge between(GpsNode a, GpsNode b, boolean bidirectional) {
      double dist = a.distance(b);
      return new GpsEdge(a.getId(), b.getId(), dist, bidirectional);
   }

   public String getNodeA() {
      return this.nodeA;
   }

   public String getNodeB() {
      return this.nodeB;
   }

   public double getWeight() {
      return this.weight;
   }

   public boolean isBidirectional() {
      return this.bidirectional;
   }

   public boolean connects(String idA, String idB) {
      if (this.nodeA.equalsIgnoreCase(idA) && this.nodeB.equalsIgnoreCase(idB)) {
         return true;
      } else {
         return this.bidirectional && this.nodeA.equalsIgnoreCase(idB) && this.nodeB.equalsIgnoreCase(idA);
      }
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o instanceof GpsEdge) {
         GpsEdge gpsEdge = (GpsEdge)o;
         if (this.bidirectional != gpsEdge.bidirectional) {
            return false;
         } else {
            return Objects.equals(this.nodeA, gpsEdge.nodeA) && Objects.equals(this.nodeB, gpsEdge.nodeB) || this.bidirectional && Objects.equals(this.nodeA, gpsEdge.nodeB) && Objects.equals(this.nodeB, gpsEdge.nodeA);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.bidirectional ? Objects.hash(new Object[]{this.nodeA}) + Objects.hash(new Object[]{this.nodeB}) : Objects.hash(new Object[]{this.nodeA, this.nodeB});
   }
}
