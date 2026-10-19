package vn.nhom7.dashboard;

import com.google.gson.JsonObject;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import vn.nhom7.common.ui.DeviceIcons;
import vn.nhom7.common.ui.DeviceText;
import vn.nhom7.common.ui.Ui;

/** Thẻ của một thiết bị trên Dashboard: hình minh họa, trạng thái, nút điều khiển nhanh. Chỉ dùng trên EDT. */
public class DeviceCard extends Ui.RoundPanel {

    /** Thẻ gọi hàm này khi người dùng bấm nút; value = null nếu lệnh không cần giá trị. */
    public interface CommandSender {
        void send(String deviceId, String action, Object value);
    }

    static final String[][] LIGHT_COLORS = {
        {"#FFC83D", "Vàng"}, {"#E5E7EB", "Trắng"}, {"#60A5FA", "Xanh"}, {"#F472B6", "Hồng"}, {"#4ADE80", "Lá"}
    };

    private final String id;
    private final String type;
    private final CommandSender sender;
    private JsonObject state = new JsonObject();
    private boolean online = true;
    private int phase;

    private final JLabel nameLabel;
    private final JLabel summaryLabel = Ui.label(" ", Font.BOLD, 15, Ui.TEXT);
    private final Ui.Pill onlinePill = new Ui.Pill("● Online", new Color(0xDCFCE7), Ui.GREEN);
    private final List<JComponent> controls = new ArrayList<>();
    private final Deque<Double> history = new ArrayDeque<>(); // nhiệt độ gần đây cho biểu đồ nhỏ
    private Ui.RoundButton toggleBtn;
    private JSlider brightness;
    private boolean syncing;
    private Timer flashTimer;

    public DeviceCard(String id, String type, String name, CommandSender sender) {
        super(Ui.CARD, Ui.BORDER, 20);
        this.id = id;
        this.type = (type != null) ? type : "";
        this.sender = sender;
        setLayout(new BorderLayout(10, 6));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setPreferredSize(new Dimension(260, 228));

        nameLabel = Ui.label(name == null || name.isEmpty() ? id : name, Font.BOLD, 15, Ui.TEXT);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(nameLabel);
        titles.add(Ui.label(id + " · " + DeviceText.label(this.type), Font.PLAIN, 12, Ui.MUTED));
        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.add(titles, BorderLayout.CENTER);
        JPanel pillWrap = new JPanel(new BorderLayout());
        pillWrap.setOpaque(false);
        pillWrap.add(onlinePill, BorderLayout.NORTH);
        top.add(pillWrap, BorderLayout.EAST);

        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int s = Math.min(getWidth(), getHeight());
                DeviceIcons.paint(g2, DeviceCard.this.type, state, online, (getWidth() - s) / 2, 0, s, phase);
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(88, 88));

        JPanel middle = new JPanel(new BorderLayout(4, 4));
        middle.setOpaque(false);
        middle.add(summaryLabel, BorderLayout.NORTH);
        if ("TEMP_SENSOR".equals(this.type)) {
            middle.add(new Sparkline(), BorderLayout.CENTER);
        }

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(icon, BorderLayout.WEST);
        center.add(middle, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
        refresh();
    }

