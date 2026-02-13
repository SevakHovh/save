package sevak.hovhannisyan.myproject.data.converter;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Type converter for Room database to convert Date objects to Long and vice versa.
 */
public class DateConverter {
    
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
