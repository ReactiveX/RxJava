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
//  SQLiteConnectionNative.m
//  squidb-ios
//  This file is a fork/port of AOSP SQLiteConnection.cpp (https://github.com/android/platform_frameworks_base/blob/master/core/jni/android_database_SQLiteConnection.cpp)
//  The core logic/structures defined in the file have been left intact; this is just a translation to use Objective-C
//  syntax instead of C++ to make working with the j2objc tool easier.
//

#import <Foundation/Foundation.h>
#import "SQLiteConnectionNative.h"
#import "SQLitePreparedStatement.h"
#import "CursorWindowNative.h"
#import "NSString+JavaString.h"

@implementation SQLiteConnectionNative

@synthesize db;
@synthesize path;
@synthesize label;
@synthesize openFlags;

static const int BUSY_TIMEOUT_MS = 2500;
static const int SQLITE_SOFT_HEAP_LIMIT = (4 * 1024 * 1024);

enum {
    OPEN_READWRITE          = 0x00000000,
    OPEN_READONLY           = 0x00000001,
    OPEN_READ_MASK          = 0x00000001,
    NO_LOCALIZED_COLLATORS  = 0x00000010,
    CREATE_IF_NECESSARY     = 0x10000000,
};

enum CopyRowResult {
    CPR_OK,
    CPR_FULL,
    CPR_ERROR,
};

static int coll_localized(
 void *not_used,
 int nKey1, const void *pKey1,
 int nKey2, const void *pKey2
){
    int rc, n;
    n = nKey1<nKey2 ? nKey1 : nKey2;
    rc = memcmp(pKey1, pKey2, n);
    if( rc==0 ){
        rc = nKey1 - nKey2;
    }
    return rc;
}

// Called each time a statement begins execution, when tracing is enabled.
static void sqliteTraceCallback(void *data, const char *sql) {
//    SQLiteConnection* connection = (__bridge SQLiteConnection *)(data);
//    ALOG(LOG_VERBOSE, SQLITE_TRACE_TAG, "%s: \"%s\"\n",
//         connection->label.c_str(), sql);
}


// Called each time a statement finishes execution, when profiling is enabled.
static void sqliteProfileCallback(void *data, const char *sql, sqlite3_uint64 tm) {
//    SQLiteConnection* connection = (__bridge SQLiteConnection *)(data);
//    ALOG(LOG_VERBOSE, SQLITE_PROFILE_TAG, "%s: \"%s\" took %0.3f ms\n",
//         connection->label.c_str(), sql, tm * 0.000001f);
}

+ (SQLiteConnectionNative *) nativeOpen:(NSString *)pathStr openFlags:(jint) openFlags labelStr:(NSString *)labelStr enableTrace:(jboolean)enableTrace enableProfile:(jboolean)enableProfile {
    int sqliteFlags;
    if (openFlags & CREATE_IF_NECESSARY) {
        sqliteFlags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
    } else if (openFlags & OPEN_READONLY) {
        sqliteFlags = SQLITE_OPEN_READONLY;
    } else {
        sqliteFlags = SQLITE_OPEN_READWRITE;
    }

    const char* pathChars = [pathStr UTF8String];

    sqlite3* db;
    int err = sqlite3_open_v2(pathChars, &db, sqliteFlags, NULL);

    if (err != SQLITE_OK) {
        throw_sqlite3_exception_errcode(err, "Could not open database");
        return 0;
    }
    sqlite3_soft_heap_limit(SQLITE_SOFT_HEAP_LIMIT);
    
    err = sqlite3_create_collation(db, "localized", SQLITE_UTF8, 0, coll_localized);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_errcode(err, "Could not register collation");
        sqlite3_close(db);
        return 0;
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(db, NULL)) {
        throw_sqlite3_exception_message(db, "Could not open the database in read/write mode.");
        sqlite3_close(db);
        return 0;
    }

    // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
    err = sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_message(db, "Could not set busy timeout");
        sqlite3_close(db);
        return 0;
    }

    // Register custom Android functions.
#if 0
    err = register_android_functions(db, UTF16_STORAGE);
    if (err) {
        throw_sqlite3_exception_message(db, "Could not register Android SQL functions.");
        sqlite3_close(db);
        return 0;
    }
