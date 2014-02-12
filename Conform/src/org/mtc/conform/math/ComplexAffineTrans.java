package org.mtc.conform.math;
/**
 * A class representing an affine transformation of the complex plane.  Warning: very mutable (for speed)!
 */
public class ComplexAffineTrans {

	public final Complex sc;
	public final Complex tr;

	final public static ComplexAffineTrans IDENT = new ComplexAffineTrans(Complex.ONE, Complex.ZERO);

	public ComplexAffineTrans(final Complex scaling, final Complex translation) {
		sc = new Complex(scaling);
		tr = new Complex(translation);
	}
	public ComplexAffineTrans(final ComplexAffineTrans t) {
		this(t.sc,t.tr);
	}

	public static ComplexAffineTrans translation(final Complex z) {
		return new ComplexAffineTrans(Complex.ONE, z);
	}
	public static ComplexAffineTrans translation(final float x, final float y) {
		return translation(new Complex(x,y));
	}
	public static ComplexAffineTrans scaling(final float s, final Complex p) {
		return new ComplexAffineTrans(new Complex(s), new Complex(p).mult(s-1.0f));
	}
	public static ComplexAffineTrans scaling(final float s, final float x, final float y) {
		return new ComplexAffineTrans(new Complex(s), new Complex(x,y).mult(s-1.0f));
	}
	
	public ComplexAffineTrans preMult(final ComplexAffineTrans t) {
		tr.add(new Complex(sc).mult(t.tr));
		sc.mult(t.sc);
		return this;
	}
	public ComplexAffineTrans postMult(final ComplexAffineTrans t) {
		tr.mult(t.sc).add(t.tr);
		sc.mult(t.sc);
		return this;
	}
	public ComplexAffineTrans inv() {
		sc.inv();
		tr.mult(sc).neg();
		return this;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("[z->").append(sc).append("z+").append(tr).append(']');
		return sb.toString();
	}
	@Override
	public boolean equals(Object o) {
		return sc.equals(((ComplexAffineTrans)o).sc) && tr.equals(((ComplexAffineTrans)o).tr);
	}
	@Override
	public int hashCode() {
		return sc.hashCode()+7*tr.hashCode();
	}
}
