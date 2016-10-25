//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorObserveOn.java
//

#include "J2ObjC_source.h"
#include "RxExceptionsMissingBackpressureException.h"
#include "RxInternalOperatorsBackpressureUtils.h"
#include "RxInternalOperatorsNotificationLite.h"
#include "RxInternalOperatorsOperatorObserveOn.h"
#include "RxInternalSchedulersImmediateScheduler.h"
#include "RxInternalSchedulersTrampolineScheduler.h"
#include "RxInternalUtilAtomicSpscAtomicArrayQueue.h"
#include "RxInternalUtilRxRingBuffer.h"
#include "RxInternalUtilUnsafeSpscArrayQueue.h"
#include "RxInternalUtilUnsafeUnsafeAccess.h"
#include "RxObservable.h"
#include "RxPluginsRxJavaHooks.h"
#include "RxProducer.h"
#include "RxScheduler.h"
#include "RxSchedulersSchedulers.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/util/Queue.h"
#include "java/util/concurrent/atomic/AtomicLong.h"

@interface RxInternalOperatorsOperatorObserveOn () {
 @public
  RxScheduler *scheduler_;
  jboolean delayError_;
  jint bufferSize_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorObserveOn, scheduler_, RxScheduler *)

@interface RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 : NSObject < RxProducer > {
 @public
  RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *this$0_;
}

- (void)requestWithLong:(jlong)n;

- (instancetype)initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber:(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *)outer$;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1, this$0_, RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 *self, RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *outer$);

__attribute__((unused)) static RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 *new_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *outer$) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 *create_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *outer$);

@interface RxInternalOperatorsOperatorObserveOn_$1 : NSObject < RxObservable_Operator > {
 @public
  jint val$n_;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child;

- (instancetype)initWithInt:(jint)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorObserveOn_$1)

__attribute__((unused)) static void RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(RxInternalOperatorsOperatorObserveOn_$1 *self, jint capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorObserveOn_$1 *new_RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(jint capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorObserveOn_$1 *create_RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(jint capture$0);

@implementation RxInternalOperatorsOperatorObserveOn

- (instancetype)initWithRxScheduler:(RxScheduler *)scheduler
                        withBoolean:(jboolean)delayError {
  RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_(self, scheduler, delayError);
  return self;
}

- (instancetype)initWithRxScheduler:(RxScheduler *)scheduler
                        withBoolean:(jboolean)delayError
                            withInt:(jint)bufferSize {
  RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_withInt_(self, scheduler, delayError, bufferSize);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  if ([scheduler_ isKindOfClass:[RxInternalSchedulersImmediateScheduler class]]) {
    return child;
  }
  else if ([scheduler_ isKindOfClass:[RxInternalSchedulersTrampolineScheduler class]]) {
    return child;
  }
  else {
    RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *parent = create_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_initWithRxScheduler_withRxSubscriber_withBoolean_withInt_(scheduler_, child, delayError_, bufferSize_);
    [parent init__];
    return parent;
  }
}

+ (id<RxObservable_Operator>)rebatchWithInt:(jint)n {
  return RxInternalOperatorsOperatorObserveOn_rebatchWithInt_(n);
}

- (void)dealloc {
  RELEASE_(scheduler_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 1, -1, -1, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "LRxObservable_Operator;", 0x9, 5, 6, -1, 7, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxScheduler:withBoolean:);
  methods[1].selector = @selector(initWithRxScheduler:withBoolean:withInt:);
  methods[2].selector = @selector(callWithId:);
  methods[3].selector = @selector(rebatchWithInt:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "scheduler_", "LRxScheduler;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "delayError_", "Z", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "bufferSize_", "I", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxScheduler;Z", "LRxScheduler;ZI", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "rebatch", "I", "<T:Ljava/lang/Object;>(I)Lrx/Observable$Operator<TT;TT;>;", "LRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorObserveOn = { "OperatorObserveOn", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 4, 3, -1, 8, -1, 9, -1 };
  return &_RxInternalOperatorsOperatorObserveOn;
}

@end

void RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_(RxInternalOperatorsOperatorObserveOn *self, RxScheduler *scheduler, jboolean delayError) {
  RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_withInt_(self, scheduler, delayError, JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE));
}

RxInternalOperatorsOperatorObserveOn *new_RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_(RxScheduler *scheduler, jboolean delayError) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorObserveOn, initWithRxScheduler_withBoolean_, scheduler, delayError)
}

