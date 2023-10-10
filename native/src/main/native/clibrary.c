/*******************************************************************************
 * Copyright (C) 2009-2017 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
#include "jlinenative.h"

#if !defined(_WIN32) && !defined(_WIN64)

#define Termios_NATIVE(func) Java_org_jline_nativ_CLibrary_00024Termios_##func
#define WinSize_NATIVE(func) Java_org_jline_nativ_CLibrary_00024WinSize_##func
#define CLibrary_NATIVE(func) Java_org_jline_nativ_CLibrary_##func

typedef struct Termios_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID c_iflag, c_oflag, c_cflag, c_lflag, c_cc, c_ispeed, c_ospeed;
} Termios_FID_CACHE;

Termios_FID_CACHE TermiosFc;

void cacheTermiosFields(JNIEnv *env, jobject lpObject)
{
	if (TermiosFc.cached) return;
	TermiosFc.clazz = (*env)->GetObjectClass(env, lpObject);
	TermiosFc.c_iflag = (*env)->GetFieldID(env, TermiosFc.clazz, "c_iflag", "J");
	TermiosFc.c_oflag = (*env)->GetFieldID(env, TermiosFc.clazz, "c_oflag", "J");
	TermiosFc.c_cflag = (*env)->GetFieldID(env, TermiosFc.clazz, "c_cflag", "J");
	TermiosFc.c_lflag = (*env)->GetFieldID(env, TermiosFc.clazz, "c_lflag", "J");
	TermiosFc.c_cc = (*env)->GetFieldID(env, TermiosFc.clazz, "c_cc", "[B");
	TermiosFc.c_ispeed = (*env)->GetFieldID(env, TermiosFc.clazz, "c_ispeed", "J");
	TermiosFc.c_ospeed = (*env)->GetFieldID(env, TermiosFc.clazz, "c_ospeed", "J");
	hawtjni_w_barrier();
	TermiosFc.cached = 1;
}

struct termios *getTermiosFields(JNIEnv *env, jobject lpObject, struct termios *lpStruct)
{
	if (!TermiosFc.cached) cacheTermiosFields(env, lpObject);
	lpStruct->c_iflag = (*env)->GetLongField(env, lpObject, TermiosFc.c_iflag);
	lpStruct->c_oflag = (*env)->GetLongField(env, lpObject, TermiosFc.c_oflag);
	lpStruct->c_cflag = (*env)->GetLongField(env, lpObject, TermiosFc.c_cflag);
	lpStruct->c_lflag = (*env)->GetLongField(env, lpObject, TermiosFc.c_lflag);
	{
        jbyteArray lpObject1 = (jbyteArray)(*env)->GetObjectField(env, lpObject, TermiosFc.c_cc);
        (*env)->GetByteArrayRegion(env, lpObject1, 0, sizeof(lpStruct->c_cc), (jbyte *)lpStruct->c_cc);
	}
	lpStruct->c_ispeed = (*env)->GetLongField(env, lpObject, TermiosFc.c_ispeed);
	lpStruct->c_ospeed = (*env)->GetLongField(env, lpObject, TermiosFc.c_ospeed);
	return lpStruct;
}

void setTermiosFields(JNIEnv *env, jobject lpObject, struct termios *lpStruct)
{
	if (!TermiosFc.cached) cacheTermiosFields(env, lpObject);
	(*env)->SetLongField(env, lpObject, TermiosFc.c_iflag, (jlong)lpStruct->c_iflag);
	(*env)->SetLongField(env, lpObject, TermiosFc.c_oflag, (jlong)lpStruct->c_oflag);
	(*env)->SetLongField(env, lpObject, TermiosFc.c_cflag, (jlong)lpStruct->c_cflag);
	(*env)->SetLongField(env, lpObject, TermiosFc.c_lflag, (jlong)lpStruct->c_lflag);
	{
        jbyteArray lpObject1 = (jbyteArray)(*env)->GetObjectField(env, lpObject, TermiosFc.c_cc);
        (*env)->SetByteArrayRegion(env, lpObject1, 0, sizeof(lpStruct->c_cc), (jbyte *)lpStruct->c_cc);
	}
	(*env)->SetLongField(env, lpObject, TermiosFc.c_ispeed, (jlong)lpStruct->c_ispeed);
	(*env)->SetLongField(env, lpObject, TermiosFc.c_ospeed, (jlong)lpStruct->c_ospeed);
}

typedef struct WinSize_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID ws_row, ws_col, ws_xpixel, ws_ypixel;
} WinSize_FID_CACHE;

WinSize_FID_CACHE WinSizeFc;

void cacheWinSizeFields(JNIEnv *env, jobject lpObject)
{
	if (WinSizeFc.cached) return;
	WinSizeFc.clazz = (*env)->GetObjectClass(env, lpObject);
	WinSizeFc.ws_row = (*env)->GetFieldID(env, WinSizeFc.clazz, "ws_row", "S");
	WinSizeFc.ws_col = (*env)->GetFieldID(env, WinSizeFc.clazz, "ws_col", "S");
	WinSizeFc.ws_xpixel = (*env)->GetFieldID(env, WinSizeFc.clazz, "ws_xpixel", "S");
	WinSizeFc.ws_ypixel = (*env)->GetFieldID(env, WinSizeFc.clazz, "ws_ypixel", "S");
	hawtjni_w_barrier();
	WinSizeFc.cached = 1;
}

struct winsize *getWinSizeFields(JNIEnv *env, jobject lpObject, struct winsize *lpStruct)
{
	if (!WinSizeFc.cached) cacheWinSizeFields(env, lpObject);
	lpStruct->ws_row = (*env)->GetShortField(env, lpObject, WinSizeFc.ws_row);
	lpStruct->ws_col = (*env)->GetShortField(env, lpObject, WinSizeFc.ws_col);
	lpStruct->ws_xpixel = (*env)->GetShortField(env, lpObject, WinSizeFc.ws_xpixel);
	lpStruct->ws_ypixel = (*env)->GetShortField(env, lpObject, WinSizeFc.ws_ypixel);
	return lpStruct;
}

void setWinSizeFields(JNIEnv *env, jobject lpObject, struct winsize *lpStruct)
{
	if (!WinSizeFc.cached) cacheWinSizeFields(env, lpObject);
	(*env)->SetShortField(env, lpObject, WinSizeFc.ws_row, (jshort)lpStruct->ws_row);
	(*env)->SetShortField(env, lpObject, WinSizeFc.ws_col, (jshort)lpStruct->ws_col);
	(*env)->SetShortField(env, lpObject, WinSizeFc.ws_xpixel, (jshort)lpStruct->ws_xpixel);
	(*env)->SetShortField(env, lpObject, WinSizeFc.ws_ypixel, (jshort)lpStruct->ws_ypixel);
}

JNIEXPORT void JNICALL Termios_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(struct termios));
   return;
}

JNIEXPORT void JNICALL WinSize_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(struct winsize));
    return;
}

JNIEXPORT void JNICALL CLibrary_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "TCSANOW", "I"), (jint)TCSANOW);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "TCSADRAIN", "I"), (jint)TCSADRAIN);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "TCSAFLUSH", "I"), (jint)TCSAFLUSH);
	(*env)->SetStaticLongField(env, that, (*env)->GetStaticFieldID(env, that, "TIOCGWINSZ", "J"), (jlong)TIOCGWINSZ);
	(*env)->SetStaticLongField(env, that, (*env)->GetStaticFieldID(env, that, "TIOCSWINSZ", "J"), (jlong)TIOCSWINSZ);
   return;
}

JNIEXPORT jint JNICALL CLibrary_NATIVE(ioctl__IJLorg_jline_nativ_CLibrary_00024WinSize_2)
	(JNIEnv *env, jclass that, jint arg0, jlong arg1, jobject arg2)
{
	struct winsize _arg2, *lparg2=NULL;
	jint rc = 0;

	if (arg2) if ((lparg2 = getWinSizeFields(env, arg2, &_arg2)) == NULL) goto fail;
	rc = (jint)ioctl(arg0, arg1, (intptr_t)lparg2);
fail:
	if (arg2 && lparg2) setWinSizeFields(env, arg2, lparg2);

	return rc;
}

JNIEXPORT jint JNICALL CLibrary_NATIVE(ioctl__IJ_3I)
	(JNIEnv *env, jclass that, jint arg0, jlong arg1, jintArray arg2)
{
	jint *lparg2=NULL;
	jint rc = 0;

	if (arg2) if ((lparg2 = (*env)->GetIntArrayElements(env, arg2, NULL)) == NULL) goto fail;
	rc = (jint)ioctl(arg0, arg1, lparg2);
fail:
	if (arg2 && lparg2) (*env)->ReleaseIntArrayElements(env, arg2, lparg2, 0);

	return rc;
}

JNIEXPORT jint JNICALL CLibrary_NATIVE(openpty)
	(JNIEnv *env, jclass that, jintArray arg0, jintArray arg1, jbyteArray arg2, jobject arg3, jobject arg4)
{
	jint *lparg0=NULL;
	jint *lparg1=NULL;
	jbyte *lparg2=NULL;
	struct termios _arg3, *lparg3=NULL;
	struct winsize _arg4, *lparg4=NULL;
	jint rc = 0;

	if (arg0) if ((lparg0 = (*env)->GetIntArrayElements(env, arg0, NULL)) == NULL) goto fail;
	if (arg1) if ((lparg1 = (*env)->GetIntArrayElements(env, arg1, NULL)) == NULL) goto fail;
	if (arg2) if ((lparg2 = (*env)->GetByteArrayElements(env, arg2, NULL)) == NULL) goto fail;
	if (arg3) if ((lparg3 = getTermiosFields(env, arg3, &_arg3)) == NULL) goto fail;
	if (arg4) if ((lparg4 = getWinSizeFields(env, arg4, &_arg4)) == NULL) goto fail;
	rc = (jint)openpty((int *)lparg0, (int *)lparg1, (char *)lparg2, (struct termios *)lparg3, (struct winsize *)lparg4);
fail:
	if (arg2 && lparg2) (*env)->ReleaseByteArrayElements(env, arg2, lparg2, 0);
	if (arg1 && lparg1) (*env)->ReleaseIntArrayElements(env, arg1, lparg1, 0);
	if (arg0 && lparg0) (*env)->ReleaseIntArrayElements(env, arg0, lparg0, 0);

	return rc;
}

JNIEXPORT jint JNICALL CLibrary_NATIVE(tcgetattr)
	(JNIEnv *env, jclass that, jint arg0, jobject arg1)
{
	struct termios _arg1, *lparg1=NULL;
	jint rc = 0;

	if (arg1) if ((lparg1 = &_arg1) == NULL) goto fail;
	rc = (jint)tcgetattr(arg0, (struct termios *)lparg1);
fail:
	if (arg1 && lparg1) setTermiosFields(env, arg1, lparg1);

	return rc;
}

JNIEXPORT jint JNICALL CLibrary_NATIVE(tcsetattr)
	(JNIEnv *env, jclass that, jint arg0, jint arg1, jobject arg2)
{
	struct termios _arg2, *lparg2=NULL;
	jint rc = 0;

	if (arg2) if ((lparg2 = getTermiosFields(env, arg2, &_arg2)) == NULL) goto fail;
	rc = (jint)tcsetattr(arg0, arg1, (struct termios *)lparg2);
fail:

	return rc;
}

JNIEXPORT jint JNICALL CLibrary_NATIVE(isatty)
	(JNIEnv *env, jclass that, jint arg0)
{
	jint rc = 0;

	rc = (jint)isatty(arg0);

	return rc;
}

JNIEXPORT jstring JNICALL CLibrary_NATIVE(ttyname)
	(JNIEnv *env, jclass that, jint arg0)
{
	jstring rc = 0;
	char s[256] = { 0 };
	int r = 0;

	r = ttyname_r(arg0, s, 256);
	if (!r) rc = (*env)->NewStringUTF(env,s);

	return rc;
}

#endif
