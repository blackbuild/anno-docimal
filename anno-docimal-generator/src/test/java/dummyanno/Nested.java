package dummyanno;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Nested {
    WithPrimitives primitiveValue() default @WithPrimitives;
    WithEnum enumValue() default @WithEnum(value = RetentionPolicy.CLASS);
    WithPrimitives[] primitivesArray() default {};
}
