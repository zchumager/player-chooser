package net.mazatlab;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

public class PlayerChooser {

    private JFrame frame;
    private JButton btnSortear;

    private ImageChooser imageChooser;
    private List<File> files;
    private JLabel lblPlayer;
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
        this.initialize();
        this.imageChooser = new ImageChooser();
        this.setDefaultImage();

        this.files = this.imageChooser.getImages();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 387, 379);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        this.btnSortear = new JButton("SortearJugadores");
        this.btnSortear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PlayerChooser.this.raffle();
            }
        });
        this.btnSortear.setBounds(87, 290, 200, 32);
        this.frame.getContentPane().add(this.btnSortear);

        this.lblPlayer = new JLabel("");
        this.lblPlayer.setForeground(Color.LIGHT_GRAY);
        this.lblPlayer.setBackground(Color.LIGHT_GRAY);
        this.lblPlayer.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        this.lblPlayer.setBounds(120, 130, 120, 130);
        this.lblPlayer.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e) {
                PlayerChooser.this.timer.stop();
                PlayerChooser.this.btnSortear.setEnabled(true);
            }
        });
        this.frame.getContentPane().add(lblPlayer);
    }

    public void setDefaultImage(){
        File defaultImage = this.imageChooser.getDefaultImage();
        this.lblPlayer.setIcon(new ImageIcon(defaultImage.getAbsolutePath()));

    }

    public void raffle() {
        this.timer = new Timer(100, new ActionListener() {

            int low = 0;
            int high = PlayerChooser.this.files.size();
            Random random = new Random();

            @Override public void actionPerformed(ActionEvent e) {
                int index = random.nextInt(high-low)+low;
                PlayerChooser.this.lblPlayer.setIcon(new ImageIcon(files.get(index).getAbsolutePath()));
                PlayerChooser.this.btnSortear.setEnabled(false);
            }
        });
        this.timer.start();
    }
}