RxInternalOperatorsOperatorObserveOn *create_RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_(RxScheduler *scheduler, jboolean delayError) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorObserveOn, initWithRxScheduler_withBoolean_, scheduler, delayError)
}

void RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_withInt_(RxInternalOperatorsOperatorObserveOn *self, RxScheduler *scheduler, jboolean delayError, jint bufferSize) {
  NSObject_init(self);
  JreStrongAssign(&self->scheduler_, scheduler);
  self->delayError_ = delayError;
  self->bufferSize_ = (bufferSize > 0) ? bufferSize : JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE);
}

RxInternalOperatorsOperatorObserveOn *new_RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_withInt_(RxScheduler *scheduler, jboolean delayError, jint bufferSize) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorObserveOn, initWithRxScheduler_withBoolean_withInt_, scheduler, delayError, bufferSize)
}

RxInternalOperatorsOperatorObserveOn *create_RxInternalOperatorsOperatorObserveOn_initWithRxScheduler_withBoolean_withInt_(RxScheduler *scheduler, jboolean delayError, jint bufferSize) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorObserveOn, initWithRxScheduler_withBoolean_withInt_, scheduler, delayError, bufferSize)
}

id<RxObservable_Operator> RxInternalOperatorsOperatorObserveOn_rebatchWithInt_(jint n) {
  RxInternalOperatorsOperatorObserveOn_initialize();
  return create_RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(n);
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorObserveOn)

@implementation RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber

- (instancetype)initWithRxScheduler:(RxScheduler *)scheduler
                   withRxSubscriber:(RxSubscriber *)child
                        withBoolean:(jboolean)delayError
                            withInt:(jint)bufferSize {
  RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_initWithRxScheduler_withRxSubscriber_withBoolean_withInt_(self, scheduler, child, delayError, bufferSize);
  return self;
}

- (void)init__ {
  RxSubscriber *localChild = child_;
  [((RxSubscriber *) nil_chk(localChild)) setProducerWithRxProducer:create_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(self)];
  [localChild addWithRxSubscription:recursiveScheduler_];
  [localChild addWithRxSubscription:self];
}

- (void)onNextWithId:(id)t {
  if ([self isUnsubscribed] || JreLoadVolatileBoolean(&finished_)) {
    return;
  }
  if (![((id<JavaUtilQueue>) nil_chk(queue_)) offerWithId:RxInternalOperatorsNotificationLite_nextWithId_(t)]) {
    [self onErrorWithNSException:create_RxExceptionsMissingBackpressureException_init()];
    return;
  }
  [self schedule];
}

- (void)onCompleted {
  if ([self isUnsubscribed] || JreLoadVolatileBoolean(&finished_)) {
    return;
  }
  JreAssignVolatileBoolean(&finished_, true);
  [self schedule];
}

- (void)onErrorWithNSException:(NSException *)e {
  if ([self isUnsubscribed] || JreLoadVolatileBoolean(&finished_)) {
    RxPluginsRxJavaHooks_onErrorWithNSException_(e);
    return;
  }
  JreStrongAssign(&error_, e);
  JreAssignVolatileBoolean(&finished_, true);
  [self schedule];
}

- (void)schedule {
  if ([((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(counter_)) getAndIncrement] == 0) {
    [((RxScheduler_Worker *) nil_chk(recursiveScheduler_)) scheduleWithRxFunctionsAction0:self];
  }
}

- (void)call {
  jlong missed = 1LL;
  jlong currentEmission = emitted_;
  id<JavaUtilQueue> q = self->queue_;
  RxSubscriber *localChild = self->child_;
  for (; ; ) {
    jlong requestAmount = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(requested_ObserveOnSubscriber_)) get];
    while (requestAmount != currentEmission) {
      jboolean done = JreLoadVolatileBoolean(&finished_);
      id v = [((id<JavaUtilQueue>) nil_chk(q)) poll];
      jboolean empty = v == nil;
      if ([self checkTerminatedWithBoolean:done withBoolean:empty withRxSubscriber:localChild withJavaUtilQueue:q]) {
        return;
      }
      if (empty) {
        break;
      }
      [((RxSubscriber *) nil_chk(localChild)) onNextWithId:RxInternalOperatorsNotificationLite_getValueWithId_(v)];
      currentEmission++;
      if (currentEmission == limit_) {
        requestAmount = RxInternalOperatorsBackpressureUtils_producedWithJavaUtilConcurrentAtomicAtomicLong_withLong_(requested_ObserveOnSubscriber_, currentEmission);
        [self requestWithLong:currentEmission];
        currentEmission = 0LL;
      }
    }
    if (requestAmount == currentEmission) {
      if ([self checkTerminatedWithBoolean:JreLoadVolatileBoolean(&finished_) withBoolean:[((id<JavaUtilQueue>) nil_chk(q)) isEmpty] withRxSubscriber:localChild withJavaUtilQueue:q]) {
        return;
      }
    }
    emitted_ = currentEmission;
    missed = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(counter_)) addAndGetWithLong:-missed];
    if (missed == 0LL) {
      break;
    }
  }
}