    /** Nút điều khiển theo loại thiết bị. */
    private JComponent buildControls() {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.setOpaque(false);
        switch (type) {
            case "LIGHT": {
                toggleBtn = button("", Ui.ORANGE, () -> sender.send(id, isOn() ? "OFF" : "ON", null));
                p.add(toggleBtn);
                brightness = new JSlider(1, 100, 100);
                brightness.setOpaque(false);
                brightness.setToolTipText("Độ sáng (gửi lệnh BRIGHTNESS khi thả chuột)");
                brightness.addChangeListener(e -> {
                    if (!syncing && !brightness.getValueIsAdjusting()) {
                        sender.send(id, "BRIGHTNESS", brightness.getValue());
                    }
                });
                controls.add(brightness);
                JPanel colors = new JPanel(new GridLayout(1, LIGHT_COLORS.length, 4, 0));
                colors.setOpaque(false);
                for (String[] c : LIGHT_COLORS) {
                    Ui.RoundButton b = button("", Color.decode(c[0]), () -> sender.send(id, "COLOR", c[0]));
                    b.setToolTipText("Đổi màu: " + c[1]);
                    b.setPreferredSize(new Dimension(30, 22));
                    colors.add(b);
                }
                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.setOpaque(false);
                row.add(brightness, BorderLayout.CENTER);
                row.add(colors, BorderLayout.EAST);
                colors.setPreferredSize(new Dimension(140, 22));
                p.add(row);
                break;
            }
            case "DOOR_LOCK":
                toggleBtn = button("", Ui.GREEN, () ->
                        sender.send(id, DeviceIcons.bool(state, "locked") ? "UNLOCK" : "LOCK", null));
                p.add(toggleBtn);
                break;
            case "CAMERA":
                toggleBtn = button("", Ui.RED, () -> sender.send(id,
                        DeviceIcons.bool(state, "recording") ? "STOP_RECORDING" : "START_RECORDING", null));
                p.add(toggleBtn);
                break;
            case "AIR_CONDITIONER": {
                toggleBtn = button("", Ui.BLUE, () -> sender.send(id, isOn() ? "OFF" : "ON", null));
                Ui.RoundButton down = button("− 1°", Ui.TEAL, () -> sender.send(id, "SET_TEMP", target() - 1));
                Ui.RoundButton up = button("+ 1°", Ui.ORANGE, () -> sender.send(id, "SET_TEMP", target() + 1));
                JPanel row = new JPanel(new GridLayout(1, 3, 6, 0));
                row.setOpaque(false);
                row.add(toggleBtn);
                row.add(down);
                row.add(up);
                p.add(row);
                break;
            }
            case "TEMP_SENSOR":
                p.add(Ui.label("Cảm biến chỉ gửi dữ liệu, không nhận lệnh", Font.ITALIC, 12, Ui.MUTED));
                break;
            default:
                p.add(Ui.label("Không có lệnh điều khiển", Font.ITALIC, 12, Ui.MUTED));
        }
        return p;
    }

    private Ui.RoundButton button(String text, Color color, Runnable action) {
        Ui.RoundButton b = new Ui.RoundButton(text, color);
        b.addActionListener(e -> action.run());
        controls.add(b);
        return b;
    }

    private boolean isOn() {
        return "ON".equals(DeviceIcons.str(state, "power"));
    }

    private int target() {
        return (int) DeviceIcons.num(state, "targetTemp", 24);
    }

    // ---------- cập nhật từ Hub ----------

    public void update(String name, JsonObject newState) {
        if (name != null && !name.isEmpty()) {
            nameLabel.setText(name);
        }
        state = (newState != null) ? newState : new JsonObject();
        online = true;
        if ("TEMP_SENSOR".equals(type) && state.has("temperature")) {
            history.addLast(state.get("temperature").getAsDouble());
            while (history.size() > 30) {
                history.removeFirst();
            }
        }
        refresh();
    }

    public void setOnline(boolean value, boolean hubConnected) {
        online = value;
        refresh();
        for (JComponent c : controls) {
            c.setEnabled(online && hubConnected);
        }
    }

    public void setControlsEnabled(boolean hubConnected) {
        for (JComponent c : controls) {
            c.setEnabled(online && hubConnected);
        }
    }

    /** Viền xanh (thành công) hoặc đỏ (thất bại) trong 1,2 giây khi nhận ACK của lệnh. */
    public void flash(boolean ok) {
        setColors(ok ? new Color(0xF0FDF4) : new Color(0xFEF2F2), ok ? Ui.GREEN : Ui.RED);
        if (flashTimer != null) {
            flashTimer.stop();
        }
        flashTimer = new Timer(1200, e -> refreshBorder());
        flashTimer.setRepeats(false);
        flashTimer.start();
    }

    /** Gọi định kỳ để camera nhấp nháy, máy lạnh có gió... */
    public void tick() {
        phase++;
        if ("CAMERA".equals(type) || "AIR_CONDITIONER".equals(type) || "DOOR_LOCK".equals(type)) {
            repaint();
        }
    }

    private void refresh() {
        summaryLabel.setText("<html>" + (online ? DeviceText.summary(type, state).replace(" · ", "<br>")
                : "Mất kết nối") + "</html>");
        summaryLabel.setForeground(!online ? Ui.GREY
                : "TEMP_SENSOR".equals(type) ? DeviceIcons.colorForTemp(DeviceIcons.num(state, "temperature", Double.NaN))
                : Ui.TEXT);
        onlinePill.set(online ? "● Online" : "● Offline",
                online ? new Color(0xDCFCE7) : new Color(0xFEE2E2), online ? Ui.GREEN : Ui.RED);
        if (toggleBtn != null) {
            switch (type) {
                case "LIGHT":
                    toggleBtn.setText(isOn() ? "Tắt đèn" : "Bật đèn");
                    toggleBtn.setColor(isOn() ? Ui.NAVY : Ui.ORANGE);
                    break;
                case "DOOR_LOCK":
                    boolean locked = DeviceIcons.bool(state, "locked");
                    toggleBtn.setText(locked ? "Mở khóa" : "Khóa cửa");
                    toggleBtn.setColor(locked ? Ui.ORANGE : Ui.GREEN);
                    break;
                case "CAMERA":
                    boolean rec = DeviceIcons.bool(state, "recording");
                    toggleBtn.setText(rec ? "Dừng ghi hình" : "Bắt đầu ghi hình");
                    toggleBtn.setColor(rec ? Ui.NAVY : Ui.RED);
                    break;
                case "AIR_CONDITIONER":
                    toggleBtn.setText(isOn() ? "Tắt" : "Bật");
                    toggleBtn.setColor(isOn() ? Ui.NAVY : Ui.BLUE);
                    break;
                default:
                    break;
            }
        }
        if (brightness != null && !brightness.getValueIsAdjusting()) {
            syncing = true;
            brightness.setValue((int) DeviceIcons.num(state, "brightness", 100));
            syncing = false;
        }
        refreshBorder();
        repaint();
    }

    private void refreshBorder() {
        boolean alarm = online && (("TEMP_SENSOR".equals(type) && DeviceIcons.num(state, "temperature", 0) > 40)
                || DeviceIcons.bool(state, "motion") || DeviceIcons.bool(state, "ringing"));
        setColors(online ? (alarm ? new Color(0xFFF7ED) : Ui.CARD) : new Color(0xF9FAFB),
                alarm ? Ui.ORANGE : Ui.BORDER);
    }

    public String getDeviceId() {
        return id;
    }

    public String getDeviceType() {
        return type;
    }

    public boolean isOnline() {
        return online;
    }

    public JsonObject getState() {
        return state;
    }

    /** Biểu đồ nhỏ nhiệt độ 30 lần đo gần nhất, có vạch ngưỡng 40 °C. */
    private class Sparkline extends JComponent {

        Sparkline() {
            setPreferredSize(new Dimension(130, 46));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Ui.smooth(g2);
            int w = getWidth();
            int h = getHeight() - 4;
            double min = 0;
            double max = 60;
            int y40 = (int) (h - (40 - min) / (max - min) * h);
            g2.setColor(new Color(0xFEE2E2));
            g2.fillRect(0, 0, w, y40);
            g2.setColor(Ui.alpha(Ui.RED, 160));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
            g2.drawLine(0, y40, w, y40);
            g2.setFont(Ui.font(Font.PLAIN, 10));
            g2.drawString("40 °C", 2, y40 - 2);
            if (history.size() >= 2) {
                Path2D path = new Path2D.Double();
                int i = 0;
                int n = history.size();
                for (double t : history) {
                    double x = (double) i / (n - 1) * (w - 4) + 2;
                    double y = h - (t - min) / (max - min) * h;
                    if (i == 0) {
                        path.moveTo(x, y);
                    } else {
                        path.lineTo(x, y);
                    }
                    i++;
                }
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(DeviceIcons.colorForTemp(history.peekLast()));
                g2.draw(path);
            } else {
                g2.setColor(Ui.MUTED);
                g2.drawString("Đang chờ dữ liệu...", 4, h / 2 + 10);
            }
            g2.dispose();
        }
    }
}
