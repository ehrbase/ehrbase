package org.ehrbase.api.authorization;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Inherited
@Retention(RUNTIME)
@Repeatable(EhrbaseAuthorizations.class)
@Target(ElementType.METHOD)
public @interface EhrbaseAuthorization {
  EhrbasePermission permission();
}
