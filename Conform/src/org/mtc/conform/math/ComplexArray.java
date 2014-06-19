package org.mtc.conform.math;

import java.util.Collection;
import java.util.Iterator;

public class ComplexArray implements Collection<IComplex> {

	final public float[] arr;
	final public int capacity;
	private int size;
	
	public ComplexArray(final int capacity) {
		this.capacity = capacity;
		arr = new float[2*capacity];
		size = 0;
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
	
	@Override
	public ComplexIterator iterator() {
		return new ComplexIterator();
	}
	
	public ComplexElement atIndexOf(final ComplexElement other) {
		return front().parallelTo(other);
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
	public class ComplexElement implements IComplex, Iterator<IComplex> {
		
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
			arr[idx] = re*z.re + im*z.im;
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

		@Override
		public boolean hasNext() {
			return (idx>>1) < size;
		}

		@Override
		public IComplex next() {
			idx += 2;
			return this;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public ComplexElement append() {
		if (size < capacity) {
			return at(size++);
		} else {
			return at(size); //Too many? just overwrite the last one.  Look I just work here.
		}
	}
	
	@Override
	public boolean add(IComplex object) {
		if (size<capacity) {
			arr[size*2] = object.re();
			arr[size*2+1] = object.im();
			++size;
			return true;
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends IComplex> collection) {
		return false;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean contains(Object object) {
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean remove(Object object) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return null;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ComplexArray");
		sb.append('(').append(size).append(')');
		sb.append('[');
		for (IComplex z : this) {
			sb.append(z).append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append(']');
		return sb.toString();
	}
}
