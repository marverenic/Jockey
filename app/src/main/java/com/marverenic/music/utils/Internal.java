package com.marverenic.music.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated field, method, constructor, or type is package protected to prevent
 * Java from creating a synthetic method at compile time. Fields and methods marked with this
 * annotation should be treated as if they were private.
 *
 * When a private field is accessed from an inner class, Java creates a package protected getter or
 * setter to access the field (for methods, Java creates a package protected shim method to access
 * the method). This adds unnecessary overhead both at runtime and in the compiled APK since it
 * counts against our 65k method count for a single DEX file.
 *
 * To enable lint warnings about these problems in IntelliJ, open IntelliJ's settings, browse to
 * Editor > Inspections, and enable "Private member access between inner and outer classes"
 * under Java > J2ME Issues.
 *
 * For more information, see
 * https://www.reddit.com/r/androiddev/comments/4zccfb/exploring_javas_hidden_costs/
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface Internal {
}
