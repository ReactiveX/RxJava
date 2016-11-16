//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeReduce.java
//

#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsFunc2.h"
#include "RxInternalOperatorsOnSubscribeReduce.h"
#include "RxObservable.h"
#include "RxPluginsRxJavaHooks.h"
#include "RxProducer.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Long.h"
#include "java/util/NoSuchElementException.h"

@interface RxInternalOperatorsOnSubscribeReduce_$1 : NSObject < RxProducer > {
 @public
  RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *val$parent_;
}

- (void)requestWithLong:(jlong)n;

- (instancetype)initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber:(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOnSubscribeReduce_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeReduce_$1, val$parent_, RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(RxInternalOperatorsOnSubscribeReduce_$1 *self, RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *capture$0);

__attribute__((unused)) static RxInternalOperatorsOnSubscribeReduce_$1 *new_RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOnSubscribeReduce_$1 *create_RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *capture$0);

@implementation RxInternalOperatorsOnSubscribeReduce

- (instancetype)initWithRxObservable:(RxObservable *)source
                withRxFunctionsFunc2:(id<RxFunctionsFunc2>)reducer {
  RxInternalOperatorsOnSubscribeReduce_initWithRxObservable_withRxFunctionsFunc2_(self, source, reducer);
  return self;
}

- (void)callWithId:(RxSubscriber *)t {
  RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *parent = create_RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_initWithRxSubscriber_withRxFunctionsFunc2_(t, reducer_);
  [((RxSubscriber *) nil_chk(t)) addWithRxSubscription:parent];
  [t setProducerWithRxProducer:create_RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(parent)];
  [((RxObservable *) nil_chk(source_)) unsafeSubscribeWithRxSubscriber:parent];
}

- (void)dealloc {
  RELEASE_(source_);
  RELEASE_(reducer_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObservable:withRxFunctionsFunc2:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "source_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "reducer_", "LRxFunctionsFunc2;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;LRxFunctionsFunc2;", "(Lrx/Observable<TT;>;Lrx/functions/Func2<TT;TT;TT;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)V", "Lrx/Observable<TT;>;", "Lrx/functions/Func2<TT;TT;TT;>;", "LRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$OnSubscribe<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeReduce = { "OnSubscribeReduce", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 2, -1, 7, -1, 8, -1 };
  return &_RxInternalOperatorsOnSubscribeReduce;
}

@end

void RxInternalOperatorsOnSubscribeReduce_initWithRxObservable_withRxFunctionsFunc2_(RxInternalOperatorsOnSubscribeReduce *self, RxObservable *source, id<RxFunctionsFunc2> reducer) {
  NSObject_init(self);
  JreStrongAssign(&self->source_, source);
  JreStrongAssign(&self->reducer_, reducer);
}

RxInternalOperatorsOnSubscribeReduce *new_RxInternalOperatorsOnSubscribeReduce_initWithRxObservable_withRxFunctionsFunc2_(RxObservable *source, id<RxFunctionsFunc2> reducer) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeReduce, initWithRxObservable_withRxFunctionsFunc2_, source, reducer)
}

RxInternalOperatorsOnSubscribeReduce *create_RxInternalOperatorsOnSubscribeReduce_initWithRxObservable_withRxFunctionsFunc2_(RxObservable *source, id<RxFunctionsFunc2> reducer) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeReduce, initWithRxObservable_withRxFunctionsFunc2_, source, reducer)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeReduce)

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber)

id RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_EMPTY;

@implementation RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                withRxFunctionsFunc2:(id<RxFunctionsFunc2>)reducer {
  RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_initWithRxSubscriber_withRxFunctionsFunc2_(self, actual, reducer);
  return self;
}

