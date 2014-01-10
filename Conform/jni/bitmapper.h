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
	explicit Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b);
	explicit Pixel(const uint32_t &pix);
	explicit Pixel(const Pixel &p0, const Pixel &p1, const fixed &t);
	std::ostream& operator<<(std::ostream &os) const;
	void write(uint32_t &dest) const;
private:
	const uint32_t a;
	const uint32_t r;
	const uint32_t g;
	const uint32_t b;
};

class BitmapSampler {
public:
	BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight);

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
	MoebiusTrans(const float a, const float b, const float c, const float d);
	virtual std::complex<fixed> operator()(const std::complex<fixed> &z) const;
private:
	const std::complex<fixed> &m_a;
	const std::complex<fixed> &m_b;
	const std::complex<fixed> &m_c;
	const std::complex<fixed> &m_d;
};

#endif /* BITMAPPER_H_ */
