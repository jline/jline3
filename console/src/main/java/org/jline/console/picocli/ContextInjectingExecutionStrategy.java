/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.picocli;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.Callable;

import org.jline.console.CommandContext;
import org.jline.utils.Log;

import picocli.CommandLine;
import picocli.CommandLine.ExecutionResult;
import picocli.CommandLine.IExecutionStrategy;
import picocli.CommandLine.ParseResult;

/**
 * Execution strategy that injects CommandContext into command methods.
 * <p>
 * This strategy wraps the default execution strategy and provides context injection
 * for commands that have methods accepting CommandContext parameters.
 * <p>
 * Supported injection patterns:
 * <ul>
 *   <li>Method parameter: {@code public Integer call(CommandContext context)}</li>
 *   <li>Method parameter with annotation: {@code public Integer call(@Context CommandContext ctx)}</li>
 * </ul>
 */
public class ContextInjectingExecutionStrategy implements IExecutionStrategy {

    private final CommandContext context;
    private final IExecutionStrategy delegate;

    /**
     * Creates a new context-injecting execution strategy.
     * @param context the context to inject
     */
    public ContextInjectingExecutionStrategy(CommandContext context) {
        this.context = context;
        this.delegate = new CommandLine.RunLast();
    }

    @Override
    public int execute(ParseResult parseResult) throws CommandLine.ExecutionException {
        // Inject context into the original command object before execution
        Object command = parseResult.commandSpec().commandLine().getCommand();
        injectContextIntoCommand(command);

        // Check if we need method parameter injection
        if (needsMethodParameterInjection(command)) {
            // Replace with wrapper that handles method parameter injection
            if (command instanceof Callable) {
                parseResult.commandSpec().commandLine().setCommand(new ContextInjectingCallable((Callable<?>) command));
            } else if (command instanceof Runnable) {
                parseResult.commandSpec().commandLine().setCommand(new ContextInjectingRunnable((Runnable) command));
            }
        }

        return delegate.execute(parseResult);
    }

    private void injectContextIntoCommand(Object command) {
        // Try field injection first
        injectContextIntoFields(command);
    }

    private boolean needsMethodParameterInjection(Object command) {
        return findMethodWithContext(command.getClass(), "call") != null ||
               findMethodWithContext(command.getClass(), "run") != null;
    }

    private void injectContextIntoFields(Object command) {
        Class<?> clazz = command.getClass();
        while (clazz != null) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (CommandContext.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        if (field.get(command) == null) { // Only inject if not already set
                            field.set(command, context);
                        }
                    } catch (Exception e) {
                        // Ignore injection failures
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Wrapper for Callable commands that supports context injection.
     */
    private class ContextInjectingCallable implements Callable<Object> {
        private final Callable<?> delegate;

        public ContextInjectingCallable(Callable<?> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object call() throws Exception {
            return invokeWithContextInjection(delegate, "call");
        }
    }

    /**
     * Wrapper for Runnable commands that supports context injection.
     */
    private class ContextInjectingRunnable implements Runnable {
        private final Runnable delegate;

        public ContextInjectingRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                invokeWithContextInjection(delegate, "run");
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Invokes a method with context injection if supported.
     */
    private Object invokeWithContextInjection(Object target, String methodName) throws Exception {
        Class<?> clazz = target.getClass();
        
        // Look for method with CommandContext parameter
        Method contextMethod = findMethodWithContext(clazz, methodName);
        if (contextMethod != null) {
            return contextMethod.invoke(target, context);
        }
        
        // Fall back to parameterless method
        Method defaultMethod = findDefaultMethod(clazz, methodName);
        if (defaultMethod != null) {
            return defaultMethod.invoke(target);
        }
        
        throw new IllegalStateException("No suitable method found: " + methodName);
    }

    /**
     * Finds a method that accepts CommandContext as a parameter.
     */
    private Method findMethodWithContext(Class<?> clazz, String methodName) {
        try {
            // Look for method with CommandContext parameter
            Method method = clazz.getMethod(methodName, CommandContext.class);
            return method;
        } catch (NoSuchMethodException e) {
            // Try to find method with any parameter that could be CommandContext
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName)) {
                    Parameter[] params = method.getParameters();
                    if (params.length == 1 && CommandContext.class.isAssignableFrom(params[0].getType())) {
                        return method;
                    }
                    // Check for @Context annotation
                    if (params.length == 1 && hasContextAnnotation(params[0])) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the default parameterless method.
     */
    private Method findDefaultMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Checks if a parameter has a @Context annotation.
     */
    private boolean hasContextAnnotation(Parameter parameter) {
        // Check for common context annotations
        return parameter.isAnnotationPresent(Context.class) ||
               parameter.getAnnotations().length > 0 && 
               parameter.getAnnotations()[0].annotationType().getSimpleName().equals("Context");
    }

    /**
     * Simple context annotation for marking parameters that should receive context injection.
     */
    public @interface Context {
    }
}
