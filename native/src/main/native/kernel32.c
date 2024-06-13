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

#if defined(_WIN32) || defined(_WIN64)

#define isatty _isatty

#define Kernel32_NATIVE(func) Java_org_jline_nativ_Kernel32_##func
#define CHAR_INFO_NATIVE(func) Java_org_jline_nativ_Kernel32_00024CHAR_1INFO_##func
#define CONSOLE_SCREEN_BUFFER_INFO_NATIVE(func) Java_org_jline_nativ_Kernel32_00024CONSOLE_1SCREEN_1BUFFER_1INFO_##func
#define COORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024COORD_##func
#define FOCUS_EVENT_RECORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024FOCUS_1EVENT_1RECORD_##func
#define INPUT_RECORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024INPUT_1RECORD_##func
#define KEY_EVENT_RECORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024KEY_1EVENT_1RECORD_##func
#define MENU_EVENT_RECORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024MENU_1EVENT_1RECORD_##func
#define MOUSE_EVENT_RECORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024MOUSE_1EVENT_1RECORD_##func
#define SMALL_RECT_NATIVE(func) Java_org_jline_nativ_Kernel32_00024SMALL_1RECT_##func
#define WINDOW_BUFFER_SIZE_RECORD_NATIVE(func) Java_org_jline_nativ_Kernel32_00024WINDOW_1BUFFER_1SIZE_1RECORD_##func

void cacheCHAR_INFOFields(JNIEnv *env, jobject lpObject);
CHAR_INFO *getCHAR_INFOFields(JNIEnv *env, jobject lpObject, CHAR_INFO *lpStruct);
void setCHAR_INFOFields(JNIEnv *env, jobject lpObject, CHAR_INFO *lpStruct);

void cacheCONSOLE_SCREEN_BUFFER_INFOFields(JNIEnv *env, jobject lpObject);
CONSOLE_SCREEN_BUFFER_INFO *getCONSOLE_SCREEN_BUFFER_INFOFields(JNIEnv *env, jobject lpObject, CONSOLE_SCREEN_BUFFER_INFO *lpStruct);
void setCONSOLE_SCREEN_BUFFER_INFOFields(JNIEnv *env, jobject lpObject, CONSOLE_SCREEN_BUFFER_INFO *lpStruct);

void cacheCOORDFields(JNIEnv *env, jobject lpObject);
COORD *getCOORDFields(JNIEnv *env, jobject lpObject, COORD *lpStruct);
void setCOORDFields(JNIEnv *env, jobject lpObject, COORD *lpStruct);

void cacheFOCUS_EVENT_RECORDFields(JNIEnv *env, jobject lpObject);
FOCUS_EVENT_RECORD *getFOCUS_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, FOCUS_EVENT_RECORD *lpStruct);
void setFOCUS_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, FOCUS_EVENT_RECORD *lpStruct);

void cacheINPUT_RECORDFields(JNIEnv *env, jobject lpObject);
INPUT_RECORD *getINPUT_RECORDFields(JNIEnv *env, jobject lpObject, INPUT_RECORD *lpStruct);
void setINPUT_RECORDFields(JNIEnv *env, jobject lpObject, INPUT_RECORD *lpStruct);

void cacheKEY_EVENT_RECORDFields(JNIEnv *env, jobject lpObject);
KEY_EVENT_RECORD *getKEY_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, KEY_EVENT_RECORD *lpStruct);
void setKEY_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, KEY_EVENT_RECORD *lpStruct);

void cacheMENU_EVENT_RECORDFields(JNIEnv *env, jobject lpObject);
MENU_EVENT_RECORD *getMENU_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MENU_EVENT_RECORD *lpStruct);
void setMENU_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MENU_EVENT_RECORD *lpStruct);

void cacheMOUSE_EVENT_RECORDFields(JNIEnv *env, jobject lpObject);
MOUSE_EVENT_RECORD *getMOUSE_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MOUSE_EVENT_RECORD *lpStruct);
void setMOUSE_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MOUSE_EVENT_RECORD *lpStruct);

void cacheSMALL_RECTFields(JNIEnv *env, jobject lpObject);
SMALL_RECT *getSMALL_RECTFields(JNIEnv *env, jobject lpObject, SMALL_RECT *lpStruct);
void setSMALL_RECTFields(JNIEnv *env, jobject lpObject, SMALL_RECT *lpStruct);

void cacheWINDOW_BUFFER_SIZE_RECORDFields(JNIEnv *env, jobject lpObject);
WINDOW_BUFFER_SIZE_RECORD *getWINDOW_BUFFER_SIZE_RECORDFields(JNIEnv *env, jobject lpObject, WINDOW_BUFFER_SIZE_RECORD *lpStruct);
void setWINDOW_BUFFER_SIZE_RECORDFields(JNIEnv *env, jobject lpObject, WINDOW_BUFFER_SIZE_RECORD *lpStruct);

typedef struct CHAR_INFO_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID attributes, unicodeChar;
} CHAR_INFO_FID_CACHE;

CHAR_INFO_FID_CACHE CHAR_INFOFc;

void cacheCHAR_INFOFields(JNIEnv *env, jobject lpObject)
{
	if (CHAR_INFOFc.cached) return;
	CHAR_INFOFc.clazz = (*env)->GetObjectClass(env, lpObject);
	CHAR_INFOFc.attributes = (*env)->GetFieldID(env, CHAR_INFOFc.clazz, "attributes", "S");
	CHAR_INFOFc.unicodeChar = (*env)->GetFieldID(env, CHAR_INFOFc.clazz, "unicodeChar", "C");
	hawtjni_w_barrier();
	CHAR_INFOFc.cached = 1;
}

CHAR_INFO *getCHAR_INFOFields(JNIEnv *env, jobject lpObject, CHAR_INFO *lpStruct)
{
	if (!CHAR_INFOFc.cached) cacheCHAR_INFOFields(env, lpObject);
	lpStruct->Attributes = (*env)->GetShortField(env, lpObject, CHAR_INFOFc.attributes);
	lpStruct->Char.UnicodeChar = (*env)->GetCharField(env, lpObject, CHAR_INFOFc.unicodeChar);
	return lpStruct;
}

void setCHAR_INFOFields(JNIEnv *env, jobject lpObject, CHAR_INFO *lpStruct)
{
	if (!CHAR_INFOFc.cached) cacheCHAR_INFOFields(env, lpObject);
	(*env)->SetShortField(env, lpObject, CHAR_INFOFc.attributes, (jshort)lpStruct->Attributes);
	(*env)->SetCharField(env, lpObject, CHAR_INFOFc.unicodeChar, (jchar)lpStruct->Char.UnicodeChar);
}

