package com.bspstudio.bspmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.List;

public class KeyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File KEY_DIR = FabricLoader.getInstance().getGameDir().resolve("flightpotion").toFile();
    private static final File KEY_FILE = new File(KEY_DIR, "activated.key");

    /** 当前有效的兑换码列表 */
    private static final List<String> VALID_CODES = List.of(
        "a000000000", "a000000001", "a000000002", "a000000003", "a000000004",
        "a000000005", "a000000006", "a000000007", "a000000008", "a000000009",
        "a00000000a", "a00000000b", "a00000000c", "a00000000d", "a00000000e",
        "a00000000f", "f000000000", "f000000001", "f000000002"
    );

    private static Boolean cachedValid = null;

    public static boolean isValidCode(String code) {
        return code != null && VALID_CODES.contains(code.trim());
    }

    public static boolean isActivated() {
        if (cachedValid != null) return cachedValid;

        if (!KEY_FILE.exists()) {
            cachedValid = false;
            return false;
        }

        try (Reader r = new InputStreamReader(new FileInputStream(KEY_FILE), StandardCharsets.UTF_8)) {
            KeyData data = GSON.fromJson(r, KeyData.class);
            if (data == null || data.codeHash == null || data.version == null || data.hwid == null) {
                cachedValid = false;
                return false;
            }

            // Check version: first two segments must match
            String currentVer = FabricLoader.getInstance().getModContainer("bsp-flight-potion")
                    .map(m -> m.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.1.4");
            if (!versionMatches(data.version, currentVer)) {
                KEY_FILE.delete();
                cachedValid = false;
                return false;
            }

            // Check HWID
            if (!data.hwid.equals(getHwid())) {
                KEY_FILE.delete();
                cachedValid = false;
                return false;
            }

            cachedValid = true;
            return true;
        } catch (Exception e) {
            cachedValid = false;
            return false;
        }
    }

    public static boolean activate(String code) {
        // Validate code against the allowed list
        if (!isValidCode(code)) return false;

        try {
            KEY_DIR.mkdirs();
            KeyData data = new KeyData();
            data.codeHash = sha256(code.trim());
            data.version = FabricLoader.getInstance().getModContainer("bsp-flight-potion")
                    .map(m -> m.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.1.4");
            data.hwid = getHwid();

            try (Writer w = new OutputStreamWriter(new FileOutputStream(KEY_FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(data, w);
            }

            cachedValid = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean versionMatches(String stored, String current) {
        try {
            String[] s = stored.split("\\.");
            String[] c = current.split("\\.");
            if (s.length < 2 || c.length < 2) return false;
            return s[0].equals(c[0]) && s[1].equals(c[1]);
        } catch (Exception e) {
            return false;
        }
    }

    private static String getHwid() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("user.name"));
            // Add hardware identifier from MAC address
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis != null) {
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    if (ni.isLoopback() || ni.isVirtual()) continue;
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        for (byte b : mac) sb.append(String.format("%02X", b));
                        break; // first non-loopback physical interface
                    }
                }
            }
            sb.append(System.getProperty("os.name"));
            return sha256(sb.toString());
        } catch (Exception e) {
            // Fallback
            return sha256(System.getProperty("user.name") + System.getProperty("os.name"));
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return input;
        }
    }

    private static class KeyData {
        String codeHash;
        String version;
        String hwid;
    }
}
