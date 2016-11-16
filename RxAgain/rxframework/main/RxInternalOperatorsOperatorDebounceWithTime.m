//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorDebounceWithTime.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsAction0.h"
#include "RxInternalOperatorsOperatorDebounceWithTime.h"
#include "RxObserversSerializedSubscriber.h"
#include "RxScheduler.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "RxSubscriptionsSerialSubscription.h"
#include "java/lang/Long.h"
#include "java/util/concurrent/TimeUnit.h"

@interface RxInternalOperatorsOperatorDebounceWithTime_$1 : RxSubscriber {
 @public
  RxInternalOperatorsOperatorDebounceWithTime *this$0_;
  RxInternalOperatorsOperatorDebounceWithTime_DebounceState *state_;
  RxSubscriber *self__;
  RxSubscriptionsSerialSubscription *val$serial_;
  RxScheduler_Worker *val$worker_;
  RxObserversSerializedSubscriber *val$s_;
}

- (void)onStart;

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxInternalOperatorsOperatorDebounceWithTime:(RxInternalOperatorsOperatorDebounceWithTime *)outer$
                              withRxSubscriptionsSerialSubscription:(RxSubscriptionsSerialSubscription *)capture$0
                                             withRxScheduler_Worker:(RxScheduler_Worker *)capture$1
                                withRxObserversSerializedSubscriber:(RxObserversSerializedSubscriber *)capture$2
                                                   withRxSubscriber:(RxSubscriber *)arg$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDebounceWithTime_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1, this$0_, RxInternalOperatorsOperatorDebounceWithTime *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1, state_, RxInternalOperatorsOperatorDebounceWithTime_DebounceState *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1, self__, RxSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1, val$serial_, RxSubscriptionsSerialSubscription *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1, val$worker_, RxScheduler_Worker *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1, val$s_, RxObserversSerializedSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDebounceWithTime_$1 *self, RxInternalOperatorsOperatorDebounceWithTime *outer$, RxSubscriptionsSerialSubscription *capture$0, RxScheduler_Worker *capture$1, RxObserversSerializedSubscriber *capture$2, RxSubscriber *arg$0);

__attribute__((unused)) static RxInternalOperatorsOperatorDebounceWithTime_$1 *new_RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDebounceWithTime *outer$, RxSubscriptionsSerialSubscription *capture$0, RxScheduler_Worker *capture$1, RxObserversSerializedSubscriber *capture$2, RxSubscriber *arg$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorDebounceWithTime_$1 *create_RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDebounceWithTime *outer$, RxSubscriptionsSerialSubscription *capture$0, RxScheduler_Worker *capture$1, RxObserversSerializedSubscriber *capture$2, RxSubscriber *arg$0);

@interface RxInternalOperatorsOperatorDebounceWithTime_$1_$1 : NSObject < RxFunctionsAction0 > {
 @public
  RxInternalOperatorsOperatorDebounceWithTime_$1 *this$0_;
  jint val$index_;
}

- (void)call;

- (instancetype)initWithRxInternalOperatorsOperatorDebounceWithTime_$1:(RxInternalOperatorsOperatorDebounceWithTime_$1 *)outer$
                                                               withInt:(jint)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDebounceWithTime_$1_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDebounceWithTime_$1_$1, this$0_, RxInternalOperatorsOperatorDebounceWithTime_$1 *)

