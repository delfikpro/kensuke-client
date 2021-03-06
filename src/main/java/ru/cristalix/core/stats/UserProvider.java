package ru.cristalix.core.stats;

@FunctionalInterface
public interface UserProvider<T> {

	T createUser(StatContext snapshot);

}
