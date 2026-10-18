package vn.nhom7.dashboard;

import com.google.gson.JsonArray;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import vn.nhom7.common.Message;
import vn.nhom7.common.Protocol;

/** Cửa sổ Dashboard: trạng thái kết nối, bảng thiết bị realtime, điều khiển, nhật ký. */
public class DashboardFrame extends JFrame {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DeviceTableModel model = new DeviceTableModel();
    private final JTable table = new JTable(model);
    private final JLabel connLabel = new JLabel();
    private final JButton connectBtn = new JButton("Kết nối Hub");
    private final JTextArea logArea = new JTextArea(8, 40);
    private final HubConnection hub;

    // Khu vực điều khiển (Issue #11)
    private final JLabel selectedLabel = new JLabel("Chọn một thiết bị trong bảng");
    private final JLabel hintLabel = new JLabel(" ");
    private final JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 0, 6));
    private final List<JButton> actionButtons = new ArrayList<>();

    public DashboardFrame(String dashboardId) {
        super("Smart Home Dashboard - " + dashboardId);

        // Tin từ Hub đến trên luồng mạng -> chuyển về luồng giao diện (EDT) trước khi đụng vào Swing
        hub = new HubConnection(dashboardId,
                msg -> SwingUtilities.invokeLater(() -> handleMessage(msg)),
                () -> SwingUtilities.invokeLater(this::onDisconnected));

        // Thanh trên: trạng thái kết nối
        connLabel.setFont(connLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        top.add(connLabel, BorderLayout.CENTER);
        top.add(connectBtn, BorderLayout.EAST);

        // Giữa: bảng thiết bị
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Thiết bị"));

        // Phải: khu vực điều khiển
        JPanel control = new JPanel(new BorderLayout(0, 8));
        control.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Điều khiển"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        control.setPreferredSize(new Dimension(220, 0));
        hintLabel.setForeground(Color.GRAY);
        JPanel buttonsWrap = new JPanel(new BorderLayout());
        buttonsWrap.add(buttonsPanel, BorderLayout.NORTH); // nút không bị kéo dãn hết chiều cao
        control.add(selectedLabel, BorderLayout.NORTH);
        control.add(buttonsWrap, BorderLayout.CENTER);
        control.add(hintLabel, BorderLayout.SOUTH);

        // Dưới: nhật ký
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Nhật ký"));

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(control, BorderLayout.EAST);
        add(logScroll, BorderLayout.SOUTH);

        // Chọn dòng khác trong bảng -> dựng lại các nút
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                rebuildControls();
            }
        });
        connectBtn.addActionListener(e -> connectToHub());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hub.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 560);
        setLocationRelativeTo(null);
        setConnected(false);
        rebuildControls();
    }

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
                log("Nhận danh sách " + model.getRowCount() + " thiết bị");
                break;
            case DEVICE_UPDATE:
                model.update(msg.getPayload()); // không ghi nhật ký vì 5 giây có một lần
                updateButtonsEnabled();
                break;
            case DEVICE_OFFLINE:
                model.markOffline(msg.getString("deviceId"));
                updateButtonsEnabled();
                log("Thiết bị " + msg.getString("deviceId") + " mất kết nối");
                break;
            case ACK:
                String commandId = msg.getString("commandId");
                if (commandId == null) {
                    log("ACK từ " + msg.getFrom() + ": " + msg.getString("message"));
                } else {
                    boolean ok = msg.getBoolean("ok", false);
                    log((ok ? "Thành công" : "Thất bại") + " lệnh " + commandId
                            + " từ " + msg.getFrom() + ": " + msg.getString("message"));
                }
                break;
            case ERROR:
                log("LỖI từ Hub: " + msg.getString("message"));
                break;
            case ALERT:
                log("CẢNH BÁO: " + msg.getString("message"));
                break;
            default:
                log("Nhận " + msg.getType());
        }
    }

    /** Dựng lại nút theo loại của thiết bị đang chọn. */
    private void rebuildControls() {
        buttonsPanel.removeAll();
        actionButtons.clear();

        int row = table.getSelectedRow();
        if (row < 0) {
            selectedLabel.setText("Chọn một thiết bị trong bảng");
        } else {
            String deviceId = model.getDeviceId(row);
            String type = model.getDeviceType(row);
            selectedLabel.setText("<html>Thiết bị: <b>" + deviceId + "</b><br>Loại: " + type + "</html>");

            Map<String, String> actions = DeviceActions.forType(type);
            if (actions.isEmpty()) {
                buttonsPanel.add(new JLabel("Thiết bị này không nhận lệnh"));
            }
            for (Map.Entry<String, String> a : actions.entrySet()) {
                String action = a.getKey();
                JButton b = new JButton(a.getValue());
                b.addActionListener(e -> sendCommand(deviceId, action));
                actionButtons.add(b);
                buttonsPanel.add(b);
            }
        }
        updateButtonsEnabled();
        buttonsPanel.revalidate();
        buttonsPanel.repaint();
    }

    /** Chỉ cho bấm khi còn kết nối Hub và thiết bị đang online. */
    private void updateButtonsEnabled() {
        int row = table.getSelectedRow();
        boolean enabled = row >= 0 && hub.isConnected() && model.isOnline(row);
        for (JButton b : actionButtons) {
            b.setEnabled(enabled);
        }
        hintLabel.setText(!actionButtons.isEmpty() && !enabled
                ? "<html>Thiết bị hoặc Hub<br>đang mất kết nối</html>" : " ");
    }

    private void sendCommand(String deviceId, String action) {
        String commandId = hub.sendCommand(deviceId, action);
        log("Gửi lệnh " + action + " tới " + deviceId + " (" + commandId + ")");
    }

    private void onDisconnected() {
        setConnected(false);
        model.markAllOffline();
        log("Mất kết nối tới Hub");
    }

    private void setConnected(boolean connected) {
        connLabel.setText(connected ? "● Đã kết nối Hub" : "● Chưa kết nối Hub");
        connLabel.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
        connectBtn.setEnabled(!connected);
        updateButtonsEnabled();
    }

    private void log(String text) {
        logArea.append("[" + LocalTime.now().format(TIME) + "] " + text + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /** Thiết bị mất kết nối hiện chữ xám. */
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
}