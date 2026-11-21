package net.mazatlab;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;
import javax.swing.JLabel;

/** Helpers for tests that drive a real (never shown) PlayerChooser window. */
final class UiTestSupport {

    private UiTestSupport() {
    }

    /** Swing windows can't be created without a display (e.g. on CI), so those tests are skipped. */
    static void assumeDisplay() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
    }

    /** Runs on the Swing UI thread and returns the result, rethrowing any exception. */
    static <T> T onEdt(Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            EventQueue.invokeAndWait(() -> {
                try {
                    result.set(action.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() instanceof RuntimeException r && r.getCause() != null ? r.getCause() : e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
        return result.get();
    }

    static void onEdt(Runnable action) throws Exception {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    static PlayerChooser window(ImageChooser images) throws Exception {
        return onEdt(() -> new PlayerChooser(images));
    }

    static void clickButton(PlayerChooser window) throws Exception {
        onEdt(() -> window.btnPick.doClick(0));
    }

    /** Presses on the photo, releasing {@code drift} pixels away (Swing sends no click then). */
    static void pressPhoto(PlayerChooser window, int drift) throws Exception {
        onEdt(() -> {
            JLabel photo = window.lblPlayer;
            long now = System.currentTimeMillis();
            photo.dispatchEvent(new MouseEvent(photo, MouseEvent.MOUSE_PRESSED, now, 0, 50, 50, 1, false, MouseEvent.BUTTON1));
            photo.dispatchEvent(new MouseEvent(photo, MouseEvent.MOUSE_RELEASED, now, 0, 50 + drift, 50, 1, false, MouseEvent.BUTTON1));
        });
    }

    /** Starts the photos cycling, lets them cycle briefly, and stops with the button. Returns the name picked. */
    static String pickOne(PlayerChooser window) throws Exception {
        clickButton(window);
        Thread.sleep(150);
        clickButton(window);
        return onEdt(() -> window.lblName.getText());
    }

    static void waitUntil(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
    }

    /** Writes a solid-color PNG or JPG photo of the given size. */
    static Path photo(Path folder, String fileName, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(new Color(Math.abs(fileName.hashCode()) % 0xFFFFFF));
        g.fillRect(0, 0, width, height);
        g.dispose();
        Path file = folder.resolve(fileName);
        String format = fileName.endsWith(".png") ? "png" : "jpg";
        ImageIO.write(image, format, file.toFile());
        return file;
    }

    /** A folder of small photos named after the given students. */
    static Path classFolder(Path parent, String folderName, String... students) throws IOException {
        Path folder = Files.createDirectories(parent.resolve(folderName));
        for (String student : students) {
            photo(folder, student + ".png", 120, 120);
        }
        return folder;
    }
}
