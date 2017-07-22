package actions;

/**
 * {@code CreateContext} determines the context in which code generation should occur which determines unique formatting
 * constraints.
 *
 * <p>AssignableObject: Create a new object by typing the Type out and triggering the action.
 *
 * <p>PropertyObject: The property of an object is a non primitive Type which can be auto generated, see Address below.
 *
 * <p>AssignableArray: Same as Assignable object except its an array type: eg Person[]. Can only be triggered
 * when the caret is within the Person text, not the [] section.</p>
 *
 * <p>PropertyArray: Same semantics as Assignable array except within the context of a property field.</p>
 *
 * <p>None: No context, used to signal not to display the action</p>
 *
 * <br>
 * <p>Example flow</p>
 *
 * <pre>
 *     // 1. Invokes AssignableObject context
 *     Person[alt + insert]
 *
 *     // 2. Leads to this generated code.
 *     const person: Person = {
 *         firstName: string;
 *         address: Address; // 3. Repeating step 1 here invokes a PropertyObject creation.
 *     };
 *
 *     // 4. Which expands to create this code.
 *     const person: Person = {
 *         firstName: string;
 *         address: {
 *             street: string,
 *             postcode: number
 *         }
 *     };
 *
 *     Any array creations can only be triggered with the caret on the Type, not within the indexed brackets
 *     Person[]<alt + insert> is invalid.
 * </pre>
 */
public enum CreateContext {
    AssignableObject,
    PropertyObject,
    AssignableArray,
    PropertyArray,
    None
}