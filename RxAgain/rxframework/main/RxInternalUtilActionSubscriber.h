//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/ActionSubscriber.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalUtilActionSubscriber")
#ifdef RESTRICT_RxInternalUtilActionSubscriber
#define INCLUDE_ALL_RxInternalUtilActionSubscriber 0
#else
#define INCLUDE_ALL_RxInternalUtilActionSubscriber 1
#endif
#undef RESTRICT_RxInternalUtilActionSubscriber

#if !defined (RxInternalUtilActionSubscriber_) && (INCLUDE_ALL_RxInternalUtilActionSubscriber || defined(INCLUDE_RxInternalUtilActionSubscriber))
#define RxInternalUtilActionSubscriber_

#define RESTRICT_RxSubscriber 1
#define INCLUDE_RxSubscriber 1
#include "RxSubscriber.h"

@protocol RxFunctionsAction0;
@protocol RxFunctionsAction1;

@interface RxInternalUtilActionSubscriber : RxSubscriber {
 @public
  id<RxFunctionsAction1> onNext_;
  id<RxFunctionsAction1> onError_;
  id<RxFunctionsAction0> onCompleted_;
}

#pragma mark Public

- (instancetype)initWithRxFunctionsAction1:(id<RxFunctionsAction1>)onNext
                    withRxFunctionsAction1:(id<RxFunctionsAction1>)onError
                    withRxFunctionsAction0:(id<RxFunctionsAction0>)onCompleted;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)t;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilActionSubscriber)

J2OBJC_FIELD_SETTER(RxInternalUtilActionSubscriber, onNext_, id<RxFunctionsAction1>)
J2OBJC_FIELD_SETTER(RxInternalUtilActionSubscriber, onError_, id<RxFunctionsAction1>)
J2OBJC_FIELD_SETTER(RxInternalUtilActionSubscriber, onCompleted_, id<RxFunctionsAction0>)

FOUNDATION_EXPORT void RxInternalUtilActionSubscriber_initWithRxFunctionsAction1_withRxFunctionsAction1_withRxFunctionsAction0_(RxInternalUtilActionSubscriber *self, id<RxFunctionsAction1> onNext, id<RxFunctionsAction1> onError, id<RxFunctionsAction0> onCompleted);

FOUNDATION_EXPORT RxInternalUtilActionSubscriber *new_RxInternalUtilActionSubscriber_initWithRxFunctionsAction1_withRxFunctionsAction1_withRxFunctionsAction0_(id<RxFunctionsAction1> onNext, id<RxFunctionsAction1> onError, id<RxFunctionsAction0> onCompleted) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilActionSubscriber *create_RxInternalUtilActionSubscriber_initWithRxFunctionsAction1_withRxFunctionsAction1_withRxFunctionsAction0_(id<RxFunctionsAction1> onNext, id<RxFunctionsAction1> onError, id<RxFunctionsAction0> onCompleted);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilActionSubscriber)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalUtilActionSubscriber")
