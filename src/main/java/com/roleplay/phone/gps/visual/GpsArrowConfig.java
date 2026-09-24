package com.roleplay.phone.lang;

import com.roleplay.phone.PhonePlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MessageManager {
    private final PhonePlugin plugin;
    private final MiniMessage miniMessage;
    private FileConfiguration config;
    private File file;

    public MessageManager(PhonePlugin plugin) {
       this.plugin = plugin;
       this.miniMessage = MiniMessage.miniMessage();
       this.reload();
    }

    public void reload() {
       this.file = new File(this.plugin.getDataFolder(), "messages.yml");
       if (!this.file.exists()) {
          this.plugin.saveResource("messages.yml", false);
       }

       this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    public Component getRaw(String path) {
       String msg = this.config.getString(path, "<red>Missing message: " + path + "</red>");
       return this.miniMessage.deserialize(msg);
    }

    public Component get(String path, String... placeholders) {
       String msg = this.config.getString(path, "<red>Missing message: " + path + "</red>");
       TagResolver resolver = this.buildResolver(placeholders);
       return this.miniMessage.deserialize(msg, resolver);
    }

    public Component getWithPrefix(String path, String... placeholders) {
       Component prefix = this.getRaw("prefix");
       return prefix.append(this.get(path, placeholders));
    }

    public void send(Audience audience, String path, String... placeholders) {
       audience.sendMessage(this.getWithPrefix(path, placeholders));
    }

    public void sendRaw(Audience audience, String path, String... placeholders) {
       audience.sendMessage(this.get(path, placeholders));
    }

    public void sendActionBar(Player player, String path, String... placeholders) {
       player.sendActionBar(this.get(path, placeholders));
    }

    private TagResolver buildResolver(String... placeholders) {
       if (placeholders != null && placeholders.length != 0) {
          List<TagResolver> resolvers = new ArrayList();

          for(int i = 0; i < placeholders.length - 1; i += 2) {
             String key = placeholders[i];
             String val = placeholders[i + 1];
             if (key != null && val != null) {
                resolvers.add(Placeholder.parsed(key, val));
             }
          }

          return TagResolver.resolver(resolvers);
       } else {
          return TagResolver.empty();
       }
    }
}
