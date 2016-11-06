//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/subscriptions/MultipleAssignmentSubscriptionTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxSubscriptionsMultipleAssignmentSubscriptionTest")
#ifdef RESTRICT_RxSubscriptionsMultipleAssignmentSubscriptionTest
#define INCLUDE_ALL_RxSubscriptionsMultipleAssignmentSubscriptionTest 0
#else
#define INCLUDE_ALL_RxSubscriptionsMultipleAssignmentSubscriptionTest 1
#endif
#undef RESTRICT_RxSubscriptionsMultipleAssignmentSubscriptionTest

#if !defined (RxSubscriptionsMultipleAssignmentSubscriptionTest_) && (INCLUDE_ALL_RxSubscriptionsMultipleAssignmentSubscriptionTest || defined(INCLUDE_RxSubscriptionsMultipleAssignmentSubscriptionTest))
#define RxSubscriptionsMultipleAssignmentSubscriptionTest_

@protocol RxFunctionsAction0;
@protocol RxSubscription;

@interface RxSubscriptionsMultipleAssignmentSubscriptionTest : NSObject {
 @public
  id<RxFunctionsAction0> unsubscribe_;
  id<RxSubscription> s_;
}

#pragma mark Public

- (instancetype)init;

- (void)before;

- (void)subscribingWhenUnsubscribedCausesImmediateUnsubscription;

- (void)subscriptionDoesntRemainAfterUnsubscribe;

- (void)testNoUnsubscribeWhenReplaced;

- (void)testSubscriptionRemainsAfterUnsubscribe;

- (void)testUnsubscribeWhenParentUnsubscribes;

@end

J2OBJC_EMPTY_STATIC_INIT(RxSubscriptionsMultipleAssignmentSubscriptionTest)

J2OBJC_FIELD_SETTER(RxSubscriptionsMultipleAssignmentSubscriptionTest, unsubscribe_, id<RxFunctionsAction0>)
J2OBJC_FIELD_SETTER(RxSubscriptionsMultipleAssignmentSubscriptionTest, s_, id<RxSubscription>)

FOUNDATION_EXPORT void RxSubscriptionsMultipleAssignmentSubscriptionTest_init(RxSubscriptionsMultipleAssignmentSubscriptionTest *self);

FOUNDATION_EXPORT RxSubscriptionsMultipleAssignmentSubscriptionTest *new_RxSubscriptionsMultipleAssignmentSubscriptionTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxSubscriptionsMultipleAssignmentSubscriptionTest *create_RxSubscriptionsMultipleAssignmentSubscriptionTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxSubscriptionsMultipleAssignmentSubscriptionTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxSubscriptionsMultipleAssignmentSubscriptionTest")