#endif

    // Create wrapper object.
    SQLiteConnectionNative *connection = [[SQLiteConnectionNative alloc] init]; //new SQLiteConnection(db, openFlags, path, label);
    connection.path = pathStr;
    connection.label = labelStr;
    connection.openFlags = openFlags;
    connection.db = db;

    // Enable tracing and profiling if requested.
    if (enableTrace) {
        sqlite3_trace(db, &sqliteTraceCallback, (__bridge void *)(connection));
    }
    if (enableProfile) {
        sqlite3_profile(db, &sqliteProfileCallback, (__bridge void *)(connection));
    }

//    ALOGV("Opened connection %p with label '%s'", db, label.c_str());
    return connection;
}

+ (void) nativeClose:(NSObject *)connectionPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)connectionPtr;
    if (connection) {
        int err = sqlite3_close(connection.db);
        if (err != SQLITE_OK) {
//            ALOGE("sqlite3_close(%p) failed: %d", connection->db, err);
            throw_sqlite3_exception_message(connection.db, "Count not close db.");
            return;
        }
    }
}

/*+ (void) nativeRegisterLocalizedCollators:(NSObject *)connectionPtr withLocaleStr:(NSString *)localeStr {
    IOSByteArray *sql = [localeStr getBytesWithEncoding:NSUTF16StringEncoding];
    uint32_t sqlLength = [sql length];
    SQLiteConnectionNative* connection = (SQLiteConnectionNative *)(connectionPtr);

   int err = register_localized_collators(connection->db, [sql buffer], UTF16_STORAGE);
       if (err != SQLITE_OK) {
//            ALOGE("sqlite3_close(%p) failed: %d", connection->db, err);
           throw_sqlite3_exception_message(connection.db, "failed nativeRegisterLocalizedCollators");
           return;
       }
   }
}*/

+ (NSObject *) nativePrepareStatement:(NSObject *)connectionPtr withSql:(NSString *)sqlString {
    SQLiteConnectionNative* connection = (SQLiteConnectionNative *)(connectionPtr);

    IOSByteArray *sql = [sqlString getBytesWithEncoding:NSUTF16StringEncoding];
    uint32_t sqlLength = [sql length];
    sqlite3_stmt* statement;
    int err = sqlite3_prepare16_v2(connection->db, [sql buffer], sqlLength, &statement, NULL);

    if (err != SQLITE_OK) {
        // Error messages like 'near ")": syntax error' are not
        // always helpful enough, so construct an error string that
        // includes the query itself.
        const char *query = [sqlString UTF8String];
        char *message = (char*) malloc(strlen(query) + 50);
        if (message) {
            strcpy(message, ", while compiling: "); // less than 50 chars
            strcat(message, query);
        }
        throw_sqlite3_exception_message(connection.db, message);
        free(message);
        return nil;
    }

//    ALOGV("Prepared statement %p on connection %p", statement, connection->db);
    return [[SQLitePreparedStatement alloc] initWithStatement:statement];
}

+ (void) nativeFinalizeStatement:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
//    SQLiteConnection* connection = (SQLiteConnection *)(connectionPtr);
    SQLitePreparedStatement *preparedStatement = (SQLitePreparedStatement *)(statementPtr);
    sqlite3_stmt* statement = preparedStatement.statement;

    // We ignore the result of sqlite3_finalize because it is really telling us about
    // whether any errors occurred while executing the statement.  The statement itself
    // is always finalized regardless.
//    ALOGV("Finalized statement %p on connection %p", statement, connection->db);
    sqlite3_finalize(statement);
    preparedStatement.statement = nil;

}

+ (jint) nativeGetParameterCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
//    SQLiteConnection* connection = (SQLiteConnection *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    return sqlite3_bind_parameter_count(statement.statement);
}

+ (jboolean) nativeIsReadOnly:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
//    SQLiteConnection *connection = (SQLiteConnection* )(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    return sqlite3_stmt_readonly(statement.statement) != 0;
}

+ (jint) nativeGetColumnCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
//    SQLiteConnection *connection = (SQLiteConnection *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    return sqlite3_column_count(statement.statement);
}

