package com.roleplay.phone.gps.visual;

import com.roleplay.phone.PhonePlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class GpsBlockArrow {
   private final PhonePlugin plugin;
   private final UUID playerUuid;
   private final GpsArrowConfig config;
   private final List<BlockDisplay> blockDisplays = new ArrayList();
   private final List<ArrowPart> arrowParts = new ArrayList();
   private TextDisplay headerDisplay;
   private double heightOffset;
   private double forwardOffset;
   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;

   public GpsBlockArrow(PhonePlugin plugin, Player player, GpsArrowConfig config) {
      this.plugin = plugin;
      this.playerUuid = player.getUniqueId();
      this.config = config;
      this.heightOffset = config.getDefaultHeightOffset();
      this.forwardOffset = config.getDefaultForwardOffset();
      this.buildArrowGeometry();
      this.spawnEntities(player);
   }

   private void buildArrowGeometry() {
      float scale = this.config.getBlockScale();
      double sin45 = Math.sin((Math.PI / 4D));

      for(int i = 0; i < this.config.getWingLength(); ++i) {
         double distance = ((double)i + (double)0.5F) * (double)scale;
         double x = -distance * sin45;
         double z = -distance * sin45;
         this.arrowParts.add(new ArrowPart(new Vector(x, (double)0.0F, z), 45.0F));
      }

      for(int i = 0; i < this.config.getWingLength(); ++i) {
         double distance = ((double)i + (double)0.5F) * (double)scale;
         double x = distance * sin45;
         double z = -distance * sin45;
         this.arrowParts.add(new ArrowPart(new Vector(x, (double)0.0F, z), -45.0F));
      }

      double shaftStartOffset = (double)scale * 0.7071;

      for(int i = 0; i < this.config.getShaftLength(); ++i) {
         double z = -(shaftStartOffset + ((double)i + (double)0.5F) * (double)scale);
         this.arrowParts.add(new ArrowPart(new Vector((double)0.0F, (double)0.0F, z), 0.0F));
      }

   }

   private void spawnEntities(Player player) {
      Location spawnLoc = player.getLocation().add((double)0.0F, this.heightOffset, (double)0.0F);
      float scale = this.config.getBlockScale();
      Transformation transformation = new Transformation(new Vector3f(-scale / 2.0F, -scale / 2.0F, -scale / 2.0F), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f());

      for(int i = 0; i < this.arrowParts.size(); ++i) {
         BlockDisplay display = (BlockDisplay)player.getWorld().spawn(spawnLoc, BlockDisplay.class, (entity) -> {
            entity.setBlock(this.config.getBlockData());
            entity.setTransformation(transformation);
            entity.setBillboard(Billboard.FIXED);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setVisibleByDefault(false);
            entity.setTeleportDuration(1);
         });
         player.showEntity(this.plugin, display);
         this.blockDisplays.add(display);
      }

      if (this.config.isHeaderEnabled()) {
         Location headerLoc = spawnLoc.clone().add((double)0.0F, 0.45, (double)0.0F);
         this.headerDisplay = (TextDisplay)player.getWorld().spawn(headerLoc, TextDisplay.class, (entity) -> {
            entity.setBillboard(Billboard.CENTER);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setVisibleByDefault(false);
            entity.setTeleportDuration(1);
            entity.text(Component.text("Calculating...", NamedTextColor.GREEN));
         });
         player.showEntity(this.plugin, this.headerDisplay);
      }

   }

   public void update(Player player, Location targetLocation, double distanceMeters) {
      Location playerLoc = player.getLocation();
      Vector lookDir = playerLoc.getDirection().setY(0).normalize();
      Location pivot = playerLoc.clone().add((double)0.0F, this.heightOffset, (double)0.0F);
      if (this.forwardOffset != (double)0.0F && !Double.isNaN(lookDir.getX())) {
         pivot.add(lookDir.multiply(this.forwardOffset));
      }

      Vector toTarget = targetLocation.toVector().subtract(pivot.toVector());
      double horizontalDist = Math.sqrt(toTarget.getX() * toTarget.getX() + toTarget.getZ() * toTarget.getZ());
      float targetYaw;
      float targetPitch;
      if (horizontalDist < 0.2) {
         targetYaw = this.lastYaw;
         targetPitch = this.lastPitch;
      } else {
         targetYaw = (float)Math.toDegrees(Math.atan2(-toTarget.getX(), toTarget.getZ()));
         float targetPitch = (float)Math.toDegrees(-Math.atan2(toTarget.getY(), horizontalDist));
         targetPitch = Math.max(-65.0F, Math.min(65.0F, targetPitch));
         this.lastYaw = targetYaw;
         this.lastPitch = targetPitch;
      }

      double pitchRad = Math.toRadians((double)targetPitch);
      double cosPitch = Math.cos(pitchRad);
      double sinPitch = Math.sin(pitchRad);
      double yawRad = Math.toRadians((double)targetYaw);
      double cosYaw = Math.cos(yawRad);
      double sinYaw = Math.sin(yawRad);

      for(int i = 0; i < this.blockDisplays.size(); ++i) {
         BlockDisplay display = (BlockDisplay)this.blockDisplays.get(i);
         if (display.isValid()) {
            ArrowPart part = (ArrowPart)this.arrowParts.get(i);
            Vector local = part.localOffset();
            double x1 = local.getX();
            double y1 = local.getY() * cosPitch - local.getZ() * sinPitch;
            double z1 = local.getY() * sinPitch + local.getZ() * cosPitch;
            double rotX = x1 * cosYaw - z1 * sinYaw;
            double rotZ = x1 * sinYaw + z1 * cosYaw;
            Location blockLoc = pivot.clone().add(rotX, y1, rotZ);
            blockLoc.setYaw(targetYaw + part.localYaw());
            blockLoc.setPitch(targetPitch);
            display.teleport(blockLoc);
         }
      }

      if (this.headerDisplay != null && this.headerDisplay.isValid()) {
         double headUpX = -sinPitch * sinYaw * 0.45;
         double headUpY = cosPitch * 0.45;
         double headUpZ = sinPitch * cosYaw * 0.45;
         Location headLoc = pivot.clone().add(headUpX, headUpY, headUpZ);
         this.headerDisplay.teleport(headLoc);
         String formatted = String.format(this.config.getHeaderFormat(), distanceMeters);
         this.headerDisplay.text(Component.text(formatted, NamedTextColor.GREEN));
      }

   }

   public void adjustHeight(double delta) {
      this.heightOffset = Math.max((double)0.0F, Math.min((double)6.0F, this.heightOffset + delta));
   }

   public void adjustForward(double delta) {
      this.forwardOffset = Math.max((double)-2.0F, Math.min((double)8.0F, this.forwardOffset + delta));
   }

   public double getHeightOffset() {
      return this.heightOffset;
   }

   public double getForwardOffset() {
      return this.forwardOffset;
   }

   public void destroy() {
      for(BlockDisplay bd : this.blockDisplays) {
         if (bd != null && bd.isValid()) {
            bd.remove();
         }
      }

      this.blockDisplays.clear();
      if (this.headerDisplay != null && this.headerDisplay.isValid()) {
         this.headerDisplay.remove();
      }

      this.headerDisplay = null;
   }

   private static record ArrowPart(Vector localOffset, float localYaw) {
   }
}
