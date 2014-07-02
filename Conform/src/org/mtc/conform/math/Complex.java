package org.mtc.conform.math;


/**
 * A class representing a complex number with float components.  Warning: very mutable (for speed)!
 */
public class Complex implements IComplex {
	
	public float re;
	public float im;
	
	public Complex(final float re, final float im) {
		this.re = re;
		this.im = im;
	}
	public Complex(final float x) {
		this.re = x;
		this.im = 0.0f;
	}
	public Complex(final Complex z) {
		this.re = z.re;
		this.im = z.im;
	}
	public Complex(final IComplex z) {
		this.re = z.re();
		this.im = z.im();
	}
	
	@Override
	public IComplex assignTo(IComplex w) {
		w.assignFrom(re,im);
		return this;
	}
	
	@Override
	public IComplex assignFrom(final IComplex z) {
		z.assignTo(this);
		return this;
	}
	
	@Override
	public IComplex assignFrom(final Complex z) {
		re = z.re;
		im = z.im;
		return this;
	}
	@Override
	public IComplex assignFrom(final float re, final float im) {
		this.re = re;
		this.im = im;
		return this;
	}
	@Override
	public IComplex assignFrom(final float[] arr) {
		this.re = arr[0];
		this.im = arr[1];
		return this;
	}
	@Override
	public Complex mult(final Complex z) {
		final float newre = re*z.re - im*z.im;
		final float newim = re*z.im + im*z.re;
		re = newre;
		im = newim;
		return this;
	}
	@Override
	public Complex mult(final float s) {
		re *= s;
		im *= s;
		return this;
	}
	@Override
	public IComplex add(final Complex z) {
		re += z.re;
		im += z.im;
		return this;
	}
	@Override
	public IComplex sub(final Complex z) {
		re -= z.re;
		im -= z.im;
		return this;
	}
	@Override
	public IComplex neg() {
		re = -re;
		im = -im;
		return this;
	}
	@Override
	public Complex inv() {
		final float normsq = re*re+im*im;
		re /= normsq;
		im /= -normsq;
		return this;
	}
	@Override
	public IComplex conj() {
		im = -im;
		return this;
	}
	@Override
	public IComplex div(final Complex z) {
		return mult(new Complex(z).inv());
	}

	@Override
	public float distSq(IComplex z) {
		final float re = z.re() - this.re;
		final float im = z.im() - this.im;
		return re*re+im*im;
	}
	
	@Override
	public String toString() {
		return toString(this);
	}

	public static String toString(IComplex z) {
		final float re = z.re();
		final float im = z.im();
		return String.format("[%.4f%c%.4fi]", re, (im<0f ? '-' : '+'), im<0f ? -im : im);
	}
	
	@Override
	public boolean equals(Object o) {
		return re == ((Complex)o).re && im == ((Complex)o).im; 
	};
	@Override
	public int hashCode() {
		return Float.floatToIntBits(re) + 7*Float.floatToIntBits(im);
	}
	
	@Override
	public float re() {return re;}
	@Override
	public Complex re(float r) {re = r;return this;}
	@Override
	public float im() {return im;}
	@Override
	public Complex im(float i) {im = i;return this;}
}
