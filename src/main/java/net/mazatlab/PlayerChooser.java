package net.mazatlab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final String START_TEXT = "Pick a student";
    private static final String STOP_TEXT = "Stop";

    private JFrame frame;
    private JButton btnPick;
    private JLabel lblRemaining;
    private JLabel lblPlayer;
    private JLabel lblName;

    private final Random random = new Random();
    private final Map<Student, Icon> icons = new ConcurrentHashMap<>();
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
        this.frame.setBounds(100, 100, 400, 380);
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
        this.icons.clear();
        this.preloadPhotos(students);
        this.pool = new StudentPool(students, this.random);
        this.current = null;
        this.lblPlayer.setIcon(null);
        this.lblPlayer.setText(students.isEmpty() ? "No images" : "?");
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

    private void showCandidate() {
        this.current = this.pool.randomCandidate();
        this.lblPlayer.setText(null);
        this.lblPlayer.setIcon(this.iconFor(this.current));
    }

    /* Decoding large photos is slow, so each is scaled once, in the background, and cached */
    private void preloadPhotos(List<Student> students) {
        List<Student> copy = List.copyOf(students);
        Thread preload = new Thread(() -> copy.forEach(this::iconFor), "photo-preload");
        preload.setDaemon(true);
        preload.start();
    }

    private Icon iconFor(Student student) {
        return this.icons.computeIfAbsent(student, PlayerChooser::scaledIcon);
    }

    /** Loads a photo scaled down to fit the picture box. Small photos are not enlarged. */
    private static Icon scaledIcon(Student student) {
        ImageIcon original = new ImageIcon(student.image());
        int width = original.getIconWidth();
        int height = original.getIconHeight();
        if (width <= 0 || height <= 0 || (width <= PHOTO_MAX && height <= PHOTO_MAX)) {
            return original;
        }
        Dimension size = fitWithin(width, height, PHOTO_MAX);
        return new ImageIcon(original.getImage().getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH));
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