__attribute__((unused)) static void RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(RxInternalOperatorsOperatorDebounceWithTime_$1_$1 *self, RxInternalOperatorsOperatorDebounceWithTime_$1 *outer$, jint capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorDebounceWithTime_$1_$1 *new_RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(RxInternalOperatorsOperatorDebounceWithTime_$1 *outer$, jint capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorDebounceWithTime_$1_$1 *create_RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(RxInternalOperatorsOperatorDebounceWithTime_$1 *outer$, jint capture$0);

@implementation RxInternalOperatorsOperatorDebounceWithTime

- (instancetype)initWithLong:(jlong)timeout
withJavaUtilConcurrentTimeUnit:(JavaUtilConcurrentTimeUnit *)unit
             withRxScheduler:(RxScheduler *)scheduler {
  RxInternalOperatorsOperatorDebounceWithTime_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(self, timeout, unit, scheduler);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxScheduler_Worker *worker = [((RxScheduler *) nil_chk(scheduler_)) createWorker];
  RxObserversSerializedSubscriber *s = create_RxObserversSerializedSubscriber_initWithRxSubscriber_(child);
  RxSubscriptionsSerialSubscription *serial = create_RxSubscriptionsSerialSubscription_init();
  [s addWithRxSubscription:worker];
  [s addWithRxSubscription:serial];
  return create_RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(self, serial, worker, s, child);
}

- (void)dealloc {
  RELEASE_(unit_);
  RELEASE_(scheduler_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 1, 2, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithLong:withJavaUtilConcurrentTimeUnit:withRxScheduler:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "timeout_", "J", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "unit_", "LJavaUtilConcurrentTimeUnit;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "scheduler_", "LRxScheduler;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "JLJavaUtilConcurrentTimeUnit;LRxScheduler;", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "LRxInternalOperatorsOperatorDebounceWithTime_DebounceState;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDebounceWithTime = { "OperatorDebounceWithTime", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 3, -1, 4, -1, 5, -1 };
  return &_RxInternalOperatorsOperatorDebounceWithTime;
}

@end

void RxInternalOperatorsOperatorDebounceWithTime_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(RxInternalOperatorsOperatorDebounceWithTime *self, jlong timeout, JavaUtilConcurrentTimeUnit *unit, RxScheduler *scheduler) {
  NSObject_init(self);
  self->timeout_ = timeout;
  JreStrongAssign(&self->unit_, unit);
  JreStrongAssign(&self->scheduler_, scheduler);
}

RxInternalOperatorsOperatorDebounceWithTime *new_RxInternalOperatorsOperatorDebounceWithTime_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(jlong timeout, JavaUtilConcurrentTimeUnit *unit, RxScheduler *scheduler) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDebounceWithTime, initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_, timeout, unit, scheduler)
}

RxInternalOperatorsOperatorDebounceWithTime *create_RxInternalOperatorsOperatorDebounceWithTime_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(jlong timeout, JavaUtilConcurrentTimeUnit *unit, RxScheduler *scheduler) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDebounceWithTime, initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_, timeout, unit, scheduler)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDebounceWithTime)

@implementation RxInternalOperatorsOperatorDebounceWithTime_DebounceState

- (jint)nextWithId:(id)value {
  @synchronized(self) {
    JreStrongAssign(&self->value_, value);
    self->hasValue_ = true;
    return ++index_;
  }
}

- (void)emitWithInt:(jint)index
   withRxSubscriber:(RxSubscriber *)onNextAndComplete
   withRxSubscriber:(RxSubscriber *)onError {
  id localValue;
  @synchronized(self) {
    if (emitting_ || !hasValue_ || index != self->index_) {
      return;
    }
    localValue = value_;
    JreStrongAssign(&value_, nil);
    hasValue_ = false;
    emitting_ = true;
  }
  @try {
    [((RxSubscriber *) nil_chk(onNextAndComplete)) onNextWithId:localValue];
  }
  @catch (NSException *e) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_withId_(e, onError, localValue);
    return;
  }
  @synchronized(self) {
    if (!terminate_) {
      emitting_ = false;
      return;
    }
  }
  [onNextAndComplete onCompleted];
}

- (void)emitAndCompleteWithRxSubscriber:(RxSubscriber *)onNextAndComplete
                       withRxSubscriber:(RxSubscriber *)onError {
  id localValue;
  jboolean localHasValue;
  @synchronized(self) {
    if (emitting_) {
      terminate_ = true;
      return;
    }
    localValue = value_;
    localHasValue = hasValue_;
    JreStrongAssign(&value_, nil);
    hasValue_ = false;
    emitting_ = true;
  }
  if (localHasValue) {
    @try {
      [((RxSubscriber *) nil_chk(onNextAndComplete)) onNextWithId:localValue];
    }
    @catch (NSException *e) {
      RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_withId_(e, onError, localValue);
      return;
    }
  }
  [((RxSubscriber *) nil_chk(onNextAndComplete)) onCompleted];
}

