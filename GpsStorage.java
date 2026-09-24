package com.roleplay.phone.gps.storage;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.gps.model.GpsNode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class GpsStorage {
   private final PhonePlugin plugin;
   private final File file;

   public GpsStorage(PhonePlugin plugin) {
      this.plugin = plugin;
      this.file = new File(plugin.getDataFolder(), "gps_network.yml");
   }

   public Map<String, GpsNode> loadAll() {
      Map<String, GpsNode> nodes = new HashMap();
      if (!this.file.exists()) {
         return nodes;
      } else {
         YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
         ConfigurationSection nodesSection = config.getConfigurationSection("nodes");
         if (nodesSection == null) {
            return nodes;
         } else {
            for(String id : nodesSection.getKeys(false)) {
               ConfigurationSection sec = nodesSection.getConfigurationSection(id);
               if (sec != null) {
                  String world = sec.getString("world", "world");
                  double x = sec.getDouble("x");
                  double y = sec.getDouble("y");
                  double z = sec.getDouble("z");
                  GpsNode node = new GpsNode(id, world, x, y, z);

                  for(String neighbor : sec.getStringList("neighbors")) {
                     node.addNeighbor(neighbor);
                  }

                  node.setDestination(sec.getBoolean("is-destination", false));
                  node.setDestinationName(sec.getString("destination-name", ""));
                  node.setCategory(sec.getString("category", "General"));
                  String matName = sec.getString("icon-material", "COMPASS");

                  Material icon;
                  try {
                     icon = Material.valueOf(matName.toUpperCase());
                  } catch (IllegalArgumentException var19) {
                     icon = Material.COMPASS;
                  }

                  node.setIconMaterial(icon);
                  node.setDescription(sec.getString("description", ""));
                  nodes.put(id, node);
               }
            }

            this.plugin.getLogger().info("Loaded " + nodes.size() + " GPS navigation nodes from disk.");
            return nodes;
         }
      }
   }

   public void saveAll(Collection<GpsNode> nodes) {
      YamlConfiguration config = new YamlConfiguration();
      ConfigurationSection nodesSection = config.createSection("nodes");

      for(GpsNode node : nodes) {
         ConfigurationSection sec = nodesSection.createSection(node.getId());
         sec.set("world", node.getWorldName());
         sec.set("x", node.getX());
         sec.set("y", node.getY());
         sec.set("z", node.getZ());
         sec.set("neighbors", List.copyOf(node.getNeighborIds()));
         sec.set("is-destination", node.isDestination());
         sec.set("destination-name", node.getDestinationName());
         sec.set("category", node.getCategory());
         sec.set("icon-material", node.getIconMaterial().name());
         sec.set("description", node.getDescription());
      }

      try {
         config.save(this.file);
      } catch (IOException e) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to save GPS nodes to " + this.file.getName(), e);
      }

   }
}
