package villageskins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;
import org.slf4j.LoggerFactory;

public final class SkinAssignments {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private JsonObject worlds = new JsonObject();
    private String scope = "";
    private boolean readFailed;
    public String error = "";

    public SkinAssignments(Path gameDirectory) {
        this.file = gameDirectory.resolve("config/villager_skins/assignments.json");
        if (Files.exists(this.file, new LinkOption[0])) {
            try (BufferedReader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8);){
                this.worlds = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            }
            catch (Exception e) {
                this.readFailed = true;
                this.error = "\u8bbe\u7f6e\u6587\u4ef6\u635f\u574f\uff0c\u4fdd\u5b58\u524d\u4f1a\u81ea\u52a8\u5907\u4efd";
                LoggerFactory.getLogger((String)"villager_skins").warn(this.error, (Throwable)e);
            }
        }
    }

    public void updateScope(MinecraftClient client) {
        if (client.world == null) {
            this.scope = "";
            return;
        }
        this.scope = client.getServer() != null ? "local:" + String.valueOf(client.getServer().getSavePath(WorldSavePath.ROOT).toAbsolutePath().normalize()) : (client.getCurrentServerEntry() != null ? "server:" + client.getCurrentServerEntry().address.toLowerCase(Locale.ROOT) : "");
    }

    public Choice get(UUID id) {
        if (this.scope.isEmpty()) {
            return null;
        }
        try {
            JsonObject entries = this.worlds.getAsJsonObject(this.scope);
            if (entries == null || !entries.has(id.toString())) {
                return null;
            }
            JsonObject data = entries.getAsJsonObject(id.toString());
            return new Choice(data.get("file").getAsString(), data.get("slim").getAsBoolean());
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    public boolean set(UUID id, Choice choice) {
        if (this.scope.isEmpty()) {
            this.error = "\u5f53\u524d\u4e16\u754c\u5c1a\u672a\u51c6\u5907\u597d\uff0c\u8bf7\u91cd\u65b0\u6253\u5f00\u6362\u80a4\u9875\u9762";
            return false;
        }
        JsonObject next = this.worlds.deepCopy();
        JsonObject entries = next.has(this.scope) && next.get(this.scope).isJsonObject() ? next.getAsJsonObject(this.scope) : new JsonObject();
        next.add(this.scope, (JsonElement)entries);
        if (choice == null) {
            entries.remove(id.toString());
        } else {
            entries.add(id.toString(), GSON.toJsonTree((Object)choice));
        }
        try {
            Files.createDirectories(this.file.getParent(), new FileAttribute[0]);
            if (this.readFailed && Files.exists(this.file, new LinkOption[0])) {
                Files.copy(this.file, this.file.resolveSibling("assignments-broken-" + System.currentTimeMillis() + ".json"), new CopyOption[0]);
                this.readFailed = false;
            }
            Path temp = this.file.resolveSibling("assignments.json.tmp");
            Files.writeString(temp, (CharSequence)GSON.toJson((JsonElement)next), StandardCharsets.UTF_8, new OpenOption[0]);
            try {
                Files.move(temp, this.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            this.worlds = next;
            this.error = "";
            return true;
        }
        catch (IOException e) {
            this.error = "\u4fdd\u5b58\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5 config \u6587\u4ef6\u5939\u6743\u9650";
            LoggerFactory.getLogger((String)"villager_skins").error(this.error, (Throwable)e);
            return false;
        }
    }

    public record Choice(String file, boolean slim) {
    }
}

