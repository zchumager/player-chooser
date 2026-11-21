package net.mazatlab;

import static net.mazatlab.UiTestSupport.assumeDisplay;
import static net.mazatlab.UiTestSupport.classFolder;
import static net.mazatlab.UiTestSupport.clickButton;
import static net.mazatlab.UiTestSupport.onEdt;
import static net.mazatlab.UiTestSupport.pickOne;
import static net.mazatlab.UiTestSupport.pressPhoto;
import static net.mazatlab.UiTestSupport.waitUntil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives the real window (never shown on screen) the way a teacher would. Skipped without a
 * display. The modal dialogs are covered separately by {@link PlayerChooserDialogsTest}.
 */
class PlayerChooserUiTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void needsDisplay() {
        assumeDisplay();
    }

    private static List<String> pickEveryone(PlayerChooser window) throws Exception {
        List<String> picked = new ArrayList<>();
        while (onEdt(() -> window.btnPick.isEnabled()) && picked.size() < 100) {
            picked.add(pickOne(window));
        }
        return picked;
    }

    @Test
    void aFullSessionPicksEveryStudentExactlyOnce() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        assertEquals("7 of 7 students left", onEdt(() -> window.lblRemaining.getText()));

        List<String> picked = pickEveryone(window);

        assertEquals(7, picked.size());
        assertEquals(7, new HashSet<>(picked).size(), "nobody is picked twice: " + picked);
        assertEquals("Everyone has participated (7 students)", onEdt(() -> window.lblRemaining.getText()));
        assertFalse(onEdt(() -> window.btnPick.isEnabled()));
    }

    @Test
    void pressingThePhotoStopsAndPicksTheStudentShown() throws Exception {
        Path folder = classFolder(this.tempDir, "class-3B", "ana", "maria_lopez", "luis");
        PlayerChooser window = UiTestSupport.window(new ImageChooser(folder.toString()));

        List<String> picked = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            clickButton(window);
            Thread.sleep(150);
            pressPhoto(window, 0);
            picked.add(onEdt(() -> window.lblName.getText()));
        }

        assertEquals(Set.of("ana", "maria lopez", "luis"), new HashSet<>(picked));
        assertEquals("Everyone has participated (3 students)", onEdt(() -> window.lblRemaining.getText()));
    }

    @Test
    void aPressThatDriftsAFewPixelsStillStops() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        clickButton(window);
        Thread.sleep(150);

        pressPhoto(window, 4); // Swing sends no "clicked" event for this

        assertEquals("Pick a student", onEdt(() -> window.btnPick.getText()));
        assertEquals("6 of 7 students left", onEdt(() -> window.lblRemaining.getText()));
    }

    @Test
    void theButtonSaysStopWhileCyclingAndNothingKeepsCyclingAfterwards() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());

        clickButton(window);
        assertEquals("Stop", onEdt(() -> window.btnPick.getText()));
        assertTrue(onEdt(() -> window.btnPick.isEnabled()));
        Thread.sleep(150);
        clickButton(window);

        Icon shown = onEdt(() -> window.lblPlayer.getIcon());
        String text = onEdt(() -> window.lblPlayer.getText());
        Thread.sleep(350); // several timer periods
        assertSame(shown, onEdt(() -> window.lblPlayer.getIcon()), "the photo changed after stopping");
        assertEquals(text, onEdt(() -> window.lblPlayer.getText()));
    }

    @Test
    void pressingThePhotoBeforeAnyPickDoesNothing() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());

        pressPhoto(window, 0);

        assertEquals("7 of 7 students left", onEdt(() -> window.lblRemaining.getText()));
        assertEquals("Pick a student", onEdt(() -> window.btnPick.getText()));
        assertTrue(onEdt(() -> window.btnPick.isEnabled()));
    }

    @Test
    void anEmptyFolderDisablesPickingAndNamesTheFolder() throws Exception {
        Path empty = Files.createDirectories(this.tempDir.resolve("empty"));
        PlayerChooser window = UiTestSupport.window(new ImageChooser(empty.toString()));

        assertEquals("No photos found in “empty”", onEdt(() -> window.lblRemaining.getText()));
        assertFalse(onEdt(() -> window.btnPick.isEnabled()));

        onEdt(window::raffle); // even if called directly, nothing starts
        Thread.sleep(250);
        assertEquals("Pick a student", onEdt(() -> window.btnPick.getText()));
    }

    @Test
    void loadingAnotherSetStartsANewSessionButAnEmptyFolderKeepsTheCurrentOne() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        assertEquals("Player Chooser — Demo images", onEdt(() -> window.frame.getTitle()));
        pickOne(window);
        pickOne(window);
        assertEquals("5 of 7 students left", onEdt(() -> window.lblRemaining.getText()));

        Path folder = classFolder(this.tempDir, "class-3B", "ana", "luis");
        assertTrue(onEdt(() -> window.load(new ImageChooser(folder.toString()))));
        assertEquals("Player Chooser — class-3B", onEdt(() -> window.frame.getTitle()));
        assertEquals("2 of 2 students left", onEdt(() -> window.lblRemaining.getText()));

        Path empty = Files.createDirectories(this.tempDir.resolve("empty"));
        assertFalse(onEdt(() -> window.load(new ImageChooser(empty.toString()))));
        assertEquals("Player Chooser — class-3B", onEdt(() -> window.frame.getTitle()));
        assertEquals("2 of 2 students left", onEdt(() -> window.lblRemaining.getText()));
    }

    /* Phone-sized (12 MP) photos with noise, which decode about as slowly as real ones; smaller or
     * solid-color photos decode too fast to reveal a freeze */
    private Path largePhotos(String folderName, int count) throws IOException {
        Path folder = Files.createDirectories(this.tempDir.resolve(folderName));
        Random random = new Random(1);
        for (int i = 1; i <= count; i++) {
            BufferedImage image = new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 3000; y++) {
                for (int x = 0; x < 4000; x++) {
                    image.setRGB(x, y, random.nextInt(0xFFFFFF));
                }
            }
            ImageIO.write(image, "jpg", folder.resolve("student_" + i + ".jpg").toFile());
        }
        return folder;
    }

    @Test
    void theWindowDoesNotFreezeWhileLargePhotosLoad() throws Exception {
        Path folder = largePhotos("phone-photos", 6);
        PlayerChooser window = UiTestSupport.window(new ImageChooser(folder.toString()));
        clickButton(window);

        long worstMillis = 0;
        long end = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < end) {
            long start = System.nanoTime();
            EventQueue.invokeAndWait(() -> { });
            worstMillis = Math.max(worstMillis, (System.nanoTime() - start) / 1_000_000);
            Thread.sleep(5);
        }
        clickButton(window);

        System.out.println("Longest UI thread block while photos loaded: " + worstMillis + " ms");
        assertTrue(worstMillis < 100, "the UI thread was blocked for " + worstMillis + " ms");
    }

    @Test
    void switchingSetsKeepsTheOldSetsPhotosOutOfTheNewCache() throws Exception {
        Path folder = largePhotos("phone-photos", 4);
        PlayerChooser window = UiTestSupport.window(new ImageChooser(folder.toString()));

        onEdt(() -> window.load(new ImageChooser())); // switch while the large photos still load
        List<Student> demo = new ImageChooser().getImages();
        waitUntil(() -> window.icons.size() >= demo.size(), 5000);
        Thread.sleep(1500); // give a stale loader time to write into the cache, if it would

        assertTrue(new HashSet<>(demo).containsAll(window.icons.keySet()),
                "cache holds photos from the previous set: " + window.icons.keySet());
    }
}
