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
#include "fixedpoint/fixed_class.h"

typedef fixedpoint::fixed_point<16> fixed;

class Pixel {
public:
	//Construct a Pixel from a, r, g, and b values
	explicit Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b);
	//Construct a Pixel from an ARGB_8888 formatted pixel
	explicit Pixel(const uint32_t &pix);
	//Construct a Pixel by linearly interpolating between two others
	explicit Pixel(const Pixel &p0, const Pixel &p1, const fixed &t);
	//Write Pixel to an ARGB_8888 formatted destination
	void write(uint32_t &dest) const;
	friend std::ostream& operator<<(std::ostream &os, const Pixel &pix);
private:
	const uint32_t a;
	const uint32_t r;
	const uint32_t g;
	const uint32_t b;
};

std::ostream &operator<<(std::ostream &os, const fixed &f);
std::ostream &operator<<(std::ostream &os, const Pixel &pix);
std::ostream &operator<<(std::ostream &os, const std::complex<fixed> &z);

class BitmapSampler {
public:
	//Construct an object representing a bitmap whose color can be sampled in various ways
	BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight);
	//Sample color at location represented by a complex number, with the bitmap occupying [0,1]x[0,i], and wrapping values outside.
	Pixel bilinearSample(const std::complex<fixed> &z) const;
private:
	const uint32_t *m_srcPixels;
	const uint32_t m_srcWidth;
	const uint32_t m_srcHeight;
};

class ComplexMap {
public:
	virtual ~ComplexMap() {}
	virtual std::complex<fixed> operator()(const std::complex<fixed> &z) const = 0;
};

class MappedBitmap {
public:
	MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight);

	void pullbackSampledBitmap(const ComplexMap &map, const BitmapSampler &src);

private:
	uint32_t *m_destPixels;
	const int m_destWidth;
	const int m_destHeight;
};

class MoebiusTrans : public ComplexMap {
public:
	MoebiusTrans(const std::complex<fixed> &a, const std::complex<fixed> &b, const std::complex<fixed> &c, const std::complex<fixed> &d);
	MoebiusTrans(const float ar, const float ai, const float br, const float bi, const float ci, const float cr, const float dr, const float di);
	virtual std::complex<fixed> operator()(const std::complex<fixed> &z) const;
private:
	const std::complex<fixed> &m_a;
	const std::complex<fixed> &m_b;
	const std::complex<fixed> &m_c;
	const std::complex<fixed> &m_d;
};

#endif /* BITMAPPER_H_ */
