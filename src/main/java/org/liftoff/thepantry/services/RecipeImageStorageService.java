package org.liftoff.thepantry.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RecipeImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "jfif", "png", "gif");

    private final Path storagePath;

    public RecipeImageStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.storagePath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image to upload.");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalName);
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension) || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Please upload a JPG, PNG, or GIF image.");
        }

        try (InputStream input = file.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new IllegalArgumentException("The selected file is not a valid image.");
            }
        }

        Files.createDirectories(storagePath);
        String storedName = UUID.randomUUID() + "." + extension;
        Path destination = storagePath.resolve(storedName).normalize();
        if (!destination.startsWith(storagePath)) {
            throw new IllegalArgumentException("Invalid image path.");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    public void delete(String fileName) throws IOException {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        Path destination = storagePath.resolve(fileName).normalize();
        if (!destination.startsWith(storagePath)) {
            throw new IllegalArgumentException("Invalid image path.");
        }
        Files.deleteIfExists(destination);
    }
}
