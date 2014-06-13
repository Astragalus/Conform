package org.mtc.conform.math;

import java.util.Collection;
import java.util.Iterator;

public class ComplexArray implements Collection<IComplex> {

	final public float[] arr;
	private int size;
	
	public ComplexArray(final int capacity) {
		arr = new float[capacity];
		size = 0;
	}
	
	public ComplexArray(final ComplexArray other) {
		arr = new float[other.arr.length];
		System.arraycopy(other.arr, 0, arr, 0, other.size<<1);
		size = other.size;
	}
	
	public ComplexArray copyFrom(final ComplexArray other) {
		System.arraycopy(other.arr, 0, arr, 0, other.size<<1);
		size = other.size;
		return this;
	}
	
	public ComplexElement element() {
		return new ComplexElement();
	}
	
	@Override
	public Iterator<IComplex> iterator() {
		return new ComplexElement();
	}

	/**
	 * A class representing a complex number with float components.  Warning: very mutable (for speed)!
	 */
	public class ComplexElement implements IComplex, Iterator<IComplex> {
		
		private int idx;
		
		public float[] getBackingArray() {
			return arr;
		}

		public int getIndex() {
			return this.idx;
		}
		public ComplexElement atIndex(int i) {
			this.idx = i;
			return this;
		}
		public ComplexElement parallelTo(final ComplexElement other) {
			this.idx = other.idx;
			return this;
		}
		
		@Override
		public IComplex assignTo(IComplex w) {
			w.assignFrom(arr[idx<<1],arr[idx<<1+1]);
			return this;
		}
		
		@Override
		public IComplex assignFrom(final IComplex z) {
			z.assignTo(this);
			return this;
		}

		@Override
		public IComplex assignFrom(Complex z) {
			arr[idx<<1] = z.re;
			arr[idx<<1+1] = z.im;
			return this;
		}
		
		@Override
		public IComplex assignFrom(final float re, final float im) {
			arr[idx<<1] = re;
			arr[idx<<1+1] = im;
			return this;
		}
		
		@Override
		public IComplex assignFrom(final float[] arr) {
			arr[idx<<1] = arr[0];
			arr[idx<<1+1] = arr[1];
			return this;
		}
		@Override
		public IComplex mult(final Complex z) {
			final float re = arr[idx<<1];
			final float im = arr[idx<<1+1];		
			arr[idx<<1] = re*z.re + im*z.im;
			arr[idx<<1+1] = re*z.im + im*z.re;
			return this;
		}
		@Override
		public ComplexElement mult(final float s) {
			arr[idx<<1] *= s;
			arr[idx<<1+1] *= s;
			return this;
		}
		@Override
		public IComplex add(Complex z) {
			arr[idx<<1] += z.re;
			arr[idx<<1+1] += z.im;
			return this;
		}
		public IComplex add(final ComplexElement z) {
			arr[idx<<1] += z.getBackingArray()[z.idx<<1];
			arr[idx<<1+1] += z.getBackingArray()[z.idx<<1+1];
			return this;
		}
		@Override
		public IComplex sub(Complex z) {
			arr[idx<<1] -= z.re;
			arr[idx<<1+1] -= z.im;
			return this;
		}

		public IComplex sub(final ComplexElement z) {
			arr[idx<<1] -= z.getBackingArray()[z.idx<<1];
			arr[idx<<1+1] -= z.getBackingArray()[z.idx<<1+1];
			return this;
		}
		@Override
		public IComplex neg() {
			arr[idx<<1] = -arr[idx<<1];
			arr[idx<<1+1] = -arr[idx<<1+1];
			return this;
		}
		@Override
		public ComplexElement inv() {
			final float re = arr[idx<<1];
			final float im = arr[idx<<1+1];
			final float normsq = re*re+im*im;
			arr[idx<<1] /= normsq;
			arr[idx<<1+1] /= -normsq;
			return this;
		}
		@Override
		public IComplex conj() {
			arr[idx<<1+1] = -arr[idx<<1+1];
			return this;
		}
		@Override
		public IComplex div(final Complex z) {
			return mult(new Complex(z).inv());
		}

		@Override
		public float distSq(IComplex z) {
			final float re = z.re() - arr[idx<<1];
			final float im = z.im() - arr[idx<<1+1];
			return re*re+im*im;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append('(').append(arr[idx<<1]).append("+i").append(arr[idx<<1+1]).append(')');
			return sb.toString();
		}
		@Override
		public boolean equals(Object o) {
			return arr[idx<<1] == ((IComplex)o).re() && arr[idx<<1+1] == ((IComplex)o).im(); 
		};
		@Override
		public int hashCode() {
			return Float.floatToIntBits(arr[idx<<1]) + 7*Float.floatToIntBits(arr[idx<<1+1]);
		}

		@Override
		public float re() {return arr[idx<<1];}
		@Override
		public ComplexElement re(float r) {arr[idx<<1] = r;return this;}
		@Override
		public float im() {return arr[idx<<1+1];}
		@Override
		public ComplexElement im(float i) {arr[idx<<1+1] = i;return this;}

		@Override
		public boolean hasNext() {
			return idx < size - 1;
		}

		@Override
		public IComplex next() {
			++idx;
			return this;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public boolean add(IComplex object) {
		if (size<arr.length) {
			arr[size<<1] = object.re();
			arr[size<<1+1] = object.im();
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
}
