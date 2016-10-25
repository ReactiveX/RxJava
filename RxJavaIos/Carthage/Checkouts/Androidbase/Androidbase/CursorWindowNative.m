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
//  CursorWindow.m
//  squidb-ios
//  This file is a fork/port of AOSP CursowWindow.cpp (https://github.com/android/platform_frameworks_base/blob/master/libs/androidfw/CursorWindow.cpp)
//  The core logic/structures defined in the file have been left intact; this is just a translation to use Objective-C
//  syntax instead of C++ to make working with the j2objc tool easier.
//

#import "CursorWindowNative.h"
#import "java/lang/IllegalStateException.h"
#import "NSString+JavaString.h"

@implementation CursorWindowNative {
    void *data;
    struct Header *mHeader;
}

static void throwExceptionWithRowCol(jint row, jint column) {
    @throw [[JavaLangIllegalStateException alloc]
            initWithNSString:[NSString stringWithFormat:@"Couldn't read row %d, col %d from CursorWindow. Make sure the Cursor is initialized correctly before accessing data from it.", row, column]];
}

static void throwUnknownTypeException(jint type) {
    @throw [[JavaLangIllegalStateException alloc]
            initWithNSString:[NSString stringWithFormat:@"UNKNOWN type %d", type]];
}

// Class methods
+ (id<NSObject>) nativeCreate:(NSString *)name cursorWindowSize:(jint)cursorWindowSize {
CursorWindowNative* ret = [[CursorWindowNative alloc] initWithName:name size:cursorWindowSize isReadOnly:NO];
//[ret autorelease];
    return ret;
}

+ (void) nativeDispose:(id<NSObject>)windowPtr {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    [window hardDump];
    [window release];
}

- (void)hardDump {
    free(data);
        data = NULL;
}

+ (void) nativeClear:(id<NSObject>)windowPtr {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    status_t status = [window clear];
    if (status) {
//        LOG_WINDOW("Could not clear window. error=%d", status);
    }
}

+ (jint) nativeGetNumRows:(id<NSObject>)windowPtr {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    return [window getNumRows];
}

+ (jboolean) nativeSetNumColumns:(id<NSObject>)windowPtr columnNum:(jint)columnNum {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    status_t status = [window setNumColumns:columnNum];
    return status == OK;
}

+ (jboolean) nativeAllocRow:(id<NSObject>)windowPtr {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    status_t status = [window allocRow];
    return status == OK;
}

+ (void) nativeFreeLastRow:(id<NSObject>)windowPtr {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    [window freeLastRow];
}

+ (jint) nativeGetType:(id<NSObject>)windowPtr row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
//    LOG_WINDOW("returning column type affinity for %d,%d from %p", row, column, window);

    struct FieldSlot *fieldSlot = [window getFieldSlot:row column:column];
    if (!fieldSlot) {
        // FIXME: This is really broken but we have CTS tests that depend
        // on this legacy behavior.
        // throwExceptionWithRowCol(row, column);
        return FIELD_TYPE_NULL;
    }
    return fieldSlot->type;
}

// NATIVE GETTERS HERE

const char* getFieldSlotValueString(CursorWindowNative *window, struct FieldSlot* fieldSlot,
                                           uint32_t* outSizeIncludingNull) {
    *outSizeIncludingNull = fieldSlot->data.buffer.size;
    return (char *)[window offsetToPtr:fieldSlot->data.buffer.offset];
}

const void* getFieldSlotValueBlob(CursorWindowNative *window, struct FieldSlot* fieldSlot, uint32_t* outSize) {
    *outSize = fieldSlot->data.buffer.size;
    return [window offsetToPtr:fieldSlot->data.buffer.offset];
}

