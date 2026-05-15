package sevak.hovhannisyan.myproject.data.converter;

import androidx.room.TypeConverter;
import sevak.hovhannisyan.myproject.data.model.ChatMessage;

public class ChatTypeConverter {
    @TypeConverter
    public static ChatMessage.Type fromString(String value) {
        return value == null ? null : ChatMessage.Type.valueOf(value);
    }

    @TypeConverter
    public static String typeToString(ChatMessage.Type type) {
        return type == null ? null : type.name();
    }
}
