//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/build/generated/source/apt/test/rx/doppl/mock/singletest/FuncStart$Moxy.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxDopplMockSingletestFuncStart$Moxy.h"
#include "RxDopplMockSingletestFuncStart.h"
#include "RxSingle.h"
#include "java/lang/Boolean.h"
#include "java/lang/CloneNotSupportedException.h"
#include "java/lang/Integer.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/reflect/InvocationHandler.h"
#include "java/lang/reflect/Method.h"

@implementation RxDopplMockSingletestFuncStart_Moxy

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxDopplMockSingletestFuncStart_Moxy_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (id<JavaLangReflectInvocationHandler>)getHandler {
  return $__handler_;
}

- (void)setHandlerWithJavaLangReflectInvocationHandler:(id<JavaLangReflectInvocationHandler>)handler {
  JreStrongAssign(&$__handler_, handler);
}

- (id)clone {
  @try {
    if ($__handler_ == nil) {
      return [super clone];
    }
    else {
      return [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"clone" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:NSObject_class_()]];
    }
  }
  @catch (NSException *__ttlive) {
    if ([__ttlive isKindOfClass:[JavaLangRuntimeException class]]) {
      @throw (JavaLangRuntimeException *) cast_chk(__ttlive, [JavaLangRuntimeException class]);
    }
    else if ([__ttlive isKindOfClass:[JavaLangCloneNotSupportedException class]]) {
      @throw (JavaLangCloneNotSupportedException *) cast_chk(__ttlive, [JavaLangCloneNotSupportedException class]);
    }
    else {
      @throw create_JavaLangRuntimeException_initWithNSException_(__ttlive);
    }
  }
}

- (id)super$clone {
  return [super clone];
}

- (id<RxSingle_OnSubscribe>)callWithId:(RxSingle *)t1
                                withId:(id<RxSingle_OnSubscribe>)t2 {
  @try {
    if ($__handler_ == nil) {
      return [super callWithId:t1 withId:t2];
    }
    else {
      return (id<RxSingle_OnSubscribe>) cast_check([$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"call" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){ RxSingle_class_(), RxSingle_OnSubscribe_class_() } count:2 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ t1, t2 } count:2 type:NSObject_class_()]], RxSingle_OnSubscribe_class_());
    }
  }
  @catch (NSException *__ttlive) {
    if ([__ttlive isKindOfClass:[JavaLangRuntimeException class]]) {
      @throw (JavaLangRuntimeException *) cast_chk(__ttlive, [JavaLangRuntimeException class]);
    }
    else {
      @throw create_JavaLangRuntimeException_initWithNSException_(__ttlive);
    }
  }
}

- (id<RxSingle_OnSubscribe>)super$callWithRxSingle:(RxSingle *)t1
                          withRxSingle_OnSubscribe:(id<RxSingle_OnSubscribe>)t2 {
  return [super callWithId:t1 withId:t2];
}

- (NSUInteger)hash {
  @try {
    if ($__handler_ == nil) {
      return ((jint) [super hash]);
    }
    else {
      return [((JavaLangInteger *) nil_chk((JavaLangInteger *) cast_chk([$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"hashCode" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:NSObject_class_()]], [JavaLangInteger class]))) intValue];
    }
  }
  @catch (NSException *__ttlive) {
    if ([__ttlive isKindOfClass:[JavaLangRuntimeException class]]) {
      @throw (JavaLangRuntimeException *) cast_chk(__ttlive, [JavaLangRuntimeException class]);
    }
    else {
      @throw create_JavaLangRuntimeException_initWithNSException_(__ttlive);
    }
  }
}

- (jint)super$hashCode {
  return ((jint) [super hash]);
}

