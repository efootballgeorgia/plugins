package com.roleplay.phone.util;

import com.roleplay.phone.PhonePlugin;
import org.bukkit.NamespacedKey;

public final class PhoneKeys {
   public static final NamespacedKey IS_PHONE = new NamespacedKey(PhonePlugin.getInstance(), "is_phone");
   public static final NamespacedKey OWNER_UUID = new NamespacedKey(PhonePlugin.getInstance(), "owner_uuid");
   public static final NamespacedKey PHONE_NUMBER = new NamespacedKey(PhonePlugin.getInstance(), "phone_number");

   private PhoneKeys() {
   }
}
