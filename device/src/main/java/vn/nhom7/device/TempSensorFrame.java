package vn.nhom7.device;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Random;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import vn.nhom7.common.ui.Ui;

/** Cửa sổ cảm biến nhiệt: thanh trượt 0-60 °C, 3 nút đặt nhanh và chế độ tự dao động. */
public class TempSensorFrame extends DeviceFrame {

    private final TempSensorDevice sensor;
    private final JSlider slider = new JSlider(0, 600, 280); // đơn vị 0,1 °C
    private final JCheckBox autoBox = new JCheckBox("Tự dao động mỗi 2 giây (như cảm biến thật)");
    private final Random random = new Random();
    private boolean syncing;

    public TempSensorFrame(TempSensorDevice sensor) {
        super(sensor);
        this.sensor = sensor;

        slider.setOpaque(false);
        slider.addChangeListener(e -> {
            if (!syncing && !slider.getValueIsAdjusting()) {
                sensor.setTemperature(slider.getValue() / 10.0); // chỉ gửi khi thả chuột
            }
        });
        JPanel sliderRow = new JPanel(new BorderLayout(8, 0));
        sliderRow.setOpaque(false);
        sliderRow.add(Ui.label("0 °C", Font.PLAIN, 12, Ui.MUTED), BorderLayout.WEST);
        sliderRow.add(slider, BorderLayout.CENTER);
        sliderRow.add(Ui.label("60 °C", Font.PLAIN, 12, Ui.MUTED), BorderLayout.EAST);

        Ui.RoundButton cool = new Ui.RoundButton("Mát 24°", Ui.BLUE);
        Ui.RoundButton warm = new Ui.RoundButton("Ấm 33°", Ui.TEAL);
        Ui.RoundButton fire = new Ui.RoundButton("Nóng 45°", Ui.RED);
        cool.addActionListener(e -> sensor.setTemperature(24));
        warm.addActionListener(e -> sensor.setTemperature(33));
        fire.addActionListener(e -> sensor.setTemperature(45));

        autoBox.setOpaque(false);
        autoBox.setFont(Ui.font(Font.PLAIN, 12));
        new Timer(2000, e -> {
            if (autoBox.isSelected()) {
                sensor.setTemperature(sensor.getTemperature() + (random.nextDouble() - 0.5));
            }
        }).start();

        setControls(column(sliderRow, row(cool, warm, fire), autoBox));
    }

    @Override
    protected void onStateChanged() {
        if (sensor == null) {
            return;
        }
        syncing = true;
        slider.setValue((int) Math.round(sensor.getTemperature() * 10));
        syncing = false;
    }
}
