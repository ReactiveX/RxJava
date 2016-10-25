//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/CrossProcessCursorWrapper.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "android/database/CrossProcessCursor.h"
#include "android/database/CrossProcessCursorWrapper.h"
#include "android/database/Cursor.h"
#include "android/database/CursorWindow.h"
#include "android/database/CursorWrapper.h"
#include "android/database/DatabaseUtils.h"

@implementation AndroidDatabaseCrossProcessCursorWrapper

- (instancetype)initWithAndroidDatabaseCursor:(id<AndroidDatabaseCursor>)cursor {
  AndroidDatabaseCrossProcessCursorWrapper_initWithAndroidDatabaseCursor_(self, cursor);
  return self;
}

- (void)fillWindowWithInt:(jint)position
withAndroidDatabaseCursorWindow:(AndroidDatabaseCursorWindow *)window {
  if ([AndroidDatabaseCrossProcessCursor_class_() isInstance:mCursor_]) {
    id<AndroidDatabaseCrossProcessCursor> crossProcessCursor = (id<AndroidDatabaseCrossProcessCursor>) cast_check(mCursor_, AndroidDatabaseCrossProcessCursor_class_());
    [((id<AndroidDatabaseCrossProcessCursor>) nil_chk(crossProcessCursor)) fillWindowWithInt:position withAndroidDatabaseCursorWindow:window];
    return;
  }
  AndroidDatabaseDatabaseUtils_cursorFillWindowWithAndroidDatabaseCursor_withInt_withAndroidDatabaseCursorWindow_(mCursor_, position, window);
}

- (AndroidDatabaseCursorWindow *)getWindow {
  if ([AndroidDatabaseCrossProcessCursor_class_() isInstance:mCursor_]) {
    id<AndroidDatabaseCrossProcessCursor> crossProcessCursor = (id<AndroidDatabaseCrossProcessCursor>) cast_check(mCursor_, AndroidDatabaseCrossProcessCursor_class_());
    return [((id<AndroidDatabaseCrossProcessCursor>) nil_chk(crossProcessCursor)) getWindow];
  }
  return nil;
}

- (jboolean)onMoveWithInt:(jint)oldPosition
                  withInt:(jint)newPosition {
  if ([AndroidDatabaseCrossProcessCursor_class_() isInstance:mCursor_]) {
    id<AndroidDatabaseCrossProcessCursor> crossProcessCursor = (id<AndroidDatabaseCrossProcessCursor>) cast_check(mCursor_, AndroidDatabaseCrossProcessCursor_class_());
    return [((id<AndroidDatabaseCrossProcessCursor>) nil_chk(crossProcessCursor)) onMoveWithInt:oldPosition withInt:newPosition];
  }
  return true;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, -1, -1, -1 },
    { NULL, "LAndroidDatabaseCursorWindow;", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 3, 4, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithAndroidDatabaseCursor:);
  methods[1].selector = @selector(fillWindowWithInt:withAndroidDatabaseCursorWindow:);
  methods[2].selector = @selector(getWindow);
  methods[3].selector = @selector(onMoveWithInt:withInt:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LAndroidDatabaseCursor;", "fillWindow", "ILAndroidDatabaseCursorWindow;", "onMove", "II" };
  static const J2ObjcClassInfo _AndroidDatabaseCrossProcessCursorWrapper = { "CrossProcessCursorWrapper", "android.database", ptrTable, methods, NULL, 7, 0x1, 4, 0, -1, -1, -1, -1, -1 };
  return &_AndroidDatabaseCrossProcessCursorWrapper;
}

@end

void AndroidDatabaseCrossProcessCursorWrapper_initWithAndroidDatabaseCursor_(AndroidDatabaseCrossProcessCursorWrapper *self, id<AndroidDatabaseCursor> cursor) {
  AndroidDatabaseCursorWrapper_initWithAndroidDatabaseCursor_(self, cursor);
}

AndroidDatabaseCrossProcessCursorWrapper *new_AndroidDatabaseCrossProcessCursorWrapper_initWithAndroidDatabaseCursor_(id<AndroidDatabaseCursor> cursor) {
  J2OBJC_NEW_IMPL(AndroidDatabaseCrossProcessCursorWrapper, initWithAndroidDatabaseCursor_, cursor)
}

AndroidDatabaseCrossProcessCursorWrapper *create_AndroidDatabaseCrossProcessCursorWrapper_initWithAndroidDatabaseCursor_(id<AndroidDatabaseCursor> cursor) {
  J2OBJC_CREATE_IMPL(AndroidDatabaseCrossProcessCursorWrapper, initWithAndroidDatabaseCursor_, cursor)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseCrossProcessCursorWrapper)
