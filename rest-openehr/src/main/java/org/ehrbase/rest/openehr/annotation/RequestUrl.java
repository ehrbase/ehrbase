package org.ehrbase.rest.openehr.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RequestUrl annotation. Can be used to get the current request URL value
 * during a RequestMapping implementation method. Will be used as the same way
 * as the @PathValue and similar annotations to get values from the request.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RequestUrl {
}