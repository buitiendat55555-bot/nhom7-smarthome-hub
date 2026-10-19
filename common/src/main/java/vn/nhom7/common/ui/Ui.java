package vn.nhom7.common.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/** Màu, phông chữ và vài thành phần giao diện dùng chung cho Dashboard và cửa sổ thiết bị. */
public final class Ui {

    public static final Color NAVY = new Color(0x14213D);
    public static final Color BG = new Color(0xF3F4F6);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E5EA);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color ORANGE = new Color(0xC2610F);
    public static final Color BLUE = new Color(0x2B579A);
    public static final Color GREEN = new Color(0x2E7D32);
    public static final Color RED = new Color(0xC62828);
    public static final Color AMBER = new Color(0xF59E0B);
    public static final Color TEAL = new Color(0x0F766E);
    public static final Color GREY = new Color(0x9CA3AF);

    private static final String FAMILY = pickFamily();

    private Ui() {
    }

    /** Phông có dấu tiếng Việt: Windows dùng Segoe UI, máy khác dùng phông mặc định. */
    private static String pickFamily() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "Segoe UI" : Font.DIALOG;
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    /** Gọi một lần ở đầu main(): dùng giao diện Nimbus và đặt phông mặc định. */
    public static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.put("nimbusBase", NAVY);
                    UIManager.put("nimbusFocus", BLUE);
                    UIManager.put("nimbusSelectionBackground", BLUE);
                    UIManager.put("control", BG);
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // không đổi được thì giữ giao diện mặc định
        }
        Font f = font(Font.PLAIN, 13);
        for (String key : new String[]{"defaultFont", "Label.font", "Button.font", "TabbedPane.font",
                "Table.font", "TableHeader.font", "TextArea.font", "TextField.font", "ComboBox.font",
                "CheckBox.font", "List.font", "Slider.font", "ToolTip.font"}) {
            UIManager.put(key, f);
        }
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** Hàm vẽ chữ nằm giữa một điểm (dùng trong các hàm paint). */
    public static void centerText(Graphics2D g, String s, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    /** "#FFD54F" -> Color; chuỗi sai thì trả về màu mặc định. */
    public static Color parseColor(String hex, Color fallback) {
        try {
            return (hex == null) ? fallback : Color.decode(hex);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Nút phẳng bo góc, đổi màu khi rê chuột, xám khi bị khóa. */
    public static class RoundButton extends JButton {

        private Color color;
        private Color textColor = Color.WHITE;
        private boolean hover;

        public RoundButton(String text, Color color) {
            super(text);
            this.color = color;
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        public void setColor(Color c) {
            this.color = c;
            repaint();
        }

        public void setTextColor(Color c) {
            this.textColor = c;
            setForeground(isEnabled() ? c : MUTED);
        }

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            setForeground(b ? textColor : MUTED);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            Color c = !isEnabled() ? new Color(0xD1D5DB)
                    : getModel().isPressed() ? color.darker()
                    : hover ? mix(color, Color.BLACK, 0.12) : color;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Nút viền (nền trắng) cho thao tác phụ. */
    public static class GhostButton extends RoundButton {

        public GhostButton(String text) {
            super(text, Color.WHITE);
            setFont(font(Font.PLAIN, 13));
            setTextColor(TEXT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(getModel().isRollover() || getModel().isPressed() ? new Color(0xEEF2F7) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            paintLabelOnly(g);
        }

        private void paintLabelOnly(Graphics g) {
            // gọi phần vẽ chữ của JButton nhưng bỏ phần nền màu của RoundButton
            javax.swing.plaf.ButtonUI ui = getUI();
            if (ui != null) {
                ui.paint(g, this);
            }
        }
    }

    /** Ô nền bo góc (thẻ). */
    public static class RoundPanel extends JPanel {

        private Color fill;
        private Color stroke;
        private final int arc;

        public RoundPanel(Color fill, Color stroke, int arc) {
            this.fill = fill;
            this.stroke = stroke;
            this.arc = arc;
            setOpaque(false);
        }

        public void setColors(Color fill, Color stroke) {
            this.fill = fill;
            this.stroke = stroke;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            if (stroke != null) {
                g2.setStroke(new BasicStroke(stroke == BORDER ? 1f : 2f));
                g2.setColor(stroke);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Nhãn dạng viên thuốc: "● Online", "Đang ghi"... */
    public static class Pill extends JLabel {

        private Color bg;

        public Pill(String text, Color bg, Color fg) {
            super(text, SwingConstants.CENTER);
            this.bg = bg;
            setForeground(fg);
            setFont(font(Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        }

        public void set(String text, Color bg, Color fg) {
            setText(text);
            this.bg = bg;
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 4, Math.max(d.height, 24));
        }
    }
}
