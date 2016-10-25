//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeConcatMap.java
//

#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxExceptionsMissingBackpressureException.h"
#include "RxFunctionsFunc1.h"
#include "RxInternalOperatorsNotificationLite.h"
#include "RxInternalOperatorsOnSubscribeConcatMap.h"
#include "RxInternalProducersProducerArbiter.h"
#include "RxInternalUtilAtomicSpscAtomicArrayQueue.h"
#include "RxInternalUtilExceptionsUtils.h"
#include "RxInternalUtilScalarSynchronousObservable.h"
#include "RxInternalUtilUnsafeSpscArrayQueue.h"
#include "RxInternalUtilUnsafeUnsafeAccess.h"
#include "RxObservable.h"
#include "RxObserversSerializedSubscriber.h"
#include "RxPluginsRxJavaHooks.h"
#include "RxProducer.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "RxSubscriptionsSerialSubscription.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/NullPointerException.h"
#include "java/util/Queue.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"
#include "java/util/concurrent/atomic/AtomicReference.h"

@interface RxInternalOperatorsOnSubscribeConcatMap_$1 : NSObject < RxProducer > {
 @public
  RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *val$parent_;
}

- (void)requestWithLong:(jlong)n;

- (instancetype)initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeConcatMap_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeConcatMap_$1, val$parent_, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_$1 *self, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *capture$0);

__attribute__((unused)) static RxInternalOperatorsOnSubscribeConcatMap_$1 *new_RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOnSubscribeConcatMap_$1 *create_RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *capture$0);

@implementation RxInternalOperatorsOnSubscribeConcatMap

- (instancetype)initWithRxObservable:(RxObservable *)source
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)mapper
                             withInt:(jint)prefetch
                             withInt:(jint)delayErrorMode {
  RxInternalOperatorsOnSubscribeConcatMap_initWithRxObservable_withRxFunctionsFunc1_withInt_withInt_(self, source, mapper, prefetch, delayErrorMode);
  return self;
}

- (void)callWithId:(RxSubscriber *)child {
  RxSubscriber *s;
  if (delayErrorMode_ == RxInternalOperatorsOnSubscribeConcatMap_IMMEDIATE) {
    s = create_RxObserversSerializedSubscriber_initWithRxSubscriber_(child);
  }
  else {
    s = child;
  }
  RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent = create_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_(s, mapper_, prefetch_, delayErrorMode_);
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:parent];
  [child addWithRxSubscription:parent->inner_];
  [child setProducerWithRxProducer:create_RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(parent)];
  if (![child isUnsubscribed]) {
    [((RxObservable *) nil_chk(source_)) unsafeSubscribeWithRxSubscriber:parent];
  }
}

- (void)dealloc {
  RELEASE_(source_);
  RELEASE_(mapper_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObservable:withRxFunctionsFunc1:withInt:withInt:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "source_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "mapper_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
    { "prefetch_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "delayErrorMode_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "IMMEDIATE", "I", .constantValue.asInt = RxInternalOperatorsOnSubscribeConcatMap_IMMEDIATE, 0x19, -1, -1, -1, -1 },
    { "BOUNDARY", "I", .constantValue.asInt = RxInternalOperatorsOnSubscribeConcatMap_BOUNDARY, 0x19, -1, -1, -1, -1 },
    { "END", "I", .constantValue.asInt = RxInternalOperatorsOnSubscribeConcatMap_END, 0x19, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;LRxFunctionsFunc1;II", "(Lrx/Observable<+TT;>;Lrx/functions/Func1<-TT;+Lrx/Observable<+TR;>;>;II)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TR;>;)V", "Lrx/Observable<+TT;>;", "Lrx/functions/Func1<-TT;+Lrx/Observable<+TR;>;>;", "LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber;LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer;", "<T:Ljava/lang/Object;R:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$OnSubscribe<TR;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeConcatMap = { "OnSubscribeConcatMap", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 7, -1, 7, -1, 8, -1 };
  return &_RxInternalOperatorsOnSubscribeConcatMap;
}

@end

void RxInternalOperatorsOnSubscribeConcatMap_initWithRxObservable_withRxFunctionsFunc1_withInt_withInt_(RxInternalOperatorsOnSubscribeConcatMap *self, RxObservable *source, id<RxFunctionsFunc1> mapper, jint prefetch, jint delayErrorMode) {
  NSObject_init(self);
  JreStrongAssign(&self->source_, source);
  JreStrongAssign(&self->mapper_, mapper);
  self->prefetch_ = prefetch;
  self->delayErrorMode_ = delayErrorMode;
}

RxInternalOperatorsOnSubscribeConcatMap *new_RxInternalOperatorsOnSubscribeConcatMap_initWithRxObservable_withRxFunctionsFunc1_withInt_withInt_(RxObservable *source, id<RxFunctionsFunc1> mapper, jint prefetch, jint delayErrorMode) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeConcatMap, initWithRxObservable_withRxFunctionsFunc1_withInt_withInt_, source, mapper, prefetch, delayErrorMode)
}

