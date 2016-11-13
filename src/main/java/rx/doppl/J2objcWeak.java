package rx.doppl;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation that indicates a variable has a weak relationship to its owner.
 *
 * @author Tom Ball
 */
@Target({FIELD, LOCAL_VARIABLE, PARAMETER})
@Retention(CLASS)
public @interface J2objcWeak {
}
