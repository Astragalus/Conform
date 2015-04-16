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
/*
 * bitmapper.h
 *
 *  Created on: Dec 31, 2013
 *      Author: MTC
 */

#ifndef BITMAPPER_H_
#define BITMAPPER_H_

#include <iostream>
#include <complex>
#include "fixed_class.h"

using namespace std;

static const complex<fixpoint> ONE(1,0);
static const complex<fixpoint> ZERO(0,0);

const complex<fixpoint> divZeroGuard(complex<fixpoint>&& z);

class Pixel {
public:
	//Construct a Pixel from a, r, g, and b values
	explicit Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b);
	//Construct a Pixel from an ARGB_8888 formatted pixel
	explicit Pixel(const uint32_t &pix);
	//Bilinearly interpolate between four pixels with two parameters
	static const Pixel bilinterp(const Pixel &dl, const Pixel &dr, const Pixel &ul, const Pixel &ur, const fixpoint &h, const fixpoint &v);
	//Write Pixel to an ARGB_8888 formatted destination
	void write(uint32_t &dest) const;
	friend ostream& operator<<(ostream& os, const Pixel& p);
private:
	const uint32_t a;
	const uint32_t r;
	const uint32_t g;
	const uint32_t b;
};

ostream &operator<<(ostream &os, const fixed_point<16> &f);

class BitmapSampler {
public:
	BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const int wrapMode);
	//BitmapSampler(const BitmapSampler& o);
	const Pixel bilinearSample(const complex<fixpoint> &w) const;
private:
	const uint32_t *m_srcPixels;
	const uint32_t m_srcWidth;
	const uint32_t m_srcHeight;
	const fixpoint m_xMult;
	const fixpoint m_yMult;
	const int m_wrapMode;
};

class MobiusTrans {
public:
	explicit MobiusTrans(const complex<fixpoint>& a, const complex<fixpoint>& b, const complex<fixpoint>& c, const complex<fixpoint>& d);
	explicit MobiusTrans();
	MobiusTrans(const MobiusTrans& mt);
	MobiusTrans& operator=(const MobiusTrans& mt);
	static const MobiusTrans hyperbolicIsometry(complex<fixpoint>&& zero);
	const complex<fixpoint> operator()(const complex<fixpoint> &z) const;
	const MobiusTrans operator|(const MobiusTrans& f) const;
	const MobiusTrans operator-() const;
	friend ostream& operator<<(ostream &os, const MobiusTrans& mobius);
	static const MobiusTrans identity;
	const bool isIdentity() const;
private:
	complex<fixpoint> m_a;
	complex<fixpoint> m_b;
	complex<fixpoint> m_c;
	complex<fixpoint> m_d;
	bool m_isIdentity;
};

class BlaschkeMap {
public:
	explicit BlaschkeMap();
	explicit BlaschkeMap(const MobiusTrans& a);
	explicit BlaschkeMap(const MobiusTrans& a, const MobiusTrans& b);
	BlaschkeMap(const BlaschkeMap& g);
	const complex<fixpoint> operator()(const complex<fixpoint> &z) const;
	friend BlaschkeMap& operator|(const MobiusTrans& a, BlaschkeMap& b);
	friend BlaschkeMap& operator|(BlaschkeMap& b, const MobiusTrans& a);
	BlaschkeMap& operator*=(const BlaschkeMap& f);
	BlaschkeMap& operator*=(const MobiusTrans& a);
	friend ostream& operator<<(ostream &os, const BlaschkeMap& blasch);
	static const int max_factors = 6;
private:
	int m_numFactors;
	MobiusTrans m_factors[max_factors];
	MobiusTrans m_lhs;
};

class MappedBitmap {
public:
	MappedBitmap(const BitmapSampler& src, uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight, const BlaschkeMap& map);
	void pullbackSlice(const int startHeight, const int endHeight);
	void pullbackSampledBitmap();
private:
	const BlaschkeMap& m_map;
	const BitmapSampler& m_src;
	uint32_t *m_destPixels;
	const int m_destWidth;
	const int m_destHeight;
	const fixpoint m_reInc;
	const fixpoint m_imInc;
};

#endif /* BITMAPPER_H_ */
