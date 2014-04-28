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

//Boundary treatment
//#define TILE 0
//#define CLAMP 1

using namespace std;

typedef fixed_point<16> fixpoint;

static inline bool isZero(const complex<fixpoint> &z) {
	return (!z.real() && !z.imag());
}

static inline const complex<fixpoint> oneIfZero(const complex<fixpoint> &a) {
	return complex<fixpoint>(a.real()|(!a.real() && !a.imag()), a.imag());
}

struct WrapFunc {
	virtual const fixpoint operator()(const fixpoint& x) const = 0;
};

struct Clamp : public WrapFunc {
	virtual const fixpoint operator()(const fixpoint& x) const {
		return clamp(x);
	}
};

struct Tile : public WrapFunc {
	virtual const fixpoint operator()(const fixpoint& x) const {
		return frac(x);
	}
};

struct WrapFac {
	enum WrapMode {TILE, CLAMP};
	static const WrapFunc& create(const int type) {
		if (type == TILE) {
			static const Clamp clamp;
			return clamp;
		} else {
			static const Tile tile;
			return tile;
		}
	}
};

static const WrapFac Wrap;

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
	BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const WrapFunc& wrap);
	const Pixel bilinearSample(const complex<fixpoint> &w) const;
private:
	const uint32_t *m_srcPixels;
	const uint32_t m_srcWidth;
	const uint32_t m_srcHeight;
	const fixpoint m_xMult;
	const fixpoint m_yMult;
	const WrapFunc& m_wrap;
};

class ComplexMap {
	virtual const complex<fixpoint> operator()(const complex<fixpoint> &z) const = 0;
};

class MobiusTrans {
public:
	explicit MobiusTrans(const complex<fixpoint>& a, const complex<fixpoint>& b, const complex<fixpoint>& c, const complex<fixpoint>& d);
	explicit MobiusTrans();
	const complex<fixpoint> operator()(const complex<fixpoint> &z) const;
	const MobiusTrans operator|(const MobiusTrans& f) const;
	const MobiusTrans operator-() const;
	ostream& operator<<(ostream &os) const;
	static const MobiusTrans identity;
	const bool isIdentity() const;
private:
	complex<fixpoint> m_a;
	complex<fixpoint> m_b;
	complex<fixpoint> m_c;
	complex<fixpoint> m_d;
	bool m_isIdentity;
};

class BlaschkeMap : public ComplexMap {
public:
	BlaschkeMap(const MobiusTrans& a, const MobiusTrans& b, const MobiusTrans& c, const MobiusTrans& d);
	BlaschkeMap(const MobiusTrans& a, const MobiusTrans& b, const MobiusTrans& c);
	BlaschkeMap(const MobiusTrans& a, const MobiusTrans& b);
	BlaschkeMap(const MobiusTrans& a);
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
};


class MappedBitmap {
public:
	MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight);
	void pullbackSampledBitmap(const BlaschkeMap& map, const BitmapSampler& src);
private:
	uint32_t *m_destPixels;
	const int m_destWidth;
	const int m_destHeight;
	const fixpoint m_reInc;
	const fixpoint m_imInc;
};

#endif /* BITMAPPER_H_ */
