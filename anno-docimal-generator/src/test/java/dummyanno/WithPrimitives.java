package dummyanno;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
public @interface WithPrimitives {
    int intValue() default 0;
    String stringValue() default "";
    boolean booleanValue() default false;
    double doubleValue() default 0.0;
    float floatValue() default 0.0f;
    long longValue() default (long) 0;
    short shortValue() default (short) 0;
    byte byteValue() default (byte) 0;
    char charValue() default (char) 'b';
    int[] intArray() default {0};
    Class<? extends List> classValue() default List.class;

}
