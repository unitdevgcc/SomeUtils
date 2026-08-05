package dev.c0redev.someutils.lang;

import dev.c0redev.someutils.SomeUtilsPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import dev.c0redev.someutils.jade.TargetInfo;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LanguageService {
    private static final String[] HUD_KEYS = {
        "hud.health", "hud.growth", "hud.power", "hud.powered", "hud.unpowered",
        "hud.honey", "hud.smelting", "hud.fuel", "hud.items", "hud.potions",
        "hud.speed", "hud.jump", "hud.baby",
    };
    private static final String[] HUD_EN = {
        "Health:", "Growth:", "Power:", "Powered", "Unpowered",
        "Honey:", "Smelting:", "Fuel:", "Items:", "Potions:",
        "Speed:", "Jump:", "Baby",
    };

    private final SomeUtilsPlugin plugin;
    private final File file;
    private final Map<UUID, Language> preferences = new ConcurrentHashMap<>();
    private final Properties russianVanilla = new Properties();

    private final Map<String, String> langRu = new ConcurrentHashMap<>();
    private final Map<String, String> langEn = new ConcurrentHashMap<>();

    public LanguageService(SomeUtilsPlugin plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "languages.yml");
        loadVanillaRussian(plugin);
        loadLangFile(plugin);
        load();
    }

    public Language getPreference(Player player) {
        return preferences.getOrDefault(player.getUniqueId(), Language.AUTO);
    }

    public Language getEffective(Player player) {
        Language selected = getPreference(player);
        if (selected != Language.AUTO) return selected;
        Locale locale = player.locale();
        return locale != null && locale.getLanguage().equalsIgnoreCase("ru") ? Language.RU : Language.EN;
    }

    public void set(Player player, Language language) {
        preferences.put(player.getUniqueId(), language);
        save();
    }

    public void reload() {
        preferences.clear();
        russianVanilla.clear();
        langRu.clear();
        langEn.clear();
        loadVanillaRussian(plugin);
        loadLangFile(plugin);
        load();
    }

    public String tr(Player player, String key) {
        return tr(getEffective(player), key);
    }

    public String tr(Language language, String key) {
        Map<String, String> source = language == Language.RU ? langRu : langEn;
        return source.getOrDefault(key, key);
    }

    public String localizeHud(Player player, String text) {
        if (getEffective(player) != Language.RU) return text;
        String result = text;
        for (int i = 0; i < HUD_KEYS.length; i++) {
            result = result.replace(HUD_EN[i], tr(Language.RU, HUD_KEYS[i]));
        }
        return result;
    }

    public String localizeTargetTitle(Player player, TargetInfo target) {
        if (getEffective(player) != Language.RU || target.getType() != TargetInfo.Type.BLOCK) {
            return target.getTitle();
        }
        return russianVanilla.getProperty("block.minecraft." + target.getMaterial().getKey().getKey(), target.getTitle());
    }

    private void loadVanillaRussian(SomeUtilsPlugin plugin) {
        try (var input = plugin.getResource("waila-ru.properties")) {
            if (input != null) {
                russianVanilla.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
            russianVanilla.clear();
        }
    }

    private void loadLangFile(SomeUtilsPlugin plugin) {
        try (var input = plugin.getResource("lang.yml")) {
            if (input == null) {
                return;
            }
            Object loaded = new Yaml().load(new InputStreamReader(input, StandardCharsets.UTF_8));
            if (!(loaded instanceof Map<?, ?> root)) {
                return;
            }
            Object ru = root.get("ru");
            if (ru instanceof Map<?, ?> ruMap) {
                ruMap.forEach((k, v) -> langRu.put(String.valueOf(k), String.valueOf(v)));
            }
            Object en = root.get("en");
            if (en instanceof Map<?, ?> enMap) {
                enMap.forEach((k, v) -> langEn.put(String.valueOf(k), String.valueOf(v)));
            }
        } catch (Exception ignored) {
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String id : config.getKeys(false)) {
            try {
                preferences.put(UUID.fromString(id), Language.from(config.getString(id)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        preferences.forEach((id, language) -> config.set(id.toString(), language.name()));
        try {
            config.save(file);
        } catch (IOException ignored) {
        }
    }
}
