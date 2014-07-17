/*
Copyright (c) 2007, Markus Trenkwalder

All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation 
  and/or other materials provided with the distribution.

* Neither the name of the library's copyright owner nor the names of its 
  contributors may be used to endorse or promote products derived from this 
  software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Modified by Matthew Cushman in 2014
*/

#ifndef FIXEDP_CLASS_H_INCLUDED
#define FIXEDP_CLASS_H_INCLUDED

#include <iostream>
#include "fixed_func.h"

#define FRAC_MASK(p) ((1<<(p))-1)
#define INT_MASK(p) ((-1)^FRAC_MASK(p))

// The template argument p in all of the following functions refers to the 
// fixed point precision (e.g. p = 8 gives 24.8 fixed point functions).

template <int p>
struct fixed_point {
	int32_t intValue;
	
	fixed_point() : intValue(0) {}
	fixed_point(const fixed_point& f) : intValue(f.intValue) {}
	/*explicit*/ fixed_point(int32_t i) : intValue(i << p) {}
	/*explicit*/ fixed_point(uint32_t i) : intValue(i << p) {}
	explicit fixed_point(float f) : intValue(float2fix<p>(f)) {}
	
	fixed_point& operator += (fixed_point r) { intValue += r.intValue; return *this; }
	fixed_point& operator -= (fixed_point r) { intValue -= r.intValue; return *this; }
	fixed_point& operator *= (fixed_point r) { intValue = fixmul<p>(intValue, r.intValue); return *this; }
	fixed_point& operator /= (fixed_point r) { intValue = fixdiv<p>(intValue, r.intValue); return *this; }
	fixed_point& operator |= (int32_t r) { intValue |= r; return *this; }
	fixed_point& operator &= (int32_t r) { intValue &= r; return *this; }
	fixed_point& operator *= (int32_t r) { intValue *= r; return *this; }
	fixed_point& operator /= (int32_t r) { intValue /= r; return *this; }
	fixed_point& operator >>= (int32_t r) { intValue >>= r; return *this; }
	
	fixed_point operator - () const { fixed_point x; x.intValue = -intValue; return x; }
	fixed_point operator + (fixed_point r) const { fixed_point x = *this; x += r; return x;}
	fixed_point operator - (fixed_point r) const { fixed_point x = *this; x -= r; return x;}
	fixed_point operator * (fixed_point r) const { fixed_point x = *this; x *= r; return x;}
	fixed_point operator / (fixed_point r) const { fixed_point x = *this; x /= r; return x;}
	fixed_point operator | (int32_t r) const { fixed_point x = *this; x |= r; return x;}
	fixed_point operator & (int32_t r) const { fixed_point x = *this; x &= r; return x;}

	fixed_point operator >> (int32_t r) const { fixed_point x = *this; x.intValue >>= r; return x;}
	
	bool operator == (fixed_point r) const { return intValue == r.intValue; }
	bool operator != (fixed_point r) const { return !(*this == r); }
	bool operator <  (fixed_point r) const { return intValue < r.intValue; }
	bool operator >  (fixed_point r) const { return intValue > r.intValue; }
	bool operator <= (fixed_point r) const { return intValue <= r.intValue; }
	bool operator >= (fixed_point r) const { return intValue >= r.intValue; }
	bool operator ! () const { return !intValue; }

	fixed_point operator + (int32_t r) const { fixed_point x = *this; x += r; return x;}
	fixed_point operator - (int32_t r) const { fixed_point x = *this; x -= r; return x;}
	fixed_point operator * (int32_t r) const { fixed_point x = *this; x *= r; return x;}
	fixed_point operator / (int32_t r) const { fixed_point x = *this; x /= r; return x;}

//	float toFloat() const { return fix2float<p>(intValue); }
	uint32_t toUnsigned() const { return ((uint32_t)intValue) >> p; }
};

// Perform a fixed point interpolation using a 64-bit intermediate result to
// prevent overflow problems.
template <int p>
inline uint32_t interp(uint32_t a, uint32_t b, fixed_point<p> t)
{
	return (uint32_t)((((uint64_t)a)*((uint64_t)(1 - t).intValue) + ((uint64_t)b)*((uint64_t)t.intValue)) >> p);
}


