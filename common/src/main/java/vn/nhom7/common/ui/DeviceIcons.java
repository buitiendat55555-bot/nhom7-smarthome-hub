package vn.nhom7.common.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Vẽ hình minh họa thiết bị theo trạng thái (đèn sáng/tắt, nhiệt kế, cửa, camera, máy lạnh).
 * Dashboard dùng cỡ nhỏ trên thẻ, cửa sổ thiết bị dùng cỡ lớn. phase tăng dần để tạo chuyển động.
 */
public final class DeviceIcons {

    public static final Color WARM = new Color(0xFFC83D);

    private DeviceIcons() {
    }

    public static void paint(Graphics2D g, String type, JsonObject state, boolean online,
            int x, int y, int size, int phase) {
        Ui.smooth(g);
        JsonObject s = (state != null) ? state : new JsonObject();
        if (type == null) {
            type = "";
        }
        switch (type) {
            case "LIGHT":
                light(g, s, x, y, size);
                break;
            case "TEMP_SENSOR":
                thermometer(g, s, x, y, size);
                break;
            case "DOOR_LOCK":
                door(g, s, x, y, size, phase);
                break;
            case "CAMERA":
                camera(g, s, x, y, size, phase);
                break;
            case "AIR_CONDITIONER":
                aircon(g, s, x, y, size, phase);
                break;
            default:
                g.setColor(Ui.GREY);
                g.fillOval(x + size / 4, y + size / 4, size / 2, size / 2);
        }
        if (!online) {
            // phủ lớp mờ khi mất kết nối
            g.setColor(new Color(255, 255, 255, 170));
            g.fillRect(x - 4, y - 4, size + 8, size + 8);
        }
    }

    // ---------- Đèn ----------
    private static void light(Graphics2D g, JsonObject s, int x, int y, int size) {
        boolean on = "ON".equals(str(s, "power"));
        int brightness = (int) num(s, "brightness", 100);
        Color color = Ui.parseColor(str(s, "color"), WARM);
        double cx = x + size / 2.0;
        double cy = y + size * 0.40;
        double r = size * 0.27;

        if (on && brightness > 0) {
            float glowR = (float) (r * (1.6 + brightness / 100.0));
            int a = 60 + brightness * 150 / 100;
            g.setPaint(new RadialGradientPaint((float) cx, (float) cy, glowR,
                    new float[]{0f, 0.45f, 1f},
                    new Color[]{Ui.alpha(color, a), Ui.alpha(color, a / 3), Ui.alpha(color, 0)}));
            g.fill(new Ellipse2D.Double(cx - glowR, cy - glowR, glowR * 2, glowR * 2));
        }
        Color glass = on ? Ui.mix(Color.WHITE, color, 0.35 + brightness / 160.0) : new Color(0xE5E7EB);
        g.setColor(glass);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        int[] nx = {(int) (cx - r * 0.55), (int) (cx + r * 0.55), (int) (cx + r * 0.38), (int) (cx - r * 0.38)};
        int[] ny = {(int) (cy + r * 0.8), (int) (cy + r * 0.8), (int) (cy + r * 1.35), (int) (cy + r * 1.35)};
        g.fillPolygon(nx, ny, 4);
        g.setStroke(new BasicStroke(Math.max(1.5f, size / 60f)));
        g.setColor(on ? Ui.mix(color, Color.BLACK, 0.35) : Ui.GREY);
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        // dây tóc
        g.drawLine((int) (cx - r * 0.3), (int) (cy + r * 0.2), (int) cx, (int) (cy - r * 0.2));
        g.drawLine((int) cx, (int) (cy - r * 0.2), (int) (cx + r * 0.3), (int) (cy + r * 0.2));
        // đuôi đèn
        g.setColor(new Color(0x6B7280));
        int bw = (int) (r * 0.8);
        for (int i = 0; i < 3; i++) {
            g.fillRoundRect((int) (cx - bw / 2.0), (int) (cy + r * 1.38 + i * r * 0.22), bw, (int) (r * 0.16), 4, 4);
        }
    }

