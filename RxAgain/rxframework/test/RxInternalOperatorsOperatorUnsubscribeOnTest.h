//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorUnsubscribeOnTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorUnsubscribeOnTest")
#ifdef RESTRICT_RxInternalOperatorsOperatorUnsubscribeOnTest
#define INCLUDE_ALL_RxInternalOperatorsOperatorUnsubscribeOnTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorUnsubscribeOnTest 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorUnsubscribeOnTest

#if !defined (RxInternalOperatorsOperatorUnsubscribeOnTest_) && (INCLUDE_ALL_RxInternalOperatorsOperatorUnsubscribeOnTest || defined(INCLUDE_RxInternalOperatorsOperatorUnsubscribeOnTest))
#define RxInternalOperatorsOperatorUnsubscribeOnTest_

@interface RxInternalOperatorsOperatorUnsubscribeOnTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)testUnsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnDifferentThreads;

- (void)testUnsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnSameThread;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorUnsubscribeOnTest)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorUnsubscribeOnTest_init(RxInternalOperatorsOperatorUnsubscribeOnTest *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorUnsubscribeOnTest *new_RxInternalOperatorsOperatorUnsubscribeOnTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorUnsubscribeOnTest *create_RxInternalOperatorsOperatorUnsubscribeOnTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorUnsubscribeOnTest)

#endif

#if !defined (RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler_) && (INCLUDE_ALL_RxInternalOperatorsOperatorUnsubscribeOnTest || defined(INCLUDE_RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler))
#define RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler_

#define RESTRICT_RxScheduler 1
#define INCLUDE_RxScheduler 1
#include "RxScheduler.h"

@class JavaLangThread;
@class RxScheduler_Worker;

@interface RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler : RxScheduler {
 @public
  RxScheduler *single_;
}

#pragma mark Public

- (instancetype)init;

- (RxScheduler_Worker *)createWorker;

- (JavaLangThread *)getThread;

- (void)shutdown;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler, single_, RxScheduler *)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler_init(RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler *new_RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler *create_RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorUnsubscribeOnTest_UIEventLoopScheduler)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorUnsubscribeOnTest")
