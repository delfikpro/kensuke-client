package dev.implario.kensuke;

@FunctionalInterface
public interface DataDeserializer<T> {

	T createUser(DataContext snapshot);

}
