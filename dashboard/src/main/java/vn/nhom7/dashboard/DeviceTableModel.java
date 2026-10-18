package vn.nhom7.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

/** Dữ liệu bảng thiết bị trên Dashboard. Chỉ được gọi trên luồng giao diện (EDT). */
public class DeviceTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"ID", "Loại", "Tên", "Trạng thái", "Kết nối"};

    private static class Row {
        String id;
        String type;
        String name;
        String state;
        boolean online;
    }

    private final List<Row> rows = new ArrayList<>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row r = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return r.id;
            case 1:
                return r.type;
            case 2:
                return r.name;
            case 3:
                return r.state;
            default:
                return r.online ? "Online" : "Mất kết nối";
        }
    }

    /** DEVICE_LIST: thay toàn bộ bảng. */
    public void setAll(JsonArray devices) {
        rows.clear();
        for (JsonElement e : devices) {
            Row r = new Row();
            fill(r, e.getAsJsonObject());
            rows.add(r);
        }
        fireTableDataChanged();
    }

    /** DEVICE_UPDATE: cập nhật một dòng; thiết bị mới thì thêm dòng. */
    public void update(JsonObject device) {
        int i = indexOf(str(device, "deviceId"));
        if (i < 0) {
            Row r = new Row();
            fill(r, device);
            rows.add(r);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        } else {
            fill(rows.get(i), device);
            fireTableRowsUpdated(i, i);
        }
    }

    /** DEVICE_OFFLINE: giữ dòng nhưng đánh dấu mất kết nối. */
    public void markOffline(String deviceId) {
        int i = indexOf(deviceId);
        if (i >= 0) {
            rows.get(i).online = false;
            fireTableRowsUpdated(i, i);
        }
    }

    /** Dashboard mất kết nối Hub: không còn biết trạng thái thật của thiết bị nào. */
    public void markAllOffline() {
        for (Row r : rows) {
            r.online = false;
        }
        fireTableDataChanged();
    }

    public String getDeviceId(int row) {
        return rows.get(row).id;
    }

    public String getDeviceType(int row) {
        return rows.get(row).type;
    }

    public boolean isOnline(int row) {
        return rows.get(row).online;
    }

    private int indexOf(String id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static void fill(Row r, JsonObject d) {
        r.id = str(d, "deviceId");
        r.type = str(d, "deviceType");
        r.name = str(d, "name");
        JsonObject state = (d.has("state") && d.get("state").isJsonObject())
                ? d.getAsJsonObject("state") : new JsonObject();
        r.state = formatState(state);
        r.online = true;
    }

    /** {"power":"ON"} -> "power = ON". */
    private static String formatState(JsonObject state) {
        if (state.size() == 0) {
            return "(chưa có dữ liệu)";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JsonElement> e : state.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            JsonElement v = e.getValue();
            sb.append(e.getKey()).append(" = ")
              .append(v.isJsonPrimitive() ? v.getAsString() : v.toString());
        }
        return sb.toString();
    }

    private static String str(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : "";
    }
}