+ (IOSByteArray *)nativeGetBlob:(id<NSObject>)windowPtr row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
//    LOG_WINDOW("Getting blob for %d,%d from %p", row, column, window);

    struct FieldSlot *fieldSlot = [window getFieldSlot:row column:column];
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return NULL;
    }

    int32_t type = fieldSlot->type;
    if (type == FIELD_TYPE_BLOB || type == FIELD_TYPE_STRING) {
        uint32_t size;
        const void* value = getFieldSlotValueBlob(window, fieldSlot, &size);
        IOSByteArray *byteArray = [IOSByteArray newArrayWithBytes:value count:size];
//        jbyteArray byteArray = env->NewByteArray(size);
        if (!byteArray) {
//            env->ExceptionClear();
            throw_sqlite3_exception_message(NULL, "Native could not create new byte[]");
            return NULL;
        }
        [byteArray autorelease];
//        env->SetByteArrayRegion(byteArray, 0, size, static_cast<const jbyte*>(value));
        return byteArray;
    } else if (type == FIELD_TYPE_INTEGER) {
        throw_sqlite3_exception_message(NULL, "INTEGER data in nativeGetBlob ");
    } else if (type == FIELD_TYPE_FLOAT) {
        throw_sqlite3_exception_message(NULL, "FLOAT data in nativeGetBlob ");
    } else if (type == FIELD_TYPE_NULL) {
        // do nothing
    } else {
        throwUnknownTypeException(type);
    }
    return NULL;
}

+ (NSString *)nativeGetString:(id<NSObject>)windowPtr row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
//    LOG_WINDOW("Getting string for %d,%d from %p", row, column, window);

    struct FieldSlot *fieldSlot = [window getFieldSlot:row column:column];
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return NULL;
    }

    int32_t type = fieldSlot->type;
    if (type == FIELD_TYPE_STRING) {
        uint32_t sizeIncludingNull;
        const char* value = getFieldSlotValueString(window, fieldSlot, &sizeIncludingNull);
        if (sizeIncludingNull <= 1) {
            return @"";
        }

        IOSByteArray *bytes = [IOSByteArray newArrayWithBytes:(const jbyte *)value count:sizeIncludingNull - 1];
        NSString *result = [NSString stringWithBytes:bytes offset:0 length:[bytes count] encoding:NSUTF8StringEncoding];

#if ! __has_feature(objc_arc)
[bytes release];
#endif
        return result;
    } else if (type == FIELD_TYPE_INTEGER) {
        int64_t value = fieldSlot->data.l;
        return [NSString stringWithFormat:@"%lld", value];
    } else if (type == FIELD_TYPE_FLOAT) {
        double value = fieldSlot->data.d;
        return [NSString stringWithFormat:@"%f", value];
    } else if (type == FIELD_TYPE_NULL) {
        return NULL;
    } else if (type == FIELD_TYPE_BLOB) {
        throw_sqlite3_exception_message(NULL, "Unable to convert BLOB to string");
        return NULL;
    } else {
        throwUnknownTypeException(type);
        return NULL;
    }
}

+ (jlong) nativeGetLong:(id<NSObject>)windowPtr row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
//    LOG_WINDOW("Getting long for %d,%d from %p", row, column, window);

    struct FieldSlot *fieldSlot = [window getFieldSlot:row column:column];
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return 0;
    }

    int32_t type = fieldSlot->type;
    if (type == FIELD_TYPE_INTEGER) {
        return fieldSlot->data.l;
    } else if (type == FIELD_TYPE_STRING) {
        uint32_t sizeIncludingNull;
        const char* value = getFieldSlotValueString(window, fieldSlot, &sizeIncludingNull);
        return sizeIncludingNull > 1 ? strtoll(value, NULL, 0) : 0L;
    } else if (type == FIELD_TYPE_FLOAT) {
        double fieldValue = fieldSlot->data.d;
        return (jlong)fieldValue;
    } else if (type == FIELD_TYPE_NULL) {
        return 0;
    } else if (type == FIELD_TYPE_BLOB) {
        throw_sqlite3_exception_message(NULL, "Unable to convert BLOB to long");
        return 0;
    } else {
        throwUnknownTypeException(type);
        return 0;
    }
}

