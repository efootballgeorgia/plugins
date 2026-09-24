package com.roleplay.phone.listener;

import com.roleplay.phone.call.CallManager;
import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.model.GpsNode;
import com.roleplay.phone.gps.model.GpsSession;
import com.roleplay.phone.gps.visual.GpsArrowRenderer;
import com.roleplay.phone.gui.PhoneGuiManager;
import com.roleplay.phone.gui.PhoneHolder;
import com.roleplay.phone.gui.config.GuiConfig;
import com.roleplay.phone.item.PhoneItemFactory;
import com.roleplay.phone.model.CallSession;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.sms.SmsManager;
import com.roleplay.phone.storage.ProfileManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PhoneInteractionListener implements Listener {
   private final PhoneGuiManager guiManager;
   private final ProfileManager profileManager;
   private final CallManager callManager;
   private final GpsManager gpsManager;
   private final GpsArrowRenderer arrowRenderer;
   private final SmsManager smsManager;

   public PhoneInteractionListener(PhoneGuiManager guiManager, ProfileManager profileManager, CallManager callManager, GpsManager gpsManager, GpsArrowRenderer arrowRenderer, SmsManager smsManager) {
      this.guiManager = guiManager;
      this.profileManager = profileManager;
      this.callManager = callManager;
      this.gpsManager = gpsManager;
      this.arrowRenderer = arrowRenderer;
      this.smsManager = smsManager;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (this.callManager.isInCall(player.getUniqueId())) {
         ItemStack dropped = event.getItemDrop().getItemStack();
         if (PhoneItemFactory.isPhone(dropped)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("✖ You cannot drop your phone while on a call!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6F, 0.5F);
         }
      }

   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Action action = event.getAction();
         if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (PhoneItemFactory.isPhone(item)) {
               event.setCancelled(true);
               Player player = event.getPlayer();
               String deviceNumber = PhoneItemFactory.getPhoneNumber(item);
               if (deviceNumber == null) {
                  player.sendMessage(Component.text("This phone has no registered SIM/number.", NamedTextColor.RED));
               } else {
                  if (this.callManager.isInCall(player.getUniqueId())) {
                     CallSession session = this.callManager.getSession(player.getUniqueId());
                     if (session != null && session.getState() == CallSession.CallState.ACTIVE) {
                        UUID otherUuid = session.getOtherParty(player.getUniqueId());
                        Player otherPlayer = otherUuid != null ? Bukkit.getPlayer(otherUuid) : null;
                        this.guiManager.openActiveCall(player, deviceNumber, otherPlayer != null ? otherPlayer.getName() : "Unknown", "Connected");
                        return;
                     }
                  }

                  this.guiManager.openMainMenu(player, deviceNumber);
                  player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5F, 1.5F);
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onInventoryClick(InventoryClickEvent event) {
      Player player = (Player)event.getWhoClicked();
      if (this.callManager.isInCall(player.getUniqueId()) && (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP)) {
         ItemStack current = event.getCurrentItem();
         if (PhoneItemFactory.isPhone(current)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("✖ You cannot drop your phone while on a call!", NamedTextColor.RED));
            return;
         }
      }

      InventoryHolder var4 = event.getInventory().getHolder();
      if (var4 instanceof PhoneHolder holder) {
         event.setCancelled(true);
         if (event.getClickedInventory() == event.getView().getTopInventory()) {
            String deviceNumber = holder.getDevicePhoneNumber();
            if (!this.callManager.isCarryingDevice(player, deviceNumber)) {
               player.closeInventory();
               player.sendMessage(Component.text("You are no longer holding this phone device!", NamedTextColor.RED));
            } else {
               int slot = event.getRawSlot();
               PhoneHolder.MenuType menuType = holder.getMenuType();
               GuiConfig guiConfig = this.guiManager.getGuiConfig();
               switch (menuType) {
                  case MAIN_MENU -> this.handleMainMenuClick(player, deviceNumber, slot, event.getClick(), guiConfig);
                  case DIALER -> this.handleDialerClick(player, holder, slot, guiConfig);
                  case CONTACTS -> this.handleContactsClick(player, deviceNumber, slot, event.getClick(), guiConfig);
                  case SMS_INBOX -> this.handleSmsInboxClick(player, deviceNumber, slot, guiConfig);
                  case SMS_CONVERSATION -> this.handleSmsConversationClick(player, holder, slot, guiConfig);
                  case GPS_DESTINATIONS -> this.handleGpsMenuClick(player, deviceNumber, slot, guiConfig);
                  case INCOMING_CALL -> this.handleIncomingCallClick(player, slot, guiConfig);
                  case ACTIVE_CALL -> this.handleActiveCallClick(player, slot, guiConfig);
               }

            }
         }
      }
   }

   private void handleMainMenuClick(Player player, String deviceNumber, int slot, ClickType clickType, GuiConfig config) {
      PhoneProfile profile = this.profileManager.getProfile(deviceNumber);
      if (profile != null) {
         if (slot == config.getAppDialer().getSlot()) {
            config.getAppDialer().playSound(player);
            this.guiManager.openDialer(player, deviceNumber, "");
         } else if (slot == config.getAppContacts().getSlot()) {
            config.getAppContacts().playSound(player);
            this.guiManager.openContacts(player, deviceNumber);
         } else if (slot == config.getAppMessages().getSlot()) {
            config.getAppMessages().playSound(player);
            this.guiManager.openSmsInbox(player, deviceNumber);
         } else if (slot == config.getAppGps().getSlot()) {
            config.getAppGps().playSound(player);
            this.guiManager.openGpsMenu(player, deviceNumber);
         } else {
            if (slot == config.getAppSettings().getSlot()) {
               config.getAppSettings().playSound(player);
               if (clickType.isLeftClick()) {
                  profile.setSilentMode(!profile.isSilentMode());
                  player.sendMessage(Component.text("Silent Mode set to: " + (profile.isSilentMode() ? "ON" : "OFF"), NamedTextColor.YELLOW));
               } else if (clickType.isRightClick()) {
                  profile.setAirplaneMode(!profile.isAirplaneMode());
                  player.sendMessage(Component.text("Airplane Mode set to: " + (profile.isAirplaneMode() ? "ON" : "OFF"), NamedTextColor.YELLOW));
               }

               this.profileManager.saveProfile(profile);
               this.guiManager.openMainMenu(player, deviceNumber);
            }

         }
      }
   }

   private void handleDialerClick(Player player, PhoneHolder holder, int slot, GuiConfig config) {
      String deviceNumber = holder.getDevicePhoneNumber();
      StringBuilder buffer = holder.getDialBuffer();
      if (slot == config.getDialerBackButton().getSlot()) {
         config.getDialerBackButton().playSound(player);
         this.guiManager.openMainMenu(player, deviceNumber);
      } else if (slot == config.getDialerBackspace().getSlot()) {
         config.getDialerBackspace().playSound(player);
         if (!buffer.isEmpty()) {
            if (buffer.charAt(buffer.length() - 1) == '-') {
               buffer.deleteCharAt(buffer.length() - 1);
            }

            buffer.deleteCharAt(buffer.length() - 1);
            this.guiManager.openDialer(player, deviceNumber, buffer.toString());
         }

      } else if (slot == config.getDialerCall().getSlot()) {
         config.getDialerCall().playSound(player);
         String targetNumber = buffer.toString();
         CallManager.DialResult result = this.callManager.startCall(player, deviceNumber, targetNumber);
         this.handleDialResult(player, result, targetNumber);
      } else {
         for(Map.Entry<String, Integer> entry : config.getKeypadSlots().entrySet()) {
            if (slot == (Integer)entry.getValue()) {
               config.getKeypadClickSound().play(player);
               this.appendDigit(player, deviceNumber, buffer, (String)entry.getKey());
               return;
            }
         }

      }
   }

   private void appendDigit(Player player, String deviceNumber, StringBuilder buffer, String digit) {
      if (buffer.length() < 8) {
         if (buffer.length() == 3) {
            buffer.append("-");
         }

         buffer.append(digit);
         this.guiManager.openDialer(player, deviceNumber, buffer.toString());
      }
   }

   private void handleContactsClick(Player player, String deviceNumber, int slot, ClickType click, GuiConfig config) {
      if (slot == config.getContactsBackButton().getSlot()) {
         config.getContactsBackButton().playSound(player);
         this.guiManager.openMainMenu(player, deviceNumber);
      } else if (slot == config.getContactsAddButton().getSlot()) {
         config.getContactsAddButton().playSound(player);
         PhoneChatCallListener.startContactOrSmsPrompt(player, deviceNumber, false, "");
      } else {
         ItemStack clicked = player.getOpenInventory().getTopInventory().getItem(slot);
         if (clicked != null && clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.lore() != null && !meta.lore().isEmpty()) {
               String contactName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
               List<Component> lore = meta.lore();
               String plainNumberLine = PlainTextComponentSerializer.plainText().serialize((Component)lore.get(0));
               String contactNumber = plainNumberLine.replace("Number: ", "").trim();
               PhoneProfile profile = this.profileManager.getProfile(deviceNumber);
               if (profile != null) {
                  if (click == ClickType.MIDDLE) {
                     config.getContactClickSound().play(player);
                     this.guiManager.openSmsConversation(player, deviceNumber, contactNumber);
                  } else if (click.isRightClick()) {
                     config.getContactDeleteSound().play(player);
                     profile.removeContact(contactName);
                     this.profileManager.saveProfile(profile);
                     player.sendMessage(Component.text("✔ Deleted contact: " + contactName, NamedTextColor.RED));
                     this.guiManager.openContacts(player, deviceNumber);
                  } else if (click.isLeftClick()) {
                     config.getContactClickSound().play(player);
                     CallManager.DialResult result = this.callManager.startCall(player, deviceNumber, contactNumber);
                     this.handleDialResult(player, result, contactNumber);
                  }

               }
            }
         }
      }
   }

   private void handleSmsInboxClick(Player player, String deviceNumber, int slot, GuiConfig config) {
      if (slot == config.getSmsInboxBackButton().getSlot()) {
         config.getSmsInboxBackButton().playSound(player);
         this.guiManager.openMainMenu(player, deviceNumber);
      } else if (slot == config.getSmsInboxComposeButton().getSlot()) {
         config.getSmsInboxComposeButton().playSound(player);
         PhoneChatCallListener.startContactOrSmsPrompt(player, deviceNumber, true, "");
      } else {
         int bottomStart = config.getSmsInboxSize() - 9;
         if (slot >= 0 && slot < bottomStart) {
            PhoneProfile profile = this.profileManager.getProfile(deviceNumber);
            if (profile == null) {
               return;
            }

            List<String> partners = new ArrayList(profile.getConversations().keySet());
            if (slot < partners.size()) {
               config.getSmsThreadClickSound().play(player);
               String targetNumber = (String)partners.get(slot);
               this.guiManager.openSmsConversation(player, deviceNumber, targetNumber);
            }
         }

      }
   }

   private void handleSmsConversationClick(Player player, PhoneHolder holder, int slot, GuiConfig config) {
      String deviceNumber = holder.getDevicePhoneNumber();
      String partnerNumber = holder.getTargetConversationNumber();
      if (slot == config.getSmsConvBackButton().getSlot()) {
         config.getSmsConvBackButton().playSound(player);
         this.guiManager.openSmsInbox(player, deviceNumber);
      } else {
         if (slot == config.getSmsConvReplyButton().getSlot()) {
            config.getSmsConvReplyButton().playSound(player);
            this.smsManager.startComposePrompt(player, deviceNumber, partnerNumber);
         }

      }
   }

   private void handleGpsMenuClick(Player player, String deviceNumber, int slot, GuiConfig config) {
      if (slot == config.getGpsBackButton().getSlot()) {
         config.getGpsBackButton().playSound(player);
         this.guiManager.openMainMenu(player, deviceNumber);
      } else if (slot == config.getGpsCancelButton().getSlot() && this.gpsManager != null && this.gpsManager.isNavigating(player.getUniqueId())) {
         config.getGpsCancelButton().playSound(player);
         this.gpsManager.stopNavigation(player, GpsSession.Status.CANCELLED);
         if (this.arrowRenderer != null) {
            this.arrowRenderer.stopTracking(player.getUniqueId());
         }

         this.guiManager.openGpsMenu(player, deviceNumber);
      } else {
         int bottomStart = config.getGpsSize() - 9;
         if (slot >= 0 && slot < bottomStart && this.gpsManager != null) {
            List<GpsNode> destinations = this.gpsManager.getDestinations();
            if (slot < destinations.size()) {
               config.getGpsDestinationClickSound().play(player);
               GpsNode selected = (GpsNode)destinations.get(slot);
               boolean success = this.gpsManager.startNavigation(player, selected);
               if (success) {
                  GpsSession session = this.gpsManager.getSession(player.getUniqueId());
                  if (session != null && this.arrowRenderer != null) {
                     this.arrowRenderer.startTracking(player, session);
                  }

                  player.closeInventory();
               }
            }
         }

      }
   }

   private void handleIncomingCallClick(Player player, int slot, GuiConfig config) {
      if (slot == config.getIncomingAcceptButton().getSlot()) {
         config.getIncomingAcceptButton().playSound(player);
         this.callManager.acceptCall(player);
      } else if (slot == config.getIncomingDeclineButton().getSlot()) {
         config.getIncomingDeclineButton().playSound(player);
         this.callManager.declineCall(player);
         player.closeInventory();
      }

   }

   private void handleActiveCallClick(Player player, int slot, GuiConfig config) {
      if (slot == config.getActiveHangupButton().getSlot()) {
         config.getActiveHangupButton().playSound(player);
         this.callManager.endCall(player.getUniqueId(), "Hung up via phone GUI");
         player.closeInventory();
      }

   }

   private void handleDialResult(Player player, CallManager.DialResult result, String targetNumber) {
      switch (result) {
         case SUCCESS:
            player.sendMessage(Component.text("Dialing " + targetNumber + "...", NamedTextColor.GREEN));
            break;
         case BUSY:
            player.sendMessage(Component.text("Line is currently busy.", NamedTextColor.RED));
            break;
         case AIRPLANE_MODE:
            player.sendMessage(Component.text("Cannot connect: device is in Airplane Mode.", NamedTextColor.RED));
            break;
         case OFFLINE:
         case TARGET_NOT_CARRYING_PHONE:
            player.sendMessage(Component.text("The subscriber is unavailable or not carrying their phone.", NamedTextColor.RED));
            break;
         case SELF_CALL:
            player.sendMessage(Component.text("You cannot call this phone itself.", NamedTextColor.RED));
            break;
         case ALREADY_IN_CALL:
            player.sendMessage(Component.text("You are already engaged in a call.", NamedTextColor.RED));
            break;
         case NUMBER_NOT_FOUND:
            player.sendMessage(Component.text("The number dialed is not recognized.", NamedTextColor.RED));
      }

   }
}
