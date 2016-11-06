//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/CursorIndexOutOfBoundsException.java
//

#include "AndroidDatabaseCursorIndexOutOfBoundsException.h"
#include "J2ObjC_source.h"
#include "java/lang/IndexOutOfBoundsException.h"

@implementation AndroidDatabaseCursorIndexOutOfBoundsException

- (instancetype)initWithInt:(jint)index
                    withInt:(jint)size {
  AndroidDatabaseCursorIndexOutOfBoundsException_initWithInt_withInt_(self, index, size);
  return self;
}

- (instancetype)initWithNSString:(NSString *)message {
  AndroidDatabaseCursorIndexOutOfBoundsException_initWithNSString_(self, message);
  return self;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithInt:withInt:);
  methods[1].selector = @selector(initWithNSString:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "II", "LNSString;" };
  static const J2ObjcClassInfo _AndroidDatabaseCursorIndexOutOfBoundsException = { "CursorIndexOutOfBoundsException", "android.database", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_AndroidDatabaseCursorIndexOutOfBoundsException;
}

@end

void AndroidDatabaseCursorIndexOutOfBoundsException_initWithInt_withInt_(AndroidDatabaseCursorIndexOutOfBoundsException *self, jint index, jint size) {
  JavaLangIndexOutOfBoundsException_initWithNSString_(self, JreStrcat("$I$I", @"Index ", index, @" requested, with a size of ", size));
}

AndroidDatabaseCursorIndexOutOfBoundsException *new_AndroidDatabaseCursorIndexOutOfBoundsException_initWithInt_withInt_(jint index, jint size) {
  J2OBJC_NEW_IMPL(AndroidDatabaseCursorIndexOutOfBoundsException, initWithInt_withInt_, index, size)
}

AndroidDatabaseCursorIndexOutOfBoundsException *create_AndroidDatabaseCursorIndexOutOfBoundsException_initWithInt_withInt_(jint index, jint size) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseCursorIndexOutOfBoundsException, initWithInt_withInt_, index, size)
}

void AndroidDatabaseCursorIndexOutOfBoundsException_initWithNSString_(AndroidDatabaseCursorIndexOutOfBoundsException *self, NSString *message) {
  JavaLangIndexOutOfBoundsException_initWithNSString_(self, message);
}

AndroidDatabaseCursorIndexOutOfBoundsException *new_AndroidDatabaseCursorIndexOutOfBoundsException_initWithNSString_(NSString *message) {
  J2OBJC_NEW_IMPL(AndroidDatabaseCursorIndexOutOfBoundsException, initWithNSString_, message)
}

AndroidDatabaseCursorIndexOutOfBoundsException *create_AndroidDatabaseCursorIndexOutOfBoundsException_initWithNSString_(NSString *message) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseCursorIndexOutOfBoundsException, initWithNSString_, message)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseCursorIndexOutOfBoundsException)
