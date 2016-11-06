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
//  SQLiteErrors.m
//  squidb-ios
//  This file extracts portions of AOSP SQLiteCommon.cpp (https://github.com/android/platform_frameworks_base/blob/master/core/jni/android_database_SQLiteCommon.cpp)
//  The core logic/structures defined in the file have been left intact; this is just a translation to use Objective-C
//  syntax instead of C++ to make working with the j2objc tool easier.
//

#import <Foundation/Foundation.h>
#import "SQLiteErrors.h"
#import "AndroidDatabaseSqliteSQLiteException.h"
#import "AndroidDatabaseSqliteSQLiteDiskIOException.h"
#import "AndroidDatabaseSqliteSQLiteDatabaseCorruptException.h"
#import "AndroidDatabaseSqliteSQLiteConstraintException.h"
#import "AndroidDatabaseSqliteSQLiteAbortException.h"
#import "AndroidDatabaseSqliteSQLiteDoneException.h"
#import "AndroidDatabaseSqliteSQLiteFullException.h"
#import "AndroidDatabaseSqliteSQLiteMisuseException.h"
#import "AndroidDatabaseSqliteSQLiteAccessPermException.h"
#import "AndroidDatabaseSqliteSQLiteDatabaseLockedException.h"
#import "AndroidDatabaseSqliteSQLiteTableLockedException.h"
#import "AndroidDatabaseSqliteSQLiteReadOnlyDatabaseException.h"
#import "AndroidDatabaseSqliteSQLiteCantOpenDatabaseException.h"
#import "AndroidDatabaseSqliteSQLiteBlobTooBigException.h"
#import "AndroidDatabaseSqliteSQLiteBindOrColumnIndexOutOfRangeException.h"
#import "AndroidDatabaseSqliteSQLiteOutOfMemoryException.h"
#import "AndroidDatabaseSqliteSQLiteDatatypeMismatchException.h"

/* throw a SQLiteException with a message appropriate for the error in handle */
void throw_sqlite3_exception_handle(sqlite3* handle) {
    throw_sqlite3_exception_message(handle, NULL);
}

/* throw a SQLiteException with a message appropriate for the error in handle
 concatenated with the given message
 */
void throw_sqlite3_exception_message(sqlite3* handle, const char* message) {
    if (handle) {
        // get the error code and message from the SQLite connection
        // the error message may contain more information than the error code
        // because it is based on the extended error code rather than the simplified
        // error code that SQLite normally returns.
        throw_sqlite3_exception(sqlite3_extended_errcode(handle),
                                sqlite3_errmsg(handle), message);
    } else {
        // we use SQLITE_OK so that a generic SQLiteException is thrown;
        // any code not specified in the switch statement below would do.
        throw_sqlite3_exception(SQLITE_OK, "unknown error", message);
    }
}

/* throw a SQLiteException for a given error code
 * should only be used when the database connection is not available because the
 * error information will not be quite as rich */
void throw_sqlite3_exception_errcode(int errcode, const char* message) {
    throw_sqlite3_exception(errcode, "unknown error", message);
}

/* throw a SQLiteException for a given error code, sqlite3message, and
 user message
 */
void throw_sqlite3_exception(int errcode, const char* sqlite3Message, const char* message) {

    int errcodeMask = errcode & 0xff; /* mask off extended error code */
    if (errcodeMask == SQLITE_DONE) {
        sqlite3Message = NULL; // SQLite error message is irrelevant in this case
    }

    NSString *exceptionMessage;
    if (sqlite3Message) {
        char *zFullmsg = sqlite3_mprintf(
             "%s (code %d)%s%s", sqlite3Message, errcode,
             (message ? ": " : ""), (message ? message : "")
         );

        exceptionMessage = [NSString stringWithUTF8String:zFullmsg];
        sqlite3_free(zFullmsg);
    }
    else
    {
        if(message)
        {
            exceptionMessage = [NSString stringWithUTF8String:message];
        }
        else
        {
            exceptionMessage = @"(null)";
        }
    }
    switch (errcodeMask) {
        case SQLITE_IOERR:
            @throw [[AndroidDatabaseSqliteSQLiteDiskIOException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_CORRUPT:
        case SQLITE_NOTADB: // treat "unsupported file format" error as corruption also
            @throw [[AndroidDatabaseSqliteSQLiteDatabaseCorruptException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_CONSTRAINT:
            @throw [[AndroidDatabaseSqliteSQLiteConstraintException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_ABORT:
            @throw [[AndroidDatabaseSqliteSQLiteAbortException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_DONE:
            @throw [[AndroidDatabaseSqliteSQLiteDoneException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_FULL:
            @throw [[AndroidDatabaseSqliteSQLiteFullException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_MISUSE:
            @throw [[AndroidDatabaseSqliteSQLiteMisuseException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_PERM:
            @throw [[AndroidDatabaseSqliteSQLiteAccessPermException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_BUSY:
            @throw [[AndroidDatabaseSqliteSQLiteDatabaseLockedException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_LOCKED:
            @throw [[AndroidDatabaseSqliteSQLiteTableLockedException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_READONLY:
            @throw [[AndroidDatabaseSqliteSQLiteReadOnlyDatabaseException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_CANTOPEN:
            @throw [[AndroidDatabaseSqliteSQLiteCantOpenDatabaseException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_TOOBIG:
            @throw [[AndroidDatabaseSqliteSQLiteBlobTooBigException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_RANGE:
            @throw [[AndroidDatabaseSqliteSQLiteBindOrColumnIndexOutOfRangeException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_NOMEM:
            @throw [[AndroidDatabaseSqliteSQLiteOutOfMemoryException alloc] initWithNSString:exceptionMessage];
            break;
        case SQLITE_MISMATCH:
            @throw [[AndroidDatabaseSqliteSQLiteDatatypeMismatchException alloc] initWithNSString:exceptionMessage];
            break;
//        case SQLITE_INTERRUPT:
//            @throw [[AndroidDatabaseSqliteOperationCanceledException alloc] initWithNSString:exceptionMessage];
//            break;
        default:
            @throw [[AndroidDatabaseSqliteSQLiteException alloc] initWithNSString:exceptionMessage];
            break;
    }
}
