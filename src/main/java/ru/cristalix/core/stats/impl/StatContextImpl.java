package ru.cristalix.core.stats.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import ru.cristalix.core.stats.Scope;
import ru.cristalix.core.stats.StatContext;

import java.util.Map;
import java.util.UUID;

@Data
public class StatContextImpl implements StatContext {

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
