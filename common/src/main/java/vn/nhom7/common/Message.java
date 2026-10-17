package vn.nhom7.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Một message trao đổi qua TCP. Cấu trúc theo mục 2 của docs/PROTOCOL.md. */
public class Message {

    private MessageType type;
    private String from;
    private String to;
    private long timestamp;
    private JsonObject payload;

    /** Constructor rỗng để Gson tạo đối tượng khi đọc JSON. */
    public Message() {
    }

    public Message(MessageType type, String from, String to, JsonObject payload) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.timestamp = System.currentTimeMillis();
        this.payload = (payload != null) ? payload : new JsonObject();
    }

    public MessageType getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JsonObject getPayload() {
        return (payload != null) ? payload : new JsonObject();
    }

    /** Lấy một trường chuỗi trong payload, trả về null nếu không có. */
    public String getString(String key) {
        JsonElement e = getPayload().get(key);
        return (e == null || e.isJsonNull()) ? null : e.getAsString();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement e = getPayload().get(key);
        return (e == null || e.isJsonNull()) ? defaultValue : e.getAsBoolean();
    }

    public double getDouble(String key, double defaultValue) {
        JsonElement e = getPayload().get(key);
        return (e == null || e.isJsonNull()) ? defaultValue : e.getAsDouble();
    }
}