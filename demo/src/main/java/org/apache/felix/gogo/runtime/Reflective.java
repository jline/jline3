/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Parameter;

public final class Reflective
{
    public final static Object NO_MATCH = new Object();
    public final static String MAIN = "_main";
    public final static Set<String> KEYWORDS = new HashSet<>(
        Arrays.asList("abstract", "continue", "for", "new", "switch",
                "assert", "default", "goto", "package", "synchronized", "boolean", "do",
                "if", "private", "this", "break", "double", "implements", "protected",
                "throw", "byte", "else", "import", "public", "throws", "case", "enum",
                "instanceof", "return", "transient", "catch", "extends", "int", "short",
                "try", "char", "final", "interface", "static", "void", "class",
                "finally", "long", "strictfp", "volatile", "const", "float", "native",
                "super", "while"));

    /**
     * invokes the named method on the given target using the supplied args,
     * which are converted if necessary.
     * @param session the session
     * @param target the target
     * @param name the name
     * @param args the args
     * @return the result of the invoked method
     * @throws Exception on exception
     */
    public static Object invoke(CommandSession session, Object target, String name,
        List<Object> args) throws Exception
    {
        name = name.toLowerCase(Locale.ENGLISH);

        String org = name;
        String get = "get" + name;
        String is = "is" + name;
        String set = "set" + name;

        if (KEYWORDS.contains(name))
        {
            name = "_" + name;
        }

        Set<Class<?>> publicClasses = new LinkedHashSet<>();
        Set<Class<?>> nonPublicClasses = new LinkedHashSet<>();
        getClassAndAncestors(publicClasses, nonPublicClasses, target.getClass());
        List<Method> methods = new ArrayList<>();
        for (Class<?> cl : publicClasses) {
            Collections.addAll(methods, cl.getMethods());
        }
        for (Class<?> cl : nonPublicClasses) {
            Collections.addAll(methods, cl.getMethods());
        }

        Method bestMethod = null;
        Object[] bestArgs = null;
        int lowestMatch = Integer.MAX_VALUE;
        ArrayList<Class<?>[]> possibleTypes = new ArrayList<>();

        for (Method m : methods)
        {
            String mname = m.getName().toLowerCase(Locale.ENGLISH);
            if (mname.equals(name) || mname.equals(get) || mname.equals(set)
                || mname.equals(is) || mname.equals(MAIN))
            {
                Class<?>[] types = m.getParameterTypes();
                ArrayList<Object> xargs = new ArrayList<>(args);

                // pass command name as argv[0] to main, so it can handle
                // multiple commands
                if (mname.equals(MAIN))
                {
                    xargs.add(0, org);
                }

                Object[] parms = new Object[types.length];
                int match = coerce(session, target, m, types, parms, xargs);

                if (match < 0)
                {
                    // coerce failed
                    possibleTypes.add(types);
                }
                else
                {
                    if (match < lowestMatch)
                    {
                        lowestMatch = match;
                        bestMethod = m;
                        bestArgs = parms;
                    }

                    if (match == 0)
                        break; // can't get better score
                }
            }
        }

        if (bestMethod != null)
        {
            bestMethod.setAccessible(true);
            try
            {
                return bestMethod.invoke(target, bestArgs);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof Exception)
                {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
        else
        {
            if (args.isEmpty())
            {
                Field[] fields;
                if (target instanceof Class<?>)
                {
                    fields = ((Class<?>) target).getFields();
                }
                else
                    {
                    fields = target.getClass().getFields();
                }
                for (Field f : fields)
                {
                    String mname = f.getName().toLowerCase(Locale.ENGLISH);
                    if (mname.equals(name))
                    {
                        return f.get(target);
                    }
                }
            }
            ArrayList<String> list = new ArrayList<>();
            for (Class<?>[] types : possibleTypes)
            {
                StringBuilder buf = new StringBuilder();
                buf.append('(');
                for (Class<?> type : types)
                {
                    if (buf.length() > 1)
                    {
                        buf.append(", ");
                    }
                    buf.append(type.getSimpleName());
                }
                buf.append(')');
                list.add(buf.toString());
            }

            StringBuilder params = new StringBuilder();
            for (Object arg : args)
            {
                if (params.length() > 1)
                {
                    params.append(", ");
                }
                params.append(arg == null ? "null" : arg.getClass().getSimpleName());
            }

            throw new IllegalArgumentException(String.format(
                "Cannot coerce %s(%s) to any of %s", name, params, list));
        }
    }

    private static void getClassAndAncestors(Set<Class<?>> publicClasses, Set<Class<?>> nonPublicClasses, Class<?> aClass)
    {
        for (Class<?> itf : aClass.getInterfaces())
        {
            getClassAndAncestors(publicClasses, nonPublicClasses, itf);
        }
        if (aClass.getSuperclass() != null)
        {
            getClassAndAncestors(publicClasses, nonPublicClasses, aClass.getSuperclass());
        }
        if (Modifier.isPublic(aClass.getModifiers()))
        {
            publicClasses.add(aClass);
        }
        else
        {
            nonPublicClasses.add(aClass);
        }
    }

    /**
     * transform name/value parameters into ordered argument list.
     * params: --param2, value2, --flag1, arg3
     * args: true, value2, arg3
     * @return new ordered list of args.
     */
    private static List<Object> transformParameters(Method method, List<Object> in)
    {
        Annotation[][] pas = method.getParameterAnnotations();
        ArrayList<Object> out = new ArrayList<>();
        ArrayList<Object> parms = new ArrayList<>(in);

        for (Annotation as[] : pas)
        {
            for (Annotation a : as)
            {
                if (a instanceof Parameter)
                {
                    int i = -1;
                    Parameter p = (Parameter) a;
                    for (String name : p.names())
                    {
                        i = parms.indexOf(name);
                        if (i >= 0)
                            break;
                    }

                    if (i >= 0)
                    {
                        // parameter present
                        parms.remove(i);
                        Object value = p.presentValue();
                        if (Parameter.UNSPECIFIED.equals(value))
                        {
                            if (i >= parms.size())
                                return null; // missing parameter, so try other methods
                            value = parms.remove(i);
                        }
                        out.add(value);
                    }
                    else
                    {
                        out.add(p.absentValue());
                    }

                }
            }
        }

        out.addAll(parms);

        return out;
    }

    /**
     * Complex routein to convert the arguments given from the command line to
     * the arguments of the method call. First, an attempt is made to convert
     * each argument. If this fails, a check is made to see if varargs can be
     * applied. This happens when the last method argument is an array.
     * @return -1 if arguments can't be coerced; 0 if no coercion was necessary;
     *          > 0 if coercion was needed.
     */
    private static int coerce(CommandSession session, Object target, Method m,
        Class<?> types[], Object out[], List<Object> in)
    {
        List<Object> cnvIn = new ArrayList<>();
        List<Object> cnvIn2 = new ArrayList<>();
        int different = 0;
        for (Object obj : in)
        {
            if (obj instanceof Token)
            {
                Object s1 = Closure.eval(obj);
                Object s2 = obj.toString();
                cnvIn.add(s1);
                cnvIn2.add(s2);
                different += s2.equals(s1) ? 0 : 1;
            } else
                {
                cnvIn.add(obj);
                cnvIn2.add(obj);
            }
        }

        cnvIn = transformParameters(m, cnvIn);
        if (different != 0)
        {
            cnvIn2 = transformParameters(m, cnvIn2);
        }
        if (cnvIn == null || cnvIn2 == null)
        {
            // missing parameter argument?
            return -1;
        }

        int res;

        res = docoerce(session, target, m, types, out, cnvIn);
        // Without conversion
        if (different != 0 && res < 0)
        {
            res = docoerce(session, target, m, types, out, cnvIn2);
        }
        else if (different != 0 && res > 0)
        {
            int res2;
            Object[] out2 = out.clone();
            res2 = docoerce(session, target, m, types, out2, cnvIn2) + different * 2;
            if (res >= 0 && res2 <= res)
            {
                res = res2;
                System.arraycopy(out2, 0, out, 0, out.length);
            }
        }
        // Check if the command takes a session
        if (res < 0 && (types.length > 0) && types[0].isInterface()
                    && types[0].isAssignableFrom(session.getClass()))
        {
            cnvIn.add(0, session);
            res = docoerce(session, target, m, types, out, cnvIn);
            if (different != 0 && res < 0)
            {
                cnvIn2.add(0, session);
                res = docoerce(session, target, m, types, out, cnvIn2);
            }
            else if (different != 0 && res > 0)
            {
                int res2;
                cnvIn2.add(0, session);
                Object[] out2 = out.clone();
                res2 = docoerce(session, target, m, types, out2, cnvIn2) + different * 2;
                if (res >= 0 && res2 <= res)
                {
                    res = res2;
                    System.arraycopy(out2, 0, out, 0, out.length);
                }
            }
        }
        return res;
    }

    private static int docoerce(CommandSession session, Object target, Method m,
                              Class<?> types[], Object out[], List<Object> in)
    {
        int[] convert = { 0 };

        int i = 0;
        while (i < out.length)
        {
            out[i] = null;

            // Try to convert one argument
            if (in.size() == 0 || i == types.length - 1 && types[i].isArray() && in.size() > 1)
            {
                out[i] = NO_MATCH;
            }
            else
            {
                out[i] = coerce(session, types[i], in.get(0), convert);

                if (out[i] == null && types[i].isArray() && in.size() > 0)
                {
                    // don't coerce null to array FELIX-2432
                    out[i] = NO_MATCH;
                }

                if (out[i] != NO_MATCH)
                {
                    in.remove(0);
                }
            }

            if (out[i] == NO_MATCH)
            {
                // No match, check for varargs
                if (types[i].isArray() && (i == types.length - 1))
                {
                    // Try to parse the remaining arguments in an array
                    Class<?> ctype = types[i].getComponentType();
                    int asize = in.size();
                    Object array = Array.newInstance(ctype, asize);
                    int n = i;
                    while (in.size() > 0)
                    {
                        Object t = coerce(session, ctype, in.remove(0), convert);
                        if (t == NO_MATCH)
                        {
                            return -1;
                        }
                        Array.set(array, i - n, t);
                        i++;
                    }
                    out[n] = array;

                    /*
                     * 1. prefer f() to f(T[]) with empty array
                     * 2. prefer f(T) to f(T[1])
                     * 3. prefer f(T) to f(Object[1]) even if there is a conversion cost for T
                     *
                     * 1 & 2 require to add 1 to conversion cost, but 3 also needs to match
                     * the conversion cost for T.
                     */
                    return convert[0] + 1 + (asize * 2);
                }
                return -1;
            }
            i++;
        }

        if (in.isEmpty())
            return convert[0];
        return -1;
    }

    /**
     * converts given argument to specified type and increments convert[0] if any conversion was needed.
     * @param session the session
     * @param type the type
     * @param arg the arg
     * @param convert convert[0] is incremented according to the conversion needed,
     * to allow the "best" conversion to be determined.
     * @return converted arg or NO_MATCH if no conversion possible.
     */
    public static Object coerce(CommandSession session, Class<?> type, final Object arg,
        int[] convert)
    {
        if (arg == null)
        {
            return null;
        }

        if (type.isAssignableFrom(arg.getClass()))
        {
            return arg;
        }

        if (type.isArray() && arg instanceof Collection)
        {
            Collection<?> col = (Collection<?>) arg;
            return col.toArray((Object[]) Array.newInstance(type.getComponentType(), col.size()));
        }

        if (type.isAssignableFrom(List.class) && arg.getClass().isArray())
        {
            return new AbstractList<Object>()
            {
                @Override
                public Object get(int index)
                {
                    return Array.get(arg, index);
                }

                @Override
                public int size()
                {
                    return Array.getLength(arg);
                }
            };
        }

        if (type.isArray())
        {
            return NO_MATCH;
        }

        if (type.isPrimitive() && arg instanceof Long)
        {
            // no-cost conversions between integer types
            Number num = (Number) arg;

            if (type == short.class)
            {
                return num.shortValue();
            }
            if (type == int.class)
            {
                return num.intValue();
            }
            if (type == long.class)
            {
                return num.longValue();
            }
        }

        // all following conversions cost 2 points
        convert[0] += 2;

        Object converted = ((CommandSessionImpl) session).doConvert(type, arg);
        if (converted != null)
        {
            return converted;
        }

        String string = toString(arg);

        if (type.isAssignableFrom(String.class))
        {
            return string;
        }

        if (type.isEnum())
        {
            for (Object o : type.getEnumConstants())
            {
                if (o.toString().equalsIgnoreCase(string))
                {
                    return o;
                }
            }
        }

        if (type.isPrimitive())
        {
            type = primitiveToObject(type);
        }

        try
        {
            return type.getConstructor(String.class).newInstance(string);
        }
        catch (Exception e)
        {
        }

        if (type == Character.class && string.length() == 1)
        {
            return string.charAt(0);
        }

        return NO_MATCH;
    }

    private static String toString(Object arg)
    {
        if (arg instanceof Map)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Map.Entry<?,?> entry : ((Map<?,?>) arg).entrySet())
            {
                if (!first) {
                    sb.append(" ");
                }
                first = false;
                writeValue(sb, entry.getKey());
                sb.append("=");
                writeValue(sb, entry.getValue());
            }
            sb.append("]");
            return sb.toString();
        }
        else if (arg instanceof Collection)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Object o : ((Collection<?>) arg))
            {
                if (!first) {
                    sb.append(" ");
                }
                first = false;
                writeValue(sb, o);
            }
            sb.append("]");
            return sb.toString();
        }
        else
        {
            return arg.toString();
        }
    }

    private static void writeValue(StringBuilder sb, Object o) {
        if (o == null || o instanceof Boolean || o instanceof Number)
        {
            sb.append(o);
        }
        else
        {
            String s = o.toString();
            sb.append("\"");
            for (int i = 0; i < s.length(); i++)
            {
                char c = s.charAt(i);
                if (c == '\"' || c == '=')
                {
                    sb.append("\\");
                }
                sb.append(c);
            }
            sb.append("\"");
        }
    }

    private static Class<?> primitiveToObject(Class<?> type)
    {
        if (type == boolean.class)
        {
            return Boolean.class;
        }
        if (type == byte.class)
        {
            return Byte.class;
        }
        if (type == char.class)
        {
            return Character.class;
        }
        if (type == short.class)
        {
            return Short.class;
        }
        if (type == int.class)
        {
            return Integer.class;
        }
        if (type == float.class)
        {
            return Float.class;
        }
        if (type == double.class)
        {
            return Double.class;
        }
        if (type == long.class)
        {
            return Long.class;
        }
        return null;
    }

}
