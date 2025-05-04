/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

import org.jline.reader.Completer;

/**
 * Interface for managing script engine variables, statements, and script execution.
 * <p>
 * The ScriptEngine interface provides methods for interacting with a script engine,
 * such as executing scripts and statements, managing variables, and serializing/deserializing
 * objects. It serves as a bridge between JLine and various scripting languages like
 * JavaScript, Groovy, etc.
 * <p>
 * The ScriptEngine is responsible for:
 * <ul>
 *   <li>Executing scripts and statements in the underlying script engine</li>
 *   <li>Managing variables in the script engine's context</li>
 *   <li>Converting objects between Java and the script engine's native format</li>
 *   <li>Serializing and deserializing objects to/from various formats</li>
 *   <li>Providing completers for script-specific syntax</li>
 * </ul>
 */
public interface ScriptEngine {

    /**
     * Returns the name of the underlying script engine.
     * <p>
     * This method retrieves the name of the script engine implementation being used,
     * such as "JavaScript", "Groovy", etc.
     *
     * @return the name of the script engine
     */
    String getEngineName();

    /**
     * Returns the file name extensions associated with this script engine.
     * <p>
     * This method retrieves the file extensions that are recognized by this script engine,
     * such as ".js" for JavaScript, ".groovy" for Groovy, etc.
     *
     * @return a collection of file name extensions associated with this script engine
     */
    Collection<String> getExtensions();

    /**
     * Returns a completer for script-specific syntax.
     * <p>
     * This method retrieves a completer that can provide completion for script-specific
     * syntax, such as keywords, built-in functions, etc. The completer can be used for
     * tab completion in the console.
     *
     * @return a completer for script-specific syntax
     */
    Completer getScriptCompleter();

    /**
     * Tests if a variable exists in the script engine's context.
     * <p>
     * This method determines whether a variable with the specified name exists
     * in the script engine's context.
     *
     * @param name the name of the variable to check
     * @return true if a variable with the specified name exists, false otherwise
     */
    boolean hasVariable(String name);

    /**
     * Creates or updates a variable in the script engine's context.
     * <p>
     * This method creates a new variable with the specified name and value in the
     * script engine's context, or updates an existing variable if one with the
     * specified name already exists.
     *
     * @param name the name of the variable to create or update
     * @param value the value to assign to the variable
     */
    void put(String name, Object value);

    /**
     * Gets the value of a variable from the script engine's context.
     * <p>
     * This method retrieves the value of the variable with the specified name
     * from the script engine's context.
     *
     * @param name the name of the variable to get
     * @return the value of the variable, or null if no variable with the specified name exists
     */
    Object get(String name);

    /**
     * Gets all variables with their values from the script engine's context.
     * <p>
     * This method retrieves all variables and their values from the script engine's context.
     * This is a convenience method that calls {@link #find(String)} with a null pattern.
     *
     * @return a map of variable names to their values
     */
    default Map<String, Object> find() {
        return find(null);
    }

    /**
     * Gets all variables that match the specified pattern.
     * <p>
     * This method retrieves all variables whose names match the specified pattern
     * from the script engine's context. The pattern can contain wildcard characters (*)
     * to match multiple variable names.
     *
     * @param name the pattern to match variable names against, or null to match all variables
     * @return a map of matching variable names to their values
     */
    Map<String, Object> find(String name);

    /**
     * Deletes variables from the script engine's context.
     * <p>
     * This method removes variables from the script engine's context. The variable names
     * can contain wildcard characters (*) to match multiple variables.
     *
     * @param vars the names of the variables to delete, which can contain wildcard characters
     */
    void del(String... vars);

    /**
     * Serializes an object to a JSON string.
     * <p>
     * This method converts the specified object to a formatted JSON string representation.
     * The exact format of the JSON string depends on the script engine implementation.
     *
     * @param object the object to serialize to JSON
     * @return a formatted JSON string representation of the object
     */
    String toJson(Object object);

    /**
     * Converts an object to its string representation.
     * <p>
     * This method converts the specified object to a string representation.
     * The exact format of the string depends on the script engine implementation.
     *
     * @param object the object to convert to a string
     * @return a string representation of the object
     */
    String toString(Object object);

    /**
     * Converts an object's fields to a map.
     * <p>
     * This method extracts the fields of the specified object and returns them as a map
     * of field names to field values. The exact set of fields included in the map depends
     * on the script engine implementation.
     *
     * @param object the object whose fields to extract
     * @return a map of field names to field values
     */
    Map<String, Object> toMap(Object object);

    /**
     * Deserializes a value from its string representation.
     * <p>
     * This method converts the specified string representation back to an object.
     * This is a convenience method that calls {@link #deserialize(String, String)}
     * with a null format.
     *
     * @param value the string representation to deserialize
     * @return the deserialized object
     */
    default Object deserialize(String value) {
        return deserialize(value, null);
    }

    /**
     * Deserializes a value from its string representation using the specified format.
     * <p>
     * This method converts the specified string representation back to an object
     * using the specified serialization format.
     *
     * @param value the string representation to deserialize
     * @param format the serialization format to use, or null to use the default format
     * @return the deserialized object
     */
    Object deserialize(String value, String format);

    /**
     * Returns the serialization formats supported by this script engine.
     * <p>
     * This method retrieves the names of the serialization formats that can be used
     * with the {@link #persist(Path, Object, String)} method.
     *
     * @return a list of supported serialization format names
     */
    List<String> getSerializationFormats();

    /**
     * Returns the deserialization formats supported by this script engine.
     * <p>
     * This method retrieves the names of the deserialization formats that can be used
     * with the {@link #deserialize(String, String)} method.
     *
     * @return a list of supported deserialization format names
     */
    List<String> getDeserializationFormats();

    /**
     * Persists an object to a file.
     * <p>
     * This method serializes the specified object and writes it to the specified file.
     * This is a convenience method that calls {@link #persist(Path, Object, String)}
     * with a null format.
     *
     * @param file the file to write the serialized object to
     * @param object the object to serialize and persist
     */
    void persist(Path file, Object object);

    /**
     * Persists an object to a file using the specified serialization format.
     * <p>
     * This method serializes the specified object using the specified format
     * and writes it to the specified file.
     *
     * @param file the file to write the serialized object to
     * @param object the object to serialize and persist
     * @param format the serialization format to use, or null to use the default format
     */
    void persist(Path file, Object object, String format);

    /**
     * Executes a script engine statement.
     * <p>
     * This method evaluates the specified statement in the script engine and returns
     * the result of the evaluation.
     *
     * @param statement the statement to execute
     * @return the result of executing the statement
     * @throws Exception if an error occurs during execution
     */
    Object execute(String statement) throws Exception;

    /**
     * Executes a script from a file.
     * <p>
     * This method executes the script contained in the specified file and returns
     * the result of the execution. This is a convenience method that calls
     * {@link #execute(File, Object[])} with null arguments.
     *
     * @param script the file containing the script to execute
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    default Object execute(Path script) throws Exception {
        return execute(script.toFile(), null);
    }

    /**
     * Executes a script from a file.
     * <p>
     * This method executes the script contained in the specified file and returns
     * the result of the execution. This is a convenience method that calls
     * {@link #execute(File, Object[])} with null arguments.
     *
     * @param script the file containing the script to execute
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    default Object execute(File script) throws Exception {
        return execute(script, null);
    }

    /**
     * Executes a script from a file with the specified arguments.
     * <p>
     * This method executes the script contained in the specified file with the specified
     * arguments and returns the result of the execution. This is a convenience method
     * that calls {@link #execute(File, Object[])} with the file converted to a File object.
     *
     * @param script the file containing the script to execute
     * @param args the arguments to pass to the script, or null if no arguments
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    default Object execute(Path script, Object[] args) throws Exception {
        return execute(script.toFile(), args);
    }

    /**
     * Executes a script from a file with the specified arguments.
     * <p>
     * This method executes the script contained in the specified file with the specified
     * arguments and returns the result of the execution.
     *
     * @param script the file containing the script to execute
     * @param args the arguments to pass to the script, or null if no arguments
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    Object execute(File script, Object[] args) throws Exception;

    /**
     * Executes a script engine closure with the specified arguments.
     * <p>
     * This method executes the specified closure (a callable object) with the specified
     * arguments and returns the result of the execution.
     *
     * @param closure the closure to execute
     * @param args the arguments to pass to the closure
     * @return the result of executing the closure
     */
    Object execute(Object closure, Object... args);
}
