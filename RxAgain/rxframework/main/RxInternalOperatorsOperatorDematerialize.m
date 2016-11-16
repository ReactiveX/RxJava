//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorDematerialize.java
//

#include "J2ObjC_source.h"
#include "RxInternalOperatorsOperatorDematerialize.h"
#include "RxNotification.h"
#include "RxSubscriber.h"
#include "java/lang/IllegalArgumentException.h"

@interface RxInternalOperatorsOperatorDematerialize_$1 : RxSubscriber {
 @public
  jboolean terminated_;
  RxSubscriber *val$child_;
}

- (void)onNextWithId:(RxNotification *)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0
                    withRxSubscriber:(RxSubscriber *)arg$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDematerialize_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDematerialize_$1, val$child_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDematerialize_$1 *self, RxSubscriber *capture$0, RxSubscriber *arg$0);

__attribute__((unused)) static RxInternalOperatorsOperatorDematerialize_$1 *new_RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(RxSubscriber *capture$0, RxSubscriber *arg$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorDematerialize_$1 *create_RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(RxSubscriber *capture$0, RxSubscriber *arg$0);

@implementation RxInternalOperatorsOperatorDematerialize

+ (RxInternalOperatorsOperatorDematerialize *)instance {
  return RxInternalOperatorsOperatorDematerialize_instance();
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorDematerialize_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  return create_RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(child, child);
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalOperatorsOperatorDematerialize;", 0x9, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 0, 1, -1, 2, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(instance);
  methods[1].selector = @selector(init);
  methods[2].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-Lrx/Notification<TT;>;>;", "LRxInternalOperatorsOperatorDematerialize_Holder;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;Lrx/Notification<TT;>;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDematerialize = { "OperatorDematerialize", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x11, 3, 0, -1, 3, -1, 4, -1 };
  return &_RxInternalOperatorsOperatorDematerialize;
}

@end

RxInternalOperatorsOperatorDematerialize *RxInternalOperatorsOperatorDematerialize_instance() {
  RxInternalOperatorsOperatorDematerialize_initialize();
  return JreLoadStatic(RxInternalOperatorsOperatorDematerialize_Holder, INSTANCE);
}

void RxInternalOperatorsOperatorDematerialize_init(RxInternalOperatorsOperatorDematerialize *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorDematerialize *new_RxInternalOperatorsOperatorDematerialize_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDematerialize, init)
}

RxInternalOperatorsOperatorDematerialize *create_RxInternalOperatorsOperatorDematerialize_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDematerialize, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDematerialize)

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsOperatorDematerialize_Holder)

RxInternalOperatorsOperatorDematerialize *RxInternalOperatorsOperatorDematerialize_Holder_INSTANCE;

@implementation RxInternalOperatorsOperatorDematerialize_Holder

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorDematerialize_Holder_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "INSTANCE", "LRxInternalOperatorsOperatorDematerialize;", .constantValue.asLong = 0, 0x18, -1, 0, 1, -1 },
  };
  static const void *ptrTable[] = { &RxInternalOperatorsOperatorDematerialize_Holder_INSTANCE, "Lrx/internal/operators/OperatorDematerialize<Ljava/lang/Object;>;", "LRxInternalOperatorsOperatorDematerialize;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDematerialize_Holder = { "Holder", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 1, 1, 2, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorDematerialize_Holder;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsOperatorDematerialize_Holder class]) {
    JreStrongAssignAndConsume(&RxInternalOperatorsOperatorDematerialize_Holder_INSTANCE, new_RxInternalOperatorsOperatorDematerialize_init());
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsOperatorDematerialize_Holder)
  }
}

@end

void RxInternalOperatorsOperatorDematerialize_Holder_init(RxInternalOperatorsOperatorDematerialize_Holder *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorDematerialize_Holder *new_RxInternalOperatorsOperatorDematerialize_Holder_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDematerialize_Holder, init)
}

RxInternalOperatorsOperatorDematerialize_Holder *create_RxInternalOperatorsOperatorDematerialize_Holder_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDematerialize_Holder, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDematerialize_Holder)

@implementation RxInternalOperatorsOperatorDematerialize_$1

- (void)onNextWithId:(RxNotification *)t {
  switch ([[((RxNotification *) nil_chk(t)) getKind] ordinal]) {
    case RxNotification_Kind_Enum_OnNext:
    if (!terminated_) {
      [((RxSubscriber *) nil_chk(val$child_)) onNextWithId:[t getValue]];
    }
    break;
    case RxNotification_Kind_Enum_OnError:
    [self onErrorWithNSException:[t getThrowable]];
    break;
    case RxNotification_Kind_Enum_OnCompleted:
    [self onCompleted];
    break;
    default:
    [self onErrorWithNSException:create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$@", @"Unsupported notification type: ", t))];
    break;
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  if (!terminated_) {
    terminated_ = true;
    [((RxSubscriber *) nil_chk(val$child_)) onErrorWithNSException:e];
  }
}

- (void)onCompleted {
  if (!terminated_) {
    terminated_ = true;
    [((RxSubscriber *) nil_chk(val$child_)) onCompleted];
  }
}

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0
                    withRxSubscriber:(RxSubscriber *)arg$0 {
  RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(self, capture$0, arg$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$child_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 5, -1, 6, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onNextWithId:);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onCompleted);
  methods[3].selector = @selector(initWithRxSubscriber:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "terminated_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "val$child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LRxNotification;", "(Lrx/Notification<TT;>;)V", "onError", "LNSException;", "LRxSubscriber;LRxSubscriber;", "(Lrx/Subscriber<-TT;>;Lrx/Subscriber<*>;)V", "Lrx/Subscriber<-TT;>;", "LRxInternalOperatorsOperatorDematerialize;", "callWithId:", "Lrx/Subscriber<Lrx/Notification<TT;>;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDematerialize_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 2, 8, -1, 9, 10, -1 };
  return &_RxInternalOperatorsOperatorDematerialize_$1;
}

@end

void RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDematerialize_$1 *self, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  JreStrongAssign(&self->val$child_, capture$0);
  RxSubscriber_initWithRxSubscriber_(self, arg$0);
}

RxInternalOperatorsOperatorDematerialize_$1 *new_RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(RxSubscriber *capture$0, RxSubscriber *arg$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDematerialize_$1, initWithRxSubscriber_withRxSubscriber_, capture$0, arg$0)
}

RxInternalOperatorsOperatorDematerialize_$1 *create_RxInternalOperatorsOperatorDematerialize_$1_initWithRxSubscriber_withRxSubscriber_(RxSubscriber *capture$0, RxSubscriber *arg$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDematerialize_$1, initWithRxSubscriber_withRxSubscriber_, capture$0, arg$0)
}
