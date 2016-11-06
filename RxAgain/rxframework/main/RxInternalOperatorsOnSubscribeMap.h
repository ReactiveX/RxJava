//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeMap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeMap")
#ifdef RESTRICT_RxInternalOperatorsOnSubscribeMap
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeMap 0
#else
#define INCLUDE_ALL_RxInternalOperatorsOnSubscribeMap 1
#endif
#undef RESTRICT_RxInternalOperatorsOnSubscribeMap

#if !defined (RxInternalOperatorsOnSubscribeMap_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeMap || defined(INCLUDE_RxInternalOperatorsOnSubscribeMap))
#define RxInternalOperatorsOnSubscribeMap_

#define RESTRICT_RxObservable 1
#define INCLUDE_RxObservable_OnSubscribe 1
#include "RxObservable.h"

@class RxObservable;
@class RxSubscriber;
@protocol RxFunctionsFunc1;

@interface RxInternalOperatorsOnSubscribeMap : NSObject < RxObservable_OnSubscribe > {
 @public
  RxObservable *source_;
  id<RxFunctionsFunc1> transformer_;
}

#pragma mark Public

- (instancetype)initWithRxObservable:(RxObservable *)source
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)transformer;

- (void)callWithId:(RxSubscriber *)o;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeMap)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeMap, source_, RxObservable *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeMap, transformer_, id<RxFunctionsFunc1>)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeMap_initWithRxObservable_withRxFunctionsFunc1_(RxInternalOperatorsOnSubscribeMap *self, RxObservable *source, id<RxFunctionsFunc1> transformer);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeMap *new_RxInternalOperatorsOnSubscribeMap_initWithRxObservable_withRxFunctionsFunc1_(RxObservable *source, id<RxFunctionsFunc1> transformer) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeMap *create_RxInternalOperatorsOnSubscribeMap_initWithRxObservable_withRxFunctionsFunc1_(RxObservable *source, id<RxFunctionsFunc1> transformer);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeMap)

#endif

#if !defined (RxInternalOperatorsOnSubscribeMap_MapSubscriber_) && (INCLUDE_ALL_RxInternalOperatorsOnSubscribeMap || defined(INCLUDE_RxInternalOperatorsOnSubscribeMap_MapSubscriber))
#define RxInternalOperatorsOnSubscribeMap_MapSubscriber_

#define RESTRICT_RxSubscriber 1
#define INCLUDE_RxSubscriber 1
#include "RxSubscriber.h"

@protocol RxFunctionsFunc1;
@protocol RxProducer;

@interface RxInternalOperatorsOnSubscribeMap_MapSubscriber : RxSubscriber {
 @public
  RxSubscriber *actual_;
  id<RxFunctionsFunc1> mapper_;
  jboolean done_;
}

#pragma mark Public

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)mapper;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)t;

- (void)setProducerWithRxProducer:(id<RxProducer>)p;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeMap_MapSubscriber)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeMap_MapSubscriber, actual_, RxSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeMap_MapSubscriber, mapper_, id<RxFunctionsFunc1>)

FOUNDATION_EXPORT void RxInternalOperatorsOnSubscribeMap_MapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_(RxInternalOperatorsOnSubscribeMap_MapSubscriber *self, RxSubscriber *actual, id<RxFunctionsFunc1> mapper);

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeMap_MapSubscriber *new_RxInternalOperatorsOnSubscribeMap_MapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_(RxSubscriber *actual, id<RxFunctionsFunc1> mapper) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxInternalOperatorsOnSubscribeMap_MapSubscriber *create_RxInternalOperatorsOnSubscribeMap_MapSubscriber_initWithRxSubscriber_withRxFunctionsFunc1_(RxSubscriber *actual, id<RxFunctionsFunc1> mapper);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOnSubscribeMap_MapSubscriber)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsOnSubscribeMap")
