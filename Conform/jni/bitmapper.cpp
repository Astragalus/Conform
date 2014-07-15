#include "bitmapper.h"

#include <iomanip>
#include <android/log.h>
#include <sstream>
#include <complex>
#include <algorithm>

using namespace std;

#include "fixed_class.h"
#include "logstream.h"

#define  LOG_TAG    "Conform"

static logstream<ANDROID_LOG_INFO> INFO(LOG_TAG);
static logstream<ANDROID_LOG_DEBUG> DEBUG(LOG_TAG);

const complex<fixpoint> divZeroGuard(complex<fixpoint>&& z) {
	return z + (fixpoint(!(z.real().intValue|z.imag().intValue)) >> 16);
}

//Pixel-----------------------------------------------

Pixel::Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b) :
		a(a), r(r), g(g), b(b) {
}
Pixel::Pixel(const uint32_t &pix) : //Construct a Pixel from an ARGB_8888 formatted pixel
		a((pix & 0xFF000000) >> 24), r((pix & 0x00FF0000) >> 16), g((pix & 0x0000FF00) >> 8), b(pix & 0x000000FF) {
}
//Biliearly interpolate between four pixels (left/right,up/down) using two parameters (horiz, vert) between 0 and 1.
const Pixel Pixel::bilinterp(const Pixel &dl, const Pixel &dr, const Pixel &ul, const Pixel &ur, const fixpoint &h, const fixpoint &v) {
	const fixpoint hh(1-h);
	const fixpoint vv(1-v);
	const fixpoint t_dl(hh*vv);
	const fixpoint t_dr(h*vv);
	const fixpoint t_ul(hh*v);
	const fixpoint t_ur(h*v);
	return Pixel((dl.a*t_dl+dr.a*t_dr+ul.a*t_ul+ur.a*t_ur).toUnsigned(),
				 (dl.r*t_dl+dr.r*t_dr+ul.r*t_ul+ur.r*t_ur).toUnsigned(),
				 (dl.g*t_dl+dr.g*t_dr+ul.g*t_ul+ur.g*t_ur).toUnsigned(),
				 (dl.b*t_dl+dr.b*t_dr+ul.b*t_ul+ur.b*t_ur).toUnsigned());
}
//Write Pixel to an ARGB_8888 formatted destination
void Pixel::write(uint32_t &dest) const {
	dest |= a << 24 | r << 16 | g << 8 | b;
}

ostream& operator<<(ostream& os, const Pixel& p) {
	return os << hex << '[' << p.a << ':' << p.r << ':' << p.g << ':' << p.b << ']';
}

ostream &operator<<(ostream &os, const fixed_point<16> &f) {
	return os << (f.intValue >> 16) << setw(15) << ios::right <<  '.' << setfill('0') << (((1<<16)-1)&f.intValue); //FIXME: without converting to floats!
}

//BitmapSampler----------------------------------------

BitmapSampler::BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const int wrapMode) :
	m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight),
	m_xMult(srcWidth<srcHeight?fixpoint(srcHeight)/fixpoint(srcWidth):fixpoint(1)),
	m_yMult(srcWidth>srcHeight?fixpoint(srcWidth)/fixpoint(srcHeight):fixpoint(1)),
	m_wrapMode(wrapMode) {
}

const Pixel BitmapSampler::bilinearSample(const complex<fixpoint> &w) const {
	const fixpoint xfix = wrapOrClamp(((w.real()+1)/2)*m_xMult, m_wrapMode)*fixpoint(m_srcWidth-1);
	const fixpoint yfix = wrapOrClamp(((w.imag()+1)/2)*m_yMult, m_wrapMode)*fixpoint(m_srcHeight-1);
	const fixpoint tx = frac(xfix);
	const fixpoint ty = frac(yfix);
	const uint32_t x0 = (xfix-tx).toUnsigned(); //x index of left side
	const uint32_t y0 = (yfix-ty).toUnsigned(); //y index of bottom (top?) side
	const Pixel result(Pixel::bilinterp(Pixel(m_srcPixels[y0*m_srcWidth+x0]),
										Pixel(m_srcPixels[y0*m_srcWidth+x0+1]),
										Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0]),
										Pixel(m_srcPixels[(y0+1)*m_srcWidth+(x0+1)]),
										tx, ty));
	return result;
}
//MappedBitmap----------------------------------------

MappedBitmap::MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight) :
	m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight),
	m_reInc(fixpoint(2)/fixpoint(m_destWidth-1)), m_imInc(fixpoint(2)/fixpoint(m_destHeight-1)) {
}

void MappedBitmap::pullbackSampledBitmap(const BlaschkeMap& map, const BitmapSampler& src) {
	DEBUG << "pullbackSampledBitmap [START] map=[" << map << "]" << endl;
	fixpoint zim(-1);
	for (int v = 0; v < m_destHeight; ++v) {
		fixpoint zre(-1);
		for (int u = 0; u < m_destWidth; ++u) {
			const complex<fixpoint> z(zre,zim);
			const complex<fixpoint> w(map(z));
			if (((u % (m_destWidth/4)) == 0)&&((v % (m_destHeight/4)) == 0)) {
				DEBUG << "u,v=[" << u << "," << v << "], z=[" << z << "], map(z)=[" << w << "], map=[" << map << "]" << endl;
			}
			src.bilinearSample(w).write(m_destPixels[v*m_destWidth+u]); //sample color from src at map(z) and write to dest
			zre += m_reInc;
		}
		zim += m_imInc;
	}
	DEBUG << "pullbackSampledBitmap [END]" << endl;
}

