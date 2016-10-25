//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/AbstractWindowedCursor.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_AndroidDatabaseAbstractWindowedCursor")
#ifdef RESTRICT_AndroidDatabaseAbstractWindowedCursor
#define INCLUDE_ALL_AndroidDatabaseAbstractWindowedCursor 0
#else
#define INCLUDE_ALL_AndroidDatabaseAbstractWindowedCursor 1
#endif
#undef RESTRICT_AndroidDatabaseAbstractWindowedCursor

#if !defined (AndroidDatabaseAbstractWindowedCursor_) && (INCLUDE_ALL_AndroidDatabaseAbstractWindowedCursor || defined(INCLUDE_AndroidDatabaseAbstractWindowedCursor))
#define AndroidDatabaseAbstractWindowedCursor_

#define RESTRICT_AndroidDatabaseAbstractCursor 1
#define INCLUDE_AndroidDatabaseAbstractCursor 1
#include "android/database/AbstractCursor.h"

@class AndroidDatabaseCharArrayBuffer;
@class AndroidDatabaseCursorWindow;
@class IOSByteArray;

@interface AndroidDatabaseAbstractWindowedCursor : AndroidDatabaseAbstractCursor {
 @public
  AndroidDatabaseCursorWindow *mWindow_;
}

#pragma mark Public

- (instancetype)init;

- (void)copyStringToBufferWithInt:(jint)columnIndex
withAndroidDatabaseCharArrayBuffer:(AndroidDatabaseCharArrayBuffer *)buffer OBJC_METHOD_FAMILY_NONE;

- (IOSByteArray *)getBlobWithInt:(jint)columnIndex;

- (jdouble)getDoubleWithInt:(jint)columnIndex;

- (jfloat)getFloatWithInt:(jint)columnIndex;

- (jint)getIntWithInt:(jint)columnIndex;

- (jlong)getLongWithInt:(jint)columnIndex;

- (jshort)getShortWithInt:(jint)columnIndex;

- (NSString *)getStringWithInt:(jint)columnIndex;

- (jint)getTypeWithInt:(jint)columnIndex;

- (AndroidDatabaseCursorWindow *)getWindow;

- (jboolean)hasWindow;

- (jboolean)isBlobWithInt:(jint)columnIndex;

- (jboolean)isFloatWithInt:(jint)columnIndex;

- (jboolean)isLongWithInt:(jint)columnIndex;

- (jboolean)isNullWithInt:(jint)columnIndex;

- (jboolean)isStringWithInt:(jint)columnIndex;

- (void)setWindowWithAndroidDatabaseCursorWindow:(AndroidDatabaseCursorWindow *)window;

#pragma mark Protected

- (void)checkPosition;

- (void)clearOrCreateWindowWithNSString:(NSString *)name;

- (void)closeWindow;

- (void)onDeactivateOrClose;

@end

J2OBJC_EMPTY_STATIC_INIT(AndroidDatabaseAbstractWindowedCursor)

J2OBJC_FIELD_SETTER(AndroidDatabaseAbstractWindowedCursor, mWindow_, AndroidDatabaseCursorWindow *)

FOUNDATION_EXPORT void AndroidDatabaseAbstractWindowedCursor_init(AndroidDatabaseAbstractWindowedCursor *self);

J2OBJC_TYPE_LITERAL_HEADER(AndroidDatabaseAbstractWindowedCursor)

#endif

#pragma pop_macro("INCLUDE_ALL_AndroidDatabaseAbstractWindowedCursor")