- (void)onNextWithId:(id)t {
  if (done_) {
    return;
  }
  id o = value_;
  if (o == RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_EMPTY) {
    JreStrongAssign(&value_, t);
  }
  else {
    @try {
      JreStrongAssign(&value_, [((id<RxFunctionsFunc2>) nil_chk(reducer_)) callWithId:o withId:t]);
    }
    @catch (NSException *ex) {
      RxExceptionsExceptions_throwIfFatalWithNSException_(ex);
      [self unsubscribe];
      [self onErrorWithNSException:ex];
    }
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  if (!done_) {
    done_ = true;
    [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:e];
  }
  else {
    RxPluginsRxJavaHooks_onErrorWithNSException_(e);
  }
}

- (void)onCompleted {
  if (done_) {
    return;
  }
  done_ = true;
  id o = value_;
  if (o != RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_EMPTY) {
    [((RxSubscriber *) nil_chk(actual_)) onNextWithId:o];
    [actual_ onCompleted];
  }
  else {
    [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:create_JavaUtilNoSuchElementException_init()];
  }
}

- (void)downstreamRequestWithLong:(jlong)n {
  if (n < 0LL) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$J", @"n >= 0 required but it was ", n));
  }
  if (n != 0LL) {
    [self requestWithLong:JavaLangLong_MAX_VALUE];
  }
}

- (void)dealloc {
  RELEASE_(actual_);
  RELEASE_(reducer_);
  RELEASE_(value_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, 7, 8, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withRxFunctionsFunc2:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(downstreamRequestWithLong:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 9, -1 },
    { "reducer_", "LRxFunctionsFunc2;", .constantValue.asLong = 0, 0x10, -1, -1, 10, -1 },
    { "value_", "LNSObject;", .constantValue.asLong = 0, 0x0, -1, -1, 11, -1 },
    { "EMPTY", "LNSObject;", .constantValue.asLong = 0, 0x18, -1, 12, -1, -1 },
    { "done_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LRxFunctionsFunc2;", "(Lrx/Subscriber<-TT;>;Lrx/functions/Func2<TT;TT;TT;>;)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "downstreamRequest", "J", "Lrx/Subscriber<-TT;>;", "Lrx/functions/Func2<TT;TT;TT;>;", "TT;", &RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_EMPTY, "LRxInternalOperatorsOnSubscribeReduce;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber = { "ReduceSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 5, 5, 13, -1, -1, 14, -1 };
  return &_RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber class]) {
    JreStrongAssignAndConsume(&RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_EMPTY, new_NSObject_init());
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber)
  }
}

@end

void RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_initWithRxSubscriber_withRxFunctionsFunc2_(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *self, RxSubscriber *actual, id<RxFunctionsFunc2> reducer) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->actual_, actual);
  JreStrongAssign(&self->reducer_, reducer);
  JreStrongAssign(&self->value_, RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_EMPTY);
  [self requestWithLong:0];
}

RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *new_RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_initWithRxSubscriber_withRxFunctionsFunc2_(RxSubscriber *actual, id<RxFunctionsFunc2> reducer) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber, initWithRxSubscriber_withRxFunctionsFunc2_, actual, reducer)
}

RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *create_RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_initWithRxSubscriber_withRxFunctionsFunc2_(RxSubscriber *actual, id<RxFunctionsFunc2> reducer) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber, initWithRxSubscriber_withRxFunctionsFunc2_, actual, reducer)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber)

@implementation RxInternalOperatorsOnSubscribeReduce_$1

- (void)requestWithLong:(jlong)n {
  [((RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *) nil_chk(val$parent_)) downstreamRequestWithLong:n];
}

- (instancetype)initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber:(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *)capture$0 {
  RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$parent_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$parent_", "LRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 4, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber;", "(Lrx/internal/operators/OnSubscribeReduce$ReduceSubscriber<TT;>;)V", "Lrx/internal/operators/OnSubscribeReduce$ReduceSubscriber<TT;>;", "LRxInternalOperatorsOnSubscribeReduce;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeReduce_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 5, -1, 6, -1, -1 };
  return &_RxInternalOperatorsOnSubscribeReduce_$1;
}

@end

void RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(RxInternalOperatorsOnSubscribeReduce_$1 *self, RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *capture$0) {
  JreStrongAssign(&self->val$parent_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOnSubscribeReduce_$1 *new_RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeReduce_$1, initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_, capture$0)
}

RxInternalOperatorsOnSubscribeReduce_$1 *create_RxInternalOperatorsOnSubscribeReduce_$1_initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_(RxInternalOperatorsOnSubscribeReduce_ReduceSubscriber *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeReduce_$1, initWithRxInternalOperatorsOnSubscribeReduce_ReduceSubscriber_, capture$0)
}
