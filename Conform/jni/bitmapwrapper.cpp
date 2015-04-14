/*
 * bitmapwrapper.cpp
 *
 *  Created on: Apr 11, 2015
 *      Author: astragalus
 */

#include "bitmapwrapper.h"
#include "logstream.h"
#include <iostream>

extern logstream<ANDROID_LOG_INFO> INFO;
extern logstream<ANDROID_LOG_DEBUG> DEBUG;
extern logstream<ANDROID_LOG_ERROR> ERROR;


BitmapWrapper::BitmapWrapper(JNIEnv *env, jobject bm) : m_env(env), m_bm(bm), m_isValid(false), m_width(0), m_height(0), m_dataPtr(0) {
	AndroidBitmapInfo info;
	if (extractAndCheckBitmapInfo(info)) {
		m_width = info.width;
		m_height = info.height;
		m_dataPtr = acquireBitmapData();
		m_isValid = !(m_dataPtr == 0);
	}
}
BitmapWrapper::~BitmapWrapper() {
	int status = AndroidBitmap_unlockPixels(m_env, m_bm);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_unlockPixels failed for source bm: " << bitmapStatusToString(status) << std::endl;
	}
}
const bool BitmapWrapper::isValid() const {
	return m_isValid;
}
uint32_t *BitmapWrapper::getData() const {
	return m_dataPtr;
}
const uint32_t BitmapWrapper::getWidth() const {
	return m_width;
}
const uint32_t BitmapWrapper::getHeight() const {
	return m_height;
}

const bool BitmapWrapper::extractAndCheckBitmapInfo(AndroidBitmapInfo &info) const {
	const int status = AndroidBitmap_getInfo(m_env, m_bm, &info);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_getInfo failed for source bm: " << bitmapStatusToString(status) << std::endl;
		return false;
	}
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		ERROR << "Source bitmap format is not 8888 - format: " << bitmapFormatToString(info.format) << std::endl;
		return false;
	}
	return true;
}

uint32_t *BitmapWrapper::acquireBitmapData() const {
	uint32_t *dataPtr;
	int status = AndroidBitmap_lockPixels(m_env, m_bm, (void **) &dataPtr);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_lockPixels failed for source bm: " << bitmapStatusToString(status) << std::endl;
		return 0;
	}
	return dataPtr;
}

const char *BitmapWrapper::bitmapStatusToString(const int status) const {
	switch (status) {
	case ANDROID_BITMAP_RESULT_SUCCESS:
		return "ANDROID_BITMAP_RESULT_SUCCESS";
	case ANDROID_BITMAP_RESULT_BAD_PARAMETER:
		return "ANDROID_BITMAP_RESULT_BAD_PARAMETER";
	case ANDROID_BITMAP_RESULT_JNI_EXCEPTION:
		return "ANDROID_BITMAP_RESULT_JNI_EXCEPTION";
	case ANDROID_BITMAP_RESULT_ALLOCATION_FAILED:
		return "ANDROID_BITMAP_RESULT_ALLOCATION_FAILED";
	default:
		return "Unknown Status";
	}
}

const char *BitmapWrapper::bitmapFormatToString(const int fmt) const {
	switch (fmt) {
	case ANDROID_BITMAP_FORMAT_NONE:
		return "ANDROID_BITMAP_FORMAT_NONE";
	case ANDROID_BITMAP_FORMAT_RGB_565:
		return "ANDROID_BITMAP_FORMAT_RGB_565";
	case ANDROID_BITMAP_FORMAT_RGBA_4444:
		return "ANDROID_BITMAP_FORMAT_RGBA_4444";
	case ANDROID_BITMAP_FORMAT_A_8:
		return "ANDROID_BITMAP_FORMAT_A_8";
	case ANDROID_BITMAP_FORMAT_RGBA_8888:
		return "ANDROID_BITMAP_FORMAT_RGBA_8888";
	default:
		return "Unknown Format";
	}
}




