package ru.cristalix.core.stats;

@FunctionalInterface
public interface UserSerializer<T> {

	void serialize(T user, StatContext context);

}