- (void)clear {
  @synchronized(self) {
    ++index_;
    JreStrongAssign(&value_, nil);
    hasValue_ = false;
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorDebounceWithTime_DebounceState_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  RELEASE_(value_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "I", 0x21, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, 5, -1, -1 },
    { NULL, "V", 0x1, 6, 7, -1, 8, -1, -1 },
    { NULL, "V", 0x21, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(nextWithId:);
  methods[1].selector = @selector(emitWithInt:withRxSubscriber:withRxSubscriber:);
  methods[2].selector = @selector(emitAndCompleteWithRxSubscriber:withRxSubscriber:);
  methods[3].selector = @selector(clear);
  methods[4].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "index_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "value_", "LNSObject;", .constantValue.asLong = 0, 0x0, -1, -1, 9, -1 },
    { "hasValue_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "terminate_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "emitting_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "next", "LNSObject;", "(TT;)I", "emit", "ILRxSubscriber;LRxSubscriber;", "(ILrx/Subscriber<TT;>;Lrx/Subscriber<*>;)V", "emitAndComplete", "LRxSubscriber;LRxSubscriber;", "(Lrx/Subscriber<TT;>;Lrx/Subscriber<*>;)V", "TT;", "LRxInternalOperatorsOperatorDebounceWithTime;", "<T:Ljava/lang/Object;>Ljava/lang/Object;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDebounceWithTime_DebounceState = { "DebounceState", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 5, 5, 10, -1, -1, 11, -1 };
  return &_RxInternalOperatorsOperatorDebounceWithTime_DebounceState;
}

@end

void RxInternalOperatorsOperatorDebounceWithTime_DebounceState_init(RxInternalOperatorsOperatorDebounceWithTime_DebounceState *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorDebounceWithTime_DebounceState *new_RxInternalOperatorsOperatorDebounceWithTime_DebounceState_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDebounceWithTime_DebounceState, init)
}

RxInternalOperatorsOperatorDebounceWithTime_DebounceState *create_RxInternalOperatorsOperatorDebounceWithTime_DebounceState_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDebounceWithTime_DebounceState, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDebounceWithTime_DebounceState)

@implementation RxInternalOperatorsOperatorDebounceWithTime_$1

- (void)onStart {
  [self requestWithLong:JavaLangLong_MAX_VALUE];
}

- (void)onNextWithId:(id)t {
  jint index = [((RxInternalOperatorsOperatorDebounceWithTime_DebounceState *) nil_chk(state_)) nextWithId:t];
  [((RxSubscriptionsSerialSubscription *) nil_chk(val$serial_)) setWithRxSubscription:[((RxScheduler_Worker *) nil_chk(val$worker_)) scheduleWithRxFunctionsAction0:create_RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(self, index) withLong:this$0_->timeout_ withJavaUtilConcurrentTimeUnit:this$0_->unit_]];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxObserversSerializedSubscriber *) nil_chk(val$s_)) onErrorWithNSException:e];
  [self unsubscribe];
  [((RxInternalOperatorsOperatorDebounceWithTime_DebounceState *) nil_chk(state_)) clear];
}

- (void)onCompleted {
  [((RxInternalOperatorsOperatorDebounceWithTime_DebounceState *) nil_chk(state_)) emitAndCompleteWithRxSubscriber:val$s_ withRxSubscriber:self];
}

- (instancetype)initWithRxInternalOperatorsOperatorDebounceWithTime:(RxInternalOperatorsOperatorDebounceWithTime *)outer$
                              withRxSubscriptionsSerialSubscription:(RxSubscriptionsSerialSubscription *)capture$0
                                             withRxScheduler_Worker:(RxScheduler_Worker *)capture$1
                                withRxObserversSerializedSubscriber:(RxObserversSerializedSubscriber *)capture$2
                                                   withRxSubscriber:(RxSubscriber *)arg$0 {
  RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(self, outer$, capture$0, capture$1, capture$2, arg$0);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  RELEASE_(state_);
  RELEASE_(self__);
  RELEASE_(val$serial_);
  RELEASE_(val$worker_);
  RELEASE_(val$s_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 5, -1, 6, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onStart);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(initWithRxInternalOperatorsOperatorDebounceWithTime:withRxSubscriptionsSerialSubscription:withRxScheduler_Worker:withRxObserversSerializedSubscriber:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorDebounceWithTime;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "state_", "LRxInternalOperatorsOperatorDebounceWithTime_DebounceState;", .constantValue.asLong = 0, 0x10, -1, -1, 7, -1 },
    { "self__", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, 8, -1, 9, -1 },
    { "val$serial_", "LRxSubscriptionsSerialSubscription;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$worker_", "LRxScheduler_Worker;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$s_", "LRxObserversSerializedSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 10, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "LRxInternalOperatorsOperatorDebounceWithTime;LRxSubscriptionsSerialSubscription;LRxScheduler_Worker;LRxObserversSerializedSubscriber;LRxSubscriber;", "(Lrx/internal/operators/OperatorDebounceWithTime;Lrx/subscriptions/SerialSubscription;Lrx/Scheduler$Worker;Lrx/observers/SerializedSubscriber<TT;>;Lrx/Subscriber<*>;)V", "Lrx/internal/operators/OperatorDebounceWithTime$DebounceState<TT;>;", "self", "Lrx/Subscriber<*>;", "Lrx/observers/SerializedSubscriber<TT;>;", "LRxInternalOperatorsOperatorDebounceWithTime;", "callWithId:", "Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDebounceWithTime_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 5, 6, 11, -1, 12, 13, -1 };
  return &_RxInternalOperatorsOperatorDebounceWithTime_$1;
}

