/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.groovy

import java.lang.reflect.*

public class ObjectInspector {
    public static final List<String> METHOD_COLUMNS = ['language','modifiers','this','return','name','parameters','exception','8']
    def obj
    def inspector

    public ObjectInspector(def obj) {
        this.obj = obj
        this.inspector = new groovy.inspect.Inspector(obj)
    }

    List<Map<String, String>> methods() {
        def out = []
        inspector.methods.each {
           def mdef = [:]
           for (int i = 0; i < it.size(); i++) {
               mdef.put(METHOD_COLUMNS.get(i), it.getAt(i))
           }
           out.add(mdef)
        }
        out
    }

    List<Map<String, String>> metaMethods() {
        def out = []
        inspector.metaMethods.each {
           def mdef = [:]
           for (int i = 0; i < it.size(); i++) {
               mdef.put(METHOD_COLUMNS.get(i),it.getAt(i))
           }
           out.add(mdef)
        }
        out
    }

    def properties() {
        def out = [:]
        def props=['propertyInfo', 'publicFields', 'classProps']
        props.each {
           def val = inspector.properties.get(it)
           out.put(it, val)
        }
        out
    }
}
