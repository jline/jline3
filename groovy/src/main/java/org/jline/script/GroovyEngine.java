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
import java.util.stream.Collectors;

import groovy.lang.*;
import org.apache.groovy.ast.tools.ImmutablePropertyUtils;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.jline.builtins.Nano.SyntaxHighlighter;
import org.jline.builtins.Styles;
import org.jline.console.CmdDesc;
import org.jline.console.CmdLine;
import org.jline.console.ScriptEngine;
import org.jline.console.SystemRegistry;
import org.jline.groovy.ObjectInspector;
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
import org.jline.utils.OSUtils;
import org.jline.utils.StyleResolver;

/**
 * Implements Groovy ScriptEngine.
 * You must be very careful when using GroovyEngine in a multithreaded environment. The Binding instance is not
 * thread safe, and it is shared by all scripts.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class GroovyEngine implements ScriptEngine {
    public enum Format {JSON, GROOVY, NONE}

    public static final String CANONICAL_NAMES = "canonicalNames";
    public static final String NANORC_SYNTAX = "nanorcSyntax";
    public static final String NANORC_VALUE = "nanorcValue";
    public static final String GROOVY_COLORS = "GROOVY_COLORS";
    public static final String NO_SYNTAX_CHECK = "noSyntaxCheck";
    public static final String RESTRICTED_COMPLETION = "restrictedCompletion";
    public static final String ALL_FIELDS_COMPLETION = "allFieldsCompletion";
    public static final String ALL_METHODS_COMPLETION = "allMethodsCompletion";
    public static final String ALL_CONSTRUCTORS_COMPLETION = "allConstructorsCompletion";
    public static final String ALL_CLASSES_COMPLETION = "allClassesCompletion";
    public static final String IDENTIFIERS_COMPLETION = "identifiersCompletion";
    public static final String META_METHODS_COMPLETION = "metaMethodsCompletion";

    private static final String VAR_GROOVY_OPTIONS = "GROOVY_OPTIONS";
    private static final String REGEX_SYSTEM_VAR = "[A-Z]+[A-Z_]*";
    private static final String REGEX_VAR = "[a-zA-Z_]+[a-zA-Z0-9_]*";
    private static final Pattern PATTERN_FUNCTION_DEF = Pattern.compile(
                                      "^def\\s+(" + REGEX_VAR + ")\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?}(|\n)$"
                                           , Pattern.DOTALL);
    private static final Pattern PATTERN_CLASS_DEF = Pattern.compile("^class\\s+(" + REGEX_VAR + ") .*?\\{.*?}(|\n)$"
                                                                  , Pattern.DOTALL);
    private static final Pattern PATTERN_CLASS_NAME = Pattern.compile("(.*?)\\.([A-Z].*)");
    private static final List<String> DEFAULT_IMPORTS = Arrays.asList("java.lang.*", "java.util.*", "java.io.*"
                                                     , "java.net.*", "groovy.lang.*", "groovy.util.*"
                                                     , "java.math.BigInteger", "java.math.BigDecimal");
    private final Map<String,Class<?>> defaultNameClass = new HashMap<>();
    private final GroovyShell shell;
    protected Binding sharedData;
    private final Map<String,String> imports = new HashMap<>();
    private final Map<String,String> methods = new HashMap<>();
    private final Map<String,Class<?>> nameClass;
    private Cloner objectCloner = new ObjectCloner();

    public interface Cloner {
        Object clone(Object obj);
        void markCache();
        void purgeCache();
    }

    public GroovyEngine() {
        this.sharedData = new Binding();
        shell = new GroovyShell(sharedData);
        for (String s : DEFAULT_IMPORTS) {
            addToNameClass(s, defaultNameClass);
        }
        nameClass = new HashMap<>(defaultNameClass);
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
                                // ignore
                            }
                        } else {
                            out = Utils.toObject(value); // try json
                        }
                    }
                } else if (value.startsWith("{") && value.endsWith("}")) {
                    out = Utils.toObject(value);
                }
            } catch (Exception e) {
                // ignore
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

    private static Set<Class<?>> classesForPackage(String pckgname) throws ClassNotFoundException {
        String name = pckgname;
        Matcher matcher = PATTERN_CLASS_NAME.matcher(name);
        if (matcher.matches()) {
            name = matcher.group(1) + ".**";
        }
        Set<Class<?>> out = new HashSet<>(PackageHelper.getClassesForPackage(name));
        if (out.isEmpty()) {
            out.addAll(JrtJavaBasePackages.getClassesForPackage(name));
        }
        return out;
    }

    private void addToNameClass(String name) {
        addToNameClass(name, nameClass);
    }

    private void addToNameClass(String name, Map<String,Class<?>> nameClass) {
        try {
            if (name.endsWith(".*")) {
                for (Class<?> c : classesForPackage(name)) {
                    nameClass.put(c.getSimpleName(), c);
                }
            } else {
                Class<?> clazz = classResolver(name);
                if (clazz != null) {
                    nameClass.put(clazz.getSimpleName(), clazz);
                }
            }
        } catch (Exception e) {
            // ignore
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
            StringBuilder e = new StringBuilder();
            for (Map.Entry<String, String> entry : imports.entrySet()) {
                e.append(entry.getValue()).append("\n");
            }
            e.append(statement);
            if (classDef(statement)) {
                e.append("; null");
            }
            out = shell.evaluate(e.toString());
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
        return Collections.singletonList("groovy");
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

    private boolean classDef(String statement) {
        return PATTERN_CLASS_DEF.matcher(statement).matches();
    }

    private void refreshNameClass() {
        nameClass.clear();
        nameClass.putAll(defaultNameClass);
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
            methods.remove(var);
        } else if (!var.contains(".") && var.contains("*")) {
            for (String v : internalFind(var)){
                if (sharedData.hasVariable(v) && !v.equals("_") && !v.matches(REGEX_SYSTEM_VAR)) {
                    sharedData.getVariables().remove(v);
                    methods.remove(v);
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
    protected Map<String,Object> groovyOptions() {
        return hasVariable(VAR_GROOVY_OPTIONS) ? (Map<String, Object>) get(VAR_GROOVY_OPTIONS)
                                                       : new HashMap<>();
    }

    protected <T>T groovyOption(String option, T defval) {
        return groovyOption(groovyOptions(), option, defval);
    }

    @SuppressWarnings("unchecked")
    protected static <T>T groovyOption(Map<String,Object> options, String option, T defval) {
        T out = defval;
        try {
            out = (T) options.getOrDefault(option, defval);
        } catch (Exception e) {
            // ignore
        }
        return out;
    }

    private Completer compileCompleter() {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(new StringsCompleter("class", "print", "println"), NullCompleter.INSTANCE));
        completers.add(new ArgumentCompleter(new StringsCompleter("def"), new StringsCompleter(methods::keySet)
                                           , NullCompleter.INSTANCE));
        completers.add(new ArgumentCompleter(new StringsCompleter("import")
                                           , new PackageCompleter(CandidateType.PACKAGE, this), NullCompleter.INSTANCE));
        completers.add(new MethodCompleter(this));
        return new AggregateCompleter(completers);
    }

    private enum CandidateType {CONSTRUCTOR, STATIC_METHOD, PACKAGE, METHOD, FIELD, IDENTIFIER, META_METHOD, OTHER}

    private static Class<?> classResolver(String classDotName) {
        Class<?> out = null;
        Matcher matcher = PATTERN_CLASS_NAME.matcher(classDotName);
        if (matcher.matches()) {
            String classname = matcher.group(2).replaceAll("\\.", "\\$");
            try {
                out = Class.forName(matcher.group(1) + "." + classname);
            } catch (ClassNotFoundException ex) {
                if (Log.isDebugEnabled()) {
                    ex.printStackTrace();
                }
            }
        }
        return out;
    }

    protected static class AccessRules {
        protected final boolean allMethods;
        protected final boolean allFields;
        protected final boolean allConstructors;
        protected final boolean allClasses;

        public AccessRules() {
            this(new HashMap<>());
        }

        public AccessRules(Map<String,Object> options) {
            this.allMethods = groovyOption(options, ALL_METHODS_COMPLETION, false);
            this.allFields = groovyOption(options, ALL_FIELDS_COMPLETION, false);
            this.allConstructors = groovyOption(options, ALL_CONSTRUCTORS_COMPLETION, false);
            this.allClasses = groovyOption(options, ALL_CLASSES_COMPLETION, false);
        }
    }

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

        public static Set<Method> getClassMethods(Class<?> clazz, boolean all) {
            Set<Method> out = new HashSet<>(Arrays.asList(clazz.getMethods()));
            if (all) {
                out.addAll(Arrays.asList(clazz.getDeclaredMethods()));
            }
            return out;
        }

        public static Set<String> getMethods(Class<?> clazz, boolean all) {
            return getMethods(clazz, all, false, false);
        }

        public static Set<String> getStaticMethods(Class<?> clazz, boolean all) {
            return getMethods(clazz, all, true, false);
        }

        public static boolean noStaticMethods(Class<?> clazz, boolean all) {
            return getMethods(clazz, all, true, true).isEmpty();
        }

        private static Set<String> getMethods(Class<?> clazz, boolean all, boolean statc, boolean firstOnly) {
            Set<String> out = new HashSet<>();
            try {
                for (Method method : getClassMethods(clazz, all)) {
                    if ((statc && Modifier.isStatic(method.getModifiers()))
                            || (!statc && !Modifier.isStatic(method.getModifiers()))) {
                        out.add(method.getName());
                        if (firstOnly) {
                            break;
                        }
                    }
                }
            } catch (NoClassDefFoundError e) {
                // ignore
            }
            return out;
        }

        public static Map<String,String> getFields(Class<?> clazz, boolean all) {
            return getFields(clazz, all, false, false);
        }

        public static Map<String,String> getStaticFields(Class<?> clazz, boolean all) {
            return getFields(clazz, all, true, false);
        }

        public static boolean noStaticFields(Class<?> clazz, boolean all) {
            return getFields(clazz, all, true, true).isEmpty();
        }

        private static Map<String,String> getFields(Class<?> clazz, boolean all, boolean statc, boolean firstOnly) {
            Map<String,String> out = new HashMap<>();
            for (Field field : all ? clazz.getDeclaredFields() : clazz.getFields()) {
                if ((statc && Modifier.isStatic(field.getModifiers()))
                        || (!statc && !Modifier.isStatic(field.getModifiers()))) {
                    out.put(field.getName(), field.getType().getSimpleName());
                    if (firstOnly) {
                        break;
                    }
                }
            }
            return out;
        }

        public static Set<String> nextDomain(String domain, CandidateType type) {
            return nextDomain(domain, new AccessRules(), type);
        }

        public static Set<String> nextDomain(String domain, AccessRules access, CandidateType type) {
            Set<String> out = new HashSet<>();
            if (domain.isEmpty()) {
                for (String p : loadedPackages()) {
                    out.add(p.split("\\.")[0]);
                }
            } else if ((domain.split("\\.")).length < 2) {
                out = names(domain);
            } else {
                try {
                    for (Class<?> c : classesForPackage(domain)) {
                        try {
                            if ((!Modifier.isPublic(c.getModifiers()) && !access.allClasses) || c.getCanonicalName() == null) {
                                continue;
                            }
                            if ((type == CandidateType.CONSTRUCTOR && (c.getConstructors().length == 0
                                    || Modifier.isAbstract(c.getModifiers())))
                                    || (type == CandidateType.STATIC_METHOD && noStaticMethods(c, access.allMethods)
                                         && noStaticFields(c, access.allFields))) {
                                continue;
                            }
                            String name = c.getCanonicalName();
                            Log.debug(name);
                            if (name.startsWith(domain)) {
                                int idx = name.indexOf('.', domain.length());
                                if (idx < 0) {
                                    idx = name.length();
                                }
                                out.add(name.substring(domain.length(), idx));
                            }
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

        private static Map<String, String> listToMap(Collection<String> list) {
            return list.stream()
                    .collect(Collectors.toMap(it -> it, it -> ""));
        }

        public static void doCandidates(List<Candidate> candidates, Collection<String> fields, String curBuf, CandidateType type) {
            doCandidates(candidates, listToMap(fields), curBuf, type);
        }

        public static void doCandidates(List<Candidate> candidates, Map<String,String> fields, String curBuf, CandidateType type) {
            if (fields == null) {
                return;
            }
            for (Map.Entry<String,String> entry : fields.entrySet()) {
                String group = null;
                String desc = entry.getValue().isEmpty() ? null : entry.getValue();
                String s = entry.getKey();
                if (s == null) {
                    continue;
                }
                String postFix = "";
                if (type == CandidateType.CONSTRUCTOR) {
                    if (s.matches("[a-z]+.*")) {
                        postFix = ".";
                    } else if (s.matches("[A-Z]+.*")) {
                        postFix = "(";
                    }
                } else if (type == CandidateType.PACKAGE) {
                    if (s.matches("[a-z]+.*")) {
                        postFix = ".";
                    }
                } else if (type == CandidateType.METHOD) {
                    postFix = "(";
                    group = "Methods";
                } else if (type == CandidateType.FIELD) {
                    group = "Fields";
                } else if (type == CandidateType.IDENTIFIER) {
                    group = "Identifiers";
                    if (s.contains("-") || s.contains("+") || s.contains(" ") || s.contains("#")
                            || !s.matches("[a-zA-Z$_].*")){
                        continue;
                    }
                } else if (type == CandidateType.META_METHOD) {
                    postFix = "(";
                    group = "MetaMethods";
                }
                candidates.add(new Candidate(AttributedString.stripAnsi(curBuf + s + postFix), s, group, desc, null
                             ,null, false));
            }
        }

        public static int statementBegin(String buffer) {
            String buf = buffer;
            while (buf.matches(".*\\)\\.\\w+$")) {
                int idx = buf.lastIndexOf(".");
                int openingRound = Brackets.indexOfOpeningRound(buf.substring(0,idx));
                buf = buf.substring(0,openingRound);
            }
            return statementBegin(new Brackets(buf));
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

        private static int statementBegin(Brackets brackets) {
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
        private final CandidateType type;
        private final GroovyEngine groovyEngine;

        public PackageCompleter(CandidateType type, GroovyEngine groovyEngine) {
            this.type = type;
            this.groovyEngine = groovyEngine;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            String buffer = commandLine.word().substring(0, commandLine.wordCursor());
            String curBuf = "";
            int lastDelim = buffer.lastIndexOf('.');
            if (lastDelim > -1) {
                curBuf = buffer.substring(0, lastDelim + 1);
            }
            Helpers.doCandidates(candidates
                               , Helpers.nextDomain(curBuf, new AccessRules(groovyEngine.groovyOptions()), type)
                               , curBuf, type);
        }

    }

    private static class MethodCompleter implements Completer {
        private static final List<String> VALUES = Arrays.asList("true", "false");
        private final GroovyEngine groovyEngine;
        private final SystemRegistry systemRegistry = SystemRegistry.get();
        private Inspector inspector;
        private AccessRules access;
        private boolean metaMethodCompletion;
        private boolean identifierCompletion;

        public MethodCompleter(GroovyEngine engine){
            this.groovyEngine = engine;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            if (systemRegistry.isCommandOrScript(commandLine)
                    || (commandLine.wordIndex() > 0 && commandLine.words().get(0).equals("import"))) {
                return;
            }
            String wordbuffer = commandLine.word();
            String buffer = commandLine.line().substring(0, commandLine.cursor());
            Brackets brackets;
            try {
                brackets = new Brackets(buffer);
            } catch (Exception e) {
                return;
            }
            if (brackets.openQuote()) {
                return;
            }
            boolean restrictedCompletion = groovyEngine.groovyOption(RESTRICTED_COMPLETION, false);
            metaMethodCompletion = groovyEngine.groovyOption(META_METHODS_COMPLETION, false);
            identifierCompletion = groovyEngine.groovyOption(IDENTIFIERS_COMPLETION, false);
            access = new AccessRules(groovyEngine.groovyOptions());
            inspector = new Inspector(groovyEngine);
            inspector.loadStatementVars(buffer);
            int eqsep = Helpers.statementBegin(buffer);
            if (brackets.numberOfRounds() > 0 && brackets.lastCloseRound() > eqsep) {
                int varsep = buffer.lastIndexOf('.');
                if (varsep > 0 && varsep > brackets.lastCloseRound() && !restrictedCompletion) {
                    Class<?> clazz = inspector.evaluateClass(buffer.substring(eqsep + 1, varsep));
                    Object involvedObject = inspector.getInvolvedObject();
                    int vs = wordbuffer.lastIndexOf('.');
                    String curBuf = wordbuffer.substring(0, vs + 1);
                    doMethodCandidates(candidates, involvedObject == null ? clazz : involvedObject, curBuf);
                }
            } else if (completingConstructor(commandLine)) {
                if (wordbuffer.matches("[a-z]+.*")) {
                    int idx = wordbuffer.lastIndexOf('.');
                    if (idx > 0 && wordbuffer.substring(idx + 1).matches("[A-Z]+.*")) {
                        try {
                            Class.forName(wordbuffer);
                            Helpers.doCandidates(candidates, Collections.singletonList("("), wordbuffer, CandidateType.OTHER);
                        } catch (Exception e) {
                            String param = wordbuffer.substring(0, idx + 1);
                            Helpers.doCandidates(candidates
                                               , Helpers.nextDomain(param, CandidateType.CONSTRUCTOR)
                                               , param, CandidateType.CONSTRUCTOR);
                        }
                    } else {
                        new PackageCompleter(CandidateType.CONSTRUCTOR, groovyEngine).complete(reader, commandLine, candidates);
                    }
                } else {
                    Helpers.doCandidates(candidates, retrieveConstructors(access.allConstructors), ""
                            , CandidateType.CONSTRUCTOR);
                }
            } else {
                boolean addKeyWords = eqsep == brackets.lastSemicolon() || eqsep == brackets.lastOpenCurly();
                int varsep = wordbuffer.lastIndexOf('.');
                eqsep = Helpers.statementBegin(buffer, wordbuffer, brackets);
                String param = wordbuffer.substring(eqsep + 1);
                if (param.trim().length() == 0) {
                    // do nothing
                } else if (varsep < 0 || varsep < eqsep) {
                    String curBuf = wordbuffer.substring(0, eqsep + 1);
                    if (addKeyWords) {
                        Helpers.doCandidates(candidates, ObjectInspector.GLOBAL_META_METHODS, curBuf, CandidateType.METHOD);
                    } else {
                        Helpers.doCandidates(candidates, VALUES, curBuf, CandidateType.OTHER);
                    }
                    Helpers.doCandidates(candidates, inspector.variables(), curBuf, CandidateType.OTHER);
                    Helpers.doCandidates(candidates, retrieveClassesWithStaticMethods(), curBuf, CandidateType.PACKAGE);
                } else {
                    boolean firstMethod = param.indexOf('.') == param.lastIndexOf('.');
                    String var = param.substring(0, param.indexOf('.'));
                    String curBuf = wordbuffer.substring(0, varsep + 1);
                    if (inspector.nameClass().containsKey(var)) {
                        if (firstMethod) {
                            doStaticMethodCandidates(candidates, inspector.nameClass().get(var), curBuf);
                        } else if (!restrictedCompletion) {
                            Class<?> clazz = inspector.evaluateClass(wordbuffer.substring(eqsep + 1, varsep));
                            Object involvedObject = inspector.getInvolvedObject();
                            doMethodCandidates(candidates, involvedObject == null ? clazz : involvedObject, curBuf);
                        }
                    } else if (inspector.hasVariable(var)) {
                        if (firstMethod) {
                            doMethodCandidates(candidates, inspector.getVariable(var), curBuf);
                        } else if (!restrictedCompletion) {
                            Class<?> clazz = inspector.evaluateClass(wordbuffer.substring(eqsep + 1, varsep));
                            Object involvedObject = inspector.getInvolvedObject();
                            doMethodCandidates(candidates, involvedObject == null ? clazz : involvedObject, curBuf);
                        }
                    } else {
                        try {
                            param = wordbuffer.substring(eqsep + 1, varsep);
                            Class<?> clazz = classResolver(param);
                            if (clazz != null) {
                                doStaticMethodCandidates(candidates, clazz, curBuf);
                            }
                        } catch (Exception e) {
                            // ignore
                        } finally {
                            param = wordbuffer.substring(eqsep + 1, varsep + 1);
                            Helpers.doCandidates(candidates
                                    , Helpers.nextDomain(param, CandidateType.STATIC_METHOD)
                                    , curBuf, CandidateType.PACKAGE);
                        }
                    }
                }
            }
        }

        private boolean completingConstructor(ParsedLine commandLine) {
            return !commandLine.word().contains("(") && (
                    (commandLine.wordIndex() == 1 && commandLine.words().get(0).matches("(new|\\w+=[{]?new)"))
                          ||
                    (commandLine.wordIndex() > 1
                            && Helpers.constructorStatement(commandLine.words().get(commandLine.wordIndex() - 1)))
                    );
        }

        @SuppressWarnings("unchecked")
        private void doIdentifierCandidates(List<Candidate> candidates, Object object, String curBuf) {
            if (!(object instanceof Map)) {
                return;
            }
            Map<?,?> map = (Map<?,?>)object;
            if (map.isEmpty() || !(map.keySet().iterator().next() instanceof String)) {
                return;
            }
            Helpers.doCandidates(candidates, (Set<String>)map.keySet(), curBuf, CandidateType.IDENTIFIER);
        }

        private Set<String> doMetaMethodCandidates(List<Candidate> candidates, Object object, String curBuf) {
            ObjectInspector inspector = new ObjectInspector(object);
            List<Map<String,String>> mms = inspector.metaMethods(false);
            Set<String> metaMethods = new HashSet<>();
            for (Map<String,String> mm : mms) {
                metaMethods.add(mm.get(ObjectInspector.FIELD_NAME));
            }
            Helpers.doCandidates(candidates, metaMethods, curBuf, CandidateType.META_METHOD);
            return metaMethods;
        }

        private void doMethodCandidates(List<Candidate> candidates, Object object, String curBuf) {
            if (object == null) {
                return;
            }
            Set<String> metaMethods = null;
            if (identifierCompletion) {
                doIdentifierCandidates(candidates, object, curBuf);
            }
            if (metaMethodCompletion) {
                metaMethods = doMetaMethodCandidates(candidates, object, curBuf);
            }
            doMethodCandidates(candidates, object.getClass(), curBuf
                    , identifierCompletion  && !(object instanceof Map), metaMethods);
        }

        private void doMethodCandidates(List<Candidate> candidates, Class<?> clazz, String curBuf, boolean addIdentifiers
                , Set<String> metaMethods) {
            if (clazz == null) {
                return;
            }
            Set<String> methods = Helpers.getMethods(clazz, access.allMethods);
            if (addIdentifiers) {
                Set<String> identifiers = new HashSet<>();
                for (String m : methods) {
                    if (m.matches("get[A-Z].*")) {
                        Class<?> cc = clazz;
                        while (cc != null) {
                            try {
                                try {
                                    cc.getMethod(m);
                                } catch (NoSuchMethodException exp) {
                                    cc.getDeclaredMethod(m);
                                }
                                char[] c = m.substring(3).toCharArray();
                                c[0] = Character.toLowerCase(c[0]);
                                identifiers.add(new String(c));
                                break;
                            } catch (NoSuchMethodException e) {
                                cc = cc.getSuperclass();
                            }
                        }
                    }
                }
                Helpers.doCandidates(candidates, identifiers, curBuf, CandidateType.IDENTIFIER);
            }
            if (metaMethods != null) {
                for (String mm : metaMethods) {
                    methods.remove(mm);
                }
            }
            Helpers.doCandidates(candidates, methods, curBuf, CandidateType.METHOD);
            Helpers.doCandidates(candidates, Helpers.getFields(clazz, access.allFields), curBuf, CandidateType.FIELD);
        }

        private void doStaticMethodCandidates(List<Candidate> candidates, Class<?> clazz, String curBuf) {
            if (clazz == null) {
                return;
            }
            Helpers.doCandidates(candidates, Helpers.getStaticMethods(clazz, access.allMethods), curBuf
                               , CandidateType.METHOD);
            Helpers.doCandidates(candidates, Helpers.getStaticFields(clazz, access.allFields), curBuf
                               , CandidateType.FIELD);
        }

        private Set<String> retrieveConstructors(boolean all) {
            Set<String> out = new HashSet<>();
            for (Iterator<Map.Entry<String, Class<?>>> it = inspector.nameClass().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Class<?>> entry = it.next();
                Class<?> c = entry.getValue();
                try {
                    if ((!all && c.getConstructors().length == 0) || (all && c.getDeclaredConstructors().length == 0)
                            || Modifier.isAbstract(c.getModifiers())) {
                        continue;
                    }
                    out.add(entry.getKey());
                } catch (NoClassDefFoundError e) {
                    it.remove();
                }
            }
            return out;
        }

        private Set<String> retrieveClassesWithStaticMethods() {
            Set<String> out = new HashSet<>();
            for (Iterator<Map.Entry<String, Class<?>>> it = inspector.nameClass().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Class<?>> entry = it.next();
                Class<?> c = entry.getValue();
                try {
                    if (Helpers.noStaticMethods(c, access.allMethods) && Helpers.noStaticFields(c, access.allFields)) {
                        continue;
                    }
                    out.add(entry.getKey());
                } catch (NoClassDefFoundError e) {
                    it.remove();
                }
            }
            return out;
        }
    }

    private static class Inspector {
        static final Pattern PATTERN_FOR = Pattern.compile("^for\\s*\\((.*?)");
        static final Pattern PATTERN_FOR_EACH = Pattern.compile("^for\\s*\\((.*?):(.*?)\\).*");
        static final Pattern PATTERN_LAMBDA = Pattern.compile(".*\\([(]*(.*?)[)]*->.*");
        static final Pattern PATTERN_FUNCTION_BODY = Pattern.compile("^\\s*\\(([a-zA-Z0-9_ ,]*)\\)\\s*\\{(.*)?}(|\n)$"
                                                                   , Pattern.DOTALL);
        static final Pattern PATTERN_FUNCTION = Pattern.compile("\\s*def\\s+\\w+\\s*\\((.*?)\\).*");
        static final Pattern PATTERN_CLOSURE = Pattern.compile(".*\\{(.*?)->.*");
        static final Pattern PATTERN_TYPE_VAR = Pattern.compile("(\\w+)\\s+(\\w+)");
        static final String DEFAULT_NANORC_SYNTAX = "classpath:/org/jline/groovy/java.nanorc";
        static final String DEFAULT_GROOVY_COLORS = "ti=1;34:me=31";

        private final GroovyShell shell;
        protected Binding sharedData = new Binding();
        private final Map<String,String> imports;
        private final Map<String,Class<?>> nameClass;
        private PrintStream nullstream;
        private boolean canonicalNames = false;
        private final boolean noSyntaxCheck;
        private final boolean restrictedCompletion;
        private final boolean metaMethodsCompletion;
        private final AccessRules access;
        private String[] equationLines;
        private int cuttedSize;
        private final String nanorcSyntax;
        private final String groovyColors;
        private Object involvedObject = null;

        public Inspector(GroovyEngine groovyEngine) {
            this.imports = groovyEngine.imports;
            this.nameClass = groovyEngine.nameClass;
            this.canonicalNames = groovyEngine.groovyOption(CANONICAL_NAMES, canonicalNames);
            this.nanorcSyntax = groovyEngine.groovyOption(NANORC_SYNTAX, DEFAULT_NANORC_SYNTAX);
            this.noSyntaxCheck = groovyEngine.groovyOption(NO_SYNTAX_CHECK, false);
            this.restrictedCompletion = groovyEngine.groovyOption(RESTRICTED_COMPLETION, false);
            this.metaMethodsCompletion = groovyEngine.groovyOption(META_METHODS_COMPLETION, false);
            this.access = new AccessRules(groovyEngine.groovyOptions());
            String gc = groovyEngine.groovyOption(GROOVY_COLORS, null);
            groovyColors = gc != null && Styles.isAnsiStylePattern(gc) ? gc : DEFAULT_GROOVY_COLORS;
            groovyEngine.getObjectCloner().markCache();
            for (Map.Entry<String, Object> entry : groovyEngine.find().entrySet()) {
                Object obj = groovyEngine.getObjectCloner().clone(entry.getValue());
                sharedData.setVariable(entry.getKey(), obj);
            }
            groovyEngine.getObjectCloner().purgeCache();
            shell = new GroovyShell(sharedData);
            try {
                File file = OSUtils.IS_WINDOWS ? new File("NUL") : new File("/dev/null");
                OutputStream outputStream = new FileOutputStream(file);
                nullstream = new PrintStream(outputStream);
            } catch (Exception e) {
                // ignore
            }
            for (Map.Entry<String,String> entry : groovyEngine.methods.entrySet()) {
                Matcher m = PATTERN_FUNCTION_BODY.matcher(entry.getValue());
                if (m.matches() && sharedData.hasVariable(entry.getKey())
                        && sharedData.getVariable(entry.getKey()) instanceof Closure) {
                    sharedData.setVariable(entry.getKey(), execute("{" + m.group(1) + "->" + m.group(2) + "}"));
                }
            }
        }

        public Object getInvolvedObject() {
            return involvedObject;
        }

        public Class<?> evaluateClass(String objectStatement) {
            Class<?> out = null;
            try {
                involvedObject = execute(objectStatement);
                out = involvedObject.getClass();
            } catch (Exception e) {
                // ignore
            }
            try {
                if (out == null || out == Class.class) {
                    if (!objectStatement.contains(".") ) {
                        out = (Class<?>)execute(objectStatement + ".class");
                    } else {
                        out = Class.forName(objectStatement);
                    }
                }
            } catch (Exception e) {
                // ignore
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
            Object out;
            try {
                StringBuilder e = new StringBuilder();
                for (Map.Entry<String, String> entry : imports.entrySet()) {
                    e.append(entry.getValue()).append("\n");
                }
                e.append(statement);
                out = shell.evaluate(e.toString());
            } finally {
                System.setOut(origOut);
                System.setErr(origErr);
            }
            return out;
        }

        private String stripVarType(String statement) {
            if (statement.matches("\\w+\\s+\\w+.*")) {
                int idx = statement.indexOf(' ');
                return statement.substring(idx + 1);
            }
            return statement;
        }

        private String defineArgs(String[] args) {
            StringBuilder out = new StringBuilder();
            for (String v : args) {
                Matcher matcher = PATTERN_TYPE_VAR.matcher(v.trim());
                if (matcher.matches()) {
                    out.append(constructVariable(matcher.group(1), matcher.group(2)));
                } else {
                    out.append(v).append(" = null; ");
                }
            }
            return out.toString();
        }

        private String constructVariable(String type, String name) {
            String out = "";
            if (type.matches("[B|b]yte") || type.matches("[S|s]hort")
                    || type.equals("int") || type.equals("Integer") || type.matches("[L|l]ong")
                    || type.matches("[F|f]loat") || type.matches("[D|d]ouble")
                    || type.matches("[B|b]oolean") || type.equals("char") || type.equals("Character")) {
                out = name + " = (" + type + ")0; ";
            } else if (type.matches("[A-Z].*")) {
                out = "try {" + name + " = new " + type + "() } catch (Exception e) {" + name + " = null}; ";
            }
            return out;
        }

        public void loadStatementVars(String line) {
            if (!new Brackets(line).openCurly()) {
                return;
            }
            for (String s : line.split("\\r?\\n|;")) {
                String statement = s.trim();
                boolean constructedStatement = true;
                try {
                    Matcher forEachMatcher = PATTERN_FOR_EACH.matcher(statement);
                    Matcher forMatcher = PATTERN_FOR.matcher(statement);
                    Matcher lambdaMatcher = PATTERN_LAMBDA.matcher(statement);
                    Matcher functionMatcher = PATTERN_FUNCTION.matcher(statement);
                    Matcher closureMatcher = PATTERN_CLOSURE.matcher(statement);
                    Matcher typeVarMatcher = PATTERN_TYPE_VAR.matcher(statement);
                    if (statement.matches("^(if|while)\\s*\\(.*") || statement.matches("(}\\s*|^)else(\\s*\\{|$)")
                            || statement.matches("(}\\s*|^)else\\s+if\\s*\\(.*") || statement.matches("^break[;]+")
                            || statement.matches("^case\\s+.*:") || statement.matches("^default\\s+:")
                            || statement.matches("([{}])") || statement.length() == 0) {
                        continue;
                    } else if (forEachMatcher.matches()) {
                        statement = stripVarType(forEachMatcher.group(1).trim());
                        String cc = forEachMatcher.group(2);
                        statement += "=" + cc + " instanceof Map ? " + cc + ".entrySet()[0] : " + cc + "[0]";
                    } else if (forMatcher.matches()) {
                        statement = stripVarType(forMatcher.group(1).trim());
                        if (!statement.contains("=")) {
                            statement += " = null";
                        }
                    } else if (closureMatcher.matches()) {
                        statement = defineArgs(closureMatcher.group(1).split(","));
                    } else if (functionMatcher.matches()) {
                        statement = defineArgs(functionMatcher.group(1).split(","));
                    } else if (lambdaMatcher.matches()) {
                        statement = defineArgs(lambdaMatcher.group(1).split(","));
                    } else if (statement.contains("=")) {
                        statement = stripVarType(statement);
                        constructedStatement = false;
                    } else if (typeVarMatcher.matches()) {
                        statement = constructVariable(typeVarMatcher.group(1), typeVarMatcher.group(2));
                    }
                    Brackets br = new Brackets(statement);
                    if (statement.contains("=") && !br.openRound() && !br.openCurly() && !br.openSquare()) {
                        int idx = statement.indexOf('=');
                        String st = "null";
                        if (restrictedCompletion && !constructedStatement && br.numberOfRounds() > 0) {
                            statement = statement.substring(0, idx + 1) + "null";
                        } else {
                            st = statement.substring(idx + 1).trim();
                        }
                        if (!st.isEmpty() && !st.equals("new") ) {
                            execute(statement);
                        }
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
                    if (!noSyntaxCheck) {
                        out = checkSyntax(line);
                    }
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

        private String accessModifier(int modifier, boolean all) {
            String out = "";
            if (!all) {
                return out;
            }
            if (Modifier.isPrivate(modifier)) {
                out = "private ";
            } else if (Modifier.isProtected(modifier)) {
                out = "protected ";
            } else if (Modifier.isPublic(modifier)) {
                out = "public ";
            }
            return out;
        }

        private CmdDesc methodDescription(CmdLine line) {
            CmdDesc out = new CmdDesc();
            List<String> args = line.getArgs();
            boolean constructor = false;
            Class<?> clazz = null;
            String methodName = null;
            String buffer = line.getHead();
            int eqsep = Helpers.statementBegin(buffer);
            int varsep = buffer.lastIndexOf('.');
            if (varsep > 0 && varsep > eqsep) {
                loadStatementVars(buffer);
                methodName = buffer.substring(varsep + 1);
                int ior = Brackets.indexOfOpeningRound(buffer.substring(0, varsep));
                if (ior > 0 && ior < eqsep) {
                    eqsep = ior;
                }
                String st = buffer.substring(eqsep + 1, varsep);
                if (st.matches("[A-Z]+\\w+\\s*\\(.*")) {
                    st = "new " + st;
                }
                int nb = new Brackets(st).numberOfRounds();
                if (!restrictedCompletion || nb == 0) {
                    clazz = evaluateClass(st);
                }
            } else if (args.size() > 1 && Helpers.constructorStatement(args.get(args.size() - 2))
                    && args.get(args.size() - 1).matches("[A-Z]+\\w+\\s*\\(.*")
                    && new Brackets(args.get(args.size() - 1)).openRound()) {
                constructor = true;
                clazz = evaluateClass(trimName(args.get(args.size() - 1)));
            }
            List<AttributedString> mainDesc = new ArrayList<>();
            if (clazz != null) {
                SyntaxHighlighter java = SyntaxHighlighter.build(nanorcSyntax);
                mainDesc.add(java.highlight(clazz.toString()));
                if (constructor) {
                    for (Constructor<?> m : access.allConstructors ? clazz.getDeclaredConstructors()
                                                                   : clazz.getConstructors()) {
                        StringBuilder sb = new StringBuilder();
                        String name = m.getName();
                        if (!canonicalNames) {
                            int idx = name.lastIndexOf('.');
                            name = name.substring(idx + 1);
                        }
                        sb.append(accessModifier(m.getModifiers(), access.allConstructors));
                        sb.append(name);
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
                        mainDesc.add(java.highlight(trimMethodDescription(sb)));
                    }
                } else {
                    List<String> addedMethods = new ArrayList<>();
                    if (metaMethodsCompletion && involvedObject != null) {
                        for (Map<String,String> mm : new ObjectInspector(involvedObject).metaMethods(false)) {
                            if (!mm.get(ObjectInspector.FIELD_NAME).equals(methodName)) {
                                continue;
                            }
                            StringBuilder sb = new StringBuilder();
                            String modifiers = mm.get(ObjectInspector.FIELD_MODIFIERS);
                            if (!access.allMethods) {
                                if (modifiers.equals("public")) {
                                    modifiers = "";
                                } else if (modifiers.startsWith("public ")) {
                                    modifiers = modifiers.substring(7);
                                }
                            }
                            if (!modifiers.isEmpty()) {
                                sb.append(modifiers).append(" ");
                            }
                            sb.append(convertArrayParams(mm.get(ObjectInspector.FIELD_RETURN))).append(" ");
                            sb.append(methodName).append("(");
                            sb.append(convertArrayParams(mm.get(ObjectInspector.FIELD_PARAMETERS)));
                            sb.append(")");
                            if (!addedMethods.contains(sb.toString())) {
                                addedMethods.add(sb.toString());
                                mainDesc.add(java.highlight(trimMethodDescription(sb)));
                            }
                        }
                    }
                    do {
                        for (Method m : Helpers.getClassMethods(clazz, access.allMethods)) {
                            if (!m.getName().equals(methodName)) {
                                continue;
                            }
                            StringBuilder sb = new StringBuilder();
                            sb.append(accessModifier(m.getModifiers(), access.allMethods));
                            if (Modifier.isFinal(m.getModifiers())) {
                                sb.append("final ");
                            }
                            if (Modifier.isStatic(m.getModifiers())) {
                                sb.append("static ");
                            }
                            sb.append(canonicalNames ? m.getReturnType().getCanonicalName() : m.getReturnType().getSimpleName());
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

        private String convertArrayParams(String value) {
            String out = value.replaceAll("\\[B", "byte[]");
            Pattern arrayPattern = Pattern.compile("(.*)\\[L.*\\.([A-Z].*?);(.*)");
            Matcher matcher = arrayPattern.matcher(value);
            while (matcher.matches()) {
                out = matcher.group(1) + matcher.group(2) + "[]" + matcher.group(3);
                matcher = arrayPattern.matcher(out);
            }
            return out;
        }

        private String trimMethodDescription(StringBuilder sb) {
            String out = sb.toString();
            if (canonicalNames) {
                out = out.replaceAll("java\\.lang\\.", "");
            }
            return out;
        }

        private CmdDesc checkSyntax(CmdLine line) {
            CmdDesc out = new CmdDesc();
            int openingRound = Brackets.indexOfOpeningRound(line.getHead());
            if (openingRound == -1) {
                return out;
            }
            String cuttedLine = line.getHead().substring(0, openingRound);
            if (new Brackets(cuttedLine).openQuote()) {
                return out;
            }
            loadStatementVars(line.getHead());
            int eqsep = Helpers.statementBegin(cuttedLine);
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
                    || line.getHead().substring(eqsep + 1).matches("\\s*def\\s+\\w+\\s*\\(.*")
                    || line.getHead().substring(eqsep + 1).matches("\\s*catch\\s*\\(.*")) {
                return out;
            }
            List<AttributedString> mainDesc = new ArrayList<>();
            String objEquation = line.getHead().substring(eqsep + 1, end).trim();
            equationLines = objEquation.split("\\r?\\n");
            cuttedSize = eqsep + 1;
            if (objEquation.matches("\\(\\s*\\w+\\s*[,\\s*\\w+]*\\)")
                    || objEquation.matches("\\(\\s*\\)")) {
                // do nothing
            } else {
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
                } catch (org.codehaus.groovy.control.MultipleCompilationErrorsException e) {
                    if (e.getErrorCollector().getErrors() != null) {
                        for (Object o : e.getErrorCollector().getErrors()) {
                            if (o instanceof SyntaxErrorMessage) {
                                SyntaxErrorMessage sem = (SyntaxErrorMessage) o;
                                out.setErrorIndex(errorIndex(e.getMessage(), sem.getCause()));
                            }
                        }
                    }
                    if (e.getErrorCollector().getWarnings() != null) {
                        for (Object o : e.getErrorCollector().getWarnings()) {
                            if (o instanceof SyntaxErrorMessage) {
                                SyntaxErrorMessage sem = (SyntaxErrorMessage) o;
                                out.setErrorIndex(errorIndex(e.getMessage(), sem.getCause()));
                            }
                        }
                    }
                    mainDesc.addAll(doExceptionMessage(e));
                } catch (MissingMethodException e) {
                    if (!e.getMessage().split("\r?\n")[0].matches(".*types:\\s+\\(.*null.*\\).*")) {
                        mainDesc.addAll(doExceptionMessage(e));
                    }
                } catch (NullPointerException e) {
                    // do nothing
                } catch (Exception e) {
                    mainDesc.addAll(doExceptionMessage(e));
                }
            }
            out.setMainDesc(mainDesc);
            return out;
        }

        private List<AttributedString> doExceptionMessage(Exception exception) {
            List<AttributedString> out = new ArrayList<>();
            SyntaxHighlighter java = SyntaxHighlighter.build(nanorcSyntax);
            StyleResolver resolver = style(groovyColors);
            Pattern header = Pattern.compile("^[a-zA-Z() ]{3,}:(\\s+|$)");
            out.add(java.highlight(exception.getClass().getCanonicalName()));
            if (exception.getMessage() != null) {
                for (String s: exception.getMessage().split("\\r?\\n")) {
                    if (s.trim().length() == 0) {
                        // do nothing
                    } else if (s.length() > 80) {
                        boolean doHeader = true;
                        int start = 0;
                        for (int i = 80; i < s.length(); i++) {
                            if ((s.charAt(i) == ' ' && i - start > 80 ) || i - start > 100) {
                                AttributedString as = new AttributedString(s.substring(start, i), resolver.resolve(".me"));
                                if (doHeader) {
                                    as = as.styleMatches(header, resolver.resolve(".ti"));
                                    doHeader = false;
                                }
                                out.add(as);
                                start = i;
                                if (s.length() - start < 80) {
                                    out.add(new AttributedString(s.substring(start), resolver.resolve(".me")));
                                    break;
                                }
                            }
                        }
                        if (doHeader) {
                            AttributedString as = new AttributedString(s, resolver.resolve(".me"));
                            as = as.styleMatches(header, resolver.resolve(".ti"));
                            out.add(as);
                        }
                    } else {
                        AttributedString as = new AttributedString(s, resolver.resolve(".me"));
                        as = as.styleMatches(header, resolver.resolve(".ti"));
                        out.add(as);
                    }
                }
            }
            return out;
        }

        private int errorIndex(String message, SyntaxException se) {
            int out;
            String line = null;
            String[] mlines = message.split("\n");
            for (int i = 0; i < mlines.length; i++) {
                if (mlines[i].matches(".*Script[0-9]+\\.groovy: .*")) {
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

        private static StyleResolver style(String style) {
            Map<String, String> colors = Arrays.stream(style.split(":"))
                    .collect(Collectors.toMap(s -> s.substring(0, s.indexOf('=')),
                            s -> s.substring(s.indexOf('=') + 1)));
            return new StyleResolver(colors::get);
        }

    }

    private static class ObjectCloner implements Cloner {
        Map<String,Object> cache = new HashMap<>();
        Set<String> marked = new HashSet<>();

        public ObjectCloner() {

        }

        /**
         * Shallow copy of the object using java Cloneable clone() method.
         */
        public Object clone(Object obj) {
            if (obj == null || ImmutablePropertyUtils.builtinOrMarkedImmutableClass(obj.getClass())
                    || obj instanceof Exception || obj instanceof Closure) {
                return obj;
            }
            Object out;
            String key = cacheKey(obj);
            try {
                if (cache.containsKey(key)) {
                    marked.remove(key);
                    out = cache.get(key);
                } else {
                    Class<?> clazz = obj.getClass();
                    Method clone = clazz.getDeclaredMethod("clone");
                    out = clone.invoke(obj);
                    cache.put(key, out);
                }
            } catch (Exception e) {
                out = obj;
                cache.put(key, out);
            }
            return out;
        }

        public void markCache() {
            marked = new HashSet<>(cache.keySet());
        }

        public void purgeCache() {
            for (String k : marked) {
                cache.remove(k);
            }
        }

        private String cacheKey(Object obj) {
            return obj.getClass().getCanonicalName() + ":" + obj.hashCode();
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
        int square = 0;
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
                } else if (ch == '[') {
                    square++;
                } else if (ch == ']') {
                    square--;
                } else if (ch == ',' && !roundOpen.isEmpty()) {
                    lastComma.put(roundOpen.getLast(), pos);
                } else if (ch == ';' || ch == '\n' || (ch == '>' && prevChar == '-')) {
                    lastSemicolon = pos;
                } else if (ch == ' ' && round == 0 && String.valueOf(prevChar).matches("\\w")) {
                    lastBlanck = pos;
                } else if (DELIMS.contains(ch)) {
                    lastDelim = pos;
                }
                prevChar = ch;
                if (round < 0 || curly < 0 || square < 0) {
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

        public boolean openSquare() {
            return square > 0;
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
            return lastComma.getOrDefault(last, -1);
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
            return "rounds: " + rounds + "\n"
                 + "curlies: " + curlies + "\n"
                 + "lastOpenRound: " + lastOpenRound() + "\n"
                 + "lastCloseRound: " + lastRoundClose + "\n"
                 + "lastComma: " + lastComma() + "\n";
        }
    }

}
