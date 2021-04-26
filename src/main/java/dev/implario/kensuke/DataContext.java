package dev.implario.kensuke;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.implario.kensuke.scope.Scope;
import ru.cristalix.core.GlobalSerializers;

import java.util.Map;
import java.util.UUID;

public interface DataContext {

	Map<String, JsonElement> getDictionary();

	<T> void storeRaw(Scope<T> scope, JsonObject json);

	default <T> void store(Scope<T> scope, T object) {
		if (object == null) return;
		JsonElement jsonElement = GlobalSerializers.toJsonTree(object);
		if (jsonElement instanceof JsonObject)
			this.storeRaw(scope, (JsonObject) jsonElement);
		else
			throw new IllegalArgumentException("Attempted to store primitive/array at scope '" + scope + "', Kensuke only supports JsonObjects");
	}

	UUID getUuid();

	String getName();

	<T> JsonElement getRawData(Scope<T> scope);

	default <T> T getData(Scope<T> scope) {
		JsonElement json = getRawData(scope);
		if (json == null) {
			return null;
		}
		return GlobalSerializers.fromJson(json, scope.getType());
	}

}
