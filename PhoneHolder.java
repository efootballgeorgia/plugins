package com.roleplay.phone.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PhoneHolder implements InventoryHolder {
   private final UUID playerUuid;
   private final String devicePhoneNumber;
   private final MenuType menuType;
   private final StringBuilder dialBuffer;
   private final String targetConversationNumber;
   private Inventory inventory;

   public PhoneHolder(UUID playerUuid, String devicePhoneNumber, MenuType menuType) {
      this(playerUuid, devicePhoneNumber, menuType, "", "");
   }

   public PhoneHolder(UUID playerUuid, String devicePhoneNumber, MenuType menuType, String initialDialBuffer) {
      this(playerUuid, devicePhoneNumber, menuType, initialDialBuffer, "");
   }

   public PhoneHolder(UUID playerUuid, String devicePhoneNumber, MenuType menuType, String dialBuffer, String targetConversationNumber) {
      this.playerUuid = playerUuid;
      this.devicePhoneNumber = devicePhoneNumber;
      this.menuType = menuType;
      this.dialBuffer = new StringBuilder(dialBuffer == null ? "" : dialBuffer);
      this.targetConversationNumber = targetConversationNumber != null ? targetConversationNumber : "";
   }

   public UUID getPlayerUuid() {
      return this.playerUuid;
   }

   public String getDevicePhoneNumber() {
      return this.devicePhoneNumber;
   }

   public MenuType getMenuType() {
      return this.menuType;
   }

   public StringBuilder getDialBuffer() {
      return this.dialBuffer;
   }

   public String getTargetConversationNumber() {
      return this.targetConversationNumber;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public static enum MenuType {
      MAIN_MENU,
      DIALER,
      CONTACTS,
      INCOMING_CALL,
      ACTIVE_CALL,
      GPS_DESTINATIONS,
      SMS_INBOX,
      SMS_CONVERSATION;

      // $FF: synthetic method
      private static MenuType[] $values() {
         return new MenuType[]{MAIN_MENU, DIALER, CONTACTS, INCOMING_CALL, ACTIVE_CALL, GPS_DESTINATIONS, SMS_INBOX, SMS_CONVERSATION};
      }
   }
}
