/*
 * bitmapper.h
 *
 *  Created on: Dec 31, 2013
 *      Author: MTC
 */

#ifndef BITMAPPER_H_
#define BITMAPPER_H_

#include <complex>
#include <fixedpoint/fixed_class.h>

namespace bitmapper {

	using namespace std;
	using namespace fixedpoint;

	typedef fixed_point<16> fixed;

	class BitmapSampler {
	public:
		BitmapSampler(const uint32_t *srcPixels, const int srcWidth, const int srcHeight) :
			m_srcPixels(srcPixels), m_srcWidth(srcWidth), m_srcHeight(srcHeight) {}

		const uint32_t bilinearSample(const complex<fixed> z) const {
			const fixed xfix = mod1(z.real())*fixed(m_srcWidth-1);
			const fixed yfix = mod1(z.imag())*fixed(m_srcHeight-1);
			const fixed tx = frac(xfix);
			const fixed ty = frac(yfix);
			int x0 = trunc(xfix-tx);
			int y0 = trunc(yfix-ty);
			const uint32_t pixDown = interpPixels(m_srcPixels[y0*m_srcWidth+x0],m_srcPixels[y0*m_srcWidth+x0+1],1-tx);
			const uint32_t pixUp = interpPixels(m_srcPixels[(y0+1)*m_srcWidth+x0],m_srcPixels[(y0+1)*m_srcWidth+x0+1],1-tx);
			const uint32_t pixInterped = interpPixels(pixDown, pixUp, 1-ty);
			return pixInterped;
		}

private:
		const uint32_t interpPixels(const uint32_t &pixA, const uint32_t &pixB, const fixed &t) const {
			const int32_t A_A = (pixA & 0xFF000000) >> 24;
			const int32_t R_A = (pixA & 0x00FF0000) >> 16;
			const int32_t G_A = (pixA & 0x0000FF00) >> 8;
			const int32_t B_A = (pixA & 0x000000FF);
			const int32_t A_B = (pixB & 0xFF000000) >> 24;
			const int32_t R_B = (pixB & 0x00FF0000) >> 16;
			const int32_t G_B = (pixB & 0x0000FF00) >> 8;
			const int32_t B_B = (pixB & 0x000000FF);
			const uint32_t A_t = (uint32_t) trunc(fixed(A_A)*t + fixed(A_B)*(1-t));
			const uint32_t R_t = (uint32_t) trunc(fixed(R_A)*t + fixed(R_B)*(1-t));
			const uint32_t G_t = (uint32_t) trunc(fixed(G_A)*t + fixed(G_B)*(1-t));
			const uint32_t B_t = (uint32_t) trunc(fixed(B_A)*t + fixed(B_B)*(1-t));
			const uint32_t result = A_t << 24 + R_t << 16 + G_t << 8 + B_t;
			return result;
		}

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
			m_destPixels(destPixels), m_destWidth(destWidth), m_destHeight(destHeight) {}

		void pullbackSampledBitmap(const ComplexMap &map, const BitmapSampler &src) {
			for (int v = 0; v < m_destHeight; ++v) {
				for (int u = 0; u < m_destWidth; ++u) {
					const complex<fixed> z(fixed(u)/(m_destWidth-1),fixed(v)/(m_destHeight-1));
					m_destPixels[v*m_destWidth+u] = src.bilinearSample(map(z));
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
			return (m_a*z+m_b)/(m_c*z+m_d);
		}
	private:
		const complex<fixed> &m_a;
		const complex<fixed> &m_b;
		const complex<fixed> &m_c;
		const complex<fixed> &m_d;
	};
}


#endif /* BITMAPPER_H_ */