    // ---------- Nhiệt kế ----------
    private static void thermometer(Graphics2D g, JsonObject s, int x, int y, int size) {
        double t = num(s, "temperature", Double.NaN);
        double cx = x + size / 2.0;
        double tubeW = size * 0.16;
        double top = y + size * 0.06;
        double bulbR = size * 0.15;
        double bulbCy = y + size * 0.80;
        double tubeBottom = bulbCy - bulbR * 0.6;
        double ratio = Double.isNaN(t) ? 0 : Math.max(0, Math.min(1, t / 60.0));
        Color fill = colorForTemp(t);

        g.setColor(new Color(0xEEF2F7));
        g.fill(new RoundRectangle2D.Double(cx - tubeW / 2, top, tubeW, tubeBottom - top + tubeW, tubeW, tubeW));
        g.fill(new Ellipse2D.Double(cx - bulbR, bulbCy - bulbR, bulbR * 2, bulbR * 2));

        double inner = tubeW * 0.5;
        double fillTop = tubeBottom - (tubeBottom - top - inner) * ratio;
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(cx - inner / 2, fillTop, inner, bulbCy - fillTop, inner, inner));
        g.fill(new Ellipse2D.Double(cx - bulbR * 0.72, bulbCy - bulbR * 0.72, bulbR * 1.44, bulbR * 1.44));

