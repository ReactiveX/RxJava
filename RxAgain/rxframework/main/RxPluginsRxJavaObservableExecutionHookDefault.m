//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/plugins/RxJavaObservableExecutionHookDefault.java
//

#include "J2ObjC_source.h"
#include "RxPluginsRxJavaObservableExecutionHook.h"
#include "RxPluginsRxJavaObservableExecutionHookDefault.h"

@interface RxPluginsRxJavaObservableExecutionHookDefault ()

- (instancetype)init;

@end

inline RxPluginsRxJavaObservableExecutionHookDefault *RxPluginsRxJavaObservableExecutionHookDefault_get_INSTANCE();
static RxPluginsRxJavaObservableExecutionHookDefault *RxPluginsRxJavaObservableExecutionHookDefault_INSTANCE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxPluginsRxJavaObservableExecutionHookDefault, INSTANCE, RxPluginsRxJavaObservableExecutionHookDefault *)

__attribute__((unused)) static void RxPluginsRxJavaObservableExecutionHookDefault_init(RxPluginsRxJavaObservableExecutionHookDefault *self);

__attribute__((unused)) static RxPluginsRxJavaObservableExecutionHookDefault *new_RxPluginsRxJavaObservableExecutionHookDefault_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxPluginsRxJavaObservableExecutionHookDefault *create_RxPluginsRxJavaObservableExecutionHookDefault_init();

J2OBJC_INITIALIZED_DEFN(RxPluginsRxJavaObservableExecutionHookDefault)

@implementation RxPluginsRxJavaObservableExecutionHookDefault

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxPluginsRxJavaObservableExecutionHookDefault_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (RxPluginsRxJavaObservableExecutionHook *)getInstance {
  return RxPluginsRxJavaObservableExecutionHookDefault_getInstance();
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x2, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxPluginsRxJavaObservableExecutionHook;", 0x9, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(getInstance);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "INSTANCE", "LRxPluginsRxJavaObservableExecutionHookDefault;", .constantValue.asLong = 0, 0x1a, -1, 0, -1, -1 },
  };
  static const void *ptrTable[] = { &RxPluginsRxJavaObservableExecutionHookDefault_INSTANCE };
  static const J2ObjcClassInfo _RxPluginsRxJavaObservableExecutionHookDefault = { "RxJavaObservableExecutionHookDefault", "rx.plugins", ptrTable, methods, fields, 7, 0x10, 2, 1, -1, -1, -1, -1, -1 };
  return &_RxPluginsRxJavaObservableExecutionHookDefault;
}

+ (void)initialize {
  if (self == [RxPluginsRxJavaObservableExecutionHookDefault class]) {
    JreStrongAssignAndConsume(&RxPluginsRxJavaObservableExecutionHookDefault_INSTANCE, new_RxPluginsRxJavaObservableExecutionHookDefault_init());
    J2OBJC_SET_INITIALIZED(RxPluginsRxJavaObservableExecutionHookDefault)
  }
}

@end

void RxPluginsRxJavaObservableExecutionHookDefault_init(RxPluginsRxJavaObservableExecutionHookDefault *self) {
  RxPluginsRxJavaObservableExecutionHook_init(self);
}

RxPluginsRxJavaObservableExecutionHookDefault *new_RxPluginsRxJavaObservableExecutionHookDefault_init() {
  J2OBJC_NEW_IMPL(RxPluginsRxJavaObservableExecutionHookDefault, init)
}

RxPluginsRxJavaObservableExecutionHookDefault *create_RxPluginsRxJavaObservableExecutionHookDefault_init() {
  J2OBJC_CREATE_IMPL(RxPluginsRxJavaObservableExecutionHookDefault, init)
}

RxPluginsRxJavaObservableExecutionHook *RxPluginsRxJavaObservableExecutionHookDefault_getInstance() {
  RxPluginsRxJavaObservableExecutionHookDefault_initialize();
  return RxPluginsRxJavaObservableExecutionHookDefault_INSTANCE;
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxPluginsRxJavaObservableExecutionHookDefault)
