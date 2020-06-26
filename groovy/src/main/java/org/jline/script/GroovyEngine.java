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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.jline.builtins.Nano.SyntaxHighlighter;
import org.jline.console.CmdDesc;
import org.jline.console.CmdLine;
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
import org.jline.utils.AttributedStyle;
import org.jline.utils.Log;
import org.jline.utils.OSUtils;

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
    public static final String CANONICAL_NAMES = "canonicalNames";

    private static final String VAR_GROOVY_OPTIONS = "GROOVY_OPTIONS";
    private static final String REGEX_SYSTEM_VAR = "[A-Z]+[A-Z_]*";
    private static final String REGEX_VAR = "[a-zA-Z_]+[a-zA-Z0-9_]*";
    private static final Pattern PATTERN_FUNCTION_DEF = Pattern.compile("^def\\s+(" + REGEX_VAR + ")\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?\\}(|\n)$"
                                                                     , Pattern.DOTALL);
    private static final Pattern PATTERN_CLASS_DEF = Pattern.compile("^class\\s+(" + REGEX_VAR + ")\\ .*?\\{.*?\\}(|\n)$"
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
                int  idx = classname.lastIndexOf(".");
                String name = classname.substring(idx + 1);
                try {
                    nameClass.put(name, Class.forName(classname));
                } catch (ClassNotFoundException ex) {
                    String innerclass = classname.substring(0, idx) + "$" + name;
                    nameClass.put(name, Class.forName(innerclass));
                }
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

    private boolean functionDef(String statement) throws Exception{
        boolean out = false;
        Matcher m = PATTERN_FUNCTION_DEF.matcher(statement);
        if (m.matches()) {
            out = true;
            put(m.group(1), execute("{" + m.group(2) + "->" + m.group(3) + "}"));
            methods.put(m.group(1), "(" + m.group(2) + ")" + "{" + m.group(3) + "}");
        }
        return out;
    }

    private boolean classDef(String statement) throws Exception{
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

    public CmdDesc scriptDescription(CmdLine line) {
        return new Inspector(this).scriptDescription(line);
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> groovyOptions() {
        return hasVariable(VAR_GROOVY_OPTIONS) ? (Map<String, Object>) get(VAR_GROOVY_OPTIONS)
                                                       : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private <T>T groovyOption(String option, T defval) {
        T out = defval;
        try {
            out = (T) groovyOptions().getOrDefault(option, defval);
        } catch (Exception e) {
        }
        return out;
    }

    private Completer compileCompleter() {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(new StringsCompleter("class", "print", "println"), NullCompleter.INSTANCE));
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

        public static int statementBegin(String buffer, String wordbuffer, Brackets brackets) {
            int out =  -1;
            int idx = buffer.lastIndexOf(wordbuffer);
            if (idx > -1) {
                out = statementBegin(brackets.lastDelim() - idx
                                   , brackets.lastOpenRound() - idx
                                   , brackets.lastComma() - idx
                                   , brackets.lastOpenCurly() - idx
                                   , brackets.lastCloseCurly() - idx
                                   , brackets.lastSemicolon() - idx);
            }
            return out;
        }

        public static int statementBegin(Brackets brackets) {
            return statementBegin(brackets.lastDelim()
                                , brackets.lastOpenRound()
                                , brackets.lastComma()
                                , brackets.lastOpenCurly(), brackets.lastCloseCurly(), brackets.lastSemicolon());
        }

        private static int statementBegin(int lastDelim, int openRound, int comma, int openCurly, int closeCurly, int semicolon) {
            int out = lastDelim;
            if (openRound > out) {
                out = openRound;
            }
            if (comma > out) {
                out = comma;
            }
            if (openCurly > out) {
                out = openCurly;
            }
            if (closeCurly > out) {
                out = closeCurly;
            }
            if (semicolon > out) {
                out = semicolon;
            }
            return Math.max(out, -1);
        }

        public static boolean constructorStatement(String fragment) {
            return fragment.matches("(.*\\s+new|.*\\(new|.*\\{new|.*=new|.*,new|new)");
        }

    }

    private static class PackageCompleter implements Completer {
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

    private static class MethodCompleter implements Completer {
        private static List<String> KEY_WORDS = Arrays.asList("print", "println");
        private GroovyEngine groovyEngine;
        Inspector inspector;

        public MethodCompleter(GroovyEngine engine){
            this.groovyEngine = engine;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            String wordbuffer = commandLine.word();
            String buffer = commandLine.line().substring(0, commandLine.cursor());
            Brackets brackets = null;
            try {
                brackets = new Brackets(buffer);
            } catch (Exception e) {
                return;
            }
            if (brackets.openQuote() || (commandLine.wordIndex() > 0 && !commandLine.words().get(0).matches("(new|\\w+=new)")
                    && brackets.numberOfRounds() == 0 && !brackets.openRound() && !brackets.openCurly())) {
                return;
            }
            inspector = null;
            if (brackets.openCurly()) {
                inspector = new Inspector(groovyEngine);
                inspector.loadStatementVars(buffer);
            }
            int eqsep = Helpers.statementBegin(brackets);
            if (brackets.numberOfRounds() > 0 && brackets.lastCloseRound() > eqsep) {
                int varsep = buffer.lastIndexOf('.');
                if (varsep > 0 && varsep > eqsep) {
                    Class<?> clazz = evaluateClass(buffer.substring(eqsep + 1, varsep));
                    int vs = wordbuffer.lastIndexOf('.');
                    String curBuf = wordbuffer.substring(0, vs + 1);
                    String hint = wordbuffer.substring(vs + 1);
                    doMethodCandidates(candidates, clazz, curBuf, hint);
                }
            } else if (!wordbuffer.contains("(") &&
                      ((commandLine.wordIndex() == 1 && commandLine.words().get(0).matches("(new|\\w+=new)"))
                    || (commandLine.wordIndex() > 1 && Helpers.constructorStatement(commandLine.words().get(commandLine.wordIndex() - 1))))
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
                boolean addKeyWords = eqsep == brackets.lastSemicolon() || eqsep == brackets.lastOpenCurly();
                int varsep = wordbuffer.lastIndexOf('.');
                eqsep = Helpers.statementBegin(buffer, wordbuffer, brackets);
                String param = wordbuffer.substring(eqsep + 1);
                if (varsep < 0 || varsep < eqsep) {
                    String curBuf = wordbuffer.substring(0, eqsep + 1);
                    if (param.trim().length() == 0) {
                        Helpers.doCandidates(candidates, Arrays.asList("") , curBuf, param, CandidateType.OTHER);
                    } else {
                        if (addKeyWords) {
                            Helpers.doCandidates(candidates, KEY_WORDS, curBuf, param, CandidateType.METHOD);
                        }
                        Helpers.doCandidates(candidates, variables(), curBuf, param, CandidateType.OTHER);
                        Helpers.doCandidates(candidates, retrieveClassesWithStaticMethods(), curBuf, param,
                                CandidateType.STATIC_METHOD);
                    }
                } else {
                    boolean firstMethod = param.indexOf('.') == param.lastIndexOf('.');
                    String var = param.substring(0, param.indexOf('.'));
                    String curBuf = wordbuffer.substring(0, varsep + 1);
                    String p = wordbuffer.substring(varsep + 1);
                    if (nameClass().containsKey(var)) {
                        if (firstMethod) {
                            doStaticMethodCandidates(candidates, nameClass().get(var), curBuf, p);
                        } else {
                            Class<?> clazz = evaluateClass(wordbuffer.substring(eqsep + 1, varsep));
                            doMethodCandidates(candidates, clazz, curBuf, p);
                        }
                    } else if (hasVariable(var)) {
                        if (firstMethod) {
                            doMethodCandidates(candidates, getVariable(var).getClass(), curBuf, p);
                        } else {
                            Class<?> clazz = evaluateClass(wordbuffer.substring(eqsep + 1, varsep));
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

        private Set<String> variables() {
            if (inspector == null) {
                inspector = new Inspector(groovyEngine);
            }
            return inspector.variables();
        }

        private boolean hasVariable(String name) {
            if (inspector == null) {
                inspector = new Inspector(groovyEngine);
            }
            return inspector.hasVariable(name);
        }

        private Object getVariable(String name) {
            if (inspector == null) {
                inspector = new Inspector(groovyEngine);
            }
            return inspector.getVariable(name);
        }

        private Class<?> evaluateClass(String objectStatement) {
            if (inspector == null) {
                inspector = new Inspector(groovyEngine);
            }
            return inspector.evaluateClass(objectStatement);
        }

        private Map<String,Class<?>> nameClass() {
            if (inspector == null) {
                inspector = new Inspector(groovyEngine);
            }
            return inspector.nameClass();
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
            for (Map.Entry<String, Class<?>> entry : nameClass().entrySet()) {
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
            for (Map.Entry<String, Class<?>> entry : nameClass().entrySet()) {
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
        static final Pattern PATTERN_FOR = Pattern.compile("^for\\s*\\((.*?);.*");
        static final Pattern PATTERN_FUNCTION_BODY = Pattern.compile("^\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?\\}(|\n)$"
                                                                   , Pattern.DOTALL);
        private GroovyShell shell;
        protected Binding sharedData = new Binding();
        private Map<String,String> imports = new HashMap<>();
        private Map<String,Class<?>> nameClass = new HashMap<>();
        PrintStream nullstream;
        boolean canonicalNames = false;
        String[] equationLines;
        int cuttedSize;

        public Inspector(GroovyEngine groovyEngine) {
            this.imports = groovyEngine.imports;
            this.nameClass = groovyEngine.nameClass;
            this.canonicalNames = groovyEngine.groovyOption(CANONICAL_NAMES, canonicalNames);
            for (Map.Entry<String, Object> entry : groovyEngine.find().entrySet()) {
                Object obj = groovyEngine.getObjectCloner().clone(entry.getValue());
                sharedData.setVariable(entry.getKey(), obj);
            }
            shell = new GroovyShell(sharedData);
            try {
                File file = OSUtils.IS_WINDOWS ? new File("NUL") : new File("/dev/null");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                OutputStream outputStream = fileOutputStream;
                nullstream = new PrintStream(outputStream);
            } catch (Exception e) {
            }
            for (Map.Entry<String,String> entry : groovyEngine.methods.entrySet()) {
                Matcher m = PATTERN_FUNCTION_BODY.matcher(entry.getValue());
                if (m.matches()) {
                    sharedData.setVariable(entry.getKey(), execute("{" + m.group(1) + "->" + m.group(2) + "}"));
                }
            }
        }

        public Class<?> evaluateClass(String objectStatement) {
            Class<?> out = null;
            try {
                if (objectStatement.contains("(") || objectStatement.contains(")")
                        || objectStatement.contains("{") || objectStatement.contains("}")) {
                    out = execute(objectStatement).getClass();
                } else if (!objectStatement.contains(".") ) {
                    out = (Class<?>)execute(objectStatement + ".class");
                } else {
                    out = Class.forName(objectStatement);
                }
            } catch (Exception e) {

            }
            return out;
        }

        private Object execute(String statement) {
            PrintStream origOut = System.out;
            PrintStream origErr = System.err;
            if (nullstream != null) {
                System.setOut(nullstream);
                System.setErr(nullstream);
            }
            Object out = null;
            try {
                String e = "";
                for (Map.Entry<String, String> entry : imports.entrySet()) {
                    e += entry.getValue() + "\n";
                }
                e += statement;
                out = shell.evaluate(e);
            } catch (Exception e) {
                throw e;
            } finally {
                System.setOut(origOut);
                System.setErr(origErr);
            }
            return out;
        }

        public void loadStatementVars(String line) {
            for (String s : line.split("\\r?\\n")) {
                String statement = s.trim();
                try {
                    Matcher forMatcher = PATTERN_FOR.matcher(statement);
                    if (statement.matches("^(if|while)\\s*\\(.*") || statement.matches("(\\}\\s*|^)else(\\s*\\{|$)")
                            || statement.matches("(\\}\\s*|^)else\\s+if\\s*\\(.*") || statement.matches("^break[;]{1,}")
                            || statement.matches("^case\\s+.*:") || statement.matches("^default\\s+:")
                            || statement.matches("(\\{|\\})") || statement.length() == 0) {
                        continue;
                    } else if (forMatcher.matches()) {
                        statement = forMatcher.group(1).trim();
                        int idx = statement.indexOf(' ');
                        statement = statement.substring(idx + 1);
                    } else if (statement.matches("\\w+\\s+.*=.*")) {
                        int idx = statement.indexOf(' ');
                        statement = statement.substring(idx + 1);
                    }
                    Brackets br = new Brackets(statement);
                    if (statement.contains("=") && !br.openRound() && !br.openCurly()) {
                        execute(statement);
                    }
                } catch (Exception e) {
                    if (Log.isDebugEnabled()) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public Map<String,Class<?>> nameClass() {
            return nameClass;
        }

        @SuppressWarnings("unchecked")
        public Set<String> variables() {
            return sharedData.getVariables().keySet();
        }

        public boolean hasVariable(String name) {
            return sharedData.hasVariable(name);
        }

        public Object getVariable(String name) {
            return sharedData.hasVariable(name) ? sharedData.getVariable(name) : null;
        }

        public CmdDesc scriptDescription(CmdLine line) {
            CmdDesc out = null;
            try {
                switch (line.getDescriptionType()) {
                case COMMAND:
                    break;
                case METHOD:
                    out = methodDescription(line);
                    break;
                case SYNTAX:
                    out = checkSyntax(line);
                    break;
                }
            } catch (Throwable e) {
                if (Log.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
            return out;
        }

        private String trimName(String name) {
            String out = name;
            int idx = name.lastIndexOf('(');
            if (idx > 0) {
                out = name.substring(0, idx);
            }
            return out;
        }

        private CmdDesc methodDescription(CmdLine line) throws Exception {
            CmdDesc out = new CmdDesc();
            List<String> args = line.getArgs();
            boolean constructor = false;
            Class<?> clazz = null;
            String methodName = null;
            if ((args.size() == 2 && args.get(0).matches("(new|\\w+=new)"))
                || (args.size() > 2 && Helpers.constructorStatement(args.get(args.size() - 2)))) {
                constructor = true;
                clazz = evaluateClass(trimName(args.get(args.size() - 1)));
            } else {
                String buffer = line.getHead();
                String wordbuffer = trimName(args.get(args.size() - 1));
                Brackets brackets = new Brackets(buffer);
                int varsep = wordbuffer.lastIndexOf('.');
                int eqsep = Helpers.statementBegin(buffer, wordbuffer, brackets);
                if (varsep > 0 && varsep > eqsep) {
                    loadStatementVars(buffer);
                    methodName = wordbuffer.substring(varsep + 1);
                    clazz = evaluateClass(wordbuffer.substring(eqsep + 1, varsep));
                }
            }
            List<AttributedString> mainDesc = new ArrayList<>();
            if (clazz != null) {
                SyntaxHighlighter java = SyntaxHighlighter.build("classpath:/org/jline/groovy/java.nanorc");
                mainDesc.add(java.highlight(clazz.toString()));
                if (constructor) {
                    for (Constructor<?> m : clazz.getConstructors()) {
                        StringBuilder sb = new StringBuilder();
                        String name = m.getName();
                        if (!canonicalNames) {
                            int idx = name.lastIndexOf('.');
                            name = name.substring(idx + 1);
                        }
                        sb.append(name);
                        sb.append("(");
                        boolean first = true;
                        for(Class<?> p: m.getParameterTypes()) {
                            if (!first) {
                                sb.append(", ");
                            }
                            sb.append(canonicalNames ? p.getTypeName() : p.getSimpleName());
                            first = false;
                        }
                        sb.append(")");
                        first = true;
                        for (Class<?> e: m.getExceptionTypes()) {
                            if (first) {
                                sb.append(" throws ");
                            } else {
                                sb.append(", ");
                            }
                            sb.append(canonicalNames ? e.getCanonicalName() : e.getSimpleName());
                            first = false;
                        }
                        mainDesc.add(java.highlight(trimMethodDescription(sb)));
                    }
                } else {
                    List<String> addedMethods = new ArrayList<>();
                    do {
                        for (Method m : clazz.getMethods()) {
                            if (!m.getName().equals(methodName)) {
                                continue;
                            }
                            StringBuilder sb = new StringBuilder();
                            if (Modifier.isFinal(m.getModifiers())) {
                                sb.append("final ");
                            }
                            if (Modifier.isStatic(m.getModifiers())) {
                                sb.append("static ");
                            }
                            sb.append(canonicalNames ?  m.getReturnType().getCanonicalName() : m.getReturnType().getSimpleName());
                            sb.append(" ");
                            sb.append(methodName);
                            sb.append("(");
                            boolean first = true;
                            for (Class<?> p : m.getParameterTypes()) {
                                if (!first) {
                                    sb.append(", ");
                                }
                                sb.append(canonicalNames ? p.getTypeName() : p.getSimpleName());
                                first = false;
                            }
                            sb.append(")");
                            first = true;
                            for (Class<?> e : m.getExceptionTypes()) {
                                if (first) {
                                    sb.append(" throws ");
                                } else {
                                    sb.append(", ");
                                }
                                sb.append(canonicalNames ? e.getCanonicalName() : e.getSimpleName());
                                first = false;
                            }
                            if (!addedMethods.contains(sb.toString())) {
                                addedMethods.add(sb.toString());
                                mainDesc.add(java.highlight(trimMethodDescription(sb)));
                            }
                        }
                        clazz = clazz.getSuperclass();
                    } while (clazz != null);
                }
                out.setMainDesc(mainDesc);
            }
            return out;
        }

        private String trimMethodDescription(StringBuilder sb) {
            String out = sb.toString();
            if (canonicalNames) {
                out = out.replaceAll("java.lang.", "");
            }
            return out;
        }

        private CmdDesc checkSyntax(CmdLine line) {
            CmdDesc out = new CmdDesc();
            int openingRound = Brackets.indexOfOpeningRound(line.getHead());
            if (openingRound == -1) {
                return out;
            }
            loadStatementVars(line.getHead());
            Brackets brackets = new Brackets(line.getHead().substring(0, openingRound));
            int eqsep = Helpers.statementBegin(brackets);
            int end = line.getHead().length();
            if (eqsep > 0 && Helpers.constructorStatement(line.getHead().substring(0, eqsep))) {
                eqsep = line.getHead().substring(0, eqsep).lastIndexOf("new") - 1;
            } else if (line.getHead().substring(eqsep + 1).matches("\\s*for\\s*\\(.*")
                    || line.getHead().substring(eqsep + 1).matches("\\s*while\\s*\\(.*")
                    || line.getHead().substring(eqsep + 1).matches("\\s*else\\s+if\\s*\\(.*")
                    || line.getHead().substring(eqsep + 1).matches("\\s*if\\s*\\(.*")) {
                eqsep = openingRound;
                end = end - 1;
            } else if (line.getHead().substring(eqsep + 1).matches("\\s*switch\\s*\\(.*")
                    || line.getHead().substring(eqsep + 1).matches("\\s*catch\\s*\\(.*")) {
                return out;
            }
            List<AttributedString> mainDesc = new ArrayList<>();
            String objEquation = line.getHead().substring(eqsep + 1, end);
            equationLines = objEquation.split("\\r?\\n");
            cuttedSize = eqsep + 1;
            if (objEquation != null) {
                try {
                    execute(objEquation);
                } catch (groovy.lang.MissingPropertyException e) {
                    mainDesc.addAll(doExceptionMessage(e));
                    out.setErrorPattern(Pattern.compile("\\b" + e.getProperty() + "\\b"));
                } catch (java.util.regex.PatternSyntaxException e) {
                    mainDesc.addAll(doExceptionMessage(e));
                    int idx = line.getHead().lastIndexOf(e.getPattern());
                    if (idx >= 0) {
                        out.setErrorIndex(idx + e.getIndex());
                    }
                } catch (org.codehaus.groovy.control.MultipleCompilationErrorsException e){
                    if (e.getErrorCollector().getErrors() != null) {
                        for (Object o: e.getErrorCollector().getErrors()) {
                            if (o instanceof SyntaxErrorMessage) {
                                SyntaxErrorMessage sem = (SyntaxErrorMessage)o;
                                out.setErrorIndex(errorIndex(e.getMessage(), sem.getCause()));
                            } else {
                                mainDesc.add(new AttributedString("Error: " + o.getClass().getCanonicalName()
                                                                 , AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)));
                            }
                        }
                    }
                    if (e.getErrorCollector().getWarnings() != null) {
                        for (Object o: e.getErrorCollector().getWarnings()) {
                            if (o instanceof SyntaxErrorMessage) {
                                SyntaxErrorMessage sem = (SyntaxErrorMessage)o;
                                out.setErrorIndex(errorIndex(e.getMessage(), sem.getCause()));
                            } else {
                                mainDesc.add(new AttributedString("Warning: " + o.getClass().getCanonicalName()
                                                                , AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)));
                            }
                        }
                    }
                    mainDesc.addAll(doExceptionMessage(e));
                } catch (Exception e) {
                    mainDesc.addAll(doExceptionMessage(e));
                }
            }
            out.setMainDesc(mainDesc);
            return out;
        }

        private static List<AttributedString> doExceptionMessage(Exception exception) {
            List<AttributedString> out = new ArrayList<>();
            Pattern header = Pattern.compile("^[a-zA-Z() ]{3,}:(\\s+|$)");
            out.add(new AttributedString(exception.getClass().getCanonicalName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)));
            if (exception.getMessage() != null) {
                for (String s: exception.getMessage().split("\\r?\\n")) {
                    if (s.length() > 80) {
                        boolean doHeader = true;
                        int start = 0;
                        for (int i = 80; i < s.length(); i++) {
                            if ((s.charAt(i) == ' ' && i - start > 80 ) || i - start > 100) {
                                AttributedString as = new AttributedString(s.substring(start, i), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                                if (doHeader) {
                                    as = as.styleMatches(header, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
                                    doHeader = false;
                                }
                                out.add(as);
                                start = i;
                                if (s.length() - start < 80) {
                                    out.add(new AttributedString(s.substring(start), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)));
                                    break;
                                }
                            }
                        }
                        if (doHeader) {
                            AttributedString as = new AttributedString(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                            as = as.styleMatches(header, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
                            out.add(as);
                        }
                    } else {
                        AttributedString as = new AttributedString(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                        as = as.styleMatches(header, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
                        out.add(as);

                    }
                }
            }
            return out;
        }

        private int errorIndex(String message, SyntaxException se) {
            int out = -1;
            String line = null;
            String[] mlines = message.split("\n");
            for (int i = 0; i < mlines.length; i++) {
                if (mlines[i].matches(".*Script[0-9]+.groovy: .*")) {
                    line = mlines[i + 1].trim();
                    break;
                }
            }
            int tot = 0;
            if (line != null) {
                for (String l: equationLines) {
                    if (l.contains(line)) {
                        break;
                    }
                    tot += l.length() + 1;
                }
            }
            out = cuttedSize + tot + se.getStartColumn() - 1;
            return out;
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
        static final List<Character> DELIMS = Arrays.asList('+', '-', '*', '=', '/');
        static char[] quote = {'"', '\''};
        Deque<Integer> roundOpen = new ArrayDeque<>();
        Deque<Integer> curlyOpen = new ArrayDeque<>();
        Map<Integer,Integer> lastComma = new HashMap<>();
        int lastRoundClose = -1;
        int lastCurlyClose = -1;
        int lastSemicolon = -1;
        int lastBlanck = -1;
        int lastDelim = -1;
        int quoteId = -1;
        int round = 0;
        int curly = 0;
        int rounds = 0;
        int curlies = 0;

        public Brackets(String line) {
            int pos = -1;
            char prevChar = ' ';
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
                    curlyOpen.add(pos);
                } else if (ch == '}') {
                    curlies++;
                    curly--;
                    curlyOpen.removeLast();
                    lastCurlyClose = pos;
                } else if (ch == ',' && !roundOpen.isEmpty()) {
                    lastComma.put(roundOpen.getLast(), pos);
                } else if (ch == ';' || ch == '\n') {
                    lastSemicolon = pos;
                } else if (ch == ' ' && round == 0 && String.valueOf(prevChar).matches("\\w")) {
                    lastBlanck = pos;
                } else if (DELIMS.contains(ch)) {
                    lastDelim = pos;
                }
                prevChar = ch;
                if (round < 0 || curly < 0) {
                    throw new IllegalArgumentException();
                }
            }
        }

        public static int indexOfOpeningRound(String line) {
            int out = -1;
            if (!line.endsWith(")")) {
                return out;
            }
            int quoteId = -1;
            int round = 0;
            int curly = 0;
            char[] chars = line.toCharArray();
            for (int i = line.length() - 1; i >= 0; i--) {
                char ch = chars[i];
                if (quoteId < 0) {
                    for (int j = 0; j < quote.length; j++) {
                        if (ch == quote[j]) {
                            quoteId = j;
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
                } else if (ch == ')') {
                    round--;
                } else if (ch == '{') {
                    curly++;
                } else if (ch == '}') {
                    curly--;
                }
                if (curly == 0 && round == 0) {
                    out = i;
                    break;
                }
            }
            return out;
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

        public int lastOpenRound() {
            return !roundOpen.isEmpty() ? roundOpen.getLast() : -1;
        }

        public int lastCloseRound() {
            return lastRoundClose;
        }

        public int lastOpenCurly() {
            return !curlyOpen.isEmpty() ? curlyOpen.getLast() : -1;
        }

        public int lastCloseCurly() {
            return lastCurlyClose;
        }

        public int lastComma() {
            int last = lastOpenRound();
            return lastComma.containsKey(last) ? lastComma.get(last) : -1;
        }

        public int lastSemicolon() {
            return lastSemicolon;
        }

        public int lastDelim() {
            return lastDelim;
        }

        public boolean openQuote() {
            return quoteId != -1;
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
