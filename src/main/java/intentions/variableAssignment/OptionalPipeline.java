package intentions.variableAssignment;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OptionalPipeline<T> {

    private Optional<T> optional;

    private OptionalPipeline(Optional<T> optional) {
        this.optional = optional;
    }

    public static <T> OptionalPipeline<T> of(Optional<T> optional) {
        return new OptionalPipeline<>(optional);
    }

    /**
     * Sets the current Optional to the supplied value only if the current Optional is not present
     * eg: {@code !optional.isPresent()}.
     */
    public OptionalPipeline<T> orElse(Optional<T> nextOptional) {
        if (!optional.isPresent()) {
            optional = nextOptional;
        }
        return this;
    }

    public Optional<T> get() {
        return optional;
    }
}