+ (NSString *) nativeGetColumnName:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index {
//    SQLiteConnection *connection = (SQLiteConnection *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    const jchar* name = (const jchar *)(sqlite3_column_name16(statement.statement, index));
    if (name) {
        size_t length = 0;
        while (name[length]) {
            length += 1;
        }
        IOSCharArray *chars = [IOSCharArray newArrayWithChars:name count:length];
        NSString* asdf = [NSString stringWithCharacters:chars];
        [chars release];
        return asdf;
    }
    return nil;
}

+ (void) nativeBindNull:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);
    int err = sqlite3_bind_null(statement.statement, index);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_handle(connection.db);
    }
}

+ (void) nativeBindLong:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(jlong)value {
        SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);
    int err = sqlite3_bind_int64(statement.statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_handle(connection.db);
    }
}

+ (void) nativeBindDouble:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(jdouble)value {
        SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);
    int err = sqlite3_bind_double(statement.statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_handle(connection.db);
    }
}

+ (void) nativeBindBlob:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(IOSByteArray *)value {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    int valueLength = [value length];
    void * buffer = [value buffer];
//    jbyte* value = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(valueArray, NULL));
    int err = sqlite3_bind_blob(statement.statement, index, buffer, valueLength, SQLITE_TRANSIENT);
//    env->ReleasePrimitiveArrayCritical(valueArray, value, JNI_ABORT);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_handle(connection.db);
    }
}

+ (void) nativeBindString:(NSObject *)connectionPtr statement:(NSObject *)statementPtr index:(jint)index value:(NSString *)value {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    //TODO: Figure this out with UTF16...
    const IOSByteArray *bytes = [value getBytesWithEncoding:NSUTF8StringEncoding];
    int err = sqlite3_bind_text(statement.statement, index, [bytes buffer], [bytes length],
                                  SQLITE_TRANSIENT);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_handle(connection.db);
    }
}

+ (void) nativeResetStatementAndClearBindings:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    int err = sqlite3_reset(statement.statement);
    if (err == SQLITE_OK) {
        err = sqlite3_clear_bindings(statement.statement);
    }
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_handle(connection.db);
    }
}

+ (jint) executeNonQuery:(SQLiteConnectionNative *)connection statement:(SQLitePreparedStatement *)statement {
    int err = sqlite3_step(statement.statement);
    if (err == SQLITE_ROW) {
        throw_sqlite3_exception_message(NULL,
                                "Queries can be performed using SQLiteDatabase query or rawQuery methods only.");
    } else if (err != SQLITE_DONE) {
        throw_sqlite3_exception_handle(connection.db);
    }
    return err;
}

+ (void) nativeExecute:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    [SQLiteConnectionNative executeNonQuery:connection statement:statement];
}

+ (jint) executeOneRowQuery:(SQLiteConnectionNative *)connection statement:(SQLitePreparedStatement *)statement {
    jint err = sqlite3_step(statement.statement);
    if (err != SQLITE_ROW) {
        throw_sqlite3_exception_handle(connection.db);
    }
    return err;
}

+ (jlong) nativeExecuteForLong:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    int err = [SQLiteConnectionNative executeOneRowQuery:connection statement:statement];
    if (err == SQLITE_ROW && sqlite3_column_count(statement.statement) >= 1) {
        return sqlite3_column_int64(statement.statement, 0);
    }
    return -1;
}

+ (NSString *) nativeExecuteForString:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    int err = [SQLiteConnectionNative executeOneRowQuery:connection statement:statement];
    if (err == SQLITE_ROW && sqlite3_column_count(statement.statement) >= 1) {
        const jchar* text = (const jchar *)(sqlite3_column_text16(statement.statement, 0));
        if (text) {
            size_t length = sqlite3_column_bytes16(statement.statement, 0) / sizeof(jchar);
            IOSCharArray *chars = [IOSCharArray newArrayWithChars:text count:length];
            NSString* asdf = [NSString stringWithCharacters:chars];
            [chars release];
            return asdf;
        }
    }
    return NULL;
}

