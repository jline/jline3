package jline.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker annotation for members which are exposed for testing access.
 *
 * @since 2.7
 */
@Retention(RUNTIME)
@Target({TYPE, CONSTRUCTOR, METHOD, FIELD, PARAMETER})
@Documented
public @interface TestAccessible
{
    // empty
}