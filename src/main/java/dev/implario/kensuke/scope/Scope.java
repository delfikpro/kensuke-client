package dev.implario.kensuke.scope;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public abstract class Scope<T> {

	public static final Pattern PATTERN = Pattern.compile("^[a-zA-Z_-]+$");

	protected final String id;
	protected final Class<T> type;

	protected Scope(String id, Class<T> type) {
		if (!PATTERN.matcher(id).matches()) {
			throw new IllegalArgumentException("Malformed scope id: " + id);
		}
		this.id = id;
		this.type = type;
	}

	public abstract String getInternalId();

}
