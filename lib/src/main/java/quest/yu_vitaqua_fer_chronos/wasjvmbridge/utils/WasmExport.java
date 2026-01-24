package quest.yu_vitaqua_fer_chronos.wasjvmbridge.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WasmExport {
    String name() default "";

    String module() default "";

    String[] params() default {};

    Class<?> expect() default void.class;
}