//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/build/generated/source/apt/test/rx/doppl/mock/MSubscriber$Moxy.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxDopplMockMSubscriber$Moxy.h"
#include "RxDopplMockMSubscriber.h"
#include "RxProducer.h"
#include "java/lang/Boolean.h"
#include "java/lang/CloneNotSupportedException.h"
#include "java/lang/Integer.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/reflect/InvocationHandler.h"
#include "java/lang/reflect/Method.h"

@implementation RxDopplMockMSubscriber_Moxy

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxDopplMockMSubscriber_Moxy_init(self);
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

- (void)onCompleted {
  @try {
    if ($__handler_ == nil) {
      [super onCompleted];
    }
    else {
      [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"onCompleted" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:NSObject_class_()]];
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

- (void)super$onCompleted {
  [super onCompleted];
}

- (void)onStart {
  @try {
    if ($__handler_ == nil) {
      [super onStart];
    }
    else {
      [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"onStart" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:NSObject_class_()]];
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

- (void)super$onStart {
  [super onStart];
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

- (void)onNextWithId:(id)t {
  @try {
    if ($__handler_ == nil) {
      [super onNextWithId:t];
    }
    else {
      [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"onNext" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){ NSObject_class_() } count:1 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ t } count:1 type:NSObject_class_()]];
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

- (void)super$onNextWithId:(id)t {
  [super onNextWithId:t];
}

- (void)setProducerWithRxProducer:(id<RxProducer>)p {
  @try {
    if ($__handler_ == nil) {
      [super setProducerWithRxProducer:p];
    }
    else {
      [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"setProducer" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){ RxProducer_class_() } count:1 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ p } count:1 type:NSObject_class_()]];
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

- (void)super$setProducerWithRxProducer:(id<RxProducer>)p {
  [super setProducerWithRxProducer:p];
}

- (void)j2objcCleanup {
  @try {
    if ($__handler_ == nil) {
      [super j2objcCleanup];
    }
    else {
      [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"j2objcCleanup" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){  } count:0 type:NSObject_class_()]];
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

- (void)super$j2objcCleanup {
  [super j2objcCleanup];
}

- (void)onErrorWithNSException:(NSException *)e {
  @try {
    if ($__handler_ == nil) {
      [super onErrorWithNSException:e];
    }
    else {
      [$__handler_ invokeWithId:self withJavaLangReflectMethod:[[self java_getClass] getMethod:@"onError" parameterTypes:[IOSObjectArray arrayWithObjects:(id[]){ NSException_class_() } count:1 type:IOSClass_class_()]] withNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ e } count:1 type:NSObject_class_()]];
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

- (void)super$onErrorWithNSException:(NSException *)e {
  [super onErrorWithNSException:e];
}

- (void)dealloc {
  JreCheckFinalize(self, [RxDopplMockMSubscriber_Moxy class]);
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
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, 3, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 4, 5, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 6, 5, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0x1, 7, -1, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 8, 5, -1, 9, -1, -1 },
    { NULL, "V", 0x1, 10, 5, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 11, 12, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 13, 12, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 14, 15, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 16, 15, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(getHandler);
  methods[2].selector = @selector(setHandlerWithJavaLangReflectInvocationHandler:);
  methods[3].selector = @selector(clone);
  methods[4].selector = @selector(super$clone);
  methods[5].selector = @selector(onCompleted);
  methods[6].selector = @selector(super$onCompleted);
  methods[7].selector = @selector(onStart);
  methods[8].selector = @selector(super$onStart);
  methods[9].selector = @selector(hash);
  methods[10].selector = @selector(super$hashCode);
  methods[11].selector = @selector(isEqual:);
  methods[12].selector = @selector(super$equalsWithId:);
  methods[13].selector = @selector(description);
  methods[14].selector = @selector(super$toString);
  methods[15].selector = @selector(onNextWithId:);
  methods[16].selector = @selector(super$onNextWithId:);
  methods[17].selector = @selector(setProducerWithRxProducer:);
  methods[18].selector = @selector(super$setProducerWithRxProducer:);
  methods[19].selector = @selector(j2objcCleanup);
  methods[20].selector = @selector(super$j2objcCleanup);
  methods[21].selector = @selector(onErrorWithNSException:);
  methods[22].selector = @selector(super$onErrorWithNSException:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "$__handler_", "LJavaLangReflectInvocationHandler;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "setHandler", "LJavaLangReflectInvocationHandler;", "LJavaLangCloneNotSupportedException;", "hashCode", "equals", "LNSObject;", "super$equals", "toString", "onNext", "(Ljava/lang/Object;)V", "super$onNext", "setProducer", "LRxProducer;", "super$setProducer", "onError", "LNSException;", "super$onError" };
  static const J2ObjcClassInfo _RxDopplMockMSubscriber_Moxy = { "MSubscriber$Moxy", "rx.doppl.mock", ptrTable, methods, fields, 7, 0x1, 23, 1, -1, -1, -1, -1, -1 };
  return &_RxDopplMockMSubscriber_Moxy;
}

@end

void RxDopplMockMSubscriber_Moxy_init(RxDopplMockMSubscriber_Moxy *self) {
  RxDopplMockMSubscriber_init(self);
}

RxDopplMockMSubscriber_Moxy *new_RxDopplMockMSubscriber_Moxy_init() {
  J2OBJC_NEW_IMPL(RxDopplMockMSubscriber_Moxy, init)
}

RxDopplMockMSubscriber_Moxy *create_RxDopplMockMSubscriber_Moxy_init() {
  J2OBJC_CREATE_IMPL(RxDopplMockMSubscriber_Moxy, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxDopplMockMSubscriber_Moxy)
