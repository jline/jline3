package jline.internal;

import java.lang.annotation.*;

/**
 * Marker for reference which can be a null value.
 *
 * @since 2.7
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface Nullable
{
    String value() default "";
}
