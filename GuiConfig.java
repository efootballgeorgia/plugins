package com.roleplay.phone.gui.config;

import com.roleplay.phone.PhonePlugin;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiConfig {
   private final PhonePlugin plugin;
   private final MiniMessage miniMessage = MiniMessage.miniMessage();
   private File file;
   private FileConfiguration config;
   private int mainMenuSize;
   private Component mainMenuTitle;
   private ItemStack mainMenuFiller;
   private int deviceInfoSlot;
   private GuiItemConfig appDialer;
   private GuiItemConfig appContacts;
   private GuiItemConfig appMessages;
   private Material appMessagesUnreadMaterial;
   private GuiItemConfig appGps;
   private GuiItemConfig appSettings;
   private int dialerSize;
   private Component dialerTitle;
   private ItemStack dialerFiller;
   private GuiItemConfig dialerScreen;
   private final Map<String, Integer> keypadSlots = new HashMap();
   private Material keypadMaterial;
   private GuiSound keypadClickSound;
   private GuiItemConfig dialerBackspace;
   private GuiItemConfig dialerCall;
   private GuiItemConfig dialerBackButton;
   private int contactsSize;
   private Component contactsTitle;
   private ItemStack contactsFiller;
   private GuiItemConfig contactsAddButton;
   private GuiItemConfig contactsBackButton;
   private GuiSound contactClickSound;
   private GuiSound contactDeleteSound;
   private int smsInboxSize;
   private Component smsInboxTitle;
   private ItemStack smsInboxFiller;
   private GuiItemConfig smsInboxComposeButton;
   private GuiItemConfig smsInboxBackButton;
   private GuiSound smsThreadClickSound;
   private int smsConvSize;
   private ItemStack smsConvFiller;
   private Material smsConvSentMaterial;
   private Material smsConvReceivedMaterial;
   private GuiItemConfig smsConvReplyButton;
   private GuiItemConfig smsConvBackButton;
   private int incomingCallSize;
   private Component incomingCallTitle;
   private int incomingCallerHeadSlot;
   private GuiItemConfig incomingAcceptButton;
   private GuiItemConfig incomingDeclineButton;
   private int activeCallSize;
   private Component activeCallTitle;
   private int activePartnerHeadSlot;
   private GuiItemConfig activeHangupButton;
   private int gpsSize;
   private Component gpsTitle;
   private ItemStack gpsFiller;
   private GuiItemConfig gpsCancelButton;
   private GuiItemConfig gpsBackButton;
   private GuiSound gpsDestinationClickSound;

   public GuiConfig(PhonePlugin plugin) {
      this.plugin = plugin;
      this.reload();
   }

   public void reload() {
      this.file = new File(this.plugin.getDataFolder(), "gui.yml");
      if (!this.file.exists()) {
         this.plugin.saveResource("gui.yml", false);
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
      this.loadMainMenu();
      this.loadDialer();
      this.loadContacts();
      this.loadSmsInbox();
      this.loadSmsConversation();
      this.loadCalls();
      this.loadGps();
   }

   private void loadMainMenu() {
      ConfigurationSection sec = this.config.getConfigurationSection("main-menu");
      this.mainMenuSize = this.sanitizeSize(sec != null ? sec.getInt("size", 27) : 27);
      this.mainMenuTitle = this.parseTitle(sec != null ? sec.getString("title", "Phone Home Screen") : "Phone Home Screen");
      this.mainMenuFiller = this.parseFiller(sec != null ? sec.getConfigurationSection("filler") : null);
      this.deviceInfoSlot = sec != null ? sec.getInt("device-info.slot", 4) : 4;
      ConfigurationSection apps = sec != null ? sec.getConfigurationSection("apps") : null;
      this.appDialer = GuiItemConfig.fromSection(apps != null ? apps.getConfigurationSection("dialer") : null, 10, Material.NOTE_BLOCK, "<green><bold>Phone / Dialer</bold></green>", List.of("<gray>Dial numbers manually</gray>"), new GuiSound(Sound.UI_BUTTON_CLICK, 0.6F, 1.2F));
      this.appContacts = GuiItemConfig.fromSection(apps != null ? apps.getConfigurationSection("contacts") : null, 11, Material.BOOK, "<yellow><bold>Contacts Directory</bold></yellow>", List.of("<gray>View contacts</gray>"), new GuiSound(Sound.ITEM_BOOK_PAGE_TURN, 0.7F, 1.3F));
      ConfigurationSection msgSec = apps != null ? apps.getConfigurationSection("messages") : null;
      this.appMessages = GuiItemConfig.fromSection(msgSec, 13, Material.PAPER, "<gold><bold>Messages / SMS</bold></gold>", List.of("<gray>Read text messages</gray>"), new GuiSound(Sound.ITEM_BOOK_PAGE_TURN, 0.7F, 1.5F));
      this.appMessagesUnreadMaterial = this.parseMaterial(msgSec != null ? msgSec.getString("unread-material", "WRITABLE_BOOK") : "WRITABLE_BOOK", Material.WRITABLE_BOOK);
      this.appGps = GuiItemConfig.fromSection(apps != null ? apps.getConfigurationSection("gps") : null, 15, Material.COMPASS, "<gold><bold>GPS Maps & Navigation</bold></gold>", List.of("<gray>Turn-by-turn routes</gray>"), new GuiSound(Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.6F, 1.2F));
      this.appSettings = GuiItemConfig.fromSection(apps != null ? apps.getConfigurationSection("settings") : null, 16, Material.REPEATER, "<aqua><bold>Phone Settings</bold></aqua>", List.of("<gray>Toggles</gray>"), new GuiSound(Sound.BLOCK_LEVER_CLICK, 0.6F, 1.8F));
   }

   private void loadDialer() {
      ConfigurationSection sec = this.config.getConfigurationSection("dialer");
      this.dialerSize = this.sanitizeSize(sec != null ? sec.getInt("size", 54) : 54);
      this.dialerTitle = this.parseTitle(sec != null ? sec.getString("title", "Dial Pad") : "Dial Pad");
      this.dialerFiller = this.parseFiller(sec != null ? sec.getConfigurationSection("filler") : null);
      this.dialerScreen = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("screen") : null, 4, Material.NAME_TAG, "<gold>Number: <yellow><number></yellow></gold>", List.of(), (GuiSound)null);
      this.keypadSlots.clear();
      ConfigurationSection keypad = sec != null ? sec.getConfigurationSection("keypad") : null;
      this.keypadMaterial = this.parseMaterial(keypad != null ? keypad.getString("material", "LIGHT_GRAY_CONCRETE") : "LIGHT_GRAY_CONCRETE", Material.LIGHT_GRAY_CONCRETE);
      this.keypadClickSound = GuiSound.fromSection(keypad != null ? keypad.getConfigurationSection("click-sound") : null, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5F, 1.3F);
      ConfigurationSection slotsSec = keypad != null ? keypad.getConfigurationSection("slots") : null;
      if (slotsSec != null) {
         for(String key : slotsSec.getKeys(false)) {
            this.keypadSlots.put(key, slotsSec.getInt(key));
         }
      } else {
         this.keypadSlots.putAll(Map.of("1", 12, "2", 13, "3", 14, "4", 21, "5", 22, "6", 23, "7", 30, "8", 31, "9", 32));
         this.keypadSlots.put("0", 40);
      }

      this.dialerBackspace = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("backspace") : null, 39, Material.REDSTONE_BLOCK, "<red><bold>Backspace</bold></red>", List.of("<gray>Delete digit</gray>"), new GuiSound(Sound.UI_BUTTON_CLICK, 0.5F, 0.7F));
      this.dialerCall = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("call") : null, 41, Material.EMERALD_BLOCK, "<green><bold>Place Call</bold></green>", List.of("<gray>Click to dial</gray>"), new GuiSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8F, 1.4F));
      this.dialerBackButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("back-button") : null, 49, Material.ARROW, "<white>Back to Home Screen</white>", List.of(), new GuiSound(Sound.UI_BUTTON_CLICK, 0.5F, 0.9F));
   }

   private void loadContacts() {
      ConfigurationSection sec = this.config.getConfigurationSection("contacts");
      this.contactsSize = this.sanitizeSize(sec != null ? sec.getInt("size", 54) : 54);
      this.contactsTitle = this.parseTitle(sec != null ? sec.getString("title", "Contacts Directory") : "Contacts Directory");
      this.contactsFiller = this.parseFiller(sec != null ? sec.getConfigurationSection("filler") : null);
      this.contactsAddButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("add-contact") : null, 48, Material.EMERALD, "<green><bold>+ Add New Contact</bold></green>", List.of("<gray>Add in chat</gray>"), new GuiSound(Sound.ITEM_BOOK_PAGE_TURN, 0.6F, 1.4F));
      this.contactsBackButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("back-button") : null, 49, Material.ARROW, "<white>Back to Home Screen</white>", List.of(), new GuiSound(Sound.UI_BUTTON_CLICK, 0.5F, 0.9F));
      this.contactClickSound = GuiSound.fromSection(sec != null ? sec.getConfigurationSection("contact-click-sound") : null, Sound.UI_BUTTON_CLICK, 0.5F, 1.2F);
      this.contactDeleteSound = GuiSound.fromSection(sec != null ? sec.getConfigurationSection("contact-delete-sound") : null, Sound.BLOCK_ANVIL_BREAK, 0.4F, 1.4F);
   }

   private void loadSmsInbox() {
      ConfigurationSection sec = this.config.getConfigurationSection("sms-inbox");
      this.smsInboxSize = this.sanitizeSize(sec != null ? sec.getInt("size", 54) : 54);
      this.smsInboxTitle = this.parseTitle(sec != null ? sec.getString("title", "Messages - Inbox") : "Messages - Inbox");
      this.smsInboxFiller = this.parseFiller(sec != null ? sec.getConfigurationSection("filler") : null);
      this.smsInboxComposeButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("compose-new") : null, 48, Material.EMERALD, "<green><bold>+ Compose Message</bold></green>", List.of("<gray>Compose text</gray>"), new GuiSound(Sound.ITEM_BOOK_PAGE_TURN, 0.6F, 1.4F));
      this.smsInboxBackButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("back-button") : null, 49, Material.ARROW, "<white>Back to Home Screen</white>", List.of(), new GuiSound(Sound.UI_BUTTON_CLICK, 0.5F, 0.9F));
      this.smsThreadClickSound = GuiSound.fromSection(sec != null ? sec.getConfigurationSection("thread-click-sound") : null, Sound.ITEM_BOOK_PAGE_TURN, 0.5F, 1.2F);
   }

   private void loadSmsConversation() {
      ConfigurationSection sec = this.config.getConfigurationSection("sms-conversation");
      this.smsConvSize = this.sanitizeSize(sec != null ? sec.getInt("size", 54) : 54);
      this.smsConvFiller = this.parseFiller(sec != null ? sec.getConfigurationSection("filler") : null);
      this.smsConvSentMaterial = this.parseMaterial(sec != null ? sec.getString("sent-material", "LIME_STAINED_GLASS_PANE") : "LIME_STAINED_GLASS_PANE", Material.LIME_STAINED_GLASS_PANE);
      this.smsConvReceivedMaterial = this.parseMaterial(sec != null ? sec.getString("received-material", "LIGHT_BLUE_STAINED_GLASS_PANE") : "LIGHT_BLUE_STAINED_GLASS_PANE", Material.LIGHT_BLUE_STAINED_GLASS_PANE);
      this.smsConvReplyButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("reply") : null, 48, Material.WRITABLE_BOOK, "<aqua><bold>Reply / Compose</bold></aqua>", List.of("<gray>Reply in chat</gray>"), new GuiSound(Sound.ITEM_BOOK_PAGE_TURN, 0.6F, 1.5F));
      this.smsConvBackButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("back-button") : null, 49, Material.ARROW, "<white>Back to Inbox</white>", List.of(), new GuiSound(Sound.UI_BUTTON_CLICK, 0.5F, 0.9F));
   }

   private void loadCalls() {
      ConfigurationSection inSec = this.config.getConfigurationSection("incoming-call");
      this.incomingCallSize = this.sanitizeSize(inSec != null ? inSec.getInt("size", 27) : 27);
      this.incomingCallTitle = this.parseTitle(inSec != null ? inSec.getString("title", "Incoming Call...") : "Incoming Call...");
      this.incomingCallerHeadSlot = inSec != null ? inSec.getInt("caller-head-slot", 4) : 4;
      this.incomingAcceptButton = GuiItemConfig.fromSection(inSec != null ? inSec.getConfigurationSection("accept") : null, 11, Material.LIME_CONCRETE, "<green><bold>Accept / Pickup</bold></green>", List.of(), new GuiSound(Sound.BLOCK_NOTE_BLOCK_BELL, 0.8F, 1.5F));
      this.incomingDeclineButton = GuiItemConfig.fromSection(inSec != null ? inSec.getConfigurationSection("decline") : null, 15, Material.RED_CONCRETE, "<red><bold>Decline Call</bold></red>", List.of(), new GuiSound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.7F));
      ConfigurationSection actSec = this.config.getConfigurationSection("active-call");
      this.activeCallSize = this.sanitizeSize(actSec != null ? actSec.getInt("size", 27) : 27);
      this.activeCallTitle = this.parseTitle(actSec != null ? actSec.getString("title", "Active Call") : "Active Call");
      this.activePartnerHeadSlot = actSec != null ? actSec.getInt("partner-head-slot", 4) : 4;
      this.activeHangupButton = GuiItemConfig.fromSection(actSec != null ? actSec.getConfigurationSection("hangup") : null, 13, Material.RED_CONCRETE, "<red><bold>Hang Up</bold></red>", List.of(), new GuiSound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.6F));
   }

   private void loadGps() {
      ConfigurationSection sec = this.config.getConfigurationSection("gps-destinations");
      this.gpsSize = this.sanitizeSize(sec != null ? sec.getInt("size", 54) : 54);
      this.gpsTitle = this.parseTitle(sec != null ? sec.getString("title", "GPS Maps - Destinations") : "GPS Maps - Destinations");
      this.gpsFiller = this.parseFiller(sec != null ? sec.getConfigurationSection("filler") : null);
      this.gpsCancelButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("cancel-route") : null, 48, Material.BARRIER, "<red><bold>Cancel Navigation</bold></red>", List.of("<gray>Stop active route</gray>"), new GuiSound(Sound.BLOCK_FIRE_EXTINGUISH, 0.6F, 1.4F));
      this.gpsBackButton = GuiItemConfig.fromSection(sec != null ? sec.getConfigurationSection("back-button") : null, 49, Material.ARROW, "<white>Back to Home Screen</white>", List.of(), new GuiSound(Sound.UI_BUTTON_CLICK, 0.5F, 0.9F));
      this.gpsDestinationClickSound = GuiSound.fromSection(sec != null ? sec.getConfigurationSection("destination-click-sound") : null, Sound.ITEM_LODESTONE_COMPASS_LOCK, 0.8F, 1.2F);
   }

   private int sanitizeSize(int size) {
      if (size < 9) {
         return 9;
      } else {
         return size > 54 ? 54 : size / 9 * 9;
      }
   }

   private Component parseTitle(String raw) {
      return this.miniMessage.deserialize(raw).decoration(TextDecoration.ITALIC, false);
   }

   private Material parseMaterial(String name, Material fallback) {
      try {
         return Material.valueOf(name.toUpperCase());
      } catch (IllegalArgumentException var4) {
         return fallback;
      }
   }

   private ItemStack parseFiller(ConfigurationSection sec) {
      Material mat = Material.GRAY_STAINED_GLASS_PANE;
      int cmd = 0;
      String name = " ";
      if (sec != null) {
         mat = this.parseMaterial(sec.getString("material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
         cmd = sec.getInt("custom-model-data", 0);
         name = sec.getString("name", " ");
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(this.miniMessage.deserialize(name).decoration(TextDecoration.ITALIC, false));
         if (cmd > 0) {
            meta.setCustomModelData(cmd);
         }

         item.setItemMeta(meta);
      }

      return item;
   }

   public MiniMessage getMiniMessage() {
      return this.miniMessage;
   }

   public int getMainMenuSize() {
      return this.mainMenuSize;
   }

   public Component getMainMenuTitle() {
      return this.mainMenuTitle;
   }

   public ItemStack getMainMenuFiller() {
      return this.mainMenuFiller.clone();
   }

   public int getDeviceInfoSlot() {
      return this.deviceInfoSlot;
   }

   public GuiItemConfig getAppDialer() {
      return this.appDialer;
   }

   public GuiItemConfig getAppContacts() {
      return this.appContacts;
   }

   public GuiItemConfig getAppMessages() {
      return this.appMessages;
   }

   public Material getAppMessagesUnreadMaterial() {
      return this.appMessagesUnreadMaterial;
   }

   public GuiItemConfig getAppGps() {
      return this.appGps;
   }

   public GuiItemConfig getAppSettings() {
      return this.appSettings;
   }

   public int getDialerSize() {
      return this.dialerSize;
   }

   public Component getDialerTitle() {
      return this.dialerTitle;
   }

   public ItemStack getDialerFiller() {
      return this.dialerFiller.clone();
   }

   public GuiItemConfig getDialerScreen() {
      return this.dialerScreen;
   }

   public Map<String, Integer> getKeypadSlots() {
      return Collections.unmodifiableMap(this.keypadSlots);
   }

   public Material getKeypadMaterial() {
      return this.keypadMaterial;
   }

   public GuiSound getKeypadClickSound() {
      return this.keypadClickSound;
   }

   public GuiItemConfig getDialerBackspace() {
      return this.dialerBackspace;
   }

   public GuiItemConfig getDialerCall() {
      return this.dialerCall;
   }

   public GuiItemConfig getDialerBackButton() {
      return this.dialerBackButton;
   }

   public int getContactsSize() {
      return this.contactsSize;
   }

   public Component getContactsTitle() {
      return this.contactsTitle;
   }

   public ItemStack getContactsFiller() {
      return this.contactsFiller.clone();
   }

   public GuiItemConfig getContactsAddButton() {
      return this.contactsAddButton;
   }

   public GuiItemConfig getContactsBackButton() {
      return this.contactsBackButton;
   }

   public GuiSound getContactClickSound() {
      return this.contactClickSound;
   }

   public GuiSound getContactDeleteSound() {
      return this.contactDeleteSound;
   }

   public int getSmsInboxSize() {
      return this.smsInboxSize;
   }

   public Component getSmsInboxTitle() {
      return this.smsInboxTitle;
   }

   public ItemStack getSmsInboxFiller() {
      return this.smsInboxFiller.clone();
   }

   public GuiItemConfig getSmsInboxComposeButton() {
      return this.smsInboxComposeButton;
   }

   public GuiItemConfig getSmsInboxBackButton() {
      return this.smsInboxBackButton;
   }

   public GuiSound getSmsThreadClickSound() {
      return this.smsThreadClickSound;
   }

   public int getSmsConvSize() {
      return this.smsConvSize;
   }

   public ItemStack getSmsConvFiller() {
      return this.smsConvFiller.clone();
   }

   public Material getSmsConvSentMaterial() {
      return this.smsConvSentMaterial;
   }

   public Material getSmsConvReceivedMaterial() {
      return this.smsConvReceivedMaterial;
   }

   public GuiItemConfig getSmsConvReplyButton() {
      return this.smsConvReplyButton;
   }

   public GuiItemConfig getSmsConvBackButton() {
      return this.smsConvBackButton;
   }

   public int getIncomingCallSize() {
      return this.incomingCallSize;
   }

   public Component getIncomingCallTitle() {
      return this.incomingCallTitle;
   }

   public int getIncomingCallerHeadSlot() {
      return this.incomingCallerHeadSlot;
   }

   public GuiItemConfig getIncomingAcceptButton() {
      return this.incomingAcceptButton;
   }

   public GuiItemConfig getIncomingDeclineButton() {
      return this.incomingDeclineButton;
   }

   public int getActiveCallSize() {
      return this.activeCallSize;
   }

   public Component getActiveCallTitle() {
      return this.activeCallTitle;
   }

   public int getActivePartnerHeadSlot() {
      return this.activePartnerHeadSlot;
   }

   public GuiItemConfig getActiveHangupButton() {
      return this.activeHangupButton;
   }

   public int getGpsSize() {
      return this.gpsSize;
   }

   public Component getGpsTitle() {
      return this.gpsTitle;
   }

   public ItemStack getGpsFiller() {
      return this.gpsFiller.clone();
   }

   public GuiItemConfig getGpsCancelButton() {
      return this.gpsCancelButton;
   }

   public GuiItemConfig getGpsBackButton() {
      return this.gpsBackButton;
   }

   public GuiSound getGpsDestinationClickSound() {
      return this.gpsDestinationClickSound;
   }
}
