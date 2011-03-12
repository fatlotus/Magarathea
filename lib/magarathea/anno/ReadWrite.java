package magarathea.anno;

import java.lang.annotation.*;

/**
 * Describes a read- and write- accessible port on a certain bus.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface ReadWrite {
	int id();
	String name();
}