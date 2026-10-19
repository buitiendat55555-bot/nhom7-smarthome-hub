package vn.nhom7.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import vn.nhom7.common.Message;
import vn.nhom7.common.Protocol;
import vn.nhom7.common.ui.DeviceIcons;
import vn.nhom7.common.ui.Ui;

/**
 * Cửa sổ Dashboard: thẻ thiết bị realtime, thống kê nhanh, kịch bản, bật/tắt tự động hóa,
 * nhật ký, cảnh báo, gói tin JSON và lịch sử log.
 */
public class DashboardFrame extends JFrame {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ALERT_TAB = 1;
    private static final int MAX_TRAFFIC_LINES = 400;

    private final HubConnection hub;
    private final DashboardLog history;

    // Thẻ thiết bị (giữ thứ tự xuất hiện) và bảng chi tiết
    private final Map<String, DeviceCard> cards = new LinkedHashMap<>();
    private final JPanel cardGrid = new JPanel(new GridLayout(0, 3, 14, 14));
    private final JLabel emptyLabel = Ui.label("Chưa có thiết bị nào. Mở Trình khởi chạy thiết bị → \"Mở bộ demo\".",
            Font.PLAIN, 14, Ui.MUTED);
    private final DeviceTableModel model = new DeviceTableModel();
    private final JTable table = new JTable(model);

    // Thanh trên
    private final Ui.Pill connPill = new Ui.Pill("", Ui.GREY, Color.WHITE);
    private final Ui.RoundButton connectBtn = new Ui.RoundButton("Kết nối Hub", Ui.ORANGE);
    private final Ui.GhostButton historyBtn = new Ui.GhostButton("Lịch sử log");

    // Thống kê nhanh
    private final JLabel statOnline = statValue();
    private final JLabel statTemp = statValue();
    private final JLabel statLights = statValue();
    private final JLabel statAlerts = statValue();
    private int alertCount;

    // Kịch bản và tự động hóa
    private final Ui.RoundButton autoBtn = new Ui.RoundButton("", Ui.GREEN);
    private boolean automation = true;
    private final java.util.List<Ui.RoundButton> sceneButtons = new java.util.ArrayList<>();

    // Dải cảnh báo nổi phía trên các thẻ
    private final Ui.RoundPanel toast = new Ui.RoundPanel(Ui.RED, null, 14);
    private final JLabel toastLabel = Ui.label(" ", Font.BOLD, 14, Color.WHITE);
    private Timer toastTimer;

    // Khung bên phải: Nhật ký / Cảnh báo / Gói tin
    private final JTextArea logArea = new JTextArea();
    private final DefaultTableModel alertModel = new DefaultTableModel(
            new String[]{"Giờ", "Thiết bị", "Mức", "Nội dung"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable alertTable = new JTable(alertModel);
    private final JTextArea trafficArea = new JTextArea();
    private final JCheckBox hideUpdates = new JCheckBox("Ẩn DEVICE_UPDATE định kỳ", true);
    private final JTabbedPane sideTabs = new JTabbedPane();
    private int unreadAlerts = 0;

    public DashboardFrame(String dashboardId) {
        super("Smart Home Dashboard - " + dashboardId);
        history = new DashboardLog(dashboardId);

        // Tin từ Hub đến trên luồng mạng -> chuyển về luồng giao diện (EDT) trước khi đụng vào Swing
        hub = new HubConnection(dashboardId,
                msg -> SwingUtilities.invokeLater(() -> handleMessage(msg)),
                () -> SwingUtilities.invokeLater(this::onDisconnected));
        hub.setTrafficListener((out, line) -> SwingUtilities.invokeLater(() -> addTraffic(out, line)));

        JPanel north = new JPanel(new BorderLayout());
        north.add(buildHeader(dashboardId), BorderLayout.NORTH);
        north.add(buildStatsAndScenes(), BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout(14, 0));
        main.setBackground(Ui.BG);
        main.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        main.add(buildCenter(), BorderLayout.CENTER);
        main.add(buildSide(), BorderLayout.EAST);

        setLayout(new BorderLayout());
        getContentPane().setBackground(Ui.BG);
        add(north, BorderLayout.NORTH);
        add(main, BorderLayout.CENTER);

        connectBtn.addActionListener(e -> connectToHub());
        historyBtn.addActionListener(e -> new HistoryDialog(this, history).setVisible(true));
        // Nhịp chung cho chuyển động trên các thẻ (camera nhấp nháy, gió máy lạnh)
        new Timer(450, e -> cards.values().forEach(DeviceCard::tick)).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hub.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1340, screen.width), Math.min(800, screen.height - 40));
        setLocationRelativeTo(null);
        setConnected(false);
        refreshCards();
    }

