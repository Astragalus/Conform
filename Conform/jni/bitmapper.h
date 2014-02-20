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
#define TILE 0
#define CLAMP 1

using namespace std;

typedef fixed_point<16> fixpoint;

static inline bool isZero(const complex<fixpoint> &z) {
	return (!z.real() && !z.imag());
}

static inline const complex<fixpoint> oneIfZero(const complex<fixpoint> &a) {
	return complex<fixpoint>(a.real()|(!a.real() && !a.imag()), a.imag());
}

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
	//Construct an object representing a bitmap whose color can be sampled in various ways
	BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const int wrapMode);
	//Sample color at location represented by a complex number, with the bitmap occupying [0,1]x[0,i], and wrapping values outside.
	const Pixel bilinearSample(const complex<fixpoint> &w) const;
private:
	const uint32_t *m_srcPixels;
	const uint32_t m_srcWidth;
	const uint32_t m_srcHeight;
	const fixpoint m_xMult;
	const fixpoint m_yMult;
	const int m_wrapMode;
};

class MoebiusTrans {
public:
	MoebiusTrans(const complex<fixpoint> &a, const complex<fixpoint> &b, const complex<fixpoint> &c, const complex<fixpoint> &d);
	const complex<fixpoint> operator()(const complex<fixpoint> &z) const;
	const MoebiusTrans inv() const;
	static MoebiusTrans identity();
	//MoebiusTrans& operator*=(const MoebiusTrans &g);
	const MoebiusTrans operator*(const MoebiusTrans &g) const;
	friend ostream &operator<<(ostream &os, const MoebiusTrans &mt);
private:
	const complex<fixpoint> m_a;
	const complex<fixpoint> m_b;
	const complex<fixpoint> m_c;
	const complex<fixpoint> m_d;
};

class MappedBitmap {
public:
	MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight);
	void pullbackSampledBitmap(const MoebiusTrans &map, const BitmapSampler &src);
private:
	uint32_t *m_destPixels;
	const int m_destWidth;
	const int m_destHeight;
	const fixpoint m_reInc;
	const fixpoint m_imInc;
};

#endif /* BITMAPPER_H_ */
