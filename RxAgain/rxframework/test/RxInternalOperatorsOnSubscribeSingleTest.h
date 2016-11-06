//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OnSubscribeSingleTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeSingleTest")
#ifdef RESTRICT_RxInternalOperatorsOnSubscribeSingleTest
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeSingleTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeSingleTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOnSubscribeSingleTest

#if !defined (RxInternalOperatorsOnSubscribeSingleTest_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeSingleTest || defined(INCLUDE_RxInternalOperatorsOnSubscribeSingleTest))
#define RxInternalOperatorsOnSubscribeSingleTest_

@interface RxInternalOperatorsOnSubscribeSingleTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)testEmptyObservable;

- (void)testErrorObservable;

- (void)testJustSingleItemObservable;

- (void)testJustTwoEmissionsObservableThrowsError;

- (void)testRepeatObservableThrowsError;

- (void)testShouldUseUnsafeSubscribeInternallyNotSubscribe;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeSingleTest)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeSingleTest_init(RxInternalOperatorsOnSubscribeSingleTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeSingleTest *new_RxInternalOperatorsOnSubscribeSingleTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeSingleTest *create_RxInternalOperatorsOnSubscribeSingleTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeSingleTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeSingleTest")