+ (jint) nativeExecuteForChangedRowCount:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    int err = [SQLiteConnectionNative executeNonQuery:connection statement:statement];
    return err == SQLITE_DONE ? sqlite3_changes(connection.db) : -1;
}

+ (jlong) nativeExecuteForLastInsertedRowId:(NSObject *)connectionPtr statement:(NSObject *)statementPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);

    int err = [SQLiteConnectionNative executeNonQuery:connection statement:statement];
    return err == SQLITE_DONE && sqlite3_changes(connection.db) > 0
    ? sqlite3_last_insert_rowid(connection->db) : -1;
}

static enum CopyRowResult copyRow(CursorWindowNative* window,
                             sqlite3_stmt* statement, int numColumns, int startPos, int addedRows) {
    // Allocate a new field directory for the row.
    status_t status = [window allocRow];
    if (status) {
//        LOG_WINDOW("Failed allocating fieldDir at startPos %d row %d, error=%d",
//                   startPos, addedRows, status);
        return CPR_FULL;
    }

    // Pack the row into the window.
    enum CopyRowResult result = CPR_OK;
    for (int i = 0; i < numColumns; i++) {
        int type = sqlite3_column_type(statement, i);
        if (type == SQLITE_TEXT) {
            // TEXT data
            const char* text = (const char*)(sqlite3_column_text(statement, i));
            // SQLite does not include the NULL terminator in size, but does
            // ensure all strings are NULL terminated, so increase size by
            // one to make sure we store the terminator.
            uint32_t sizeIncludingNull = sqlite3_column_bytes(statement, i) + 1;
            status = [window putStringInRow:addedRows column:i value:text size:sizeIncludingNull];
            if (status) {
//                LOG_WINDOW("Failed allocating %u bytes for text at %d,%d, error=%d",
//                           sizeIncludingNull, startPos + addedRows, i, status);
                result = CPR_FULL;
                break;
            }
//            LOG_WINDOW("%d,%d is TEXT with %u bytes",
//                       startPos + addedRows, i, sizeIncludingNull);
        } else if (type == SQLITE_INTEGER) {
            // INTEGER data
            int64_t value = sqlite3_column_int64(statement, i);
            status = [window putLongInRow:addedRows column:i value:value];
            if (status) {
//                LOG_WINDOW("Failed allocating space for a long in column %d, error=%d",
//                           i, status);
                result = CPR_FULL;
                break;
            }
//            LOG_WINDOW("%d,%d is INTEGER 0x%016llx", startPos + addedRows, i, value);
        } else if (type == SQLITE_FLOAT) {
            // FLOAT data
            double value = sqlite3_column_double(statement, i);
            status = [window putDoubleInRow:addedRows column:i value:value];
            if (status) {
//                LOG_WINDOW("Failed allocating space for a double in column %d, error=%d",
//                           i, status);
                result = CPR_FULL;
                break;
            }
//            LOG_WINDOW("%d,%d is FLOAT %lf", startPos + addedRows, i, value);
        } else if (type == SQLITE_BLOB) {
            // BLOB data
            const void* blob = sqlite3_column_blob(statement, i);
            uint32_t size = sqlite3_column_bytes(statement, i);
            status = [window putBlobInRow:addedRows column:i value:blob size:size];
            if (status) {
//                LOG_WINDOW("Failed allocating %u bytes for blob at %d,%d, error=%d",
//                           size, startPos + addedRows, i, status);
                result = CPR_FULL;
                break;
            }
//            LOG_WINDOW("%d,%d is Blob with %u bytes",
//                       startPos + addedRows, i, size);
        } else if (type == SQLITE_NULL) {
            // NULL field
            status = [window putNullInRow:addedRows column:i];
            if (status) {
//                LOG_WINDOW("Failed allocating space for a null in column %d, error=%d",
//                           i, status);
                result = CPR_FULL;
                break;
            }

//            LOG_WINDOW("%d,%d is NULL", startPos + addedRows, i);
        } else {
            // Unknown data
//            ALOGE("Unknown column type when filling database window");
            throw_sqlite3_exception_message(NULL, "Unknown column type when filling window");
            result = CPR_ERROR;
            break;
        }
    }

    // Free the last row if if was not successfully copied.
    if (result != CPR_OK) {
        [window freeLastRow];
    }
    return result;
}

