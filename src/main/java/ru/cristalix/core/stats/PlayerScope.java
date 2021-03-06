package ru.cristalix.core.stats;

public class PlayerScope<T> extends Scope<T> {

	public PlayerScope(String id, Class<T> type) {
		super(id, type);
	}

	@Override
	public String getInternalId() {
		return "players:" + id;
	}

}
