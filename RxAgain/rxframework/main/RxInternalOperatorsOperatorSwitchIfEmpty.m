//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorSwitchIfEmpty.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxInternalOperatorsOperatorSwitchIfEmpty.h"
#include "RxInternalProducersProducerArbiter.h"
#include "RxObservable.h"
#include "RxProducer.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "RxSubscriptionsSerialSubscription.h"

@interface RxInternalOperatorsOperatorSwitchIfEmpty () {
 @public
  RxObservable *alternate_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty, alternate_, RxObservable *)

@interface RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber () {
 @public
  jboolean empty_;
  RxSubscriber *child_;
  RxSubscriptionsSerialSubscription *serial_;
  RxInternalProducersProducerArbiter *arbiter_;
  RxObservable *alternate_;
}

- (void)subscribeToAlternate;

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber, child_, RxSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber, serial_, RxSubscriptionsSerialSubscription *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber, arbiter_, RxInternalProducersProducerArbiter *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber, alternate_, RxObservable *)

__attribute__((unused)) static void RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_subscribeToAlternate(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber *self);

@interface RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber () {
 @public
  RxInternalProducersProducerArbiter *arbiter_;
  RxSubscriber *child_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber, arbiter_, RxInternalProducersProducerArbiter *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber, child_, RxSubscriber *)

@implementation RxInternalOperatorsOperatorSwitchIfEmpty

- (instancetype)initWithRxObservable:(RxObservable *)alternate {
  RxInternalOperatorsOperatorSwitchIfEmpty_initWithRxObservable_(self, alternate);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxSubscriptionsSerialSubscription *serial = create_RxSubscriptionsSerialSubscription_init();
  RxInternalProducersProducerArbiter *arbiter = create_RxInternalProducersProducerArbiter_init();
  RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber *parent = create_RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_(child, serial, arbiter, alternate_);
  [serial setWithRxSubscription:parent];
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:serial];
  [child setProducerWithRxProducer:arbiter];
  return parent;
}

- (void)dealloc {
  RELEASE_(alternate_);
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
    { "alternate_", "LRxObservable;", .constantValue.asLong = 0, 0x12, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;", "(Lrx/Observable<+TT;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "Lrx/Observable<+TT;>;", "LRxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber;LRxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSwitchIfEmpty = { "OperatorSwitchIfEmpty", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 1, -1, 6, -1, 7, -1 };
  return &_RxInternalOperatorsOperatorSwitchIfEmpty;
}

@end

void RxInternalOperatorsOperatorSwitchIfEmpty_initWithRxObservable_(RxInternalOperatorsOperatorSwitchIfEmpty *self, RxObservable *alternate) {
  NSObject_init(self);
  JreStrongAssign(&self->alternate_, alternate);
}

RxInternalOperatorsOperatorSwitchIfEmpty *new_RxInternalOperatorsOperatorSwitchIfEmpty_initWithRxObservable_(RxObservable *alternate) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSwitchIfEmpty, initWithRxObservable_, alternate)
}

RxInternalOperatorsOperatorSwitchIfEmpty *create_RxInternalOperatorsOperatorSwitchIfEmpty_initWithRxObservable_(RxObservable *alternate) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSwitchIfEmpty, initWithRxObservable_, alternate)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorSwitchIfEmpty)

@implementation RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber

- (instancetype)initWithRxSubscriber:(RxSubscriber *)child
withRxSubscriptionsSerialSubscription:(RxSubscriptionsSerialSubscription *)serial
withRxInternalProducersProducerArbiter:(RxInternalProducersProducerArbiter *)arbiter
                    withRxObservable:(RxObservable *)alternate {
  RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_(self, child, serial, arbiter, alternate);
  return self;
}

- (void)setProducerWithRxProducer:(id<RxProducer>)producer {
  [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) setProducerWithRxProducer:producer];
}

- (void)onCompleted {
  if (!empty_) {
    [((RxSubscriber *) nil_chk(child_)) onCompleted];
  }
  else if (![((RxSubscriber *) nil_chk(child_)) isUnsubscribed]) {
    RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_subscribeToAlternate(self);
  }
}

- (void)subscribeToAlternate {
  RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_subscribeToAlternate(self);
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubscriber *) nil_chk(child_)) onErrorWithNSException:e];
}

