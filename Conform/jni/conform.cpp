/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
#include <signal.h>
#include <jni.h>

#include "logstream.h"
#include "bitmapwrapper.h"
#include "bitmapper.h"

using namespace std;

#define  LOG_TAG    "Conform"

logstream<ANDROID_LOG_INFO> INFO(LOG_TAG);
logstream<ANDROID_LOG_DEBUG> DEBUG(LOG_TAG);
logstream<ANDROID_LOG_ERROR> ERROR(LOG_TAG);

extern "C" {
	JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmap(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jfloatArray paramArray, jint numParams, jfloat pivotX, jfloat pivotY, jfloat scaleFac, jint wrapMode);
	JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmapByExpression(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jstring expression, jfloatArray paramArray, jint numParams, jfloat pivotX, jfloat pivotY, jfloat scaleFac, jint wrapMode);
}

JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmap(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jfloatArray paramArray, jint numParams, jfloat pivotX, jfloat pivotY, jfloat scaleFac, jint wrapMode) {
	signal(SIGFPE, SIG_IGN); //ignore arithmetic errors - tried rooting them out but still get 'em.  Don't care anyway, so...

	BitmapWrapper source(env, bmSource);
	BitmapWrapper dest(env, bmDest);

	jboolean isCopy;
	jfloat* params = env->GetFloatArrayElements(paramArray, &isCopy);

	const MobiusTrans affine(complex<fixpoint>(fixpoint(scaleFac)),complex<fixpoint>(fixpoint(pivotX), fixpoint(pivotY)),ZERO,ONE);
	BlaschkeMap blas;
	for (int i = 0; i < numParams; ++i) {
		blas *= MobiusTrans::hyperbolicIsometry(complex<fixpoint>(fixpoint(params[2*i]),fixpoint(params[2*i+1])));
	}
	const BlaschkeMap map(blas|-affine);
	const BitmapSampler sampler(source.getData(), source.getWidth(), source.getHeight(), wrapMode);
	MappedBitmap viewPlane(sampler, dest.getData(), dest.getWidth(), dest.getHeight(), map);
	viewPlane.pullbackSampledBitmap();

	env->ReleaseFloatArrayElements(paramArray, params, 0);
	uint32_t t;
	return 0;
}

JNIEXPORT jint JNICALL Java_org_mtc_conform_ConformLib_pullbackBitmapByExpression(JNIEnv *env, jobject thiz, jobject bmSource, jobject bmDest, jstring expression, jfloatArray paramArray, jint numParams, jfloat pivotX, jfloat pivotY, jfloat scaleFac, jint wrapMode) {
	return 0;
}
