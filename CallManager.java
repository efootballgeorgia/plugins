package com.roleplay.phone.call;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.config.PhoneConfig;
import com.roleplay.phone.gui.PhoneGuiManager;
import com.roleplay.phone.gui.PhoneHolder;
import com.roleplay.phone.item.PhoneItemFactory;
import com.roleplay.phone.model.CallSession;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.storage.ProfileManager;
import com.roleplay.phone.voice.PhoneVoiceBridge;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class CallManager {
   private final PhonePlugin plugin;
   private final ProfileManager profileManager;
   private final PhoneConfig phoneConfig;
   private PhoneVoiceBridge voiceBridge;
   private PhoneGuiManager guiManager;
   private final Map<UUID, CallSession> sessionsByPlayer = new ConcurrentHashMap();
   private final Set<CallSession> activeSessions = ConcurrentHashMap.newKeySet();

   public CallManager(PhonePlugin plugin, ProfileManager profileManager, PhoneConfig phoneConfig) {
      this.plugin = plugin;
      this.profileManager = profileManager;
      this.phoneConfig = phoneConfig;
   }

   public void setVoiceBridge(PhoneVoiceBridge voiceBridge) {
      this.voiceBridge = voiceBridge;
   }

   public void setGuiManager(PhoneGuiManager guiManager) {
      this.guiManager = guiManager;
   }

   public DialResult startCall(Player caller, String callerNumber, String targetNumber) {
      if (this.isInCall(caller.getUniqueId())) {
         return CallManager.DialResult.ALREADY_IN_CALL;
      } else if (callerNumber.equalsIgnoreCase(targetNumber)) {
         return CallManager.DialResult.SELF_CALL;
      } else {
         PhoneProfile callerProfile = this.profileManager.getProfile(callerNumber);
         if (callerProfile != null && callerProfile.isAirplaneMode()) {
            return CallManager.DialResult.AIRPLANE_MODE;
         } else {
            PhoneProfile targetProfile = this.profileManager.getProfile(targetNumber);
            if (targetProfile == null) {
               return CallManager.DialResult.NUMBER_NOT_FOUND;
            } else if (targetProfile.isAirplaneMode()) {
               return CallManager.DialResult.AIRPLANE_MODE;
            } else {
               Player targetPlayer = this.findCarrierOfPhone(targetNumber);
               if (targetPlayer != null && targetPlayer.isOnline()) {
                  if (this.isInCall(targetPlayer.getUniqueId())) {
                     return CallManager.DialResult.BUSY;
                  } else {
                     CallSession session = new CallSession(caller.getUniqueId(), targetPlayer.getUniqueId());
                     this.sessionsByPlayer.put(caller.getUniqueId(), session);
                     this.sessionsByPlayer.put(targetPlayer.getUniqueId(), session);
                     this.activeSessions.add(session);
                     if (this.guiManager != null) {
                        this.guiManager.openIncomingCall(targetPlayer, targetNumber, caller.getName(), callerNumber);
                     }

                     this.startRingingTask(session, caller, targetPlayer, targetProfile.isSilentMode(), callerNumber, targetNumber);
                     return CallManager.DialResult.SUCCESS;
                  }
               } else {
                  return CallManager.DialResult.TARGET_NOT_CARRYING_PHONE;
               }
            }
         }
      }
   }

   private void startRingingTask(final CallSession session, final Player caller, final Player callee, final boolean calleeSilent, String callerNumber, final String calleeNumber) {
      final int timeoutSeconds = this.phoneConfig.getCallTimeoutSeconds();
      BukkitTask task = (new BukkitRunnable() {
         int elapsedSeconds = 0;

         public void run() {
            if (session.getState() != CallSession.CallState.RINGING) {
               this.cancel();
            } else if (caller.isOnline() && callee.isOnline() && CallManager.this.isCarryingDevice(callee, calleeNumber)) {
               if (this.elapsedSeconds >= timeoutSeconds) {
                  caller.sendMessage(Component.text("No answer from " + callee.getName(), NamedTextColor.RED));
                  callee.sendMessage(Component.text("Missed call from " + caller.getName(), NamedTextColor.YELLOW));
                  CallManager.this.endCall(session, "No answer.");
                  this.cancel();
               } else {
                  Player var10000 = caller;
                  String var10001 = callee.getName();
                  var10000.sendActionBar(Component.text("Calling " + var10001 + "... (" + (timeoutSeconds - this.elapsedSeconds) + "s)", NamedTextColor.YELLOW));
                  caller.playSound(caller.getLocation(), CallManager.this.phoneConfig.getRingbackSound(), CallManager.this.phoneConfig.getRingbackVolume(), CallManager.this.phoneConfig.getRingbackPitch());
                  callee.sendActionBar(Component.text("Incoming call from " + caller.getName() + "!", NamedTextColor.GREEN));
                  if (!calleeSilent) {
                     callee.playSound(callee.getLocation(), CallManager.this.phoneConfig.getRingtoneSound(), CallManager.this.phoneConfig.getRingtoneVolume(), CallManager.this.phoneConfig.getRingtonePitch());
                  }

                  this.elapsedSeconds += 2;
               }
            } else {
               CallManager.this.endCall(session, "Call dropped: recipient unavailable.");
               this.cancel();
            }
         }
      }).runTaskTimer(this.plugin, 0L, 40L);
      session.setRingingTask(task);
   }

   public boolean acceptCall(Player callee) {
      CallSession session = (CallSession)this.sessionsByPlayer.get(callee.getUniqueId());
      if (session != null && session.getState() == CallSession.CallState.RINGING) {
         if (!session.getTargetUuid().equals(callee.getUniqueId())) {
            return false;
         } else {
            session.cancelRingtone();
            session.setState(CallSession.CallState.ACTIVE);
            Player caller = Bukkit.getPlayer(session.getCallerUuid());
            if (caller != null && caller.isOnline()) {
               Component connectedMsg = Component.text("✔ Call Connected. Voice and text relay active.", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true);
               caller.sendMessage(connectedMsg);
               callee.sendMessage(connectedMsg);
               caller.playSound(caller.getLocation(), this.phoneConfig.getConnectedSound(), this.phoneConfig.getConnectedVolume(), this.phoneConfig.getConnectedPitch());
               callee.playSound(callee.getLocation(), this.phoneConfig.getConnectedSound(), this.phoneConfig.getConnectedVolume(), this.phoneConfig.getConnectedPitch());
               if (this.voiceBridge != null) {
                  this.voiceBridge.onCallConnected(caller, callee);
               }

               if (this.guiManager != null) {
                  String callerPhone = this.getFirstCarriedPhoneNumber(caller);
                  String calleePhone = this.getFirstCarriedPhoneNumber(callee);
                  if (callerPhone != null) {
                     this.guiManager.openActiveCall(caller, callerPhone, callee.getName(), calleePhone != null ? calleePhone : "Unknown");
                  }

                  if (calleePhone != null) {
                     this.guiManager.openActiveCall(callee, calleePhone, caller.getName(), callerPhone != null ? callerPhone : "Unknown");
                  }
               }

               return true;
            } else {
               this.endCall(session, "Caller disconnected.");
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void declineCall(Player player) {
      CallSession session = (CallSession)this.sessionsByPlayer.get(player.getUniqueId());
      if (session != null && session.getState() == CallSession.CallState.RINGING) {
         this.endCall(session, "Call declined.");
      }

   }

   public void endCall(UUID playerUuid, String reason) {
      CallSession session = (CallSession)this.sessionsByPlayer.get(playerUuid);
      if (session != null) {
         this.endCall(session, reason);
      }

   }

   public void endCall(CallSession session, String reason) {
      session.cancelRingtone();
      session.setState(CallSession.CallState.TERMINATED);
      this.sessionsByPlayer.remove(session.getCallerUuid());
      this.sessionsByPlayer.remove(session.getTargetUuid());
      this.activeSessions.remove(session);
      if (this.voiceBridge != null) {
         this.voiceBridge.onCallDisconnected(session.getCallerUuid(), session.getTargetUuid());
      }

      Player caller = Bukkit.getPlayer(session.getCallerUuid());
      Player callee = Bukkit.getPlayer(session.getTargetUuid());
      Component endNotice = Component.text("✖ Call ended: " + reason, NamedTextColor.RED);
      if (caller != null && caller.isOnline()) {
         caller.sendMessage(endNotice);
         caller.playSound(caller.getLocation(), this.phoneConfig.getHangupSound(), this.phoneConfig.getHangupVolume(), this.phoneConfig.getHangupPitch());
         InventoryHolder var7 = caller.getOpenInventory().getTopInventory().getHolder();
         if (var7 instanceof PhoneHolder) {
            PhoneHolder holder = (PhoneHolder)var7;
            if (holder.getMenuType() == PhoneHolder.MenuType.ACTIVE_CALL || holder.getMenuType() == PhoneHolder.MenuType.INCOMING_CALL) {
               caller.closeInventory();
            }
         }
      }

      if (callee != null && callee.isOnline()) {
         callee.sendMessage(endNotice);
         callee.playSound(callee.getLocation(), this.phoneConfig.getHangupSound(), this.phoneConfig.getHangupVolume(), this.phoneConfig.getHangupPitch());
         InventoryHolder var9 = callee.getOpenInventory().getTopInventory().getHolder();
         if (var9 instanceof PhoneHolder) {
            PhoneHolder holder = (PhoneHolder)var9;
            if (holder.getMenuType() == PhoneHolder.MenuType.ACTIVE_CALL || holder.getMenuType() == PhoneHolder.MenuType.INCOMING_CALL) {
               callee.closeInventory();
            }
         }
      }

   }

   public void terminateAllCalls() {
      for(CallSession session : this.activeSessions) {
         this.endCall(session, "Server reload/shutdown.");
      }

   }

   public boolean isInCall(UUID uuid) {
      return this.sessionsByPlayer.containsKey(uuid);
   }

   public CallSession getSession(UUID uuid) {
      return (CallSession)this.sessionsByPlayer.get(uuid);
   }

   public void relayMessage(Player speaker, Component chatComponent) {
      CallSession session = (CallSession)this.sessionsByPlayer.get(speaker.getUniqueId());
      if (session != null && session.getState() == CallSession.CallState.ACTIVE) {
         UUID partnerUuid = session.getOtherParty(speaker.getUniqueId());
         if (partnerUuid != null) {
            Player partner = Bukkit.getPlayer(partnerUuid);
            if (partner != null && partner.isOnline()) {
               Component prefix = this.phoneConfig.getChatPrefix();
               partner.sendMessage(prefix.append(speaker.displayName()).append(Component.text(": ", NamedTextColor.GRAY)).append(chatComponent));
               speaker.sendMessage(prefix.append(Component.text("You: ", NamedTextColor.GRAY)).append(chatComponent));
            }

         }
      }
   }

   public Player findCarrierOfPhone(String phoneNumber) {
      for(Player player : Bukkit.getOnlinePlayers()) {
         if (this.isCarryingDevice(player, phoneNumber)) {
            return player;
         }
      }

      return null;
   }

   public boolean isCarryingDevice(Player player, String phoneNumber) {
      if (this.phoneConfig.isRequireInHand()) {
         ItemStack mainHand = player.getInventory().getItemInMainHand();
         if (PhoneItemFactory.isPhone(mainHand)) {
            String number = PhoneItemFactory.getPhoneNumber(mainHand);
            return phoneNumber.equalsIgnoreCase(number);
         } else {
            return false;
         }
      } else {
         for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && PhoneItemFactory.isPhone(item)) {
               String number = PhoneItemFactory.getPhoneNumber(item);
               if (phoneNumber.equalsIgnoreCase(number)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public String getFirstCarriedPhoneNumber(Player player) {
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      if (PhoneItemFactory.isPhone(mainHand)) {
         return PhoneItemFactory.getPhoneNumber(mainHand);
      } else if (this.phoneConfig.isRequireInHand()) {
         return null;
      } else {
         for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && PhoneItemFactory.isPhone(item)) {
               return PhoneItemFactory.getPhoneNumber(item);
            }
         }

         return null;
      }
   }

   public static enum DialResult {
      SUCCESS,
      BUSY,
      AIRPLANE_MODE,
      OFFLINE,
      SELF_CALL,
      ALREADY_IN_CALL,
      NUMBER_NOT_FOUND,
      TARGET_NOT_CARRYING_PHONE;

      // $FF: synthetic method
      private static DialResult[] $values() {
         return new DialResult[]{SUCCESS, BUSY, AIRPLANE_MODE, OFFLINE, SELF_CALL, ALREADY_IN_CALL, NUMBER_NOT_FOUND, TARGET_NOT_CARRYING_PHONE};
      }
   }
}