typedef struct CONSOLE_SCREEN_BUFFER_INFO_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID size, cursorPosition, attributes, window, maximumWindowSize;
} CONSOLE_SCREEN_BUFFER_INFO_FID_CACHE;

CONSOLE_SCREEN_BUFFER_INFO_FID_CACHE CONSOLE_SCREEN_BUFFER_INFOFc;

void cacheCONSOLE_SCREEN_BUFFER_INFOFields(JNIEnv *env, jobject lpObject)
{
	if (CONSOLE_SCREEN_BUFFER_INFOFc.cached) return;
	CONSOLE_SCREEN_BUFFER_INFOFc.clazz = (*env)->GetObjectClass(env, lpObject);
	CONSOLE_SCREEN_BUFFER_INFOFc.size = (*env)->GetFieldID(env, CONSOLE_SCREEN_BUFFER_INFOFc.clazz, "size", "Lorg/jline/nativ/Kernel32$COORD;");
	CONSOLE_SCREEN_BUFFER_INFOFc.cursorPosition = (*env)->GetFieldID(env, CONSOLE_SCREEN_BUFFER_INFOFc.clazz, "cursorPosition", "Lorg/jline/nativ/Kernel32$COORD;");
	CONSOLE_SCREEN_BUFFER_INFOFc.attributes = (*env)->GetFieldID(env, CONSOLE_SCREEN_BUFFER_INFOFc.clazz, "attributes", "S");
	CONSOLE_SCREEN_BUFFER_INFOFc.window = (*env)->GetFieldID(env, CONSOLE_SCREEN_BUFFER_INFOFc.clazz, "window", "Lorg/jline/nativ/Kernel32$SMALL_RECT;");
	CONSOLE_SCREEN_BUFFER_INFOFc.maximumWindowSize = (*env)->GetFieldID(env, CONSOLE_SCREEN_BUFFER_INFOFc.clazz, "maximumWindowSize", "Lorg/jline/nativ/Kernel32$COORD;");
	hawtjni_w_barrier();
	CONSOLE_SCREEN_BUFFER_INFOFc.cached = 1;
}

CONSOLE_SCREEN_BUFFER_INFO *getCONSOLE_SCREEN_BUFFER_INFOFields(JNIEnv *env, jobject lpObject, CONSOLE_SCREEN_BUFFER_INFO *lpStruct)
{
	if (!CONSOLE_SCREEN_BUFFER_INFOFc.cached) cacheCONSOLE_SCREEN_BUFFER_INFOFields(env, lpObject);
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.size);
        if (lpObject1 != NULL) getCOORDFields(env, lpObject1, &lpStruct->dwSize);
	}
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.cursorPosition);
        if (lpObject1 != NULL) getCOORDFields(env, lpObject1, &lpStruct->dwCursorPosition);
	}
	lpStruct->wAttributes = (*env)->GetShortField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.attributes);
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.window);
        if (lpObject1 != NULL) getSMALL_RECTFields(env, lpObject1, &lpStruct->srWindow);
	}
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.maximumWindowSize);
        if (lpObject1 != NULL) getCOORDFields(env, lpObject1, &lpStruct->dwMaximumWindowSize);
	}
	return lpStruct;
}

void setCONSOLE_SCREEN_BUFFER_INFOFields(JNIEnv *env, jobject lpObject, CONSOLE_SCREEN_BUFFER_INFO *lpStruct)
{
	if (!CONSOLE_SCREEN_BUFFER_INFOFc.cached) cacheCONSOLE_SCREEN_BUFFER_INFOFields(env, lpObject);
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.size);
        if (lpObject1 != NULL) setCOORDFields(env, lpObject1, &lpStruct->dwSize);
	}
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.cursorPosition);
        if (lpObject1 != NULL) setCOORDFields(env, lpObject1, &lpStruct->dwCursorPosition);
	}
	(*env)->SetShortField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.attributes, (jshort)lpStruct->wAttributes);
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.window);
        if (lpObject1 != NULL) setSMALL_RECTFields(env, lpObject1, &lpStruct->srWindow);
	}
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, CONSOLE_SCREEN_BUFFER_INFOFc.maximumWindowSize);
        if (lpObject1 != NULL) setCOORDFields(env, lpObject1, &lpStruct->dwMaximumWindowSize);
	}
}

typedef struct COORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID x, y;
} COORD_FID_CACHE;

COORD_FID_CACHE COORDFc;

void cacheCOORDFields(JNIEnv *env, jobject lpObject)
{
	if (COORDFc.cached) return;
	COORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	COORDFc.x = (*env)->GetFieldID(env, COORDFc.clazz, "x", "S");
	COORDFc.y = (*env)->GetFieldID(env, COORDFc.clazz, "y", "S");
	hawtjni_w_barrier();
	COORDFc.cached = 1;
}

COORD *getCOORDFields(JNIEnv *env, jobject lpObject, COORD *lpStruct)
{
	if (!COORDFc.cached) cacheCOORDFields(env, lpObject);
	lpStruct->X = (*env)->GetShortField(env, lpObject, COORDFc.x);
	lpStruct->Y = (*env)->GetShortField(env, lpObject, COORDFc.y);
	return lpStruct;
}

void setCOORDFields(JNIEnv *env, jobject lpObject, COORD *lpStruct)
{
	if (!COORDFc.cached) cacheCOORDFields(env, lpObject);
	(*env)->SetShortField(env, lpObject, COORDFc.x, (jshort)lpStruct->X);
	(*env)->SetShortField(env, lpObject, COORDFc.y, (jshort)lpStruct->Y);
}

typedef struct FOCUS_EVENT_RECORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID setFocus;
} FOCUS_EVENT_RECORD_FID_CACHE;

FOCUS_EVENT_RECORD_FID_CACHE FOCUS_EVENT_RECORDFc;

void cacheFOCUS_EVENT_RECORDFields(JNIEnv *env, jobject lpObject)
{
	if (FOCUS_EVENT_RECORDFc.cached) return;
	FOCUS_EVENT_RECORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	FOCUS_EVENT_RECORDFc.setFocus = (*env)->GetFieldID(env, FOCUS_EVENT_RECORDFc.clazz, "setFocus", "Z");
	hawtjni_w_barrier();
	FOCUS_EVENT_RECORDFc.cached = 1;
}

FOCUS_EVENT_RECORD *getFOCUS_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, FOCUS_EVENT_RECORD *lpStruct)
{
	if (!FOCUS_EVENT_RECORDFc.cached) cacheFOCUS_EVENT_RECORDFields(env, lpObject);
	lpStruct->bSetFocus = (*env)->GetBooleanField(env, lpObject, FOCUS_EVENT_RECORDFc.setFocus);
	return lpStruct;
}

