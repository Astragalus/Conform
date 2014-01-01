#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <bitmapper.h>

#define  LOG_TAG    "conform"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

using namespace bitmapper;

extern "C" {
	JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmaps(JNIEnv *env, jobject thiz, jobject bmfrom, jobject bmTo);
}

JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmaps(JNIEnv *env, jobject thiz, jobject bmfrom, jobject bmTo) {
	AndroidBitmapInfo fromInfo;
	if (!AndroidBitmap_getInfo(env, bmfrom, &fromInfo))
		return -1;
	uint32_t *fromPtr;
	if (!AndroidBitmap_lockPixels(env, bmfrom, (void **) &fromPtr))
		return -1;
	AndroidBitmapInfo toInfo;
	if (!AndroidBitmap_getInfo(env, bmfrom, &toInfo))
		return -1;
	uint32_t *toPtr;
	if (!AndroidBitmap_lockPixels(env, bmfrom, (void **) &toPtr))
		return -1;
	MappedBitmap to(toPtr, toInfo.width, toInfo.height);
	const BitmapSampler from(fromPtr, fromInfo.width, fromInfo.height);
	const MoebiusTrans map(0.0f,1.0f,1.0f,0.0f);
	to.pullbackSampledBitmap(map,from);
	return 0;
}
