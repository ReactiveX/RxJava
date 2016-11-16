//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorTakeUntil.java
//

#include "J2ObjC_source.h"
#include "RxInternalOperatorsOperatorTakeUntil.h"
#include "RxObservable.h"
#include "RxObserversSerializedSubscriber.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/lang/Long.h"

@interface RxInternalOperatorsOperatorTakeUntil () {
 @public
  RxObservable *other_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorTakeUntil, other_, RxObservable *)

@interface RxInternalOperatorsOperatorTakeUntil_$1 : RxSubscriber {
 @public
  RxSubscriber *val$serial_;
}

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0
                    withRxSubscriber:(RxSubscriber *)arg$0
                         withBoolean:(jboolean)arg$1;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorTakeUntil_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorTakeUntil_$1, val$serial_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(RxInternalOperatorsOperatorTakeUntil_$1 *self, RxSubscriber *capture$0, RxSubscriber *arg$0, jboolean arg$1);

__attribute__((unused)) static RxInternalOperatorsOperatorTakeUntil_$1 *new_RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(RxSubscriber *capture$0, RxSubscriber *arg$0, jboolean arg$1) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorTakeUntil_$1 *create_RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(RxSubscriber *capture$0, RxSubscriber *arg$0, jboolean arg$1);

@interface RxInternalOperatorsOperatorTakeUntil_$2 : RxSubscriber {
 @public
  RxSubscriber *val$main_;
}

- (void)onStart;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)t;

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorTakeUntil_$2)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorTakeUntil_$2, val$main_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(RxInternalOperatorsOperatorTakeUntil_$2 *self, RxSubscriber *capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorTakeUntil_$2 *new_RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(RxSubscriber *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorTakeUntil_$2 *create_RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(RxSubscriber *capture$0);

@implementation RxInternalOperatorsOperatorTakeUntil

- (instancetype)initWithRxObservable:(RxObservable *)other {
  RxInternalOperatorsOperatorTakeUntil_initWithRxObservable_(self, other);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxSubscriber *serial = create_RxObserversSerializedSubscriber_initWithRxSubscriber_withBoolean_(child, false);
  RxSubscriber *main = create_RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(serial, serial, false);
  RxSubscriber *so = create_RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(main);
  [serial addWithRxSubscription:main];
  [serial addWithRxSubscription:so];
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:serial];
  [((RxObservable *) nil_chk(other_)) unsafeSubscribeWithRxSubscriber:so];
  return main;
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
    { "other_", "LRxObservable;", .constantValue.asLong = 0, 0x12, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;", "(Lrx/Observable<+TE;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "Lrx/Observable<+TE;>;", "<T:Ljava/lang/Object;E:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorTakeUntil = { "OperatorTakeUntil", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 1, -1, -1, -1, 6, -1 };
  return &_RxInternalOperatorsOperatorTakeUntil;
}

@end

void RxInternalOperatorsOperatorTakeUntil_initWithRxObservable_(RxInternalOperatorsOperatorTakeUntil *self, RxObservable *other) {
  NSObject_init(self);
  JreStrongAssign(&self->other_, other);
}

RxInternalOperatorsOperatorTakeUntil *new_RxInternalOperatorsOperatorTakeUntil_initWithRxObservable_(RxObservable *other) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorTakeUntil, initWithRxObservable_, other)
}

RxInternalOperatorsOperatorTakeUntil *create_RxInternalOperatorsOperatorTakeUntil_initWithRxObservable_(RxObservable *other) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorTakeUntil, initWithRxObservable_, other)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorTakeUntil)

@implementation RxInternalOperatorsOperatorTakeUntil_$1

- (void)onNextWithId:(id)t {
  [((RxSubscriber *) nil_chk(val$serial_)) onNextWithId:t];
}

- (void)onErrorWithNSException:(NSException *)e {
  @try {
    [((RxSubscriber *) nil_chk(val$serial_)) onErrorWithNSException:e];
  }
  @finally {
    [val$serial_ unsubscribe];
  }
}