void setFOCUS_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, FOCUS_EVENT_RECORD *lpStruct)
{
	if (!FOCUS_EVENT_RECORDFc.cached) cacheFOCUS_EVENT_RECORDFields(env, lpObject);
	(*env)->SetBooleanField(env, lpObject, FOCUS_EVENT_RECORDFc.setFocus, (jboolean)lpStruct->bSetFocus);
}

typedef struct INPUT_RECORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID eventType, keyEvent, mouseEvent, windowBufferSizeEvent, menuEvent, focusEvent;
} INPUT_RECORD_FID_CACHE;

INPUT_RECORD_FID_CACHE INPUT_RECORDFc;

void cacheINPUT_RECORDFields(JNIEnv *env, jobject lpObject)
{
	if (INPUT_RECORDFc.cached) return;
	INPUT_RECORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	INPUT_RECORDFc.eventType = (*env)->GetFieldID(env, INPUT_RECORDFc.clazz, "eventType", "S");
	INPUT_RECORDFc.keyEvent = (*env)->GetFieldID(env, INPUT_RECORDFc.clazz, "keyEvent", "Lorg/jline/nativ/Kernel32$KEY_EVENT_RECORD;");
	INPUT_RECORDFc.mouseEvent = (*env)->GetFieldID(env, INPUT_RECORDFc.clazz, "mouseEvent", "Lorg/jline/nativ/Kernel32$MOUSE_EVENT_RECORD;");
	INPUT_RECORDFc.windowBufferSizeEvent = (*env)->GetFieldID(env, INPUT_RECORDFc.clazz, "windowBufferSizeEvent", "Lorg/jline/nativ/Kernel32$WINDOW_BUFFER_SIZE_RECORD;");
	INPUT_RECORDFc.menuEvent = (*env)->GetFieldID(env, INPUT_RECORDFc.clazz, "menuEvent", "Lorg/jline/nativ/Kernel32$MENU_EVENT_RECORD;");
	INPUT_RECORDFc.focusEvent = (*env)->GetFieldID(env, INPUT_RECORDFc.clazz, "focusEvent", "Lorg/jline/nativ/Kernel32$FOCUS_EVENT_RECORD;");
	hawtjni_w_barrier();
	INPUT_RECORDFc.cached = 1;
}

INPUT_RECORD *getINPUT_RECORDFields(JNIEnv *env, jobject lpObject, INPUT_RECORD *lpStruct)
{
	if (!INPUT_RECORDFc.cached) cacheINPUT_RECORDFields(env, lpObject);
	lpStruct->EventType = (*env)->GetShortField(env, lpObject, INPUT_RECORDFc.eventType);
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.keyEvent);
        if (lpObject1 != NULL) getKEY_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.KeyEvent);
	}
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.mouseEvent);
        if (lpObject1 != NULL) getMOUSE_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.MouseEvent);
	}
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.windowBufferSizeEvent);
        if (lpObject1 != NULL) getWINDOW_BUFFER_SIZE_RECORDFields(env, lpObject1, &lpStruct->Event.WindowBufferSizeEvent);
	}
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.menuEvent);
        if (lpObject1 != NULL) getMENU_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.MenuEvent);
	}
    {
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.focusEvent);
        if (lpObject1 != NULL) getFOCUS_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.FocusEvent);
	}
	return lpStruct;
}

void setINPUT_RECORDFields(JNIEnv *env, jobject lpObject, INPUT_RECORD *lpStruct)
{
	if (!INPUT_RECORDFc.cached) cacheINPUT_RECORDFields(env, lpObject);
	(*env)->SetShortField(env, lpObject, INPUT_RECORDFc.eventType, (jshort)lpStruct->EventType);
	{
    	jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.keyEvent);
	    if (lpObject1 != NULL) setKEY_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.KeyEvent);
	}
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.mouseEvent);
        if (lpObject1 != NULL) setMOUSE_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.MouseEvent);
	}
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.windowBufferSizeEvent);
        if (lpObject1 != NULL) setWINDOW_BUFFER_SIZE_RECORDFields(env, lpObject1, &lpStruct->Event.WindowBufferSizeEvent);
	}
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.menuEvent);
        if (lpObject1 != NULL) setMENU_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.MenuEvent);
	}
	{
        jobject lpObject1 = (*env)->GetObjectField(env, lpObject, INPUT_RECORDFc.focusEvent);
        if (lpObject1 != NULL) setFOCUS_EVENT_RECORDFields(env, lpObject1, &lpStruct->Event.FocusEvent);
	}
}

typedef struct KEY_EVENT_RECORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID keyDown, repeatCount, keyCode, scanCode, uchar, controlKeyState;
} KEY_EVENT_RECORD_FID_CACHE;

KEY_EVENT_RECORD_FID_CACHE KEY_EVENT_RECORDFc;

void cacheKEY_EVENT_RECORDFields(JNIEnv *env, jobject lpObject)
{
	if (KEY_EVENT_RECORDFc.cached) return;
	KEY_EVENT_RECORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	KEY_EVENT_RECORDFc.keyDown = (*env)->GetFieldID(env, KEY_EVENT_RECORDFc.clazz, "keyDown", "Z");
	KEY_EVENT_RECORDFc.repeatCount = (*env)->GetFieldID(env, KEY_EVENT_RECORDFc.clazz, "repeatCount", "S");
	KEY_EVENT_RECORDFc.keyCode = (*env)->GetFieldID(env, KEY_EVENT_RECORDFc.clazz, "keyCode", "S");
	KEY_EVENT_RECORDFc.scanCode = (*env)->GetFieldID(env, KEY_EVENT_RECORDFc.clazz, "scanCode", "S");
	KEY_EVENT_RECORDFc.uchar = (*env)->GetFieldID(env, KEY_EVENT_RECORDFc.clazz, "uchar", "C");
	KEY_EVENT_RECORDFc.controlKeyState = (*env)->GetFieldID(env, KEY_EVENT_RECORDFc.clazz, "controlKeyState", "I");
	hawtjni_w_barrier();
	KEY_EVENT_RECORDFc.cached = 1;
}

