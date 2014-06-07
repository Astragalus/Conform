package org.mtc.conform.math;


/**
 * A class representing a complex number with float components.  Warning: very mutable (for speed)!
 */
public class ComplexArrayBacked implements IComplex {
	
	private int idx;
	private float[] arr;

	public ComplexArrayBacked() {
	}
	
	public ComplexArrayBacked(float[] backingArray) {
		this.idx = 0;
		this.arr = backingArray;
	}
	
	public float[] getBackingArray() {
		return this.arr;
	}
	public void setBackingArray(float[] arr) {
		this.arr = arr;
	}

	public int getIndex() {
		return this.idx;
	}
	public IComplex atIndex(int i) {
		this.idx = i;
		return this;
	}
	
	@Override
	public IComplex assignTo(IComplex w) {
		w.assignFrom(arr[idx],arr[idx+1]);
		return this;
	}
	
	@Override
	public IComplex assignFrom(final IComplex z) {
		z.assignTo(this);
		return this;
	}

	@Override
	public IComplex assignFrom(Complex z) {
		arr[idx] = z.re;
		arr[idx+1] = z.im;
		return this;
	}
	
	@Override
	public IComplex assignFrom(final float re, final float im) {
		arr[idx] = re;
		arr[idx+1] = im;
		return this;
	}
	
	@Override
	public IComplex assignFrom(final float[] arr) {
		arr[idx] = arr[0];
		arr[idx+1] = arr[1];
		return this;
	}
	@Override
	public IComplex mult(final Complex z) {
		final float re = arr[idx];
		final float im = arr[idx+1];		
		arr[idx] = re*z.re + im*z.im;
		arr[idx+1] = re*z.im + im*z.re;
		return this;
	}
	@Override
	public ComplexArrayBacked mult(final float s) {
		arr[idx] *= s;
		arr[idx+1] *= s;
		return this;
	}
	@Override
	public IComplex add(Complex z) {
		arr[idx] += z.re;
		arr[idx+1] += z.im;
		return this;
	}
	public IComplex add(final ComplexArrayBacked z) {
		arr[idx] += z.arr[z.idx];
		arr[idx+1] += z.arr[z.idx+1];
		return this;
	}
	@Override
	public IComplex sub(Complex z) {
		arr[idx] -= z.re;
		arr[idx+1] -= z.im;
		return this;
	}

	public IComplex sub(final ComplexArrayBacked z) {
		arr[idx] -= z.arr[z.idx];
		arr[idx+1] -= z.arr[z.idx+1];
		return this;
	}
	@Override
	public IComplex neg() {
		arr[idx] = -arr[idx];
		arr[idx+1] = -arr[idx+1];
		return this;
	}
	@Override
	public ComplexArrayBacked inv() {
		final float re = arr[idx];
		final float im = arr[idx+1];
		final float normsq = re*re+im*im;
		arr[idx] /= normsq;
		arr[idx+1] /= -normsq;
		return this;
	}
	@Override
	public IComplex conj() {
		arr[idx+1] = -arr[idx+1];
		return this;
	}
	@Override
	public IComplex div(final Complex z) {
		return mult(new Complex(z).inv());
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('(').append(arr[idx]).append("+i").append(arr[idx+1]).append(')');
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
	public float re() {return arr[idx];}
	@Override
	public ComplexArrayBacked re(float r) {arr[idx] = r;return this;}
	@Override
	public float im() {return arr[idx+1];}
	@Override
	public ComplexArrayBacked im(float i) {arr[idx+1] = i;return this;}
}
