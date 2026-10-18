package vn.nhom7.device;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Hashtable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import vn.nhom7.common.Protocol;

/** Cửa sổ mô phỏng cảm biến nhiệt: kéo thanh trượt để đổi nhiệt độ. */
public class TempSensorFrame extends JFrame {

    private final TempSensorDevice sensor;
    private final JLabel tempLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel warnLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JSlider slider = new JSlider(0, 600, 280); // đơn vị 0,1 °C: 0..60 °C
    private final JLabel connLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton connectBtn = new JButton("Kết nối Hub");

    public TempSensorFrame(TempSensorDevice sensor) {
        super("Cảm biến nhiệt - " + sensor.getDeviceId());
        this.sensor = sensor;

        tempLabel.setFont(tempLabel.getFont().deriveFont(Font.BOLD, 48f));
        warnLabel.setFont(warnLabel.getFont().deriveFont(Font.BOLD, 14f));
        warnLabel.setForeground(Color.RED);

        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        for (int v = 0; v <= 600; v += 100) {
            labels.put(v, new JLabel(String.valueOf(v / 10)));
        }
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(100);
        slider.setPaintTicks(true);
        slider.addChangeListener(e -> {
            refreshTemp(slider.getValue() / 10.0);
            if (!slider.getValueIsAdjusting()) {
                sensor.setTemperature(slider.getValue() / 10.0); // chỉ gửi khi thả chuột
            }
        });

        JPanel center = new JPanel(new GridLayout(2, 1));
        center.add(tempLabel);
        center.add(warnLabel);

        JPanel bottom = new JPanel(new GridLayout(3, 1, 0, 6));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        bottom.add(slider);
        bottom.add(connLabel);
        bottom.add(connectBtn);

        setLayout(new BorderLayout());
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        connectBtn.addActionListener(e -> connectToHub());
        new Timer(1000, e -> refreshConnection()).start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sensor.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(340, 360);
        setLocationRelativeTo(null);

        refreshTemp(sensor.getTemperature());
        refreshConnection();
    }

    public void connectToHub() {
        try {
            sensor.connect();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được Hub: " + ex.getMessage());
        }
        refreshConnection();
    }

    private void refreshTemp(double t) {
        tempLabel.setText(String.format("%.1f °C", t));
        boolean hot = t > Protocol.TEMP_ALERT_THRESHOLD;
        tempLabel.setForeground(hot ? Color.RED : new Color(0, 110, 200));
        warnLabel.setText(hot ? "Vượt ngưỡng " + (int) Protocol.TEMP_ALERT_THRESHOLD + " °C!" : " ");
    }

    private void refreshConnection() {
        boolean connected = sensor.isConnected();
        connLabel.setText(connected ? "Đã kết nối Hub" : "Chưa kết nối Hub");
        connLabel.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
        connectBtn.setEnabled(!connected);
    }
}