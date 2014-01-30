#include <jni.h>
#include <android/bitmap.h>

#include "logstream.h"
#include "bitmapper.h"

#define  LOG_TAG    "conform"

static logstream<ANDROID_LOG_INFO> INFO(LOG_TAG);
static logstream<ANDROID_LOG_DEBUG> DEBUG(LOG_TAG);
static logstream<ANDROID_LOG_ERROR> ERROR(LOG_TAG);

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
	int status = 0;

	AndroidBitmapInfo sourceInfo;
	status = AndroidBitmap_getInfo(env, bmSource, &sourceInfo);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_getInfo failed for source bm: " << bitmapStatusToString(status) << endl;
		return status;
	}
	if (sourceInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		ERROR << "Source bitmap format is not 8888 - format: " << bitmapFormatToString(sourceInfo.format) << endl;
	}
	uint32_t *sourcePtr;
	status = AndroidBitmap_lockPixels(env, bmSource, (void **) &sourcePtr);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_lockPixels failed for source bm: " << bitmapStatusToString(status) << endl;
		return status;
	}
	AndroidBitmapInfo destInfo;
	status = AndroidBitmap_getInfo(env, bmDest, &destInfo);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_getInfo failed for dest bm: " << bitmapStatusToString(status) << endl;
		return status;
	}
	if (destInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		ERROR << "Dest bitmap format is not 8888 - format: " << bitmapFormatToString(destInfo.format) << endl;
	}
	uint32_t *destPtr;
	status = AndroidBitmap_lockPixels(env, bmDest, (void **) &destPtr);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_lockPixels failed for dest bm: " << bitmapStatusToString(status) << endl;
		return status;
	}

	const BitmapSampler from(sourcePtr, sourceInfo.width, sourceInfo.height);
	MappedBitmap to(destPtr, destInfo.width, destInfo.height);

	const MoebiusTrans scale(complex<fixpoint>(2,0), complex<fixpoint>(-1,-1), complex<fixpoint>(0,0), complex<fixpoint>(1,0));
	const complex<fixpoint> a(scale(complex<fixpoint>(x,y)));
	const MoebiusTrans blaschke(complex<fixpoint>(1,0),-a,-conj(a),complex<fixpoint>(1,0));
//	const MoebiusTrans translate(complex<fixpoint>(1,0),-a,complex<fixpoint>(0,0),complex<fixpoint>(1,0));

	const MoebiusTrans map(scale.inv()*blaschke*scale);
	to.pullbackSampledBitmap(map, from);

	status = AndroidBitmap_unlockPixels(env, bmSource);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_unlockPixels failed for source bm: " << bitmapStatusToString(status) << endl;
		return status;
	}

	status = AndroidBitmap_unlockPixels(env, bmDest);
	if (status != ANDROID_BITMAP_RESULT_SUCCESS) {
		ERROR << "AndroidBitmap_unlockPixels failed for dest bm: " << bitmapStatusToString(status) << endl;
		return status;
	}
	return status;
}

