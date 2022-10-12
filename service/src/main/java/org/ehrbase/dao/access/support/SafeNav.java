package org.ehrbase.dao.access.support;

import java.util.function.BiFunction;
import java.util.function.Function;

public class SafeNav<T> {
  public static <T1> SafeNav<T1> of(T1 obj) {
    return new SafeNav<T1>(obj);
  }
  
  private final T obj;
  
  private SafeNav(T obj) {
    this.obj = obj;
  }

  public <T1> SafeNav<T1> get(Function<T,T1> accessor) {
    if(obj == null)
      return new SafeNav<T1>(null);
    try {
      return new SafeNav<T1>(accessor.apply(obj));
    } catch(NullPointerException e) {
      return new SafeNav<T1>(null);
    }
  }
  
  public T get() {
    return obj;
  }
  
  public boolean isSafe() {
    return obj != null;
  }
  
  public <T1> CombineSafeNav<T1> use(SafeNav<T1> s) {
    return new CombineSafeNav<>(s);
  }
  
  public class CombineSafeNav<T1> {
    private final SafeNav<T1> safe;
    
    private CombineSafeNav(SafeNav<T1> safe) {
      this.safe = safe;
    }
    
    public SafeNav<T> get(BiFunction<T1,T,T> accessor) {
      if(!SafeNav.this.isSafe())
        return null;
      try {
        return SafeNav.of(accessor.apply(safe.get(), SafeNav.this.obj));
      } catch(NullPointerException e) {
        return new SafeNav<T>(null);
      }
    }
  }
}
