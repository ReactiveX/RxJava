//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/sqlite/SQLiteDebug.java
//

#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "android/database/sqlite/SQLiteDatabase.h"
#include "android/database/sqlite/SQLiteDebug.h"
#include "android/util/Log.h"
#include "android/util/Printer.h"

@interface AndroidDatabaseSqliteSQLiteDebug ()

- (instancetype)init;

@end

__attribute__((unused)) static void AndroidDatabaseSqliteSQLiteDebug_init(AndroidDatabaseSqliteSQLiteDebug *self);

__attribute__((unused)) static AndroidDatabaseSqliteSQLiteDebug *new_AndroidDatabaseSqliteSQLiteDebug_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static AndroidDatabaseSqliteSQLiteDebug *create_AndroidDatabaseSqliteSQLiteDebug_init();

J2OBJC_INITIALIZED_DEFN(AndroidDatabaseSqliteSQLiteDebug)

jboolean AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_LOG;
jboolean AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_STATEMENTS;
jboolean AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_TIME;

@implementation AndroidDatabaseSqliteSQLiteDebug

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  AndroidDatabaseSqliteSQLiteDebug_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (jboolean)shouldLogSlowQueryWithLong:(jlong)elapsedTimeMillis {
  return AndroidDatabaseSqliteSQLiteDebug_shouldLogSlowQueryWithLong_(elapsedTimeMillis);
}

+ (void)dumpWithAndroidUtilPrinter:(id<AndroidUtilPrinter>)printer
                 withNSStringArray:(IOSObjectArray *)args {
  AndroidDatabaseSqliteSQLiteDebug_dumpWithAndroidUtilPrinter_withNSStringArray_(printer, args);
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x2, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x19, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x9, 2, 3, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(shouldLogSlowQueryWithLong:);
  methods[2].selector = @selector(dumpWithAndroidUtilPrinter:withNSStringArray:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "DEBUG_SQL_LOG", "Z", .constantValue.asLong = 0, 0x19, -1, 4, -1, -1 },
    { "DEBUG_SQL_STATEMENTS", "Z", .constantValue.asLong = 0, 0x19, -1, 5, -1, -1 },
    { "DEBUG_SQL_TIME", "Z", .constantValue.asLong = 0, 0x19, -1, 6, -1, -1 },
    { "DEBUG_LOG_SLOW_QUERIES", "Z", .constantValue.asBOOL = AndroidDatabaseSqliteSQLiteDebug_DEBUG_LOG_SLOW_QUERIES, 0x19, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "shouldLogSlowQuery", "J", "dump", "LAndroidUtilPrinter;[LNSString;", &AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_LOG, &AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_STATEMENTS, &AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_TIME, "LAndroidDatabaseSqliteSQLiteDebug_DbStats;" };
  static const J2ObjcClassInfo _AndroidDatabaseSqliteSQLiteDebug = { "SQLiteDebug", "android.database.sqlite", ptrTable, methods, fields, 7, 0x11, 3, 4, -1, 7, -1, -1, -1 };
  return &_AndroidDatabaseSqliteSQLiteDebug;
}

+ (void)initialize {
  if (self == [AndroidDatabaseSqliteSQLiteDebug class]) {
    AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_LOG = AndroidUtilLog_isLoggableWithNSString_withInt_(@"SQLiteLog", AndroidUtilLog_VERBOSE);
    AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_STATEMENTS = AndroidUtilLog_isLoggableWithNSString_withInt_(@"SQLiteStatements", AndroidUtilLog_VERBOSE);
    AndroidDatabaseSqliteSQLiteDebug_DEBUG_SQL_TIME = AndroidUtilLog_isLoggableWithNSString_withInt_(@"SQLiteTime", AndroidUtilLog_VERBOSE);
    J2OBJC_SET_INITIALIZED(AndroidDatabaseSqliteSQLiteDebug)
  }
}

@end

void AndroidDatabaseSqliteSQLiteDebug_init(AndroidDatabaseSqliteSQLiteDebug *self) {
  NSObject_init(self);
}

AndroidDatabaseSqliteSQLiteDebug *new_AndroidDatabaseSqliteSQLiteDebug_init() {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteDebug, init)
}

AndroidDatabaseSqliteSQLiteDebug *create_AndroidDatabaseSqliteSQLiteDebug_init() {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteDebug, init)
}

