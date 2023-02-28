/*******************************************************************************
 * Copyright (C) 2023 the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *******************************************************************************/
#include "jlinenative.h"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
  return JNI_VERSION_1_2;
}

#define JLineLibrary_NATIVE(func) Java_org_jline_nativ_JLineLibrary_##func

JNIEXPORT jobject JNICALL JLineLibrary_NATIVE(newFileDescriptor)(JNIEnv *env, jclass that, int fd)
{
    jfieldID field_fd;
    jmethodID const_fdesc;
    jclass class_fdesc;
    jobject ret;

    class_fdesc = (*env)->FindClass(env, "java/io/FileDescriptor");
    if (class_fdesc == NULL) return NULL;

    // construct a new FileDescriptor
    const_fdesc = (*env)->GetMethodID(env, class_fdesc, "<init>", "()V");
    if (const_fdesc == NULL) return NULL;
    ret = (*env)->NewObject(env, class_fdesc, const_fdesc);

    // poke the "fd" field with the file descriptor
    field_fd = (*env)->GetFieldID(env, class_fdesc, "fd", "I");
    if (field_fd == NULL) return NULL;
    (*env)->SetIntField(env, ret, field_fd, fd);

    // and return it
    return ret;
}


