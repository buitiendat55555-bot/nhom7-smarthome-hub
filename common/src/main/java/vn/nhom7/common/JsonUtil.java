package vn.nhom7.common;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/** Chuyển Message <-> một dòng JSON. Gson an toàn khi dùng chung giữa nhiều luồng. */
public final class JsonUtil {

    private static final Gson GSON = new Gson();

    private JsonUtil() {
    }

    /** Kết quả luôn nằm trên một dòng, dùng được với println()/readLine(). */
    public static String toJson(Message message) {
        return GSON.toJson(message);
    }

    /** Ném IllegalArgumentException nếu dòng không phải JSON hợp lệ hoặc sai/thiếu type. */
    public static Message fromJson(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Dong rong");
        }
        try {
            Message m = GSON.fromJson(line, Message.class);
            if (m == null || m.getType() == null) {
                throw new IllegalArgumentException("Thieu hoac sai truong type: " + line);
            }
            return m;
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("JSON sai dinh dang: " + line, e);
        }
    }
}