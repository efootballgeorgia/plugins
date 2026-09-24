package com.roleplay.phone.storage;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.model.PhoneProfile;
import com.roleplay.phone.sms.model.TextMessage;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ProfileManager {
   private final PhonePlugin plugin;
   private final File devicesFolder;
   private final Map<String, PhoneProfile> loadedProfiles = new ConcurrentHashMap();
   private final Map<UUID, String> playerToNumberMap = new ConcurrentHashMap();
   private final SecureRandom random = new SecureRandom();

   public ProfileManager(PhonePlugin plugin) {
      this.plugin = plugin;
      this.devicesFolder = new File(plugin.getDataFolder(), "devices");
      if (!this.devicesFolder.exists()) {
         this.devicesFolder.mkdirs();
      }

      this.indexAllProfiles();
   }

   private void indexAllProfiles() {
      File[] files = this.devicesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
      if (files != null) {
         for(File file : files) {
            String fileName = file.getName();
            String number = fileName.substring(0, fileName.length() - 4);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String creatorStr = config.getString("creator-uuid", "");
            if (!creatorStr.isBlank()) {
               try {
                  UUID uuid = UUID.fromString(creatorStr);
                  if (this.playerToNumberMap.containsKey(uuid)) {
                     String oldNumber = (String)this.playerToNumberMap.get(uuid);
                     File oldFile = new File(this.devicesFolder, oldNumber + ".yml");
                     if (oldFile.lastModified() < file.lastModified()) {
                        oldFile.delete();
                        this.playerToNumberMap.put(uuid, number);
                     } else {
                        file.delete();
                     }
                  } else {
                     this.playerToNumberMap.put(uuid, number);
                  }
               } catch (IllegalArgumentException var13) {
               }
            }
         }

      }
   }

   public PhoneProfile createNewPhone(UUID creatorUuid) {
      if (creatorUuid != null && this.playerToNumberMap.containsKey(creatorUuid)) {
         String oldNumber = (String)this.playerToNumberMap.get(creatorUuid);
         this.deletePhone(oldNumber);
      }

      String uniqueNumber = this.generateUniquePhoneNumber();
      PhoneProfile profile = new PhoneProfile(uniqueNumber, creatorUuid);
      if (creatorUuid != null) {
         this.playerToNumberMap.put(creatorUuid, uniqueNumber);
      }

      this.loadedProfiles.put(uniqueNumber, profile);
      this.saveProfile(profile);
      return profile;
   }

   public void deletePhone(String phoneNumber) {
      if (phoneNumber != null && !phoneNumber.isBlank()) {
         PhoneProfile removed = (PhoneProfile)this.loadedProfiles.remove(phoneNumber);
         UUID creatorUuid = removed != null ? removed.getCreatorUuid() : null;
         if (creatorUuid == null) {
            for(Map.Entry<UUID, String> entry : this.playerToNumberMap.entrySet()) {
               if (((String)entry.getValue()).equalsIgnoreCase(phoneNumber)) {
                  creatorUuid = (UUID)entry.getKey();
                  break;
               }
            }
         }

         if (creatorUuid != null) {
            this.playerToNumberMap.remove(creatorUuid);
         }

         File file = new File(this.devicesFolder, phoneNumber + ".yml");
         if (file.exists()) {
            file.delete();
         }

      }
   }

   public PhoneProfile getProfileByPlayer(UUID playerUuid) {
      if (playerUuid == null) {
         return null;
      } else {
         String number = (String)this.playerToNumberMap.get(playerUuid);
         return number != null ? this.getProfile(number) : null;
      }
   }

   public UUID getOwnerOfNumber(String phoneNumber) {
      if (phoneNumber != null && !phoneNumber.isBlank()) {
         PhoneProfile profile = this.getProfile(phoneNumber);
         return profile != null ? profile.getCreatorUuid() : null;
      } else {
         return null;
      }
   }

   public String getOwnerNameOfNumber(String phoneNumber) {
      UUID ownerUuid = this.getOwnerOfNumber(phoneNumber);
      if (ownerUuid == null) {
         return null;
      } else {
         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerUuid);
         return offlinePlayer.getName();
      }
   }

   public PhoneProfile getProfile(String phoneNumber) {
      if (phoneNumber != null && !phoneNumber.isBlank()) {
         PhoneProfile cached = (PhoneProfile)this.loadedProfiles.get(phoneNumber);
         if (cached != null) {
            return cached;
         } else {
            File file = new File(this.devicesFolder, phoneNumber + ".yml");
            if (file.exists()) {
               PhoneProfile loaded = this.loadProfileFromFile(phoneNumber, file);
               if (loaded != null) {
                  this.loadedProfiles.put(phoneNumber, loaded);
                  if (loaded.getCreatorUuid() != null) {
                     this.playerToNumberMap.put(loaded.getCreatorUuid(), phoneNumber);
                  }

                  return loaded;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public void saveProfile(PhoneProfile profile) {
      File file = new File(this.devicesFolder, profile.getPhoneNumber() + ".yml");
      YamlConfiguration config = new YamlConfiguration();
      config.set("phone-number", profile.getPhoneNumber());
      config.set("creator-uuid", profile.getCreatorUuid() != null ? profile.getCreatorUuid().toString() : "");
      config.set("silent-mode", profile.isSilentMode());
      config.set("airplane-mode", profile.isAirplaneMode());
      ConfigurationSection contactsSec = config.createSection("contacts");

      for(Map.Entry<String, String> entry : profile.getContacts().entrySet()) {
         contactsSec.set((String)entry.getKey(), entry.getValue());
      }

      List<TextMessage> allMessages = profile.getMessages();
      int startIndex = Math.max(0, allMessages.size() - 150);
      List<Map<String, Object>> serializedMessages = new ArrayList();

      for(int i = startIndex; i < allMessages.size(); ++i) {
         TextMessage msg = (TextMessage)allMessages.get(i);
         Map<String, Object> map = new HashMap();
         map.put("id", msg.getId().toString());
         map.put("sender", msg.getSenderNumber());
         map.put("recipient", msg.getRecipientNumber());
         map.put("content", msg.getContent());
         map.put("timestamp", msg.getTimestampMillis());
         map.put("read", msg.isRead());
         serializedMessages.add(map);
      }

      config.set("messages", serializedMessages);

      try {
         config.save(file);
      } catch (IOException e) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to save phone data for " + profile.getPhoneNumber(), e);
      }

   }

   public void saveAll() {
      for(PhoneProfile profile : this.loadedProfiles.values()) {
         this.saveProfile(profile);
      }

   }

   private PhoneProfile loadProfileFromFile(String phoneNumber, File file) {
      YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
      String creatorStr = config.getString("creator-uuid", "");
      UUID creatorUuid = null;
      if (!creatorStr.isBlank()) {
         try {
            creatorUuid = UUID.fromString(creatorStr);
         } catch (IllegalArgumentException var23) {
         }
      }

      boolean silent = config.getBoolean("silent-mode", false);
      boolean airplane = config.getBoolean("airplane-mode", false);
      PhoneProfile profile = new PhoneProfile(phoneNumber, creatorUuid);
      profile.setSilentMode(silent);
      profile.setAirplaneMode(airplane);
      ConfigurationSection contactsSec = config.getConfigurationSection("contacts");
      if (contactsSec != null) {
         for(String contactName : contactsSec.getKeys(false)) {
            String contactNumber = contactsSec.getString(contactName);
            if (contactNumber != null) {
               profile.addContact(contactName, contactNumber);
            }
         }
      }

      for(Map<?, ?> map : config.getMapList("messages")) {
         try {
            String idStr = Objects.toString(map.get("id"), "");
            UUID id = !idStr.isBlank() ? UUID.fromString(idStr) : UUID.randomUUID();
            String sender = Objects.toString(map.get("sender"), "");
            String recipient = Objects.toString(map.get("recipient"), "");
            String content = Objects.toString(map.get("content"), "");
            Object var21 = map.get("timestamp");
            long var10000;
            if (var21 instanceof Number num) {
               var10000 = num.longValue();
            } else {
               var10000 = System.currentTimeMillis();
            }

            long timestamp = var10000;
            boolean read = Boolean.TRUE.equals(map.get("read"));
            TextMessage msg = new TextMessage(id, sender, recipient, content, timestamp, read);
            profile.addMessage(msg);
         } catch (Exception var22) {
         }
      }

      return profile;
   }

   private String generateUniquePhoneNumber() {
      String number;
      do {
         int prefix = 100 + this.random.nextInt(900);
         int suffix = 1000 + this.random.nextInt(9000);
         number = prefix + "-" + suffix;
      } while(this.loadedProfiles.containsKey(number) || (new File(this.devicesFolder, number + ".yml")).exists());

      return number;
   }
}
