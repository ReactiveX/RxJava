//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorDebounceTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorDebounceTest")
#ifdef RESTRICT_RxInternalOperatorsOperatorDebounceTest
#define INCLUDE_ALL_RxInternalOperatorsOperatorDebounceTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorDebounceTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorDebounceTest

#if !defined (RxInternalOperatorsOperatorDebounceTest_) && (INCLUDE_ALL_RxInternalOperatorsOperatorDebounceTest || defined(INCLUDE_RxInternalOperatorsOperatorDebounceTest))
#define RxInternalOperatorsOperatorDebounceTest_

@interface RxInternalOperatorsOperatorDebounceTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)before;

- (void)debounceDefaultScheduler;

- (void)debounceSelectorFuncThrows;

- (void)debounceSelectorLastIsNotLost;

- (void)debounceSelectorNormal1;

- (void)debounceSelectorObservableThrows;

- (void)debounceTimedLastIsNotLost;

- (void)debounceWithTimeBackpressure;

- (void)testDebounceNeverEmits;

- (void)testDebounceWithCompleted;

- (void)testDebounceWithError;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDebounceTest)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorDebounceTest_init(RxInternalOperatorsOperatorDebounceTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorDebounceTest *new_RxInternalOperatorsOperatorDebounceTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorDebounceTest *create_RxInternalOperatorsOperatorDebounceTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorDebounceTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorDebounceTest")
