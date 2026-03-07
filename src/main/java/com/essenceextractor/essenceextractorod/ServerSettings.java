package com.essenceextractor.essenceextractormod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.neoforged.fml.loading.FMLPaths;

public final class ServerSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "essenceextractor-server.json";
    private static final int DEFAULT_MIN_CAPTURE_TICKS = 5;
    private static final int DEFAULT_MIN_PROCESS_PERCENT = 1;
    private static final int DEFAULT_MOB_PROCESS_INTERVAL_TICKS = 20;
    private static final String NOTES = "Capture tick in machine cannot go below minCaptureTicks. Process % cannot go below minProcessPercent. Captured mob processing runs every mobProcessIntervalTicks.";

    private static int minCaptureTicks = DEFAULT_MIN_CAPTURE_TICKS;
    private static int minProcessPercent = DEFAULT_MIN_PROCESS_PERCENT;
    private static int mobProcessIntervalTicks = DEFAULT_MOB_PROCESS_INTERVAL_TICKS;

    private ServerSettings() {
    }

    public static int getMinCaptureTicks() {
        return minCaptureTicks;
    }

    public static int getMinProcessPercent() {
        return minProcessPercent;
    }

    public static int getMobProcessIntervalTicks() {
        return mobProcessIntervalTicks;
    }

    public static void load() {
        // Load once during server startup; keep resilient defaults if file is malformed.
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try {
            if (!Files.exists(path)) {
                writeDefault(path);
            }

            String json = Files.readString(path);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                root = new JsonObject();
            }

            boolean changed = false;
            IntValue minCapture = readIntOrDefault(root, "minCaptureTicks", DEFAULT_MIN_CAPTURE_TICKS, 0, 200);
            IntValue minProcess = readIntOrDefault(root, "minProcessPercent", DEFAULT_MIN_PROCESS_PERCENT, 1, 100);
            IntValue processInterval = readIntOrDefault(root, "mobProcessIntervalTicks", DEFAULT_MOB_PROCESS_INTERVAL_TICKS, 1, 1200);

            minCaptureTicks = minCapture.value();
            minProcessPercent = minProcess.value();
            mobProcessIntervalTicks = processInterval.value();

            changed = minCapture.changed() || minProcess.changed() || processInterval.changed();
            if (!root.has("notes") || !root.get("notes").isJsonPrimitive()) {
                root.addProperty("notes", NOTES);
                changed = true;
            }

            if (changed) {
                Files.writeString(path, GSON.toJson(root));
            }
        } catch (Exception e) {
            EssenceExtractor.LOGGER.error("Failed to load {}, using defaults", FILE_NAME, e);
            minCaptureTicks = DEFAULT_MIN_CAPTURE_TICKS;
            minProcessPercent = DEFAULT_MIN_PROCESS_PERCENT;
            mobProcessIntervalTicks = DEFAULT_MOB_PROCESS_INTERVAL_TICKS;
        }
    }

    private static IntValue readIntOrDefault(JsonObject root, String key, int defaultValue, int min, int max) {
        if (!root.has(key) || !root.get(key).isJsonPrimitive() || !root.get(key).getAsJsonPrimitive().isNumber()) {
            root.addProperty(key, defaultValue);
            return new IntValue(defaultValue, true);
        }

        int value = root.get(key).getAsInt();
        if (value < min || value > max) {
            root.addProperty(key, defaultValue);
            return new IntValue(defaultValue, true);
        }
        return new IntValue(value, false);
    }

    private static void writeDefault(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("minCaptureTicks", DEFAULT_MIN_CAPTURE_TICKS);
        root.addProperty("minProcessPercent", DEFAULT_MIN_PROCESS_PERCENT);
        root.addProperty("mobProcessIntervalTicks", DEFAULT_MOB_PROCESS_INTERVAL_TICKS);
        root.addProperty("notes", NOTES);
        Files.writeString(path, GSON.toJson(root));
    }

    private record IntValue(int value, boolean changed) {
    }
}
