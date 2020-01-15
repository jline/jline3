/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.groovy;

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

public class Utils {

    private Utils() {}

    static String toString(Object object) {
        object.toString()
    }

    static Object convert(Object object) {
        def slurper = new JsonSlurper()
        slurper.parseText(JsonOutput.toJson(object)) 
    }

    static String toJson(Object object) {
        return object instanceof String ? JsonOutput.prettyPrint(object) 
                                        : JsonOutput.prettyPrint(JsonOutput.toJson(object))
    }
}
