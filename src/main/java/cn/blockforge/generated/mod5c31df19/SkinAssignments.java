package cn.blockforge.generated.mod5c31df19;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 以存档或服务器地址和村民 UUID 保存外观，不修改世界数据。 */
public final class SkinAssignments {
    public record Choice(String file, boolean slim) { }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private JsonObject worlds = new JsonObject();
    private String scope = "";
    private boolean readFailed;
    public String error = "";

    public SkinAssignments(Path gameDirectory) {
        file = gameDirectory.resolve("config/villager_skins/assignments.json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                worlds = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                readFailed = true;
                error = "设置文件损坏，保存前会自动备份";
                LoggerFactory.getLogger("villager_skins").warn(error, e);
            }
        }
    }

    public void updateScope(MinecraftClient client) {
        if (client.world == null) { scope = ""; return; }
        if (client.getServer() != null) {
            scope = "local:" + client.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize();
        } else if (client.getCurrentServerEntry() != null) {
            scope = "server:" + client.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
        } else {
            scope = "";
        }
    }

    public Choice get(UUID id) {
        if (scope.isEmpty()) return null;
        try {
            JsonObject entries = worlds.getAsJsonObject(scope);
            if (entries == null || !entries.has(id.toString())) return null;
            JsonObject data = entries.getAsJsonObject(id.toString());
            return new Choice(data.get("file").getAsString(), data.get("slim").getAsBoolean());
        } catch (RuntimeException e) { return null; }
    }

    public boolean set(UUID id, Choice choice) {
        if (scope.isEmpty()) { error = "当前世界尚未准备好，请重新打开换肤页面"; return false; }
        JsonObject next = worlds.deepCopy();
        JsonObject entries = next.has(scope) && next.get(scope).isJsonObject() ? next.getAsJsonObject(scope) : new JsonObject();
        next.add(scope, entries);
        if (choice == null) entries.remove(id.toString());
        else entries.add(id.toString(), GSON.toJsonTree(choice));
        try {
            Files.createDirectories(file.getParent());
            if (readFailed && Files.exists(file)) {
                Files.copy(file, file.resolveSibling("assignments-broken-" + System.currentTimeMillis() + ".json"));
                readFailed = false;
            }
            Path temp = file.resolveSibling("assignments.json.tmp");
            Files.writeString(temp, GSON.toJson(next), StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
            worlds = next;
            error = "";
            return true;
        } catch (IOException e) {
            error = "保存失败，请检查 config 文件夹权限";
            LoggerFactory.getLogger("villager_skins").error(error, e);
            return false;
        }
    }
}
