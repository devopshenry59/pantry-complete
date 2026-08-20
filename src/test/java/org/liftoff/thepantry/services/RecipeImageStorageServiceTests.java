package org.liftoff.thepantry.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeImageStorageServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void storeUsesGeneratedNameAndPersistsImage() throws Exception {
        RecipeImageStorageService storage = new RecipeImageStorageService(tempDir.toString());
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", imageBytes);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dinner.png",
                "image/png",
                imageBytes.toByteArray()
        );

        String storedName = storage.store(file);

        assertNotEquals("dinner.png", storedName);
        assertTrue(storedName.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(storedName)));

        storage.delete(storedName);
        assertFalse(Files.exists(tempDir.resolve(storedName)));
    }

    @Test
    void storeRejectsNonImageFiles() {
        RecipeImageStorageService storage = new RecipeImageStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not an image".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> storage.store(file));
    }
}