RxInternalOperatorsOnSubscribeConcatMap *create_RxInternalOperatorsOnSubscribeConcatMap_initWithRxObservable_withRxFunctionsFunc1_withInt_withInt_(RxObservable *source, id<RxFunctionsFunc1> mapper, jint prefetch, jint delayErrorMode) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeConcatMap, initWithRxObservable_withRxFunctionsFunc1_withInt_withInt_, source, mapper, prefetch, delayErrorMode)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeConcatMap)

@implementation RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)mapper
                             withInt:(jint)prefetch
                             withInt:(jint)delayErrorMode {
  RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_(self, actual, mapper, prefetch, delayErrorMode);
  return self;
}

- (void)onNextWithId:(id)t {
  if (![((id<JavaUtilQueue>) nil_chk(queue_)) offerWithId:RxInternalOperatorsNotificationLite_nextWithId_(t)]) {
    [self unsubscribe];
    [self onErrorWithNSException:create_RxExceptionsMissingBackpressureException_init()];
  }
  else {
    [self drain];
  }
}

- (void)onErrorWithNSException:(NSException *)mainError {
  if (RxInternalUtilExceptionsUtils_addThrowableWithJavaUtilConcurrentAtomicAtomicReference_withNSException_(error_, mainError)) {
    JreAssignVolatileBoolean(&done_, true);
    if (delayErrorMode_ == RxInternalOperatorsOnSubscribeConcatMap_IMMEDIATE) {
      NSException *ex = RxInternalUtilExceptionsUtils_terminateWithJavaUtilConcurrentAtomicAtomicReference_(error_);
      if (!RxInternalUtilExceptionsUtils_isTerminatedWithNSException_(ex)) {
        [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:ex];
      }
      [((RxSubscriptionsSerialSubscription *) nil_chk(inner_)) unsubscribe];
    }
    else {
      [self drain];
    }
  }
  else {
    [self pluginErrorWithNSException:mainError];
  }
}

- (void)onCompleted {
  JreAssignVolatileBoolean(&done_, true);
  [self drain];
}

- (void)requestMoreWithLong:(jlong)n {
  if (n > 0) {
    [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) requestWithLong:n];
  }
  else if (n < 0) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$J", @"n >= 0 required but it was ", n));
  }
}

- (void)innerNextWithId:(id)value {
  [((RxSubscriber *) nil_chk(actual_)) onNextWithId:value];
}

- (void)innerErrorWithNSException:(NSException *)innerError
                         withLong:(jlong)produced {
  if (!RxInternalUtilExceptionsUtils_addThrowableWithJavaUtilConcurrentAtomicAtomicReference_withNSException_(error_, innerError)) {
    [self pluginErrorWithNSException:innerError];
  }
  else if (delayErrorMode_ == RxInternalOperatorsOnSubscribeConcatMap_IMMEDIATE) {
    NSException *ex = RxInternalUtilExceptionsUtils_terminateWithJavaUtilConcurrentAtomicAtomicReference_(error_);
    if (!RxInternalUtilExceptionsUtils_isTerminatedWithNSException_(ex)) {
      [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:ex];
    }
    [self unsubscribe];
  }
  else {
    if (produced != 0LL) {
      [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) producedWithLong:produced];
    }
    JreAssignVolatileBoolean(&active_, false);
    [self drain];
  }
}

- (void)innerCompletedWithLong:(jlong)produced {
  if (produced != 0LL) {
    [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) producedWithLong:produced];
  }
  JreAssignVolatileBoolean(&active_, false);
  [self drain];
}

- (void)pluginErrorWithNSException:(NSException *)e {
  RxPluginsRxJavaHooks_onErrorWithNSException_(e);
}

