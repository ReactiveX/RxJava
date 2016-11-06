//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/ScalarSynchronousObservable.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable")
#ifdef RESTRICT_RxInternalUtilScalarSynchronousObservable
#define INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable 0
#else
#define INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable 1
#endif
#undef RESTRICT_RxInternalUtilScalarSynchronousObservable

#if !defined (RxInternalUtilScalarSynchronousObservable_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable || defined(INCLUDE_RxInternalUtilScalarSynchronousObservable))
#define RxInternalUtilScalarSynchronousObservable_

#define RESTRICT_RxObservable 1
#define INCLUDE_RxObservable 1
#include "RxObservable.h"

@class RxScheduler;
@class RxSubscriber;
@protocol RxFunctionsFunc1;
@protocol RxProducer;

@interface RxInternalUtilScalarSynchronousObservable : RxObservable {
 @public
  id t_;
}

#pragma mark Public

+ (RxInternalUtilScalarSynchronousObservable *)createWithId:(id)t;

- (id)get;

- (RxObservable *)scalarFlatMapWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)func;

- (RxObservable *)scalarScheduleOnWithRxScheduler:(RxScheduler *)scheduler;

#pragma mark Protected

- (instancetype)initWithId:(id)t;

#pragma mark Package-Private

+ (id<RxProducer>)createProducerWithRxSubscriber:(RxSubscriber *)s
                                          withId:(id)v;

@end

J2OBJC_STATIC_INIT(RxInternalUtilScalarSynchronousObservable)

J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable, t_, id)

inline jboolean RxInternalUtilScalarSynchronousObservable_get_STRONG_MODE();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT jboolean RxInternalUtilScalarSynchronousObservable_STRONG_MODE;
J2OBJC_STATIC_FIELD_PRIMITIVE_FINAL(RxInternalUtilScalarSynchronousObservable, STRONG_MODE, jboolean)

FOUNDATION_EXPORT id<RxProducer> RxInternalUtilScalarSynchronousObservable_createProducerWithRxSubscriber_withId_(RxSubscriber *s, id v);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable *RxInternalUtilScalarSynchronousObservable_createWithId_(id t);

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousObservable_initWithId_(RxInternalUtilScalarSynchronousObservable *self, id t);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable *new_RxInternalUtilScalarSynchronousObservable_initWithId_(id t) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable *create_RxInternalUtilScalarSynchronousObservable_initWithId_(id t);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousObservable)

#endif

#if !defined (RxInternalUtilScalarSynchronousObservable_JustOnSubscribe_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable || defined(INCLUDE_RxInternalUtilScalarSynchronousObservable_JustOnSubscribe))
#define RxInternalUtilScalarSynchronousObservable_JustOnSubscribe_

#define RESTRICT_RxObservable 1
#define INCLUDE_RxObservable_OnSubscribe 1
#include "RxObservable.h"

@class RxSubscriber;

@interface RxInternalUtilScalarSynchronousObservable_JustOnSubscribe : NSObject < RxObservable_OnSubscribe > {
 @public
  id value_;
}

#pragma mark Public

- (void)callWithId:(RxSubscriber *)s;

#pragma mark Package-Private

- (instancetype)initWithId:(id)value;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousObservable_JustOnSubscribe)

J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_JustOnSubscribe, value_, id)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousObservable_JustOnSubscribe_initWithId_(RxInternalUtilScalarSynchronousObservable_JustOnSubscribe *self, id value);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_JustOnSubscribe *new_RxInternalUtilScalarSynchronousObservable_JustOnSubscribe_initWithId_(id value) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_JustOnSubscribe *create_RxInternalUtilScalarSynchronousObservable_JustOnSubscribe_initWithId_(id value);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousObservable_JustOnSubscribe)

#endif

#if !defined (RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable || defined(INCLUDE_RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe))
#define RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe_

#define RESTRICT_RxObservable 1
#define INCLUDE_RxObservable_OnSubscribe 1
#include "RxObservable.h"

@class RxSubscriber;
@protocol RxFunctionsFunc1;

@interface RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe : NSObject < RxObservable_OnSubscribe > {
 @public
  id value_;
  id<RxFunctionsFunc1> onSchedule_;
}

#pragma mark Public

- (void)callWithId:(RxSubscriber *)s;