KEY_EVENT_RECORD *getKEY_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, KEY_EVENT_RECORD *lpStruct)
{
	if (!KEY_EVENT_RECORDFc.cached) cacheKEY_EVENT_RECORDFields(env, lpObject);
	lpStruct->bKeyDown = (*env)->GetBooleanField(env, lpObject, KEY_EVENT_RECORDFc.keyDown);
	lpStruct->wRepeatCount = (*env)->GetShortField(env, lpObject, KEY_EVENT_RECORDFc.repeatCount);
	lpStruct->wVirtualKeyCode = (*env)->GetShortField(env, lpObject, KEY_EVENT_RECORDFc.keyCode);
	lpStruct->wVirtualScanCode = (*env)->GetShortField(env, lpObject, KEY_EVENT_RECORDFc.scanCode);
	lpStruct->uChar.UnicodeChar = (*env)->GetCharField(env, lpObject, KEY_EVENT_RECORDFc.uchar);
	lpStruct->dwControlKeyState = (*env)->GetIntField(env, lpObject, KEY_EVENT_RECORDFc.controlKeyState);
	return lpStruct;
}

void setKEY_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, KEY_EVENT_RECORD *lpStruct)
{
	if (!KEY_EVENT_RECORDFc.cached) cacheKEY_EVENT_RECORDFields(env, lpObject);
	(*env)->SetBooleanField(env, lpObject, KEY_EVENT_RECORDFc.keyDown, (jboolean)lpStruct->bKeyDown);
	(*env)->SetShortField(env, lpObject, KEY_EVENT_RECORDFc.repeatCount, (jshort)lpStruct->wRepeatCount);
	(*env)->SetShortField(env, lpObject, KEY_EVENT_RECORDFc.keyCode, (jshort)lpStruct->wVirtualKeyCode);
	(*env)->SetShortField(env, lpObject, KEY_EVENT_RECORDFc.scanCode, (jshort)lpStruct->wVirtualScanCode);
	(*env)->SetCharField(env, lpObject, KEY_EVENT_RECORDFc.uchar, (jchar)lpStruct->uChar.UnicodeChar);
	(*env)->SetIntField(env, lpObject, KEY_EVENT_RECORDFc.controlKeyState, (jint)lpStruct->dwControlKeyState);
}

typedef struct MENU_EVENT_RECORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID commandId;
} MENU_EVENT_RECORD_FID_CACHE;

MENU_EVENT_RECORD_FID_CACHE MENU_EVENT_RECORDFc;

void cacheMENU_EVENT_RECORDFields(JNIEnv *env, jobject lpObject)
{
	if (MENU_EVENT_RECORDFc.cached) return;
	MENU_EVENT_RECORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	MENU_EVENT_RECORDFc.commandId = (*env)->GetFieldID(env, MENU_EVENT_RECORDFc.clazz, "commandId", "I");
	hawtjni_w_barrier();
	MENU_EVENT_RECORDFc.cached = 1;
}

MENU_EVENT_RECORD *getMENU_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MENU_EVENT_RECORD *lpStruct)
{
	if (!MENU_EVENT_RECORDFc.cached) cacheMENU_EVENT_RECORDFields(env, lpObject);
#if defined(_WIN32) || defined(_WIN64)
	lpStruct->dwCommandId = (*env)->GetIntField(env, lpObject, MENU_EVENT_RECORDFc.commandId);
#endif
	return lpStruct;
}

void setMENU_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MENU_EVENT_RECORD *lpStruct)
{
	if (!MENU_EVENT_RECORDFc.cached) cacheMENU_EVENT_RECORDFields(env, lpObject);
	(*env)->SetIntField(env, lpObject, MENU_EVENT_RECORDFc.commandId, (jint)lpStruct->dwCommandId);
}

typedef struct MOUSE_EVENT_RECORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID mousePosition, buttonState, controlKeyState, eventFlags;
} MOUSE_EVENT_RECORD_FID_CACHE;

MOUSE_EVENT_RECORD_FID_CACHE MOUSE_EVENT_RECORDFc;

void cacheMOUSE_EVENT_RECORDFields(JNIEnv *env, jobject lpObject)
{
	if (MOUSE_EVENT_RECORDFc.cached) return;
	MOUSE_EVENT_RECORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	MOUSE_EVENT_RECORDFc.mousePosition = (*env)->GetFieldID(env, MOUSE_EVENT_RECORDFc.clazz, "mousePosition", "Lorg/jline/nativ/Kernel32$COORD;");
	MOUSE_EVENT_RECORDFc.buttonState = (*env)->GetFieldID(env, MOUSE_EVENT_RECORDFc.clazz, "buttonState", "I");
	MOUSE_EVENT_RECORDFc.controlKeyState = (*env)->GetFieldID(env, MOUSE_EVENT_RECORDFc.clazz, "controlKeyState", "I");
	MOUSE_EVENT_RECORDFc.eventFlags = (*env)->GetFieldID(env, MOUSE_EVENT_RECORDFc.clazz, "eventFlags", "I");
	hawtjni_w_barrier();
	MOUSE_EVENT_RECORDFc.cached = 1;
}

MOUSE_EVENT_RECORD *getMOUSE_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MOUSE_EVENT_RECORD *lpStruct)
{
	if (!MOUSE_EVENT_RECORDFc.cached) cacheMOUSE_EVENT_RECORDFields(env, lpObject);
    {
	    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, MOUSE_EVENT_RECORDFc.mousePosition);
	    if (lpObject1 != NULL) getCOORDFields(env, lpObject1, &lpStruct->dwMousePosition);
	}
	lpStruct->dwButtonState = (*env)->GetIntField(env, lpObject, MOUSE_EVENT_RECORDFc.buttonState);
	lpStruct->dwControlKeyState = (*env)->GetIntField(env, lpObject, MOUSE_EVENT_RECORDFc.controlKeyState);
	lpStruct->dwEventFlags = (*env)->GetIntField(env, lpObject, MOUSE_EVENT_RECORDFc.eventFlags);
	return lpStruct;
}

void setMOUSE_EVENT_RECORDFields(JNIEnv *env, jobject lpObject, MOUSE_EVENT_RECORD *lpStruct)
{
	if (!MOUSE_EVENT_RECORDFc.cached) cacheMOUSE_EVENT_RECORDFields(env, lpObject);
	{
	    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, MOUSE_EVENT_RECORDFc.mousePosition);
	    if (lpObject1 != NULL) setCOORDFields(env, lpObject1, &lpStruct->dwMousePosition);
	}
	(*env)->SetIntField(env, lpObject, MOUSE_EVENT_RECORDFc.buttonState, (jint)lpStruct->dwButtonState);
	(*env)->SetIntField(env, lpObject, MOUSE_EVENT_RECORDFc.controlKeyState, (jint)lpStruct->dwControlKeyState);
	(*env)->SetIntField(env, lpObject, MOUSE_EVENT_RECORDFc.eventFlags, (jint)lpStruct->dwEventFlags);
}

