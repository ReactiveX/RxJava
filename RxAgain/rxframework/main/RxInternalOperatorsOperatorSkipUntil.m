//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorSkipUntil.java
//

#include "J2ObjC_source.h"
#include "RxInternalOperatorsOperatorSkipUntil.h"
#include "RxObservable.h"
#include "RxObserversSerializedSubscriber.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/util/concurrent/atomic/AtomicBoolean.h"

@interface RxInternalOperatorsOperatorSkipUntil_$1 : RxSubscriber {
 @public
  JavaUtilConcurrentAtomicAtomicBoolean *val$gate_;
  RxObserversSerializedSubscriber *val$s_;
}

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicBoolean:(JavaUtilConcurrentAtomicAtomicBoolean *)capture$0
                          withRxObserversSerializedSubscriber:(RxObserversSerializedSubscriber *)capture$1;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSkipUntil_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSkipUntil_$1, val$gate_, JavaUtilConcurrentAtomicAtomicBoolean *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSkipUntil_$1, val$s_, RxObserversSerializedSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(RxInternalOperatorsOperatorSkipUntil_$1 *self, JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1);

__attribute__((unused)) static RxInternalOperatorsOperatorSkipUntil_$1 *new_RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorSkipUntil_$1 *create_RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1);

@interface RxInternalOperatorsOperatorSkipUntil_$2 : RxSubscriber {
 @public
  JavaUtilConcurrentAtomicAtomicBoolean *val$gate_;
  RxObserversSerializedSubscriber *val$s_;
}

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicBoolean:(JavaUtilConcurrentAtomicAtomicBoolean *)capture$0
                          withRxObserversSerializedSubscriber:(RxObserversSerializedSubscriber *)capture$1
                                             withRxSubscriber:(RxSubscriber *)arg$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSkipUntil_$2)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSkipUntil_$2, val$gate_, JavaUtilConcurrentAtomicAtomicBoolean *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSkipUntil_$2, val$s_, RxObserversSerializedSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorSkipUntil_$2 *self, JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1, RxSubscriber *arg$0);

__attribute__((unused)) static RxInternalOperatorsOperatorSkipUntil_$2 *new_RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1, RxSubscriber *arg$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorSkipUntil_$2 *create_RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1, RxSubscriber *arg$0);

@implementation RxInternalOperatorsOperatorSkipUntil

- (instancetype)initWithRxObservable:(RxObservable *)other {
  RxInternalOperatorsOperatorSkipUntil_initWithRxObservable_(self, other);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxObserversSerializedSubscriber *s = create_RxObserversSerializedSubscriber_initWithRxSubscriber_(child);
  JavaUtilConcurrentAtomicAtomicBoolean *gate = create_JavaUtilConcurrentAtomicAtomicBoolean_init();
  RxSubscriber *u = create_RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(gate, s);
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:u];
  [((RxObservable *) nil_chk(other_)) unsafeSubscribeWithRxSubscriber:u];
  return create_RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(gate, s, child);
}

- (void)dealloc {
  RELEASE_(other_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObservable:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "other_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;", "(Lrx/Observable<TU;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "Lrx/Observable<TU;>;", "<T:Ljava/lang/Object;U:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSkipUntil = { "OperatorSkipUntil", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 1, -1, -1, -1, 6, -1 };
  return &_RxInternalOperatorsOperatorSkipUntil;
}

@end

void RxInternalOperatorsOperatorSkipUntil_initWithRxObservable_(RxInternalOperatorsOperatorSkipUntil *self, RxObservable *other) {
  NSObject_init(self);
  JreStrongAssign(&self->other_, other);
}

RxInternalOperatorsOperatorSkipUntil *new_RxInternalOperatorsOperatorSkipUntil_initWithRxObservable_(RxObservable *other) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSkipUntil, initWithRxObservable_, other)
}

RxInternalOperatorsOperatorSkipUntil *create_RxInternalOperatorsOperatorSkipUntil_initWithRxObservable_(RxObservable *other) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSkipUntil, initWithRxObservable_, other)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorSkipUntil)

@implementation RxInternalOperatorsOperatorSkipUntil_$1

- (void)onNextWithId:(id)t {
  [((JavaUtilConcurrentAtomicAtomicBoolean *) nil_chk(val$gate_)) setWithBoolean:true];
  [self unsubscribe];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxObserversSerializedSubscriber *) nil_chk(val$s_)) onErrorWithNSException:e];
  [val$s_ unsubscribe];
}

- (void)onCompleted {
  [self unsubscribe];
}

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicBoolean:(JavaUtilConcurrentAtomicAtomicBoolean *)capture$0
                          withRxObserversSerializedSubscriber:(RxObserversSerializedSubscriber *)capture$1 {
  RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(self, capture$0, capture$1);
  return self;
}

