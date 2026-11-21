package net.mazatlab;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import junit.framework.TestCase;

public class ImageChooserTest extends TestCase {

    private Path tempDir;

    @Override
    protected void setUp() throws IOException {
        this.tempDir = Files.createTempDirectory("player-chooser-test");
    }

    @Override
    protected void tearDown() throws IOException {
        File[] files = this.tempDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        Files.deleteIfExists(this.tempDir);
    }

    private void createFile(String name) throws IOException {
        Files.createFile(this.tempDir.resolve(name));
    }

    public void testBundledImagesLoadFromClasspath() {
        ImageChooser chooser = new ImageChooser();

        assertEquals(7, chooser.getImages().size());
        assertNotNull(chooser.getDefaultImage());
    }

    public void testExternalFolderImagesAreLoaded() throws IOException {
        createFile("a.png");
        createFile("b.jpg");

        List<URL> images = new ImageChooser(this.tempDir.toString()).getImages();

        assertEquals(2, images.size());
    }

    public void testMissingFolderReturnsNoImages() {
        ImageChooser chooser = new ImageChooser(this.tempDir.resolve("missing").toString());

        assertTrue(chooser.getImages().isEmpty());
        assertNull(chooser.getDefaultImage());
    }
}
