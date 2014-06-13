package org.mtc.conform.math;

public interface IComplex {

	final public static Complex ONE = new Complex(1.0f, 0.0f);
	final public static Complex ZERO = new Complex(0.0f, 0.0f);
	final public static Complex I = new Complex(0.0f, 1.0f);

	public abstract float re();
	public abstract IComplex re(float r);
	public abstract float im();
	public abstract IComplex im(float i);
	
	public abstract IComplex assignTo(IComplex w);
	
	public abstract IComplex assignFrom(IComplex z);
	
	public abstract IComplex assignFrom(Complex z);

	public abstract IComplex assignFrom(float re, float im);

	public abstract IComplex assignFrom(float[] arr);

	public abstract IComplex mult(Complex z);

	public abstract IComplex mult(float s);

	public abstract IComplex add(Complex z);

	public abstract IComplex sub(Complex z);

	public abstract IComplex neg();

	public abstract IComplex inv();

	public abstract IComplex conj();

	public abstract IComplex div(Complex z);
	
	public abstract float distSq(IComplex z);

	public abstract String toString();

	public abstract boolean equals(Object o);

	public abstract int hashCode();

}