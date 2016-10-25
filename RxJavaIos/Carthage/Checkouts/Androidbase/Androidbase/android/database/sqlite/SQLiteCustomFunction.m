//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/sqlite/SQLiteCustomFunction.java
//

#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "android/database/sqlite/SQLiteCustomFunction.h"
#include "android/database/sqlite/SQLiteDatabase.h"
#include "java/lang/IllegalArgumentException.h"

@interface AndroidDatabaseSqliteSQLiteCustomFunction ()

- (void)dispatchCallbackWithNSStringArray:(IOSObjectArray *)args;

@end

@implementation AndroidDatabaseSqliteSQLiteCustomFunction

- (instancetype)initWithNSString:(NSString *)name
                         withInt:(jint)numArgs
withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction:(id<AndroidDatabaseSqliteSQLiteDatabase_CustomFunction>)callback {
  AndroidDatabaseSqliteSQLiteCustomFunction_initWithNSString_withInt_withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction_(self, name, numArgs, callback);
  return self;
}

- (void)dispatchCallbackWithNSStringArray:(IOSObjectArray *)args {
  [((id<AndroidDatabaseSqliteSQLiteDatabase_CustomFunction>) nil_chk(callback_)) callbackWithNSStringArray:args];
}

- (void)dealloc {
  RELEASE_(name_);
  RELEASE_(callback_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x2, 1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithNSString:withInt:withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction:);
  methods[1].selector = @selector(dispatchCallbackWithNSStringArray:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "name_", "LNSString;", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "numArgs_", "I", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "callback_", "LAndroidDatabaseSqliteSQLiteDatabase_CustomFunction;", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LNSString;ILAndroidDatabaseSqliteSQLiteDatabase_CustomFunction;", "dispatchCallback", "[LNSString;" };
  static const J2ObjcClassInfo _AndroidDatabaseSqliteSQLiteCustomFunction = { "SQLiteCustomFunction", "android.database.sqlite", ptrTable, methods, fields, 7, 0x11, 2, 3, -1, -1, -1, -1, -1 };
  return &_AndroidDatabaseSqliteSQLiteCustomFunction;
}

@end

void AndroidDatabaseSqliteSQLiteCustomFunction_initWithNSString_withInt_withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction_(AndroidDatabaseSqliteSQLiteCustomFunction *self, NSString *name, jint numArgs, id<AndroidDatabaseSqliteSQLiteDatabase_CustomFunction> callback) {
  NSObject_init(self);
  if (name == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"name must not be null.");
  }
  JreStrongAssign(&self->name_, name);
  self->numArgs_ = numArgs;
  JreStrongAssign(&self->callback_, callback);
}

AndroidDatabaseSqliteSQLiteCustomFunction *new_AndroidDatabaseSqliteSQLiteCustomFunction_initWithNSString_withInt_withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction_(NSString *name, jint numArgs, id<AndroidDatabaseSqliteSQLiteDatabase_CustomFunction> callback) {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteCustomFunction, initWithNSString_withInt_withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction_, name, numArgs, callback)
}

AndroidDatabaseSqliteSQLiteCustomFunction *create_AndroidDatabaseSqliteSQLiteCustomFunction_initWithNSString_withInt_withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction_(NSString *name, jint numArgs, id<AndroidDatabaseSqliteSQLiteDatabase_CustomFunction> callback) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteCustomFunction, initWithNSString_withInt_withAndroidDatabaseSqliteSQLiteDatabase_CustomFunction_, name, numArgs, callback)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseSqliteSQLiteCustomFunction)
