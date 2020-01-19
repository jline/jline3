/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.io.File;
import java.util.*;

import org.jline.utils.AttributedString;

/**
 * Manage scriptEngine variables, statements and script execution.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface ScriptEngine {

    /**
     *
     * @return scriptEngine name
     */
    String getEngineName();

    /**
     *
     * @return script file name extensions
     */
    Collection<String> getExtensions();

    /**
     * Tests if console variable exists
     * @param name
     * @return true if variable exists
     */
    boolean hasVariable(String name);

    /**
     * Creates variable
     * @param name of the variable
     * @param value of the variable
     */
    void put(String name, Object value);

    /**
     * Gets variable value
     * @param name of the variable
     * @return value of the variable
     */
    Object get(String name);

    /**
     * Gets all variables with values
     * @return map of the variables
     */
    default Map<String,Object> find() {
        return find(null);
    }

    /**
     * Gets all the variables that match the name. Name can contain * wild cards.
     * @param name of the variable
     * @return map of the variables
     */
    Map<String,Object> find(String name);

    /**
     * Deletes variables. Variable name cab contain * wild cards.
     * @param vars
     */
    void del(String... vars);

    /**
     * Prepares formatted string of the object.
     * @param options for string formatting
     * @param object to display
     * @return formatted string
     */
    String format(Map<String, Object> options, Object object);

    /**
     * Prepares highlighted string of the object
     * @param options for highlighting
     * @param object to highlight
     * @return highlighted string
     */
    List<AttributedString> highlight(Map<String, Object> options, Object object);

    /**
     * Substitute variable reference with its value.
     * @param variable
     * @return Substituted variable
     * @throws Exception
     */
    default Object expandParameter(String variable) {
        return expandParameter(variable, "");
    }

    /**
     * Substitute variable reference with its value.
     * @param variable
     * @param format serialization format
     * @return Substituted variable
     * @throws Exception
     */
    Object expandParameter(String variable, String format);
    /**
     * Executes scriptEngine statement
     * @param statement
     * @return result
     * @throws Exception
     */
    Object execute(String statement) throws Exception;

    /**
     * Executes scriptEngine script
     * @param script
     * @return result
     * @throws Exception
     */
    default Object execute(File script) throws Exception {
        return execute(script, null);
    }

    /**
     * Executes scriptEngine script
     * @param script
     * @param args
     * @return
     * @throws Exception
     */
    Object execute(File script, Object[] args) throws Exception;

}
