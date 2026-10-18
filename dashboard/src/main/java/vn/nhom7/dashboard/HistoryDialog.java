package vn.nhom7.dashboard;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Cửa sổ xem lại toàn bộ nhật ký đã lưu ra file, có ô lọc theo từ khóa. */
public class HistoryDialog extends JDialog {

    private final DashboardLog history;
    private final JTextField filterField = new JTextField(20);
    private final JTextArea area = new JTextArea();
    private final JLabel countLabel = new JLabel();
    private List<String> lines = List.of();

    public HistoryDialog(JFrame owner, DashboardLog history) {
        super(owner, "Lịch sử log - " + history.getFile(), false);
        this.history = history;

        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JButton reloadBtn = new JButton("Tải lại");
        reloadBtn.addActionListener(e -> reload());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Lọc:"));
        top.add(filterField);
        top.add(reloadBtn);
        top.add(countLabel);

        // Gõ tới đâu lọc tới đó
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        setSize(760, 460);
        setLocationRelativeTo(owner);
        reload();
    }

    private void reload() {
        try {
            lines = history.readAll();
        } catch (IOException e) {
            lines = List.of("Không đọc được file: " + e.getMessage());
        }
        applyFilter();
    }

    /** Chỉ hiện các dòng chứa từ khóa (không phân biệt hoa thường). */
    private void applyFilter() {
        String key = filterField.getText().trim().toLowerCase();
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String line : lines) {
            if (key.isEmpty() || line.toLowerCase().contains(key)) {
                sb.append(line).append('\n');
                shown++;
            }
        }
        area.setText(sb.toString());
        area.setCaretPosition(area.getDocument().getLength());
        countLabel.setText(shown + "/" + lines.size() + " dòng");
    }
}