@end

void RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDebounceWithTime_$1 *self, RxInternalOperatorsOperatorDebounceWithTime *outer$, RxSubscriptionsSerialSubscription *capture$0, RxScheduler_Worker *capture$1, RxObserversSerializedSubscriber *capture$2, RxSubscriber *arg$0) {
  JreStrongAssign(&self->this$0_, outer$);
  JreStrongAssign(&self->val$serial_, capture$0);
  JreStrongAssign(&self->val$worker_, capture$1);
  JreStrongAssign(&self->val$s_, capture$2);
  RxSubscriber_initWithRxSubscriber_(self, arg$0);
  JreStrongAssignAndConsume(&self->state_, new_RxInternalOperatorsOperatorDebounceWithTime_DebounceState_init());
  JreStrongAssign(&self->self__, self);
}

RxInternalOperatorsOperatorDebounceWithTime_$1 *new_RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDebounceWithTime *outer$, RxSubscriptionsSerialSubscription *capture$0, RxScheduler_Worker *capture$1, RxObserversSerializedSubscriber *capture$2, RxSubscriber *arg$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDebounceWithTime_$1, initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_, outer$, capture$0, capture$1, capture$2, arg$0)
}

RxInternalOperatorsOperatorDebounceWithTime_$1 *create_RxInternalOperatorsOperatorDebounceWithTime_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDebounceWithTime *outer$, RxSubscriptionsSerialSubscription *capture$0, RxScheduler_Worker *capture$1, RxObserversSerializedSubscriber *capture$2, RxSubscriber *arg$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDebounceWithTime_$1, initWithRxInternalOperatorsOperatorDebounceWithTime_withRxSubscriptionsSerialSubscription_withRxScheduler_Worker_withRxObserversSerializedSubscriber_withRxSubscriber_, outer$, capture$0, capture$1, capture$2, arg$0)
}

@implementation RxInternalOperatorsOperatorDebounceWithTime_$1_$1

- (void)call {
  [((RxInternalOperatorsOperatorDebounceWithTime_DebounceState *) nil_chk(this$0_->state_)) emitWithInt:val$index_ withRxSubscriber:this$0_->val$s_ withRxSubscriber:this$0_->self__];
}

- (instancetype)initWithRxInternalOperatorsOperatorDebounceWithTime_$1:(RxInternalOperatorsOperatorDebounceWithTime_$1 *)outer$
                                                               withInt:(jint)capture$0 {
  RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(self, outer$, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorDebounceWithTime_$1:withInt:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorDebounceWithTime_$1;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$index_", "I", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsOperatorDebounceWithTime_$1;I", "LRxInternalOperatorsOperatorDebounceWithTime_$1;", "onNextWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDebounceWithTime_$1_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 2, 1, -1, 2, -1, -1 };
  return &_RxInternalOperatorsOperatorDebounceWithTime_$1_$1;
}

@end

void RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(RxInternalOperatorsOperatorDebounceWithTime_$1_$1 *self, RxInternalOperatorsOperatorDebounceWithTime_$1 *outer$, jint capture$0) {
  JreStrongAssign(&self->this$0_, outer$);
  self->val$index_ = capture$0;
  NSObject_init(self);
}

RxInternalOperatorsOperatorDebounceWithTime_$1_$1 *new_RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(RxInternalOperatorsOperatorDebounceWithTime_$1 *outer$, jint capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDebounceWithTime_$1_$1, initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_, outer$, capture$0)
}

RxInternalOperatorsOperatorDebounceWithTime_$1_$1 *create_RxInternalOperatorsOperatorDebounceWithTime_$1_$1_initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_(RxInternalOperatorsOperatorDebounceWithTime_$1 *outer$, jint capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDebounceWithTime_$1_$1, initWithRxInternalOperatorsOperatorDebounceWithTime_$1_withInt_, outer$, capture$0)
}
