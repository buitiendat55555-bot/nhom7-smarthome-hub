package vn.nhom7.device;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Cửa sổ mô phỏng camera: khung hình, nút ghi, nút mô phỏng chuyển động. */
public class CameraFrame extends JFrame {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CameraDevice camera;
    private final ViewPanel view = new ViewPanel();
    private final JLabel stateLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel connLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton recordBtn = new JButton("Ghi / Dừng ghi");
    private final JButton motionBtn = new JButton("Mô phỏng chuyển động");
    private final JButton connectBtn = new JButton("Kết nối Hub");

    public CameraFrame(CameraDevice camera) {
        super("Camera - " + camera.getDeviceId());
        this.camera = camera;

        stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 15f));

        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.add(recordBtn);
        buttons.add(motionBtn);
        buttons.add(connectBtn);

        JPanel bottom = new JPanel(new GridLayout(3, 1, 0, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        bottom.add(stateLabel);
        bottom.add(connLabel);
        bottom.add(buttons);

        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Lệnh từ Hub và hẹn giờ chuyển động chạy ngoài EDT -> chuyển về EDT mới vẽ lại
        camera.setOnChange(() -> SwingUtilities.invokeLater(this::refreshState));
        recordBtn.addActionListener(e -> camera.toggleRecording());
        motionBtn.addActionListener(e -> camera.triggerMotion());
        connectBtn.addActionListener(e -> connectToHub());
        new Timer(1000, e -> {
            refreshConnection();
            view.repaint(); // cho đồng hồ trên khung hình chạy
        }).start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                camera.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 440);
        setLocationRelativeTo(null);

        refreshState();
        refreshConnection();
    }

    public void connectToHub() {
        try {
            camera.connect();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được Hub: " + ex.getMessage()
                    + "\nCamera sẽ tự thử kết nối lại.");
        }
        refreshConnection();
    }

    private void refreshState() {
        stateLabel.setText(camera.isRecording() ? "Đang GHI HÌNH" : "Không ghi hình");
        stateLabel.setForeground(camera.isRecording() ? Color.RED : Color.GRAY);
        view.repaint();
    }

    private void refreshConnection() {
        boolean connected = camera.isConnected();
        connLabel.setText(connected ? "Đã kết nối Hub" : "Chưa kết nối Hub (đang tự thử lại)");
        connLabel.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
        connectBtn.setEnabled(!connected);
    }

    /** Khung hình camera. */
    private class ViewPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 20;
            int y = 15;
            int w = getWidth() - 40;
            int h = getHeight() - 30;

            // Căn phòng: nền tối, sàn, cánh cửa
            g2.setColor(new Color(40, 44, 52));
            g2.fillRect(x, y, w, h);
            g2.setColor(new Color(70, 75, 85));
            g2.fillRect(x, y + h * 2 / 3, w, h / 3);
            g2.setColor(new Color(110, 85, 60));
            g2.fillRect(x + w / 2 - 30, y + h / 3, 60, h / 3);

            // Có chuyển động: vẽ người màu vàng
            if (camera.isMotion()) {
                int px = x + w / 4;
                int py = y + h / 3;
                g2.setColor(new Color(255, 210, 0));
                g2.fillOval(px, py, 30, 30);
                g2.fillRoundRect(px - 5, py + 32, 40, 60, 12, 12);
                g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                g2.drawString("PHÁT HIỆN CHUYỂN ĐỘNG", x + 10, y + h - 12);
            }

            // Đang ghi: chấm đỏ + REC
            if (camera.isRecording()) {
                g2.setColor(Color.RED);
                g2.fillOval(x + 10, y + 10, 14, 14);
                g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
                g2.drawString("REC", x + 30, y + 22);
            }

            // Đồng hồ góc phải
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            g2.drawString(LocalTime.now().format(TIME), x + w - 70, y + 22);
        }
    }
}