+ (jdouble) nativeGetDouble:(id<NSObject>)windowPtr row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
//    LOG_WINDOW("Getting double for %d,%d from %p", row, column, window);

    struct FieldSlot *fieldSlot = [window getFieldSlot:row column:column];
    if (!fieldSlot) {
        throwExceptionWithRowCol(row, column);
        return 0.0;
    }

    int32_t type = fieldSlot->type;
    if (type == FIELD_TYPE_FLOAT) {
        return fieldSlot->data.d;
    } else if (type == FIELD_TYPE_STRING) {
        uint32_t sizeIncludingNull;
        const char* value = getFieldSlotValueString(window, fieldSlot, &sizeIncludingNull);
        return sizeIncludingNull > 1 ? strtod(value, NULL) : 0.0;
    } else if (type == FIELD_TYPE_INTEGER) {
        jlong fieldValue = fieldSlot->data.l;
        return (jdouble)fieldValue;
    } else if (type == FIELD_TYPE_NULL) {
        return 0.0;
    } else if (type == FIELD_TYPE_BLOB) {
        throw_sqlite3_exception_message(NULL, "Unable to convert BLOB to double");
        return 0.0;
    } else {
        throwUnknownTypeException(type);
        return 0.0;
    }
}

+ (jboolean) nativePutBlob:(id<NSObject>)windowPtr value:(IOSByteArray *)value row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    jint len = [value length];

    void* buffer = [value buffer]; //env->GetPrimitiveArrayCritical(valueObj, NULL);
    status_t status = [window putBlobInRow:row column:column value:buffer size:len];
//    env->ReleasePrimitiveArrayCritical(valueObj, buffer, JNI_ABORT);

    if (status) {
//        LOG_WINDOW("Failed to put blob. error=%d", status);
        return false;
    }

//    LOG_WINDOW("%d,%d is BLOB with %u bytes", row, column, len);
    return true;
}

+ (jboolean) nativePutString:(NSObject *)windowPtr value:(NSString *)value row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);

    uint32_t sizeIncludingNull = (uint32_t)[value lengthOfBytesUsingEncoding:NSUTF8StringEncoding] + 1; //env->GetStringUTFLength(valueObj) + 1;
    const char* valueStr = [value UTF8String]; //env->GetStringUTFChars(valueObj, NULL);
    if (!valueStr) {
//        LOG_WINDOW("value can't be transferred to UTFChars");
        return false;
    }
    status_t status = [window putStringInRow:row column:column value:valueStr size:sizeIncludingNull];
//    env->ReleaseStringUTFChars(valueObj, valueStr);

    if (status) {
//        LOG_WINDOW("Failed to put string. error=%d", status);
        return false;
    }

//    LOG_WINDOW("%d,%d is TEXT with %u bytes", row, column, sizeIncludingNull);
    return true;
}

+ (jboolean) nativePutLong:(NSObject *)windowPtr value:(jlong)value row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    status_t status = [window putLongInRow:row column:column value:value];

    if (status) {
//        LOG_WINDOW("Failed to put long. error=%d", status);
        return false;
    }

//    LOG_WINDOW("%d,%d is INTEGER 0x%016llx", row, column, value);
    return true;
}

+ (jboolean) nativePutDouble:(NSObject *)windowPtr value:(jdouble)value row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    status_t status = [window putDoubleInRow:row column:column value:value];

    if (status) {
//        LOG_WINDOW("Failed to put double. error=%d", status);
        return false;
    }

//    LOG_WINDOW("%d,%d is FLOAT %lf", row, column, value);
    return true;
}

+ (jboolean) nativePutNull:(NSObject *)windowPtr row:(jint)row column:(jint)column {
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);
    status_t status = [window putNullInRow:row column:column];

    if (status) {
//        LOG_WINDOW("Failed to put null. error=%d", status);
        return false;
    }

//    LOG_WINDOW("%d,%d is NULL", row, column);
    return true;
}

// Instance methods
@synthesize mName;
@synthesize mSize;
@synthesize mIsReadOnly;

- (void *) offsetToPtr:(uint32_t) offset {
    return (uint8_t *)(data) + offset;
}

