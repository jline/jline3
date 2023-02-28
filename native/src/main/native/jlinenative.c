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


