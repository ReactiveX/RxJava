//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/observers/TestSubscriberTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxObserversTestSubscriberTest")
#ifdef RESTRICT_RxObserversTestSubscriberTest
#define INCLUDE_ALL_RxObserversTestSubscriberTest 0
#else
#define INCLUDE_ALL_RxObserversTestSubscriberTest 1
#endif
#undef RESTRICT_RxObserversTestSubscriberTest

#if !defined (RxObserversTestSubscriberTest_) && (INCLUDE_ALL_RxObserversTestSubscriberTest || defined(INCLUDE_RxObserversTestSubscriberTest))
#define RxObserversTestSubscriberTest_

@class JavaLangAssertionError;

@interface RxObserversTestSubscriberTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)assertAndConsume;

+ (void)assertExceptionWithJavaLangAssertionError:(JavaLangAssertionError *)e
                                     withNSString:(NSString *)message;

- (void)assertionFailureGivesActiveDetails;

- (void)assertionFailureShowsCompletion;

- (void)assertionFailureShowsMultipleCompletions;

- (void)assertionFailureShowsMultipleErrors;

- (void)assertValuesShouldThrowIfNumberOfItemsDoesNotMatch;

- (void)awaitValueCount;

- (void)awaitValueCountFails;

- (void)completionCount;

- (void)testAssert;

- (void)testAssertError;

- (void)testAssertNotMatchCount;

- (void)testAssertNotMatchValue;

- (void)testAssertTerminalEventNotReceived;

- (void)testAwaitTerminalEventWithDuration;

- (void)testAwaitTerminalEventWithDurationAndUnsubscribeOnTimeout;

- (void)testCompleted;

- (void)testDelegate1;

- (void)testDelegate2;

- (void)testDelegate3;

- (void)testDifferentError;

- (void)testDifferentError2;

- (void)testDifferentError3;

- (void)testInterruptTerminalEventAwait;

- (void)testInterruptTerminalEventAwaitAndUnsubscribe;

- (void)testInterruptTerminalEventAwaitTimed;

- (void)testMultipleCompletions;

- (void)testMultipleCompletions2;

- (void)testMultipleErrors;

- (void)testMultipleErrors2;

- (void)testMultipleErrors3;

- (void)testNoError;

- (void)testNoError2;

- (void)testNoErrors;

- (void)testNotCompleted;

- (void)testNoTerminalEventBut1Completed;

- (void)testNoTerminalEventBut1Error;

- (void)testNoTerminalEventBut1Error1Completed;

- (void)testNoTerminalEventBut2Errors;

- (void)testNoValues;

- (void)testNullDelegate1;

- (void)testNullDelegate2;

- (void)testNullDelegate3;

- (void)testOnCompletedCrashCountsDownLatch;

- (void)testOnErrorCrashCountsDownLatch;

- (void)testUnsubscribed;

- (void)testValueCount;

- (void)testWrappingMock;

- (void)testWrappingMockWhenUnsubscribeInvolved;

@end

J2OBJC_EMPTY_STATIC_INIT(RxObserversTestSubscriberTest)

FOUNDATION_EXPORT void RxObserversTestSubscriberTest_assertExceptionWithJavaLangAssertionError_withNSString_(JavaLangAssertionError *e, NSString *message);

FOUNDATION_EXPORT void RxObserversTestSubscriberTest_init(RxObserversTestSubscriberTest *self);

FOUNDATION_EXPORT RxObserversTestSubscriberTest *new_RxObserversTestSubscriberTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxObserversTestSubscriberTest *create_RxObserversTestSubscriberTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxObserversTestSubscriberTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxObserversTestSubscriberTest")
