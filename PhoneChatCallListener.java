package com.roleplay.phone.listener;

import com.roleplay.phone.call.CallManager;
import com.roleplay.phone.gui.PhoneGuiManager;
import com.roleplay.phone.lang.MessageManager;
import com.roleplay.phone.model.CallSession;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.sms.SmsManager;
import com.roleplay.phone.storage.ProfileManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PhoneChatCallListener implements Listener {
   private final Plugin plugin;
   private final CallManager callManager;
   private final ProfileManager profileManager;
   private final PhoneGuiManager guiManager;
   private final SmsManager smsManager;
   private final MessageManager messageManager;
   private static final Map<UUID, PromptSession> activePrompts = new ConcurrentHashMap();

   public PhoneChatCallListener(Plugin plugin, CallManager callManager, ProfileManager profileManager, PhoneGuiManager guiManager, SmsManager smsManager, MessageManager messageManager) {
      this.plugin = plugin;
      this.callManager = callManager;
      this.profileManager = profileManager;
      this.guiManager = guiManager;
      this.smsManager = smsManager;
      this.messageManager = messageManager;
   }

   public static void startContactOrSmsPrompt(Player player, String deviceNumber, boolean isSms, String targetNumber) {
      activePrompts.put(player.getUniqueId(), new PromptSession(deviceNumber, isSms, targetNumber));
      player.closeInventory();
      player.sendMessage(Component.empty());
      if (isSms) {
         player.sendMessage(Component.text("--- New Text Message ---", NamedTextColor.GOLD));
         player.sendMessage(Component.text("Type: ", NamedTextColor.GRAY).append(Component.text("<PhoneNumber> <Message...>", NamedTextColor.YELLOW)));
         player.sendMessage(Component.text("Example: ", NamedTextColor.DARK_GRAY).append(Component.text("555-1234 Hey, are you at the bank?", NamedTextColor.WHITE)));
      } else {
         player.sendMessage(Component.text("--- New Contact Setup ---", NamedTextColor.GOLD));
         player.sendMessage(Component.text("Type the phone number to add: ", NamedTextColor.GRAY).append(Component.text("<PhoneNumber>", NamedTextColor.YELLOW)));
         player.sendMessage(Component.text("Example: ", NamedTextColor.DARK_GRAY).append(Component.text("555-1234", NamedTextColor.WHITE)));
      }

      player.sendMessage(Component.text("Or type 'cancel' to abort.", NamedTextColor.RED));
      player.sendMessage(Component.empty());
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6F, 1.5F);
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onAsyncChat(AsyncChatEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      String raw = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
      if (this.smsManager.isComposing(uuid)) {
         event.setCancelled(true);
         SmsManager.ComposeSession session = this.smsManager.getComposeSession(uuid);
         this.smsManager.cancelComposePrompt(uuid);
         if (raw.equalsIgnoreCase("cancel")) {
            this.messageManager.send(player, "sms.compose-cancelled");
            Bukkit.getScheduler().runTask(this.plugin, () -> this.guiManager.openSmsConversation(player, session.deviceNumber(), session.targetNumber()));
         } else {
            SmsManager.SendResult result = this.smsManager.sendSms(player, session.deviceNumber(), session.targetNumber(), raw);
            this.handleSmsResultFeedback(player, result, session.targetNumber());
            Bukkit.getScheduler().runTask(this.plugin, () -> this.guiManager.openSmsConversation(player, session.deviceNumber(), session.targetNumber()));
         }
      } else {
         PromptSession prompt = (PromptSession)activePrompts.get(uuid);
         if (prompt != null) {
            event.setCancelled(true);
            activePrompts.remove(uuid);
            if (raw.equalsIgnoreCase("cancel")) {
               player.sendMessage(Component.text("Operation cancelled.", NamedTextColor.YELLOW));
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  if (prompt.isSms()) {
                     this.guiManager.openSmsInbox(player, prompt.deviceNumber());
                  } else {
                     this.guiManager.openContacts(player, prompt.deviceNumber());
                  }

               });
            } else if (prompt.isSms()) {
               String[] parts = raw.split("\\s+", 2);
               if (parts.length < 2) {
                  player.sendMessage(Component.text("✖ Invalid format. Expected: <PhoneNumber> <Message>", NamedTextColor.RED));
                  Bukkit.getScheduler().runTask(this.plugin, () -> this.guiManager.openSmsInbox(player, prompt.deviceNumber()));
               } else {
                  String targetNum = parts[0];
                  String textBody = parts[1];
                  SmsManager.SendResult result = this.smsManager.sendSms(player, prompt.deviceNumber(), targetNum, textBody);
                  this.handleSmsResultFeedback(player, result, targetNum);
                  Bukkit.getScheduler().runTask(this.plugin, () -> this.guiManager.openSmsConversation(player, prompt.deviceNumber(), targetNum));
               }
            } else {
               String targetNumber = raw.trim();
               String ownerName = this.profileManager.getOwnerNameOfNumber(targetNumber);
               if (ownerName != null && !ownerName.isBlank()) {
                  PhoneProfile profile = this.profileManager.getProfile(prompt.deviceNumber());
                  if (profile != null) {
                     profile.addContact(ownerName, targetNumber);
                     this.profileManager.saveProfile(profile);
                     this.messageManager.send(player, "contacts.contact-added", "name", ownerName, "number", targetNumber);
                     player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 2.0F);
                  }

                  Bukkit.getScheduler().runTask(this.plugin, () -> this.guiManager.openContacts(player, prompt.deviceNumber()));
               } else {
                  player.sendMessage(Component.text("✖ No registered player found with phone number " + targetNumber + "!", NamedTextColor.RED));
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.5F);
                  Bukkit.getScheduler().runTask(this.plugin, () -> this.guiManager.openContacts(player, prompt.deviceNumber()));
               }
            }
         } else {
            CallSession session = this.callManager.getSession(player.getUniqueId());
            if (session != null && session.getState() == CallSession.CallState.ACTIVE) {
               event.setCancelled(true);
               this.callManager.relayMessage(player, event.message());
            }

         }
      }
   }

   private void handleSmsResultFeedback(Player player, SmsManager.SendResult result, String targetNumber) {
      switch (result) {
         case SUCCESS:
            this.messageManager.send(player, "sms.message-sent", "target", targetNumber);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.6F);
            break;
         case SUCCESS_STORED_OFFLINE:
            this.messageManager.send(player, "sms.message-sent", "target", targetNumber);
            this.messageManager.send(player, "sms.stored-offline");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.2F);
            break;
         case TARGET_NOT_FOUND:
            this.messageManager.send(player, "calls.number-not-found");
            break;
         case SENDER_AIRPLANE_MODE:
            this.messageManager.send(player, "calls.airplane-mode-caller");
            break;
         case TARGET_AIRPLANE_MODE:
            this.messageManager.send(player, "sms.target-airplane");
            break;
         case SELF_MESSAGE:
            player.sendMessage(Component.text("✖ You cannot text your own phone number.", NamedTextColor.RED));
            break;
         case EMPTY_MESSAGE:
            this.messageManager.send(player, "sms.empty-message");
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (event.getPlayer().isOnline()) {
            this.smsManager.notifyUnreadOnJoin(event.getPlayer());
         }

      }, 30L);
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      activePrompts.remove(player.getUniqueId());
      this.smsManager.cancelComposePrompt(player.getUniqueId());
      this.callManager.endCall(player.getUniqueId(), "Subscriber disconnected");
   }

   private static record PromptSession(String deviceNumber, boolean isSms, String targetNumber) {
   }
}
