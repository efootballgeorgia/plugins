package com.roleplay.phone.sms;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.call.CallManager;
import com.roleplay.phone.lang.MessageManager;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.sms.model.TextMessage;
import com.roleplay.phone.storage.ProfileManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SmsManager {
   private final PhonePlugin plugin;
   private final ProfileManager profileManager;
   private final MessageManager messageManager;
   private final CallManager callManager;
   private final Map<UUID, ComposeSession> activeComposeSessions = new ConcurrentHashMap();

   public SmsManager(PhonePlugin plugin, ProfileManager profileManager, MessageManager messageManager, CallManager callManager) {
      this.plugin = plugin;
      this.profileManager = profileManager;
      this.messageManager = messageManager;
      this.callManager = callManager;
   }

   public SendResult sendSms(Player senderPlayer, String senderNumber, String targetNumber, String content) {
      if (content != null && !content.trim().isEmpty()) {
         if (senderNumber.equalsIgnoreCase(targetNumber)) {
            return SmsManager.SendResult.SELF_MESSAGE;
         } else {
            PhoneProfile senderProfile = this.profileManager.getProfile(senderNumber);
            if (senderProfile != null && senderProfile.isAirplaneMode()) {
               return SmsManager.SendResult.SENDER_AIRPLANE_MODE;
            } else {
               PhoneProfile targetProfile = this.profileManager.getProfile(targetNumber);
               if (targetProfile == null) {
                  return SmsManager.SendResult.TARGET_NOT_FOUND;
               } else if (targetProfile.isAirplaneMode()) {
                  return SmsManager.SendResult.TARGET_AIRPLANE_MODE;
               } else {
                  String trimmed = content.trim();
                  TextMessage outgoingMsg = new TextMessage(senderNumber, targetNumber, trimmed);
                  outgoingMsg.setRead(true);
                  TextMessage incomingMsg = new TextMessage(outgoingMsg.getId(), senderNumber, targetNumber, trimmed, outgoingMsg.getTimestampMillis(), false);
                  if (senderProfile != null) {
                     senderProfile.addMessage(outgoingMsg);
                     this.profileManager.saveProfile(senderProfile);
                  }

                  targetProfile.addMessage(incomingMsg);
                  this.profileManager.saveProfile(targetProfile);
                  Player targetPlayer = this.callManager.findCarrierOfPhone(targetNumber);
                  if (targetPlayer != null && targetPlayer.isOnline()) {
                     this.deliverRealtimeNotification(targetPlayer, targetProfile, senderProfile, senderNumber, trimmed);
                     return SmsManager.SendResult.SUCCESS;
                  } else {
                     return SmsManager.SendResult.SUCCESS_STORED_OFFLINE;
                  }
               }
            }
         }
      } else {
         return SmsManager.SendResult.EMPTY_MESSAGE;
      }
   }

   private void deliverRealtimeNotification(Player recipient, PhoneProfile targetProfile, PhoneProfile senderProfile, String senderNumber, String message) {
      String displayName = targetProfile.resolveContactName(senderNumber);
      if (displayName.equalsIgnoreCase(senderNumber) && senderProfile != null && senderProfile.getCreatorUuid() != null) {
         Player sender = Bukkit.getPlayer(senderProfile.getCreatorUuid());
         if (sender != null) {
            displayName = sender.getName();
         }
      }

      this.messageManager.sendActionBar(recipient, "sms.message-received-actionbar", "sender", displayName);
      if (!targetProfile.isSilentMode()) {
         this.messageManager.sendRaw(recipient, "sms.message-received-chat", "sender", displayName, "message", message);
         recipient.playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F, 1.8F);
      }

   }

   public void startComposePrompt(Player player, String deviceNumber, String targetNumber) {
      this.activeComposeSessions.put(player.getUniqueId(), new ComposeSession(deviceNumber, targetNumber));
      player.closeInventory();
      PhoneProfile profile = this.profileManager.getProfile(deviceNumber);
      String targetDisplay = profile != null ? profile.resolveContactName(targetNumber) : targetNumber;
      this.messageManager.sendRaw(player, "sms.compose-header");
      this.messageManager.sendRaw(player, "sms.compose-prompt", "target", targetDisplay, "number", targetNumber);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5F, 1.6F);
   }

   public boolean isComposing(UUID playerUuid) {
      return this.activeComposeSessions.containsKey(playerUuid);
   }

   public ComposeSession getComposeSession(UUID playerUuid) {
      return (ComposeSession)this.activeComposeSessions.get(playerUuid);
   }

   public void cancelComposePrompt(UUID playerUuid) {
      this.activeComposeSessions.remove(playerUuid);
   }

   public void notifyUnreadOnJoin(Player player) {
      String carriedNumber = this.callManager.getFirstCarriedPhoneNumber(player);
      if (carriedNumber != null) {
         PhoneProfile profile = this.profileManager.getProfile(carriedNumber);
         if (profile != null) {
            int unread = profile.getUnreadCount();
            if (unread > 0) {
               player.sendMessage(Component.text("\ud83d\udcac You have " + unread + " unread text message" + (unread == 1 ? "" : "s") + "! Open your phone to view.", NamedTextColor.AQUA));
               player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6F, 1.4F);
            }

         }
      }
   }

   public static record ComposeSession(String deviceNumber, String targetNumber) {
   }

   public static enum SendResult {
      SUCCESS,
      SUCCESS_STORED_OFFLINE,
      TARGET_NOT_FOUND,
      SENDER_AIRPLANE_MODE,
      TARGET_AIRPLANE_MODE,
      SELF_MESSAGE,
      EMPTY_MESSAGE;

      // $FF: synthetic method
      private static SendResult[] $values() {
         return new SendResult[]{SUCCESS, SUCCESS_STORED_OFFLINE, TARGET_NOT_FOUND, SENDER_AIRPLANE_MODE, TARGET_AIRPLANE_MODE, SELF_MESSAGE, EMPTY_MESSAGE};
      }
   }
}
