//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorMapNotification.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsFunc0.h"
#include "RxFunctionsFunc1.h"
#include "RxInternalOperatorsBackpressureUtils.h"
#include "RxInternalOperatorsOperatorMapNotification.h"
#include "RxProducer.h"
#include "RxSubscriber.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/IllegalStateException.h"
#include "java/util/concurrent/atomic/AtomicLong.h"
#include "java/util/concurrent/atomic/AtomicReference.h"

@interface RxInternalOperatorsOperatorMapNotification_$1 : NSObject < RxProducer > {
 @public
  RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *val$parent_;
}

- (void)requestWithLong:(jlong)n;

- (instancetype)initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber:(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorMapNotification_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorMapNotification_$1, val$parent_, RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(RxInternalOperatorsOperatorMapNotification_$1 *self, RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorMapNotification_$1 *new_RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorMapNotification_$1 *create_RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *capture$0);

@implementation RxInternalOperatorsOperatorMapNotification

- (instancetype)initWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)onNext
                    withRxFunctionsFunc1:(id<RxFunctionsFunc1>)onError
                    withRxFunctionsFunc0:(id<RxFunctionsFunc0>)onCompleted {
  RxInternalOperatorsOperatorMapNotification_initWithRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(self, onNext, onError, onCompleted);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *parent = create_RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(child, onNext_, onError_, onCompleted_);
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:parent];
  [child setProducerWithRxProducer:create_RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(parent)];
  return parent;
}

- (void)dealloc {
  RELEASE_(onNext_);
  RELEASE_(onError_);
  RELEASE_(onCompleted_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxFunctionsFunc1:withRxFunctionsFunc1:withRxFunctionsFunc0:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "onNext_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "onError_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
    { "onCompleted_", "LRxFunctionsFunc0;", .constantValue.asLong = 0, 0x10, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "LRxFunctionsFunc1;LRxFunctionsFunc1;LRxFunctionsFunc0;", "(Lrx/functions/Func1<-TT;+TR;>;Lrx/functions/Func1<-Ljava/lang/Throwable;+TR;>;Lrx/functions/Func0<+TR;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TR;>;)Lrx/Subscriber<-TT;>;", "Lrx/functions/Func1<-TT;+TR;>;", "Lrx/functions/Func1<-Ljava/lang/Throwable;+TR;>;", "Lrx/functions/Func0<+TR;>;", "LRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber;", "<T:Ljava/lang/Object;R:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TR;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMapNotification = { "OperatorMapNotification", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 3, -1, 8, -1, 9, -1 };
  return &_RxInternalOperatorsOperatorMapNotification;
}

@end

void RxInternalOperatorsOperatorMapNotification_initWithRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxInternalOperatorsOperatorMapNotification *self, id<RxFunctionsFunc1> onNext, id<RxFunctionsFunc1> onError, id<RxFunctionsFunc0> onCompleted) {
  NSObject_init(self);
  JreStrongAssign(&self->onNext_, onNext);
  JreStrongAssign(&self->onError_, onError);
  JreStrongAssign(&self->onCompleted_, onCompleted);
}

RxInternalOperatorsOperatorMapNotification *new_RxInternalOperatorsOperatorMapNotification_initWithRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(id<RxFunctionsFunc1> onNext, id<RxFunctionsFunc1> onError, id<RxFunctionsFunc0> onCompleted) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMapNotification, initWithRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_, onNext, onError, onCompleted)
}

RxInternalOperatorsOperatorMapNotification *create_RxInternalOperatorsOperatorMapNotification_initWithRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(id<RxFunctionsFunc1> onNext, id<RxFunctionsFunc1> onError, id<RxFunctionsFunc0> onCompleted) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMapNotification, initWithRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_, onNext, onError, onCompleted)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorMapNotification)

@implementation RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)onNext
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)onError
                withRxFunctionsFunc0:(id<RxFunctionsFunc0>)onCompleted {
  RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(self, actual, onNext, onError, onCompleted);
  return self;
}

- (void)onNextWithId:(id)t {
  @try {
    produced_++;
    [((RxSubscriber *) nil_chk(actual_)) onNextWithId:[((id<RxFunctionsFunc1>) nil_chk(onNext_)) callWithId:t]];
  }
  @catch (NSException *ex) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_withId_(ex, actual_, t);
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  [self accountProduced];
  @try {
    JreStrongAssign(&value_, [((id<RxFunctionsFunc1>) nil_chk(onError_)) callWithId:e]);
  }
  @catch (NSException *ex) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_withId_(ex, actual_, e);
  }
  [self tryEmit];
}

