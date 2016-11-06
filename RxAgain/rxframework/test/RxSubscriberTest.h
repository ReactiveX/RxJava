//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/SubscriberTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxSubscriberTest")
#ifdef RESTRICT_RxSubscriberTest
#define INCLUDE_ALL_RxSubscriberTest 0
#else
#define INCLUDE_ALL_RxSubscriberTest 1
#endif
#undef RESTRICT_RxSubscriberTest

#if !defined (RxSubscriberTest_) && (INCLUDE_ALL_RxSubscriberTest || defined(INCLUDE_RxSubscriberTest))
#define RxSubscriberTest_

@interface RxSubscriberTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)testNegativeRequestThrowsIllegalArgumentException;

- (void)testOnStartCalledOnceViaLift;

- (void)testOnStartCalledOnceViaSubscribe;

- (void)testOnStartCalledOnceViaUnsafeSubscribe;

- (void)testOnStartRequestsAreAdditive;

- (void)testOnStartRequestsAreAdditiveAndOverflowBecomesMaxValue;

- (void)testRequestFromChainedOperator;

- (void)testRequestFromDecoupledOperator;

- (void)testRequestFromDecoupledOperatorThatRequestsN;

- (void)testRequestFromFinalSubscribeWithoutRequestValue;

- (void)testRequestFromFinalSubscribeWithRequestValue;

- (void)testRequestThroughMap;

- (void)testRequestThroughTakeThatReducesRequest;

- (void)testRequestThroughTakeWhereRequestIsSmallerThanTake;

- (void)testRequestToObservable;

@end

J2OBJC_EMPTY_STATIC_INIT(RxSubscriberTest)

FOUNDATION_EXPORT void RxSubscriberTest_init(RxSubscriberTest *self);

FOUNDATION_EXPORT RxSubscriberTest *new_RxSubscriberTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxSubscriberTest *create_RxSubscriberTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxSubscriberTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxSubscriberTest")
