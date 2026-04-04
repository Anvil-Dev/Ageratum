package dev.anvilcraft.resource.ageratum.util;

import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public final class ReferenceHolder<T> {
    private @Nullable T value = null;
    private final Supplier<T> valueSupplier;

    private ReferenceHolder(Supplier<T> valueSupplier) {
        this.valueSupplier = valueSupplier;
    }

    public static <T> ReferenceHolder<T> create(Supplier<T> valueSupplier) {
        return new ReferenceHolder<>(valueSupplier);
    }

    public T get() {
        if (value == null) {
            return value = Objects.requireNonNull(valueSupplier.get());
        }
        return value;
    }
}
