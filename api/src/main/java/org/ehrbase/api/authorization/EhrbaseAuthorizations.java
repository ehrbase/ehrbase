package org.ehrbase.api.authorization;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Inherited
@Retention(RUNTIME)
@Target(ElementType.METHOD)
public @interface EhrbaseAuthorizations {
  EhrbaseAuthorization[] value();
}
