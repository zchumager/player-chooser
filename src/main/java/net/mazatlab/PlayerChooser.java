package net.mazatlab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Picks students to participate in class. Photos cycle until stopped, and the student shown is
 * picked. Each student is picked at most once per session; a session lasts until the app closes.
 */
public class PlayerChooser {

    private static final String TITLE = "Player Chooser";
    private static final int CYCLE_MILLIS = 100;
    private static final int PHOTO_BOX = 160;
    private static final int PHOTO_MAX = PHOTO_BOX - 10; // leaves a margin inside the border
    private static final float PLACEHOLDER_FONT_SIZE = 32f;
    private static final float NAME_FONT_SIZE = 18f;
    /* Cached for photos that can't be decoded; has no size, so the student's name is shown */
    static final Icon UNREADABLE = new ImageIcon();
    private static final String START_TEXT = "Pick a student";
    private static final String STOP_TEXT = "Stop";

    private JFrame frame;
    private JButton btnPick;
    private JLabel lblRemaining;
    private JLabel lblPlayer;
    private JLabel lblName;

    private final Random random = new Random();
    /* Scaled photos for the current session. Replaced (not cleared) when a session starts, so a
     * loader from an earlier session notices it's stale and stops. */
    private volatile Map<Student, Icon> icons = new ConcurrentHashMap<>();
    private StudentPool pool;
    private Student current;
    private Timer timer;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ImageChooser images = ImageChooser.forStartup(args, Paths.get(""));
                    PlayerChooser window = new PlayerChooser(images);
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public PlayerChooser() {
        this(new ImageChooser());
    }

