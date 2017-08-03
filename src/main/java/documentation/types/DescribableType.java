package documentation.types;

/**
 * Implemented by classes capable of describing their type by providing a type name and {@code TypeDescription}.
 *
 * <p>
 * Created by matt on 05-Jun-17.
 */
public interface DescribableType {

    /**
     * Some examples
     *
     * <ul>
     * <li>{@code TypeScriptObjectProperty} = The full getSignature eg: 'firstName: string'</li>
     * <li>{@code HybridFunctionProperty} = The complete callable function expression - '(start: number): string'</li>
     * <li>{@code UnionOrIntersectionType} = The union or intersection value</li>
     * <li>{@code EnumField} = The enum value</li>
     * <li>{@code SingleType} = The single type value</li>
     * <li>{@code FunctionType} = {@code type Constructor<T = {}> = new (...args: any[]) => T} the function type
     * is the expression on the right {@code (...args: any[]) => T)}</li>
     * </ul>
     *
     * @return The full type value.
     */
    String getType();
}