// Specializations for use with plain integers
template <int p>
inline fixed_point<p> operator + (int32_t a, fixed_point<p> b)
{ return b + a; }

template <int p>
inline fixed_point<p> operator - (int32_t a, fixed_point<p> b)
{ return -b + a; }

template <int p>
inline fixed_point<p> operator * (int32_t a, fixed_point<p> b)
{ return b * a; }

template <int p>
inline fixed_point<p> operator / (int32_t a, fixed_point<p> b)
{ fixed_point<p> r(a); r /= b; return r; }

// math functions
// no default implementation

template <int p>
inline fixed_point<p> sin(fixed_point<p> a);

template <int p>
inline fixed_point<p> cos(fixed_point<p> a);

template <int p>
inline fixed_point<p> sqrt(fixed_point<p> a);

template <int p>
inline fixed_point<p> rsqrt(fixed_point<p> a);

template <int p>
inline fixed_point<p> inv(fixed_point<p> a);

template <int p>
inline fixed_point<p> abs(fixed_point<p> a)
{ 
	fixed_point<p> r; 
	r.intValue = a.intValue > 0 ? a.intValue : -a.intValue; 
	return r; 
}

template <int p>
inline fixed_point<p> trunc(const fixed_point<p> a)
{
	fixed_point<p> r;
	r.intValue = a.intValue & INT_MASK(p);
	return r;
}

template <int p>
inline fixed_point<p> frac(const fixed_point<p> a)
{
	fixed_point<p> r;
	r.intValue = a.intValue & FRAC_MASK(p);
	return r;
}

template <int p>
inline fixed_point<p> mod(const fixed_point<p> a, const fixed_point<p> modulus)
{
	fixed_point<p> r;
	r.intValue = ((uint32_t)a.intValue) % ((uint32_t)modulus.intValue);
	return r;
}

template <int p>
inline fixed_point<p> clamp(const fixed_point<p> a)
{
	fixed_point<p> r;
	r.intValue = (a.intValue & -(a.intValue > 0) & -(a.intValue <= (1<<p))) | ((a.intValue > (1<<p) << p));
	return r;
}

template <int p>
inline fixed_point<p> wrapOrClamp(const fixed_point<p> a, const int wrapMode) {
	fixed_point<p> r;
	r.intValue = ((wrapMode-1)&(a.intValue & ((1<<p)-1))) | (((-wrapMode) & (a.intValue & -(a.intValue > 0) & -(a.intValue <= (1<<p)))) | ((a.intValue > (1<<p) << p)));
	return r;
}

template <int p>
inline fixed_point<p> fixinterp(fixed_point<p> a, fixed_point<p> b, fixed_point<p> t) {
	fixed_point<p> r;
	r.intValue = fixinterp(a.intValue, b.intValue, t);
	return r;
}

// specializations for 16.16 format

template <>
inline fixed_point<16> sin(fixed_point<16> a)
{
	fixed_point<16> r;
	r.intValue = fixsin16(a.intValue);
	return r;
}

template <>
inline fixed_point<16> cos(fixed_point<16> a)
{
	fixed_point<16> r;
	r.intValue = fixcos16(a.intValue);
	return r;
}


template <>
inline fixed_point<16> sqrt(fixed_point<16> a)
{
	fixed_point<16> r;
	r.intValue = fixsqrt16(a.intValue);
	return r;
}

template <>
inline fixed_point<16> rsqrt(fixed_point<16> a)
{
	fixed_point<16> r;
	r.intValue = fixrsqrt16(a.intValue);
	return r;
}

template <>
inline fixed_point<16> inv(fixed_point<16> a)
{
	fixed_point<16> r;
	r.intValue = fixinv<16>(a.intValue);
	return r;
}

// The multiply accumulate case can be optimized.
template <int p>
inline fixed_point<p> multiply_accumulate(
	int count, 
	const fixed_point<p> *a,
	const fixed_point<p> *b)
{
	long long result = 0;
	for (int i = 0; i < count; ++i)
		result += static_cast<long long>(a[i].intValue) * b[i].intValue;
	fixed_point<p> r;
	r.intValue = static_cast<int>(result >> p);
	return r;
}

#endif

