package magarathea.anno;

import java.lang.annotation.*;

/**
 * Describes a write-accessible port on a certain bus.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Write {
	int id();
	String name();
}