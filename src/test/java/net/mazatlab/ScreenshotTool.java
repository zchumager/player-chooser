package net.mazatlab;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JRootPane;

/**
 * Renders the real window, after two picks with the demo images, to a PNG at 2x scale. Used for
 * the README screenshot. The OS title bar isn't included. Run after {@code mvn test-compile}:
 * <pre>java -cp target/classes:target/test-classes net.mazatlab.ScreenshotTool docs/screenshot.png</pre>
 */
public final class ScreenshotTool {

    private ScreenshotTool() {
    }

    public static void main(String[] args) throws Exception {
        String output = args.length > 0 ? args[0] : "docs/screenshot.png";
        PlayerChooser window = UiTestSupport.window(new ImageChooser());
        UiTestSupport.pickOne(window);
        UiTestSupport.pickOne(window);
        UiTestSupport.onEdt(() -> {
            window.frame.addNotify();
            window.frame.validate();
            JRootPane root = window.frame.getRootPane();
            int scale = 2;
            BufferedImage image = new BufferedImage(root.getWidth() * scale, root.getHeight() * scale, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            root.printAll(g);
            g.dispose();
            ImageIO.write(image, "png", new File(output));
            System.out.println("Wrote " + output + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            return null;
        });
        System.exit(0);
    }
}
