package com.moonlight.vanilla.modules.wraps;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

public class TestModelSupport {
    public static void test(ItemMeta meta) {
        try {
            // Check if setItemModel exists in Paper 1.21.4 ItemMeta
            ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
            System.out.println("setItemModel is supported!");
        } catch (NoSuchMethodException e) {
            System.out.println("setItemModel is not present.");
        }
    }
}
