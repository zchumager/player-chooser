package net.mazatlab;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Picks students to participate in class. Photos cycle until stopped, and the student shown is
 * picked. Each student is picked at most once per session; a session lasts until the app closes.
 */
public class PlayerChooser {

    private static final int CYCLE_MILLIS = 100;

    private JFrame frame;
    private JButton btnPick;
    private JLabel lblRemaining;
    private JLabel lblPlayer;
    private JLabel lblName;

    private final Random random = new Random();
    private StudentPool pool;
    private Student current;
    private Timer timer;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    PlayerChooser window = new PlayerChooser();
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
        this.startSession(imageChooser.getImages());
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        this.frame = new JFrame("Player Chooser");
        this.frame.setBounds(100, 100, 400, 360);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.getContentPane().setLayout(null);

        this.lblRemaining = new JLabel("", SwingConstants.CENTER);
        this.lblRemaining.setBounds(20, 10, 360, 20);
        this.frame.getContentPane().add(this.lblRemaining);

        this.lblPlayer = new JLabel("", SwingConstants.CENTER);
        this.lblPlayer.setForeground(Color.DARK_GRAY);
        this.lblPlayer.setFont(this.lblPlayer.getFont().deriveFont(Font.BOLD, 32f));
        this.lblPlayer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.lblPlayer.setBounds(120, 40, 160, 160);
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

        this.btnPick = new JButton("Pick a student");
        this.btnPick.addActionListener(e -> this.raffle());
        this.btnPick.setBounds(100, 250, 200, 36);
        this.frame.getContentPane().add(this.btnPick);
    }

    /** Starts a new session: every student is back in the pool. */
    void startSession(List<Student> students) {
        if (this.timer != null) {
            this.timer.stop();
        }
        this.pool = new StudentPool(students, this.random);
        this.current = null;
        this.lblPlayer.setIcon(null);
        this.lblPlayer.setText(students.isEmpty() ? "No images" : "?");
        this.lblName.setText(" ");
        this.updateStatus();
    }

    /** Starts cycling through the photos of the students who haven't participated yet. */
    public void raffle() {
        if (this.pool.isEmpty() || this.isRunning()) {
            return;
        }
        this.btnPick.setEnabled(false);
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
        this.lblPlayer.setIcon(new ImageIcon(this.current.image()));
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
