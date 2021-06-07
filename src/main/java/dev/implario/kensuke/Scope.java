package dev.implario.kensuke;

import lombok.Data;

import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

@Data
public class Scope<T> {

    public static final Pattern PATTERN = Pattern.compile("^[a-zA-Z_-]+$");

    protected final String id;
    protected final Class<T> type;
    private Function<UUID, T> defaultDataSupplier;

    public Scope(String id, Class<T> type) {
        this(id, type, null);
    }

    public Scope(String id, Class<T> type, Function<UUID, T> defaultDataSupplier) {
        if (!PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Malformed scope id: " + id);
        }
        this.id = id;
        this.type = type;
    }

}