- (id)initWithName:(NSString *)_name size:(uint32_t)_size isReadOnly:(BOOL)readOnly {
    if (self = [super init]) {
        self.mName = [@"CursorWindow: " stringByAppendingString:_name];
        self.mIsReadOnly = readOnly;
        self.mSize = _size;
        data = malloc(_size);
        mHeader = (struct Header *)data;
        if (data == NULL) {
            return nil;
        }
    }
    return self;
}

- (status_t) clear {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    mHeader->freeOffset = sizeof(struct Header) + sizeof(struct RowSlotChunk);
    mHeader->firstChunkOffset = sizeof(struct Header);
    mHeader->numRows = 0;
    mHeader->numColumns = 0;

    struct RowSlotChunk* firstChunk = (struct RowSlotChunk *)[self offsetToPtr:mHeader->firstChunkOffset];
    firstChunk->nextChunkOffset = 0;
    return OK;
}

- (status_t) setNumColumns:(uint32_t)numColumns {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    uint32_t cur = mHeader->numColumns;
    if ((cur > 0 || mHeader->numRows > 0) && cur != numColumns) {
//        ALOGE("Trying to go from %d columns to %d", cur, numColumns);
        return INVALID_OPERATION;
    }
    mHeader->numColumns = numColumns;
    return OK;
}

- (uint32_t) alloc:(uint32_t)_size aligned:(bool)aligned {
    uint32_t padding;
    if (aligned) {
        // 4 byte alignment
        padding = (~mHeader->freeOffset + 1) & 3;
    } else {
        padding = 0;
    }

    uint32_t offset = mHeader->freeOffset + padding;
    uint32_t nextFreeOffset = offset + _size;
    if (nextFreeOffset > mSize) {
//        ALOGW("Window is full: requested allocation %zu bytes, "
//              "free space %zu bytes, window size %zu bytes",
//              size, freeSpace(), mSize);
        return 0;
    }

    mHeader->freeOffset = nextFreeOffset;
    return offset;
}

- (struct RowSlot*) allocRowSlot {
    uint32_t chunkPos = mHeader->numRows;
    struct RowSlotChunk* chunk = (struct RowSlotChunk *)[self offsetToPtr:mHeader->firstChunkOffset];
    while (chunkPos > ROW_SLOT_CHUNK_NUM_ROWS) {
        chunk = (struct RowSlotChunk *)[self offsetToPtr:chunk->nextChunkOffset];
        chunkPos -= ROW_SLOT_CHUNK_NUM_ROWS;
    }
    if (chunkPos == ROW_SLOT_CHUNK_NUM_ROWS) {
        if (!chunk->nextChunkOffset) {
            chunk->nextChunkOffset = [self alloc:sizeof(struct RowSlotChunk) aligned:true];
            if (!chunk->nextChunkOffset) {
                return NULL;
            }
        }
        chunk = (struct RowSlotChunk *)[self offsetToPtr:chunk->nextChunkOffset];
        chunk->nextChunkOffset = 0;
        chunkPos = 0;
    }
    mHeader->numRows += 1;
    return &chunk->slots[chunkPos];
}

- (status_t) allocRow {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    // Fill in the row slot
    struct RowSlot* rowSlot = [self allocRowSlot];
    if (rowSlot == NULL) {
        return NO_MEMORY;
    }

    // Allocate the slots for the field directory
    uint32_t fieldDirSize = mHeader->numColumns * sizeof(struct FieldSlot);
    uint32_t fieldDirOffset = [self alloc:fieldDirSize aligned:true];
    if (!fieldDirOffset) {
        mHeader->numRows--;
//        LOG_WINDOW("The row failed, so back out the new row accounting "
//                   "from allocRowSlot %d", mHeader->numRows);
        return NO_MEMORY;
    }
    struct FieldSlot* fieldDir = (struct FieldSlot *)[self offsetToPtr:fieldDirOffset];
    memset(fieldDir, 0, fieldDirSize);

//    LOG_WINDOW("Allocated row %u, rowSlot is at offset %u, fieldDir is %d bytes at offset %u\n",
//               mHeader->numRows - 1, offsetFromPtr(rowSlot), fieldDirSize, fieldDirOffset);
    rowSlot->offset = fieldDirOffset;
    return OK;
}