//MoebiusTrans----------------------------------------
MobiusTrans::MobiusTrans(const complex<fixpoint>& a, const complex<fixpoint>& b, const complex<fixpoint>& c, const complex<fixpoint>& d) : m_a(a), m_b(b), m_c(c), m_d(d), m_isIdentity(false) {
}
MobiusTrans::MobiusTrans() : m_a(complex<fixpoint>(1,0)), m_b(complex<fixpoint>(0,0)), m_c(complex<fixpoint>(0,0)), m_d(complex<fixpoint>(1,0)), m_isIdentity(true) {
}

MobiusTrans::MobiusTrans(const MobiusTrans& mt) : m_a(mt.m_a), m_b(mt.m_b), m_c(mt.m_c), m_d(mt.m_d), m_isIdentity(mt.m_isIdentity) {
}

MobiusTrans& MobiusTrans::operator=(const MobiusTrans& mt) {
	m_a=mt.m_a;
	m_b=mt.m_b;
	m_c=mt.m_c;
	m_d=mt.m_d;
	m_isIdentity=mt.m_isIdentity;
	return *this;
}

const MobiusTrans MobiusTrans::hyperbolicIsometry(complex<fixpoint>&& zero) {
	return MobiusTrans(ONE,-zero,-conj(zero),ONE);
}

const complex<fixpoint> MobiusTrans::operator()(const complex<fixpoint> &z) const {
	return (m_a*z+m_b)/divZeroGuard(complex<fixpoint>(m_c*z+m_d));
}
const MobiusTrans MobiusTrans::operator|(const MobiusTrans& f) const {
	return MobiusTrans(m_a*f.m_a + m_b*f.m_c, m_a*f.m_b + m_b*f.m_d, m_c*f.m_a + m_d*f.m_c, m_c*f.m_b + m_d*f.m_d);
}
const MobiusTrans MobiusTrans::operator-() const {
	const complex<fixpoint> det = divZeroGuard(complex<fixpoint>(m_a*m_d-m_b*m_c));
	return MobiusTrans(m_d/det, -m_b/det, -m_c/det, m_a/det);
}

ostream& operator<<(ostream &os, const MobiusTrans& mobius) {
	return os << '[' << mobius.m_a << "z+" << mobius.m_b << "]/[" << mobius.m_c << "z+" << mobius.m_d << ')';
}

const MobiusTrans MobiusTrans::identity = MobiusTrans();

const bool MobiusTrans::isIdentity() const {
	return m_isIdentity;
}

//BlaschkeMap----------------------------------------

BlaschkeMap::BlaschkeMap() : m_factors({MobiusTrans::identity}), m_numFactors(0) {
}
BlaschkeMap::BlaschkeMap(const MobiusTrans& a) : m_factors({a}), m_numFactors(1) {
}
BlaschkeMap::BlaschkeMap(const MobiusTrans& a, const MobiusTrans& b) : m_factors({a,b}), m_numFactors(2) {
}

BlaschkeMap::BlaschkeMap(const BlaschkeMap& g) : m_numFactors(g.m_numFactors), m_lhs(g.m_lhs) {
	copy(&g.m_factors[0], &g.m_factors[0] + g.m_numFactors, &m_factors[0]);
}

const complex<fixpoint> BlaschkeMap::operator()(const complex<fixpoint> &z) const {
	complex<fixpoint> w(ONE);
	for (int i = 0; i < m_numFactors; ++i) {
		w *= m_factors[i](z);
	}
	return m_lhs.isIdentity() ? w : m_lhs(w);
}
BlaschkeMap& operator|(const MobiusTrans& a, BlaschkeMap& b) {
	b.m_lhs = (a|b.m_lhs);
	return b;
}
BlaschkeMap& operator|(BlaschkeMap& b, const MobiusTrans& a) {
	for (int i = 0; i < b.m_numFactors; ++i) {
		b.m_factors[i] = (b.m_factors[i]|a);
	}
	return b;
}

BlaschkeMap& BlaschkeMap::operator*=(const BlaschkeMap& f) {
	if (m_numFactors + f.m_numFactors < BlaschkeMap::max_factors) {
		for (int i = 0; i < f.m_numFactors; ++i) {
			(*this) *= f.m_factors[i];
		}
	}
	return *this;
}
BlaschkeMap& BlaschkeMap::operator*=(const MobiusTrans& a) {
	if (m_numFactors < BlaschkeMap::max_factors) {
		m_factors[m_numFactors++] = a;
	}
	return *this;
}

ostream& operator<<(ostream &os, const BlaschkeMap& blasch) {
//	os << blasch.m_lhs << 'o';
	os << blasch.m_factors[0];
	for (int i = 1; i < BlaschkeMap::max_factors; ++i) {
		if (!blasch.m_factors[i].isIdentity()) {
			os << '*' << blasch.m_factors[i];
		}
	}
	return os;
}