+ (jlong) nativeExecuteForCursorWindow:(NSObject *)connectionPtr statement:(NSObject *)statementPtr window:(NSObject *)windowPtr startPos:(jint)startPos requiredPos:(jint)requiredPos countAllRows:(jboolean)countAllRows {

    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);
    SQLitePreparedStatement *statement = (SQLitePreparedStatement *)(statementPtr);
    CursorWindowNative *window = (CursorWindowNative *)(windowPtr);

    status_t status = [window clear];
    if (status) {
        NSString *message = [NSString stringWithFormat:@"Failed to clear the cursor window, status=%d", status];
        throw_sqlite3_exception_message(connection.db, [message UTF8String]);
        return 0;
    }

    int numColumns = sqlite3_column_count(statement.statement);
    status = [window setNumColumns:numColumns];
    if (status) {
        NSString *message = [NSString stringWithFormat:
                             @"Failed to set the cursor window column count to %d, status=%d", numColumns, status];
        throw_sqlite3_exception_message(connection.db, [message UTF8String]);
        return 0;
    }

    int retryCount = 0;
    int totalRows = 0;
    int addedRows = 0;
    bool windowFull = false;
    bool gotException = false;
    while (!gotException && (!windowFull || countAllRows)) {
        int err = sqlite3_step(statement.statement);
        if (err == SQLITE_ROW) {
//            LOG_WINDOW("Stepped statement %p to row %d", statement, totalRows);
            retryCount = 0;
            totalRows += 1;

            // Skip the row if the window is full or we haven't reached the start position yet.
            if (startPos >= totalRows || windowFull) {
                continue;
            }

            enum CopyRowResult cpr = copyRow(window, statement.statement, numColumns, startPos, addedRows);
            if (cpr == CPR_FULL && addedRows && startPos + addedRows <= requiredPos) {
                // We filled the window before we got to the one row that we really wanted.
                // Clear the window and start filling it again from here.
                // TODO: Would be nicer if we could progressively replace earlier rows.
                [window clear];
                [window setNumColumns:numColumns];
                startPos += addedRows;
                addedRows = 0;
                cpr = copyRow(window, statement.statement, numColumns, startPos, addedRows);
            }

            if (cpr == CPR_OK) {
                addedRows += 1;
            } else if (cpr == CPR_FULL) {
                windowFull = true;
            } else {
                gotException = true;
            }
        } else if (err == SQLITE_DONE) {
            // All rows processed, bail
//            LOG_WINDOW("Processed all rows");
            break;
        } else if (err == SQLITE_LOCKED || err == SQLITE_BUSY) {
            // The table is locked, retry
//            LOG_WINDOW("Database locked, retrying");
            if (retryCount > 50) {
//                ALOGE("Bailing on database busy retry");
                throw_sqlite3_exception_message(connection.db, "retrycount exceeded");
                gotException = true;
            } else {
                // Sleep to give the thread holding the lock a chance to finish
                usleep(1000);
                retryCount++;
            }
        } else {
            throw_sqlite3_exception_handle(connection.db);
            gotException = true;
        }
    }

//    LOG_WINDOW("Resetting statement %p after fetching %d rows and adding %d rows"
//               "to the window in %d bytes",
//               statement, totalRows, addedRows, window->size() - window->freeSpace());
    sqlite3_reset(statement.statement);

    // Report the total number of rows on request.
    if (startPos > totalRows) {
//        ALOGE("startPos %d > actual rows %d", startPos, totalRows);
    }
    jlong result = (jlong)(startPos) << 32 | (jlong)(totalRows);
    return result;
}

+ (jint) nativeGetDbLookaside:(NSObject *)connectionPtr {
    SQLiteConnectionNative *connection = (SQLiteConnectionNative *)(connectionPtr);

    int cur = -1;
    int unused;
    sqlite3_db_status(connection.db, SQLITE_DBSTATUS_LOOKASIDE_USED, &cur, &unused, 0);
    return cur;
}

- (void)dealloc {
[path release];
[label release];
[super dealloc];
}


@end
