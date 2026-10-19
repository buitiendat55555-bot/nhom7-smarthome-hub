package vn.nhom7.device;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import vn.nhom7.common.ui.DeviceIcons;
import vn.nhom7.common.ui.DeviceText;
import vn.nhom7.common.ui.Ui;

/**
 * Khung chung cho mọi cửa sổ thiết bị: thanh tiêu đề, hình minh họa, dòng trạng thái,
 * dải báo "vừa nhận lệnh / vừa gửi STATUS" và nút kết nối. Lớp con chỉ thêm phần nút điều khiển.
 */
public abstract class DeviceFrame extends JFrame {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    protected final DeviceClient client;
    private final IconView view = new IconView();
    private final JLabel stateLabel = Ui.label("", Font.BOLD, 16, Ui.TEXT);
    private final Ui.Pill connPill = new Ui.Pill("", Ui.GREY, Color.WHITE);
    private final JLabel commandLabel = Ui.label("Chưa nhận lệnh nào", Font.BOLD, 12, Ui.MUTED);
    private final JLabel statusLabel = Ui.label(" ", Font.PLAIN, 12, Ui.MUTED);
    private final Ui.RoundPanel commandBox = new Ui.RoundPanel(Ui.BG, null, 12);
    private final Ui.RoundButton connectBtn = new Ui.RoundButton("Kết nối Hub", Ui.BLUE);
    private final JPanel controlsHolder = new JPanel(new BorderLayout());
    private Timer commandFade;
    private int phase;

    protected DeviceFrame(DeviceClient client) {
        super(DeviceText.label(client.getDeviceType()) + " - " + client.getDeviceId());
        this.client = client;

        // Thanh tiêu đề màu navy
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(Ui.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Ui.label(client.getName(), Font.BOLD, 16, Color.WHITE));
        titles.add(Ui.label(client.getDeviceId() + " · " + DeviceText.label(client.getDeviceType()),
                Font.PLAIN, 12, new Color(0xB8C2D6)));
        JPanel pillWrap = new JPanel(new BorderLayout());
        pillWrap.setOpaque(false);
        pillWrap.add(connPill, BorderLayout.CENTER);
        pillWrap.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        header.add(titles, BorderLayout.CENTER);
        header.add(pillWrap, BorderLayout.EAST);

        // Hình minh họa + dòng trạng thái
        view.setPreferredSize(new Dimension(170, 170));
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel card = new Ui.RoundPanel(Ui.CARD, Ui.BORDER, 18);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
        card.add(view, BorderLayout.CENTER);
        card.add(stateLabel, BorderLayout.SOUTH);

        // Dải báo lệnh / STATUS: cho thấy dữ liệu đang đi qua mạng
        commandBox.setLayout(new GridLayout(2, 1));
        commandBox.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        commandBox.add(commandLabel);
        commandBox.add(statusLabel);

        controlsHolder.setOpaque(false);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(controlsHolder, BorderLayout.NORTH);
        south.add(commandBox, BorderLayout.CENTER);
        south.add(connectBtn, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(Ui.BG);
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        body.add(card, BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        // Các listener được gọi trên luồng mạng -> chuyển về luồng giao diện (EDT)
        client.setOnStateChange(() -> SwingUtilities.invokeLater(this::refresh));
        client.setOnCommand(text -> SwingUtilities.invokeLater(() -> showCommand(text)));
        client.setOnStatusSent(() -> SwingUtilities.invokeLater(() ->
                statusLabel.setText("↑ Gửi STATUS lên Hub lúc " + LocalTime.now().format(TIME))));
        connectBtn.addActionListener(e -> connectToHub());

        new Timer(1000, e -> refreshConnection()).start();
        stateLabel.setPreferredSize(new Dimension(10, 44));
        new Timer(450, e -> { // nhịp chuyển động cho camera nhấp nháy, gió máy lạnh...
            phase++;
            view.repaint();
        }).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(310, 520);
        setLocationRelativeTo(null);
        refreshConnection();
    }

    /** Lớp con gọi ở cuối constructor để thêm khu vực nút điều khiển. */
    protected void setControls(JComponent controls) {
        controlsHolder.removeAll();
        controlsHolder.add(controls, BorderLayout.CENTER);
        refresh();
    }

    /** Lớp con override để đồng bộ thanh trượt, nút... theo trạng thái mới. Luôn chạy trên EDT. */
    protected void onStateChanged() {
    }

    private void refresh() {
        stateLabel.setText("<html><div style='text-align:center'>"
                + DeviceText.summary(client.getDeviceType(), client.getState()).replace(" · ", "<br>")
                + "</div></html>");
        onStateChanged();
        view.repaint();
    }

    public void connectToHub() {
        try {
            client.connect();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối được Hub: " + ex.getMessage()
                    + "\nThiết bị sẽ tự thử lại mỗi " + DeviceClient.RECONNECT_DELAY_SECONDS + " giây.");
        }
        refreshConnection();
    }

    private void refreshConnection() {
        boolean c = client.isConnected();
        connPill.set(c ? "● Online" : "● Mất kết nối", c ? Ui.GREEN : Ui.RED, Color.WHITE);
        connectBtn.setEnabled(!c);
        connectBtn.setText(c ? "Đã kết nối Hub 127.0.0.1:5000" : "Kết nối Hub");
        view.online = c;
    }

    /** Hiện dòng "Nhận lệnh ... từ dashboard1" nền xanh trong 3 giây. */
    private void showCommand(String text) {
        boolean ok = text.startsWith("✔");
        commandLabel.setText(text);
        commandLabel.setToolTipText(text + " lúc " + LocalTime.now().format(TIME));
        commandLabel.setForeground(ok ? Ui.GREEN : Ui.RED);
        commandBox.setColors(ok ? new Color(0xDCFCE7) : new Color(0xFEE2E2), ok ? Ui.GREEN : Ui.RED);
        if (commandFade != null) {
            commandFade.stop();
        }
        commandFade = new Timer(3000, e -> commandBox.setColors(Ui.BG, null));
        commandFade.setRepeats(false);
        commandFade.start();
    }

    /** Vùng vẽ hình thiết bị theo trạng thái hiện tại. */
    private class IconView extends JPanel {

        boolean online;

        IconView() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int size = Math.min(getWidth(), getHeight()) - 10;
            DeviceIcons.paint(g2, client.getDeviceType(), client.getState(), online,
                    (getWidth() - size) / 2, (getHeight() - size) / 2, size, phase);
            g2.dispose();
        }
    }

    // ---- tiện ích cho lớp con ----

    protected static JPanel row(JComponent... items) {
        JPanel p = new JPanel(new GridLayout(1, items.length, 8, 0));
        p.setOpaque(false);
        for (JComponent c : items) {
            p.add(c);
        }
        return p;
    }

    protected static JPanel column(JComponent... items) {
        JPanel p = new JPanel(new GridLayout(items.length, 1, 0, 8));
        p.setOpaque(false);
        for (JComponent c : items) {
            p.add(c);
        }
        return p;
    }
}