        g.setStroke(new BasicStroke(Math.max(1.5f, size / 70f)));
        g.setColor(new Color(0x94A3B8));
        g.draw(new RoundRectangle2D.Double(cx - tubeW / 2, top, tubeW, tubeBottom - top + tubeW, tubeW, tubeW));
        // vạch chia 0..60 và vạch ngưỡng 40 °C
        g.setFont(Ui.font(Font.PLAIN, Math.max(9, size / 14f)));
        for (int v = 0; v <= 60; v += 20) {
            double ty = tubeBottom - (tubeBottom - top - inner) * (v / 60.0);
            g.setColor(new Color(0x94A3B8));
            g.drawLine((int) (cx + tubeW / 2 + 3), (int) ty, (int) (cx + tubeW / 2 + size * 0.07), (int) ty);
            if (size >= 90) {
                g.drawString(String.valueOf(v), (int) (cx + tubeW / 2 + size * 0.09), (int) ty + 4);
            }
        }
        double ty40 = tubeBottom - (tubeBottom - top - inner) * (40 / 60.0);
        g.setColor(Ui.RED);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
        g.drawLine((int) (cx - tubeW / 2 - size * 0.12), (int) ty40, (int) (cx - tubeW / 2 - 2), (int) ty40);
        if (size >= 90) {
            g.drawString("40", (int) (cx - tubeW / 2 - size * 0.26), (int) ty40 + 4);
        }
    }

    /** Xanh (lạnh) -> cam -> đỏ (nóng). */
    public static Color colorForTemp(double t) {
        if (Double.isNaN(t)) {
            return Ui.GREY;
        }
        if (t <= 30) {
            return Ui.mix(new Color(0x3B82F6), new Color(0x10B981), (t - 15) / 15.0);
        }
        if (t <= 40) {
            return Ui.mix(new Color(0x10B981), new Color(0xF59E0B), (t - 30) / 10.0);
        }
        return Ui.mix(new Color(0xF97316), new Color(0xDC2626), (t - 40) / 15.0);
    }

    // ---------- Cửa ----------
    private static void door(Graphics2D g, JsonObject s, int x, int y, int size, int phase) {
        boolean locked = bool(s, "locked");
        boolean ringing = bool(s, "ringing");
        int fx = x + (int) (size * 0.22);
        int fy = y + (int) (size * 0.08);
        int fw = (int) (size * 0.50);
        int fh = (int) (size * 0.84);

        g.setColor(new Color(0x374151)); // khung cửa
        g.fillRect(fx - 4, fy - 4, fw + 8, fh + 4);
        g.setColor(new Color(0xFDF6E3)); // phần trong nhà nhìn qua khe cửa
        g.fillRect(fx, fy, fw, fh);

        Color wood = new Color(0x9A5B2E);
        if (locked) {
            g.setColor(wood);
            g.fillRect(fx, fy, fw, fh);
            g.setColor(Ui.mix(wood, Color.BLACK, 0.2));
            g.drawRect(fx + fw / 8, fy + fh / 10, fw * 3 / 4, fh * 3 / 10);
            g.drawRect(fx + fw / 8, fy + fh / 2, fw * 3 / 4, fh * 4 / 10);
        } else {
            // cửa hé mở: tấm cửa xoay về bên trái, nhìn thành hình thang
            int open = (int) (fw * 0.55);
            Polygon p = new Polygon(new int[]{fx, fx + open, fx + open, fx},
                    new int[]{fy, fy + fh / 14, fy + fh - fh / 14, fy + fh}, 4);
            g.setColor(Ui.mix(wood, Color.WHITE, 0.15));
            g.fillPolygon(p);
        }
        // tay nắm
        g.setColor(new Color(0xFCD34D));
        int hx = locked ? fx + fw - fw / 6 : fx + (int) (fw * 0.55) - fw / 9;
        g.fillOval(hx - size / 40, fy + fh / 2, Math.max(4, size / 20), Math.max(4, size / 20));

        // ổ khóa
        int lr = (int) (size * 0.13);
        int lx = x + (int) (size * 0.78);
        int ly = y + (int) (size * 0.62);
        Color lc = locked ? Ui.GREEN : Ui.ORANGE;
        g.setStroke(new BasicStroke(Math.max(2f, size / 30f)));
        g.setColor(lc);
        int shackleLift = locked ? 0 : lr / 2;
        g.draw(new Arc2D.Double(lx - lr * 0.6, ly - lr * 1.3 - shackleLift, lr * 1.2, lr * 1.4, 0, 180, Arc2D.OPEN));
        if (locked) {
            g.drawLine((int) (lx - lr * 0.6), (int) (ly - lr * 0.6), (int) (lx - lr * 0.6), (int) (ly - lr * 0.2));
        }
        g.drawLine((int) (lx + lr * 0.6), (int) (ly - lr * 0.6 - shackleLift), (int) (lx + lr * 0.6), (int) (ly - lr * 0.2));
        g.fillRoundRect(lx - lr, (int) (ly - lr * 0.3), lr * 2, (int) (lr * 1.6), lr / 2, lr / 2);
        g.setColor(Color.WHITE);
        g.fillOval(lx - lr / 5, (int) (ly + lr * 0.25), lr * 2 / 5, lr * 2 / 5);

        if (ringing) {
            // sóng âm của chuông
            g.setColor(Ui.alpha(Ui.AMBER, 220));
            int bx = x + (int) (size * 0.17);
            int by = y + (int) (size * 0.35);
            for (int i = 0; i < 3; i++) {
                int rr = size / 20 + i * size / 22 + (phase % 3) * 2;
                g.draw(new Arc2D.Double(bx - rr, by - rr, rr * 2, rr * 2, 120, 120, Arc2D.OPEN));
            }
        }
    }

    // ---------- Camera ----------
    private static void camera(Graphics2D g, JsonObject s, int x, int y, int size, int phase) {
        boolean recording = bool(s, "recording");
        boolean motion = bool(s, "motion");
        int bx = x + size / 8;
        int by = y + size * 3 / 10;
        int bw = size * 3 / 5;
        int bh = size * 2 / 5;

        if (motion) {
            g.setColor(Ui.alpha(Ui.AMBER, 70));
            g.fillOval(x, y + size / 8, size, size * 3 / 4);
        }
        g.setColor(new Color(0x1F2937));
        g.fillRoundRect(bx, by, bw, bh, size / 8, size / 8);
        int[] px = {bx + bw, bx + bw + size / 5, bx + bw + size / 5, bx + bw};
        int[] py = {by + bh / 4, by + bh / 10, by + bh - bh / 10, by + bh * 3 / 4};
        g.fillPolygon(px, py, 4);
        int lr = bh / 3;
        g.setColor(new Color(0x60A5FA));
        g.fillOval(bx + bw / 2 - lr, by + bh / 2 - lr, lr * 2, lr * 2);
        g.setColor(new Color(0x1E3A8A));
        g.fillOval(bx + bw / 2 - lr / 2, by + bh / 2 - lr / 2, lr, lr);

        // chấm đỏ REC nhấp nháy
        if (recording && phase % 2 == 0) {
            g.setColor(new Color(0xEF4444));
            int d = Math.max(6, size / 9);
            g.fillOval(bx + d / 2, by + d / 2, d, d);
        }
        if (recording && size >= 90) {
            g.setColor(new Color(0xEF4444));
            g.setFont(Ui.font(Font.BOLD, size / 10f));
            g.drawString("REC", bx, by - size / 20);
        }
        if (motion) {
            // hình người đi qua
            g.setColor(new Color(0xB45309));
            int hx = x + size * 4 / 5;
            int hy = y + size * 3 / 4;
            int hr = Math.max(4, size / 14);
            g.fillOval(hx - hr, hy - hr * 5, hr * 2, hr * 2);
            g.fillRoundRect(hx - hr, hy - hr * 3, hr * 2, hr * 3, hr, hr);
        }
    }

    // ---------- Máy lạnh ----------
    private static void aircon(Graphics2D g, JsonObject s, int x, int y, int size, int phase) {
        boolean on = "ON".equals(str(s, "power"));
        int target = (int) num(s, "targetTemp", 24);
        int ux = x + size / 14;
        int uy = y + size / 6;
        int uw = size - size / 7;
        int uh = size * 3 / 10;

        g.setColor(Color.WHITE);
        g.fillRoundRect(ux, uy, uw, uh, size / 8, size / 8);
        g.setStroke(new BasicStroke(Math.max(1.5f, size / 70f)));
        g.setColor(new Color(0x94A3B8));
        g.drawRoundRect(ux, uy, uw, uh, size / 8, size / 8);
        g.drawLine(ux + uw / 10, uy + uh * 3 / 4, ux + uw - uw / 10, uy + uh * 3 / 4);

        // màn hình nhiệt độ
        g.setColor(on ? new Color(0x0EA5E9) : new Color(0xCBD5E1));
        g.setFont(Ui.font(Font.BOLD, Math.max(10, size / 8f)));
        g.drawString(on ? target + "°" : "--", ux + uw - uw / 3, uy + uh / 2 + size / 30);
        g.setColor(on ? Ui.GREEN : new Color(0xCBD5E1));
        g.fillOval(ux + uw / 10, uy + uh / 3, Math.max(4, size / 22), Math.max(4, size / 22));

        if (on) {
            // các luồng gió lạnh chạy xuống
            g.setStroke(new BasicStroke(Math.max(2f, size / 40f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                int ax = ux + uw / 6 + i * uw / 5;
                int shift = (phase * size / 24 + i * size / 10) % (size / 2);
                int ay = uy + uh + size / 20 + shift;
                g.setColor(Ui.alpha(new Color(0x38BDF8), 230 - shift * 400 / size));
                g.drawLine(ax, ay, ax - size / 25, ay + size / 9);
            }
        }
    }

    // ---------- Đọc trạng thái ----------
    public static String str(JsonObject o, String key) {
        JsonElement e = (o != null) ? o.get(key) : null;
        return (e == null || e.isJsonNull()) ? null : e.getAsString();
    }

    public static boolean bool(JsonObject o, String key) {
        JsonElement e = (o != null) ? o.get(key) : null;
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }

    public static double num(JsonObject o, String key, double def) {
        JsonElement e = (o != null) ? o.get(key) : null;
        return (e == null || e.isJsonNull()) ? def : e.getAsDouble();
    }
}
