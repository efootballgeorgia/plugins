package com.roleplay.phone.gps.visual;

import com.roleplay.phone.PhonePlugin;
import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.model.GpsNode;
import com.roleplay.phone.gps.model.GpsSession;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GpsArrowRenderer implements Listener {
   private final PhonePlugin plugin;
   private final GpsManager gpsManager;
   private final GpsArrowConfig arrowConfig;
   private final Map<UUID, GpsBlockArrow> activeArrows = new ConcurrentHashMap();
   private final Set<UUID> moveModePlayers = ConcurrentHashMap.newKeySet();
   private BukkitTask renderTask;

   public GpsArrowRenderer(PhonePlugin plugin, GpsManager gpsManager) {
      this.plugin = plugin;
      this.gpsManager = gpsManager;
      this.arrowConfig = new GpsArrowConfig();
      this.arrowConfig.loadFromConfig(plugin.getConfig());
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      this.startRenderLoop();
   }

   public GpsArrowConfig getArrowConfig() {
      return this.arrowConfig;
   }

   public void startTracking(Player player, GpsSession session) {
      this.stopTracking(player.getUniqueId());
      GpsBlockArrow arrow = new GpsBlockArrow(this.plugin, player, this.arrowConfig);
      this.activeArrows.put(player.getUniqueId(), arrow);
   }

   public void stopTracking(UUID playerUuid) {
      this.moveModePlayers.remove(playerUuid);
      GpsBlockArrow arrow = (GpsBlockArrow)this.activeArrows.remove(playerUuid);
      if (arrow != null) {
         arrow.destroy();
      }

   }

   public void toggleMoveMode(Player player) {
      UUID uuid = player.getUniqueId();
      if (!this.activeArrows.containsKey(uuid)) {
         player.sendMessage(Component.text("✖ You must have an active GPS route to adjust arrow position.", NamedTextColor.RED));
      } else {
         if (this.moveModePlayers.contains(uuid)) {
            this.moveModePlayers.remove(uuid);
            player.sendMessage(Component.text("✔ Exited Arrow Move Mode.", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 1.0F);
         } else {
            this.moveModePlayers.add(uuid);
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("--- GPS Arrow Move Mode ---", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            player.sendMessage(Component.text("• Scroll Wheel: ", NamedTextColor.YELLOW).append(Component.text("Move arrow Forward / Backward", NamedTextColor.WHITE)));
            player.sendMessage(Component.text("• Shift + Scroll: ", NamedTextColor.YELLOW).append(Component.text("Move arrow Up / Down", NamedTextColor.WHITE)));
            player.sendMessage(((TextComponent)Component.text("Run ", NamedTextColor.GRAY).append(Component.text("/phone gps move", NamedTextColor.YELLOW))).append(Component.text(" again to lock position.", NamedTextColor.GRAY)));
            player.sendMessage(Component.empty());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.6F, 1.5F);
         }

      }
   }

   public boolean isMoveMode(UUID uuid) {
      return this.moveModePlayers.contains(uuid);
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onPlayerItemHeld(PlayerItemHeldEvent event) {
      Player player = event.getPlayer();
      if (this.moveModePlayers.contains(player.getUniqueId())) {
         GpsBlockArrow arrow = (GpsBlockArrow)this.activeArrows.get(player.getUniqueId());
         if (arrow != null) {
            int prev = event.getPreviousSlot();
            int next = event.getNewSlot();
            int diff = next - prev;
            if (diff == -8) {
               diff = 1;
            }

            if (diff == 8) {
               diff = -1;
            }

            double step = (double)diff * 0.15;
            if (player.isSneaking()) {
               arrow.adjustHeight(step);
            } else {
               arrow.adjustForward(step);
            }

            Component feedback = ((TextComponent)((TextComponent)Component.text("Arrow Offset » Height: ", NamedTextColor.GRAY).append(Component.text(String.format("%.2fm", arrow.getHeightOffset()), NamedTextColor.AQUA))).append(Component.text(" | Forward: ", NamedTextColor.GRAY))).append(Component.text(String.format("%.2fm", arrow.getForwardOffset()), NamedTextColor.AQUA));
            player.sendActionBar(feedback);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3F, 1.6F);
         }
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.stopTracking(event.getPlayer().getUniqueId());
   }

   public void cleanupAll() {
      if (this.renderTask != null) {
         this.renderTask.cancel();
      }

      for(GpsBlockArrow arrow : this.activeArrows.values()) {
         arrow.destroy();
      }

      this.activeArrows.clear();
      this.moveModePlayers.clear();
   }

   private void startRenderLoop() {
      this.renderTask = (new BukkitRunnable() {
         public void run() {
            for(Map.Entry<UUID, GpsBlockArrow> entry : GpsArrowRenderer.this.activeArrows.entrySet()) {
               UUID uuid = (UUID)entry.getKey();
               GpsBlockArrow arrow = (GpsBlockArrow)entry.getValue();
               Player player = Bukkit.getPlayer(uuid);
               if (player != null && player.isOnline()) {
                  GpsSession session = GpsArrowRenderer.this.gpsManager.getSession(uuid);
                  if (session != null && session.getStatus() == GpsSession.Status.NAVIGATING) {
                     GpsNode targetNode = session.getCurrentTargetNode();
                     Location targetLoc = targetNode != null && targetNode.toLocation() != null ? targetNode.toLocation().clone().add((double)0.5F, (double)0.5F, (double)0.5F) : session.getDestinationLocation().clone().add((double)0.5F, (double)0.5F, (double)0.5F);
                     double distance = session.getRemainingDistance(player.getLocation());
                     arrow.update(player, targetLoc, distance);
                  } else {
                     GpsArrowRenderer.this.stopTracking(uuid);
                  }
               } else {
                  GpsArrowRenderer.this.stopTracking(uuid);
               }
            }

         }
      }).runTaskTimer(this.plugin, 1L, 1L);
   }
}
