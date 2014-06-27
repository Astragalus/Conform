#include <jni.h>
#include <android/bitmap.h>

#include "logstream.h"
#include "bitmapper.h"

#define  LOG_TAG    "Conform"

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
	JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmaps(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jint numParams, jfloat pivotX, jfloat pivotY,
			jfloat scaleFac, jint wrapMode, jfloat p1r, jfloat p1i, jfloat p2r, jfloat p2i, jfloat p3r, jfloat p3i, jfloat p4r, jfloat p4i, jfloat p5r, jfloat p5i, jfloat p6r, jfloat p6i);
}

JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmaps(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jint numParams, jfloat pivotX, jfloat pivotY,
			jfloat scaleFac, jint wrapMode, jfloat p1r, jfloat p1i, jfloat p2r, jfloat p2i, jfloat p3r, jfloat p3i, jfloat p4r, jfloat p4i, jfloat p5r, jfloat p5i, jfloat p6r, jfloat p6i) {
	int status = 0;
	const float params[12] = {p1r, p1i, p2r, p2i, p3r, p3i, p4r, p4i, p5r, p5i, p6r, p6i};
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

	const MobiusTrans affine(complex<fixpoint>(scaleFac),complex<fixpoint>(pivotX, pivotY),ZERO,ONE);

	BlaschkeMap blas;
	for (int i = 0; i < numParams; ++i) {
		const complex<fixpoint> param(complex<fixpoint>(params[2*i],params[2*i+1]));
		blas *= MobiusTrans::hyperbolicIsometry(param);
	}

	const BlaschkeMap map(blas|-affine);

	DEBUG << "[C++land] blas[" << blas << "] affine[" << affine << "]" << endl;

	MappedBitmap viewPlane(destPtr, destInfo.width, destInfo.height);
	const BitmapSampler sampler(sourcePtr, sourceInfo.width, sourceInfo.height, wrapMode);
	viewPlane.pullbackSampledBitmap(map, sampler);

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
