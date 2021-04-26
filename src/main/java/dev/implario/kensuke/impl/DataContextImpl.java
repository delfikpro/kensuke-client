package dev.implario.kensuke.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.implario.kensuke.DataContext;
import dev.implario.kensuke.scope.Scope;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class DataContextImpl implements DataContext {

	private final UUID uuid;
	private final String name;
	private final Map<String, JsonElement> dictionary;

	@Override
	public <T> JsonElement getRawData(Scope<T> scope) {
		return dictionary.get(scope.getInternalId());
	}

	@Override
	public <T> void storeRaw(Scope<T> scope, JsonObject json) {
		dictionary.put(scope.getInternalId(), json);
	}

}
