//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeToMap.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsFunc0.h"
#include "RxFunctionsFunc1.h"
#include "RxInternalOperatorsDeferredScalarSubscriberSafe.h"
#include "RxInternalOperatorsOnSubscribeToMap.h"
#include "RxObservable.h"
#include "RxSubscriber.h"
#include "java/lang/Long.h"
#include "java/util/HashMap.h"
#include "java/util/Map.h"

@implementation RxInternalOperatorsOnSubscribeToMap

- (instancetype)initWithRxObservable:(RxObservable *)source
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)keySelector
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)valueSelector {
  RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_(self, source, keySelector, valueSelector);
  return self;
}

- (instancetype)initWithRxObservable:(RxObservable *)source
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)keySelector
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)valueSelector
                withRxFunctionsFunc0:(id<RxFunctionsFunc0>)mapFactory {
  RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(self, source, keySelector, valueSelector, mapFactory);
  return self;
}

- (id<JavaUtilMap>)call {
  return create_JavaUtilHashMap_init();
}

- (void)callWithId:(RxSubscriber *)subscriber {
  id<JavaUtilMap> map;
  @try {
    map = [((id<RxFunctionsFunc0>) nil_chk(mapFactory_)) call];
  }
  @catch (NSException *ex) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_(ex, subscriber);
    return;
  }
  [create_RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber_initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_(subscriber, map, keySelector_, valueSelector_) subscribeToWithRxObservable:source_];
}

- (void)dealloc {
  RELEASE_(source_);
  RELEASE_(keySelector_);
  RELEASE_(valueSelector_);
  RELEASE_(mapFactory_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, NULL, 0x1, -1, 2, -1, 3, -1, -1 },
    { NULL, "LJavaUtilMap;", 0x1, -1, -1, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, 7, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObservable:withRxFunctionsFunc1:withRxFunctionsFunc1:);
  methods[1].selector = @selector(initWithRxObservable:withRxFunctionsFunc1:withRxFunctionsFunc1:withRxFunctionsFunc0:);
  methods[2].selector = @selector(call);
  methods[3].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "source_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 8, -1 },
    { "keySelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 9, -1 },
    { "valueSelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 10, -1 },
    { "mapFactory_", "LRxFunctionsFunc0;", .constantValue.asLong = 0, 0x10, -1, -1, 11, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;LRxFunctionsFunc1;LRxFunctionsFunc1;", "(Lrx/Observable<TT;>;Lrx/functions/Func1<-TT;+TK;>;Lrx/functions/Func1<-TT;+TV;>;)V", "LRxObservable;LRxFunctionsFunc1;LRxFunctionsFunc1;LRxFunctionsFunc0;", "(Lrx/Observable<TT;>;Lrx/functions/Func1<-TT;+TK;>;Lrx/functions/Func1<-TT;+TV;>;Lrx/functions/Func0<+Ljava/util/Map<TK;TV;>;>;)V", "()Ljava/util/Map<TK;TV;>;", "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/util/Map<TK;TV;>;>;)V", "Lrx/Observable<TT;>;", "Lrx/functions/Func1<-TT;+TK;>;", "Lrx/functions/Func1<-TT;+TV;>;", "Lrx/functions/Func0<+Ljava/util/Map<TK;TV;>;>;", "LRxInternalOperatorsOnSubscribeToMap_ToMapSubscriber;", "<T:Ljava/lang/Object;K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/util/Map<TK;TV;>;>;Lrx/functions/Func0<Ljava/util/Map<TK;TV;>;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeToMap = { "OnSubscribeToMap", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 4, 4, -1, 12, -1, 13, -1 };
  return &_RxInternalOperatorsOnSubscribeToMap;
}

@end

void RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_(RxInternalOperatorsOnSubscribeToMap *self, RxObservable *source, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector) {
  RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(self, source, keySelector, valueSelector, nil);
}

RxInternalOperatorsOnSubscribeToMap *new_RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_(RxObservable *source, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeToMap, initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_, source, keySelector, valueSelector)
}

RxInternalOperatorsOnSubscribeToMap *create_RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_(RxObservable *source, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeToMap, initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_, source, keySelector, valueSelector)
}

void RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxInternalOperatorsOnSubscribeToMap *self, RxObservable *source, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector, id<RxFunctionsFunc0> mapFactory) {
  NSObject_init(self);
  JreStrongAssign(&self->source_, source);
  JreStrongAssign(&self->keySelector_, keySelector);
  JreStrongAssign(&self->valueSelector_, valueSelector);
  if (mapFactory == nil) {
    JreStrongAssign(&self->mapFactory_, self);
  }
  else {
    JreStrongAssign(&self->mapFactory_, mapFactory);
  }
}

RxInternalOperatorsOnSubscribeToMap *new_RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxObservable *source, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector, id<RxFunctionsFunc0> mapFactory) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeToMap, initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_, source, keySelector, valueSelector, mapFactory)
}

RxInternalOperatorsOnSubscribeToMap *create_RxInternalOperatorsOnSubscribeToMap_initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_(RxObservable *source, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector, id<RxFunctionsFunc0> mapFactory) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeToMap, initWithRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc0_, source, keySelector, valueSelector, mapFactory)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeToMap)

@implementation RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                     withJavaUtilMap:(id<JavaUtilMap>)map
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)keySelector
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)valueSelector {
  RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber_initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_(self, actual, map, keySelector, valueSelector);
  return self;
}

- (void)onStart {
  [self requestWithLong:JavaLangLong_MAX_VALUE];
}

- (void)onNextWithId:(id)t {
  if (done_) {
    return;
  }
  @try {
    id key = [((id<RxFunctionsFunc1>) nil_chk(keySelector_)) callWithId:t];
    id val = [((id<RxFunctionsFunc1>) nil_chk(valueSelector_)) callWithId:t];
    [((id<JavaUtilMap>) nil_chk(value_)) putWithId:key withId:val];
  }
  @catch (NSException *ex) {
    RxExceptionsExceptions_throwIfFatalWithNSException_(ex);
    [self unsubscribe];
    [self onErrorWithNSException:ex];
  }
}

- (void)dealloc {
  RELEASE_(keySelector_);
  RELEASE_(valueSelector_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withJavaUtilMap:withRxFunctionsFunc1:withRxFunctionsFunc1:);
  methods[1].selector = @selector(onStart);
  methods[2].selector = @selector(onNextWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "keySelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "valueSelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LJavaUtilMap;LRxFunctionsFunc1;LRxFunctionsFunc1;", "(Lrx/Subscriber<-Ljava/util/Map<TK;TV;>;>;Ljava/util/Map<TK;TV;>;Lrx/functions/Func1<-TT;+TK;>;Lrx/functions/Func1<-TT;+TV;>;)V", "onNext", "LNSObject;", "(TT;)V", "Lrx/functions/Func1<-TT;+TK;>;", "Lrx/functions/Func1<-TT;+TV;>;", "LRxInternalOperatorsOnSubscribeToMap;", "<T:Ljava/lang/Object;K:Ljava/lang/Object;V:Ljava/lang/Object;>Lrx/internal/operators/DeferredScalarSubscriberSafe<TT;Ljava/util/Map<TK;TV;>;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber = { "ToMapSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 3, 2, 7, -1, -1, 8, -1 };
  return &_RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber_initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_(RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber *self, RxSubscriber *actual, id<JavaUtilMap> map, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector) {
  RxInternalOperatorsDeferredScalarSubscriberSafe_initWithRxSubscriber_(self, actual);
  JreStrongAssign(&self->value_, map);
  self->hasValue_ = true;
  JreStrongAssign(&self->keySelector_, keySelector);
  JreStrongAssign(&self->valueSelector_, valueSelector);
}

RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber *new_RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber_initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_(RxSubscriber *actual, id<JavaUtilMap> map, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber, initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_, actual, map, keySelector, valueSelector)
}

RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber *create_RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber_initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_(RxSubscriber *actual, id<JavaUtilMap> map, id<RxFunctionsFunc1> keySelector, id<RxFunctionsFunc1> valueSelector) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber, initWithRxSubscriber_withJavaUtilMap_withRxFunctionsFunc1_withRxFunctionsFunc1_, actual, map, keySelector, valueSelector)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeToMap_ToMapSubscriber)