#pragma mark Package-Private

- (instancetype)initWithId:(id)value
      withRxFunctionsFunc1:(id<RxFunctionsFunc1>)onSchedule;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe)

J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe, value_, id)
J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe, onSchedule_, id<RxFunctionsFunc1>)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe_initWithId_withRxFunctionsFunc1_(RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe *self, id value, id<RxFunctionsFunc1> onSchedule);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe *new_RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe_initWithId_withRxFunctionsFunc1_(id value, id<RxFunctionsFunc1> onSchedule) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe *create_RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe_initWithId_withRxFunctionsFunc1_(id value, id<RxFunctionsFunc1> onSchedule);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncOnSubscribe)

#endif

#if !defined (RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable || defined(INCLUDE_RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer))
#define RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer_

#define RESTRICT_JavaUtilConcurrentAtomicAtomicBoolean 1
#define INCLUDE_JavaUtilConcurrentAtomicAtomicBoolean 1
#include "java/util/concurrent/atomic/AtomicBoolean.h"

#define RESTRICT_RxProducer 1
#define INCLUDE_RxProducer 1
#include "RxProducer.h"

#define RESTRICT_RxFunctionsAction0 1
#define INCLUDE_RxFunctionsAction0 1
#include "RxFunctionsAction0.h"

@class RxSubscriber;
@protocol RxFunctionsFunc1;

@interface RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer : JavaUtilConcurrentAtomicAtomicBoolean < RxProducer, RxFunctionsAction0 > {
 @public
  RxSubscriber *actual_;
  id value_ScalarAsyncProducer_;
  id<RxFunctionsFunc1> onSchedule_;
}

#pragma mark Public

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                              withId:(id)value
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)onSchedule;

- (void)call;

- (void)requestWithLong:(jlong)n;

- (NSString *)description;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer)

J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer, actual_, RxSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer, value_ScalarAsyncProducer_, id)
J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer, onSchedule_, id<RxFunctionsFunc1>)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer_initWithRxSubscriber_withId_withRxFunctionsFunc1_(RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer *self, RxSubscriber *actual, id value, id<RxFunctionsFunc1> onSchedule);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer *new_RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer_initWithRxSubscriber_withId_withRxFunctionsFunc1_(RxSubscriber *actual, id value, id<RxFunctionsFunc1> onSchedule) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer *create_RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer_initWithRxSubscriber_withId_withRxFunctionsFunc1_(RxSubscriber *actual, id value, id<RxFunctionsFunc1> onSchedule);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousObservable_ScalarAsyncProducer)

#endif

#if !defined (RxInternalUtilScalarSynchronousObservable_WeakSingleProducer_) && (INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable || defined(INCLUDE_RxInternalUtilScalarSynchronousObservable_WeakSingleProducer))
#define RxInternalUtilScalarSynchronousObservable_WeakSingleProducer_

#define RESTRICT_RxProducer 1
#define INCLUDE_RxProducer 1
#include "RxProducer.h"

@class RxSubscriber;

@interface RxInternalUtilScalarSynchronousObservable_WeakSingleProducer : NSObject < RxProducer > {
 @public
  RxSubscriber *actual_;
  id value_;
  jboolean once_;
}

#pragma mark Public

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                              withId:(id)value;

- (void)requestWithLong:(jlong)n;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilScalarSynchronousObservable_WeakSingleProducer)

J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_WeakSingleProducer, actual_, RxSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalUtilScalarSynchronousObservable_WeakSingleProducer, value_, id)

FOUNDATION_EXPORT void RxInternalUtilScalarSynchronousObservable_WeakSingleProducer_initWithRxSubscriber_withId_(RxInternalUtilScalarSynchronousObservable_WeakSingleProducer *self, RxSubscriber *actual, id value);

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_WeakSingleProducer *new_RxInternalUtilScalarSynchronousObservable_WeakSingleProducer_initWithRxSubscriber_withId_(RxSubscriber *actual, id value) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalUtilScalarSynchronousObservable_WeakSingleProducer *create_RxInternalUtilScalarSynchronousObservable_WeakSingleProducer_initWithRxSubscriber_withId_(RxSubscriber *actual, id value);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalUtilScalarSynchronousObservable_WeakSingleProducer)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalUtilScalarSynchronousObservable")
