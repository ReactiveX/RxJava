//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorSkipTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorSkipTest")
#ifdef RESTRICT_RxInternalOperatorsOperatorSkipTest
#define INCLUDE_ALL_RxInternalOperatorsOperatorSkipTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorSkipTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorSkipTest

#if !defined (RxInternalOperatorsOperatorSkipTest_) && (INCLUDE_ALL_RxInternalOperatorsOperatorSkipTest || defined(INCLUDE_RxInternalOperatorsOperatorSkipTest))
#define RxInternalOperatorsOperatorSkipTest_

@interface RxInternalOperatorsOperatorSkipTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)skipDefaultScheduler;

- (void)testBackpressureMultipleSmallAsyncRequests;

- (void)testRequestOverflowDoesNotOccur;

- (void)testSkipEmptyStream;

- (void)testSkipError;

- (void)testSkipMultipleObservers;

- (void)testSkipNegativeElements;

- (void)testSkipOneElement;

- (void)testSkipTwoElements;

- (void)testSkipZeroElements;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSkipTest)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorSkipTest_init(RxInternalOperatorsOperatorSkipTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorSkipTest *new_RxInternalOperatorsOperatorSkipTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorSkipTest *create_RxInternalOperatorsOperatorSkipTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorSkipTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorSkipTest")
