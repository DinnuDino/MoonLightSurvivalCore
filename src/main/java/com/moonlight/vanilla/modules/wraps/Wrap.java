package com.moonlight.vanilla.modules.wraps;

public class Wrap {
    private final String key;
    private final String name;
    private final String category;
    private final String colorFormat;
    private final String tier;
    private final String model;
    private final String modelName;
    private final int customModelData;
    private final String permission;

    public Wrap(String key, String name, String category, String colorFormat, String tier, String model, String modelName, int customModelData, String permission) {
        this.key = key;
        this.name = name;
        this.category = category != null ? category : "SWORD";
        this.colorFormat = colorFormat != null ? colorFormat : name;
        this.tier = tier != null ? tier : "default";
        this.model = model;
        this.modelName = modelName;
        this.customModelData = customModelData;
        this.permission = permission != null ? permission : "moonlight.wraps.use." + key;
    }

    public Wrap(String key, String name, String category, String colorFormat, String tier, String model, String modelName, String permission) {
        this(key, name, category, colorFormat, tier, model, modelName, 0, permission);
    }

    public Wrap(String key, String name, String category, String colorFormat, String model, String modelName, String permission) {
        this(key, name, category, colorFormat, "default", model, modelName, 0, permission);
    }

    public Wrap(String key, String name, String model, String modelName, String permission) {
        this(key, name, "SWORD", name, "default", model, modelName, 0, permission);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getColorFormat() {
        return colorFormat;
    }

    public String getTier() {
        return tier;
    }

    public String getModel() {
        return model;
    }

    public String getModelName() {
        return modelName;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getPermission() {
        return permission;
    }
}
