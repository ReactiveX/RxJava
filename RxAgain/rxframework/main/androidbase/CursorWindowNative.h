/*
 * Copyright (C) 2006-2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//
//  CursorWindow.h
//  squidb-ios
//  This file is a fork/port of AOSP CursorWindow.h (https://github.com/android/platform_frameworks_base/blob/master/include/androidfw/CursorWindow.h)
//  The core logic/structures defined in the file have been left intact; this is just a translation to use Objective-C
//  syntax instead of C++ to make working with the j2objc tool easier.
//
    
#import <Foundation/Foundation.h>
#import "IOSPrimitiveArray.h"
#import "SQLiteErrors.h"

static const uint32_t ROW_SLOT_CHUNK_NUM_ROWS = 100;

@interface CursorWindowNative : NSObject

enum {
    FIELD_TYPE_NULL = 0,
    FIELD_TYPE_INTEGER = 1,
    FIELD_TYPE_FLOAT = 2,
    FIELD_TYPE_STRING = 3,
    FIELD_TYPE_BLOB = 4,
};

struct Header {
    // Offset of the lowest unused byte in the window.
    uint32_t freeOffset;

    // Offset of the first row slot chunk.
    uint32_t firstChunkOffset;

    uint32_t numRows;
    uint32_t numColumns;
};

struct RowSlot {
    uint32_t offset;
};

struct RowSlotChunk {
    struct RowSlot slots[ROW_SLOT_CHUNK_NUM_ROWS];
    uint32_t nextChunkOffset;
};

struct FieldSlot {
    int32_t type;
    union {
        double d;
        int64_t l;
        struct {
            uint32_t offset;
            uint32_t size;
        } buffer;
    } data;
};

+ (id<NSObject>)nativeCreate:(NSString *)name cursorWindowSize:(jint)cursorWindowSize;

+ (void) nativeDispose:(id<NSObject>)windowPtr;

+ (void) nativeClear:(id<NSObject>)windowPtr;

+ (jint) nativeGetNumRows:(id<NSObject>)windowPtr;

+ (jboolean) nativeSetNumColumns:(id<NSObject>)windowPtr columnNum:(jint)columnNum;

+ (jboolean) nativeAllocRow:(id<NSObject>)windowPtr;

+ (void) nativeFreeLastRow:(id<NSObject>)windowPtr;

+ (jint) nativeGetType:(id<NSObject>)windowPtr row:(jint)row column:(jint)column;

+ (IOSByteArray *) nativeGetBlob:(id<NSObject>)windowPtr row:(jint)row column:(jint)column;

+ (NSString *) nativeGetString:(id<NSObject>)windowPtr row:(jint) row column:(jint) column;

+ (jlong) nativeGetLong:(id<NSObject>)windowPtr row:(jint) row column:(jint) column;

+ (jdouble) nativeGetDouble:(id<NSObject>)windowPtr row:(jint) row column:(jint) column;

//private static native void nativeCopyStringToBuffer(Object windowPtr, int row, int column, CharArrayBuffer buffer);

+ (jboolean) nativePutBlob:(id<NSObject>)windowPtr value:(IOSByteArray *)value row:(jint)row column:(jint)column;

+ (jboolean) nativePutString:(id<NSObject>)windowPtr value:(NSString *)value row:(jint)row column:(jint)column;

+ (jboolean) nativePutLong:(id<NSObject>)windowPtr value:(jlong)value row:(jint)row column:(jint)column;

+ (jboolean) nativePutDouble:(id<NSObject>)windowPtr value:(jdouble)value row:(jint)row column:(jint)column;

+ (jboolean) nativePutNull:(id<NSObject>)windowPtr row:(jint)row column:(jint)column;

@property(retain) NSString *mName;
@property uint32_t mSize;
@property BOOL mIsReadOnly;

- (id) initWithName:(NSString *)name size:(uint32_t)size isReadOnly:(BOOL)readOnly;

- (status_t) clear;

- (status_t) setNumColumns:(uint32_t)numColumns;

- (status_t) allocRow;

- (status_t) freeLastRow;

- (jint) getNumRows;

- (jint) getNumColumns;

- (struct RowSlot *) getRowSlot:(uint32_t)row;

- (struct RowSlot *)allocRowSlot;

- (struct FieldSlot *) getFieldSlot:(uint32_t)row column:(uint32_t)column;

- (status_t) putBlobInRow:(uint32_t)row column:(uint32_t)column value:(const void *)value size:(uint32_t)size;

- (status_t) putStringInRow:(uint32_t)row column:(uint32_t)column value:(const char *)value
                       size:(uint32_t)sizeIncludingNull;

- (status_t) putBlobOrStringInRow:(uint32_t)row column:(uint32_t) column
                            value:(const void *)value size:(uint32_t)size type:(int32_t)type;

- (status_t) putLongInRow:(uint32_t)row column:(uint32_t)column value:(jlong)value;

- (status_t) putDoubleInRow:(uint32_t)row column:(uint32_t)column value:(jdouble)value;

- (status_t) putNullInRow:(uint32_t)row column:(uint32_t)column;

@end
