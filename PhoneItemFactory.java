package com.roleplay.phone.item;

import com.roleplay.phone.config.PhoneConfig;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.util.PhoneKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PhoneItemFactory {
   public static ItemStack createPhoneItem(PhoneConfig config, PhoneProfile profile, String ownerName) {
      ItemStack item = new ItemStack(config.getPhoneMaterial());
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         PersistentDataContainer pdc = meta.getPersistentDataContainer();
         pdc.set(PhoneKeys.IS_PHONE, PersistentDataType.BYTE, (byte)1);
         pdc.set(PhoneKeys.OWNER_UUID, PersistentDataType.STRING, profile.getCreatorUuid() != null ? profile.getCreatorUuid().toString() : "");
         pdc.set(PhoneKeys.PHONE_NUMBER, PersistentDataType.STRING, profile.getPhoneNumber());
         if (config.getCustomModelData() > 0) {
            meta.setCustomModelData(config.getCustomModelData());
         }

         if (config.getItemModel() != null) {
            meta.setItemModel(config.getItemModel());
         }

         applyVisuals(meta, config, profile.getPhoneNumber(), ownerName, profile.isSilentMode(), profile.isAirplaneMode());
         item.setItemMeta(meta);
         return item;
      }
   }

   public static boolean isPhone(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            Byte value = (Byte)meta.getPersistentDataContainer().get(PhoneKeys.IS_PHONE, PersistentDataType.BYTE);
            return value != null && value == 1;
         }
      } else {
         return false;
      }
   }

   public static UUID getOwnerUuid(ItemStack item) {
      if (!isPhone(item)) {
         return null;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return null;
         } else {
            String uuidStr = (String)meta.getPersistentDataContainer().get(PhoneKeys.OWNER_UUID, PersistentDataType.STRING);
            if (uuidStr != null && !uuidStr.isBlank()) {
               try {
                  return UUID.fromString(uuidStr);
               } catch (IllegalArgumentException var4) {
                  return null;
               }
            } else {
               return null;
            }
         }
      }
   }

   public static String getPhoneNumber(ItemStack item) {
      if (!isPhone(item)) {
         return null;
      } else {
         ItemMeta meta = item.getItemMeta();
         return meta == null ? null : (String)meta.getPersistentDataContainer().get(PhoneKeys.PHONE_NUMBER, PersistentDataType.STRING);
      }
   }

   public static void updatePhoneItem(PhoneConfig config, ItemStack item, PhoneProfile profile, String ownerName) {
      if (isPhone(item)) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(PhoneKeys.PHONE_NUMBER, PersistentDataType.STRING, profile.getPhoneNumber());
            pdc.set(PhoneKeys.OWNER_UUID, PersistentDataType.STRING, profile.getCreatorUuid() != null ? profile.getCreatorUuid().toString() : "");
            if (config.getCustomModelData() > 0) {
               meta.setCustomModelData(config.getCustomModelData());
            }

            if (config.getItemModel() != null) {
               meta.setItemModel(config.getItemModel());
            }

            applyVisuals(meta, config, profile.getPhoneNumber(), ownerName, profile.isSilentMode(), profile.isAirplaneMode());
            item.setItemMeta(meta);
         }
      }
   }

   private static void applyVisuals(ItemMeta meta, PhoneConfig config, String phoneNumber, String ownerName, boolean silent, boolean airplane) {
      meta.displayName(config.getPhoneItemName().decoration(TextDecoration.ITALIC, false));
      List<Component> lore = new ArrayList();
      lore.add(Component.empty());
      lore.add(((TextComponent)Component.text(" Registered To: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(Component.text(ownerName, NamedTextColor.WHITE)));
      lore.add(((TextComponent)Component.text(" Number: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(Component.text(phoneNumber, NamedTextColor.YELLOW)));
      lore.add(((TextComponent)Component.text(" Silent Mode: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(silent ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED)));
      lore.add(((TextComponent)Component.text(" Airplane Mode: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).append(airplane ? Component.text("ON", NamedTextColor.GREEN) : Component.text("OFF", NamedTextColor.RED)));
      lore.add(Component.empty());
      lore.add(Component.text("▶ Right-Click to open device", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
      meta.lore(lore);
   }
}
