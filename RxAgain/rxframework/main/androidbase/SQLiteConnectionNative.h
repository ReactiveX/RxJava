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
//  SQLiteConnectionNative.h
//  squidb-ios
//  This file is a fork/port of AOSP SQLiteConnection.cpp (https://github.com/android/platform_frameworks_base/blob/master/core/jni/android_database_SQLiteConnection.cpp)
//  The core logic/structures defined in the file have been left intact; this is just a translation to use Objective-C
//  syntax instead of C++ to make working with the j2objc tool easier.
//

#import <sqlite3.h>
#import "IOSPrimitiveArray.h"

@interface SQLiteConnectionNative : NSObject

@property sqlite3 *db;
@property (retain) NSString *path;
@property (retain) NSString *label;
@property int openFlags;

+ (SQLiteConnectionNative *) nativeOpen:(NSString *)pathStr openFlags:(jint) openFlags labelStr:(NSString *)labelStr enableTrace:(jboolean)enableTrace enableProfile:(jboolean)enableProfile;

+ (void) nativeClose:(NSObject *)connectionPtr;

//+ (void) nativeRegisterLocalizedCollators:(NSObject *)connectionPtr withLocaleStr:(NSString *)localeStr;

+ (NSObject *) nativePrepareStatement:(NSObject *)connectionPtr withSql:(NSString *)sqlString;

+ (void) nativeFinalizeStatement:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jint) nativeGetParameterCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jboolean) nativeIsReadOnly:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jint) nativeGetColumnCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (NSString *) nativeGetColumnName:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index;

+ (void) nativeBindNull:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index;

+ (void) nativeBindLong:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(jlong)value;

+ (void) nativeBindDouble:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(jdouble)value;

+ (void) nativeBindBlob:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(IOSByteArray *)value;

+ (void) nativeBindString:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(NSString *)value;

+ (void) nativeResetStatementAndClearBindings:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (void) nativeExecute:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jlong) nativeExecuteForLong:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (NSString *) nativeExecuteForString:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jint) nativeExecuteForChangedRowCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jlong) nativeExecuteForLastInsertedRowId:(NSObject *)connectionPtr statement:(NSObject *)statementPtr;

+ (jlong) nativeExecuteForCursorWindow:(NSObject *)connectionPtr statement:(NSObject *)statementPtr
                               window:(NSObject *)windowPtr startPos:(jint)startPos requiredPos:(jint)requiredPos
                               countAllRows:(jboolean)countAllRows;


+ (jint) nativeGetDbLookaside:(NSObject *)connectionPtr;

@end
