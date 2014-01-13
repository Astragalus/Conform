#include "bitmapper.h"

#include <android/log.h>
#include <sstream>
#include <complex>

#include "fixedpoint/fixed_class.h"

#define  LOG_TAG    "bitmapper"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

//Construct a Pixel from a, r, g, and b values
Pixel::Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b) :
		a(a), r(r), g(g), b(b) {
}
//Construct a Pixel from an ARGB_8888 formatted pixel
Pixel::Pixel(const uint32_t &pix) :
		a((pix & 0xFF000000) >> 24), r((pix & 0x00FF0000) >> 16), g((pix & 0x0000FF00) >> 8), b(pix & 0x000000FF) {
}
//Construct a Pixel by linearly interpolating between two others
Pixel::Pixel(const Pixel &p0, const Pixel &p1, const fixed &t) :
		a((p0.a*(1-t) + p1.a*t).toUnsigned()), r((p0.r*(1-t) + p1.r*t).toUnsigned()), g((p0.g*(1-t) + p1.g*t).toUnsigned()), b((p0.b*(1-t) + p1.b*t).toUnsigned()) {
}
//Write Pixel to an ARGB_8888 formatted destination
void Pixel::write(uint32_t &dest) const {
	dest |= a << 24 | r << 16 | g << 8 | b;
}

std::ostream& operator<<(std::ostream &os, const fixed& f) {
	return os << f.toFloat();
}

std::ostream& operator<<(std::ostream &os, const Pixel &pix) {
	return os << std::hex << '[' << pix.a << ':' << pix.r << ':' << pix.g << ':' << pix.b << ']';
}

std::ostream& operator<<(std::ostream &os, const std::complex<fixed> &z) {
	return os << '(' << z.real() << '+' << z.imag() << "i)";
}

//Construct an object representing a bitmap whose color can be sampled in various ways
BitmapSampler::BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight) :
	m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight) {
	LOGI("BitmapSampler object created.  srcWidth = %d, srcHeight = %d",m_srcWidth,m_srcHeight);
}
//Sample color at location represented by a complex number, with the bitmap occupying [0,1]x[0,i], and wrapping values outside.
Pixel BitmapSampler::bilinearSample(const std::complex<fixed> &z) const {
	const fixed xfix = frac(z.real())*fixed(m_srcWidth);
	const fixed yfix = frac(z.imag())*fixed(m_srcHeight);
	const fixed tx = frac(xfix);
	const fixed ty = frac(yfix);
	const uint32_t x0 = (xfix-tx).toUnsigned(); //x index of left side
	const uint32_t y0 = (yfix-ty).toUnsigned(); //y index of bottom (top?) side
	const Pixel pixDown(Pixel(m_srcPixels[y0*m_srcWidth+x0]),Pixel(m_srcPixels[y0*m_srcWidth+x0+1]),tx); //bottom, interp'd in x
	const Pixel pixUp(Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0]),Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0+1]),tx); //top, interp'd in x
	const Pixel pixInterped(pixDown, pixUp, ty); //interp'd interp'ds
	//LOGI("bilinearSample: x0=%d, y0=%d, tx=%1.4f, ty=%1.4f",x0,y0,tx.toFloat(),ty.toFloat());
	return pixInterped;
}


MappedBitmap::MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight) :
	m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight) {
	LOGI("MappedBitmap object created.  destWidth = %d, destHeight = %d",m_destWidth,m_destHeight);
}

void MappedBitmap::pullbackSampledBitmap(const ComplexMap &map, const BitmapSampler &src) {
	LOGI("pullbackSampledBitmap: m_destWidth=%d, m_destHeight=%d",m_destWidth, m_destHeight);
	for (int v = 0; v < m_destHeight; ++v) {
		for (int u = 0; u < m_destWidth; ++u) {
			const std::complex<fixed> z(fixed(u)/fixed(m_destWidth),fixed(v)/fixed(m_destHeight));
			const std::complex<fixed> w(map(z));
			uint32_t &destPix = m_destPixels[v*m_destWidth+u];
			src.bilinearSample(w).write(destPix); //sample color from src at map(z) and write to dest
			//LOGI("u=%d, v=%d, z=%1.4f+%1.4fi, w=%1.4f+%1.4fi, pix=%#010x",u,v,z.real().toFloat(),z.imag().toFloat(),w.real().toFloat(),w.imag().toFloat(),destPix);
		}
	}
}


MoebiusTrans::MoebiusTrans(const std::complex<fixed> &a, const std::complex<fixed> &b, const std::complex<fixed> &c, const std::complex<fixed> &d) :
		m_a(a), m_b(b), m_c(c), m_d(d) {
}
MoebiusTrans::MoebiusTrans(const float ar, const float ai, const float br, const float bi, const float ci, const float cr, const float dr, const float di) :
		m_a(std::complex<fixed>(ar,ai)), m_b(std::complex<fixed>(br,bi)), m_c(std::complex<fixed>(cr,ci)), m_d(std::complex<fixed>(dr,di)) {
}
std::complex<fixed> MoebiusTrans::operator()(const std::complex<fixed> &z) const {
	//return z - std::complex<fixed>(fixed::createRaw(0x8000),fixed::createRaw(0x8000));
	return (m_a*z+m_b)/(m_c*z+m_d);
}
