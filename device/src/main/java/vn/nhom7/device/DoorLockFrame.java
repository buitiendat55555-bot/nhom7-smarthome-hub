package vn.nhom7.device;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Cửa sổ mô phỏng khóa cửa: ổ khóa đổi hình khi khóa/mở, nút khóa/mở tại chỗ. */
public class DoorLockFrame extends JFrame {

    private final DoorLockDevice lock;
    private final LockPanel lockPanel = new LockPanel();
    private final JLabel stateLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel connLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton toggleBtn = new JButton("Khóa / Mở khóa");
    private final JButton connectBtn = new JButton("Kết nối Hub");

    public DoorLockFrame(DoorLockDevice lock) {
        super("Khóa cửa - " + lock.getDeviceId());
        this.lock = lock;

        stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 16f));

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(toggleBtn);
        buttons.add(connectBtn);

        JPanel bottom = new JPanel(new GridLayout(3, 1, 0, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        bottom.add(stateLabel);
        bottom.add(connLabel);
        bottom.add(buttons);

        setLayout(new BorderLayout());
        add(lockPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Lệnh từ Hub chạy trên luồng mạng -> chuyển về EDT mới vẽ lại
        lock.setOnChange(locked -> SwingUtilities.invokeLater(this::refreshState));
        toggleBtn.addActionListener(e -> lock.toggle());
        connectBtn.addActionListener(e -> connectToHub());
        new Timer(1000, e -> refreshConnection()).start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                lock.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(320, 400);
        setLocationRelativeTo(null);

        refreshState();
        refreshConnection();
    }

    public void connectToHub() {
        try {
            lock.connect();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được Hub: " + ex.getMessage());
        }
        refreshConnection();
    }

    private void refreshState() {
        lockPanel.setLocked(lock.isLocked());
        stateLabel.setText(lock.isLocked() ? "Cửa đang KHÓA" : "Cửa đang MỞ");
        stateLabel.setForeground(lock.isLocked() ? new Color(200, 40, 40) : new Color(40, 150, 60));
    }

    private void refreshConnection() {
        boolean connected = lock.isConnected();
        connLabel.setText(connected ? "Đã kết nối Hub" : "Chưa kết nối Hub");
        connLabel.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
        connectBtn.setEnabled(!connected);
    }

    /** Vẽ ổ khóa: quai đóng + thân đỏ khi khóa; quai nhấc lên + thân xanh khi mở. */
    private static class LockPanel extends JPanel {

        private boolean locked = true;

        void setLocked(boolean locked) {
            this.locked = locked;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = 120;
            int h = 90;
            int x = (getWidth() - w) / 2;
            int y = getHeight() / 2 - 10;

            // Quai khóa
            int sx = x + 25;
            int sw = w - 50;
            int sh = 80;
            int sy = locked ? y - sh / 2 : y - sh / 2 - 25; // mở thì nhấc quai lên
            g2.setStroke(new BasicStroke(12f));
            g2.setColor(Color.DARK_GRAY);
            g2.drawArc(sx, sy, sw, sh, 0, 180);
            g2.drawLine(sx, sy + sh / 2, sx, y + 5);
            if (locked) {
                g2.drawLine(sx + sw, sy + sh / 2, sx + sw, y + 5); // chân phải cắm vào thân
            }

            // Thân khóa
            g2.setColor(locked ? new Color(200, 40, 40) : new Color(40, 160, 60));
            g2.fillRoundRect(x, y, w, h, 16, 16);

            // Lỗ khóa
            g2.setColor(Color.WHITE);
            g2.fillOval(x + w / 2 - 8, y + h / 2 - 14, 16, 16);
            g2.fillRect(x + w / 2 - 3, y + h / 2 - 2, 6, 18);
        }
    }
}