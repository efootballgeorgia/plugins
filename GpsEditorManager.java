package com.roleplay.phone.gps.editor;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.model.GpsNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class GpsEditorManager {
   private final PhonePlugin plugin;
   private final GpsManager gpsManager;
   private final NamespacedKey wandKey;
   private final Set<UUID> activeEditors = ConcurrentHashMap.newKeySet();
   private final Map<UUID, String> selectedNodes = new ConcurrentHashMap();
   private BukkitTask visualizerTask;
   private static final Particle.DustOptions COLOR_NODE = new Particle.DustOptions(Color.fromRGB(0, 255, 128), 1.3F);
   private static final Particle.DustOptions COLOR_DESTINATION = new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1.8F);
   private static final Particle.DustOptions COLOR_SELECTED = new Particle.DustOptions(Color.fromRGB(0, 200, 255), 2.2F);
   private static final Particle.DustOptions COLOR_EDGE = new Particle.DustOptions(Color.fromRGB(180, 180, 180), 0.8F);

   public GpsEditorManager(PhonePlugin plugin, GpsManager gpsManager) {
      this.plugin = plugin;
      this.gpsManager = gpsManager;
      this.wandKey = new NamespacedKey(plugin, "gps_configurator_wand");
      this.startParticleVisualizer();
   }

   public boolean isEditor(UUID playerUuid) {
      return this.activeEditors.contains(playerUuid);
   }

   public void toggleEditor(Player player) {
      if (this.activeEditors.contains(player.getUniqueId())) {
         this.activeEditors.remove(player.getUniqueId());
         this.selectedNodes.remove(player.getUniqueId());
         player.sendMessage(Component.text("✖ Exited GPS Configurator mode.", NamedTextColor.RED));
      } else {
         this.activeEditors.add(player.getUniqueId());
         player.getInventory().addItem(new ItemStack[]{this.createWand()});
         player.sendMessage(Component.text("✔ Entered GPS Configurator mode. Wand added to inventory.", NamedTextColor.GREEN));
      }

   }

   public String getSelectedNode(UUID playerUuid) {
      return (String)this.selectedNodes.get(playerUuid);
   }

   public void setSelectedNode(UUID playerUuid, String nodeId) {
      if (nodeId == null) {
         this.selectedNodes.remove(playerUuid);
      } else {
         this.selectedNodes.put(playerUuid, nodeId);
      }

   }

   public ItemStack createWand() {
      ItemStack item = new ItemStack(Material.BLAZE_ROD);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(((TextComponent)Component.text("GPS Configurator Wand", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)).decoration(TextDecoration.BOLD, true));
         meta.lore(List.of((TextComponent)((TextComponent)Component.text("Left-Click Block: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)).append(Component.text("Create new Node", NamedTextColor.GRAY)), (TextComponent)((TextComponent)Component.text("Right-Click Node: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)).append(Component.text("Connect / Select Node", NamedTextColor.GRAY)), (TextComponent)((TextComponent)Component.text("Shift + Left-Click: ", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)).append(Component.text("Delete Nearest Node", NamedTextColor.GRAY)), (TextComponent)((TextComponent)Component.text("Shift + Right-Click: ", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)).append(Component.text("Toggle Destination Mode", NamedTextColor.GRAY))));
         meta.getPersistentDataContainer().set(this.wandKey, PersistentDataType.BYTE, (byte)1);
         item.setItemMeta(meta);
      }

      return item;
   }

   public boolean isWand(ItemStack item) {
      if (item != null && item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return false;
         } else {
            Byte val = (Byte)meta.getPersistentDataContainer().get(this.wandKey, PersistentDataType.BYTE);
            return val != null && val == 1;
         }
      } else {
         return false;
      }
   }

   private void startParticleVisualizer() {
      this.visualizerTask = (new BukkitRunnable() {
         public void run() {
            if (!GpsEditorManager.this.activeEditors.isEmpty()) {
               for(UUID uuid : GpsEditorManager.this.activeEditors) {
                  Player player = Bukkit.getPlayer(uuid);
                  if (player != null && player.isOnline()) {
                     GpsEditorManager.this.renderEditorParticles(player);
                  } else {
                     GpsEditorManager.this.activeEditors.remove(uuid);
                     GpsEditorManager.this.selectedNodes.remove(uuid);
                  }
               }

            }
         }
      }).runTaskTimer(this.plugin, 10L, 8L);
   }

   private void renderEditorParticles(Player player) {
      Location pLoc = player.getLocation();
      String selected = (String)this.selectedNodes.get(player.getUniqueId());

      for(GpsNode node : this.gpsManager.getNodes().values()) {
         Location nLoc = node.toLocation();
         if (nLoc != null && nLoc.getWorld().equals(pLoc.getWorld()) && !(nLoc.distanceSquared(pLoc) > (double)1296.0F)) {
            boolean isSelected = node.getId().equalsIgnoreCase(selected);
            Particle.DustOptions dust = isSelected ? COLOR_SELECTED : (node.isDestination() ? COLOR_DESTINATION : COLOR_NODE);
            Location nodeCenter = nLoc.clone().add((double)0.5F, 0.2, (double)0.5F);
            player.spawnParticle(Particle.DUST, nodeCenter, 4, 0.1, 0.1, 0.1, (double)0.0F, dust);
            if (isSelected) {
               player.spawnParticle(Particle.DUST, nodeCenter.clone().add((double)0.0F, 0.8, (double)0.0F), 2, 0.05, 0.2, 0.05, (double)0.0F, COLOR_SELECTED);
            }

            for(String neighborId : node.getNeighborIds()) {
               GpsNode neighbor = this.gpsManager.getNode(neighborId);
               if (neighbor != null) {
                  Location neighborLoc = neighbor.toLocation();
                  if (neighborLoc != null && neighborLoc.getWorld().equals(pLoc.getWorld()) && node.getId().compareTo(neighbor.getId()) < 0) {
                     this.drawParticleLine(player, nodeCenter, neighborLoc.clone().add((double)0.5F, 0.2, (double)0.5F));
                  }
               }
            }
         }
      }

   }

   private void drawParticleLine(Player player, Location from, Location to) {
      Vector dir = to.toVector().subtract(from.toVector());
      double length = dir.length();
      dir.normalize();

      for(double d = (double)0.5F; d < length; d += 0.8) {
         Location point = from.clone().add(dir.clone().multiply(d));
         player.spawnParticle(Particle.DUST, point, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, COLOR_EDGE);
      }

   }

   public void cleanup() {
      if (this.visualizerTask != null) {
         this.visualizerTask.cancel();
      }

      this.activeEditors.clear();
      this.selectedNodes.clear();
   }
}
