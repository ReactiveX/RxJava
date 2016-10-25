//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeTimerOnce.java
//

#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsAction0.h"
#include "RxInternalOperatorsOnSubscribeTimerOnce.h"
#include "RxScheduler.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/lang/Long.h"
#include "java/util/concurrent/TimeUnit.h"

@interface RxInternalOperatorsOnSubscribeTimerOnce_$1 : NSObject < RxFunctionsAction0 > {
 @public
  RxSubscriber *val$child_;
}

- (void)call;

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeTimerOnce_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeTimerOnce_$1, val$child_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(RxInternalOperatorsOnSubscribeTimerOnce_$1 *self, RxSubscriber *capture$0);

__attribute__((unused)) static RxInternalOperatorsOnSubscribeTimerOnce_$1 *new_RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(RxSubscriber *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOnSubscribeTimerOnce_$1 *create_RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(RxSubscriber *capture$0);

@implementation RxInternalOperatorsOnSubscribeTimerOnce

- (instancetype)initWithLong:(jlong)time
withJavaUtilConcurrentTimeUnit:(JavaUtilConcurrentTimeUnit *)unit
             withRxScheduler:(RxScheduler *)scheduler {
  RxInternalOperatorsOnSubscribeTimerOnce_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(self, time, unit, scheduler);
  return self;
}

- (void)callWithId:(RxSubscriber *)child {
  RxScheduler_Worker *worker = [((RxScheduler *) nil_chk(scheduler_)) createWorker];
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:worker];
  [((RxScheduler_Worker *) nil_chk(worker)) scheduleWithRxFunctionsAction0:create_RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(child) withLong:time_ withJavaUtilConcurrentTimeUnit:unit_];
}

- (void)dealloc {
  RELEASE_(unit_);
  RELEASE_(scheduler_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithLong:withJavaUtilConcurrentTimeUnit:withRxScheduler:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "time_", "J", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "unit_", "LJavaUtilConcurrentTimeUnit;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "scheduler_", "LRxScheduler;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "JLJavaUtilConcurrentTimeUnit;LRxScheduler;", "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/Long;>;)V", "Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/lang/Long;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeTimerOnce = { "OnSubscribeTimerOnce", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 3, -1, -1, -1, 4, -1 };
  return &_RxInternalOperatorsOnSubscribeTimerOnce;
}

@end

void RxInternalOperatorsOnSubscribeTimerOnce_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(RxInternalOperatorsOnSubscribeTimerOnce *self, jlong time, JavaUtilConcurrentTimeUnit *unit, RxScheduler *scheduler) {
  NSObject_init(self);
  self->time_ = time;
  JreStrongAssign(&self->unit_, unit);
  JreStrongAssign(&self->scheduler_, scheduler);
}

RxInternalOperatorsOnSubscribeTimerOnce *new_RxInternalOperatorsOnSubscribeTimerOnce_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(jlong time, JavaUtilConcurrentTimeUnit *unit, RxScheduler *scheduler) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeTimerOnce, initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_, time, unit, scheduler)
}

RxInternalOperatorsOnSubscribeTimerOnce *create_RxInternalOperatorsOnSubscribeTimerOnce_initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_(jlong time, JavaUtilConcurrentTimeUnit *unit, RxScheduler *scheduler) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeTimerOnce, initWithLong_withJavaUtilConcurrentTimeUnit_withRxScheduler_, time, unit, scheduler)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeTimerOnce)

@implementation RxInternalOperatorsOnSubscribeTimerOnce_$1

- (void)call {
  @try {
    [((RxSubscriber *) nil_chk(val$child_)) onNextWithId:JavaLangLong_valueOfWithLong_(0LL)];
  }
  @catch (NSException *t) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_(t, val$child_);
    return;
  }
  [val$child_ onCompleted];
}

- (instancetype)initWithRxSubscriber:(RxSubscriber *)capture$0 {
  RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$child_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 2, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/Long;>;)V", "Lrx/Subscriber<-Ljava/lang/Long;>;", "LRxInternalOperatorsOnSubscribeTimerOnce;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeTimerOnce_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 3, -1, 4, -1, -1 };
  return &_RxInternalOperatorsOnSubscribeTimerOnce_$1;
}

@end

void RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(RxInternalOperatorsOnSubscribeTimerOnce_$1 *self, RxSubscriber *capture$0) {
  JreStrongAssign(&self->val$child_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOnSubscribeTimerOnce_$1 *new_RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(RxSubscriber *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeTimerOnce_$1, initWithRxSubscriber_, capture$0)
}

RxInternalOperatorsOnSubscribeTimerOnce_$1 *create_RxInternalOperatorsOnSubscribeTimerOnce_$1_initWithRxSubscriber_(RxSubscriber *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeTimerOnce_$1, initWithRxSubscriber_, capture$0)
}
