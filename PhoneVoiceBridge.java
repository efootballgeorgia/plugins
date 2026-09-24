package com.roleplay.phone.voice;

import java.util.UUID;
import org.bukkit.entity.Player;

public interface PhoneVoiceBridge {
   void onCallConnected(Player var1, Player var2);

   void onCallDisconnected(UUID var1, UUID var2);
}
