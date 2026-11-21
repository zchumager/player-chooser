package net.mazatlab;

import static net.mazatlab.UiTestSupport.assumeDisplay;
import static net.mazatlab.UiTestSupport.classFolder;
import static net.mazatlab.UiTestSupport.onEdt;
import static net.mazatlab.UiTestSupport.pickOne;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives "Images → Load my images…" through the real folder chooser and message dialogs. The
 * dialogs appear on screen while it runs, so it only runs when asked:
 * {@code mvn test -Dui.dialogs=true}
 */
@EnabledIfSystemProperty(named = "ui.dialogs", matches = "true")
class PlayerChooserDialogsTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void needsDisplay() {
        assumeDisplay();
    }

    private static <T> T find(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = find(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JDialog nextDialog(long timeoutMillis) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < end) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JDialog dialog && dialog.isShowing()) {
                    return dialog;
                }
            }
            Thread.sleep(50);
        }
        return null;
    }

    /**
     * Picks one student (so replacing the session needs confirmation), then chooses the folder and
     * answers any confirmation. Returns the dialogs seen, in order.
     */
    private static List<String> loadMyImages(PlayerChooser window, Path folder, boolean confirm) throws Exception {
        pickOne(window);
        EventQueue.invokeLater(onEdtMenuItem(window)::doClick);

        List<String> seen = new ArrayList<>();
        JDialog dialog;
        while ((dialog = nextDialog(3000)) != null) {
            JFileChooser chooser = find(dialog, JFileChooser.class);
            JOptionPane pane = find(dialog, JOptionPane.class);
            if (chooser != null) {
                seen.add("folder chooser");
                onEdt(() -> {
                    chooser.setSelectedFile(folder.toFile());
                    chooser.approveSelection();
                });
            } else if (pane != null && pane.getOptionType() == JOptionPane.YES_NO_OPTION) {
                seen.add("confirm");
                onEdt(() -> pane.setValue(confirm ? JOptionPane.YES_OPTION : JOptionPane.NO_OPTION));
            } else if (pane != null) {
                seen.add("warning: " + pane.getMessage());
                onEdt(() -> pane.setValue(JOptionPane.OK_OPTION));
            } else {
                JDialog unknown = dialog;
                seen.add("unknown: " + unknown.getTitle());
                onEdt(unknown::dispose);
            }
            Thread.sleep(200);
        }
        return seen;
    }

    private static javax.swing.JMenuItem onEdtMenuItem(PlayerChooser window) {
        return window.frame.getJMenuBar().getMenu(0).getItem(1); // Images → Load my images…
    }

    @Test
    void aFolderWithoutPhotosWarnsWithoutAskingToReplaceTheSession() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        Path empty = Files.createDirectories(this.tempDir.resolve("empty"));

        List<String> dialogs = loadMyImages(window, empty, true);

        assertEquals(2, dialogs.size(), dialogs.toString());
        assertEquals("folder chooser", dialogs.get(0));
        assertTrue(dialogs.get(1).startsWith("warning: No photos found"), dialogs.get(1));
        assertTrue(dialogs.get(1).contains("HEIC"), dialogs.get(1));
        assertEquals("6 of 7 students left", onEdt(() -> window.lblRemaining.getText()));
    }

    @Test
    void answeringNoKeepsTheCurrentSession() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        Path folder = classFolder(this.tempDir, "class-3B", "ana", "luis");

        List<String> dialogs = loadMyImages(window, folder, false);

        assertEquals(List.of("folder chooser", "confirm"), dialogs);
        assertEquals("Player Chooser — Demo images", onEdt(() -> window.frame.getTitle()));
        assertEquals("6 of 7 students left", onEdt(() -> window.lblRemaining.getText()));
    }

    @Test
    void answeringYesStartsANewSessionWithTheFolder() throws Exception {
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        Path folder = classFolder(this.tempDir, "class-3B", "ana", "luis");

        List<String> dialogs = loadMyImages(window, folder, true);

        assertEquals(List.of("folder chooser", "confirm"), dialogs);
        assertEquals("Player Chooser — class-3B", onEdt(() -> window.frame.getTitle()));
        assertEquals("2 of 2 students left", onEdt(() -> window.lblRemaining.getText()));
    }
}
