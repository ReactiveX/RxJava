//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/CompletableSubscriber.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxCompletableSubscriber")
#ifdef RESTRICT_RxCompletableSubscriber
#define INCLUDE_ALL_RxCompletableSubscriber 0
#else
#define INCLUDE_ALL_RxCompletableSubscriber 1
#endif
#undef RESTRICT_RxCompletableSubscriber

#if !defined (RxCompletableSubscriber_) && (INCLUDE_ALL_RxCompletableSubscriber || defined(INCLUDE_RxCompletableSubscriber))
#define RxCompletableSubscriber_

@protocol RxSubscription;

@protocol RxCompletableSubscriber < JavaObject >

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onSubscribeWithRxSubscription:(id<RxSubscription>)d;

@end

J2OBJC_EMPTY_STATIC_INIT(RxCompletableSubscriber)

J2OBJC_TYPE_LITERAL_HEADER(RxCompletableSubscriber)

#endif

#pragma pop_macro("INCLUDE_ALL_RxCompletableSubscriber")
