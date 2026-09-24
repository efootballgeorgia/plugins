package com.roleplay.phone.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.model.GpsNode;
import com.roleplay.phone.gps.model.GpsSession;
import com.roleplay.phone.gui.config.GuiConfig;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.sms.SmsManager;
import com.roleplay.phone.sms.model.TextMessage;
import com.roleplay.phone.storage.ProfileManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class PhoneGuiManager {
   private final ProfileManager profileManager;
   private final GpsManager gpsManager;
   private final SmsManager smsManager;
   private final GuiConfig guiConfig;

   public PhoneGuiManager(ProfileManager profileManager, GpsManager gpsManager, SmsManager smsManager, GuiConfig guiConfig) {
      this.profileManager = profileManager;
      this.gpsManager = gpsManager;
      this.smsManager = smsManager;
      this.guiConfig = guiConfig;
   }

   public GuiConfig getGuiConfig() {
      return this.guiConfig;
   }

   public void openMainMenu(Player player, String deviceNumber) {
      PhoneProfile phoneProfile = this.profileManager.getProfile(deviceNumber);
      if (phoneProfile == null) {
         player.sendMessage(Component.text("Invalid device data.", NamedTextColor.RED));
      } else {
         int size = this.guiConfig.getMainMenuSize();
         PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.MAIN_MENU);
         Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getMainMenuTitle());
         holder.setInventory(inv);
         this.fillInventory(inv, size, this.guiConfig.getMainMenuFiller());
         ItemStack infoHead = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta skullMeta = (SkullMeta)infoHead.getItemMeta();
         if (skullMeta != null) {
            PlayerProfile userProfile = Bukkit.createProfile(player.getUniqueId(), player.getName());
            skullMeta.setPlayerProfile(userProfile);
            skullMeta.displayName(Component.text("Device Status", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            skullMeta.lore(List.of((TextComponent)((TextComponent)Component.text("Current Holder: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(Component.text(player.getName(), NamedTextColor.WHITE)), (TextComponent)((TextComponent)Component.text("Device Number: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(Component.text(deviceNumber, NamedTextColor.YELLOW))));
            infoHead.setItemMeta(skullMeta);
         }

         inv.setItem(this.guiConfig.getDeviceInfoSlot(), infoHead);
         inv.setItem(this.guiConfig.getAppDialer().getSlot(), this.guiConfig.getAppDialer().build(this.guiConfig.getMiniMessage()));
         inv.setItem(this.guiConfig.getAppContacts().getSlot(), this.guiConfig.getAppContacts().build(this.guiConfig.getMiniMessage()));
         int unread = phoneProfile.getUnreadCount();
         Component unreadStatus = (Component)(unread > 0 ? Component.text("• " + unread + " New Message" + (unread == 1 ? "" : "s"), NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true) : Component.text("No unread messages", NamedTextColor.DARK_GRAY));
         ItemStack msgItem = this.guiConfig.getAppMessages().build(this.guiConfig.getMiniMessage(), Placeholder.component("unread_status", unreadStatus));
         if (unread > 0) {
            msgItem.setType(this.guiConfig.getAppMessagesUnreadMaterial());
         }

         inv.setItem(this.guiConfig.getAppMessages().getSlot(), msgItem);
         Component routeStatus = Component.empty();
         if (this.gpsManager != null && this.gpsManager.isNavigating(player.getUniqueId())) {
            GpsSession session = this.gpsManager.getSession(player.getUniqueId());
            String dest = session != null ? session.getDestinationTitle() : "Target";
            routeStatus = Component.text("Active Route: ", NamedTextColor.AQUA).append(Component.text(dest, NamedTextColor.GOLD));
         }

         inv.setItem(this.guiConfig.getAppGps().getSlot(), this.guiConfig.getAppGps().build(this.guiConfig.getMiniMessage(), Placeholder.component("route_status", routeStatus)));
         Component silentStatus = phoneProfile.isSilentMode() ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED);
         Component airplaneStatus = phoneProfile.isAirplaneMode() ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED);
         inv.setItem(this.guiConfig.getAppSettings().getSlot(), this.guiConfig.getAppSettings().build(this.guiConfig.getMiniMessage(), Placeholder.component("silent_status", silentStatus), Placeholder.component("airplane_status", airplaneStatus)));
         player.openInventory(inv);
      }
   }

   public void openDialer(Player player, String deviceNumber, String dialBuffer) {
      int size = this.guiConfig.getDialerSize();
      PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.DIALER, dialBuffer);
      Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getDialerTitle());
      holder.setInventory(inv);
      this.fillInventory(inv, size, this.guiConfig.getDialerFiller());
      String displayNum = dialBuffer.isEmpty() ? "---" : dialBuffer;
      inv.setItem(this.guiConfig.getDialerScreen().getSlot(), this.guiConfig.getDialerScreen().build(this.guiConfig.getMiniMessage(), Placeholder.parsed("number", displayNum)));

      for(Map.Entry<String, Integer> entry : this.guiConfig.getKeypadSlots().entrySet()) {
         String digit = (String)entry.getKey();
         int slot = (Integer)entry.getValue();
         if (slot >= 0 && slot < size) {
            inv.setItem(slot, createGuiItem(this.guiConfig.getKeypadMaterial(), ((TextComponent)Component.text(digit, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)).decoration(TextDecoration.ITALIC, false), List.of()));
         }
      }

      inv.setItem(this.guiConfig.getDialerBackspace().getSlot(), this.guiConfig.getDialerBackspace().build(this.guiConfig.getMiniMessage()));
      inv.setItem(this.guiConfig.getDialerCall().getSlot(), this.guiConfig.getDialerCall().build(this.guiConfig.getMiniMessage()));
      inv.setItem(this.guiConfig.getDialerBackButton().getSlot(), this.guiConfig.getDialerBackButton().build(this.guiConfig.getMiniMessage()));
      player.openInventory(inv);
   }

   public void openContacts(Player player, String deviceNumber) {
      PhoneProfile phoneProfile = this.profileManager.getProfile(deviceNumber);
      if (phoneProfile != null) {
         int size = this.guiConfig.getContactsSize();
         PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.CONTACTS);
         Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getContactsTitle());
         holder.setInventory(inv);
         int bottomStart = size - 9;

         for(int i = bottomStart; i < size; ++i) {
            inv.setItem(i, this.guiConfig.getContactsFiller());
         }

         int slot = 0;

         for(Map.Entry<String, String> entry : phoneProfile.getContacts().entrySet()) {
            if (slot >= bottomStart) {
               break;
            }

            String contactName = (String)entry.getKey();
            String contactNumber = (String)entry.getValue();
            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta)headItem.getItemMeta();
            if (meta != null) {
               UUID ownerUuid = this.profileManager.getOwnerOfNumber(contactNumber);
               PlayerProfile skinProfile = ownerUuid != null ? Bukkit.createProfile(ownerUuid, contactName) : Bukkit.createProfile(UUID.nameUUIDFromBytes(contactName.getBytes()), contactName);
               meta.setPlayerProfile(skinProfile);
               meta.displayName(Component.text(contactName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
               meta.lore(List.of((TextComponent)Component.text("Number: " + contactNumber, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false), Component.empty(), (TextComponent)Component.text("▶ Left-Click: Call", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false), (TextComponent)Component.text("\ud83d\udcac Middle-Click: Send Text", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false), (TextComponent)Component.text("✖ Right-Click: Delete", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
               headItem.setItemMeta(meta);
            }

            inv.setItem(slot++, headItem);
         }

         inv.setItem(this.guiConfig.getContactsAddButton().getSlot(), this.guiConfig.getContactsAddButton().build(this.guiConfig.getMiniMessage()));
         inv.setItem(this.guiConfig.getContactsBackButton().getSlot(), this.guiConfig.getContactsBackButton().build(this.guiConfig.getMiniMessage()));
         player.openInventory(inv);
      }
   }

   public void openSmsInbox(Player player, String deviceNumber) {
      PhoneProfile phoneProfile = this.profileManager.getProfile(deviceNumber);
      if (phoneProfile != null) {
         int size = this.guiConfig.getSmsInboxSize();
         PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.SMS_INBOX);
         Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getSmsInboxTitle());
         holder.setInventory(inv);
         int bottomStart = size - 9;

         for(int i = bottomStart; i < size; ++i) {
            inv.setItem(i, this.guiConfig.getSmsInboxFiller());
         }

         Map<String, List<TextMessage>> conversations = phoneProfile.getConversations();
         if (conversations.isEmpty()) {
            inv.setItem(22, createGuiItem(Material.MAP, Component.text("No Conversations Yet", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false), List.of(Component.text("Click '+ Compose' below to text someone!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
         } else {
            int slot = 0;

            for(Map.Entry<String, List<TextMessage>> entry : conversations.entrySet()) {
               if (slot >= bottomStart) {
                  break;
               }

               String partnerNumber = (String)entry.getKey();
               List<TextMessage> thread = (List)entry.getValue();
               TextMessage lastMsg = (TextMessage)thread.get(thread.size() - 1);
               String displayName = phoneProfile.resolveContactName(partnerNumber);
               UUID ownerUuid = this.profileManager.getOwnerOfNumber(partnerNumber);
               if (displayName.equalsIgnoreCase(partnerNumber) && ownerUuid != null) {
                  String ownerName = this.profileManager.getOwnerNameOfNumber(partnerNumber);
                  if (ownerName != null) {
                     displayName = ownerName;
                  }
               }

               int unread = phoneProfile.getUnreadCountWith(partnerNumber);
               List<Component> lore = new ArrayList();
               lore.add(Component.text("Number: " + partnerNumber, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
               if (unread > 0) {
                  lore.add(((TextComponent)Component.text("• " + unread + " Unread", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)).decoration(TextDecoration.ITALIC, false));
               }

               lore.add(((TextComponent)Component.text("Last: ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)).append(Component.text(truncate(lastMsg.getContent(), 28), NamedTextColor.WHITE)));
               lore.add(Component.text("Time: " + lastMsg.getFormattedTimestamp(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
               lore.add(Component.empty());
               lore.add(Component.text("▶ Click to open conversation", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
               ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
               SkullMeta skullMeta = (SkullMeta)headItem.getItemMeta();
               if (skullMeta != null) {
                  if (ownerUuid != null) {
                     PlayerProfile skinProfile = Bukkit.createProfile(ownerUuid, displayName);
                     skullMeta.setPlayerProfile(skinProfile);
                  } else {
                     PlayerProfile fallbackProfile = Bukkit.createProfile(UUID.nameUUIDFromBytes(displayName.getBytes()), displayName);
                     skullMeta.setPlayerProfile(fallbackProfile);
                  }

                  skullMeta.displayName(((TextComponent)Component.text(displayName, unread > 0 ? NamedTextColor.GREEN : NamedTextColor.GOLD).decoration(TextDecoration.BOLD, unread > 0)).decoration(TextDecoration.ITALIC, false));
                  skullMeta.lore(lore);
                  headItem.setItemMeta(skullMeta);
               }

               inv.setItem(slot++, headItem);
            }
         }

         inv.setItem(this.guiConfig.getSmsInboxComposeButton().getSlot(), this.guiConfig.getSmsInboxComposeButton().build(this.guiConfig.getMiniMessage()));
         inv.setItem(this.guiConfig.getSmsInboxBackButton().getSlot(), this.guiConfig.getSmsInboxBackButton().build(this.guiConfig.getMiniMessage()));
         player.openInventory(inv);
      }
   }

   public void openSmsConversation(Player player, String deviceNumber, String partnerNumber) {
      PhoneProfile phoneProfile = this.profileManager.getProfile(deviceNumber);
      if (phoneProfile != null) {
         phoneProfile.markConversationAsRead(partnerNumber);
         this.profileManager.saveProfile(phoneProfile);
         int size = this.guiConfig.getSmsConvSize();
         String partnerDisplay = phoneProfile.resolveContactName(partnerNumber);
         UUID ownerUuid = this.profileManager.getOwnerOfNumber(partnerNumber);
         if (partnerDisplay.equalsIgnoreCase(partnerNumber) && ownerUuid != null) {
            String ownerName = this.profileManager.getOwnerNameOfNumber(partnerNumber);
            if (ownerName != null) {
               partnerDisplay = ownerName;
            }
         }

         PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.SMS_CONVERSATION, "", partnerNumber);
         Inventory inv = Bukkit.createInventory(holder, size, Component.text("SMS: " + partnerDisplay, NamedTextColor.DARK_AQUA));
         holder.setInventory(inv);
         int bottomStart = size - 9;

         for(int i = bottomStart; i < size; ++i) {
            inv.setItem(i, this.guiConfig.getSmsConvFiller());
         }

         List<TextMessage> thread = phoneProfile.getMessagesWith(partnerNumber);
         int startIndex = Math.max(0, thread.size() - bottomStart);
         int slot = 0;

         for(int i = startIndex; i < thread.size(); ++i) {
            TextMessage msg = (TextMessage)thread.get(i);
            boolean outgoing = msg.isOutgoing(deviceNumber);
            Material mat = outgoing ? this.guiConfig.getSmsConvSentMaterial() : this.guiConfig.getSmsConvReceivedMaterial();
            Component title = outgoing ? Component.text("You (" + msg.getFormattedTimestamp() + ")", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false) : Component.text(partnerDisplay + " (" + msg.getFormattedTimestamp() + ")", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
            List<Component> lore = wrapTextToLore(msg.getContent(), 36);
            inv.setItem(slot++, createGuiItem(mat, title, lore));
         }

         inv.setItem(this.guiConfig.getSmsConvReplyButton().getSlot(), this.guiConfig.getSmsConvReplyButton().build(this.guiConfig.getMiniMessage()));
         inv.setItem(this.guiConfig.getSmsConvBackButton().getSlot(), this.guiConfig.getSmsConvBackButton().build(this.guiConfig.getMiniMessage()));
         player.openInventory(inv);
      }
   }

   public void openIncomingCall(Player player, String deviceNumber, String callerName, String callerNumber) {
      int size = this.guiConfig.getIncomingCallSize();
      PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.INCOMING_CALL);
      Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getIncomingCallTitle());
      holder.setInventory(inv);
      this.fillInventory(inv, size, this.guiConfig.getMainMenuFiller());
      ItemStack callerHead = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)callerHead.getItemMeta();
      if (meta != null) {
         UUID callerUuid = this.profileManager.getOwnerOfNumber(callerNumber);
         PlayerProfile skinProfile = callerUuid != null ? Bukkit.createProfile(callerUuid, callerName) : Bukkit.createProfile(UUID.nameUUIDFromBytes(callerName.getBytes()), callerName);
         meta.setPlayerProfile(skinProfile);
         meta.displayName(Component.text(callerName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
         meta.lore(List.of((TextComponent)Component.text("Incoming Call", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false), (TextComponent)Component.text("Number: " + callerNumber, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
         callerHead.setItemMeta(meta);
      }

      inv.setItem(this.guiConfig.getIncomingCallerHeadSlot(), callerHead);
      inv.setItem(this.guiConfig.getIncomingAcceptButton().getSlot(), this.guiConfig.getIncomingAcceptButton().build(this.guiConfig.getMiniMessage()));
      inv.setItem(this.guiConfig.getIncomingDeclineButton().getSlot(), this.guiConfig.getIncomingDeclineButton().build(this.guiConfig.getMiniMessage()));
      player.openInventory(inv);
   }

   public void openActiveCall(Player player, String deviceNumber, String partnerName, String partnerNumber) {
      int size = this.guiConfig.getActiveCallSize();
      PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.ACTIVE_CALL);
      Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getActiveCallTitle());
      holder.setInventory(inv);
      this.fillInventory(inv, size, this.guiConfig.getMainMenuFiller());
      ItemStack partnerHead = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)partnerHead.getItemMeta();
      if (meta != null) {
         UUID partnerUuid = this.profileManager.getOwnerOfNumber(partnerNumber);
         PlayerProfile skinProfile = partnerUuid != null ? Bukkit.createProfile(partnerUuid, partnerName) : Bukkit.createProfile(UUID.nameUUIDFromBytes(partnerName.getBytes()), partnerName);
         meta.setPlayerProfile(skinProfile);
         meta.displayName(Component.text(partnerName, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
         meta.lore(List.of((TextComponent)Component.text("Line Active", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false), (TextComponent)Component.text("Number: " + partnerNumber, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
         partnerHead.setItemMeta(meta);
      }

      inv.setItem(this.guiConfig.getActivePartnerHeadSlot(), partnerHead);
      inv.setItem(this.guiConfig.getActiveHangupButton().getSlot(), this.guiConfig.getActiveHangupButton().build(this.guiConfig.getMiniMessage()));
      player.openInventory(inv);
   }

   public void openGpsMenu(Player player, String deviceNumber) {
      int size = this.guiConfig.getGpsSize();
      PhoneHolder holder = new PhoneHolder(player.getUniqueId(), deviceNumber, PhoneHolder.MenuType.GPS_DESTINATIONS);
      Inventory inv = Bukkit.createInventory(holder, size, this.guiConfig.getGpsTitle());
      holder.setInventory(inv);
      int bottomStart = size - 9;

      for(int i = bottomStart; i < size; ++i) {
         inv.setItem(i, this.guiConfig.getGpsFiller());
      }

      boolean navigating = this.gpsManager != null && this.gpsManager.isNavigating(player.getUniqueId());
      if (navigating) {
         inv.setItem(this.guiConfig.getGpsCancelButton().getSlot(), this.guiConfig.getGpsCancelButton().build(this.guiConfig.getMiniMessage()));
      }

      inv.setItem(this.guiConfig.getGpsBackButton().getSlot(), this.guiConfig.getGpsBackButton().build(this.guiConfig.getMiniMessage()));
      if (this.gpsManager != null) {
         List<GpsNode> destinations = this.gpsManager.getDestinations();
         if (destinations.isEmpty()) {
            inv.setItem(22, createGuiItem(Material.MAP, Component.text("No Destinations Configured", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false), List.of(Component.text("Staff can register points with /phone gps editor", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
         } else {
            int slot = 0;

            for(GpsNode node : destinations) {
               if (slot >= bottomStart) {
                  break;
               }

               String name = node.getDestinationName().isBlank() ? node.getId() : node.getDestinationName();
               double dist = node.distance(player.getLocation());
               List<Component> lore = new ArrayList();
               lore.add(((TextComponent)Component.text("Category: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(Component.text(node.getCategory(), NamedTextColor.AQUA)));
               lore.add(Component.text(String.format("Distance: %.0fm", dist), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
               if (!node.getDescription().isBlank()) {
                  lore.add(Component.text(node.getDescription(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
               }

               lore.add(Component.empty());
               lore.add(Component.text("▶ Click to start GPS navigation", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
               inv.setItem(slot++, createGuiItem(node.getIconMaterial(), ((TextComponent)Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)).decoration(TextDecoration.BOLD, true), lore));
            }
         }
      }

      player.openInventory(inv);
   }

   private void fillInventory(Inventory inv, int size, ItemStack filler) {
      for(int i = 0; i < size; ++i) {
         inv.setItem(i, filler);
      }

   }

   private static List<Component> wrapTextToLore(String text, int maxLineLen) {
      List<Component> lines = new ArrayList();
      String[] words = text.split("\\s+");
      StringBuilder current = new StringBuilder();

      for(String word : words) {
         if (current.length() + word.length() + 1 > maxLineLen) {
            lines.add(Component.text(current.toString(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            current = new StringBuilder();
         }

         if (!current.isEmpty()) {
            current.append(" ");
         }

         current.append(word);
      }

      if (!current.isEmpty()) {
         lines.add(Component.text(current.toString(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
      }

      return lines;
   }

   private static String truncate(String text, int max) {
      if (text == null) {
         return "";
      } else {
         return text.length() <= max ? text : text.substring(0, max - 3) + "...";
      }
   }

   public static ItemStack createGuiItem(Material material, Component name, List<Component> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(name);
         meta.lore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }
}