- (void)onNextWithId:(id)t {
  empty_ = false;
  [((RxSubscriber *) nil_chk(child_)) onNextWithId:t];
  [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) producedWithLong:1];
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber class]);
  RELEASE_(child_);
  RELEASE_(serial_);
  RELEASE_(arbiter_);
  RELEASE_(alternate_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x2, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 6, 7, -1, 8, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withRxSubscriptionsSerialSubscription:withRxInternalProducersProducerArbiter:withRxObservable:);
  methods[1].selector = @selector(setProducerWithRxProducer:);
  methods[2].selector = @selector(onCompleted);
  methods[3].selector = @selector(subscribeToAlternate);
  methods[4].selector = @selector(onErrorWithNSException:);
  methods[5].selector = @selector(onNextWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "empty_", "Z", .constantValue.asLong = 0, 0x2, -1, -1, -1, -1 },
    { "child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x12, -1, -1, 9, -1 },
    { "serial_", "LRxSubscriptionsSerialSubscription;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "arbiter_", "LRxInternalProducersProducerArbiter;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "alternate_", "LRxObservable;", .constantValue.asLong = 0, 0x12, -1, -1, 10, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LRxSubscriptionsSerialSubscription;LRxInternalProducersProducerArbiter;LRxObservable;", "(Lrx/Subscriber<-TT;>;Lrx/subscriptions/SerialSubscription;Lrx/internal/producers/ProducerArbiter;Lrx/Observable<+TT;>;)V", "setProducer", "LRxProducer;", "onError", "LNSException;", "onNext", "LNSObject;", "(TT;)V", "Lrx/Subscriber<-TT;>;", "Lrx/Observable<+TT;>;", "LRxInternalOperatorsOperatorSwitchIfEmpty;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber = { "ParentSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 6, 5, 11, -1, -1, 12, -1 };
  return &_RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber;
}

@end

void RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber *self, RxSubscriber *child, RxSubscriptionsSerialSubscription *serial, RxInternalProducersProducerArbiter *arbiter, RxObservable *alternate) {
  RxSubscriber_init(self);
  self->empty_ = true;
  JreStrongAssign(&self->child_, child);
  JreStrongAssign(&self->serial_, serial);
  JreStrongAssign(&self->arbiter_, arbiter);
  JreStrongAssign(&self->alternate_, alternate);
}

RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber *new_RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_(RxSubscriber *child, RxSubscriptionsSerialSubscription *serial, RxInternalProducersProducerArbiter *arbiter, RxObservable *alternate) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber, initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_, child, serial, arbiter, alternate)
}

RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber *create_RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_(RxSubscriber *child, RxSubscriptionsSerialSubscription *serial, RxInternalProducersProducerArbiter *arbiter, RxObservable *alternate) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber, initWithRxSubscriber_withRxSubscriptionsSerialSubscription_withRxInternalProducersProducerArbiter_withRxObservable_, child, serial, arbiter, alternate)
}

void RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber_subscribeToAlternate(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber *self) {
  RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber *as = create_RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber_initWithRxSubscriber_withRxInternalProducersProducerArbiter_(self->child_, self->arbiter_);
  [((RxSubscriptionsSerialSubscription *) nil_chk(self->serial_)) setWithRxSubscription:as];
  [((RxObservable *) nil_chk(self->alternate_)) unsafeSubscribeWithRxSubscriber:as];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorSwitchIfEmpty_ParentSubscriber)

@implementation RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber

- (instancetype)initWithRxSubscriber:(RxSubscriber *)child
withRxInternalProducersProducerArbiter:(RxInternalProducersProducerArbiter *)arbiter {
  RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber_initWithRxSubscriber_withRxInternalProducersProducerArbiter_(self, child, arbiter);
  return self;
}

- (void)setProducerWithRxProducer:(id<RxProducer>)producer {
  [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) setProducerWithRxProducer:producer];
}

- (void)onCompleted {
  if (child_ != nil) {
    [child_ onCompleted];
    JreStrongAssign(&child_, nil);
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  if (child_ != nil) {
    [child_ onErrorWithNSException:e];
    JreStrongAssign(&child_, nil);
  }
}

- (void)onNextWithId:(id)t {
  [((RxSubscriber *) nil_chk(child_)) onNextWithId:t];
  [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) producedWithLong:1];
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber class]);
  RELEASE_(arbiter_);
  RELEASE_(child_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 6, 7, -1, 8, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withRxInternalProducersProducerArbiter:);
  methods[1].selector = @selector(setProducerWithRxProducer:);
  methods[2].selector = @selector(onCompleted);
  methods[3].selector = @selector(onErrorWithNSException:);
  methods[4].selector = @selector(onNextWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "arbiter_", "LRxInternalProducersProducerArbiter;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x2, -1, -1, 9, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LRxInternalProducersProducerArbiter;", "(Lrx/Subscriber<-TT;>;Lrx/internal/producers/ProducerArbiter;)V", "setProducer", "LRxProducer;", "onError", "LNSException;", "onNext", "LNSObject;", "(TT;)V", "Lrx/Subscriber<-TT;>;", "LRxInternalOperatorsOperatorSwitchIfEmpty;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber = { "AlternateSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 5, 2, 10, -1, -1, 11, -1 };
  return &_RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber;
}

@end

void RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber_initWithRxSubscriber_withRxInternalProducersProducerArbiter_(RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber *self, RxSubscriber *child, RxInternalProducersProducerArbiter *arbiter) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->child_, child);
  JreStrongAssign(&self->arbiter_, arbiter);
}

RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber *new_RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber_initWithRxSubscriber_withRxInternalProducersProducerArbiter_(RxSubscriber *child, RxInternalProducersProducerArbiter *arbiter) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber, initWithRxSubscriber_withRxInternalProducersProducerArbiter_, child, arbiter)
}

RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber *create_RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber_initWithRxSubscriber_withRxInternalProducersProducerArbiter_(RxSubscriber *child, RxInternalProducersProducerArbiter *arbiter) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber, initWithRxSubscriber_withRxInternalProducersProducerArbiter_, child, arbiter)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorSwitchIfEmpty_AlternateSubscriber)
