package dev.implario.kensuke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.UUID;

public interface DataContext {

    Map<String, JsonElement> getDictionary();

    <T> void storeRaw(Scope<T> scope, JsonObject json);

    <T> void store(Scope<T> scope, T object);

    String getId();

    default UUID getUuid() {
        return UUID.fromString(getId());
    }

    <T> JsonElement getRawData(Scope<T> scope);

    <T> T getData(Scope<T> scope);

}