typedef struct SMALL_RECT_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID left, top, right, bottom;
} SMALL_RECT_FID_CACHE;

SMALL_RECT_FID_CACHE SMALL_RECTFc;

void cacheSMALL_RECTFields(JNIEnv *env, jobject lpObject)
{
	if (SMALL_RECTFc.cached) return;
	SMALL_RECTFc.clazz = (*env)->GetObjectClass(env, lpObject);
	SMALL_RECTFc.left = (*env)->GetFieldID(env, SMALL_RECTFc.clazz, "left", "S");
	SMALL_RECTFc.top = (*env)->GetFieldID(env, SMALL_RECTFc.clazz, "top", "S");
	SMALL_RECTFc.right = (*env)->GetFieldID(env, SMALL_RECTFc.clazz, "right", "S");
	SMALL_RECTFc.bottom = (*env)->GetFieldID(env, SMALL_RECTFc.clazz, "bottom", "S");
	hawtjni_w_barrier();
	SMALL_RECTFc.cached = 1;
}

SMALL_RECT *getSMALL_RECTFields(JNIEnv *env, jobject lpObject, SMALL_RECT *lpStruct)
{
	if (!SMALL_RECTFc.cached) cacheSMALL_RECTFields(env, lpObject);
	lpStruct->Left = (*env)->GetShortField(env, lpObject, SMALL_RECTFc.left);
	lpStruct->Top = (*env)->GetShortField(env, lpObject, SMALL_RECTFc.top);
	lpStruct->Right = (*env)->GetShortField(env, lpObject, SMALL_RECTFc.right);
	lpStruct->Bottom = (*env)->GetShortField(env, lpObject, SMALL_RECTFc.bottom);
	return lpStruct;
}

void setSMALL_RECTFields(JNIEnv *env, jobject lpObject, SMALL_RECT *lpStruct)
{
	if (!SMALL_RECTFc.cached) cacheSMALL_RECTFields(env, lpObject);
	(*env)->SetShortField(env, lpObject, SMALL_RECTFc.left, (jshort)lpStruct->Left);
	(*env)->SetShortField(env, lpObject, SMALL_RECTFc.top, (jshort)lpStruct->Top);
	(*env)->SetShortField(env, lpObject, SMALL_RECTFc.right, (jshort)lpStruct->Right);
	(*env)->SetShortField(env, lpObject, SMALL_RECTFc.bottom, (jshort)lpStruct->Bottom);
}

typedef struct WINDOW_BUFFER_SIZE_RECORD_FID_CACHE {
	int cached;
	jclass clazz;
	jfieldID size;
} WINDOW_BUFFER_SIZE_RECORD_FID_CACHE;

WINDOW_BUFFER_SIZE_RECORD_FID_CACHE WINDOW_BUFFER_SIZE_RECORDFc;

void cacheWINDOW_BUFFER_SIZE_RECORDFields(JNIEnv *env, jobject lpObject)
{
	if (WINDOW_BUFFER_SIZE_RECORDFc.cached) return;
	WINDOW_BUFFER_SIZE_RECORDFc.clazz = (*env)->GetObjectClass(env, lpObject);
	WINDOW_BUFFER_SIZE_RECORDFc.size = (*env)->GetFieldID(env, WINDOW_BUFFER_SIZE_RECORDFc.clazz, "size", "Lorg/jline/nativ/Kernel32$COORD;");
	hawtjni_w_barrier();
	WINDOW_BUFFER_SIZE_RECORDFc.cached = 1;
}

WINDOW_BUFFER_SIZE_RECORD *getWINDOW_BUFFER_SIZE_RECORDFields(JNIEnv *env, jobject lpObject, WINDOW_BUFFER_SIZE_RECORD *lpStruct)
{
	if (!WINDOW_BUFFER_SIZE_RECORDFc.cached) cacheWINDOW_BUFFER_SIZE_RECORDFields(env, lpObject);
	{
	    jobject lpObject1 = (*env)->GetObjectField(env, lpObject, WINDOW_BUFFER_SIZE_RECORDFc.size);
	    if (lpObject1 != NULL) getCOORDFields(env, lpObject1, &lpStruct->dwSize);
	}
	return lpStruct;
}

void setWINDOW_BUFFER_SIZE_RECORDFields(JNIEnv *env, jobject lpObject, WINDOW_BUFFER_SIZE_RECORD *lpStruct)
{
	if (!WINDOW_BUFFER_SIZE_RECORDFc.cached) cacheWINDOW_BUFFER_SIZE_RECORDFields(env, lpObject);
	{
    	jobject lpObject1 = (*env)->GetObjectField(env, lpObject, WINDOW_BUFFER_SIZE_RECORDFc.size);
	    if (lpObject1 != NULL) setCOORDFields(env, lpObject1, &lpStruct->dwSize);
	}
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(CloseHandle)
	(JNIEnv *env, jclass that, jlong arg0)
{
	jint rc = 0;
	rc = (jint)CloseHandle((HANDLE)arg0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(FillConsoleOutputAttribute)
	(JNIEnv *env, jclass that, jlong arg0, jshort arg1, jint arg2, jobject arg3, jintArray arg4)
{
	COORD _arg3, *lparg3=NULL;
	jint *lparg4=NULL;
	jint rc = 0;
	if (arg3) if ((lparg3 = getCOORDFields(env, arg3, &_arg3)) == NULL) goto fail;
	if (arg4) if ((lparg4 = (*env)->GetIntArrayElements(env, arg4, NULL)) == NULL) goto fail;
	rc = (jint)FillConsoleOutputAttribute((HANDLE)(intptr_t)arg0, arg1, arg2, *lparg3, lparg4);
fail:
	if (arg4 && lparg4) (*env)->ReleaseIntArrayElements(env, arg4, lparg4, 0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(FillConsoleOutputCharacterW)
	(JNIEnv *env, jclass that, jlong arg0, jchar arg1, jint arg2, jobject arg3, jintArray arg4)
{
	COORD _arg3, *lparg3=NULL;
	jint *lparg4=NULL;
	jint rc = 0;
	if (arg3) if ((lparg3 = getCOORDFields(env, arg3, &_arg3)) == NULL) goto fail;
	if (arg4) if ((lparg4 = (*env)->GetIntArrayElements(env, arg4, NULL)) == NULL) goto fail;
	rc = (jint)FillConsoleOutputCharacterW((HANDLE)(intptr_t)arg0, arg1, arg2, *lparg3, lparg4);
fail:
	if (arg4 && lparg4) (*env)->ReleaseIntArrayElements(env, arg4, lparg4, 0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(FlushConsoleInputBuffer)
	(JNIEnv *env, jclass that, jlong arg0)
{
	jint rc = 0;
	rc = (jint)FlushConsoleInputBuffer((HANDLE)(intptr_t)arg0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(FormatMessageW)
	(JNIEnv *env, jclass that, jint arg0, jlong arg1, jint arg2, jint arg3, jbyteArray arg4, jint arg5, jlongArray arg6)
{
	jbyte *lparg4=NULL;
	jlong *lparg6=NULL;
	jint rc = 0;
    if (arg4) if ((lparg4 = (*env)->GetPrimitiveArrayCritical(env, arg4, NULL)) == NULL) goto fail;
    if (arg6) if ((lparg6 = (*env)->GetPrimitiveArrayCritical(env, arg6, NULL)) == NULL) goto fail;
	rc = (jint)FormatMessageW(arg0, (void *)(intptr_t)arg1, arg2, arg3, (void *)lparg4, arg5, (void *)NULL);
fail:
    if (arg6 && lparg6) (*env)->ReleasePrimitiveArrayCritical(env, arg6, lparg6, 0);
    if (arg4 && lparg4) (*env)->ReleasePrimitiveArrayCritical(env, arg4, lparg4, 0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(GetConsoleMode)
	(JNIEnv *env, jclass that, jlong arg0, jintArray arg1)
{
	jint *lparg1=NULL;
	jint rc = 0;
	if (arg1) if ((lparg1 = (*env)->GetIntArrayElements(env, arg1, NULL)) == NULL) goto fail;
	rc = (jint)GetConsoleMode((HANDLE)(intptr_t)arg0, lparg1);
fail:
	if (arg1 && lparg1) (*env)->ReleaseIntArrayElements(env, arg1, lparg1, 0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(GetConsoleOutputCP)
	(JNIEnv *env, jclass that)
{
	jint rc = 0;
	rc = (jint)GetConsoleOutputCP();
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(GetConsoleScreenBufferInfo)
	(JNIEnv *env, jclass that, jlong arg0, jobject arg1)
{
	CONSOLE_SCREEN_BUFFER_INFO _arg1, *lparg1=NULL;
	jint rc = 0;
	if (arg1) if ((lparg1 = &_arg1) == NULL) goto fail;
	rc = (jint)GetConsoleScreenBufferInfo((HANDLE)(intptr_t)arg0, lparg1);
fail:
	if (arg1 && lparg1) setCONSOLE_SCREEN_BUFFER_INFOFields(env, arg1, lparg1);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(GetLastError)
	(JNIEnv *env, jclass that)
{
	jint rc = 0;
	rc = (jint)GetLastError();
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(GetNumberOfConsoleInputEvents)
	(JNIEnv *env, jclass that, jlong arg0, jintArray arg1)
{
	jint *lparg1=NULL;
	jint rc = 0;
	if (arg1) if ((lparg1 = (*env)->GetIntArrayElements(env, arg1, NULL)) == NULL) goto fail;
	rc = (jint)GetNumberOfConsoleInputEvents((HANDLE)(intptr_t)arg0, lparg1);
fail:
	if (arg1 && lparg1) (*env)->ReleaseIntArrayElements(env, arg1, lparg1, 0);
	return rc;
}

JNIEXPORT jlong JNICALL Kernel32_NATIVE(GetStdHandle)
	(JNIEnv *env, jclass that, jint arg0)
{
	jlong rc = 0;
	rc = (intptr_t)(HANDLE)GetStdHandle(arg0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(PeekConsoleInputW)
	(JNIEnv *env, jclass that, jlong arg0, jlong arg1, jint arg2, jintArray arg3)
{
	jint *lparg3=NULL;
	jint rc = 0;
	if (arg3) if ((lparg3 = (*env)->GetIntArrayElements(env, arg3, NULL)) == NULL) goto fail;
	rc = (jint)PeekConsoleInputW((HANDLE)(intptr_t)arg0, (PINPUT_RECORD)(intptr_t)arg1, arg2, lparg3);
fail:
	if (arg3 && lparg3) (*env)->ReleaseIntArrayElements(env, arg3, lparg3, 0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(ReadConsoleInputW)
	(JNIEnv *env, jclass that, jlong arg0, jlong arg1, jint arg2, jintArray arg3)
{
	jint *lparg3=NULL;
	jint rc = 0;
	if (arg3) if ((lparg3 = (*env)->GetIntArrayElements(env, arg3, NULL)) == NULL) goto fail;
	rc = (jint)ReadConsoleInputW((HANDLE)(intptr_t)arg0, (PINPUT_RECORD)(intptr_t)arg1, arg2, lparg3);
fail:
	if (arg3 && lparg3) (*env)->ReleaseIntArrayElements(env, arg3, lparg3, 0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(ScrollConsoleScreenBuffer)
	(JNIEnv *env, jclass that, jlong arg0, jobject arg1, jobject arg2, jobject arg3, jobject arg4)
{
	SMALL_RECT _arg1, *lparg1=NULL;
	SMALL_RECT _arg2, *lparg2=NULL;
	COORD _arg3, *lparg3=NULL;
	CHAR_INFO _arg4, *lparg4=NULL;
	jint rc = 0;
	if (arg1) if ((lparg1 = getSMALL_RECTFields(env, arg1, &_arg1)) == NULL) goto fail;
	if (arg2) if ((lparg2 = getSMALL_RECTFields(env, arg2, &_arg2)) == NULL) goto fail;
	if (arg3) if ((lparg3 = getCOORDFields(env, arg3, &_arg3)) == NULL) goto fail;
	if (arg4) if ((lparg4 = getCHAR_INFOFields(env, arg4, &_arg4)) == NULL) goto fail;
	rc = (jint)ScrollConsoleScreenBuffer((HANDLE)(intptr_t)arg0, lparg1, lparg2, *lparg3, lparg4);
fail:
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(SetConsoleCursorPosition)
	(JNIEnv *env, jclass that, jlong arg0, jobject arg1)
{
	COORD _arg1, *lparg1=NULL;
	jint rc = 0;
	if (arg1) if ((lparg1 = getCOORDFields(env, arg1, &_arg1)) == NULL) goto fail;
	rc = (jint)SetConsoleCursorPosition((HANDLE)(intptr_t)arg0, *lparg1);
fail:
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(SetConsoleMode)
	(JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
	return (jint)SetConsoleMode((HANDLE)(intptr_t)arg0, arg1);
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(SetConsoleOutputCP)
	(JNIEnv *env, jclass that, jint arg0)
{
	return (jint)SetConsoleOutputCP(arg0);
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(SetConsoleTextAttribute)
	(JNIEnv *env, jclass that, jlong arg0, jshort arg1)
{
	return (jint)SetConsoleTextAttribute((HANDLE)arg0, arg1);
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(SetConsoleTitle)
	(JNIEnv *env, jclass that, jstring arg0)
{
	const jchar *lparg0= NULL;
	jint rc = 0;
	if (arg0) if ((lparg0 = (*env)->GetStringChars(env, arg0, NULL)) == NULL) goto fail;
	rc = (jint)SetConsoleTitle(lparg0);
fail:
	if (arg0 && lparg0) (*env)->ReleaseStringChars(env, arg0, lparg0);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(WaitForSingleObject)
	(JNIEnv *env, jclass that, jlong arg0, jint arg1)
{
	return (jint)WaitForSingleObject((HANDLE)arg0, arg1);
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(WriteConsoleW)
	(JNIEnv *env, jclass that, jlong arg0, jcharArray arg1, jint arg2, jintArray arg3, jlong arg4)
{
	jchar *lparg1=NULL;
	jint *lparg3=NULL;
	jint rc = 0;
	if (arg1) if ((lparg1 = (*env)->GetCharArrayElements(env, arg1, NULL)) == NULL) goto fail;
	if (arg3) if ((lparg3 = (*env)->GetIntArrayElements(env, arg3, NULL)) == NULL) goto fail;
	rc = (jint)WriteConsoleW((HANDLE)(intptr_t)arg0, lparg1, arg2, lparg3, (LPVOID)(intptr_t)arg4);
fail:
	if (arg3 && lparg3) (*env)->ReleaseIntArrayElements(env, arg3, lparg3, 0);
	if (arg1 && lparg1) (*env)->ReleaseCharArrayElements(env, arg1, lparg1, JNI_ABORT);
	return rc;
}

JNIEXPORT jint JNICALL Kernel32_NATIVE(_1getch)
	(JNIEnv *env, jclass that)
{
	jint rc = 0;
	rc = (jint)_getch();
	return rc;
}

JNIEXPORT void JNICALL Kernel32_NATIVE(free)
	(JNIEnv *env, jclass that, jlong arg0)
{
	free((void *)(intptr_t)arg0);
}

JNIEXPORT void JNICALL Kernel32_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "FOREGROUND_BLUE", "S"), (jshort)FOREGROUND_BLUE);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "FOREGROUND_GREEN", "S"), (jshort)FOREGROUND_GREEN);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "FOREGROUND_RED", "S"), (jshort)FOREGROUND_RED);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "FOREGROUND_INTENSITY", "S"), (jshort)FOREGROUND_INTENSITY);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "BACKGROUND_BLUE", "S"), (jshort)BACKGROUND_BLUE);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "BACKGROUND_GREEN", "S"), (jshort)BACKGROUND_GREEN);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "BACKGROUND_RED", "S"), (jshort)BACKGROUND_RED);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "BACKGROUND_INTENSITY", "S"), (jshort)BACKGROUND_INTENSITY);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_LEADING_BYTE", "S"), (jshort)COMMON_LVB_LEADING_BYTE);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_TRAILING_BYTE", "S"), (jshort)COMMON_LVB_TRAILING_BYTE);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_GRID_HORIZONTAL", "S"), (jshort)COMMON_LVB_GRID_HORIZONTAL);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_GRID_LVERTICAL", "S"), (jshort)COMMON_LVB_GRID_LVERTICAL);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_GRID_RVERTICAL", "S"), (jshort)COMMON_LVB_GRID_RVERTICAL);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_REVERSE_VIDEO", "S"), (jshort)COMMON_LVB_REVERSE_VIDEO);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "COMMON_LVB_UNDERSCORE", "S"), (jshort)COMMON_LVB_UNDERSCORE);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "FORMAT_MESSAGE_FROM_SYSTEM", "I"), (jint)FORMAT_MESSAGE_FROM_SYSTEM);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "STD_INPUT_HANDLE", "I"), (jint)STD_INPUT_HANDLE);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "STD_OUTPUT_HANDLE", "I"), (jint)STD_OUTPUT_HANDLE);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "STD_ERROR_HANDLE", "I"), (jint)STD_ERROR_HANDLE);
	(*env)->SetStaticLongField(env, that, (*env)->GetStaticFieldID(env, that, "INVALID_HANDLE_VALUE", "J"), (jlong)INVALID_HANDLE_VALUE);
   return;
}

JNIEXPORT jlong JNICALL Kernel32_NATIVE(malloc)
	(JNIEnv *env, jclass that, jlong arg0)
{
	jlong rc = 0;
	rc = (intptr_t)(void *)malloc((size_t)arg0);
	return rc;
}

JNIEXPORT void JNICALL CHAR_INFO_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(CHAR_INFO));
   return;
}

JNIEXPORT void JNICALL CONSOLE_SCREEN_BUFFER_INFO_NATIVE(init)(JNIEnv *env, jclass that)
{
#if defined(_WIN32) || defined(_WIN64)
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(CONSOLE_SCREEN_BUFFER_INFO));
#endif
   return;
}

JNIEXPORT void JNICALL COORD_NATIVE(init)(JNIEnv *env, jclass that)
{
#if defined(_WIN32) || defined(_WIN64)
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(COORD));
#endif
   return;
}

JNIEXPORT void JNICALL FOCUS_EVENT_RECORD_NATIVE(init)(JNIEnv *env, jclass that)
{
#if defined(_WIN32) || defined(_WIN64)
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(WINDOW_BUFFER_SIZE_RECORD));
#endif
   return;
}

JNIEXPORT void JNICALL INPUT_RECORD_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(INPUT_RECORD));
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "KEY_EVENT", "S"), (jshort)KEY_EVENT);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "MOUSE_EVENT", "S"), (jshort)MOUSE_EVENT);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "WINDOW_BUFFER_SIZE_EVENT", "S"), (jshort)WINDOW_BUFFER_SIZE_EVENT);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "FOCUS_EVENT", "S"), (jshort)FOCUS_EVENT);
	(*env)->SetStaticShortField(env, that, (*env)->GetStaticFieldID(env, that, "MENU_EVENT", "S"), (jshort)MENU_EVENT);
   return;
}
JNIEXPORT void JNICALL INPUT_RECORD_NATIVE(memmove)
	(JNIEnv *env, jclass that, jobject arg0, jlong arg1, jlong arg2)
{
	INPUT_RECORD _arg0, *lparg0=NULL;
	if (arg0) if ((lparg0 = &_arg0) == NULL) goto fail;
	memmove((void *)lparg0, (const void *)(intptr_t)arg1, (size_t)arg2);
fail:
	if (arg0 && lparg0) setINPUT_RECORDFields(env, arg0, lparg0);
}

