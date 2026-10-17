package vn.nhom7.common;

import com.google.gson.JsonObject;

/** Chương trình kiểm tra nhanh Message và JsonUtil. */
public class Common {

    public static void main(String[] args) {
        JsonObject p = new JsonObject();
        p.addProperty("commandId", "c-001");
        p.addProperty("action", "ON");
        Message cmd = new Message(MessageType.COMMAND, "dashboard1", "light1", p);

        String line = JsonUtil.toJson(cmd);
        System.out.println("Gui di : " + line);

        Message back = JsonUtil.fromJson(line);
        System.out.println("Doc lai: type=" + back.getType()
                + ", to=" + back.getTo() + ", action=" + back.getString("action"));

        try {
            JsonUtil.fromJson("{khong phai json");
        } catch (IllegalArgumentException e) {
            System.out.println("Bat loi dung: " + e.getMessage());
        }
    }
}