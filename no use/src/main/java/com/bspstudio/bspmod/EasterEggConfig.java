package com.bspstudio.bspmod;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class EasterEggConfig {
    private static final File FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("flightpotion").resolve("easter_egg.json").toFile();
    private static boolean enabled = false;
    private static boolean musicToast = true;
    private static boolean loaded = false;

    public static boolean isEnabled() {
        if (!loaded) load();
        return enabled;
    }

    public static boolean isMusicToastEnabled() {
        if (!loaded) load();
        return musicToast;
    }

    public static void setMusicToastEnabled(boolean e) {
        musicToast = e;
        save();
    }

    public static void setEnabled(boolean e) {
        enabled = e;
        save();
    }

    public static void toggle() {
        setEnabled(!isEnabled());
    }

    private static void load() {
        loaded = true;
        if (!FILE.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
            JsonObject obj = new Gson().fromJson(r, JsonObject.class);
            if (obj != null && obj.has("enabled")) enabled = obj.get("enabled").getAsBoolean();
            if (obj != null && obj.has("musicToast")) musicToast = obj.get("musicToast").getAsBoolean();
        } catch (Exception ignored) {}
    }

    private static void save() {
        FILE.getParentFile().mkdirs();
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled);
        obj.addProperty("musicToast", musicToast);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
            new Gson().toJson(obj, w);
        } catch (Exception ignored) {}
    }
}
