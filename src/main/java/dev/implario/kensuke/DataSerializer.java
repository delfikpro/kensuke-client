package dev.implario.kensuke;

@FunctionalInterface
public interface DataSerializer<T> {

	void serialize(T data, DataContext context);

}
