/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;

import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Function;

public class FunctionConverter implements Converter {

    @Override
    public Object convert(Class<?> desiredType, Object in) throws Exception {
        if (in instanceof Function && desiredType.isInterface() && isFunctional(desiredType)) {
            return Proxy.newProxyInstance(
                    desiredType.getClassLoader(), new Class<?>[] {desiredType}, new InvocationHandler() {
                        Function command = ((Function) in);

                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            if (isObjectMethod(method)) {
                                return method.invoke(command, args);
                            } else if (method.isDefault()) {
                                final Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                                field.setAccessible(true);
                                final MethodHandles.Lookup lookup = (MethodHandles.Lookup) field.get(null);
                                return lookup.unreflectSpecial(method, method.getDeclaringClass())
                                        .bindTo(proxy)
                                        .invokeWithArguments(args);
                            } else {
                                return command.execute(
                                        null, args != null ? Arrays.asList(args) : Collections.emptyList());
                            }
                        }
                    });
        }
        return null;
    }

    @Override
    public CharSequence format(Object target, int level, Converter escape) throws Exception {
        return null;
    }

    public static boolean isFunctional(Class<?> clazz) {
        if (!clazz.isInterface()) {
            return false;
        }
        int nb = 0;
        for (Method method : clazz.getMethods()) {
            if (method.isDefault() || isObjectMethod(method) || isStatic(method)) {
                continue;
            }
            nb++;
        }
        return nb == 1;
    }

    public static boolean isStatic(Method method) {
        return (method.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
    }

    public static boolean isObjectMethod(Method method) {
        switch (method.getName()) {
            case "toString":
                if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                    return true;
                }
                break;
            case "equals":
                if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == Object.class
                        && method.getReturnType() == boolean.class) {
                    return true;
                }
                break;
            case "hashCode":
                if (method.getParameterCount() == 0 && method.getReturnType() == int.class) {
                    return true;
                }
                break;
        }
        return false;
    }
}
