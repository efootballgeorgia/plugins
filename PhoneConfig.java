package com.roleplay.phone.config;

import com.roleplay.phone.gui.config.GuiSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PhoneConfig {
   private final JavaPlugin plugin;
   private final MiniMessage miniMessage = MiniMessage.miniMessage();
   private Material phoneMaterial;
   private int customModelData;
   private NamespacedKey itemModel;
   private Component phoneItemName;
   private boolean requireInHand;
   private int callTimeoutSeconds;
   private Component chatPrefix;
   private Sound ringtoneSound;
   private float ringtoneVolume;
   private float ringtonePitch;
   private Sound ringbackSound;
   private float ringbackVolume;
   private float ringbackPitch;
   private Sound connectedSound;
   private float connectedVolume;
   private float connectedPitch;
   private Sound hangupSound;
   private float hangupVolume;
   private float hangupPitch;
   private boolean voiceChatEnabled;
   private String voiceGroupType;
   private Component titleMainMenu;
   private Component titleDialer;
   private Component titleContacts;
   private Component titleIncomingCall;
   private Component titleActiveCall;

   public PhoneConfig(JavaPlugin plugin) {
      this.plugin = plugin;
      plugin.saveDefaultConfig();
      this.reload();
   }

   public void reload() {
      this.plugin.reloadConfig();
      FileConfiguration config = this.plugin.getConfig();
      this.phoneMaterial = this.parseMaterial(config.getString("phone-item.material", "NETHER_BRICK"), Material.NETHER_BRICK);
      this.customModelData = config.getInt("phone-item.custom-model-data", 0);
      String modelStr = config.getString("phone-item.item-model", "");
      if (modelStr != null && !modelStr.isBlank()) {
         this.itemModel = NamespacedKey.fromString(modelStr.toLowerCase());
      } else {
         this.itemModel = null;
      }

      this.phoneItemName = this.miniMessage.deserialize(config.getString("phone-item.name", "<gold><bold>Smartphone Pro</bold></gold>"));
      this.requireInHand = config.getBoolean("phone-item.require-in-hand", false);
      this.callTimeoutSeconds = config.getInt("call-settings.timeout-seconds", 30);
      this.chatPrefix = this.miniMessage.deserialize(config.getString("call-settings.chat-prefix", "<aqua>[On Phone]</aqua> "));
      this.ringtoneSound = GuiSound.parseSound(config.getString("audio.ringtone.sound", "BLOCK_NOTE_BLOCK_BELL"), Sound.BLOCK_NOTE_BLOCK_BELL);
      this.ringtoneVolume = (float)config.getDouble("audio.ringtone.volume", (double)1.0F);
      this.ringtonePitch = (float)config.getDouble("audio.ringtone.pitch", 1.2);
      this.ringbackSound = GuiSound.parseSound(config.getString("audio.ringback.sound", "BLOCK_NOTE_BLOCK_HAT"), Sound.BLOCK_NOTE_BLOCK_HAT);
      this.ringbackVolume = (float)config.getDouble("audio.ringback.volume", 0.6);
      this.ringbackPitch = (float)config.getDouble("audio.ringback.pitch", (double)1.0F);
      this.connectedSound = GuiSound.parseSound(config.getString("audio.connected.sound", "BLOCK_NOTE_BLOCK_CHIME"), Sound.BLOCK_NOTE_BLOCK_CHIME);
      this.connectedVolume = (float)config.getDouble("audio.connected.volume", (double)1.0F);
      this.connectedPitch = (float)config.getDouble("audio.connected.pitch", (double)1.5F);
      this.hangupSound = GuiSound.parseSound(config.getString("audio.hangup.sound", "BLOCK_NOTE_BLOCK_BASS"), Sound.BLOCK_NOTE_BLOCK_BASS);
      this.hangupVolume = (float)config.getDouble("audio.hangup.volume", 0.8);
      this.hangupPitch = (float)config.getDouble("audio.hangup.pitch", 0.6);
      this.voiceChatEnabled = config.getBoolean("voice-chat.enabled", true);
      this.voiceGroupType = config.getString("voice-chat.group-type", "OPEN").toUpperCase();
      this.titleMainMenu = this.miniMessage.deserialize(config.getString("gui-titles.main-menu", "Phone Home Screen"));
      this.titleDialer = this.miniMessage.deserialize(config.getString("gui-titles.dialer", "Dial Pad"));
      this.titleContacts = this.miniMessage.deserialize(config.getString("gui-titles.contacts", "Contacts Directory"));
      this.titleIncomingCall = this.miniMessage.deserialize(config.getString("gui-titles.incoming-call", "Incoming Call..."));
      this.titleActiveCall = this.miniMessage.deserialize(config.getString("gui-titles.active-call", "Active Call"));
   }

   private Material parseMaterial(String name, Material fallback) {
      try {
         return Material.valueOf(name.toUpperCase());
      } catch (IllegalArgumentException var4) {
         this.plugin.getLogger().warning("Invalid material in config: '" + name + "'. Using fallback: " + fallback.name());
         return fallback;
      }
   }

   public Material getPhoneMaterial() {
      return this.phoneMaterial;
   }

   public int getCustomModelData() {
      return this.customModelData;
   }

   public NamespacedKey getItemModel() {
      return this.itemModel;
   }

   public Component getPhoneItemName() {
      return this.phoneItemName;
   }

   public boolean isRequireInHand() {
      return this.requireInHand;
   }

   public int getCallTimeoutSeconds() {
      return this.callTimeoutSeconds;
   }

   public Component getChatPrefix() {
      return this.chatPrefix;
   }

   public Sound getRingtoneSound() {
      return this.ringtoneSound;
   }

   public float getRingtoneVolume() {
      return this.ringtoneVolume;
   }

   public float getRingtonePitch() {
      return this.ringtonePitch;
   }

   public Sound getRingbackSound() {
      return this.ringbackSound;
   }

   public float getRingbackVolume() {
      return this.ringbackVolume;
   }

   public float getRingbackPitch() {
      return this.ringbackPitch;
   }

   public Sound getConnectedSound() {
      return this.connectedSound;
   }

   public float getConnectedVolume() {
      return this.connectedVolume;
   }

   public float getConnectedPitch() {
      return this.connectedPitch;
   }

   public Sound getHangupSound() {
      return this.hangupSound;
   }

   public float getHangupVolume() {
      return this.hangupVolume;
   }

   public float getHangupPitch() {
      return this.hangupPitch;
   }

   public boolean isVoiceChatEnabled() {
      return this.voiceChatEnabled;
   }

   public String getVoiceGroupType() {
      return this.voiceGroupType;
   }

   public Component getTitleMainMenu() {
      return this.titleMainMenu;
   }

   public Component getTitleDialer() {
      return this.titleDialer;
   }

   public Component getTitleContacts() {
      return this.titleContacts;
   }

   public Component getTitleIncomingCall() {
      return this.titleIncomingCall;
   }

   public Component getTitleActiveCall() {
      return this.titleActiveCall;
   }
}
