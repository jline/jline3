/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.script;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.console.ScriptEngine;
import org.jline.groovy.Utils;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.utils.AttributedString;
import org.jline.utils.Log;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Implements Groovy ScriptEngine.
 * You must be very careful when using GroovyEngine in a multithreaded environment. The Binding instance is not
 * thread safe, and it is shared by all scripts.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class GroovyEngine implements ScriptEngine {
    public enum Format {JSON, GROOVY, NONE};
    private static final String REGEX_SYSTEM_VAR = "[A-Z]+[A-Z_]*";
    private static final String REGEX_VAR = "[a-zA-Z_]+[a-zA-Z0-9_]*";
    private static final Pattern PATTERN_FUNCTION_DEF=Pattern.compile("^def\\s+(" + REGEX_VAR + ")\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?\\}(|\n)$"
                                                                     , Pattern.DOTALL);
    private static final Pattern PATTERN_CLASS_DEF=Pattern.compile("^class\\s+(" + REGEX_VAR + ")\\ .*?\\{.*?\\}(|\n)$"
                                                                  , Pattern.DOTALL);
    private GroovyShell shell;
    protected Binding sharedData;
    private Map<String,String> imports = new HashMap<>();
    private Map<String,String> methods = new HashMap<>();
    private Map<String,Class<?>> nameClass = new HashMap<>();
    private Cloner objectCloner = new ObjectCloner();

    public interface Cloner {
        Object clone(Object obj);
    }

    public GroovyEngine() {
        this.sharedData = new Binding();
        shell = new GroovyShell(sharedData);
    }

    @Override
    public Completer getScriptCompleter() {
        return compileCompleter();
    }

    @Override
    public boolean hasVariable(String name) {
        return sharedData.hasVariable(name);
    }

    @Override
    public void put(String name, Object value) {
        sharedData.setProperty(name, value);
    }

    @Override
    public Object get(String name) {
        return sharedData.hasVariable(name) ? sharedData.getVariable(name) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> find(String name) {
        Map<String, Object> out = new HashMap<>();
        if (name == null) {
            out = sharedData.getVariables();
        } else {
            for (String v : internalFind(name)) {
                out.put(v, get(v));
            }
        }
        return out;
    }

    @Override
    public List<String> getSerializationFormats() {
        return Arrays.asList(Format.JSON.toString(), Format.NONE.toString());
    }

    @Override
    public List<String> getDeserializationFormats() {
        return Arrays.asList(Format.JSON.toString(), Format.GROOVY.toString(), Format.NONE.toString());
    }

    @Override
    public Object deserialize(String value, String formatStr) {
        Object out = value;
        Format format = formatStr != null && !formatStr.isEmpty() ? Format.valueOf(formatStr.toUpperCase()) : null;
        if (format == Format.NONE) {
            // do nothing
        } else if (format == Format.JSON) {
            out = Utils.toObject(value);
        } else if (format == Format.GROOVY) {
            try {
                out = execute(value);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            value = value.trim();
            boolean hasCurly = value.contains("{") && value.contains("}");
            try {
                if (value.startsWith("[") && value.endsWith("]")) {
                    try {
                        if (hasCurly) {
                            out = Utils.toObject(value); // try json
                        } else {
                            out = execute(value);
                        }
                    } catch (Exception e) {
                        if (hasCurly) {
                            try {
                                out = execute(value);
                            } catch (Exception e2) {

                            }
                        } else {
                            out = Utils.toObject(value); // try json
                        }
                    }
                } else if (value.startsWith("{") && value.endsWith("}")) {
                    out = Utils.toObject(value);
                }
            } catch (Exception e) {
            }
        }
        return out;
    }

    @Override
    public void persist(Path file, Object object) {
        persist(file, object, getSerializationFormats().get(0));
    }

    @Override
    public void persist(Path file, Object object, String format) {
        Utils.persist(file, object, Format.valueOf(format.toUpperCase()));
    }

    @Override
    public Object execute(File script, Object[] args) throws Exception {
        sharedData.setProperty("_args", args);
        Script s = shell.parse(script);
        return s.run();
    }

    void addToNameClass(String classname) {
        try {
            if (classname.endsWith(".*")) {
                List<Class<?>> classes = PackageHelper.getClassesForPackage(classname);
                for (Class<?> c : classes) {
                    nameClass.put(c.getSimpleName(), c);
                }
            } else {
                String name = classname.substring(classname.lastIndexOf('.') + 1);
                nameClass.put(name, Class.forName(classname));
            }
        } catch (Exception e) {
        }
    }

    @Override
    public Object execute(String statement) throws Exception {
        Object out = null;
        if (statement.startsWith("import ")) {
            shell.evaluate(statement);
            String[] p = statement.split("\\s+", 2);
            String classname = p[1].replaceAll(";", "");
            imports.put(classname, statement);
            addToNameClass(classname);
        } else if (statement.equals("import")) {
            out = new ArrayList<>(imports.keySet());
        } else if (functionDef(statement)) {
            // do nothing
        } else if (statement.equals("def")) {
            out = methods;
        } else if (statement.matches("def\\s+" + REGEX_VAR)) {
            String name = statement.split("\\s+")[1];
            if (methods.containsKey(name)) {
                out = "def " + name + methods.get(name);
            }
        } else {
            String e = "";
            for (Map.Entry<String, String> entry : imports.entrySet()) {
                e += entry.getValue()+"\n";
            }
            e += statement;
            if (classDef(statement)) {
                e += "; null";
            }
            out = shell.evaluate(e);
        }
        return out;
    }

    @Override
    public Object execute(Object closure, Object... args) {
        if (!(closure instanceof Closure)) {
            throw new IllegalArgumentException();
        }
        return ((Closure<?>)closure).call(args);
    }

    @Override
    public String getEngineName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public List<String> getExtensions() {
        return Arrays.asList("groovy");
    }

    @SuppressWarnings("unchecked")
    private List<String> internalFind(String var) {
        List<String> out = new ArrayList<>();
        if(!var.contains(".") && var.contains("*")) {
            var = var.replaceAll("\\*", ".*");
        }
        for (String v :  (Set<String>)sharedData.getVariables().keySet()) {
            if (v.matches(var)) {
                out.add(v);
            }
        }
        return out;
    }

    private boolean functionDef (String statement) throws Exception{
        boolean out = false;
        Matcher m = PATTERN_FUNCTION_DEF.matcher(statement);
        if(m.matches()){
            out = true;
            put(m.group(1), execute("{" + m.group(2) + "->" + m.group(3) + "}"));
            methods.put(m.group(1), "(" + m.group(2) + ")" + "{" + m.group(3) + "}");
        }
        return out;
    }

    private boolean classDef (String statement) throws Exception{
        return PATTERN_CLASS_DEF.matcher(statement).matches();
    }

    private void refreshNameClass() {
        nameClass.clear();
        for (String name : imports.keySet()) {
            addToNameClass(name);
        }
    }

    private void del(String var) {
        if (var == null) {
            return;
        }
        if (imports.containsKey(var)) {
            imports.remove(var);
            if (var.endsWith(".*")) {
                refreshNameClass();
            } else {
                nameClass.remove(var.substring(var.lastIndexOf('.') + 1));
            }
        } else if (sharedData.hasVariable(var)) {
            sharedData.getVariables().remove(var);
            if (methods.containsKey(var)) {
                methods.remove(var);
            }
        } else if (!var.contains(".") && var.contains("*")) {
            for (String v : internalFind(var)){
                if (sharedData.hasVariable(v) && !v.equals("_") && !v.matches(REGEX_SYSTEM_VAR)) {
                    sharedData.getVariables().remove(v);
                    if (methods.containsKey(v)) {
                        methods.remove(v);
                    }
                }
            }
        }
    }

    @Override
    public void del(String... vars) {
        if (vars == null) {
            return;
        }
        for (String s: vars) {
            del(s);
        }
    }

    @Override
    public String toJson(Object obj) {
        return Utils.toJson(obj);
    }

    @Override
    public String toString(Object obj) {
        return Utils.toString(obj);
    }

    @Override
    public Map<String,Object> toMap(Object obj) {
        return Utils.toMap(obj);
    }

    public void setObjectCloner(Cloner objectCloner) {
        this.objectCloner = objectCloner;
    }

    public Cloner getObjectCloner() {
        return objectCloner;
    }

    private Completer compileCompleter() {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(new StringsCompleter("while", "class", "for", "print", "println"), NullCompleter.INSTANCE));
        completers.add(new ArgumentCompleter(new StringsCompleter("def"), new StringsCompleter(methods::keySet), NullCompleter.INSTANCE));
        completers.add(new ArgumentCompleter(new StringsCompleter("import")
                                           , new PackageCompleter(CandidateType.PACKAGE), NullCompleter.INSTANCE));
        completers.add(new MethodCompleter(this));
        return new AggregateCompleter(completers);
    }

    private enum CandidateType {CONSTRUCTOR, STATIC_METHOD, PACKAGE, METHOD, OTHER};

    private static class Helpers {

        private static Set<String> loadedPackages() {
            Set<String> out = new HashSet<>();
            for (Package p : Package.getPackages()) {
                out.add(p.getName());
            }
            return out;
        }

        private static Set<String> names(String domain) {
            Set<String> out = new HashSet<>();
            for (String p : loadedPackages()) {
                if (p.startsWith(domain)) {
                    int idx = p.indexOf('.', domain.length());
                    if (idx < 0) {
                        idx = p.length();
                    }
                    out.add(p.substring(domain.length(), idx));
                }
            }
            return out;
        }

        public static Set<String> getMethods(Class<?> clazz) {
            return getMethods(clazz, false);
        }

        public static Set<String> getStaticMethods(Class<?> clazz) {
            return getMethods(clazz, true);
        }

        private static Set<String> getMethods(Class<?> clazz, boolean statc) {
            Set<String> out = new HashSet<>();
            for (Method method : clazz.getMethods()) {
                if ((statc && Modifier.isStatic(method.getModifiers()))
                        || (!statc && !Modifier.isStatic(method.getModifiers()))) {
                    out.add(method.getName());
                }
            }
            return out;
        }

        public static Set<String> getFields(Class<?> clazz) {
            return getFields(clazz, false);
        }

        public static Set<String> getStaticFields(Class<?> clazz) {
            return getFields(clazz, true);
        }

        private static Set<String> getFields(Class<?> clazz, boolean statc) {
            Set<String> out = new HashSet<>();
            for (Field field : clazz.getFields()) {
                if ((statc && Modifier.isStatic(field.getModifiers()))
                        || (!statc && !Modifier.isStatic(field.getModifiers()))) {
                    out.add(field.getName());
                }
            }
            return out;
        }

        public static Set<String> nextDomain(String domain, CandidateType type) {
            Set<String> out = new HashSet<>();
            if (domain.isEmpty()) {
                for (String p : loadedPackages()) {
                    out.add(p.split("\\.")[0]);
                }
            } else if ((domain.split("\\.")).length < 2) {
                out = names(domain);
            } else {
                try {
                    List<Class<?>> classes = PackageHelper.getClassesForPackage(domain);
                    for (Class<?> c : classes) {
                        if (!Modifier.isPublic(c.getModifiers()) || c.getName().contains("$")) {
                            continue;
                        }
                        if ((type == CandidateType.CONSTRUCTOR && (c.getConstructors().length == 0 || Modifier.isAbstract(c.getModifiers())))
                                || (type == CandidateType.STATIC_METHOD && getStaticMethods(c).size() == 0
                                     && getStaticFields(c).size() == 0)){
                            continue;
                        }
                        try {
                            String name = c.getCanonicalName();
                            int idx = name.indexOf('.', domain.length());
                            if (idx < 0) {
                                idx = name.length();
                            }
                            out.add(name.substring(domain.length(), idx));
                        } catch (NoClassDefFoundError e) {
                            if (Log.isDebugEnabled()) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    if (Log.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                    out = names(domain);
                }
            }
            return out;
        }

        public static void doCandidates(List<Candidate> candidates, Collection<String> fields, String curBuf, String hint,
                CandidateType type) {
            if (fields == null) {
                return;
            }
            for (String s : fields) {
                if (s == null || !s.startsWith(hint)) {
                    continue;
                }
                String postFix = "";
                if (type == CandidateType.CONSTRUCTOR) {
                    if (s.matches("[a-z]+.*")) {
                        postFix = ".";
                    } else if (s.matches("[A-Z]+.*")) {
                        postFix = "(";
                    }
                } else if (type == CandidateType.STATIC_METHOD) {
                    postFix = ".";
                } else if (type == CandidateType.PACKAGE) {
                    if (s.matches("[a-z]+.*")) {
                        postFix = ".";
                    }
                } else if (type == CandidateType.METHOD) {
                    postFix = "(";
                }
                candidates.add(new Candidate(AttributedString.stripAnsi(curBuf + s + postFix), s, null, null, null,
                        null, false));
            }
        }
    }

    private class PackageCompleter implements Completer {
        private CandidateType type;

        public PackageCompleter(CandidateType type) {
            this.type = type;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            String buffer = commandLine.word().substring(0, commandLine.wordCursor());
            String param = buffer;
            String curBuf = "";
            int lastDelim = buffer.lastIndexOf('.');
            if (lastDelim > -1) {
                param = buffer.substring(lastDelim + 1);
                curBuf = buffer.substring(0, lastDelim + 1);
            }
            Helpers.doCandidates(candidates, Helpers.nextDomain(curBuf, type), curBuf, param, type);
        }

    }

    private class MethodCompleter implements Completer {
        private GroovyEngine groovyEngine;

        public MethodCompleter(GroovyEngine engine){
            this.groovyEngine = engine;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            String wordbuffer = commandLine.word();
            String buffer = commandLine.line().substring(0, commandLine.cursor());
            Brackets brackets = new Brackets(buffer);
            if (commandLine.wordIndex() > 0 && !commandLine.words().get(0).matches("(new|\\w+=new)") && brackets.numberOfRounds() == 0
                    && !brackets.openRound()) {
                return;
            }
            if (brackets.openCurly() || brackets.numberOfCurlies() > 0) {
                return;
            }
            if (brackets.numberOfRounds() > 0 && brackets.lastCloseRound() > brackets.lastComma()
                    && brackets.lastCloseRound() > brackets.lastOpenRound()) {
                int varsep = buffer.lastIndexOf('.');
                int eqsep = statementBegin(buffer.indexOf('='), brackets.lastOpenRound(), brackets.lastComma());
                if (varsep > 0 && varsep > eqsep) {
                    Class<?> clazz = evaluateClass(buffer.substring(eqsep + 1, varsep));
                    int vs = wordbuffer.lastIndexOf('.');
                    String curBuf = wordbuffer.substring(0, vs + 1);
                    String hint = wordbuffer.substring(vs + 1);
                    doMethodCandidates(candidates, clazz, curBuf, hint);
                }
            } else if ((commandLine.wordIndex() == 1 && commandLine.words().get(0).matches("(new|\\w+=new)"))
                    || (commandLine.wordIndex() > 1 && commandLine.words().get(commandLine.wordIndex() - 1).matches("(new|.*\\(new|.*,new)"))
                    ) {
                if (wordbuffer.matches("[a-z]+.*")) {
                    int idx = wordbuffer.lastIndexOf('.');
                    if (idx > 0 && wordbuffer.substring(idx + 1).matches("[A-Z]+.*")) {
                        try {
                            Class.forName(wordbuffer);
                            Helpers.doCandidates(candidates, Arrays.asList("("), wordbuffer, "(", CandidateType.OTHER);
                        } catch (Exception e) {
                            String param = wordbuffer.substring(0, idx + 1);
                            Helpers.doCandidates(candidates
                                               , Helpers.nextDomain(param, CandidateType.CONSTRUCTOR)
                                               , param, wordbuffer.substring(idx + 1), CandidateType.CONSTRUCTOR);
                        }
                    } else {
                        new PackageCompleter(CandidateType.CONSTRUCTOR).complete(reader, commandLine, candidates);
                    }
                } else {
                    Helpers.doCandidates(candidates, retrieveConstructors(), "", wordbuffer, CandidateType.CONSTRUCTOR);
                }
            } else {
                int varsep = wordbuffer.lastIndexOf('.');
                int eqsep = statementBegin(buffer, wordbuffer, brackets);
                String param = wordbuffer.substring(eqsep + 1);
                if (varsep < 0 || varsep < eqsep) {
                    String curBuf = wordbuffer.substring(0, eqsep + 1);
                    Helpers.doCandidates(candidates, find(null).keySet(), curBuf, param, CandidateType.OTHER);
                    Helpers.doCandidates(candidates, retrieveClassesWithStaticMethods(), curBuf, param, CandidateType.STATIC_METHOD);
                } else {
                    boolean firstMethod = param.indexOf('.') == param.lastIndexOf('.');
                    String var = param.substring(0, param.indexOf('.'));
                    String curBuf = wordbuffer.substring(0, varsep + 1);
                    String p = wordbuffer.substring(varsep + 1);
                    if (nameClass.containsKey(var)) {
                        if (firstMethod) {
                            doStaticMethodCandidates(candidates, nameClass.get(var), curBuf, p);
                        } else {
                            Class<?> clazz = evaluateClass(wordbuffer.substring(eqsep, varsep));
                            doMethodCandidates(candidates, clazz, curBuf, p);
                        }
                    } else if (hasVariable(var)) {
                        if (firstMethod) {
                            doMethodCandidates(candidates, get(var).getClass(), curBuf, p);
                        } else {
                            Class<?> clazz = evaluateClass(wordbuffer.substring(eqsep, varsep));
                            doMethodCandidates(candidates, clazz, curBuf, p);
                        }
                    } else {
                        try {
                            param = wordbuffer.substring(eqsep + 1, varsep);
                            doStaticMethodCandidates(candidates, Class.forName(param), curBuf, p);
                        } catch (Exception e) {
                            param = wordbuffer.substring(eqsep + 1, varsep + 1);
                            Helpers.doCandidates(candidates
                                    , Helpers.nextDomain(param, CandidateType.STATIC_METHOD)
                                    , curBuf, p, CandidateType.STATIC_METHOD);
                        }
                    }
                }
            }
        }

        private int statementBegin(String buffer, String wordbuffer, Brackets brackets) {
            int out =  wordbuffer.indexOf('=');
            if (brackets.openRound()) {
                int idx = buffer.lastIndexOf(wordbuffer);
                if (idx > -1) {
                    out = statementBegin(out, brackets.lastOpenRound() - idx, brackets.lastComma() - idx);
                }
            }
            return out;
        }

        private int statementBegin(int eqPos, int openRound, int comma) {
            int out = eqPos;
            if (openRound > out) {
                out = openRound;
            }
            if (comma > out) {
                out = comma;
            }
            return out;
        }

        private Class<?> evaluateClass(String objectStatement) {
            return new Inspector(groovyEngine).evaluateClass(objectStatement);
        }

        private void doMethodCandidates(List<Candidate> candidates, Class<?> clazz, String curBuf, String hint) {
            if (clazz == null) {
                return;
            }
            Helpers.doCandidates(candidates, Helpers.getMethods(clazz), curBuf, hint, CandidateType.METHOD);
            Helpers.doCandidates(candidates, Helpers.getFields(clazz), curBuf, hint, CandidateType.OTHER);
        }

        private void doStaticMethodCandidates(List<Candidate> candidates, Class<?> clazz, String curBuf, String hint) {
            if (clazz == null) {
                return;
            }
            Helpers.doCandidates(candidates, Helpers.getStaticMethods(clazz), curBuf, hint, CandidateType.METHOD);
            Helpers.doCandidates(candidates, Helpers.getStaticFields(clazz), curBuf, hint, CandidateType.OTHER);
        }

        private Set<String> retrieveConstructors() {
            Set<String> out = new HashSet<>();
            for (Map.Entry<String, Class<?>> entry : nameClass.entrySet()) {
                Class<?> c = entry.getValue();
                if (c.getConstructors().length == 0 || Modifier.isAbstract(c.getModifiers())) {
                    continue;
                }
                out.add(entry.getKey());
            }
            return out;
        }

        private Set<String> retrieveClassesWithStaticMethods() {
            Set<String> out = new HashSet<>();
            for (Map.Entry<String, Class<?>> entry : nameClass.entrySet()) {
                Class<?> c = entry.getValue();
                if (Helpers.getStaticMethods(c).size() == 0 && Helpers.getStaticFields(c).size() == 0) {
                    continue;
                }
                out.add(entry.getKey());
            }
            return out;
        }
    }

    private static class Inspector {
        private GroovyShell shell;
        protected Binding sharedData = new Binding();
        private Map<String,String> imports = new HashMap<>();

        public Inspector(GroovyEngine groovyEngine) {
            imports.putAll(groovyEngine.imports);
            for (Map.Entry<String, Object> entry : groovyEngine.find().entrySet()) {
                Object obj = entry.getValue() instanceof Closure ? entry.getValue()
                                                                 : groovyEngine.getObjectCloner().clone(entry.getValue());
                sharedData.setVariable(entry.getKey(), obj);
            }
            shell = new GroovyShell(sharedData);
        }

        public Class<?> evaluateClass(String objectStatement) {
            try {
                return execute(objectStatement).getClass();
            } catch (Exception e) {

            }
            return null;
        }

        private Object execute(String statement) {
            String e = "";
            for (Map.Entry<String, String> entry : imports.entrySet()) {
                e += entry.getValue()+"\n";
            }
            e += statement;
            return shell.evaluate(e);
        }
    }

    private static class ObjectCloner implements Cloner {

        public ObjectCloner() {

        }

        /**
         * Shallow copy of the object using java Cloneable clone() method.
         */
        public Object clone(Object obj) {
            Object out = null;
            try {
                Class<?> clazz = obj.getClass();
                Method clone = clazz.getDeclaredMethod("clone");
                out = clone.invoke(obj);
            } catch (Exception e) {
                out = obj;
            }
            return out;
        }
    }

    private static class Brackets {
        char[] quote = {'"', '\''};
        Deque<Integer> roundOpen = new ArrayDeque<>();
        Map<Integer,Integer> lastComma = new HashMap<>();
        int lastRoundClose = -1;
        int quoteId = -1;
        int round = 0;
        int curly = 0;
        int rounds = 0;
        int curlies = 0;

        public Brackets(String line) {
            int pos = -1;
            for (char ch : line.toCharArray()) {
                pos++;
                if (quoteId < 0) {
                    for (int i = 0; i < quote.length; i++) {
                        if (ch == quote[i]) {
                            quoteId = i;
                            break;
                        }
                    }
                } else {
                    if (ch == quote[quoteId]) {
                        quoteId = -1;
                    }
                    continue;
                }
                if (quoteId >= 0) {
                    continue;
                }
                if (ch == '(') {
                    round++;
                    roundOpen.add(pos);
                } else if (ch == ')') {
                    rounds++;
                    round--;
                    lastComma.remove(roundOpen.getLast());
                    roundOpen.removeLast();
                    lastRoundClose = pos;
                } else if (ch == '{') {
                    curly++;
                } else if (ch == '}') {
                    curlies++;
                    curly--;
                } else if (ch == ',' && !roundOpen.isEmpty()) {
                    lastComma.put(roundOpen.getLast(), pos);
                }
                if (round < 0 || curly < 0) {
                    break;
                }
            }
        }

        public boolean openRound() {
            return round > 0;
        }

        public boolean openCurly() {
            return curly > 0;
        }

        public int numberOfRounds() {
            return rounds;
        }

        public int numberOfCurlies() {
            return curlies;
        }

        public int lastOpenRound() {
            return !roundOpen.isEmpty() ? roundOpen.getLast() : -1;
        }

        public int lastCloseRound() {
            return lastRoundClose;
        }

        public int lastComma() {
            int last = lastOpenRound();
            return lastComma.containsKey(last) ? lastComma.get(last) : -1;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("rounds: ").append(rounds).append("\n");
            sb.append("curlies: ").append(curlies).append("\n");
            sb.append("lastOpenRound: ").append(lastOpenRound()).append("\n");
            sb.append("lastCloseRound: ").append(lastRoundClose).append("\n");
            sb.append("lastComma: ").append(lastComma()).append("\n");
            return sb.toString();
        }
    }

}
