package org.ehrbase.api.aspect;

import java.lang.annotation.Annotation;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;

public interface AnnotationAspect {
  public List<Class<? extends Annotation>> matchAnnotations();
  public Object action(ProceedingJoinPoint pjp, List<Annotation> annotations) throws Throwable;
}
