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