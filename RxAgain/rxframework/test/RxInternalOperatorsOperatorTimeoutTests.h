//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorTimeoutTests.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOperatorTimeoutTests")
#ifdef RESTRICT_RxInternalOperatorsOperatorTimeoutTests
#define INCLUDE_ALL_RxInternalOperatorsOperatorTimeoutTests 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOperatorTimeoutTests 1
#endif
#undef RESTRICT_RxInternalOperatorsOperatorTimeoutTests

#if !defined (RxInternalOperatorsOperatorTimeoutTests_) && (INCLUDE_ALL_RxInternalOperatorsOperatorTimeoutTests || defined(INCLUDE_RxInternalOperatorsOperatorTimeoutTests))
#define RxInternalOperatorsOperatorTimeoutTests_

@interface RxInternalOperatorsOperatorTimeoutTests : NSObject

#pragma mark Public

- (instancetype)init;

- (void)setUp;

- (void)shouldCompleteIfUnderlyingCompletes;

- (void)shouldErrorIfUnderlyingErrors;

- (void)shouldNotTimeoutIfOnNextWithinTimeout;

- (void)shouldNotTimeoutIfSecondOnNextWithinTimeout;

- (void)shouldSwitchToOtherAndCanBeUnsubscribedIfOnNextNotWithinTimeout;

- (void)shouldSwitchToOtherIfOnCompletedNotWithinTimeout;

- (void)shouldSwitchToOtherIfOnErrorNotWithinTimeout;

- (void)shouldSwitchToOtherIfOnNextNotWithinTimeout;

- (void)shouldTimeoutIfOnNextNotWithinTimeout;

- (void)shouldTimeoutIfSecondOnNextNotWithinTimeout;

- (void)shouldTimeoutIfSynchronizedObservableEmitFirstOnNextNotWithinTimeout;

- (void)shouldUnsubscribeFromUnderlyingSubscriptionOnImmediatelyComplete;

- (void)shouldUnsubscribeFromUnderlyingSubscriptionOnImmediatelyErrored;

- (void)shouldUnsubscribeFromUnderlyingSubscriptionOnTimeout;

- (void)withDefaultScheduler;

- (void)withDefaultSchedulerAndOther;

- (void)withSelector;

- (void)withSelectorAndDefault;

- (void)withSelectorAndDefault2;

@end

J2OBJC_STATIC_INIT(RxInternalOperatorsOperatorTimeoutTests)

FOUNDATION_EXPORT void RxInternalOperatorsOperatorTimeoutTests_init(RxInternalOperatorsOperatorTimeoutTests *self);

FOUNDATION_EXPORT RxInternalOperatorsOperatorTimeoutTests *new_RxInternalOperatorsOperatorTimeoutTests_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOperatorTimeoutTests *create_RxInternalOperatorsOperatorTimeoutTests_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorTimeoutTests)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOperatorTimeoutTests")
