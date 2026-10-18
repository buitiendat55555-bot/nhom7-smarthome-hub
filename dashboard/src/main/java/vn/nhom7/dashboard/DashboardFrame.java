package vn.nhom7.dashboard;

import com.google.gson.JsonArray;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

/** Cửa sổ Dashboard: trạng thái kết nối, bảng thiết bị realtime, nhật ký sự kiện. */
public class DashboardFrame extends JFrame {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DeviceTableModel model = new DeviceTableModel();
    private final JTable table = new JTable(model);
    private final JLabel connLabel = new JLabel();
    private final JButton connectBtn = new JButton("Kết nối Hub");
    private final JTextArea logArea = new JTextArea(8, 40);
    private final HubConnection hub;

    public DashboardFrame(String dashboardId) {
        super("Smart Home Dashboard - " + dashboardId);

        // Tin từ Hub đến trên luồng mạng -> chuyển về luồng giao diện (EDT) trước khi đụng vào Swing
        hub = new HubConnection(dashboardId,
                msg -> SwingUtilities.invokeLater(() -> handleMessage(msg)),
                () -> SwingUtilities.invokeLater(this::onDisconnected));

        connLabel.setFont(connLabel.getFont().deriveFont(Font.BOLD, 14f));
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        top.add(connLabel, BorderLayout.CENTER);
        top.add(connectBtn, BorderLayout.EAST);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Thiết bị"));

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Nhật ký"));

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(logScroll, BorderLayout.SOUTH);

        connectBtn.addActionListener(e -> connectToHub());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hub.disconnect();
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(780, 540);
        setLocationRelativeTo(null);
        setConnected(false);
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
                break;
            case DEVICE_OFFLINE:
                model.markOffline(msg.getString("deviceId"));
                log("Thiết bị " + msg.getString("deviceId") + " mất kết nối");
                break;
            case ACK:
                log("ACK từ " + msg.getFrom() + ": " + msg.getString("message"));
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

    private void onDisconnected() {
        setConnected(false);
        model.markAllOffline();
        log("Mất kết nối tới Hub");
    }

    private void setConnected(boolean connected) {
        connLabel.setText(connected ? "● Đã kết nối Hub" : "● Chưa kết nối Hub");
        connLabel.setForeground(connected ? new Color(0, 140, 0) : Color.RED);
        connectBtn.setEnabled(!connected);
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