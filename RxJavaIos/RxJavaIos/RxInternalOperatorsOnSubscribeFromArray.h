//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeFromArray.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeFromArray")
#ifdef RESTRICT_RxInternalOperatorsOnSubscribeFromArray
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeFromArray 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeFromArray 1
#endif
#undef RESTRICT_RxInternalOperatorsOnSubscribeFromArray

#if !defined (RxInternalOperatorsOnSubscribeFromArray_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeFromArray || defined(INCLUDE_RxInternalOperatorsOnSubscribeFromArray))
#define RxInternalOperatorsOnSubscribeFromArray_

#define RESTRICT_RxObservable 1
#define INCLUDE_RxObservable_OnSubscribe 1
#include "RxObservable.h"

@class IOSObjectArray;
@class RxSubscriber;

@interface RxInternalOperatorsOnSubscribeFromArray : NSObject < RxObservable_OnSubscribe > {
 @public
  IOSObjectArray *array_;
}

#pragma mark Public

- (instancetype)initWithNSObjectArray:(IOSObjectArray *)array;

- (void)callWithId:(RxSubscriber *)child;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeFromArray)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeFromArray, array_, IOSObjectArray *)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeFromArray_initWithNSObjectArray_(RxInternalOperatorsOnSubscribeFromArray *self, IOSObjectArray *array);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeFromArray *new_RxInternalOperatorsOnSubscribeFromArray_initWithNSObjectArray_(IOSObjectArray *array) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeFromArray *create_RxInternalOperatorsOnSubscribeFromArray_initWithNSObjectArray_(IOSObjectArray *array);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeFromArray)

#endif

#if !defined (RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeFromArray || defined(INCLUDE_RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer))
#define RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer_

#define RESTRICT_JavaUtilConcurrentAtomicAtomicLong 1
#define INCLUDE_JavaUtilConcurrentAtomicAtomicLong 1
#include "java/util/concurrent/atomic/AtomicLong.h"

#define RESTRICT_RxProducer 1
#define INCLUDE_RxProducer 1
#include "RxProducer.h"

@class IOSObjectArray;
@class RxSubscriber;

@interface RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer : JavaUtilConcurrentAtomicAtomicLong < RxProducer > {
 @public
  RxSubscriber *child_;
  IOSObjectArray *array_;
  jint index_;
}

#pragma mark Public

- (instancetype)initWithRxSubscriber:(RxSubscriber *)child
                   withNSObjectArray:(IOSObjectArray *)array;

- (void)requestWithLong:(jlong)n;

#pragma mark Package-Private

- (void)fastPath;

- (void)slowPathWithLong:(jlong)r;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer, child_, RxSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer, array_, IOSObjectArray *)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer_initWithRxSubscriber_withNSObjectArray_(RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer *self, RxSubscriber *child, IOSObjectArray *array);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer *new_RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer_initWithRxSubscriber_withNSObjectArray_(RxSubscriber *child, IOSObjectArray *array) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer *create_RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer_initWithRxSubscriber_withNSObjectArray_(RxSubscriber *child, IOSObjectArray *array);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeFromArray_FromArrayProducer)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeFromArray")