- (void)drain {
  if ([((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) getAndIncrement] != 0) {
    return;
  }
  jint delayErrorMode = self->delayErrorMode_;
  for (; ; ) {
    if ([((RxSubscriber *) nil_chk(actual_)) isUnsubscribed]) {
      return;
    }
    if (!JreLoadVolatileBoolean(&active_)) {
      if (delayErrorMode == RxInternalOperatorsOnSubscribeConcatMap_BOUNDARY) {
        if ([((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(error_)) get] != nil) {
          NSException *ex = RxInternalUtilExceptionsUtils_terminateWithJavaUtilConcurrentAtomicAtomicReference_(error_);
          if (!RxInternalUtilExceptionsUtils_isTerminatedWithNSException_(ex)) {
            [actual_ onErrorWithNSException:ex];
          }
          return;
        }
      }
      jboolean mainDone = JreLoadVolatileBoolean(&done_);
      id v = [((id<JavaUtilQueue>) nil_chk(queue_)) poll];
      jboolean empty = v == nil;
      if (mainDone && empty) {
        NSException *ex = RxInternalUtilExceptionsUtils_terminateWithJavaUtilConcurrentAtomicAtomicReference_(error_);
        if (ex == nil) {
          [actual_ onCompleted];
        }
        else if (!RxInternalUtilExceptionsUtils_isTerminatedWithNSException_(ex)) {
          [actual_ onErrorWithNSException:ex];
        }
        return;
      }
      if (!empty) {
        RxObservable *source;
        @try {
          source = [((id<RxFunctionsFunc1>) nil_chk(mapper_)) callWithId:RxInternalOperatorsNotificationLite_getValueWithId_(v)];
        }
        @catch (NSException *mapperError) {
          RxExceptionsExceptions_throwIfFatalWithNSException_(mapperError);
          [self drainErrorWithNSException:mapperError];
          return;
        }
        if (source == nil) {
          [self drainErrorWithNSException:create_JavaLangNullPointerException_initWithNSString_(@"The source returned by the mapper was null")];
          return;
        }
        if (source != (id) RxObservable_empty()) {
          if ([source isKindOfClass:[RxInternalUtilScalarSynchronousObservable class]]) {
            RxInternalUtilScalarSynchronousObservable *scalarSource = (RxInternalUtilScalarSynchronousObservable *) cast_chk(source, [RxInternalUtilScalarSynchronousObservable class]);
            JreAssignVolatileBoolean(&active_, true);
            [((RxInternalProducersProducerArbiter *) nil_chk(arbiter_)) setProducerWithRxProducer:create_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer_initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_([scalarSource get], self)];
          }
          else {
            RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber *innerSubscriber = create_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(self);
            [((RxSubscriptionsSerialSubscription *) nil_chk(inner_)) setWithRxSubscription:innerSubscriber];
            if (![innerSubscriber isUnsubscribed]) {
              JreAssignVolatileBoolean(&active_, true);
              [source unsafeSubscribeWithRxSubscriber:innerSubscriber];
            }
            else {
              return;
            }
          }
          [self requestWithLong:1];
        }
        else {
          [self requestWithLong:1];
          continue;
        }
      }
    }
    if ([wip_ decrementAndGet] == 0) {
      break;
    }
  }
}

- (void)drainErrorWithNSException:(NSException *)mapperError {
  [self unsubscribe];
  if (RxInternalUtilExceptionsUtils_addThrowableWithJavaUtilConcurrentAtomicAtomicReference_withNSException_(error_, mapperError)) {
    NSException *ex = RxInternalUtilExceptionsUtils_terminateWithJavaUtilConcurrentAtomicAtomicReference_(error_);
    if (!RxInternalUtilExceptionsUtils_isTerminatedWithNSException_(ex)) {
      [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:ex];
    }
  }
  else {
    [self pluginErrorWithNSException:mapperError];
  }
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber class]);
  RELEASE_(actual_);
  RELEASE_(mapper_);
  RELEASE_(arbiter_);
  RELEASE_(queue_);
  RELEASE_(wip_);
  RELEASE_(error_);
  RELEASE_(inner_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 7, 8, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 9, 3, -1, 10, -1, -1 },
    { NULL, "V", 0x0, 11, 12, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 13, 8, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 14, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 15, 6, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withRxFunctionsFunc1:withInt:withInt:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(requestMoreWithLong:);
  methods[5].selector = @selector(innerNextWithId:);
  methods[6].selector = @selector(innerErrorWithNSException:withLong:);
  methods[7].selector = @selector(innerCompletedWithLong:);
  methods[8].selector = @selector(pluginErrorWithNSException:);
  methods[9].selector = @selector(drain);
  methods[10].selector = @selector(drainErrorWithNSException:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 16, -1 },
    { "mapper_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 17, -1 },
    { "delayErrorMode_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "arbiter_", "LRxInternalProducersProducerArbiter;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "queue_", "LJavaUtilQueue;", .constantValue.asLong = 0, 0x10, -1, -1, 18, -1 },
    { "wip_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "error_", "LJavaUtilConcurrentAtomicAtomicReference;", .constantValue.asLong = 0, 0x10, -1, -1, 19, -1 },
    { "inner_", "LRxSubscriptionsSerialSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "done_", "Z", .constantValue.asLong = 0, 0x40, -1, -1, -1, -1 },
    { "active_", "Z", .constantValue.asLong = 0, 0x40, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LRxFunctionsFunc1;II", "(Lrx/Subscriber<-TR;>;Lrx/functions/Func1<-TT;+Lrx/Observable<+TR;>;>;II)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "requestMore", "J", "innerNext", "(TR;)V", "innerError", "LNSException;J", "innerCompleted", "pluginError", "drainError", "Lrx/Subscriber<-TR;>;", "Lrx/functions/Func1<-TT;+Lrx/Observable<+TR;>;>;", "Ljava/util/Queue<Ljava/lang/Object;>;", "Ljava/util/concurrent/atomic/AtomicReference<Ljava/lang/Throwable;>;", "LRxInternalOperatorsOnSubscribeConcatMap;", "<T:Ljava/lang/Object;R:Ljava/lang/Object;>Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber = { "ConcatMapSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 11, 10, 20, -1, -1, 21, -1 };
  return &_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *self, RxSubscriber *actual, id<RxFunctionsFunc1> mapper, jint prefetch, jint delayErrorMode) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->actual_, actual);
  JreStrongAssign(&self->mapper_, mapper);
  self->delayErrorMode_ = delayErrorMode;
  JreStrongAssignAndConsume(&self->arbiter_, new_RxInternalProducersProducerArbiter_init());
  JreStrongAssignAndConsume(&self->wip_, new_JavaUtilConcurrentAtomicAtomicInteger_init());
  JreStrongAssignAndConsume(&self->error_, new_JavaUtilConcurrentAtomicAtomicReference_init());
  id<JavaUtilQueue> q;
  if (RxInternalUtilUnsafeUnsafeAccess_isUnsafeAvailable()) {
    q = create_RxInternalUtilUnsafeSpscArrayQueue_initWithInt_(prefetch);
  }
  else {
    q = create_RxInternalUtilAtomicSpscAtomicArrayQueue_initWithInt_(prefetch);
  }
  JreStrongAssign(&self->queue_, q);
  JreStrongAssignAndConsume(&self->inner_, new_RxSubscriptionsSerialSubscription_init());
  [self requestWithLong:prefetch];
}

RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *new_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_(RxSubscriber *actual, id<RxFunctionsFunc1> mapper, jint prefetch, jint delayErrorMode) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber, initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_, actual, mapper, prefetch, delayErrorMode)
}

RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *create_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_(RxSubscriber *actual, id<RxFunctionsFunc1> mapper, jint prefetch, jint delayErrorMode) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber, initWithRxSubscriber_withRxFunctionsFunc1_withInt_withInt_, actual, mapper, prefetch, delayErrorMode)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber)

@implementation RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber

- (instancetype)initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *)parent {
  RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(self, parent);
  return self;
}

- (void)setProducerWithRxProducer:(id<RxProducer>)p {
  [((RxInternalProducersProducerArbiter *) nil_chk(((RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *) nil_chk(parent_))->arbiter_)) setProducerWithRxProducer:p];
}

- (void)onNextWithId:(id)t {
  produced_++;
  [((RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *) nil_chk(parent_)) innerNextWithId:t];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *) nil_chk(parent_)) innerErrorWithNSException:e withLong:produced_];
}

- (void)onCompleted {
  [((RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *) nil_chk(parent_)) innerCompletedWithLong:produced_];
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber class]);
  RELEASE_(parent_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, 6, -1, -1 },
    { NULL, "V", 0x1, 7, 8, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:);
  methods[1].selector = @selector(setProducerWithRxProducer:);
  methods[2].selector = @selector(onNextWithId:);
  methods[3].selector = @selector(onErrorWithNSException:);
  methods[4].selector = @selector(onCompleted);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "parent_", "LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 9, -1 },
    { "produced_", "J", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;", "(Lrx/internal/operators/OnSubscribeConcatMap$ConcatMapSubscriber<TT;TR;>;)V", "setProducer", "LRxProducer;", "onNext", "LNSObject;", "(TR;)V", "onError", "LNSException;", "Lrx/internal/operators/OnSubscribeConcatMap$ConcatMapSubscriber<TT;TR;>;", "LRxInternalOperatorsOnSubscribeConcatMap;", "<T:Ljava/lang/Object;R:Ljava/lang/Object;>Lrx/Subscriber<TR;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber = { "ConcatMapInnerSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 5, 2, 10, -1, -1, 11, -1 };
  return &_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber *self, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->parent_, parent);
}

RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber *new_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber, initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_, parent)
}

RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber *create_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber, initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_, parent)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerSubscriber)

@implementation RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer

- (instancetype)initWithId:(id)value
withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *)parent {
  RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer_initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(self, value, parent);
  return self;
}

- (void)requestWithLong:(jlong)n {
  if (!once_ && n > 0LL) {
    once_ = true;
    RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *p = parent_;
    [((RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *) nil_chk(p)) innerNextWithId:value_];
    [p innerCompletedWithLong:1];
  }
}

- (void)__javaClone:(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer *)original {
  [super __javaClone:original];
  [parent_ release];
}

- (void)dealloc {
  RELEASE_(value_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithId:withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:);
  methods[1].selector = @selector(requestWithLong:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "value_", "LNSObject;", .constantValue.asLong = 0, 0x10, -1, -1, 4, -1 },
    { "parent_", "LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "once_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LNSObject;LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;", "(TR;Lrx/internal/operators/OnSubscribeConcatMap$ConcatMapSubscriber<TT;TR;>;)V", "request", "J", "TR;", "Lrx/internal/operators/OnSubscribeConcatMap$ConcatMapSubscriber<TT;TR;>;", "LRxInternalOperatorsOnSubscribeConcatMap;", "<T:Ljava/lang/Object;R:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Producer;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer = { "ConcatMapInnerScalarProducer", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 2, 3, 6, -1, -1, 7, -1 };
  return &_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer;
}

@end

void RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer_initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer *self, id value, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent) {
  NSObject_init(self);
  JreStrongAssign(&self->value_, value);
  self->parent_ = parent;
}

RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer *new_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer_initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(id value, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer, initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_, value, parent)
}

RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer *create_RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer_initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(id value, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *parent) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer, initWithId_withRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_, value, parent)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapInnerScalarProducer)

@implementation RxInternalOperatorsOnSubscribeConcatMap_$1

- (void)requestWithLong:(jlong)n {
  [((RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *) nil_chk(val$parent_)) requestMoreWithLong:n];
}

- (instancetype)initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *)capture$0 {
  RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$parent_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$parent_", "LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 4, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber;", "(Lrx/internal/operators/OnSubscribeConcatMap$ConcatMapSubscriber<TT;TR;>;)V", "Lrx/internal/operators/OnSubscribeConcatMap$ConcatMapSubscriber<TT;TR;>;", "LRxInternalOperatorsOnSubscribeConcatMap;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeConcatMap_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 5, -1, 6, -1, -1 };
  return &_RxInternalOperatorsOnSubscribeConcatMap_$1;
}

@end

void RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_$1 *self, RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *capture$0) {
  JreStrongAssign(&self->val$parent_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOnSubscribeConcatMap_$1 *new_RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeConcatMap_$1, initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_, capture$0)
}

RxInternalOperatorsOnSubscribeConcatMap_$1 *create_RxInternalOperatorsOnSubscribeConcatMap_$1_initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_(RxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeConcatMap_$1, initWithRxInternalOperatorsOnSubscribeConcatMap_ConcatMapSubscriber_, capture$0)
}
