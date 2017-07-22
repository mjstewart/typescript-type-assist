package codeInsight.instructions;

import actions.CreateContext;

/**
 * Represents the best position to insert an auto generated array.
 *
 * Created by matt on 29-May-17.
 */
public class ArrayInsertInstruction extends InsertInstruction {
    private long size;

    private ArrayInsertInstruction(CreateContext createContext, int offset, boolean isLastProperty, long size) {
        super(createContext, offset, isLastProperty);
        this.size = size;
    }

    public static ArrayInsertInstruction of(CreateContext createContext, int offset, boolean isLastProperty, long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Array size must be greater than 0");
        }
        return new ArrayInsertInstruction(createContext, offset, isLastProperty, size);
    }

    public long getSize() {
        return size;
    }
}
