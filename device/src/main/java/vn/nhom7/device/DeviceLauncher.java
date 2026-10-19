package vn.nhom7.device;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Toolkit;
import vn.nhom7.common.ui.Ui;

/** Trình khởi chạy: mở nhiều thiết bị (chọn loại + ID) trong cùng một chương trình. */
public class DeviceLauncher extends JFrame {

    private final JComboBox<String> typeBox = new JComboBox<>(DeviceFactory.TYPES);
    private final JTextField idField = new JTextField(12);
    private final JTextField nameField = new JTextField(12);
    private final DefaultListModel<String> openModel = new DefaultListModel<>();
    private final Set<String> openIds = new HashSet<>();
    private int nextOffset = 0;

    public DeviceLauncher() {
        super("Trình khởi chạy thiết bị");

        // Ô chọn hiện tên tiếng Việt thay vì mã LIGHT, CAMERA...
        typeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(DeviceFactory.label((String) value));
                return this;
            }
        });
        typeBox.addActionListener(e -> suggestId());

        JButton openBtn = new Ui.RoundButton("Mở thiết bị", Ui.BLUE);
        openBtn.addActionListener(e -> openFromForm());
        JButton demoBtn = new Ui.RoundButton("Mở bộ demo 5 thiết bị", Ui.ORANGE);
        demoBtn.addActionListener(e -> openDemoSet());

        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thiết bị mới"),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        form.add(new JLabel("Loại:"));
        form.add(typeBox);
        form.add(new JLabel("ID:"));
        form.add(idField);
        form.add(new JLabel("Tên:"));
        form.add(nameField);
        form.add(new JLabel());
        form.add(openBtn);

        JScrollPane listScroll = new JScrollPane(new JList<>(openModel));
        listScroll.setBorder(BorderFactory.createTitledBorder("Đang mở"));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        bottom.add(demoBtn, BorderLayout.CENTER);

        setLayout(new BorderLayout(0, 6));
        add(form, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE); // đóng trình khởi chạy = tắt hết thiết bị
        setSize(400, 440);
        setLocation(30, 60);
        suggestId();
    }

    /** Gợi ý ID chưa dùng: light1, light2, ... */
    private void suggestId() {
        String type = (String) typeBox.getSelectedItem();
        String prefix = DeviceFactory.idPrefix(type);
        int n = 1;
        while (openIds.contains(prefix + n)) {
            n++;
        }
        idField.setText(prefix + n);
        nameField.setText(DeviceFactory.label(type) + " " + n);
    }

    private void openFromForm() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        if (!id.matches("[A-Za-z0-9_-]+")) {
            JOptionPane.showMessageDialog(this, "ID chỉ gồm chữ không dấu, số, dấu - và _");
            return;
        }
        if (openIds.contains(id)) {
            JOptionPane.showMessageDialog(this, "ID " + id + " đang được mở");
            return;
        }
        open((String) typeBox.getSelectedItem(), id, name.isEmpty() ? id : name);
        suggestId();
    }

    /** Mở light1, temp1, door1, cam1, ac1 (bỏ qua cái nào đã mở) và xếp thành hàng cho dễ nhìn. */
    private void openDemoSet() {
        nextOffset = 0;
        for (String type : DeviceFactory.TYPES) {
            String id = DeviceFactory.idPrefix(type) + "1";
            if (!openIds.contains(id)) {
                open(type, id, DeviceFactory.demoName(type));
            }
        }
        suggestId();
    }

    private void open(String type, String id, String name) {
        DeviceFrame frame = DeviceFactory.create(type, id, name);

        // Các cửa sổ thiết bị mặc định EXIT_ON_CLOSE (tắt cả chương trình).
        // Trong trình khởi chạy, đóng một thiết bị chỉ được đóng cửa sổ đó.
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        String entry = id + "  (" + DeviceFactory.label(type) + ")";
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                openIds.remove(id);
                openModel.removeElement(entry);
            }
        });

        // Xếp các cửa sổ thành hàng ngang ở nửa dưới màn hình, hết chỗ thì xếp chồng lệch
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int perRow = Math.max(1, screen.width / frame.getWidth());
        int col = nextOffset % perRow;
        int rowIdx = nextOffset / perRow;
        frame.setLocation(col * frame.getWidth() + rowIdx * 30,
                Math.max(0, screen.height - frame.getHeight() - 50 - rowIdx * 30));
        nextOffset = (nextOffset + 1) % 10;

        openIds.add(id);
        openModel.addElement(entry);
        frame.setVisible(true);
        frame.connectToHub();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            new DeviceLauncher().setVisible(true);
        });
    }
}