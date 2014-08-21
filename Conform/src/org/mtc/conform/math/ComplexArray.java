/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.mtc.conform.math;

import java.util.Iterator;

public class ComplexArray {

	public interface IComplexAction {
		void actOn(IComplex z);
	}
	
	public interface IComplexPredicate {
		boolean eval(IComplex z);
	}
	
	final public float[] arr;
	final public int capacity;
	public int size;
	
	public ComplexArray(final int capacity) {
		this.capacity = capacity;
		arr = new float[2*(capacity+1)];
		size = 0;
	}

	public ComplexArray(final int capacity, final int size) {
		this.capacity = capacity;
		arr = new float[2*capacity];
		this.size = size;
	}
	
	public ComplexArray(final ComplexArray other) {
		capacity = other.capacity;
		arr = new float[capacity];
		System.arraycopy(other.arr, 0, arr, 0, other.size*2);
		size = other.size;
	}
	
	public ComplexArray copyFrom(final ComplexArray other) {
		System.arraycopy(other.arr, 0, arr, 0, other.size*2);
		size = other.size;
		return this;
	}
	
	public ComplexElement front() {
		return new ComplexElement();
	}
	
	public ComplexElement at(final int location) {
		return new ComplexElement(location);
	}
	
	public ComplexElement atIndexOf(final ComplexElement other) {
		return front().parallelTo(other);
	}
	
	//OK this is the definition of trouble.  But let's see what happens - why not?
	private final ComplexElement asComplex = new ComplexElement();

	public void apply(final IComplexAction action) {
		for (asComplex.idx = 0; asComplex.idx < size<<1; asComplex.idx += 2) {
			action.actOn(asComplex);
		}
	}
	
	public ComplexElement find(final IComplexPredicate condition) {
		final ComplexElement element = new ComplexElement();
		for (element.idx = 0; element.idx < size<<1; element.idx += 2) {
			if (condition.eval(element)) {
				return element;
			}
		}
		return null;
	}
	
	public class ComplexIterator implements Iterator<IComplex> {
		private int i;
		@Override public boolean hasNext() {
			return i < size;
		}
		@Override public ComplexElement next() {
			return new ComplexElement(i++);
		}
		@Override public void remove() {
		}
	}
	
	/**
	 * A class representing a complex number with float components.  Warning: very mutable (for speed)!
	 */
	public class ComplexElement implements IComplex {
		
		private int idx;
		
		public ComplexElement() {
			idx = 0;
		}
		
		/**
		 * @param location index in array, considered as an array of complex (not pairs of floats), i.e., don't multiply by 2.
		 */
		public ComplexElement(final int location) {
			idx = location<<1;
		}
		
		public ComplexArray getParent() {
			return ComplexArray.this;
		}
		
		public float[] getBackingArray() {
			return arr;
		}

		public int getIndex() {
			return this.idx>>1;
		}
		public ComplexElement atIndex(int i) {
			this.idx = i<<1;
			return this;
		}
		public ComplexElement parallelTo(final ComplexElement other) {
			this.idx = other.idx;
			return this;
		}
		
		@Override
		public ComplexElement assignTo(IComplex w) {
			w.assignFrom(arr[idx],arr[idx+1]);
			return this;
		}
		
		@Override
		public ComplexElement assignFrom(final IComplex z) {
			z.assignTo(this);
			return this;
		}

		@Override
		public ComplexElement assignFrom(Complex z) {
			arr[idx] = z.re;
			arr[idx+1] = z.im;
			return this;
		}
		
		@Override
		public ComplexElement assignFrom(final float re, final float im) {
			arr[idx] = re;
			arr[idx+1] = im;
			return this;
		}
		
		@Override
		public ComplexElement assignFrom(final float[] arr) {
			arr[idx] = arr[0];
			arr[idx+1] = arr[1];
			return this;
		}
		@Override
		public ComplexElement mult(final Complex z) {
			final float re = arr[idx];
			final float im = arr[idx+1];		
			arr[idx] = re*z.re - im*z.im;
			arr[idx+1] = re*z.im + im*z.re;
			return this;
		}
		@Override
		public ComplexElement mult(final float s) {
			arr[idx] *= s;
			arr[idx+1] *= s;
			return this;
		}
		@Override
		public ComplexElement add(Complex z) {
			arr[idx] += z.re;
			arr[idx+1] += z.im;
			return this;
		}
		public ComplexElement add(final ComplexElement z) {
			arr[idx] += z.getBackingArray()[z.idx];
			arr[idx+1] += z.getBackingArray()[z.idx+1];
			return this;
		}
		@Override
		public ComplexElement sub(Complex z) {
			arr[idx] -= z.re;
			arr[idx+1] -= z.im;
			return this;
		}

		public ComplexElement sub(final ComplexElement z) {
			arr[idx] -= z.getBackingArray()[z.idx];
			arr[idx+1] -= z.getBackingArray()[z.idx+1];
			return this;
		}
		@Override
		public ComplexElement neg() {
			arr[idx] = -arr[idx];
			arr[idx+1] = -arr[idx+1];
			return this;
		}
		@Override
		public ComplexElement inv() {
			final float re = arr[idx];
			final float im = arr[idx+1];
			final float normsq = re*re+im*im;
			arr[idx] /= normsq;
			arr[idx+1] /= -normsq;
			return this;
		}
		@Override
		public ComplexElement conj() {
			arr[idx+1] = -arr[idx+1];
			return this;
		}
		@Override
		public ComplexElement div(final Complex z) {
			return mult(new Complex(z).inv());
		}

		@Override
		public float distSq(IComplex z) {
			final float re = z.re() - arr[idx];
			final float im = z.im() - arr[idx+1];
			return re*re+im*im;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("Element");
			sb.append('(').append(idx>>1).append(')');
			sb.append('[').append(arr[idx]).append("+i").append(arr[idx+1]).append(']');
			return sb.toString();
		}
		@Override
		public boolean equals(Object o) {
			return arr[idx] == ((IComplex)o).re() && arr[idx+1] == ((IComplex)o).im(); 
		};
		@Override
		public int hashCode() {
			return Float.floatToIntBits(arr[idx]) + 7*Float.floatToIntBits(arr[idx+1]);
		}

		@Override
		public float re() {
			return arr[idx];
		}
		@Override
		public ComplexElement re(float r) {
			arr[idx] = r;
			return this;
		}
		@Override
		public float im() {
			return arr[idx+1];
		}
		@Override
		public ComplexElement im(float i) {
			arr[idx+1] = i;
			return this;
		}
	}

	public int size() {
		return size;
	}
	
	public ComplexElement append() {
		if (size < capacity) {
			return at(size++);
		} else {
			return at(size); //Too many? just overwrite the last one.  Look I just work here.
		}
	}

	public void remove() {
		if (size > 0) {
			--size;
		}
	}
	
	private static IComplexAction getStringifier(final StringBuilder sb) {
		return new IComplexAction() {
			boolean isFirst = true;
			@Override
			public void actOn(IComplex z) {
				if (!isFirst) {
					sb.append(',');
				} else {
					isFirst = false;
				}
				sb.append(z.toString());
			}
		};
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ComplexArray[");
		apply(getStringifier(sb));
		return sb.append(']').toString();
	}

}
