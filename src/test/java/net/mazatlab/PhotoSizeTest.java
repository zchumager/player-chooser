package net.mazatlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhotoSizeTest {

    @TempDir
    Path tempDir;

    private Student photo(String fileName, int width, int height) throws IOException {
        Path file = this.tempDir.resolve(fileName);
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", file.toFile());
        return new Student(fileName, file.toUri().toURL());
    }

    @Test
    void largeLandscapePhotoShrinksToTheBoxWidth() {
        assertEquals(new Dimension(150, 100), PlayerChooser.fitWithin(3000, 2000, 150));
    }

    @Test
    void largePortraitPhotoShrinksToTheBoxHeight() {
        assertEquals(new Dimension(113, 150), PlayerChooser.fitWithin(3000, 4000, 150));
    }

    @Test
    void smallPhotoKeepsItsSize() {
        assertEquals(new Dimension(120, 100), PlayerChooser.fitWithin(120, 100, 150));
    }

    @Test
    void extremelyThinPhotoKeepsAtLeastOnePixel() {
        assertEquals(new Dimension(150, 1), PlayerChooser.fitWithin(10000, 10, 150));
    }

    @Test
    void largePhotoIsLoadedScaledDown() throws IOException {
        Icon icon = PlayerChooser.scaledIcon(photo("big.png", 3000, 2000));

        assertEquals(150, icon.getIconWidth());
        assertEquals(100, icon.getIconHeight());
    }

    @Test
    void smallPhotoIsLoadedAtItsOwnSize() throws IOException {
        Icon icon = PlayerChooser.scaledIcon(photo("small.png", 120, 100));

        assertEquals(120, icon.getIconWidth());
        assertEquals(100, icon.getIconHeight());
    }

    @Test
    void unreadablePhotoIsReportedAsUnreadable() throws IOException {
        Path broken = Files.writeString(this.tempDir.resolve("broken.jpg"), "not an image");

        Icon icon = PlayerChooser.scaledIcon(new Student("broken", broken.toUri().toURL()));

        assertSame(PlayerChooser.UNREADABLE, icon);
    }
}
