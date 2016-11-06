//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/schedulers/ComputationSchedulerTests.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxSchedulersComputationSchedulerTests")
#ifdef RESTRICT_RxSchedulersComputationSchedulerTests
#define INCLUDE_ALL_RxSchedulersComputationSchedulerTests 0
#else
#define INCLUDE_ALL_RxSchedulersComputationSchedulerTests 1
#endif
#undef RESTRICT_RxSchedulersComputationSchedulerTests

#if !defined (RxSchedulersComputationSchedulerTests_) && (INCLUDE_ALL_RxSchedulersComputationSchedulerTests || defined(INCLUDE_RxSchedulersComputationSchedulerTests))
#define RxSchedulersComputationSchedulerTests_

#define RESTRICT_RxSchedulersAbstractSchedulerConcurrencyTests 1
#define INCLUDE_RxSchedulersAbstractSchedulerConcurrencyTests 1
#include "RxSchedulersAbstractSchedulerConcurrencyTests.h"

@class RxScheduler;

@interface RxSchedulersComputationSchedulerTests : RxSchedulersAbstractSchedulerConcurrencyTests

#pragma mark Public

- (instancetype)init;

- (void)testCancelledTaskRetention;

- (void)testComputationThreadPool1;

- (void)testHandledErrorIsNotDeliveredToThreadHandler;

- (void)testMergeWithExecutorScheduler;

- (void)testThreadSafetyWhenSchedulerIsHoppingBetweenThreads;

- (void)testUnhandledErrorIsDeliveredToThreadHandler;

#pragma mark Protected

- (RxScheduler *)getScheduler;

@end

J2OBJC_EMPTY_STATIC_INIT(RxSchedulersComputationSchedulerTests)

FOUNDATION_EXPORT void RxSchedulersComputationSchedulerTests_init(RxSchedulersComputationSchedulerTests *self);

FOUNDATION_EXPORT RxSchedulersComputationSchedulerTests *new_RxSchedulersComputationSchedulerTests_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxSchedulersComputationSchedulerTests *create_RxSchedulersComputationSchedulerTests_init();

J2OBJC_TYPE_LITERAL_HEADER(RxSchedulersComputationSchedulerTests)

#endif

#pragma pop_macro("INCLUDE_ALL_RxSchedulersComputationSchedulerTests")
