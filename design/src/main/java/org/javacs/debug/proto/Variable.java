package org.javacs.debug.proto;

/**
 * A Variable is a name/value pair. Optionally a variable can have a 'type' that is shown if space permits or when
 * hovering over the variable's name. An optional 'kind' is used to render additional properties of the variable, e.g.
 * different icons can be used to indicate that a variable is public or private. If the value is structured (has
 * children), a handle is provided to retrieve the children with the VariablesRequest. If the number of named or indexed
 * children is large, the numbers should be returned via the optional 'namedVariables' and 'indexedVariables'
 * attributes. The client can use this optional information to present the children in a paged UI and fetch them in
 * chunks.
 */
public class Variable {
    /** The variable's name. */
    public String name;
    /** The variable's value. This can be a multi-line text, e.g. for a function the body of a function. */
    public String value;
    /** The type of the variable's value. Typically shown in the UI when hovering over the value. */
    public String type;
    /** Properties of a variable that can be used to determine how to render the variable in the UI. */
    public VariablePresentationHint presentationHint;
    /**
     * Optional evaluatable name of this variable which can be passed to the 'EvaluateRequest' to fetch the variable's
     * value.
     */
    public String evaluateName;
    /**
     * If variablesReference is > 0, the variable is structured and its children can be retrieved by passing
     * variablesReference to the VariablesRequest.
     */
    public long variablesReference;
    /**
     * The number of named child variables. The client can use this optional information to present the children in a
     * paged UI and fetch them in chunks.
     */
    public Integer namedVariables;
    /**
     * The number of indexed child variables. The client can use this optional information to present the children in a
     * paged UI and fetch them in chunks.
     */
    public Integer indexedVariables;
    /**
     * Optional memory reference for the variable if the variable represents executable code, such as a function
     * pointer.
     */
    public String memoryReference;
}
