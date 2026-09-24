package com.roleplay.phone.gui.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiItemConfig {
   private final int slot;
   private final Material material;
   private final int customModelData;
   private final NamespacedKey itemModel;
   private final String name;
   private final List<String> lore;
   private final GuiSound sound;

   public GuiItemConfig(int slot, Material material, int customModelData, NamespacedKey itemModel, String name, List<String> lore, GuiSound sound) {
      this.slot = slot;
      this.material = material != null ? material : Material.STONE;
      this.customModelData = customModelData;
      this.itemModel = itemModel;
      this.name = name != null ? name : "";
      this.lore = (List<String>)(lore != null ? new ArrayList(lore) : Collections.emptyList());
      this.sound = sound;
   }

   public GuiItemConfig(int slot, Material material, int customModelData, String name, List<String> lore, GuiSound sound) {
      this(slot, material, customModelData, (NamespacedKey)null, name, lore, sound);
   }

   public int getSlot() {
      return this.slot;
   }

   public Material getMaterial() {
      return this.material;
   }

   public int getCustomModelData() {
      return this.customModelData;
   }

   public NamespacedKey getItemModel() {
      return this.itemModel;
   }

   public String getName() {
      return this.name;
   }

   public List<String> getLore() {
      return Collections.unmodifiableList(this.lore);
   }

   public GuiSound getSound() {
      return this.sound;
   }

   public void playSound(Player player) {
      if (this.sound != null) {
         this.sound.play(player);
      }

   }

   public ItemStack build(MiniMessage mm, TagResolver... resolvers) {
      ItemStack item = new ItemStack(this.material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         if (this.customModelData > 0) {
            meta.setCustomModelData(this.customModelData);
         }

         if (this.itemModel != null) {
            meta.setItemModel(this.itemModel);
         }

         TagResolver combined = resolvers != null && resolvers.length > 0 ? TagResolver.resolver(resolvers) : TagResolver.empty();
         if (!this.name.isEmpty()) {
            Component parsedName = mm.deserialize(this.name, combined).decoration(TextDecoration.ITALIC, false);
            meta.displayName(parsedName);
         }

         if (!this.lore.isEmpty()) {
            List<Component> builtLore = new ArrayList();

            for(String line : this.lore) {
               builtLore.add(mm.deserialize(line, combined).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(builtLore);
         }

         item.setItemMeta(meta);
         return item;
      }
   }

   public static GuiItemConfig fromSection(ConfigurationSection sec, int defSlot, Material defMat, String defName, List<String> defLore, GuiSound defSound) {
      if (sec == null) {
         return new GuiItemConfig(defSlot, defMat, 0, (NamespacedKey)null, defName, defLore, defSound);
      } else {
         int slot = sec.getInt("slot", defSlot);
         String matStr = sec.getString("material", defMat.name());

         Material mat;
         try {
            mat = Material.valueOf(matStr.toUpperCase());
         } catch (IllegalArgumentException var15) {
            mat = defMat;
         }

         int cmd = sec.getInt("custom-model-data", 0);
         String modelStr = sec.getString("item-model", "");
         NamespacedKey itemModel = null;
         if (modelStr != null && !modelStr.isBlank()) {
            itemModel = NamespacedKey.fromString(modelStr.toLowerCase());
         }

         String name = sec.getString("name", defName);
         List<String> lore = sec.isList("lore") ? sec.getStringList("lore") : defLore;
         GuiSound sound = GuiSound.fromSection(sec.getConfigurationSection("sound"), defSound != null ? defSound.sound() : null, defSound != null ? defSound.volume() : 1.0F, defSound != null ? defSound.pitch() : 1.0F);
         return new GuiItemConfig(slot, mat, cmd, itemModel, name, lore, sound);
      }
   }
}
