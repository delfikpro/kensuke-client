package dev.implario.kensuke.scope;

public class ArbitraryScope<T> extends Scope<T> {

	public ArbitraryScope(String id, Class<T> type) {
		super(id, type);
	}

	@Override
	public String getInternalId() {
		return "arbitrary:" + id;
	}

}
