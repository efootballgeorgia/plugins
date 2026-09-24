package com.roleplay.phone;

import com.roleplay.phone.call.CallManager;
import com.roleplay.phone.command.PhoneCommand;
import com.roleplay.phone.config.PhoneConfig;
import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.editor.GpsEditorListener;
import com.roleplay.phone.gps.editor.GpsEditorManager;
import com.roleplay.phone.gps.visual.GpsArrowRenderer;
import com.roleplay.phone.gui.PhoneGuiManager;
import com.roleplay.phone.gui.config.GuiConfig;
import com.roleplay.phone.lang.MessageManager;
import com.roleplay.phone.listener.PhoneChatCallListener;
import com.roleplay.phone.listener.PhoneInteractionListener;
import com.roleplay.phone.sms.SmsManager;
import com.roleplay.phone.storage.ProfileManager;
import com.roleplay.phone.voice.PhoneVoiceBridge;
import com.roleplay.phone.voice.PhoneVoicePlugin;
import com.roleplay.phone.voice.VoiceChatManager;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhonePlugin extends JavaPlugin {
    private static PhonePlugin instance;
    private MessageManager messageManager;
    private PhoneConfig phoneConfig;
    private GuiConfig guiConfig;
    private ProfileManager profileManager;
    private CallManager callManager;
    private SmsManager smsManager;
    private GpsManager gpsManager;
    private GpsArrowRenderer arrowRenderer;
    private GpsEditorManager editorManager;
    private PhoneGuiManager guiManager;

    public void onEnable() {
       instance = this;
       this.messageManager = new MessageManager(this);
       this.phoneConfig = new PhoneConfig(this);
       this.guiConfig = new GuiConfig(this);
       this.profileManager = new ProfileManager(this);
       this.callManager = new CallManager(this, this.profileManager, this.phoneConfig);
       this.smsManager = new SmsManager(this, this.profileManager, this.messageManager, this.callManager);
       this.gpsManager = new GpsManager(this);
       this.arrowRenderer = new GpsArrowRenderer(this, this.gpsManager);
       this.editorManager = new GpsEditorManager(this, this.gpsManager);
       this.guiManager = new PhoneGuiManager(this.profileManager, this.gpsManager, this.smsManager, this.guiConfig);
       this.callManager.setGuiManager(this.guiManager);
       this.setupVoiceChat();
       this.registerListeners();
       this.registerCommands();
       this.getComponentLogger().info(Component.text("RoleplayPhone, GPS, SMS & Custom GUI initialized successfully.", NamedTextColor.GREEN));
    }

    public void onDisable() {
       if (this.gpsManager != null) {
          this.gpsManager.cancelAll();
          this.gpsManager.save();
       }

       if (this.arrowRenderer != null) {
          this.arrowRenderer.cleanupAll();
       }

       if (this.editorManager != null) {
          this.editorManager.cleanup();
       }

       if (this.callManager != null) {
          this.callManager.terminateAllCalls();
       }

       if (this.profileManager != null) {
          this.profileManager.saveAll();
       }

       this.getComponentLogger().info(Component.text("RoleplayPhone data saved and shut down cleanly.", NamedTextColor.RED));
       instance = null;
    }

    private void setupVoiceChat() {
       if (this.getServer().getPluginManager().isPluginEnabled("voicechat")) {
          try {
             BukkitVoicechatService service = (BukkitVoicechatService)this.getServer().getServicesManager().load(BukkitVoicechatService.class);
             if (service != null) {
                service.registerPlugin(new PhoneVoicePlugin());
                PhoneVoiceBridge bridge = new VoiceChatManager(this.phoneConfig);
                this.callManager.setVoiceBridge(bridge);
                this.getComponentLogger().info(Component.text("Hooked into Simple Voice Chat successfully!", NamedTextColor.AQUA));
             }
          } catch (Throwable t) {
             this.getComponentLogger().warn(Component.text("Simple Voice Chat detected but failed to hook: " + t.getMessage(), NamedTextColor.YELLOW));
          }
       } else {
          this.getComponentLogger().info(Component.text("Simple Voice Chat not detected. Operating with text-only calls.", NamedTextColor.YELLOW));
       }

    }

    private void registerListeners() {
       PluginManager pm = this.getServer().getPluginManager();
       pm.registerEvents(new PhoneInteractionListener(this.guiManager, this.profileManager, this.callManager, this.gpsManager, this.arrowRenderer, this.smsManager), this);
       pm.registerEvents(new PhoneChatCallListener(this, this.callManager, this.profileManager, this.guiManager, this.smsManager, this.messageManager), this);
       pm.registerEvents(new GpsEditorListener(this.gpsManager, this.editorManager), this);
    }

    private void registerCommands() {
       Bukkit.getCommandMap().register("roleplayphone", new PhoneCommand(this.profileManager, this.callManager, this.guiManager, this.phoneConfig, this.gpsManager, this.editorManager, this.arrowRenderer));
    }

    public static PhonePlugin getInstance() {
       return instance;
    }

    public MessageManager getMessageManager() {
       return this.messageManager;
    }

    public PhoneConfig getPhoneConfig() {
       return this.phoneConfig;
    }

    public GuiConfig getGuiConfig() {
       return this.guiConfig;
    }

    public ProfileManager getProfileManager() {
       return this.profileManager;
    }

    public CallManager getCallManager() {
       return this.callManager;
    }

    public SmsManager getSmsManager() {
       return this.smsManager;
    }

    public GpsManager getGpsManager() {
       return this.gpsManager;
    }

    public GpsArrowRenderer getArrowRenderer() {
       return this.arrowRenderer;
    }

    public GpsEditorManager getEditorManager() {
       return this.editorManager;
    }

    public PhoneGuiManager getGuiManager() {
       return this.guiManager;
    }
}