    PlayerChooser(ImageChooser imageChooser) {
        this.initialize();
        this.load(imageChooser);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        this.frame = new JFrame(TITLE);
        this.frame.setBounds(100, 100, 400, 352);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.getContentPane().setLayout(null);

        JMenu menuImages = new JMenu("Images");
        JMenuItem itemDemo = new JMenuItem("Load demo images");
        itemDemo.addActionListener(e -> this.loadDemoImages());
        menuImages.add(itemDemo);
        JMenuItem itemOwn = new JMenuItem("Load my images…");
        itemOwn.addActionListener(e -> this.chooseOwnImages());
        menuImages.add(itemOwn);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(menuImages);
        this.frame.setJMenuBar(menuBar);

        this.lblRemaining = new JLabel("", SwingConstants.CENTER);
        this.lblRemaining.setBounds(20, 10, 360, 20);
        this.frame.getContentPane().add(this.lblRemaining);

        this.lblPlayer = new JLabel("", SwingConstants.CENTER);
        this.lblPlayer.setForeground(Color.DARK_GRAY);
        this.lblPlayer.setFont(this.lblPlayer.getFont().deriveFont(Font.BOLD, 32f));
        this.lblPlayer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.lblPlayer.setBounds(120, 40, PHOTO_BOX, PHOTO_BOX);
        this.lblPlayer.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                PlayerChooser.this.stop();
            }
        });
        this.frame.getContentPane().add(this.lblPlayer);

        this.lblName = new JLabel(" ", SwingConstants.CENTER);
        this.lblName.setFont(this.lblName.getFont().deriveFont(Font.BOLD, 18f));
        this.lblName.setBounds(20, 210, 360, 28);
        this.frame.getContentPane().add(this.lblName);

        this.btnPick = new JButton(START_TEXT);
        this.btnPick.addActionListener(e -> this.toggle());
        this.btnPick.setBounds(100, 250, 200, 36);
        this.frame.getContentPane().add(this.btnPick);
    }

    /**
     * Loads a set of images and starts a new session with them. If the set is empty and a session
     * already exists, keeps the current session and returns false.
     */
    boolean load(ImageChooser source) {
        List<Student> students = source.getImages();
        if (students.isEmpty() && this.pool != null) {
            return false;
        }
        this.frame.setTitle(TITLE + " — " + source.description());
        this.startSession(students);
        return true;
    }

    private void loadDemoImages() {
        if (this.confirmNewSession()) {
            this.load(new ImageChooser());
        }
    }

    private void chooseOwnImages() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the folder with your class photos");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this.frame) != JFileChooser.APPROVE_OPTION || !this.confirmNewSession()) {
            return;
        }
        String folder = chooser.getSelectedFile().getPath();
        if (!this.load(new ImageChooser(folder))) {
            JOptionPane.showMessageDialog(this.frame,
                    "No photos found in " + folder + "\nSupported formats: PNG, JPG, JPEG, GIF.",
                    "No photos", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Asks before throwing away a session in which some students have already participated. */
    private boolean confirmNewSession() {
        int picked = this.pool.picked().size();
        if (picked == 0) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(this.frame,
                picked + " of " + this.pool.total() + " students have already participated.\n"
                        + "Load new images and start a new session?",
                "Start a new session?", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    /** Starts a new session: every student is back in the pool. */
    void startSession(List<Student> students) {
        if (this.timer != null) {
            this.timer.stop();
        }
        this.btnPick.setText(START_TEXT);
        Map<Student, Icon> sessionIcons = new ConcurrentHashMap<>();
        this.icons = sessionIcons;
        this.preloadPhotos(students, sessionIcons);
        this.pool = new StudentPool(students, this.random);
        this.current = null;
        this.showText(students.isEmpty() ? "No images" : "?", PLACEHOLDER_FONT_SIZE);
        this.lblName.setText(" ");
        this.updateStatus();
    }

    /** The button starts the photos cycling, or stops them if they already are. */
    void toggle() {
        if (this.isRunning()) {
            this.stop();
        } else {
            this.raffle();
        }
    }

    /** Starts cycling through the photos of the students who haven't participated yet. */
    public void raffle() {
        if (this.pool.isEmpty() || this.isRunning()) {
            return;
        }
        this.btnPick.setText(STOP_TEXT);
        this.lblName.setText(" ");
        this.showCandidate();
        this.timer = new Timer(CYCLE_MILLIS, e -> this.showCandidate());
        this.timer.start();
    }

    /** Stops on the student shown, who is picked and leaves the pool for this session. */
    void stop() {
        if (!this.isRunning()) {
            return;
        }
        this.timer.stop();
        this.btnPick.setText(START_TEXT);
        this.pool.pick(this.current);
        this.lblName.setText(this.current.name());
        this.updateStatus();
    }

    private boolean isRunning() {
        return this.timer != null && this.timer.isRunning();
    }

    /* Never decodes on the UI thread: a photo that isn't loaded yet, or can't be read, is shown as
     * the student's name instead */
    private void showCandidate() {
        this.current = this.pool.randomCandidate();
        Icon icon = this.icons.get(this.current);
        if (icon != null && icon.getIconWidth() > 0) {
            this.lblPlayer.setText(null);
            this.lblPlayer.setIcon(icon);
        } else {
            this.showText(this.current.name(), NAME_FONT_SIZE);
        }
    }

    private void showText(String text, float fontSize) {
        this.lblPlayer.setIcon(null);
        this.lblPlayer.setFont(this.lblPlayer.getFont().deriveFont(Font.BOLD, fontSize));
        this.lblPlayer.setText("<html><div style='text-align:center'>" + escapeHtml(text) + "</div></html>");
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /* Decoding large photos is slow, so each is scaled once, in the background, into this
     * session's cache. The loader stops as soon as a newer session replaces the cache. */
    private void preloadPhotos(List<Student> students, Map<Student, Icon> sessionIcons) {
        List<Student> copy = List.copyOf(students);
        Thread preload = new Thread(() -> {
            for (Student student : copy) {
                if (this.icons != sessionIcons) {
                    return;
                }
                sessionIcons.put(student, scaledIcon(student));
            }
        }, "photo-preload");
        preload.setDaemon(true);
        preload.start();
    }

    /**
     * Loads a photo scaled down to fit the picture box; small photos are not enlarged. Only the
     * scaled copy is kept in memory. Returns {@link #UNREADABLE} if the file can't be decoded.
     */
    static Icon scaledIcon(Student student) {
        BufferedImage original;
        try {
            original = ImageIO.read(student.image());
        } catch (IOException | RuntimeException e) {
            System.out.println("Could not read " + student.image() + ": " + e.getMessage());
            return UNREADABLE;
        }
        if (original == null) {
            System.out.println("Not a readable image: " + student.image());
            return UNREADABLE;
        }
        Dimension size = fitWithin(original.getWidth(), original.getHeight(), PHOTO_MAX);
        if (size.width == original.getWidth() && size.height == original.getHeight()) {
            return new ImageIcon(original);
        }
        Image smooth = original.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
        BufferedImage scaled = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(new ImageIcon(smooth).getImage(), 0, 0, null); // ImageIcon waits for the scaling
        g.dispose();
        return new ImageIcon(scaled);
    }

    /** The largest size with the same proportions that fits in a max × max box, never enlarged. */
    static Dimension fitWithin(int width, int height, int max) {
        double scale = Math.min(1.0, Math.min((double) max / width, (double) max / height));
        return new Dimension(
                Math.max(1, (int) Math.round(width * scale)),
                Math.max(1, (int) Math.round(height * scale)));
    }

    private void updateStatus() {
        if (this.pool.total() == 0) {
            this.lblRemaining.setText("No images found");
            this.btnPick.setEnabled(false);
        } else if (this.pool.isEmpty()) {
            this.lblRemaining.setText("Everyone has participated (" + this.pool.total() + " students)");
            this.btnPick.setEnabled(false);
        } else {
            this.lblRemaining.setText(this.pool.remainingCount() + " of " + this.pool.total() + " students left");
            this.btnPick.setEnabled(true);
        }
    }
}
