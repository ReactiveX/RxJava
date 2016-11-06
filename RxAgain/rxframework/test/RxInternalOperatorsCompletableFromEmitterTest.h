//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/CompletableFromEmitterTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsCompletableFromEmitterTest")
#ifdef RESTRICT_RxInternalOperatorsCompletableFromEmitterTest
#define INCLUDE_ALL_RxInternalOperatorsCompletableFromEmitterTest 0
#else
#define INCLUDE_ALL_RxInternalOperatorsCompletableFromEmitterTest 1
#endif
#undef RESTRICT_RxInternalOperatorsCompletableFromEmitterTest

#if !defined (RxInternalOperatorsCompletableFromEmitterTest_) && (INCLUDE_ALL_RxInternalOperatorsCompletableFromEmitterTest || defined(INCLUDE_RxInternalOperatorsCompletableFromEmitterTest))
#define RxInternalOperatorsCompletableFromEmitterTest_

@interface RxInternalOperatorsCompletableFromEmitterTest : NSObject

#pragma mark Public

- (instancetype)init;

- (void)concurrentUse;

- (void)ensureProtocol1;

- (void)ensureProtocol2;

- (void)error;

- (void)normal;

- (void)producerCrashes;

- (void)producerCrashesAfterSignal;

- (void)resourceCleanupCancellable;

- (void)resourceCleanupError;

- (void)resourceCleanupNormal;

- (void)resourceCleanupOnCompleteCrashes;

- (void)resourceCleanupOnErrorCrashes;

- (void)resourceCleanupUnsubscirbe;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsCompletableFromEmitterTest)

FOUNDATION_EXPORT void RxInternalOperatorsCompletableFromEmitterTest_init(RxInternalOperatorsCompletableFromEmitterTest *self);

FOUNDATION_EXPORT RxInternalOperatorsCompletableFromEmitterTest *new_RxInternalOperatorsCompletableFromEmitterTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsCompletableFromEmitterTest *create_RxInternalOperatorsCompletableFromEmitterTest_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsCompletableFromEmitterTest)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsCompletableFromEmitterTest")
