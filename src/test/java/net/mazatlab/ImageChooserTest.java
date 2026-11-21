package net.mazatlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageChooserTest {

    @TempDir
    Path tempDir;

    private void createFile(String name) throws IOException {
        Files.createFile(this.tempDir.resolve(name));
    }

    @Test
    void bundledImagesLoadFromClasspath() {
        ImageChooser chooser = new ImageChooser();

        assertEquals(7, chooser.getImages().size());
        assertNotNull(chooser.getDefaultImage());
    }

    @Test
    void externalFolderImagesAreLoaded() throws IOException {
        createFile("a.png");
        createFile("b.jpg");

        List<URL> images = new ImageChooser(this.tempDir.toString()).getImages();

        assertEquals(2, images.size());
    }

    @Test
    void onlyImageFilesAreLoaded() throws IOException {
        createFile("a.png");
        createFile("b.JPG");
        createFile("c.jpeg");
        createFile("d.gif");
        createFile(".DS_Store");
        createFile("notes.txt");
        createFile("png");
        createFile("._e.png"); // macOS metadata file on external drives

        List<URL> images = new ImageChooser(this.tempDir.toString()).getImages();

        assertEquals(4, images.size());
    }

    @Test
    void invalidFolderPathReturnsNoImages() {
        ImageChooser chooser = new ImageChooser("bad\u0000path");

        assertTrue(chooser.getImages().isEmpty());
        assertNull(chooser.getDefaultImage());
    }

    @Test
    void missingFolderReturnsNoImages() {
        ImageChooser chooser = new ImageChooser(this.tempDir.resolve("missing").toString());

        assertTrue(chooser.getImages().isEmpty());
        assertNull(chooser.getDefaultImage());
    }
}