- (jboolean)isEqual:(id)arg0 {
  @try {
    if ($__handler_ == nil) {
      return [super isEqual:arg0];
    }
    else {
      return [((JavaLangBoolean *) nil_chk((JavaLangBoolean *) cast_chk([$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"equals" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){ NSObject_class_() } count:1 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ arg0 } count:1 type:NSObject_class_()]], [JavaLangBoolean class]))) booleanValue];
    }
  }
  @catch (NSException *__ttlive) {
    if ([__ttlive isKindOfClass:[JavaLangRuntimeException class]]) {
      @throw (JavaLangRuntimeException *) cast_chk(__ttlive, [JavaLangRuntimeException class]);
    }
    else {
      @throw create_JavaLangRuntimeException_initWithNSException_(__ttlive);
    }
  }
}

- (jboolean)super$equalsWithId:(id)arg0 {
  return [super isEqual:arg0];
}

- (NSString *)description {
  @try {
    if ($__handler_ == nil) {
      return [super description];
    }
    else {
      return (NSString *) cast_chk([$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"toString" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:NSObject_class_()]], [NSString class]);
    }
  }
  @catch (NSException *__ttlive) {
    if ([__ttlive isKindOfClass:[JavaLangRuntimeException class]]) {
      @throw (JavaLangRuntimeException *) cast_chk(__ttlive, [JavaLangRuntimeException class]);
    }
    else {
      @throw create_JavaLangRuntimeException_initWithNSException_(__ttlive);
    }
  }
}

- (NSString *)super$toString {
  return [super description];
}

- (void)dealloc {
  RELEASE_($__handler_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "LJavaLangReflectInvocationHandler;", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "LNSObject;", 0x4, -1, -1, 2, -1, -1, -1 },
    { NULL, "LNSObject;", 0x4, -1, -1, 2, -1, -1, -1 },
    { NULL, "LRxSingle_OnSubscribe;", 0x1, 3, 4, -1, -1, -1, -1 },
    { NULL, "LRxSingle_OnSubscribe;", 0x1, 5, 4, -1, -1, -1, -1 },
    { NULL, "I", 0x1, 6, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 7, 8, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 9, 8, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0x1, 10, -1, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(getHandler);
  methods[2].selector = @selector(setHandlerWithJavaLangReflectInvocationHandler:);
  methods[3].selector = @selector(clone);
  methods[4].selector = @selector(super$clone);
  methods[5].selector = @selector(callWithId:withId:);
  methods[6].selector = @selector(super$callWithRxSingle:withRxSingle_OnSubscribe:);
  methods[7].selector = @selector(hash);
  methods[8].selector = @selector(super$hashCode);
  methods[9].selector = @selector(isEqual:);
  methods[10].selector = @selector(super$equalsWithId:);
  methods[11].selector = @selector(description);
  methods[12].selector = @selector(super$toString);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "$__handler_", "LJavaLangReflectInvocationHandler;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "setHandler", "LJavaLangReflectInvocationHandler;", "LJavaLangCloneNotSupportedException;", "call", "LRxSingle;LRxSingle_OnSubscribe;", "super$call", "hashCode", "equals", "LNSObject;", "super$equals", "toString" };
  static const J2ObjcClassInfo _RxDopplMockSingletestFuncStart_Moxy = { "FuncStart$Moxy", "rx.doppl.mock.singletest", ptrTable, methods, fields, 7, 0x1, 13, 1, -1, -1, -1, -1, -1 };
  return &_RxDopplMockSingletestFuncStart_Moxy;
}

@end

void RxDopplMockSingletestFuncStart_Moxy_init(RxDopplMockSingletestFuncStart_Moxy *self) {
  RxDopplMockSingletestFuncStart_init(self);
}

RxDopplMockSingletestFuncStart_Moxy *new_RxDopplMockSingletestFuncStart_Moxy_init() {
  J2OBJC_NEW_IMPL(RxDopplMockSingletestFuncStart_Moxy, init)
}

RxDopplMockSingletestFuncStart_Moxy *create_RxDopplMockSingletestFuncStart_Moxy_init() {
  J2OBJC_CREATE_IMPL(RxDopplMockSingletestFuncStart_Moxy, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxDopplMockSingletestFuncStart_Moxy)
