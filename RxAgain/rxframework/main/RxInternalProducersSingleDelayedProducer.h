//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/producers/SingleDelayedProducer.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalProducersSingleDelayedProducer")
#ifdef RESTRICT_RxInternalProducersSingleDelayedProducer
#define INCLUDE_ALL_RxInternalProducersSingleDelayedProducer 0
#else
#define INCLUDE_ALL_RxInternalProducersSingleDelayedProducer 1
#endif
#undef RESTRICT_RxInternalProducersSingleDelayedProducer

#if !defined (RxInternalProducersSingleDelayedProducer_) && (INCLUDE_ALL_RxInternalProducersSingleDelayedProducer || defined(INCLUDE_RxInternalProducersSingleDelayedProducer))
#define RxInternalProducersSingleDelayedProducer_

#define RESTRICT_JavaUtilConcurrentAtomicAtomicInteger 1
#define INCLUDE_JavaUtilConcurrentAtomicAtomicInteger 1
#include "java/util/concurrent/atomic/AtomicInteger.h"

#define RESTRICT_RxProducer 1
#define INCLUDE_RxProducer 1
#include "RxProducer.h"

@class RxSubscriber;

@interface RxInternalProducersSingleDelayedProducer : JavaUtilConcurrentAtomicAtomicInteger < RxProducer > {
 @public
  __unsafe_unretained RxSubscriber *child_;
  id value_SingleDelayedProducer_;
}

#pragma mark Public

- (instancetype)initWithRxSubscriber:(RxSubscriber *)child;

- (void)requestWithLong:(jlong)n;

- (void)setValueWithId:(id)value;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalProducersSingleDelayedProducer)

J2OBJC_FIELD_SETTER(RxInternalProducersSingleDelayedProducer, value_SingleDelayedProducer_, id)

inline jint RxInternalProducersSingleDelayedProducer_get_NO_REQUEST_NO_VALUE();
#define RxInternalProducersSingleDelayedProducer_NO_REQUEST_NO_VALUE 0
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalProducersSingleDelayedProducer, NO_REQUEST_NO_VALUE, jint)

inline jint RxInternalProducersSingleDelayedProducer_get_NO_REQUEST_HAS_VALUE();
#define RxInternalProducersSingleDelayedProducer_NO_REQUEST_HAS_VALUE 1
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalProducersSingleDelayedProducer, NO_REQUEST_HAS_VALUE, jint)

inline jint RxInternalProducersSingleDelayedProducer_get_HAS_REQUEST_NO_VALUE();
#define RxInternalProducersSingleDelayedProducer_HAS_REQUEST_NO_VALUE 2
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalProducersSingleDelayedProducer, HAS_REQUEST_NO_VALUE, jint)

inline jint RxInternalProducersSingleDelayedProducer_get_HAS_REQUEST_HAS_VALUE();
#define RxInternalProducersSingleDelayedProducer_HAS_REQUEST_HAS_VALUE 3
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalProducersSingleDelayedProducer, HAS_REQUEST_HAS_VALUE, jint)

FOUNDATION_EXPORT void RxInternalProducersSingleDelayedProducer_initWithRxSubscriber_(RxInternalProducersSingleDelayedProducer *self, RxSubscriber *child);

FOUNDATION_EXPORT RxInternalProducersSingleDelayedProducer *new_RxInternalProducersSingleDelayedProducer_initWithRxSubscriber_(RxSubscriber *child) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalProducersSingleDelayedProducer *create_RxInternalProducersSingleDelayedProducer_initWithRxSubscriber_(RxSubscriber *child);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalProducersSingleDelayedProducer)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalProducersSingleDelayedProducer")
