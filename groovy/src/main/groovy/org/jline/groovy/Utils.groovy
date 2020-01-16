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
import groovy.json.JsonParserType

public class Utils {

    private Utils() {}

    static String toString(Object object) {
        object.toString()
    }

    static Object toObject(String json) {
        def slurper = new JsonSlurper(type: JsonParserType.LAX)
        slurper.parseText(json)
    }
    
    static Object convert(Object object) {
        def slurper = new JsonSlurper()
        slurper.parseText(JsonOutput.toJson(object)) 
    }
    
    static String toJson(Object object) {
        String json = object instanceof String ? object : JsonOutput.toJson(object)
        ((json.startsWith("{") && json.endsWith("}"))
            || (json.startsWith("[") && json.endsWith("]"))) ? JsonOutput.prettyPrint(json) : json
    }
    
}
