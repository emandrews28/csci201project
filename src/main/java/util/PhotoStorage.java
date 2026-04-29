package util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

public final class PhotoStorage {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/heic",
            "image/heif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "heic", "heif"
    );

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private PhotoStorage() {}

    public static Path baseDir() {
        String dir = System.getenv("PHOTO_UPLOAD_DIR");
        if (dir == null || dir.isBlank()) {
            dir = System.getProperty("user.home") + File.separator + "csci201_photos";
        }
        return Paths.get(dir);
    }

    public static boolean isAllowedContentType(String contentType) {
        if (contentType == null) return false;
        return ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    }

    public static boolean isAllowedExtension(String filename) {
        String ext = getExtension(filename);
        return ext != null && ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    public static String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    public static String extensionForContentType(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/heic", "image/heif" -> "heic";
            default -> "jpg";
        };
    }

    /**
     * Persist the stream to {baseDir}/{userId}/{restaurantId}/{filename}.
     * Returns the relative path used for the image URL.
     */
    public static String saveFile(long userId, long restaurantId, String filename, InputStream in)
            throws IOException {
        Path dir = baseDir().resolve(String.valueOf(userId)).resolve(String.valueOf(restaurantId));
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return userId + "/" + restaurantId + "/" + filename;
    }

    /**
     * Delete the physical file that backs a stored relative path.
     * Silent if the file is missing (already gone).
     */
    public static boolean deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return false;
        try {
            Path target = baseDir().resolve(relativePath).normalize();
            if (!target.startsWith(baseDir())) return false;
            return Files.deleteIfExists(target);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Resolve a relative path to the on-disk location, rejecting any path that
     * tries to escape the upload root.
     */
    public static Path resolveSafe(String relativePath) {
        Path base = baseDir().toAbsolutePath().normalize();
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) return null;
        return target;
    }
}