- (jboolean)checkTerminatedWithBoolean:(jboolean)done
                           withBoolean:(jboolean)isEmpty
                      withRxSubscriber:(RxSubscriber *)a
                     withJavaUtilQueue:(id<JavaUtilQueue>)q {
  if ([((RxSubscriber *) nil_chk(a)) isUnsubscribed]) {
    [((id<JavaUtilQueue>) nil_chk(q)) clear];
    return true;
  }
  if (done) {
    if (delayError_) {
      if (isEmpty) {
        NSException *e = error_;
        @try {
          if (e != nil) {
            [a onErrorWithNSException:e];
          }
          else {
            [a onCompleted];
          }
        }
        @finally {
          [((RxScheduler_Worker *) nil_chk(recursiveScheduler_)) unsubscribe];
        }
      }
    }
    else {
      NSException *e = error_;
      if (e != nil) {
        [((id<JavaUtilQueue>) nil_chk(q)) clear];
        @try {
          [a onErrorWithNSException:e];
        }
        @finally {
          [((RxScheduler_Worker *) nil_chk(recursiveScheduler_)) unsubscribe];
        }
        return true;
      }
      else if (isEmpty) {
        @try {
          [a onCompleted];
        }
        @finally {
          [((RxScheduler_Worker *) nil_chk(recursiveScheduler_)) unsubscribe];
        }
        return true;
      }
    }
  }
  return false;
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber class]);
  RELEASE_(child_);
  RELEASE_(recursiveScheduler_);
  RELEASE_(queue_);
  RELEASE_(requested_ObserveOnSubscriber_);
  RELEASE_(counter_);
  RELEASE_(error_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x0, 2, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, 5, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 6, 7, -1, -1, -1, -1 },
    { NULL, "V", 0x4, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x0, 8, 9, -1, 10, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxScheduler:withRxSubscriber:withBoolean:withInt:);
  methods[1].selector = @selector(init__);
  methods[2].selector = @selector(onNextWithId:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(onErrorWithNSException:);
  methods[5].selector = @selector(schedule);
  methods[6].selector = @selector(call);
  methods[7].selector = @selector(checkTerminatedWithBoolean:withBoolean:withRxSubscriber:withJavaUtilQueue:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 11, -1 },
    { "recursiveScheduler_", "LRxScheduler_Worker;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "delayError_", "Z", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "queue_", "LJavaUtilQueue;", .constantValue.asLong = 0, 0x10, -1, -1, 12, -1 },
    { "limit_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "finished_", "Z", .constantValue.asLong = 0, 0x40, -1, -1, -1, -1 },
    { "requested_ObserveOnSubscriber_", "LJavaUtilConcurrentAtomicAtomicLong;", .constantValue.asLong = 0, 0x10, 13, -1, -1, -1 },
    { "counter_", "LJavaUtilConcurrentAtomicAtomicLong;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "error_", "LNSException;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "emitted_", "J", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxScheduler;LRxSubscriber;ZI", "(Lrx/Scheduler;Lrx/Subscriber<-TT;>;ZI)V", "init", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "checkTerminated", "ZZLRxSubscriber;LJavaUtilQueue;", "(ZZLrx/Subscriber<-TT;>;Ljava/util/Queue<Ljava/lang/Object;>;)Z", "Lrx/Subscriber<-TT;>;", "Ljava/util/Queue<Ljava/lang/Object;>;", "requested", "LRxInternalOperatorsOperatorObserveOn;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;Lrx/functions/Action0;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber = { "ObserveOnSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 8, 10, 14, -1, -1, 15, -1 };
  return &_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber;
}

@end

void RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_initWithRxScheduler_withRxSubscriber_withBoolean_withInt_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *self, RxScheduler *scheduler, RxSubscriber *child, jboolean delayError, jint bufferSize) {
  RxSubscriber_init(self);
  JreStrongAssignAndConsume(&self->requested_ObserveOnSubscriber_, new_JavaUtilConcurrentAtomicAtomicLong_init());
  JreStrongAssignAndConsume(&self->counter_, new_JavaUtilConcurrentAtomicAtomicLong_init());
  JreStrongAssign(&self->child_, child);
  JreStrongAssign(&self->recursiveScheduler_, [((RxScheduler *) nil_chk(scheduler)) createWorker]);
  self->delayError_ = delayError;
  jint calculatedSize = (bufferSize > 0) ? bufferSize : JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE);
  self->limit_ = calculatedSize - (JreRShift32(calculatedSize, 2));
  if (RxInternalUtilUnsafeUnsafeAccess_isUnsafeAvailable()) {
    JreStrongAssignAndConsume(&self->queue_, new_RxInternalUtilUnsafeSpscArrayQueue_initWithInt_(calculatedSize));
  }
  else {
    JreStrongAssignAndConsume(&self->queue_, new_RxInternalUtilAtomicSpscAtomicArrayQueue_initWithInt_(calculatedSize));
  }
  [self requestWithLong:calculatedSize];
}

RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *new_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_initWithRxScheduler_withRxSubscriber_withBoolean_withInt_(RxScheduler *scheduler, RxSubscriber *child, jboolean delayError, jint bufferSize) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber, initWithRxScheduler_withRxSubscriber_withBoolean_withInt_, scheduler, child, delayError, bufferSize)
}

RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *create_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_initWithRxScheduler_withRxSubscriber_withBoolean_withInt_(RxScheduler *scheduler, RxSubscriber *child, jboolean delayError, jint bufferSize) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber, initWithRxScheduler_withRxSubscriber_withBoolean_withInt_, scheduler, child, delayError, bufferSize)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber)

@implementation RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1

- (void)requestWithLong:(jlong)n {
  if (n > 0LL) {
    RxInternalOperatorsBackpressureUtils_getAndAddRequestWithJavaUtilConcurrentAtomicAtomicLong_withLong_(this$0_->requested_ObserveOnSubscriber_, n);
    [this$0_ schedule];
  }
}

- (instancetype)initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber:(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *)outer$ {
  RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(self, outer$);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber;", "init__" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 2, -1, 3, -1, -1 };
  return &_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1;
}

@end

void RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 *self, RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  NSObject_init(self);
}

RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 *new_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1, initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_, outer$)
}

RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1 *create_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1_initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_$1, initWithRxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_, outer$)
}

@implementation RxInternalOperatorsOperatorObserveOn_$1

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber *parent = create_RxInternalOperatorsOperatorObserveOn_ObserveOnSubscriber_initWithRxScheduler_withRxSubscriber_withBoolean_withInt_(RxSchedulersSchedulers_immediate(), child, false, val$n_);
  [parent init__];
  return parent;
}

- (instancetype)initWithInt:(jint)capture$0 {
  RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(self, capture$0);
  return self;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxSubscriber;", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, NULL, 0x0, -1, 3, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(callWithId:);
  methods[1].selector = @selector(initWithInt:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$n_", "I", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "I", "LRxInternalOperatorsOperatorObserveOn;", "rebatchWithInt:", "Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorObserveOn_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 4, -1, 5, 6, -1 };
  return &_RxInternalOperatorsOperatorObserveOn_$1;
}

@end

void RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(RxInternalOperatorsOperatorObserveOn_$1 *self, jint capture$0) {
  self->val$n_ = capture$0;
  NSObject_init(self);
}

RxInternalOperatorsOperatorObserveOn_$1 *new_RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(jint capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorObserveOn_$1, initWithInt_, capture$0)
}

RxInternalOperatorsOperatorObserveOn_$1 *create_RxInternalOperatorsOperatorObserveOn_$1_initWithInt_(jint capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorObserveOn_$1, initWithInt_, capture$0)
}
