package codeInsight.instructions;

import actions.CreateContext;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Sort of acts like a monadic container similar to Optional. Specialised methods such as {@code map} and
 * {@code orElseTry} are used to simplify logic where the implementation slightly differs from {@code Optional}.
 */
public class InsertInstruction {
    // Flag to determine if a type of CreateContext is legal.
    private static final int ILLEGAL_INSERT_OFFSET = -1;

    private CreateContext createContext;
    private int offset;
    private boolean isLastProperty;

    protected InsertInstruction(CreateContext createContext, int offset, boolean isLastProperty) {
        this.createContext = createContext;
        this.offset = offset;
        this.isLastProperty = isLastProperty;
    }

    /**
     * @param createContext  The {@code CreateContext}.
     * @param offset         The best offset to insert code at.
     * @param isLastProperty {@code true} if the code to be generated is the last property in the interface.
     *                       if {@code true} then no trailing comma is inserted to prepare for the next property
     *                       (Except if dangling commas are allowed). This field is only for Property based code
     *                       generation and is {@code true} for all other contexts.
     */
    public static InsertInstruction of(CreateContext createContext, int offset, boolean isLastProperty) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non negative");
        }
        return new InsertInstruction(createContext, offset, isLastProperty);
    }

    /**
     * Creates an {@code InsertInstruction} with the {@code isLastProperty} set to {@code true}.
     *
     * @param createContext  The {@code CreateContext}.
     * @param offset         The best offset to insert code at.
     */
    public static InsertInstruction of(CreateContext createContext, int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non negative");
        }
        return new InsertInstruction(createContext, offset, true);
    }

    public static InsertInstruction none() {
        return new InsertInstruction(CreateContext.None, ILLEGAL_INSERT_OFFSET, true);
    }

    /**
     * If 'this' is valid simply return 'this', otherwise return the result of calling the {@code Supplier} function.
     *
     * @param f The function to call if 'this' is invalid according to {@code isValid}
     * @return {@code InsertInstruction}
     */
    public InsertInstruction orElseTry(Supplier<InsertInstruction> f) {
        return isValid() ? this : f.get();
    }

    /**
     * If 'this' is valid return the result of calling the mapping function, otherwise return 'this'.
     *
     * @param mapper The function to call if 'this' is valid according to {@code isValid}
     * @return {@code InsertInstruction}
     */
    public InsertInstruction map(Function<InsertInstruction, InsertInstruction> mapper) {
        return isValid() ? mapper.apply(this) : this;
    }

    public boolean isValid() {
        return createContext != CreateContext.None && offset != ILLEGAL_INSERT_OFFSET;
    }

    public CreateContext getCreateContext() {
        return createContext;
    }

    /**
     *
     * @return The best offset position to insert the generated code at.
     */
    public int getOffset() {
        return offset;
    }

    /**
     *
     * @return {@code true} if there are no more properties following the PsiElement triggered in the action.
     */
    public boolean isLastProperty() {
        return isLastProperty;
    }
}