- (void)dealloc {
  RELEASE_(val$gate_);
  RELEASE_(val$s_);
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
  methods[3].selector = @selector(initWithJavaUtilConcurrentAtomicAtomicBoolean:withRxObserversSerializedSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$gate_", "LJavaUtilConcurrentAtomicAtomicBoolean;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$s_", "LRxObserversSerializedSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TU;)V", "onError", "LNSException;", "LJavaUtilConcurrentAtomicAtomicBoolean;LRxObserversSerializedSubscriber;", "(Ljava/util/concurrent/atomic/AtomicBoolean;Lrx/observers/SerializedSubscriber<TT;>;)V", "Lrx/observers/SerializedSubscriber<TT;>;", "LRxInternalOperatorsOperatorSkipUntil;", "callWithId:", "Lrx/Subscriber<TU;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSkipUntil_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 2, 8, -1, 9, 10, -1 };
  return &_RxInternalOperatorsOperatorSkipUntil_$1;
}

@end

void RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(RxInternalOperatorsOperatorSkipUntil_$1 *self, JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1) {
  JreStrongAssign(&self->val$gate_, capture$0);
  JreStrongAssign(&self->val$s_, capture$1);
  RxSubscriber_init(self);
}

RxInternalOperatorsOperatorSkipUntil_$1 *new_RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSkipUntil_$1, initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_, capture$0, capture$1)
}

RxInternalOperatorsOperatorSkipUntil_$1 *create_RxInternalOperatorsOperatorSkipUntil_$1_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSkipUntil_$1, initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_, capture$0, capture$1)
}

@implementation RxInternalOperatorsOperatorSkipUntil_$2

- (void)onNextWithId:(id)t {
  if ([((JavaUtilConcurrentAtomicAtomicBoolean *) nil_chk(val$gate_)) get]) {
    [((RxObserversSerializedSubscriber *) nil_chk(val$s_)) onNextWithId:t];
  }
  else {
    [self requestWithLong:1];
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxObserversSerializedSubscriber *) nil_chk(val$s_)) onErrorWithNSException:e];
  [self unsubscribe];
}

- (void)onCompleted {
  [((RxObserversSerializedSubscriber *) nil_chk(val$s_)) onCompleted];
  [self unsubscribe];
}

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicBoolean:(JavaUtilConcurrentAtomicAtomicBoolean *)capture$0
                          withRxObserversSerializedSubscriber:(RxObserversSerializedSubscriber *)capture$1
                                             withRxSubscriber:(RxSubscriber *)arg$0 {
  RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(self, capture$0, capture$1, arg$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$gate_);
  RELEASE_(val$s_);
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
  methods[3].selector = @selector(initWithJavaUtilConcurrentAtomicAtomicBoolean:withRxObserversSerializedSubscriber:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$gate_", "LJavaUtilConcurrentAtomicAtomicBoolean;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$s_", "LRxObserversSerializedSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "LJavaUtilConcurrentAtomicAtomicBoolean;LRxObserversSerializedSubscriber;LRxSubscriber;", "(Ljava/util/concurrent/atomic/AtomicBoolean;Lrx/observers/SerializedSubscriber<TT;>;Lrx/Subscriber<*>;)V", "Lrx/observers/SerializedSubscriber<TT;>;", "LRxInternalOperatorsOperatorSkipUntil;", "callWithId:", "Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSkipUntil_$2 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 2, 8, -1, 9, 10, -1 };
  return &_RxInternalOperatorsOperatorSkipUntil_$2;
}

@end

void RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorSkipUntil_$2 *self, JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1, RxSubscriber *arg$0) {
  JreStrongAssign(&self->val$gate_, capture$0);
  JreStrongAssign(&self->val$s_, capture$1);
  RxSubscriber_initWithRxSubscriber_(self, arg$0);
}

RxInternalOperatorsOperatorSkipUntil_$2 *new_RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1, RxSubscriber *arg$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSkipUntil_$2, initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_, capture$0, capture$1, arg$0)
}

RxInternalOperatorsOperatorSkipUntil_$2 *create_RxInternalOperatorsOperatorSkipUntil_$2_initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_(JavaUtilConcurrentAtomicAtomicBoolean *capture$0, RxObserversSerializedSubscriber *capture$1, RxSubscriber *arg$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSkipUntil_$2, initWithJavaUtilConcurrentAtomicAtomicBoolean_withRxObserversSerializedSubscriber_withRxSubscriber_, capture$0, capture$1, arg$0)
}
