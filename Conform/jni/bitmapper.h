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

typedef fixed_point<16> fixpoint;

static const complex<fixpoint> ONE(1,0);
static const complex<fixpoint> ZERO(0,0);

static inline bool isZero(const complex<fixpoint> &z) {
	return (!z.real() && !z.imag());
}

static inline const complex<fixpoint> oneIfZero(const complex<fixpoint> &a) {
	return complex<fixpoint>(a.real()|(!a.real() && !a.imag()), a.imag());
}

struct Clamp {
	inline const fixpoint operator()(const fixpoint& x) const {
		return clamp(x);
	}
};

struct Tile {
	inline const fixpoint operator()(const fixpoint& x) const {
		return frac(x);
	}
};

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

template <typename W>
class BitmapSampler {
public:
	BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const W& wrap) :
		m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight),
		m_xMult(srcWidth<srcHeight?fixpoint(srcHeight)/fixpoint(srcWidth):fixpoint(1)),
		m_yMult(srcWidth>srcHeight?fixpoint(srcWidth)/fixpoint(srcHeight):fixpoint(1)),
		m_wrap(wrap) {
	}
	const Pixel bilinearSample(const complex<fixpoint> &w) const {
		const fixpoint xfix = m_wrap(w.real()*m_xMult)*fixpoint(m_srcWidth-1);
		const fixpoint yfix = m_wrap(w.imag()*m_yMult)*fixpoint(m_srcHeight-1);
		const fixpoint tx = frac(xfix);
		const fixpoint ty = frac(yfix);
		const uint32_t x0 = (xfix-tx).toUnsigned(); //x index of left side
		const uint32_t y0 = (yfix-ty).toUnsigned(); //y index of bottom (top?) side
		return Pixel::bilinterp(Pixel(m_srcPixels[y0*m_srcWidth+x0]),Pixel(m_srcPixels[y0*m_srcWidth+x0+1]),
								Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0]),Pixel(m_srcPixels[(y0+1)*m_srcWidth+(x0+1)]),
								tx, ty);
	}
private:
	const uint32_t *m_srcPixels;
	const uint32_t m_srcWidth;
	const uint32_t m_srcHeight;
	const fixpoint m_xMult;
	const fixpoint m_yMult;
	const W& m_wrap;
};

template <typename _W>
static const BitmapSampler<_W> createSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const _W& wrap) {
	return BitmapSampler<_W>(srcPixels, srcWidth, srcHeight, wrap);
}

class MobiusTrans {
public:
	explicit MobiusTrans(const complex<fixpoint>& a, const complex<fixpoint>& b, const complex<fixpoint>& c, const complex<fixpoint>& d);
	explicit MobiusTrans();
	static const MobiusTrans hyperbolicIsometry(const complex<fixpoint>& zero);
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
	explicit BlaschkeMap(const BlaschkeMap& g);
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
	template <typename _W>
	void pullbackSampledBitmap(const BlaschkeMap& map, const BitmapSampler<_W>& src) {
		fixpoint zim(0);
		for (int v = 0; v < m_destHeight; ++v) {
			fixpoint zre(0);
			for (int u = 0; u < m_destWidth; ++u) {
				const complex<fixpoint> z(zre,zim);
				src.bilinearSample(map(z)).write(m_destPixels[v*m_destWidth+u]); //sample color from src at map(z) and write to dest
				zre += m_reInc;
			}
			zim += m_imInc;
		}
	}
private:
	uint32_t *m_destPixels;
	const int m_destWidth;
	const int m_destHeight;
	const fixpoint m_reInc;
	const fixpoint m_imInc;
};

#endif /* BITMAPPER_H_ */
