package com.roleplay.phone.model;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.UUID;
import org.bukkit.scheduler.BukkitTask;

public class CallSession {
   private final UUID callerUuid;
   private final UUID targetUuid;
   private CallState state;
   private final long startTimeMillis;
   private BukkitTask ringingTask;
   private ScheduledTask foliaRingingTask;

   public CallSession(UUID callerUuid, UUID targetUuid) {
      this.callerUuid = callerUuid;
      this.targetUuid = targetUuid;
      this.state = CallSession.CallState.RINGING;
      this.startTimeMillis = System.currentTimeMillis();
   }

   public UUID getCallerUuid() {
      return this.callerUuid;
   }

   public UUID getTargetUuid() {
      return this.targetUuid;
   }

   public CallState getState() {
      return this.state;
   }

   public void setState(CallState state) {
      this.state = state;
   }

   public long getStartTimeMillis() {
      return this.startTimeMillis;
   }

   public BukkitTask getRingingTask() {
      return this.ringingTask;
   }

   public void setRingingTask(BukkitTask ringingTask) {
      this.ringingTask = ringingTask;
   }

   public ScheduledTask getFoliaRingingTask() {
      return this.foliaRingingTask;
   }

   public void setFoliaRingingTask(ScheduledTask foliaRingingTask) {
      this.foliaRingingTask = foliaRingingTask;
   }

   public void cancelRingtone() {
      if (this.ringingTask != null && !this.ringingTask.isCancelled()) {
         this.ringingTask.cancel();
      }

      if (this.foliaRingingTask != null && !this.foliaRingingTask.isCancelled()) {
         this.foliaRingingTask.cancel();
      }

   }

   public boolean involves(UUID uuid) {
      return this.callerUuid.equals(uuid) || this.targetUuid.equals(uuid);
   }

   public UUID getOtherParty(UUID uuid) {
      if (this.callerUuid.equals(uuid)) {
         return this.targetUuid;
      } else {
         return this.targetUuid.equals(uuid) ? this.callerUuid : null;
      }
   }

   public static enum CallState {
      RINGING,
      ACTIVE,
      TERMINATED;

      // $FF: synthetic method
      private static CallState[] $values() {
         return new CallState[]{RINGING, ACTIVE, TERMINATED};
      }
   }
}
