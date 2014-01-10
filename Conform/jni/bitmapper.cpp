#include "bitmapper.h"

#include <android/log.h>
#include <sstream>
#include <complex>

#include "fixedpoint/fixed_class.h"

#define  LOG_TAG    "bitmapper"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

Pixel::Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b) :
		a(a), r(r), g(g), b(b) {
}
Pixel::Pixel(const uint32_t &pix) :
		a((pix & 0xFF000000) >> 24), r((pix & 0x00FF0000) >> 16), g((pix & 0x0000FF00) >> 8), b(pix & 0x000000FF) {
}
Pixel::Pixel(const Pixel &p0, const Pixel &p1, const fixed &t) :
		a((p0.a*(1-t) + p1.a*t).toUnsigned()), r((p0.r*(1-t) + p1.r*t).toUnsigned()), g((p0.g*(1-t) + p1.g*t).toUnsigned()), b((p0.b*(1-t) + p1.b*t).toUnsigned()) {
}
std::ostream& Pixel::operator<<(std::ostream &os) const {
	return os << std::hex << '[' << a << ':' << r << ':' << g << ':' << b << ']';
}
void Pixel::write(uint32_t &dest) const {
	dest |= a << 24 | r << 16 | g << 8 | b;
}


BitmapSampler::BitmapSampler(const uint32_t *srcPixels, const uint32_t srcWidth, const uint32_t srcHeight) :
	m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight) {
	LOGI("BitmapSampler object created.  srcWidth = %d, srcHeight = %d",m_srcWidth,m_srcHeight);
}

Pixel BitmapSampler::bilinearSample(const std::complex<fixed> &z) const {
	const fixed xfix = frac(z.real())*fixed(m_srcWidth);
	const fixed yfix = frac(z.imag())*fixed(m_srcHeight);
	const fixed tx = frac(xfix);
	const fixed ty = frac(yfix);
	LOGI("x = %f, y = %f", xfix.toFloat(),yfix.toFloat());
	uint32_t x0 = (xfix-tx).toUnsigned();
	uint32_t y0 = (yfix-ty).toUnsigned();
	const Pixel pixDown(Pixel(m_srcPixels[y0*m_srcWidth+x0]),Pixel(m_srcPixels[y0*m_srcWidth+x0+1]),tx);
	const Pixel pixUp(Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0]),Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0+1]),tx);
	const Pixel pixInterped(pixDown, pixUp, ty);
	return pixInterped;
}


MappedBitmap::MappedBitmap(uint32_t *destPixels, const uint32_t destWidth, const uint32_t destHeight) :
	m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight) {
	LOGI("MappedBitmap object created.  destWidth = %d, destHeight = %d",m_destWidth,m_destHeight);
}

void MappedBitmap::pullbackSampledBitmap(const ComplexMap &map, const BitmapSampler &src) {
	for (int v = 0; v < m_destHeight; ++v) {
		for (int u = 0; u < m_destWidth; ++u) {
			const std::complex<fixed> z(fixed(u)/fixed(m_destWidth),fixed(v)/fixed(m_destHeight));
			src.bilinearSample(map(z)).write(m_destPixels[v*m_destWidth+u]);
		}
	}
}


MoebiusTrans::MoebiusTrans(const std::complex<fixed> &a, const std::complex<fixed> &b, const std::complex<fixed> &c, const std::complex<fixed> &d) :
		m_a(a), m_b(b), m_c(c), m_d(d) {
}
MoebiusTrans::MoebiusTrans(const float a, const float b, const float c, const float d) :
		m_a(std::complex<fixed>(a,0.0f)), m_b(std::complex<fixed>(b,0.0f)), m_c(std::complex<fixed>(c,0.0f)), m_d(std::complex<fixed>(d,0.0f)) {
}
std::complex<fixed> MoebiusTrans::operator()(const std::complex<fixed> &z) const {
	//return (m_a*z+m_b)/(m_c*z+m_d);
	return z; //DEBUG
}
