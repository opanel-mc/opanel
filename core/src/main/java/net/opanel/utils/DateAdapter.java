package net.opanel.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        if(src == null) {
            return JsonNull.INSTANCE;
        }

        return new JsonPrimitive(dateToString(src));
    }

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if(json == null || json.isJsonNull()) {
            return null;
        }

        try {
            return stringToDate(json.getAsString());
        } catch (DateTimeParseException e) {
            throw new JsonParseException("Invalid date format: " + json.getAsString(), e);
        }
    }

    public static String dateToString(Date date) {
        return FORMATTER.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    public static Date stringToDate(String dateStr) throws DateTimeParseException {
        return Date.from(LocalDateTime.parse(dateStr, FORMATTER).atZone(ZoneId.systemDefault()).toInstant());
    }
}
