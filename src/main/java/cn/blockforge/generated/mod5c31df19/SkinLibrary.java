package cn.blockforge.generated.mod5c31df19;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

/** 只在客户端主线程刷新，释放被替换或删除的纹理。 */
public final class SkinLibrary {
    private static final Logger LOG = LoggerFactory.getLogger("villager_skins");
    public record Skin(String file, Identifier texture) { }
    private record Stamp(long size, long modified) { }
    private final Path directory;
    private final Map<String, Skin> skins = new LinkedHashMap<>();
    private Map<String, Stamp> stamps = new TreeMap<>();
    private final List<String> errors = new ArrayList<>();
    private String directoryError = "";
    private int serial;

    public SkinLibrary(Path gameDirectory) {
        directory = gameDirectory.resolve("villager_skins");
        try {
            Files.createDirectories(directory);
            Path marker = directory.resolve(".initialized");
            if (!Files.exists(marker)) {
                Path target = directory.resolve("猫羽.png");
                if (!Files.exists(target)) {
                    try (InputStream in = SkinLibrary.class.getResourceAsStream("/assets/mod_5c31df19/textures/entity/maoyu.png")) {
                        if (in == null) throw new IOException("默认皮肤缺失");
                        Files.copy(in, target);
                    }
                }
                Files.writeString(marker, "默认皮肤已导入；删除 PNG 后不会重新生成。\n");
            }
        } catch (IOException e) {
            directoryError = "无法准备皮肤目录，请检查文件权限";
            LOG.error(directoryError, e);
        }
    }

    public Path directory() { return directory; }
    public List<Skin> all() { return List.copyOf(skins.values()); }
    public Skin get(String filename) { return skins.get(filename); }
    public String status() {
        if (!directoryError.isEmpty()) return directoryError;
        return errors.isEmpty() ? "已读取 " + skins.size() + " 张皮肤" : "跳过 " + errors.size() + " 个无效文件：" + errors.getFirst();
    }

    public void refresh(boolean force) {
        Map<String, Stamp> current = new TreeMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).toList()) {
                current.put(file.getFileName().toString(), new Stamp(Files.size(file), Files.getLastModifiedTime(file).toMillis()));
            }
            directoryError = "";
        } catch (IOException e) {
            directoryError = "读取皮肤文件夹失败，请检查文件权限";
            return;
        }
        if (!force && current.equals(stamps)) return;
        errors.clear();
        Map<String, Skin> next = new LinkedHashMap<>();
        for (var entry : current.entrySet()) {
            String file = entry.getKey();
            if (!force && entry.getValue().equals(stamps.get(file)) && skins.containsKey(file)) {
                next.put(file, skins.get(file));
                continue;
            }
            try {
                if (entry.getValue().size() > 2 * 1024 * 1024) throw new IOException("文件超过 2MB");
                Path path = directory.resolve(file);
                // 先读取图片头，避免把异常尺寸的图片解码到显存。
                try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
                    var readers = ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) throw new IOException("不是有效 PNG");
                    var reader = readers.next();
                    try {
                        reader.setInput(input);
                        if (!"png".equalsIgnoreCase(reader.getFormatName()) || reader.getWidth(0) != 64 || reader.getHeight(0) != 64)
                            throw new IOException("请使用 64×64 PNG 玩家皮肤");
                    } finally { reader.dispose(); }
                }
                NativeImage image;
                try (InputStream input = Files.newInputStream(path)) { image = NativeImage.read(input); }
                // 与原版玩家皮肤一样保证身体底层不透明，保留外套层透明度。
                opaque(image, 0, 0, 32, 16);
                opaque(image, 0, 16, 64, 32);
                opaque(image, 16, 48, 48, 64);
                Identifier id = Identifier.of(GeneratedMod.MOD_ID, "external/skin_" + serial++);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "村民皮肤：" + file, image);
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                next.put(file, new Skin(file, id));
            } catch (Exception e) {
                errors.add(file);
                LOG.warn("无法加载皮肤 {}：{}", file, e.getMessage());
            }
        }
        for (var old : skins.entrySet()) {
            if (next.get(old.getKey()) != old.getValue())
                MinecraftClient.getInstance().getTextureManager().destroyTexture(old.getValue().texture());
        }
        skins.clear();
        skins.putAll(next);
        stamps = current;
    }

    private static void opaque(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) for (int x = x1; x < x2; x++)
            image.setColorArgb(x, y, image.getColorArgb(x, y) | 0xff000000);
    }
}
