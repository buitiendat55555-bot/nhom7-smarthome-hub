package vn.nhom7.device;

import vn.nhom7.common.ui.Ui;

/** Cửa sổ máy lạnh: bật/tắt, giảm/tăng nhiệt độ cài đặt. */
public class AirConditionerFrame extends DeviceFrame {

    private final AirConditionerDevice ac;
    private final Ui.RoundButton toggleBtn = new Ui.RoundButton("", Ui.BLUE);

    public AirConditionerFrame(AirConditionerDevice ac) {
        super(ac);
        this.ac = ac;
        toggleBtn.addActionListener(e -> ac.toggle());
        Ui.RoundButton down = new Ui.RoundButton("−  Giảm 1°", Ui.TEAL);
        Ui.RoundButton up = new Ui.RoundButton("+  Tăng 1°", Ui.ORANGE);
        down.addActionListener(e -> ac.adjust(-1));
        up.addActionListener(e -> ac.adjust(1));
        setControls(column(toggleBtn, row(down, up)));
    }

    @Override
    protected void onStateChanged() {
        if (ac == null) {
            return;
        }
        toggleBtn.setText(ac.isOn() ? "Tắt máy lạnh" : "Bật máy lạnh");
        toggleBtn.setColor(ac.isOn() ? Ui.NAVY : Ui.BLUE);
    }
}
