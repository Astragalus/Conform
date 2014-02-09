package org.mtc.conform.math;
/**
 * A class representing a complex number with float components.  Warning: very mutable (for speed)!
 */
public class Complex {
	
	public float re;
	public float im;
	
	final public static Complex ONE = new Complex(1.0f,0.0f);
	final public static Complex ZERO = new Complex(0.0f,0.0f);
	final public static Complex I = new Complex(0.0f,1.0f);
	
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
	
	public Complex mult(final Complex z) {
		final float newre = re*z.re + im*z.im;
		final float newim = re*z.im + im*z.re;
		re = newre;
		im = newim;
		return this;
	}
	public Complex mult(final float s) {
		re *= s;
		im *= s;
		return this;
	}
	public Complex add(final Complex z) {
		re += z.re;
		im += z.im;
		return this;
	}
	public Complex sub(final Complex z) {
		re -= z.re;
		im -= z.im;
		return this;
	}
	public Complex neg() {
		re = -re;
		im = -im;
		return this;
	}
	public Complex inv() {
		final float normsq = re*re+im*im;
		re /= normsq;
		im /= -normsq;
		return this;
	}
	public Complex conj() {
		im = -im;
		return this;
	}
	public Complex div(final Complex z) {
		return mult(new Complex(z).inv());
	}
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append('(').append(re).append("+i").append(im).append(')');
		return sb.toString();
	}
	@Override
	public boolean equals(Object o) {
		return re == ((Complex)o).re && im == ((Complex)o).im; 
	};
	@Override
	public int hashCode() {
		return Float.floatToIntBits(re) + 7*Float.floatToIntBits(im);
	}
}
