package com.roleplay.phone.gps.visual;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;

public class GpsArrowConfig {
    private Material blockMaterial;
    private float blockScale;
    private int shaftLength;
    private int wingLength;
    private double defaultHeightOffset;
    private double defaultForwardOffset;
    private boolean headerEnabled;
    private String headerFormat;

    public GpsArrowConfig() {
       this.blockMaterial = Material.SMOOTH_QUARTZ;
       this.blockScale = 0.22F;
       this.shaftLength = 5;
       this.wingLength = 3;
       this.defaultHeightOffset = (double)1.0F;
       this.defaultForwardOffset = (double)4.0F;
       this.headerEnabled = true;
       this.headerFormat = "%.2f to destination";
    }

    public void loadFromConfig(FileConfiguration config) {
       String matStr = config.getString("gps.arrow.material", "SMOOTH_QUARTZ");

       try {
          this.blockMaterial = Material.valueOf(matStr.toUpperCase());
       } catch (IllegalArgumentException var4) {
          this.blockMaterial = Material.SMOOTH_QUARTZ;
       }

       this.blockScale = (float)config.getDouble("gps.arrow.block-scale", 0.22);
       this.shaftLength = config.getInt("gps.arrow.shaft-length", 5);
       this.wingLength = config.getInt("gps.arrow.wing-length", 3);
       this.defaultHeightOffset = config.getDouble("gps.arrow.height-offset", (double)1.0F);
       this.defaultForwardOffset = config.getDouble("gps.arrow.forward-offset", (double)4.0F);
       this.headerEnabled = config.getBoolean("gps.arrow.header-enabled", true);
       this.headerFormat = config.getString("gps.arrow.header-format", "%.2f to destination");
    }

    public Material getBlockMaterial() {
       return this.blockMaterial;
    }

    public BlockData getBlockData() {
       return this.blockMaterial.createBlockData();
    }

    public float getBlockScale() {
       return this.blockScale;
    }

    public int getShaftLength() {
       return this.shaftLength;
    }

    public int getWingLength() {
       return this.wingLength;
    }

    public double getDefaultHeightOffset() {
       return this.defaultHeightOffset;
    }

    public double getDefaultForwardOffset() {
       return this.defaultForwardOffset;
    }

    public boolean isHeaderEnabled() {
       return this.headerEnabled;
    }

    public String getHeaderFormat() {
       return this.headerFormat;
    }
}
