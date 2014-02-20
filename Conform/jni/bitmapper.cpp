#include "bitmapper.h"

#include <iomanip>
#include <android/log.h>
#include <sstream>
#include <complex>

#include "fixed_class.h"
#include "logstream.h"

#define  LOG_TAG    "Conform"

using namespace std;

static logstream<ANDROID_LOG_INFO> INFO(LOG_TAG);
static logstream<ANDROID_LOG_DEBUG> DEBUG(LOG_TAG);

//Construct a Pixel from a, r, g, and b values
Pixel::Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b) :
		a(a), r(r), g(g), b(b) {
}
//Construct a Pixel from an ARGB_8888 formatted pixel
Pixel::Pixel(const uint32_t &pix) :
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

//Construct an object representing a bitmap whose color can be sampled in various ways
BitmapSampler::BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight, const int wrapMode) :
	m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight),
	m_xMult(srcWidth<srcHeight?fixpoint(srcHeight)/fixpoint(srcWidth):fixpoint(1)),
	m_yMult(srcWidth>srcHeight?fixpoint(srcWidth)/fixpoint(srcHeight):fixpoint(1)),
	m_wrapMode(wrapMode) {
}
//Sample color at location represented by a complex number, with the bitmap occupying [0,1]x[0,i], and wrapping values outside.
const Pixel BitmapSampler::bilinearSample(const complex<fixpoint> &w) const {
	const fixpoint xfix = ((m_wrapMode == TILE)?frac(w.real()*m_xMult):clamp(w.real()*m_xMult))*fixpoint(m_srcWidth-1);
	const fixpoint yfix = ((m_wrapMode == TILE)?frac(w.imag()*m_yMult):clamp(w.imag()*m_yMult))*fixpoint(m_srcHeight-1);
	const fixpoint tx = frac(xfix);
	const fixpoint ty = frac(yfix);
	const uint32_t x0 = (xfix-tx).toUnsigned(); //x index of left side
	const uint32_t y0 = (yfix-ty).toUnsigned(); //y index of bottom (top?) side
	return Pixel::bilinterp(Pixel(m_srcPixels[y0*m_srcWidth+x0]),Pixel(m_srcPixels[y0*m_srcWidth+x0+1]),
							Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0]),Pixel(m_srcPixels[(y0+1)*m_srcWidth+(x0+1)]),
							tx, ty);
}

MappedBitmap::MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight) :
	m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight),
	m_reInc(fixpoint(1)/fixpoint(m_destWidth-1)), m_imInc(fixpoint(1)/fixpoint(m_destHeight-1)){
}

void MappedBitmap::pullbackSampledBitmap(const MoebiusTrans &map, const BitmapSampler &src) {
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

MoebiusTrans::MoebiusTrans(const complex<fixpoint> &a, const complex<fixpoint> &b, const complex<fixpoint> &c, const complex<fixpoint> &d) :
		m_a(a), m_b(b), m_c(c), m_d(d) {
}

const complex<fixpoint> MoebiusTrans::operator()(const complex<fixpoint> &z) const {
	return (m_a*z+m_b)/oneIfZero(m_c*z+m_d);
}

const MoebiusTrans MoebiusTrans::inv() const {
	const complex<fixpoint> det(m_a*m_d-m_b*m_c);
	if (!isZero(det)) {
		return MoebiusTrans(m_d/det, -m_b/det, -m_c/det, m_a/det);
	} else {
		return identity();
	}
}

MoebiusTrans MoebiusTrans::identity() {
	return MoebiusTrans(complex<fixpoint>(1,0),complex<fixpoint>(0,0),complex<fixpoint>(0,0),complex<fixpoint>(1,0));
}
//
//MoebiusTrans& MoebiusTrans::operator*=(const MoebiusTrans &g) {
//	m_a = m_a*g.m_a+m_b*g.m_c;
//	m_b = m_a*g.m_b+m_b*g.m_d;
//	m_c = m_c*g.m_a+m_d*g.m_c;
//	m_d = m_c*g.m_b+m_d*g.m_d;
//	return *this;
//}

const MoebiusTrans MoebiusTrans::operator*(const MoebiusTrans &g) const {
	return MoebiusTrans(m_a*g.m_a+m_b*g.m_c, m_a*g.m_b+m_b*g.m_d, m_c*g.m_a+m_d*g.m_c, m_c*g.m_b+m_d*g.m_d);
}

ostream &operator<<(ostream &os, const MoebiusTrans &mt) {
	return os << "(" << mt.m_a << "z+" << mt.m_b << ")/(" << mt.m_c << "z+" << mt.m_d << ")";
}
