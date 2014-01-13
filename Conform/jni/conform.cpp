#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include "bitmapper.h"

#define  LOG_TAG    "conform"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define ANDROID_BITMAP_RESULT_SUCCESS            0
#define ANDROID_BITMAP_RESULT_BAD_PARAMETER     -1
#define ANDROID_BITMAP_RESULT_JNI_EXCEPTION     -2
#define ANDROID_BITMAP_RESULT_ALLOCATION_FAILED -3

const char *bitmapStatusToString(const int status) {
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

const char *bitmapFormatToString(const int fmt) {
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

extern "C" {
	JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmaps(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jfloat x, jfloat y);
}

JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmaps(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jfloat x, jfloat y) {
	LOGI("pullbackBitmaps called...");

	int status = 0;

	AndroidBitmapInfo sourceInfo;
	status = AndroidBitmap_getInfo(env, bmSource, &sourceInfo);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		LOGE("AndroidBitmap_getInfo failed for source bm: %s",bitmapStatusToString(status));
		return status;
	}
	if (sourceInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGE("Source bitmap format is not 8888 - format: %s",bitmapFormatToString(sourceInfo.format));
	}
	uint32_t *sourcePtr;
	status = AndroidBitmap_lockPixels(env, bmSource, (void **) &sourcePtr);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		LOGE("AndroidBitmap_lockPixels failed for source bm: %s",bitmapStatusToString(status));
		return status;
	}
	AndroidBitmapInfo destInfo;
	status = AndroidBitmap_getInfo(env, bmDest, &destInfo);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		LOGE("AndroidBitmap_getInfo failed for dest bm: %s",bitmapStatusToString(status));
		return status;
	}
	if (destInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGE("Dest bitmap format is not 8888 - format: %s",bitmapFormatToString(destInfo.format));
	}
	uint32_t *destPtr;
	status = AndroidBitmap_lockPixels(env, bmDest, (void **) &destPtr);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		LOGE("AndroidBitmap_lockPixels failed for dest bm: %s",bitmapStatusToString(status));
		return status;
	}

	const MoebiusTrans map(-x,-y,1.0f,0.0f,1.0f,0.0f,-x,y);
	const BitmapSampler from(sourcePtr, sourceInfo.width, sourceInfo.height);
	MappedBitmap to(destPtr, destInfo.width, destInfo.height);
	to.pullbackSampledBitmap(map,from);

	status = AndroidBitmap_unlockPixels(env, bmSource);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		LOGE("AndroidBitmap_unlockPixels failed for source bm: %s",bitmapStatusToString(status));
		return status;
	}

	status = AndroidBitmap_unlockPixels(env, bmDest);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		LOGE("AndroidBitmap_unlockPixels failed for dest bm: %s",bitmapStatusToString(status));
		return status;
	}
	return status;
}

