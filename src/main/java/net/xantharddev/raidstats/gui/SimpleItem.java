package net.xantharddev.raidstats.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.xantharddev.raidstats.objects.Colour;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This class implements an easy way to map
 * config values like: materials, lore, name, color
 * to a respective ItemStack, also has utility methods
 * to merge, clone, etc
 */
public class SimpleItem implements Cloneable {
    private String name;
    private int amount;
    private boolean unbreakable;
    private List<String> lore;
    private Material material;
    private short damage;
    private boolean glowing;
    private String owner;
    private String url;



    SimpleItem(Builder builder) {
        this.name = builder.name;
        this.lore = builder.lore;
        this.unbreakable = builder.unbreakable;
        this.damage = builder.damage;
        this.material = builder.material;
        this.glowing = builder.glowing;
        this.amount = builder.amount;
        this.url = builder.url;
    }

    public SimpleItem(SimpleItem item) {
        this.name = item.name;
        this.amount = item.amount;
        this.unbreakable = item.unbreakable;
        this.lore = item.lore;
        this.damage = item.damage;
        this.material = item.material;
        this.glowing = item.glowing;
        this.owner = item.owner;
        this.url = item.url;
    }

    @Override
    public SimpleItem clone() {
        try {
            SimpleItem cloned = (SimpleItem) super.clone();
            if (this.lore != null) {
                cloned.lore = new ArrayList<>(this.lore);
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            return SimpleItem.builder().build();
        }
    }

    public ItemStack get() {
        if (!isValid()) return new ItemStack(Material.AIR);
        final ItemStack itemStack = new ItemStack(material);
        if (material == Material.AIR) return itemStack;
        final ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return itemStack;

        if (amount != 0) itemStack.setAmount(amount);
        if (name != null) meta.setDisplayName(name);

        // Empty list if not specified
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        if (glowing) meta.addEnchant(Enchantment.LUCK, 1, true);

        itemStack.setItemMeta(meta);
        if (material == Material.SKULL_ITEM) {
            final SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            if (owner != null && !owner.isEmpty()) {
                skullMeta.setOwner(owner);
            }
            itemStack.setItemMeta(skullMeta);
            if (url != null && !url.isEmpty()) {
                if (!url.startsWith("http://textures.minecraft.net/texture/")) {
                    url = "http://textures.minecraft.net/texture/" + url;
                }
                return getLegacySkull(urlToBase64(url));
            }
        }

        itemStack.setDurability(damage);
        return itemStack;
    }


    private ItemStack getLegacySkull(String base64) {
        final ItemStack item = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        final SkullMeta meta = (SkullMeta) item.getItemMeta();
        final GameProfile profile = new GameProfile(UUID.randomUUID(), null);

        profile.getProperties().put("textures", new Property("textures", base64));

        try {
            final Field field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
            item.setItemMeta(meta);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error getting skull item: " + base64, e);
        }

        if (name != null) {
            meta.setDisplayName(Colour.colour(name));
        }
        // Empty list if not specified
        if (lore != null) meta.setLore(Colour.colourList(lore));
        item.setItemMeta(meta);

        return item;
    }

    private String urlToBase64(String url) {
        try {
            new URI(url);
        } catch (URISyntaxException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Invalid URL: " + url, e);
            return url;
        }
        return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes());
    }

    // All non-null values in 'from' will be merged into this ItemGUI
    public void merge(SimpleItem from) {
        if (from.material != null) {
            material = from.material;
        }
        if (from.name != null) {
            name = from.name;
        }
        if (!from.lore.isEmpty()) {
            lore = from.lore;
        }
    }

    public boolean isValid() {
        // For an ItemStack to be built this class needs the material, if more information is available then it will be used
        return material != null;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAmount() {
        return this.amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public Material getMaterial() { return material; }
    public boolean getUnbreakable() { return unbreakable; }
    public void setMaterial(Material material) { this.material = material; }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) { this.url = url; }
    public void setUnbreakable(boolean bool) { this.unbreakable = bool; }
    public boolean isGlowing() {return glowing;}
    public void setGlowing(boolean enchant) {this.glowing = enchant;}
    public static Builder builder() {return new Builder();}
    public void setOwner(String owner) {this.owner = owner;}

    public static class Builder {
        private Material material;
        private String name;
        private boolean unbreakable;
        private int amount;
        private List<String> lore;
        private byte damage;
        private String owner;
        private String url;
        private boolean glowing;

        private Builder() {
        }

        public Builder setDamage(byte damage) {
            this.damage = damage;
            return this;
        }

        public Builder setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder setUnbreakable(boolean bool) {
            this.unbreakable = bool;
            return this;
        }

        public Builder setLore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setGlowing(boolean glowing) {
            this.glowing = glowing;
            return this;
        }

        public Builder setMaterial(Material material) {
            this.material = material;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public SimpleItem build() {
            return new SimpleItem(this);
        }
    }
}
