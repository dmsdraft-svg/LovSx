package com.example.marriagegender;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

public final class DataManager {

    private final MarriageGenderPlugin plugin;
    private File dataFile;
    private YamlConfiguration config;

    public DataManager(MarriageGenderPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            try {
                File parent = dataFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                dataFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Failed to create data.yml");
                exception.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    public Gender getGender(UUID uuid) {
        String raw = config.getString(uuid.toString() + ".gender", Gender.NONE.name());

        try {
            return Gender.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Gender.NONE;
        }
    }

    public void setGender(UUID uuid, Gender gender) {
        config.set(uuid.toString() + ".gender", gender.name());
        save();
    }

    public UUID getSpouse(UUID uuid) {
        String raw = config.getString(uuid.toString() + ".spouse");

        if (raw == null || raw.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public boolean isMarried(UUID uuid) {
        return getSpouse(uuid) != null;
    }

    public void marry(UUID first, UUID second) {
        config.set(first.toString() + ".spouse", second.toString());
        config.set(second.toString() + ".spouse", first.toString());
        save();
    }

    public void divorce(UUID uuid) {
        UUID spouse = getSpouse(uuid);

        config.set(uuid.toString() + ".spouse", null);

        if (spouse != null) {
            config.set(spouse.toString() + ".spouse", null);
        }

        save();
    }

    public void save() {
        if (config == null || dataFile == null) {
            return;
        }

        try {
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save data.yml");
            exception.printStackTrace();
        }
    }
}
