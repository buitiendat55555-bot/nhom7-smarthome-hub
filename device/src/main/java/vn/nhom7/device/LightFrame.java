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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Cửa sổ mô phỏng đèn: bóng đèn đổi màu, nút bật/tắt, trạng thái kết nối Hub. */
public class LightFrame extends JFrame {

    private final LightDevice light;
    private final BulbPanel bulb = new BulbPanel();
    private final JLabel stateLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel connLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton toggleBtn = new JButton("Bật / Tắt");
    private final JButton connectBtn = new JButton("Kết nối Hub");

    public LightFrame(LightDevice light) {
        super("Đèn - " + light.getDeviceId());
        this.light = light;

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
        add(bulb, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Lệnh từ Hub chạy trên luồng mạng -> phải chuyển về luồng giao diện (EDT) mới được vẽ lại
        light.setOnChange(on -> SwingUtilities.invokeLater(this::refreshState));
        toggleBtn.addActionListener(e -> light.toggle());
        connectBtn.addActionListener(e -> connectToHub());

        // Mỗi giây kiểm tra trạng thái kết nối một lần
        new Timer(1000, e -> refreshConnection()).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                light.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(null);

        refreshState();
        refreshConnection();
    }

    public void connectToHub() {
        try {
            light.connect();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được Hub: " + ex.getMessage());
        }
        refreshConnection();
    }

    private void refreshState() {
        bulb.setOn(light.isOn());
        stateLabel.setText(light.isOn() ? "Đèn đang BẬT" : "Đèn đang TẮT");
    }

    private void refreshConnection() {
        boolean connected = light.isConnected();
        connLabel.setText(connected ? "Đã kết nối Hub" : "Chưa kết nối Hub");
        connLabel.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
        connectBtn.setEnabled(!connected);
    }

    /** Vẽ bóng đèn: vàng và có quầng sáng khi bật, xám khi tắt. */
    private static class BulbPanel extends JPanel {

        private boolean on;

        void setOn(boolean on) {
            this.on = on;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 70;
            int x = (getWidth() - size) / 2;
            int y = 25;

            if (on) {
                g2.setColor(new Color(255, 230, 120, 90));
                g2.fillOval(x - 15, y - 15, size + 30, size + 30);
            }
            g2.setColor(on ? new Color(255, 210, 0) : new Color(170, 170, 170));
            g2.fillOval(x, y, size, size);

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(getWidth() / 2 - size / 6, y + size - 5, size / 3, 25);
        }
    }
}