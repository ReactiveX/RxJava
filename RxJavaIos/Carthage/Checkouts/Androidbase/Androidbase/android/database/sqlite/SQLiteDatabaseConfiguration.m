//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/sqlite/SQLiteDatabaseConfiguration.java
//

#include "J2ObjC_source.h"
#include "android/database/sqlite/SQLiteDatabaseConfiguration.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/util/ArrayList.h"
#include "java/util/Locale.h"
#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"

@interface AndroidDatabaseSqliteSQLiteDatabaseConfiguration ()

+ (NSString *)stripPathForLogsWithNSString:(NSString *)path;

@end

inline JavaUtilRegexPattern *AndroidDatabaseSqliteSQLiteDatabaseConfiguration_get_EMAIL_IN_DB_PATTERN();
static JavaUtilRegexPattern *AndroidDatabaseSqliteSQLiteDatabaseConfiguration_EMAIL_IN_DB_PATTERN;
J2OBJC_STATIC_FIELD_OBJ_FINAL(AndroidDatabaseSqliteSQLiteDatabaseConfiguration, EMAIL_IN_DB_PATTERN, JavaUtilRegexPattern *)

__attribute__((unused)) static NSString *AndroidDatabaseSqliteSQLiteDatabaseConfiguration_stripPathForLogsWithNSString_(NSString *path);

J2OBJC_INITIALIZED_DEFN(AndroidDatabaseSqliteSQLiteDatabaseConfiguration)

NSString *AndroidDatabaseSqliteSQLiteDatabaseConfiguration_MEMORY_DB_PATH = @":memory:";

@implementation AndroidDatabaseSqliteSQLiteDatabaseConfiguration

- (instancetype)initWithNSString:(NSString *)path
                         withInt:(jint)openFlags {
  AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithNSString_withInt_(self, path, openFlags);
  return self;
}

- (instancetype)initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration:(AndroidDatabaseSqliteSQLiteDatabaseConfiguration *)other {
  AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration_(self, other);
  return self;
}

- (void)updateParametersFromWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration:(AndroidDatabaseSqliteSQLiteDatabaseConfiguration *)other {
  if (other == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"other must not be null.");
  }
  if (![((NSString *) nil_chk(path_)) isEqual:other->path_]) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"other configuration must refer to the same database.");
  }
  openFlags_ = other->openFlags_;
  maxSqlCacheSize_ = other->maxSqlCacheSize_;
  JreStrongAssign(&locale_, other->locale_);
  foreignKeyConstraintsEnabled_ = other->foreignKeyConstraintsEnabled_;
  [((JavaUtilArrayList *) nil_chk(customFunctions_)) clear];
  [customFunctions_ addAllWithJavaUtilCollection:other->customFunctions_];
}

- (jboolean)isInMemoryDb {
  return [((NSString *) nil_chk(path_)) equalsIgnoreCase:AndroidDatabaseSqliteSQLiteDatabaseConfiguration_MEMORY_DB_PATH];
}

+ (NSString *)stripPathForLogsWithNSString:(NSString *)path {
  return AndroidDatabaseSqliteSQLiteDatabaseConfiguration_stripPathForLogsWithNSString_(path);
}

- (void)dealloc {
  RELEASE_(path_);
  RELEASE_(label_);
  RELEASE_(locale_);
  RELEASE_(customFunctions_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0xa, 3, 4, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithNSString:withInt:);
  methods[1].selector = @selector(initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration:);
  methods[2].selector = @selector(updateParametersFromWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration:);
  methods[3].selector = @selector(isInMemoryDb);
  methods[4].selector = @selector(stripPathForLogsWithNSString:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "EMAIL_IN_DB_PATTERN", "LJavaUtilRegexPattern;", .constantValue.asLong = 0, 0x1a, -1, 5, -1, -1 },
    { "MEMORY_DB_PATH", "LNSString;", .constantValue.asLong = 0, 0x19, -1, 6, -1, -1 },
    { "path_", "LNSString;", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "label_", "LNSString;", .constantValue.asLong = 0, 0x11, -1, -1, -1, -1 },
    { "openFlags_", "I", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "maxSqlCacheSize_", "I", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "locale_", "LJavaUtilLocale;", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "foreignKeyConstraintsEnabled_", "Z", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
    { "customFunctions_", "LJavaUtilArrayList;", .constantValue.asLong = 0, 0x11, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "LNSString;I", "LAndroidDatabaseSqliteSQLiteDatabaseConfiguration;", "updateParametersFrom", "stripPathForLogs", "LNSString;", &AndroidDatabaseSqliteSQLiteDatabaseConfiguration_EMAIL_IN_DB_PATTERN, &AndroidDatabaseSqliteSQLiteDatabaseConfiguration_MEMORY_DB_PATH, "Ljava/util/ArrayList<Landroid/database/sqlite/SQLiteCustomFunction;>;" };
  static const J2ObjcClassInfo _AndroidDatabaseSqliteSQLiteDatabaseConfiguration = { "SQLiteDatabaseConfiguration", "android.database.sqlite", ptrTable, methods, fields, 7, 0x11, 5, 9, -1, -1, -1, -1, -1 };
  return &_AndroidDatabaseSqliteSQLiteDatabaseConfiguration;
}

+ (void)initialize {
  if (self == [AndroidDatabaseSqliteSQLiteDatabaseConfiguration class]) {
    JreStrongAssign(&AndroidDatabaseSqliteSQLiteDatabaseConfiguration_EMAIL_IN_DB_PATTERN, JavaUtilRegexPattern_compileWithNSString_(@"[\\w\\.\\-]+@[\\w\\.\\-]+"));
    J2OBJC_SET_INITIALIZED(AndroidDatabaseSqliteSQLiteDatabaseConfiguration)
  }
}

@end

void AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithNSString_withInt_(AndroidDatabaseSqliteSQLiteDatabaseConfiguration *self, NSString *path, jint openFlags) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->customFunctions_, new_JavaUtilArrayList_init());
  if (path == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"path must not be null.");
  }
  JreStrongAssign(&self->path_, path);
  JreStrongAssign(&self->label_, AndroidDatabaseSqliteSQLiteDatabaseConfiguration_stripPathForLogsWithNSString_(path));
  self->openFlags_ = openFlags;
  self->maxSqlCacheSize_ = 25;
  JreStrongAssign(&self->locale_, JavaUtilLocale_getDefault());
}

AndroidDatabaseSqliteSQLiteDatabaseConfiguration *new_AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithNSString_withInt_(NSString *path, jint openFlags) {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteDatabaseConfiguration, initWithNSString_withInt_, path, openFlags)
}

AndroidDatabaseSqliteSQLiteDatabaseConfiguration *create_AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithNSString_withInt_(NSString *path, jint openFlags) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteDatabaseConfiguration, initWithNSString_withInt_, path, openFlags)
}

void AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration_(AndroidDatabaseSqliteSQLiteDatabaseConfiguration *self, AndroidDatabaseSqliteSQLiteDatabaseConfiguration *other) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->customFunctions_, new_JavaUtilArrayList_init());
  if (other == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"other must not be null.");
  }
  JreStrongAssign(&self->path_, other->path_);
  JreStrongAssign(&self->label_, other->label_);
  [self updateParametersFromWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration:other];
}

AndroidDatabaseSqliteSQLiteDatabaseConfiguration *new_AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration_(AndroidDatabaseSqliteSQLiteDatabaseConfiguration *other) {
  J2OBJC_NEW_IMPL(AndroidDatabaseSqliteSQLiteDatabaseConfiguration, initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration_, other)
}

AndroidDatabaseSqliteSQLiteDatabaseConfiguration *create_AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration_(AndroidDatabaseSqliteSQLiteDatabaseConfiguration *other) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseSqliteSQLiteDatabaseConfiguration, initWithAndroidDatabaseSqliteSQLiteDatabaseConfiguration_, other)
}

NSString *AndroidDatabaseSqliteSQLiteDatabaseConfiguration_stripPathForLogsWithNSString_(NSString *path) {
  AndroidDatabaseSqliteSQLiteDatabaseConfiguration_initialize();
  if ([((NSString *) nil_chk(path)) indexOf:'@'] == -1) {
    return path;
  }
  return [((JavaUtilRegexMatcher *) nil_chk([((JavaUtilRegexPattern *) nil_chk(AndroidDatabaseSqliteSQLiteDatabaseConfiguration_EMAIL_IN_DB_PATTERN)) matcherWithJavaLangCharSequence:path])) replaceAllWithNSString:@"XX@YY"];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseSqliteSQLiteDatabaseConfiguration)