JNIEXPORT void JNICALL KEY_EVENT_RECORD_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(KEY_EVENT_RECORD));
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "CAPSLOCK_ON", "I"), (jint)CAPSLOCK_ON);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "NUMLOCK_ON", "I"), (jint)NUMLOCK_ON);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SCROLLLOCK_ON", "I"), (jint)SCROLLLOCK_ON);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "ENHANCED_KEY", "I"), (jint)ENHANCED_KEY);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "LEFT_ALT_PRESSED", "I"), (jint)LEFT_ALT_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "LEFT_CTRL_PRESSED", "I"), (jint)LEFT_CTRL_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "RIGHT_ALT_PRESSED", "I"), (jint)RIGHT_ALT_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "RIGHT_CTRL_PRESSED", "I"), (jint)RIGHT_CTRL_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SHIFT_PRESSED", "I"), (jint)SHIFT_PRESSED);
   return;
}

JNIEXPORT void JNICALL MENU_EVENT_RECORD_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(MENU_EVENT_RECORD));
   return;
}

JNIEXPORT void JNICALL MOUSE_EVENT_RECORD_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(MOUSE_EVENT_RECORD));
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "FROM_LEFT_1ST_BUTTON_PRESSED", "I"), (jint)FROM_LEFT_1ST_BUTTON_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "FROM_LEFT_2ND_BUTTON_PRESSED", "I"), (jint)FROM_LEFT_2ND_BUTTON_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "FROM_LEFT_3RD_BUTTON_PRESSED", "I"), (jint)FROM_LEFT_3RD_BUTTON_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "FROM_LEFT_4TH_BUTTON_PRESSED", "I"), (jint)FROM_LEFT_4TH_BUTTON_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "RIGHTMOST_BUTTON_PRESSED", "I"), (jint)RIGHTMOST_BUTTON_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "CAPSLOCK_ON", "I"), (jint)CAPSLOCK_ON);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "NUMLOCK_ON", "I"), (jint)NUMLOCK_ON);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SCROLLLOCK_ON", "I"), (jint)SCROLLLOCK_ON);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "ENHANCED_KEY", "I"), (jint)ENHANCED_KEY);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "LEFT_ALT_PRESSED", "I"), (jint)LEFT_ALT_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "LEFT_CTRL_PRESSED", "I"), (jint)LEFT_CTRL_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "RIGHT_ALT_PRESSED", "I"), (jint)RIGHT_ALT_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "RIGHT_CTRL_PRESSED", "I"), (jint)RIGHT_CTRL_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SHIFT_PRESSED", "I"), (jint)SHIFT_PRESSED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "DOUBLE_CLICK", "I"), (jint)DOUBLE_CLICK);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "MOUSE_HWHEELED", "I"), (jint)MOUSE_HWHEELED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "MOUSE_MOVED", "I"), (jint)MOUSE_MOVED);
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "MOUSE_WHEELED", "I"), (jint)MOUSE_WHEELED);
   return;
}

JNIEXPORT void JNICALL SMALL_RECT_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(SMALL_RECT));
   return;
}

JNIEXPORT void JNICALL WINDOW_BUFFER_SIZE_RECORD_NATIVE(init)(JNIEnv *env, jclass that)
{
	(*env)->SetStaticIntField(env, that, (*env)->GetStaticFieldID(env, that, "SIZEOF", "I"), (jint)sizeof(WINDOW_BUFFER_SIZE_RECORD));
   return;
}

typedef struct _UNICODE_STRING {
	USHORT Length;
	USHORT MaximumLength;
	PWSTR  Buffer;
} UNICODE_STRING, *PUNICODE_STRING;

typedef struct _OBJECT_NAME_INFORMATION {
	UNICODE_STRING          Name;
	WCHAR                   NameBuffer[0];
} OBJECT_NAME_INFORMATION, *POBJECT_NAME_INFORMATION;

typedef enum {
	ObjectBasicInformation,
	ObjectNameInformation,
	ObjectTypeInformation,
	ObjectAllInformation,
	ObjectDataInformation
} OBJECT_INFORMATION_CLASS;

typedef NTSTATUS (NTAPI *TFNNtQueryObject)(HANDLE, OBJECT_INFORMATION_CLASS, PVOID, ULONG, PULONG);
TFNNtQueryObject NtQueryObject = 0;

HANDLE hModuleNtDll = 0;

JNIEXPORT jint JNICALL Kernel32_NATIVE(isatty)
	(JNIEnv *env, jclass that, jint arg0)
{
	jint rc;

	ULONG result;
	BYTE buffer[1024];
	POBJECT_NAME_INFORMATION nameinfo = (POBJECT_NAME_INFORMATION) buffer;
	PWSTR name;
	DWORD mode;

	/* check if fd is a pipe */
	HANDLE h = (HANDLE) _get_osfhandle(arg0);
	DWORD t = h != NULL ? GetFileType(h) : 0;
	if (h != NULL && t == FILE_TYPE_CHAR) {
	    // check that this is a real tty because the /dev/null
	    // and /dev/zero streams are also of type FILE_TYPE_CHAR
		rc = GetConsoleMode(h, &mode) != 0;
	}
	else {
		if (hModuleNtDll == 0) {
			hModuleNtDll = LoadLibraryW(L"ntdll.dll");
		}
		if (hModuleNtDll == 0) {
			rc = 0;
		}
		else {
			if (NtQueryObject == 0) {
				NtQueryObject = (TFNNtQueryObject) GetProcAddress(hModuleNtDll, "NtQueryObject");
			}
			if (NtQueryObject == 0) {
				rc = 0;
			}
			/* get pipe name */
			else if (NtQueryObject(h, ObjectNameInformation, buffer, sizeof(buffer) - 2, &result) != 0) {
				rc = 0;
			}
			else {

				name = nameinfo->Name.Buffer;
				if (name == NULL) {
				    rc = 0;
				}
				else {
                    name[nameinfo->Name.Length / 2] = 0;

                    //fprintf( stderr, "Standard stream %d: pipe name: %S\n", arg0, name);

                    /*
                     * Check if this could be a MSYS2 pty pipe ('msys-XXXX-ptyN-XX')
                     * or a cygwin pty pipe ('cygwin-XXXX-ptyN-XX')
                     */
                    if ((wcsstr(name, L"msys-") || wcsstr(name, L"cygwin-")) && wcsstr(name, L"-pty")) {
                        rc = 1;
                    } else {
                        // This is definitely not a tty
                        rc = 0;
                    }
                }
			}
		}
	}

	return rc;
}

#endif
