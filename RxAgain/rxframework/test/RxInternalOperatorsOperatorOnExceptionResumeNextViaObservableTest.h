//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorOnExceptionResumeNextViaObservableTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest")
#ifdef RESTRICT_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest
#define INCLUDE_ALL_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest

#if !defined (RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest_) && (INCLUDE_ALL_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest || defined(INCLUDE_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest))
#define RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest_

@interface RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)normalBackpressure;

- (void)testBackpressure;

- (void)testErrorPassesThru;

- (void)testMapResumeAsyncNext;

- (void)testResumeNextWithException;

- (void)testResumeNextWithRuntimeException;

- (void)testThrowablePassesThru;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest_init(RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest *new_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest *create_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorOnExceptionResumeNextViaObservableTest")
