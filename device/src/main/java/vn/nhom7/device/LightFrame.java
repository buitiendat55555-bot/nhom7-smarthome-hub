package vn.nhom7.device;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import vn.nhom7.common.ui.Ui;

/** Cửa sổ đèn: nút bật/tắt, thanh độ sáng, 5 màu có sẵn. */
public class LightFrame extends DeviceFrame {

    /** Màu ánh sáng: tên hiện trên nút -> mã màu gửi đi. */
    static final String[][] COLORS = {
        {"Vàng", LightDevice.DEFAULT_COLOR}, {"Trắng", "#E5E7EB"}, {"Xanh", "#60A5FA"},
        {"Hồng", "#F472B6"}, {"Lá", "#4ADE80"}
    };

    private final LightDevice light;
    private final Ui.RoundButton toggleBtn = new Ui.RoundButton("", Ui.ORANGE);
    private final JSlider slider = new JSlider(1, 100, 100);
    private boolean syncing; // đang đồng bộ thanh trượt theo lệnh từ Hub -> không gửi ngược lại

    public LightFrame(LightDevice light) {
        super(light);
        this.light = light;

        toggleBtn.addActionListener(e -> light.toggle());
        slider.setOpaque(false);
        slider.addChangeListener(e -> {
            if (!syncing && !slider.getValueIsAdjusting()) {
                light.setBrightness(slider.getValue()); // chỉ gửi khi thả chuột
            }
        });

        JPanel sliderRow = new JPanel(new BorderLayout(8, 0));
        sliderRow.setOpaque(false);
        sliderRow.add(Ui.label("Độ sáng", Font.PLAIN, 13, Ui.MUTED), BorderLayout.WEST);
        sliderRow.add(slider, BorderLayout.CENTER);

        JPanel swatches = new JPanel(new GridLayout(1, COLORS.length, 6, 0));
        swatches.setOpaque(false);
        for (String[] c : COLORS) {
            Color col = Color.decode(c[1]);
            Ui.RoundButton b = new Ui.RoundButton("", col);
            b.setToolTipText("Màu " + c[0]);
            b.setPreferredSize(new Dimension(40, 28));
            b.addActionListener(e -> light.setColor(c[1]));
            swatches.add(b);
        }
        setControls(column(toggleBtn, sliderRow, swatches));
    }

    @Override
    protected void onStateChanged() {
        if (light == null) {
            return; // gọi từ constructor lớp cha trước khi gán light
        }
        toggleBtn.setText(light.isOn() ? "Tắt đèn" : "Bật đèn");
        toggleBtn.setColor(light.isOn() ? Ui.NAVY : Ui.ORANGE);
        syncing = true;
        slider.setValue(light.getBrightness());
        syncing = false;
    }
}
