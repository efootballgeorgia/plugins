package com.roleplay.phone.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

public class PhoneVoicePlugin implements VoicechatPlugin {
   private static VoicechatServerApi serverApi;

   public String getPluginId() {
      return "roleplay_phone";
   }

   public void registerEvents(EventRegistration registration) {
      registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
   }

   private void onServerStarted(VoicechatServerStartedEvent event) {
      serverApi = event.getVoicechat();
   }

   public static VoicechatServerApi getServerApi() {
      return serverApi;
   }

   public static boolean isAvailable() {
      return serverApi != null;
   }
}
