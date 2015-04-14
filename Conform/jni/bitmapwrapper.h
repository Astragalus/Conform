/*
 * bitmapwrapper.h
 *
 *  Created on: Apr 11, 2015
 *      Author: astragalus
 */

#ifndef BITMAPWRAPPER_H_
#define BITMAPWRAPPER_H_

#include <stdint.h>
#include <jni.h>
#include <android/bitmap.h>

class BitmapWrapper {
public:
	BitmapWrapper(JNIEnv *env, jobject bm);
	~BitmapWrapper();

	const bool isValid() const;
	uint32_t *getData() const;
	const uint32_t getWidth() const;
	const uint32_t getHeight() const;

private:
	const bool extractAndCheckBitmapInfo(AndroidBitmapInfo &info) const;
	uint32_t *acquireBitmapData() const;
	const char *bitmapStatusToString(const int status) const;
	const char *bitmapFormatToString(const int fmt) const;

	bool m_isValid;
	JNIEnv *m_env;
	jobject m_bm;
	uint32_t *m_dataPtr;
	uint32_t m_width;
	uint32_t m_height;
};



#endif /* BITMAPWRAPPER_H_ */