    // ================== Dựng giao diện ==================

    private JPanel buildHeader(String dashboardId) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Ui.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Ui.label("Smart Home Dashboard", Font.BOLD, 20, Color.WHITE));
        titles.add(Ui.label("Nhóm 7 · " + dashboardId + " · Hub " + Protocol.DEFAULT_HOST + ":" + Protocol.PORT,
                Font.PLAIN, 12, new Color(0xB8C2D6)));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        right.setOpaque(false);
        right.add(connPill);
        right.add(historyBtn);
        right.add(connectBtn);
        // Dải thông báo nằm giữa thanh tiêu đề: cảnh báo, kết quả kịch bản (tự ẩn sau 5 giây)
        toast.setLayout(new BorderLayout());
        toast.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        toast.add(toastLabel, BorderLayout.CENTER);
        toast.setVisible(false);
        JPanel toastWrap = new JPanel(new BorderLayout());
        toastWrap.setOpaque(false);
        toastWrap.setBorder(BorderFactory.createEmptyBorder(4, 30, 4, 10));
        toastWrap.add(toast, BorderLayout.CENTER);
        header.add(titles, BorderLayout.WEST);
        header.add(toastWrap, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private static JLabel statValue() {
        return Ui.label("-", Font.BOLD, 22, Ui.TEXT);
    }

    private JPanel stat(String caption, JLabel value, Color accent) {
        Ui.RoundPanel p = new Ui.RoundPanel(Ui.CARD, Ui.BORDER, 16);
        p.setLayout(new BorderLayout(10, 0));
        p.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(5, 0));
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(Ui.label(caption, Font.PLAIN, 12, Ui.MUTED));
        text.add(value);
        p.add(bar, BorderLayout.WEST);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatsAndScenes() {
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(stat("Thiết bị online", statOnline, Ui.GREEN));
        stats.add(stat("Nhiệt độ trong nhà", statTemp, Ui.ORANGE));
        stats.add(stat("Đèn đang bật", statLights, Ui.AMBER));
        stats.add(stat("Cảnh báo phiên này", statAlerts, Ui.RED));

        JPanel scenes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scenes.setOpaque(false);
        scenes.add(Ui.label("Kịch bản nhanh:", Font.BOLD, 13, Ui.TEXT));
        scenes.add(scene("Về nhà", Ui.GREEN));
        scenes.add(scene("Rời nhà", Ui.NAVY));
        scenes.add(scene("Ban đêm", new Color(0x4338CA)));
        scenes.add(scene("Tắt tất cả", Ui.MUTED));

        autoBtn.addActionListener(e -> {
            String id = hub.sendCommand(Protocol.HUB_ID, automation ? "AUTOMATION_OFF" : "AUTOMATION_ON");
            log("Gửi lệnh " + (automation ? "tắt" : "bật") + " tự động hóa tới Hub (" + id + ")");
        });
        JPanel autoWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        autoWrap.setOpaque(false);
        autoWrap.add(Ui.label("Hub tự động hóa:", Font.BOLD, 13, Ui.TEXT));
        autoWrap.add(autoBtn);
        refreshAutomation();

        JPanel sceneRow = new JPanel(new BorderLayout());
        sceneRow.setOpaque(false);
        sceneRow.add(scenes, BorderLayout.WEST);
        sceneRow.add(autoWrap, BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Ui.BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        p.add(stats, BorderLayout.NORTH);
        p.add(sceneRow, BorderLayout.CENTER);
        return p;
    }

    private Ui.RoundButton scene(String name, Color color) {
        Ui.RoundButton b = new Ui.RoundButton(name, color);
        b.addActionListener(e -> runScene(name));
        sceneButtons.add(b);
        return b;
    }

    private JComponentHolder buildCenter() {
        cardGrid.setOpaque(false);
        GridWrap gridWrap = new GridWrap();
        gridWrap.add(cardGrid, BorderLayout.NORTH);
        JScrollPane cardScroll = new JScrollPane(gridWrap);
        cardScroll.setBorder(BorderFactory.createEmptyBorder());
        cardScroll.getViewport().setBackground(Ui.BG);
        cardScroll.getVerticalScrollBar().setUnitIncrement(20);
        cardScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Số cột thẻ thay đổi theo bề rộng cửa sổ (mỗi thẻ tối thiểu ~270 px)
        cardScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int cols = Math.max(1, cardScroll.getViewport().getWidth() / 270);
                if (((GridLayout) cardGrid.getLayout()).getColumns() != cols) {
                    cardGrid.setLayout(new GridLayout(0, cols, 14, 14));
                    cardGrid.revalidate();
                }
            }
        });

        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        JScrollPane tableScroll = new JScrollPane(table);

        JTabbedPane centerTabs = new JTabbedPane();
        centerTabs.addTab("Ngôi nhà", cardScroll);
        centerTabs.addTab("Bảng chi tiết (dữ liệu thô)", tableScroll);

        JComponentHolder p = new JComponentHolder();
        p.add(centerTabs, BorderLayout.CENTER);
        return p;
    }

    /** Khung chứa lưới thẻ: luôn rộng bằng vùng nhìn để không có thanh cuộn ngang. */
    private static class GridWrap extends JPanel implements javax.swing.Scrollable {
        GridWrap() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) {
            return 200;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Panel nền trong suốt dùng làm khung chứa. */
    private static class JComponentHolder extends JPanel {
        JComponentHolder() {
            super(new BorderLayout(0, 10));
            setOpaque(false);
        }
    }

    private JTabbedPane buildSide() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(Ui.font(Font.PLAIN, 12));

        alertTable.setRowHeight(24);
        alertTable.getColumnModel().getColumn(0).setMaxWidth(70);
        alertTable.getColumnModel().getColumn(1).setMaxWidth(70);
        alertTable.getColumnModel().getColumn(2).setMaxWidth(75);
        alertTable.setDefaultRenderer(Object.class, new AlertRenderer());

        trafficArea.setEditable(false);
        trafficArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        trafficArea.setLineWrap(true); // mỗi message là một dòng JSON dài -> cho xuống dòng khi hiển thị
        hideUpdates.setOpaque(false);
        JPanel trafficPanel = new JPanel(new BorderLayout(0, 4));
        JPanel trafficTop = new JPanel(new GridLayout(2, 1));
        trafficTop.add(Ui.label("  → Dashboard gửi lên Hub      ← Hub gửi xuống Dashboard", Font.PLAIN, 12, Ui.MUTED));
        trafficTop.add(hideUpdates);
        trafficPanel.add(trafficTop, BorderLayout.NORTH);
        trafficPanel.add(new JScrollPane(trafficArea), BorderLayout.CENTER);

        sideTabs.addTab("Nhật ký", new JScrollPane(logArea));
        sideTabs.addTab("Cảnh báo", new JScrollPane(alertTable));
        sideTabs.addTab("Gói tin JSON", trafficPanel);
        sideTabs.setPreferredSize(new Dimension(380, 0));
        sideTabs.addChangeListener(e -> {
            if (sideTabs.getSelectedIndex() == ALERT_TAB) {
                unreadAlerts = 0; // mở tab = đã xem
                updateAlertTab();
            }
        });
        return sideTabs;
    }

    // ================== Kết nối và xử lý message ==================

    public void connectToHub() {
        try {
            hub.connect(Protocol.DEFAULT_HOST, Protocol.PORT);
            setConnected(true);
            log("Đã kết nối Hub " + Protocol.DEFAULT_HOST + ":" + Protocol.PORT);
        } catch (IOException ex) {
            setConnected(false);
            log("Không kết nối được Hub: " + ex.getMessage());
        }
    }

    /** Chạy trên EDT. Mỗi loại message cập nhật một phần giao diện. */
    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case DEVICE_LIST:
                JsonArray devices = msg.getPayload().has("devices")
                        ? msg.getPayload().getAsJsonArray("devices") : new JsonArray();
                model.setAll(devices);
                cards.clear();
                for (JsonElement e : devices) {
                    updateCard(e.getAsJsonObject());
                }
                automation = msg.getBoolean("automation", true);
                refreshAutomation();
                refreshCards();
                log("Nhận danh sách " + devices.size() + " thiết bị");
                break;
            case DEVICE_UPDATE:
                model.update(msg.getPayload()); // không ghi nhật ký vì 5 giây có một lần
                boolean isNew = !cards.containsKey(msg.getString("deviceId"));
                updateCard(msg.getPayload());
                if (isNew) {
                    refreshCards();
                    log("Thiết bị mới: " + msg.getString("deviceId"));
                }
                refreshStats();
                break;
            case DEVICE_OFFLINE: {
                String id = msg.getString("deviceId");
                model.markOffline(id);
                DeviceCard c = cards.get(id);
                if (c != null) {
                    c.setOnline(false, hub.isConnected());
                }
                refreshStats();
                log("Thiết bị " + id + " mất kết nối");
                break;
            }
            case ACK:
                handleAck(msg);
                break;
            case ERROR:
                log("LỖI từ Hub: " + msg.getString("message"));
                showToast("Lỗi: " + msg.getString("message"), Ui.RED);
                break;
            case ALERT:
                addAlert(msg.getString("deviceId"), msg.getString("level"), msg.getString("message"));
                break;
            default:
                log("Nhận " + msg.getType());
        }
    }

    private void handleAck(Message msg) {
        String commandId = msg.getString("commandId");
        if (commandId == null) {
            log("ACK từ " + msg.getFrom() + ": " + msg.getString("message"));
            return;
        }
        boolean ok = msg.getBoolean("ok", false);
        log((ok ? "✔ Thành công" : "✖ Thất bại") + " lệnh " + commandId
                + " từ " + msg.getFrom() + ": " + msg.getString("message"));
        if (Protocol.HUB_ID.equals(msg.getFrom()) && msg.getPayload().has("automation")) {
            automation = msg.getBoolean("automation", automation);
            refreshAutomation();
        }
        DeviceCard c = cards.get(msg.getFrom());
        if (c != null) {
            c.flash(ok);
        }
    }

    private void updateCard(JsonObject d) {
        String id = DeviceIcons.str(d, "deviceId");
        if (id == null) {
            return;
        }
        JsonObject state = (d.has("state") && d.get("state").isJsonObject())
                ? d.getAsJsonObject("state") : new JsonObject();
        DeviceCard c = cards.get(id);
        if (c == null) {
            c = new DeviceCard(id, DeviceIcons.str(d, "deviceType"), DeviceIcons.str(d, "name"), this::sendCommand);
            cards.put(id, c);
        }
        c.update(DeviceIcons.str(d, "name"), state);
        c.setControlsEnabled(hub.isConnected());
    }

    /** Dựng lại lưới thẻ khi thêm/bớt thiết bị. */
    private void refreshCards() {
        cardGrid.removeAll();
        if (cards.isEmpty()) {
            cardGrid.add(emptyLabel);
        }
        for (DeviceCard c : cards.values()) {
            cardGrid.add(c);
        }
        cardGrid.revalidate();
        cardGrid.repaint();
        refreshStats();
    }

    private void refreshStats() {
        int online = 0;
        int lights = 0;
        double tempSum = 0;
        int tempCount = 0;
        for (DeviceCard c : cards.values()) {
            if (!c.isOnline()) {
                continue;
            }
            online++;
            JsonObject s = c.getState();
            if ("LIGHT".equals(c.getDeviceType()) && "ON".equals(DeviceIcons.str(s, "power"))) {
                lights++;
            }
            if ("TEMP_SENSOR".equals(c.getDeviceType()) && s.has("temperature")) {
                tempSum += s.get("temperature").getAsDouble();
                tempCount++;
            }
        }
        statOnline.setText(online + " / " + cards.size());
        statLights.setText(String.valueOf(lights));
        if (tempCount == 0) {
            statTemp.setText("-");
            statTemp.setForeground(Ui.TEXT);
        } else {
            double t = tempSum / tempCount;
            statTemp.setText(String.format("%.1f °C", t));
            statTemp.setForeground(DeviceIcons.colorForTemp(t));
        }
        statAlerts.setText(String.valueOf(alertCount));
        statAlerts.setForeground(alertCount > 0 ? Ui.RED : Ui.TEXT);
    }

    // ================== Lệnh và kịch bản ==================

    private void sendCommand(String deviceId, String action, Object value) {
        String commandId = hub.sendCommand(deviceId, action, value);
        log("Gửi lệnh " + action + (value != null ? " " + value : "") + " tới " + deviceId + " (" + commandId + ")");
    }

    /**
     * Một kịch bản = nhiều COMMAND gửi liên tiếp tới nhiều thiết bị qua cùng một kết nối TCP.
     * Hub định tuyến từng lệnh tới đúng thiết bị theo trường "to".
     */
    private void runScene(String name) {
        int sent = 0;
        for (DeviceCard c : cards.values()) {
            if (!c.isOnline()) {
                continue;
            }
            String id = c.getDeviceId();
            switch (c.getDeviceType()) {
                case "LIGHT":
                    if ("Về nhà".equals(name)) {
                        sent += send(id, "BRIGHTNESS", 100) + send(id, "COLOR", "#FFC83D");
                    } else if ("Ban đêm".equals(name)) {
                        sent += send(id, "BRIGHTNESS", 15) + send(id, "COLOR", "#F472B6");
                    } else {
                        sent += send(id, "OFF", null);
                    }
                    break;
                case "DOOR_LOCK":
                    if (!"Tắt tất cả".equals(name)) {
                        sent += send(id, "Về nhà".equals(name) ? "UNLOCK" : "LOCK", null);
                    }
                    break;
                case "CAMERA":
                    if (!"Tắt tất cả".equals(name)) {
                        sent += send(id, "Về nhà".equals(name) ? "STOP_RECORDING" : "START_RECORDING", null);
                    }
                    break;
                case "AIR_CONDITIONER":
                    if ("Về nhà".equals(name)) {
                        sent += send(id, "SET_TEMP", 25);
                    } else if ("Ban đêm".equals(name)) {
                        sent += send(id, "SET_TEMP", 27);
                    } else {
                        sent += send(id, "OFF", null);
                    }
                    break;
                default:
                    break;
            }
        }
        log("Kịch bản «" + name + "»: đã gửi " + sent + " lệnh");
        showToast("Kịch bản «" + name + "»: gửi " + sent + " lệnh tới các thiết bị", Ui.BLUE);
    }

    private int send(String id, String action, Object value) {
        sendCommand(id, action, value);
        return 1;
    }

    private void refreshAutomation() {
        autoBtn.setText(automation ? "ĐANG BẬT" : "ĐANG TẮT");
        autoBtn.setColor(automation ? Ui.GREEN : Ui.GREY);
        autoBtn.setToolTipText("Nóng > 40 °C → bật máy lạnh · Chuyển động khi cửa khóa → bật đèn, ghi hình");
    }

    // ================== Cảnh báo, nhật ký, gói tin ==================

    /** Thêm cảnh báo lên đầu bảng, hiện dải màu, kêu bíp, đánh dấu chưa xem nếu đang ở tab khác. */
    private void addAlert(String deviceId, String level, String text) {
        String lv = (level != null) ? level : "WARNING";
        alertModel.insertRow(0, new Object[]{LocalTime.now().format(TIME), deviceId, lv, text});
        alertCount++;
        refreshStats();
        log("CẢNH BÁO [" + lv + "] " + deviceId + ": " + text);
        Toolkit.getDefaultToolkit().beep();
        showToast(("WARNING".equals(lv) ? "⚠ " : "ℹ ") + text, "WARNING".equals(lv) ? Ui.RED : Ui.BLUE);
        if (sideTabs.getSelectedIndex() != ALERT_TAB) {
            unreadAlerts++;
            updateAlertTab();
        }
    }

    private void showToast(String text, Color color) {
        toastLabel.setText(text);
        toast.setColors(color, null);
        toast.setVisible(true);
        toast.revalidate();
        if (toastTimer != null) {
            toastTimer.stop();
        }
        toastTimer = new Timer(5000, e -> {
            toast.setVisible(false);
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    private void updateAlertTab() {
        sideTabs.setTitleAt(ALERT_TAB, unreadAlerts > 0 ? "Cảnh báo (" + unreadAlerts + " mới)" : "Cảnh báo");
        sideTabs.setForegroundAt(ALERT_TAB, unreadAlerts > 0 ? Ui.RED : null);
    }

    /** Ghi từng dòng JSON đi/đến để thấy đúng giao thức: mỗi message là một dòng. */
    private void addTraffic(boolean out, String line) {
        if (hideUpdates.isSelected() && line.contains("\"type\":\"DEVICE_UPDATE\"")) {
            return;
        }
        trafficArea.append(LocalTime.now().format(TIME) + (out ? " → " : " ← ") + line + "\n\n");
        int extra = trafficArea.getLineCount() - MAX_TRAFFIC_LINES * 2;
        if (extra > 0) {
            try {
                trafficArea.replaceRange("", 0, trafficArea.getLineEndOffset(extra - 1));
            } catch (javax.swing.text.BadLocationException ignored) {
            }
        }
        trafficArea.setCaretPosition(trafficArea.getDocument().getLength());
    }

    private void onDisconnected() {
        setConnected(false);
        model.markAllOffline();
        for (DeviceCard c : cards.values()) {
            c.setOnline(false, false);
        }
        refreshStats();
        log("Mất kết nối tới Hub");
        showToast("Mất kết nối tới Hub — bấm \"Kết nối Hub\" khi Hub chạy lại", Ui.RED);
    }

    private void setConnected(boolean connected) {
        connPill.set(connected ? "● Đã kết nối Hub" : "● Chưa kết nối Hub",
                connected ? Ui.GREEN : Ui.RED, Color.WHITE);
        connectBtn.setEnabled(!connected);
        autoBtn.setEnabled(connected);
        for (Ui.RoundButton b : sceneButtons) {
            b.setEnabled(connected);
        }
        for (DeviceCard c : cards.values()) {
            c.setControlsEnabled(connected);
        }
    }

    /** Ghi lên màn hình (giờ:phút:giây) và nối vào file log (ngày giờ đầy đủ). */
    private void log(String text) {
        logArea.append("[" + LocalTime.now().format(TIME) + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        history.append(LocalDateTime.now().format(FULL) + " | " + text);
    }

    /** Bảng chi tiết: thiết bị mất kết nối hiện chữ xám. */
    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (!selected) {
                c.setForeground(model.isOnline(row) ? Color.BLACK : Color.GRAY);
            }
            return c;
        }
    }

    /** WARNING chữ đỏ đậm, INFO chữ xanh. */
    private class AlertRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                boolean focus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (!selected) {
                boolean warning = "WARNING".equals(alertModel.getValueAt(row, 2));
                c.setForeground(warning ? new Color(200, 0, 0) : new Color(0, 90, 200));
                c.setFont(c.getFont().deriveFont(warning ? Font.BOLD : Font.PLAIN));
            }
            if (c instanceof JLabel) {
                ((JLabel) c).setToolTipText(String.valueOf(value));
            }
            return c;
        }
    }
}
