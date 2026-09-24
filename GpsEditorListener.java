package com.roleplay.phone.gps.editor;

import com.roleplay.phone.gps.GpsManager;
import com.roleplay.phone.gps.model.GpsNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GpsEditorListener implements Listener {
   private final GpsManager gpsManager;
   private final GpsEditorManager editorManager;

   public GpsEditorListener(GpsManager gpsManager, GpsEditorManager editorManager) {
      this.gpsManager = gpsManager;
      this.editorManager = editorManager;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getHand() == EquipmentSlot.HAND) {
         Player player = event.getPlayer();
         if (this.editorManager.isWand(event.getItem())) {
            if (!player.hasPermission("phone.gps.admin") && !player.isOp()) {
               player.sendMessage(Component.text("✖ You do not have permission to use the GPS Configurator Wand.", NamedTextColor.RED));
               event.setCancelled(true);
            } else {
               event.setCancelled(true);
               boolean sneaking = player.isSneaking();
               Action action = event.getAction();
               if (!sneaking || action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
                  if (!sneaking && action == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                     this.handleCreateNode(player, event.getClickedBlock());
                  } else if (!sneaking || action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                     if (!sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
                        this.handleSelectOrConnect(player);
                     }

                  } else {
                     this.handleToggleDestination(player);
                  }
               } else {
                  this.handleDeleteNode(player);
               }
            }
         }
      }
   }

   private void handleCreateNode(Player player, Block clickedBlock) {
      Location nodeLoc = clickedBlock.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);
      String newId = this.gpsManager.generateNextNodeId();
      GpsNode node = new GpsNode(newId, nodeLoc);
      this.gpsManager.registerNode(node);
      String previous = this.editorManager.getSelectedNode(player.getUniqueId());
      if (previous != null && this.gpsManager.getNode(previous) != null) {
         this.gpsManager.connectNodes(previous, newId, true);
         player.sendMessage(Component.text("✔ Node " + newId + " created and linked to " + previous + "!", NamedTextColor.GREEN));
      } else {
         player.sendMessage(Component.text("✔ Node " + newId + " created!", NamedTextColor.GREEN));
      }

      this.editorManager.setSelectedNode(player.getUniqueId(), newId);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6F, 1.8F);
   }

   private void handleDeleteNode(Player player) {
      GpsNode closest = this.findClosestNodeNear(player.getLocation(), (double)3.5F);
      if (closest == null) {
         player.sendMessage(Component.text("✖ No GPS node found nearby to delete.", NamedTextColor.RED));
      } else {
         String id = closest.getId();
         this.gpsManager.removeNode(id);
         if (id.equals(this.editorManager.getSelectedNode(player.getUniqueId()))) {
            this.editorManager.setSelectedNode(player.getUniqueId(), (String)null);
         }

         player.sendMessage(Component.text("✔ Removed node: " + id, NamedTextColor.RED));
         player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.4F, 1.4F);
      }
   }

   private void handleToggleDestination(Player player) {
      GpsNode closest = this.findClosestNodeNear(player.getLocation(), (double)3.5F);
      if (closest == null) {
         player.sendMessage(Component.text("✖ No GPS node found nearby.", NamedTextColor.RED));
      } else {
         boolean newState = !closest.isDestination();
         closest.setDestination(newState);
         if (newState && closest.getDestinationName().isBlank()) {
            closest.setDestinationName("Point " + closest.getId());
         }

         this.gpsManager.save();
         player.sendMessage(Component.text("✔ Destination status for " + closest.getId() + ": " + (newState ? "ENABLED (" + closest.getDestinationName() + ")" : "DISABLED"), NamedTextColor.AQUA));
         player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 1.5F);
      }
   }

   private void handleSelectOrConnect(Player player) {
      GpsNode closest = this.findClosestNodeNear(player.getLocation(), (double)3.5F);
      if (closest == null) {
         this.editorManager.setSelectedNode(player.getUniqueId(), (String)null);
         player.sendMessage(Component.text("Cleared selected node.", NamedTextColor.YELLOW));
      } else {
         String currentSelected = this.editorManager.getSelectedNode(player.getUniqueId());
         if (currentSelected == null) {
            this.editorManager.setSelectedNode(player.getUniqueId(), closest.getId());
            player.sendMessage(Component.text("✔ Selected node: " + closest.getId(), NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 1.5F);
         } else if (currentSelected.equalsIgnoreCase(closest.getId())) {
            this.editorManager.setSelectedNode(player.getUniqueId(), (String)null);
            player.sendMessage(Component.text("Deselected node " + closest.getId(), NamedTextColor.GRAY));
         } else {
            this.gpsManager.connectNodes(currentSelected, closest.getId(), true);
            player.sendMessage(Component.text("✔ Connected " + currentSelected + " ⮂ " + closest.getId(), NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6F, 2.0F);
            this.editorManager.setSelectedNode(player.getUniqueId(), closest.getId());
         }

      }
   }

   private GpsNode findClosestNodeNear(Location loc, double maxDistance) {
      GpsNode closest = null;
      double maxSq = maxDistance * maxDistance;
      double minDistSq = maxSq;

      for(GpsNode node : this.gpsManager.getNodes().values()) {
         Location nLoc = node.toLocation();
         if (nLoc != null && nLoc.getWorld().equals(loc.getWorld())) {
            double distSq = nLoc.distanceSquared(loc);
            if (distSq < minDistSq) {
               minDistSq = distSq;
               closest = node;
            }
         }
      }

      return closest;
   }

   @EventHandler
   public void onBlockPlace(BlockPlaceEvent event) {
      if (this.editorManager.isWand(event.getItemInHand())) {
         event.setCancelled(true);
      }

   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.editorManager.setSelectedNode(event.getPlayer().getUniqueId(), (String)null);
   }
}
