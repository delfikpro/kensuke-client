package ru.cristalix.core.stats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.cristalix.core.GlobalSerializers;

import java.util.Map;
import java.util.UUID;

public interface StatContext {

	Map<String, JsonElement> getDictionary();

	<T> void storeRaw(Scope<T> scope, JsonObject json);

	default <T> void store(Scope<T> scope, T object) {
		if (object == null) return;
		JsonElement jsonElement = GlobalSerializers.toJsonTree(object);
		if (jsonElement instanceof JsonObject)
			this.storeRaw(scope, (JsonObject) jsonElement);
		else
			throw new IllegalArgumentException("Attempted to store primitive/array at scope '" + scope + "', StatService only supports JsonObjects");
	}

	UUID getUuid();

	String getName();

	<T> JsonElement getRawData(Scope<T> scope);

	default <T> T getData(Scope<T> scope) {
		JsonElement json = this.getRawData(scope);
		if (json == null) return null;
		return GlobalSerializers.fromJson(json, scope.getType());
	}

}
