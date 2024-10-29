package org.ehrbase.util;

import java.util.function.Function;

public class NullSafeFuncWrapper<I,O> {
	
	public static <I0,O0> NullSafeFuncWrapper<I0,O0> of(Function<I0,O0> func) {
		return new NullSafeFuncWrapper<>(func);
	}
	
	private final Function<I,O> access;
	
	private NullSafeFuncWrapper(Function<I,O> access) {
		this.access = access;
	}
	
	public <OO> NullSafeFuncWrapper<I,OO> after(Function<O,OO> func) {
		Function<I,OO> acc = in -> {
			if(in == null)
				return null;
			
			O out = NullSafeFuncWrapper.this.access.apply(in);
			
			if(out == null)
				return null;
			
			return func.apply(out);
		};
		
		return new NullSafeFuncWrapper<>(acc);
	}
	
	public O apply(I in) {
		return access.apply(in);
	}
}
