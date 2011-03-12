package magarathea.anno;

import java.lang.annotation.*;

/**
 * Describes an accessible bus in the simulated OISC
 * architecture.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Bus {
	int id();
	String prefix() default "";
}