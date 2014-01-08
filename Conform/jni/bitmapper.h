/*
 * bitmapper.h
 *
 *  Created on: Dec 31, 2013
 *      Author: MTC
 */

#ifndef BITMAPPER_H_
#define BITMAPPER_H_

#include <android/log.h>
#include <sstream>
#include <complex>
#include "fixedpoint/fixed_class.h"

#define  LOG_TAG    "bitmapper"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace bitmapper {


	using namespace std;
	using namespace fixedpoint;

	typedef fixed_point<16> fixed;

	class BitmapSampler {
	public:
		class Pixel {
		public:
			explicit Pixel(const uint32_t &a, const uint32_t &r, const uint32_t &g, const uint32_t &b) : a(a), r(r), g(g), b(b) {}
			explicit Pixel(const uint32_t &pix) : a((pix & 0xFF000000) >> 24), r((pix & 0x00FF0000) >> 16), g((pix & 0x0000FF00) >> 8), b(pix & 0x000000FF) {}
			explicit Pixel(const Pixel &p0, const Pixel &p1, const fixed &t) : a(p0.a*(1-t) + p1.a*t), r(p0.r*(1-t) + p1.r*t), g(p0.g*(1-t) + p1.g*t), b(p0.b*(1-t) + p1.b*t) {}
			ostream& operator<<(ostream &os) const {
				return os << hex << '[' << a << ':' << r << ':' << g << ':' << b << ']';
			}
			void write(uint32_t &dest) const {
				dest |= a << 24 | r << 16 | g << 8 | b;
			}
		private:
			uint32_t a;
			uint32_t r;
			uint32_t g;
			uint32_t b;
		};

		BitmapSampler(const uint32_t *srcPixels, const int srcWidth, const int srcHeight) :
			m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight) {
			LOGI("BitmapSampler object created.  srcWidth = %d, srcHeight = %d",m_srcWidth,m_srcHeight);
		}

		Pixel bilinearSample(const complex<fixed> z) const {
			const fixed xfix = frac(z.real())*fixed(m_srcWidth);
			const fixed yfix = frac(z.imag())*fixed(m_srcHeight);
			const fixed tx = frac(xfix);
			const fixed ty = frac(yfix);
			LOGI("x = %f, y = %f", float(xfix),float(yfix));
			int x0 = xfix-tx;
			int y0 = yfix-ty;
			const Pixel pixDown(Pixel(m_srcPixels[y0*m_srcWidth+x0]),Pixel(m_srcPixels[y0*m_srcWidth+x0+1]),tx);
			const Pixel pixUp(Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0]),Pixel(m_srcPixels[(y0+1)*m_srcWidth+x0+1]),tx);
			const Pixel pixInterped(pixDown, pixUp, ty);
			return pixInterped;
		}

private:

		const uint32_t *m_srcPixels;
		const int m_srcWidth;
		const int m_srcHeight;
	};

	class ComplexMap {
	public:
		virtual ~ComplexMap() {}
		virtual complex<fixed> operator()(const complex<fixed> &z) const = 0;
	};

	class MappedBitmap {
	public:
		MappedBitmap(uint32_t *destPixels, const int destWidth, const int destHeight) :
			m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight) {
			LOGI("MappedBitmap object created.  destWidth = %d, destHeight = %d",m_destWidth,m_destHeight);
		}

		void pullbackSampledBitmap(const ComplexMap &map, const BitmapSampler &src) {
			for (int v = 0; v < m_destHeight; ++v) {
				for (int u = 0; u < m_destWidth; ++u) {
					const complex<fixed> z(fixed(u)/fixed(m_destWidth),fixed(v)/fixed(m_destHeight));
					src.bilinearSample(map(z)).write(m_destPixels[v*m_destWidth+u]);
				}
			}
		}

	private:
		uint32_t *m_destPixels;
		const int m_destWidth;
		const int m_destHeight;
	};

	class MoebiusTrans : public ComplexMap {
	public:
		MoebiusTrans(const complex<fixed> &a, const complex<fixed> &b, const complex<fixed> &c, const complex<fixed> &d) : m_a(a), m_b(b), m_c(c), m_d(d) {}
		MoebiusTrans(const float a, const float b, const float c, const float d) : m_a(complex<fixed>(a,0.0f)), m_b(complex<fixed>(b,0.0f)), m_c(complex<fixed>(c,0.0f)), m_d(complex<fixed>(d,0.0f)) {}
		virtual complex<fixed> operator()(const complex<fixed> &z) const {
			//return (m_a*z+m_b)/(m_c*z+m_d); //DEBUG
			return z;
		}
	private:
		const complex<fixed> &m_a;
		const complex<fixed> &m_b;
		const complex<fixed> &m_c;
		const complex<fixed> &m_d;
	};
}


#endif /* BITMAPPER_H_ */
