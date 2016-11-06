//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorSubscribeOnTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest")
#ifdef RESTRICT_RxInternalOperatorsOperatorSubscribeOnTest
#define INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorSubscribeOnTest

#if !defined (RxInternalOperatorsOperatorSubscribeOnTest_) && (INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest || defined(INCLUDE_RxInternalOperatorsOperatorSubscribeOnTest))
#define RxInternalOperatorsOperatorSubscribeOnTest_

@interface RxInternalOperatorsOperatorSubscribeOnTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)testBackpressureReschedulesCorrectly;

- (void)testIssue813;

- (void)testOnError;

- (void)testSetProducerSynchronousRequest;

- (void)testThrownErrorHandling;

- (void)testUnsubscribeInfiniteStream;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSubscribeOnTest)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorSubscribeOnTest_init(RxInternalOperatorsOperatorSubscribeOnTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorSubscribeOnTest *new_RxInternalOperatorsOperatorSubscribeOnTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorSubscribeOnTest *create_RxInternalOperatorsOperatorSubscribeOnTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorSubscribeOnTest)

#endif

#if !defined (RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_) && (INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest || defined(INCLUDE_RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler))
#define RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_

#define RESTRICT_RxScheduler 1
#define INCLUDE_RxScheduler 1
#include "RxScheduler.h"

@class JavaUtilConcurrentTimeUnit;
@class RxScheduler_Worker;

@interface RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler : RxScheduler {
 @public
  RxScheduler *actual_;
  jlong delay_;
  JavaUtilConcurrentTimeUnit *unit_;
}

#pragma mark Public

- (instancetype)init;

- (instancetype)initWithRxScheduler:(RxScheduler *)actual
                           withLong:(jlong)delay
     withJavaUtilConcurrentTimeUnit:(JavaUtilConcurrentTimeUnit *)unit;

- (RxScheduler_Worker *)createWorker;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler, actual_, RxScheduler *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler, unit_, JavaUtilConcurrentTimeUnit *)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_init(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler *new_RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler *create_RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_init();

FOUNDATION_EXPORT void RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_initWithRxScheduler_withLong_withJavaUtilConcurrentTimeUnit_(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler *self, RxScheduler *actual, jlong delay, JavaUtilConcurrentTimeUnit *unit);

FOUNDATION_EXPORT RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler *new_RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_initWithRxScheduler_withLong_withJavaUtilConcurrentTimeUnit_(RxScheduler *actual, jlong delay, JavaUtilConcurrentTimeUnit *unit) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler *create_RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_initWithRxScheduler_withLong_withJavaUtilConcurrentTimeUnit_(RxScheduler *actual, jlong delay, JavaUtilConcurrentTimeUnit *unit);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler)

#endif

#if !defined (RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_SlowInner_) && (INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest || defined(INCLUDE_RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_SlowInner))
#define RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_SlowInner_

#define RESTRICT_RxScheduler 1
#define INCLUDE_RxScheduler_Worker 1
#include "RxScheduler.h"

@class JavaUtilConcurrentTimeUnit;
@protocol RxFunctionsAction0;
@protocol RxSubscription;

@interface RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_SlowInner : RxScheduler_Worker

#pragma mark Public

- (jboolean)isUnsubscribed;

- (id<RxSubscription>)scheduleWithRxFunctionsAction0:(id<RxFunctionsAction0>)action;

- (id<RxSubscription>)scheduleWithRxFunctionsAction0:(id<RxFunctionsAction0>)action
                                            withLong:(jlong)delayTime
                      withJavaUtilConcurrentTimeUnit:(JavaUtilConcurrentTimeUnit *)delayUnit;

- (void)unsubscribe;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_SlowInner)

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorSubscribeOnTest_SlowScheduler_SlowInner)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorSubscribeOnTest")