- (status_t) freeLastRow {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    if (mHeader->numRows > 0) {
        mHeader->numRows--;
    }
    return OK;
}

- (jint) getNumRows {
    return mHeader->numRows;
}

- (jint) getNumColumns {
    return mHeader->numColumns;
}

- (struct RowSlot *) getRowSlot:(uint32_t)row {
    uint32_t chunkPos = row;
    struct RowSlotChunk* chunk = (struct RowSlotChunk *)[self offsetToPtr:mHeader->firstChunkOffset];
    while (chunkPos >= ROW_SLOT_CHUNK_NUM_ROWS) {
        chunk = (struct RowSlotChunk *)[self offsetToPtr:chunk->nextChunkOffset];
        chunkPos -= ROW_SLOT_CHUNK_NUM_ROWS;
    }
    return &chunk->slots[chunkPos];
}

- (struct FieldSlot *) getFieldSlot:(uint32_t)row column:(uint32_t)column {
    if (row >= mHeader->numRows || column >= mHeader->numColumns) {
//        ALOGE("Failed to read row %d, column %d from a CursorWindow which "
//              "has %d rows, %d columns.",
//              row, column, mHeader->numRows, mHeader->numColumns);
        return NULL;
    }
    struct RowSlot* rowSlot = [self getRowSlot:row];
    if (!rowSlot) {
//        ALOGE("Failed to find rowSlot for row %d.", row);
        return NULL;
    }
    struct FieldSlot* fieldDir = (struct FieldSlot *)[self offsetToPtr:rowSlot->offset];
    return &fieldDir[column];
}

- (status_t) putBlobInRow:(uint32_t)row column:(uint32_t)column value:(const void *)value size:(uint32_t)size {
    return [self putBlobOrStringInRow:row column:column value:value size:size type:FIELD_TYPE_BLOB];
}

- (status_t) putStringInRow:(uint32_t)row column:(uint32_t)column value:(const char *)value
                       size:(uint32_t)sizeIncludingNull {
    return [self putBlobOrStringInRow:row column:column value:value size:sizeIncludingNull type:FIELD_TYPE_STRING];
}

- (status_t) putBlobOrStringInRow:(uint32_t)row column:(uint32_t) column
            value:(const void *)value size:(uint32_t)size type:(int32_t)type {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    struct FieldSlot* fieldSlot = [self getFieldSlot:row column:column];
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    uint32_t offset = [self alloc:size aligned:false];
    if (!offset) {
        return NO_MEMORY;
    }

    memcpy([self offsetToPtr:offset], value, size);

    fieldSlot->type = type;
    fieldSlot->data.buffer.offset = offset;
    fieldSlot->data.buffer.size = (uint32_t)size;
    return OK;
}

- (status_t) putLongInRow:(uint32_t)row column:(uint32_t)column value:(jlong)value {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    struct FieldSlot* fieldSlot = [self getFieldSlot:row column:column];
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    fieldSlot->type = FIELD_TYPE_INTEGER;
    fieldSlot->data.l = value;
    return OK;
}

- (status_t) putDoubleInRow:(uint32_t)row column:(uint32_t)column value:(jdouble)value {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    struct FieldSlot* fieldSlot = [self getFieldSlot:row column:column];
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    fieldSlot->type = FIELD_TYPE_FLOAT;
    fieldSlot->data.d = value;
    return OK;
}

- (status_t) putNullInRow:(uint32_t)row column:(uint32_t)column {
    if (self.mIsReadOnly) {
        return INVALID_OPERATION;
    }

    struct FieldSlot* fieldSlot = [self getFieldSlot:row column:column];
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    fieldSlot->type = FIELD_TYPE_NULL;
    fieldSlot->data.buffer.offset = 0;
    fieldSlot->data.buffer.size = 0;
    return OK;
}

- (void)dealloc {
RELEASE_(mName);
    mHeader = NULL;
    [super dealloc];
}

@end
