package com.roleplay.phone.voice;

import com.roleplay.phone.config.PhoneConfig;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.Group.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class VoiceChatManager implements PhoneVoiceBridge {
   private final PhoneConfig phoneConfig;
   private final Map<UUID, Group> previousGroups = new ConcurrentHashMap();
   private final Map<UUID, Group> activeCallGroups = new ConcurrentHashMap();

   public VoiceChatManager(PhoneConfig phoneConfig) {
      this.phoneConfig = phoneConfig;
   }

   public void onCallConnected(Player caller, Player callee) {
      if (this.phoneConfig.isVoiceChatEnabled() && PhoneVoicePlugin.isAvailable()) {
         VoicechatServerApi api = PhoneVoicePlugin.getServerApi();
         if (api != null) {
            VoicechatConnection callerConn = api.getConnectionOf(caller.getUniqueId());
            VoicechatConnection calleeConn = api.getConnectionOf(callee.getUniqueId());
            if (callerConn != null && calleeConn != null) {
               if (callerConn.getGroup() != null) {
                  this.previousGroups.put(caller.getUniqueId(), callerConn.getGroup());
               }

               if (calleeConn.getGroup() != null) {
                  this.previousGroups.put(callee.getUniqueId(), calleeConn.getGroup());
               }

               Group.Type groupType = this.phoneConfig.getVoiceGroupType().equalsIgnoreCase("ISOLATED") ? Type.ISOLATED : Type.OPEN;
               Group.Builder var10000 = api.groupBuilder().setPersistent(false);
               String var10001 = caller.getName();
               Group callGroup = var10000.setName("Call: " + var10001 + " & " + callee.getName()).setType(groupType).build();
               callerConn.setGroup(callGroup);
               calleeConn.setGroup(callGroup);
               this.activeCallGroups.put(caller.getUniqueId(), callGroup);
               this.activeCallGroups.put(callee.getUniqueId(), callGroup);
            }
         }
      }
   }

   public void onCallDisconnected(UUID callerUuid, UUID calleeUuid) {
      if (this.phoneConfig.isVoiceChatEnabled() && PhoneVoicePlugin.isAvailable()) {
         VoicechatServerApi api = PhoneVoicePlugin.getServerApi();
         if (api != null) {
            this.restorePlayer(api, callerUuid);
            this.restorePlayer(api, calleeUuid);
         }
      }
   }

   private void restorePlayer(VoicechatServerApi api, UUID uuid) {
      this.activeCallGroups.remove(uuid);
      VoicechatConnection conn = api.getConnectionOf(uuid);
      if (conn == null) {
         this.previousGroups.remove(uuid);
      } else {
         Group previous = (Group)this.previousGroups.remove(uuid);
         conn.setGroup(previous);
      }
   }
}
