//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/sqlite/SQLiteMisuseException.java
//

#include "AndroidDatabaseSqliteSQLiteException.h"
#include "AndroidDatabaseSqliteSQLiteMisuseException.h"
#include "J2ObjC_source.h"

@implementation AndroidDatabaseSqliteSQLiteMisuseException

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  AndroidDatabaseSqliteSQLiteMisuseException_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (instancetype)initWithNSString:(NSString *)error {
  AndroidDatabaseSqliteSQLiteMisuseException_initWithNSString_(self, error);
  return self;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(initWithNSString:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LNSString;" };
  static const J2ObjcClassInfo _AndroidDatabaseSqliteSQLiteMisuseException = { "SQLiteMisuseException", "android.database.sqlite", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_AndroidDatabaseSqliteSQLiteMisuseException;
}

@end

void AndroidDatabaseSqliteSQLiteMisuseException_init(AndroidDatabaseSqliteSQLiteMisuseException *self) {
  AndroidDatabaseSqliteSQLiteException_init(self);
}

AndroidDatabaseSqliteSQLiteMisuseException *new_AndroidDatabaseSqliteSQLiteMisuseException_init() {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteMisuseException, init)
}

AndroidDatabaseSqliteSQLiteMisuseException *create_AndroidDatabaseSqliteSQLiteMisuseException_init() {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteMisuseException, init)
}

void AndroidDatabaseSqliteSQLiteMisuseException_initWithNSString_(AndroidDatabaseSqliteSQLiteMisuseException *self, NSString *error) {
  AndroidDatabaseSqliteSQLiteException_initWithNSString_(self, error);
}

AndroidDatabaseSqliteSQLiteMisuseException *new_AndroidDatabaseSqliteSQLiteMisuseException_initWithNSString_(NSString *error) {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteMisuseException, initWithNSString_, error)
}

AndroidDatabaseSqliteSQLiteMisuseException *create_AndroidDatabaseSqliteSQLiteMisuseException_initWithNSString_(NSString *error) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteMisuseException, initWithNSString_, error)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseSqliteSQLiteMisuseException)
