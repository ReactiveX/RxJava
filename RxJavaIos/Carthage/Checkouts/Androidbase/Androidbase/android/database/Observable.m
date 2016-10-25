//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/Observable.java
//

#include "J2ObjC_source.h"
#include "android/database/Observable.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/util/ArrayList.h"

@implementation AndroidDatabaseObservable

- (void)registerObserverWithId:(id)observer {
  if (observer == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"The observer is null.");
  }
  @synchronized(mObservers_) {
    if ([((JavaUtilArrayList *) nil_chk(mObservers_)) containsWithId:observer]) {
      @throw create_JavaLangIllegalStateException_initWithNSString_(JreStrcat("$@$", @"Observer ", observer, @" is already registered."));
    }
    [mObservers_ addWithId:observer];
  }
}

- (void)unregisterObserverWithId:(id)observer {
  if (observer == nil) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"The observer is null.");
  }
  @synchronized(mObservers_) {
    jint index = [((JavaUtilArrayList *) nil_chk(mObservers_)) indexOfWithId:observer];
    if (index == -1) {
      @throw create_JavaLangIllegalStateException_initWithNSString_(JreStrcat("$@$", @"Observer ", observer, @" was not registered."));
    }
    [mObservers_ removeWithInt:index];
  }
}

- (void)unregisterAll {
  @synchronized(mObservers_) {
    [((JavaUtilArrayList *) nil_chk(mObservers_)) clear];
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  AndroidDatabaseObservable_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  RELEASE_(mObservers_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(registerObserverWithId:);
  methods[1].selector = @selector(unregisterObserverWithId:);
  methods[2].selector = @selector(unregisterAll);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "mObservers_", "LJavaUtilArrayList;", .constantValue.asLong = 0, 0x14, -1, -1, 4, -1 },
  };
  static const void *ptrTable[] = { "registerObserver", "LNSObject;", "(TT;)V", "unregisterObserver", "Ljava/util/ArrayList<TT;>;", "<T:Ljava/lang/Object;>Ljava/lang/Object;" };
  static const J2ObjcClassInfo _AndroidDatabaseObservable = { "Observable", "android.database", ptrTable, methods, fields, 7, 0x401, 4, 1, -1, -1, -1, 5, -1 };
  return &_AndroidDatabaseObservable;
}

@end

void AndroidDatabaseObservable_init(AndroidDatabaseObservable *self) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->mObservers_, new_JavaUtilArrayList_init());
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidDatabaseObservable)
