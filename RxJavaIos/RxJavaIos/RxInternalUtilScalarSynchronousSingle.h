//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/ScalarSynchronousSingle.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle")
#ifdef RESTRICT_RxInternalUtilScalarSynchronousSingle
#define INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle 0
#else
#define INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle 1
#endif
#undef RESTRICT_RxInternalUtilScalarSynchronousSingle

#if !defined (RxInternalUtilScalarSynchronousSingle_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle || defined(INCLUDE_RxInternalUtilScalarSynchronousSingle))
#define RxInternalUtilScalarSynchronousSingle_

#define RESTRICT_RxSingle 1
#define INCLUDE_RxSingle 1
#include "RxSingle.h"

@class RxScheduler;
@protocol RxFunctionsFunc1;

@interface RxInternalUtilScalarSynchronousSingle : RxSingle {
 @public
  id value_;
}

#pragma mark Public

+ (RxInternalUtilScalarSynchronousSingle *)createWithId:(id)t;

- (id)get;

- (RxSingle *)scalarFlatMapWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)func;

- (RxSingle *)scalarScheduleOnWithRxScheduler:(RxScheduler *)scheduler;

#pragma mark Protected

- (instancetype)initWithId:(id)t;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousSingle)

J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousSingle, value_, id)

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle *RxInternalUtilScalarSynchronousSingle_createWithId_(id t);

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousSingle_initWithId_(RxInternalUtilScalarSynchronousSingle *self, id t);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle *new_RxInternalUtilScalarSynchronousSingle_initWithId_(id t) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle *create_RxInternalUtilScalarSynchronousSingle_initWithId_(id t);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousSingle)

#endif

#if !defined (RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle || defined(INCLUDE_RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission))
#define RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission_

#define RESTRICT_RxSingle 1
#define INCLUDE_RxSingle_OnSubscribe 1
#include "RxSingle.h"

@class RxInternalSchedulersEventLoopsScheduler;
@class RxSingleSubscriber;

@interface RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission : NSObject < RxSingle_OnSubscribe >

#pragma mark Public

- (void)callWithId:(RxSingleSubscriber *)singleSubscriber;

#pragma mark Package-Private

- (instancetype)initWithRxInternalSchedulersEventLoopsScheduler:(RxInternalSchedulersEventLoopsScheduler *)es
                                                         withId:(id)value;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission_initWithRxInternalSchedulersEventLoopsScheduler_withId_(RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission *self, RxInternalSchedulersEventLoopsScheduler *es, id value);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission *new_RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission_initWithRxInternalSchedulersEventLoopsScheduler_withId_(RxInternalSchedulersEventLoopsScheduler *es, id value) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission *create_RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission_initWithRxInternalSchedulersEventLoopsScheduler_withId_(RxInternalSchedulersEventLoopsScheduler *es, id value);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousSingle_DirectScheduledEmission)

#endif

#if !defined (RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle || defined(INCLUDE_RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission))
#define RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission_

#define RESTRICT_RxSingle 1
#define INCLUDE_RxSingle_OnSubscribe 1
#include "RxSingle.h"

@class RxScheduler;
@class RxSingleSubscriber;

@interface RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission : NSObject < RxSingle_OnSubscribe >

#pragma mark Public

- (void)callWithId:(RxSingleSubscriber *)singleSubscriber;

#pragma mark Package-Private

- (instancetype)initWithRxScheduler:(RxScheduler *)scheduler
                             withId:(id)value;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission_initWithRxScheduler_withId_(RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission *self, RxScheduler *scheduler, id value);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission *new_RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission_initWithRxScheduler_withId_(RxScheduler *scheduler, id value) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission *create_RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission_initWithRxScheduler_withId_(RxScheduler *scheduler, id value);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousSingle_NormalScheduledEmission)

#endif

#if !defined (RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle || defined(INCLUDE_RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction))
#define RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction_

#define RESTRICT_RxFunctionsAction0 1
#define INCLUDE_RxFunctionsAction0 1
#include "RxFunctionsAction0.h"

@class RxSingleSubscriber;

@interface RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction : NSObject < RxFunctionsAction0 >

#pragma mark Public

- (void)call;

#pragma mark Package-Private

- (instancetype)initWithRxSingleSubscriber:(RxSingleSubscriber *)subscriber
                                    withId:(id)value;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction_initWithRxSingleSubscriber_withId_(RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction *self, RxSingleSubscriber *subscriber, id value);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction *new_RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction_initWithRxSingleSubscriber_withId_(RxSingleSubscriber *subscriber, id value) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction *create_RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction_initWithRxSingleSubscriber_withId_(RxSingleSubscriber *subscriber, id value);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousSingle_ScalarSynchronousSingleAction)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalUtilScalarSynchronousSingle")
