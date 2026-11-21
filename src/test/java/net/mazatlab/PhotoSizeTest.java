package net.mazatlab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;

import org.junit.jupiter.api.Test;

class PhotoSizeTest {

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
}
