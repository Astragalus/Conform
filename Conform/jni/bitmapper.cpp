#include "bitmapper.h"

#include <iomanip>
#include <android/log.h>
#include <sstream>
#include <complex>
#include <algorithm>

#include "fixed_class.h"
#include "logstream.h"

#define  LOG_TAG    "Conform"

using namespace std;

static logstream<ANDROID_LOG_INFO> INFO(LOG_TAG);
static logstream<ANDROID_LOG_DEBUG> DEBUG(LOG_TAG);

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
	return os << setw(5) << f.toFloat();
}

//MappedBitmap----------------------------------------

MappedBitmap::MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight) :
	m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight),
	m_reInc(fixpoint(1)/fixpoint(m_destWidth-1)), m_imInc(fixpoint(1)/fixpoint(m_destHeight-1)) {
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
}

const MobiusTrans MobiusTrans::hyperbolicIsometry(const complex<fixpoint>& zero) {
	return MobiusTrans(ONE,-zero,-conj(zero),ONE);
}

const complex<fixpoint> MobiusTrans::operator()(const complex<fixpoint> &z) const {
	return (m_a*z+m_b)/oneIfZero(m_c*z+m_d);
}
const MobiusTrans MobiusTrans::operator|(const MobiusTrans& f) const {
	return MobiusTrans(m_a*f.m_a + m_b*f.m_c, m_a*f.m_b + m_b*f.m_d, m_c*f.m_a + m_d*f.m_c, m_c*f.m_b + m_d*f.m_d);
}
const MobiusTrans MobiusTrans::operator-() const {
	const complex<fixpoint> det(m_a*m_d-m_b*m_c);
	if (!isZero(det)) {
		return MobiusTrans(m_d/det, -m_b/det, -m_c/det, m_a/det);
	} else {
		return identity;
	}
}

ostream& operator<<(ostream &os, const MobiusTrans& mobius) {
	return os << "(" << mobius.m_a << "z+" << mobius.m_b << ")/(" << mobius.m_c << "z+" << mobius.m_d << ")";
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

BlaschkeMap::BlaschkeMap(const BlaschkeMap& g) : m_numFactors(g.m_numFactors), m_lhs(g.m_lhs), m_rhs(g.m_rhs) {
	copy(&g.m_factors[0], &g.m_factors[0] + g.m_numFactors, &m_factors[0]);
}

const complex<fixpoint> BlaschkeMap::operator()(const complex<fixpoint> &z) const {
	const complex<fixpoint> zz = m_rhs(z);
	complex<fixpoint> w(ONE);
	for (int i = 0; i < m_numFactors; ++i) {
		w *= m_factors[i](zz);
	}
	return m_lhs(w);
}
BlaschkeMap& operator|(const MobiusTrans& a, BlaschkeMap& b) {
	b.m_lhs = (a|b.m_lhs);
	return b;
}
BlaschkeMap& operator|(BlaschkeMap& b, const MobiusTrans& a) {
	b.m_rhs = (b.m_rhs|a);
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
	os << blasch.m_lhs << 'o';
	os << blasch.m_factors[0];
	for (int i = 1; i < BlaschkeMap::max_factors; ++i) {
		if (!blasch.m_factors[i].isIdentity()) {
			os << '*' << blasch.m_factors[i];
		}
	}
	os << 'o' << blasch.m_rhs;
	return os;
}
