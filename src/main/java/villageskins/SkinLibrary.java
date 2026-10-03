package villageskins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkinLibrary {
    private static final Logger LOG = LoggerFactory.getLogger((String)"villager_skins");
    private final Path directory;
    private final Map<String, Skin> skins;
    private Map<String, Stamp> stamps;
    private final List<String> errors;
    private String directoryError;
    private int serial;

    public SkinLibrary(Path gameDirectory) {
        block10: {
            this.skins = new LinkedHashMap<String, Skin>();
            this.stamps = new TreeMap<String, Stamp>();
            this.errors = new ArrayList<String>();
            this.directoryError = "";
            this.directory = gameDirectory.resolve("villager_skins");
            try {
                Files.createDirectories(this.directory, new FileAttribute[0]);
                Path marker = this.directory.resolve(".initialized");
                if (Files.exists(marker, new LinkOption[0])) break block10;
                Path target = this.directory.resolve("\u732b\u7fbd.png");
                if (!Files.exists(target, new LinkOption[0])) {
                    try (InputStream in = SkinLibrary.class.getResourceAsStream("/assets/villageskins/textures/entity/maoyu.png");){
                        if (in == null) {
                            throw new IOException("\u9ed8\u8ba4\u76ae\u80a4\u7f3a\u5931");
                        }
                        Files.copy(in, target, new CopyOption[0]);
                    }
                }
                Files.writeString(marker, (CharSequence)"\u9ed8\u8ba4\u76ae\u80a4\u5df2\u5bfc\u5165\uff1b\u5220\u9664 PNG \u540e\u4e0d\u4f1a\u91cd\u65b0\u751f\u6210\u3002\n", new OpenOption[0]);
            }
            catch (IOException e) {
                this.directoryError = "\u65e0\u6cd5\u51c6\u5907\u76ae\u80a4\u76ee\u5f55\uff0c\u8bf7\u68c0\u67e5\u6587\u4ef6\u6743\u9650";
                LOG.error(this.directoryError, (Throwable)e);
            }
        }
    }

    public Path directory() {
        return this.directory;
    }

    public List<Skin> all() {
        return List.copyOf(this.skins.values());
    }

    public Skin get(String filename) {
        return this.skins.get(filename);
    }

    public String status() {
        if (!this.directoryError.isEmpty()) {
            return this.directoryError;
        }
        return this.errors.isEmpty() ? "\u5df2\u8bfb\u53d6 " + this.skins.size() + " \u5f20\u76ae\u80a4" : "\u8df3\u8fc7 " + this.errors.size() + " \u4e2a\u65e0\u6548\u6587\u4ef6\uff1a" + this.errors.getFirst();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void refresh(boolean force) {
        TreeMap<String, Stamp> current = new TreeMap<String, Stamp>();
        try (Stream<Path> files = Files.list(this.directory);){
            for (Path path : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).toList()) {
                current.put(path.getFileName().toString(), new Stamp(Files.size(path), Files.getLastModifiedTime(path, new LinkOption[0]).toMillis()));
            }
            this.directoryError = "";
        }
        catch (IOException e) {
            this.directoryError = "\u8bfb\u53d6\u76ae\u80a4\u6587\u4ef6\u5939\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u6587\u4ef6\u6743\u9650";
            return;
        }
        if (!force && current.equals(this.stamps)) {
            return;
        }
        this.errors.clear();
        LinkedHashMap<String, Skin> next = new LinkedHashMap<String, Skin>();
        for (Map.Entry entry : current.entrySet()) {
            String file = (String)entry.getKey();
            if (!force && ((Stamp)entry.getValue()).equals(this.stamps.get(file)) && this.skins.containsKey(file)) {
                next.put(file, this.skins.get(file));
                continue;
            }
            try {
                NativeImage image;
                if (((Stamp)entry.getValue()).size() > 0x200000L) {
                    throw new IOException("\u6587\u4ef6\u8d85\u8fc7 2MB");
                }
                Path path = this.directory.resolve(file);
                try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile());){
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) {
                        throw new IOException("\u4e0d\u662f\u6709\u6548 PNG");
                    }
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(input);
                        if (!"png".equalsIgnoreCase(reader.getFormatName()) || reader.getWidth(0) != 64 || reader.getHeight(0) != 64) {
                            throw new IOException("\u8bf7\u4f7f\u7528 64\u00d764 PNG \u73a9\u5bb6\u76ae\u80a4");
                        }
                    }
                    finally {
                        reader.dispose();
                    }
                }
                try (InputStream input = Files.newInputStream(path, new OpenOption[0]);){
                    image = NativeImage.read((InputStream)input);
                }
                SkinLibrary.opaque(image, 0, 0, 32, 16);
                SkinLibrary.opaque(image, 0, 16, 64, 32);
                SkinLibrary.opaque(image, 16, 48, 48, 64);
                Identifier id = Identifier.of((String)"villageskins", (String)("external/skin_" + this.serial++));
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "\u6751\u6c11\u76ae\u80a4\uff1a" + file, image);
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, (AbstractTexture)texture);
                next.put(file, new Skin(file, id));
            }
            catch (Exception e) {
                this.errors.add(file);
                LOG.warn("\u65e0\u6cd5\u52a0\u8f7d\u76ae\u80a4 {}\uff1a{}", (Object)file, (Object)e.getMessage());
            }
        }
        for (Map.Entry entry : this.skins.entrySet()) {
            if (next.get(entry.getKey()) == entry.getValue()) continue;
            MinecraftClient.getInstance().getTextureManager().destroyTexture(((Skin)entry.getValue()).texture());
        }
        this.skins.clear();
        this.skins.putAll(next);
        this.stamps = current;
    }

    private static void opaque(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; ++y) {
            for (int x = x1; x < x2; ++x) {
                image.setColorArgb(x, y, image.getColorArgb(x, y) | 0xFF000000);
            }
        }
    }

    public record Skin(String file, Identifier texture) {
    }

    private record Stamp(long size, long modified) {
    }
}