- (void)onCompleted {
  @try {
    [((RxSubscriber *) nil_chk(val$serial_)) onCompleted];
  }
  @finally {
    [val$serial_ unsubscribe];
  }
}

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0
                    withRxSubscriber:(RxSubscriber *)arg$0
                         withBoolean:(jboolean)arg$1 {
  RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(self, capture$0, arg$0, arg$1);
  return self;
}

- (void)dealloc {
  RELEASE_(val$serial_);
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
  methods[3].selector = @selector(initWithRxSubscriber:withRxSubscriber:withBoolean:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$serial_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "LRxSubscriber;LRxSubscriber;Z", "(Lrx/Subscriber<TT;>;Lrx/Subscriber<*>;Z)V", "Lrx/Subscriber<TT;>;", "LRxInternalOperatorsOperatorTakeUntil;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorTakeUntil_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 1, 8, -1, 9, 7, -1 };
  return &_RxInternalOperatorsOperatorTakeUntil_$1;
}

@end

void RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(RxInternalOperatorsOperatorTakeUntil_$1 *self, RxSubscriber *capture$0, RxSubscriber *arg$0, jboolean arg$1) {
  JreStrongAssign(&self->val$serial_, capture$0);
  RxSubscriber_initWithRxSubscriber_withBoolean_(self, arg$0, arg$1);
}

RxInternalOperatorsOperatorTakeUntil_$1 *new_RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(RxSubscriber *capture$0, RxSubscriber *arg$0, jboolean arg$1) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorTakeUntil_$1, initWithRxSubscriber_withRxSubscriber_withBoolean_, capture$0, arg$0, arg$1)
}

RxInternalOperatorsOperatorTakeUntil_$1 *create_RxInternalOperatorsOperatorTakeUntil_$1_initWithRxSubscriber_withRxSubscriber_withBoolean_(RxSubscriber *capture$0, RxSubscriber *arg$0, jboolean arg$1) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorTakeUntil_$1, initWithRxSubscriber_withRxSubscriber_withBoolean_, capture$0, arg$0, arg$1)
}

@implementation RxInternalOperatorsOperatorTakeUntil_$2

- (void)onStart {
  [self requestWithLong:JavaLangLong_MAX_VALUE];
}

- (void)onCompleted {
  [((RxSubscriber *) nil_chk(val$main_)) onCompleted];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubscriber *) nil_chk(val$main_)) onErrorWithNSException:e];
}

- (void)onNextWithId:(id)t {
  [self onCompleted];
}

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0 {
  RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$main_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, NULL, 0x0, -1, 5, -1, 6, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onStart);
  methods[1].selector = @selector(onCompleted);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onNextWithId:);
  methods[4].selector = @selector(initWithRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$main_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "onError", "LNSException;", "onNext", "LNSObject;", "(TE;)V", "LRxSubscriber;", "(Lrx/Subscriber<TT;>;)V", "Lrx/Subscriber<TT;>;", "LRxInternalOperatorsOperatorTakeUntil;", "callWithId:", "Lrx/Subscriber<TE;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorTakeUntil_$2 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 5, 1, 8, -1, 9, 10, -1 };
  return &_RxInternalOperatorsOperatorTakeUntil_$2;
}

@end

void RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(RxInternalOperatorsOperatorTakeUntil_$2 *self, RxSubscriber *capture$0) {
  JreStrongAssign(&self->val$main_, capture$0);
  RxSubscriber_init(self);
}

RxInternalOperatorsOperatorTakeUntil_$2 *new_RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(RxSubscriber *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorTakeUntil_$2, initWithRxSubscriber_, capture$0)
}

RxInternalOperatorsOperatorTakeUntil_$2 *create_RxInternalOperatorsOperatorTakeUntil_$2_initWithRxSubscriber_(RxSubscriber *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorTakeUntil_$2, initWithRxSubscriber_, capture$0)
}