jboolean AndroidDatabaseSqliteSQLiteDebug_shouldLogSlowQueryWithLong_(jlong elapsedTimeMillis) {
  AndroidDatabaseSqliteSQLiteDebug_initialize();
  jint slowQueryMillis = 10000;
  return slowQueryMillis >= 0 && elapsedTimeMillis >= slowQueryMillis;
}

void AndroidDatabaseSqliteSQLiteDebug_dumpWithAndroidUtilPrinter_withNSStringArray_(id<AndroidUtilPrinter> printer, IOSObjectArray *args) {
  AndroidDatabaseSqliteSQLiteDebug_initialize();
  jboolean verbose = false;
  {
    IOSObjectArray *a__ = args;
    NSString * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
    NSString * const *e__ = b__ + a__->size_;
    while (b__ < e__) {
      NSString *arg = *b__++;
      if ([((NSString *) nil_chk(arg)) isEqual:@"-v"]) {
        verbose = true;
      }
    }
  }
  AndroidDatabaseSqliteSQLiteDatabase_dumpAllWithAndroidUtilPrinter_withBoolean_(printer, verbose);
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseSqliteSQLiteDebug)

@implementation AndroidDatabaseSqliteSQLiteDebug_DbStats

- (instancetype)initWithNSString:(NSString *)dbName
                        withLong:(jlong)pageCount
                        withLong:(jlong)pageSize
                         withInt:(jint)lookaside
                         withInt:(jint)hits
                         withInt:(jint)misses
                         withInt:(jint)cachesize {
  AndroidDatabaseSqliteSQLiteDebug_DbStats_initWithNSString_withLong_withLong_withInt_withInt_withInt_withInt_(self, dbName, pageCount, pageSize, lookaside, hits, misses, cachesize);
  return self;
}

- (void)dealloc {
  RELEASE_(dbName_);
  RELEASE_(cache_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithNSString:withLong:withLong:withInt:withInt:withInt:withInt:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "dbName_", "LNSString;", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "pageSize_", "J", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "dbSize_", "J", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "lookaside_", "I", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "cache_", "LNSString;", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LNSString;JJIIII", "LAndroidDatabaseSqliteSQLiteDebug;" };
  static const J2ObjcClassInfo _AndroidDatabaseSqliteSQLiteDebug_DbStats = { "DbStats", "android.database.sqlite", ptrTable, methods, fields, 7, 0x9, 1, 5, 1, -1, -1, -1, -1 };
  return &_AndroidDatabaseSqliteSQLiteDebug_DbStats;
}

@end

void AndroidDatabaseSqliteSQLiteDebug_DbStats_initWithNSString_withLong_withLong_withInt_withInt_withInt_withInt_(AndroidDatabaseSqliteSQLiteDebug_DbStats *self, NSString *dbName, jlong pageCount, jlong pageSize, jint lookaside, jint hits, jint misses, jint cachesize) {
  NSObject_init(self);
  JreStrongAssign(&self->dbName_, dbName);
  self->pageSize_ = pageSize / 1024;
  self->dbSize_ = (pageCount * pageSize) / 1024;
  self->lookaside_ = lookaside;
  JreStrongAssign(&self->cache_, JreStrcat("ICICI", hits, '/', misses, '/', cachesize));
}

AndroidDatabaseSqliteSQLiteDebug_DbStats *new_AndroidDatabaseSqliteSQLiteDebug_DbStats_initWithNSString_withLong_withLong_withInt_withInt_withInt_withInt_(NSString *dbName, jlong pageCount, jlong pageSize, jint lookaside, jint hits, jint misses, jint cachesize) {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteDebug_DbStats, initWithNSString_withLong_withLong_withInt_withInt_withInt_withInt_, dbName, pageCount, pageSize, lookaside, hits, misses, cachesize)
}

AndroidDatabaseSqliteSQLiteDebug_DbStats *create_AndroidDatabaseSqliteSQLiteDebug_DbStats_initWithNSString_withLong_withLong_withInt_withInt_withInt_withInt_(NSString *dbName, jlong pageCount, jlong pageSize, jint lookaside, jint hits, jint misses, jint cachesize) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteDebug_DbStats, initWithNSString_withLong_withLong_withInt_withInt_withInt_withInt_, dbName, pageCount, pageSize, lookaside, hits, misses, cachesize)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseSqliteSQLiteDebug_DbStats)
