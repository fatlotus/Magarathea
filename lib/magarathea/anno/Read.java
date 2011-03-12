package magarathea.anno;

import java.lang.annotation.*;

/**
 * Describes a read-accessible port on a certain bus.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Read {
	int id();
	String name();
}