- (void)onCompleted {
  [self accountProduced];
  @try {
    JreStrongAssign(&value_, [((id<RxFunctionsFunc0>) nil_chk(onCompleted_)) call]);
  }
  @catch (NSException *ex) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_(ex, actual_);
  }
  [self tryEmit];
}

- (void)accountProduced {
  jlong p = produced_;
  if (p != 0LL && [((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(producer_MapNotificationSubscriber_)) get] != nil) {
    RxInternalOperatorsBackpressureUtils_producedWithJavaUtilConcurrentAtomicAtomicLong_withLong_(requested_MapNotificationSubscriber_, p);
  }
}

- (void)setProducerWithRxProducer:(id<RxProducer>)p {
  if ([((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(producer_MapNotificationSubscriber_)) compareAndSetWithId:nil withId:p]) {
    jlong r = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(missedRequested_)) getAndSetWithLong:0LL];
    if (r != 0LL) {
      [((id<RxProducer>) nil_chk(p)) requestWithLong:r];
    }
  }
  else {
    @throw create_JavaLangIllegalStateException_initWithNSString_(@"Producer already set!");
  }
}

- (void)tryEmit {
  for (; ; ) {
    jlong r = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(requested_MapNotificationSubscriber_)) get];
    if ((r & RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_COMPLETED_FLAG) != 0) {
      break;
    }
    if ([requested_MapNotificationSubscriber_ compareAndSetWithLong:r withLong:r | RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_COMPLETED_FLAG]) {
      if (r != 0 || [((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(producer_MapNotificationSubscriber_)) get] == nil) {
        if (![((RxSubscriber *) nil_chk(actual_)) isUnsubscribed]) {
          [actual_ onNextWithId:value_];
        }
        if (![actual_ isUnsubscribed]) {
          [actual_ onCompleted];
        }
      }
      return;
    }
  }
}

- (void)requestInnerWithLong:(jlong)n {
  if (n < 0LL) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$J", @"n >= 0 required but it was ", n));
  }
  if (n == 0LL) {
    return;
  }
  for (; ; ) {
    jlong r = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(requested_MapNotificationSubscriber_)) get];
    if ((r & RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_COMPLETED_FLAG) != 0LL) {
      jlong v = r & RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_REQUESTED_MASK;
      jlong u = RxInternalOperatorsBackpressureUtils_addCapWithLong_withLong_(v, n) | RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_COMPLETED_FLAG;
      if ([requested_MapNotificationSubscriber_ compareAndSetWithLong:r withLong:u]) {
        if (v == 0LL) {
          if (![((RxSubscriber *) nil_chk(actual_)) isUnsubscribed]) {
            [actual_ onNextWithId:value_];
          }
          if (![actual_ isUnsubscribed]) {
            [actual_ onCompleted];
          }
        }
        return;
      }
    }
    else {
      jlong u = RxInternalOperatorsBackpressureUtils_addCapWithLong_withLong_(r, n);
      if ([requested_MapNotificationSubscriber_ compareAndSetWithLong:r withLong:u]) {
        break;
      }
    }
  }
  JavaUtilConcurrentAtomicAtomicReference *localProducer = producer_MapNotificationSubscriber_;
  id<RxProducer> actualProducer = [((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(localProducer)) get];
  if (actualProducer != nil) {
    [actualProducer requestWithLong:n];
  }
  else {
    RxInternalOperatorsBackpressureUtils_getAndAddRequestWithJavaUtilConcurrentAtomicAtomicLong_withLong_(missedRequested_, n);
    actualProducer = [localProducer get];
    if (actualProducer != nil) {
      jlong r = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(missedRequested_)) getAndSetWithLong:0LL];
      if (r != 0LL) {
        [actualProducer requestWithLong:r];
      }
    }
  }
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber class]);
  RELEASE_(actual_);
  RELEASE_(onNext_);
  RELEASE_(onError_);
  RELEASE_(onCompleted_);
  RELEASE_(requested_MapNotificationSubscriber_);
  RELEASE_(missedRequested_);
  RELEASE_(producer_MapNotificationSubscriber_);
  RELEASE_(value_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 7, 8, -1, -1, -1, -1 },
    { NULL, "V", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 9, 10, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withRxFunctionsFunc1:withRxFunctionsFunc1:withRxFunctionsFunc0:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(accountProduced);
  methods[5].selector = @selector(setProducerWithRxProducer:);
  methods[6].selector = @selector(tryEmit);
  methods[7].selector = @selector(requestInnerWithLong:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 11, -1 },
    { "onNext_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 12, -1 },
    { "onError_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 13, -1 },
    { "onCompleted_", "LRxFunctionsFunc0;", .constantValue.asLong = 0, 0x10, -1, -1, 14, -1 },
    { "requested_MapNotificationSubscriber_", "LJavaUtilConcurrentAtomicAtomicLong;", .constantValue.asLong = 0, 0x10, 15, -1, -1, -1 },
    { "missedRequested_", "LJavaUtilConcurrentAtomicAtomicLong;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "producer_MapNotificationSubscriber_", "LJavaUtilConcurrentAtomicAtomicReference;", .constantValue.asLong = 0, 0x10, 16, -1, 17, -1 },
    { "produced_", "J", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "value_", "LNSObject;", .constantValue.asLong = 0, 0x0, -1, -1, 18, -1 },
    { "COMPLETED_FLAG", "J", .constantValue.asLong = RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_COMPLETED_FLAG, 0x18, -1, -1, -1, -1 },
    { "REQUESTED_MASK", "J", .constantValue.asLong = RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_REQUESTED_MASK, 0x18, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LRxFunctionsFunc1;LRxFunctionsFunc1;LRxFunctionsFunc0;", "(Lrx/Subscriber<-TR;>;Lrx/functions/Func1<-TT;+TR;>;Lrx/functions/Func1<-Ljava/lang/Throwable;+TR;>;Lrx/functions/Func0<+TR;>;)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "setProducer", "LRxProducer;", "requestInner", "J", "Lrx/Subscriber<-TR;>;", "Lrx/functions/Func1<-TT;+TR;>;", "Lrx/functions/Func1<-Ljava/lang/Throwable;+TR;>;", "Lrx/functions/Func0<+TR;>;", "requested", "producer", "Ljava/util/concurrent/atomic/AtomicReference<Lrx/Producer;>;", "TR;", "LRxInternalOperatorsOperatorMapNotification;", "<T:Ljava/lang/Object;R:Ljava/lang/Object;>Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber = { "MapNotificationSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 8, 11, 19, -1, -1, 20, -1 };
  return &_RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber;
}

@end

void RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *self, RxSubscriber *actual, id<RxFunctionsFunc1> onNext, id<RxFunctionsFunc1> onError, id<RxFunctionsFunc0> onCompleted) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->actual_, actual);
  JreStrongAssign(&self->onNext_, onNext);
  JreStrongAssign(&self->onError_, onError);
  JreStrongAssign(&self->onCompleted_, onCompleted);
  JreStrongAssignAndConsume(&self->requested_MapNotificationSubscriber_, new_JavaUtilConcurrentAtomicAtomicLong_init());
  JreStrongAssignAndConsume(&self->missedRequested_, new_JavaUtilConcurrentAtomicAtomicLong_init());
  JreStrongAssignAndConsume(&self->producer_MapNotificationSubscriber_, new_JavaUtilConcurrentAtomicAtomicReference_init());
}

RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *new_RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxSubscriber *actual, id<RxFunctionsFunc1> onNext, id<RxFunctionsFunc1> onError, id<RxFunctionsFunc0> onCompleted) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber, initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_, actual, onNext, onError, onCompleted)
}

RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *create_RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxSubscriber *actual, id<RxFunctionsFunc1> onNext, id<RxFunctionsFunc1> onError, id<RxFunctionsFunc0> onCompleted) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber, initWithRxSubscriber_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_, actual, onNext, onError, onCompleted)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber)

@implementation RxInternalOperatorsOperatorMapNotification_$1

- (void)requestWithLong:(jlong)n {
  [((RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *) nil_chk(val$parent_)) requestInnerWithLong:n];
}

- (instancetype)initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber:(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *)capture$0 {
  RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(self, capture$0);
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
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$parent_", "LRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 4, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber;", "(Lrx/internal/operators/OperatorMapNotification$MapNotificationSubscriber<TT;TR;>;)V", "Lrx/internal/operators/OperatorMapNotification$MapNotificationSubscriber<TT;TR;>;", "LRxInternalOperatorsOperatorMapNotification;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMapNotification_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 5, -1, 6, -1, -1 };
  return &_RxInternalOperatorsOperatorMapNotification_$1;
}

@end

void RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(RxInternalOperatorsOperatorMapNotification_$1 *self, RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *capture$0) {
  JreStrongAssign(&self->val$parent_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOperatorMapNotification_$1 *new_RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMapNotification_$1, initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_, capture$0)
}

RxInternalOperatorsOperatorMapNotification_$1 *create_RxInternalOperatorsOperatorMapNotification_$1_initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_(RxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMapNotification_$1, initWithRxInternalOperatorsOperatorMapNotification_MapNotificationSubscriber_, capture$0)
}
