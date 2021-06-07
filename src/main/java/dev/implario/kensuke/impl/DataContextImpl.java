package dev.implario.kensuke.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.implario.kensuke.DataContext;
import dev.implario.kensuke.Scope;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class DataContextImpl implements DataContext {

    private Gson gson;
    private final String id;
    private final Map<String, JsonElement> dictionary;

    @Override
    public <T> T getData(Scope<T> scope) {
        JsonElement json = getRawData(scope);
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, scope.getType());
    }

    @Override
    public <T> JsonElement getRawData(Scope<T> scope) {
        return dictionary.get(scope.getId());
    }

    @Override
    public <T> void store(Scope<T> scope, T object) {
        if (object == null) return;
        JsonElement jsonElement = gson.toJsonTree(object);
        if (jsonElement instanceof JsonObject)
            this.storeRaw(scope, (JsonObject) jsonElement);
        else
            throw new IllegalArgumentException("Attempted to store primitive/array at scope '" + scope + "', Kensuke only supports JsonObjects");
    }

    @Override
    public <T> void storeRaw(Scope<T> scope, JsonObject json) {
        dictionary.put(scope.getId(), json);
    }

}
