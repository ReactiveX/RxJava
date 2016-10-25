//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/CompletableOnSubscribeConcatArray.java
//

#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxCompletable.h"
#include "RxCompletableSubscriber.h"
#include "RxInternalOperatorsCompletableOnSubscribeConcatArray.h"
#include "RxSubscription.h"
#include "RxSubscriptionsSerialSubscription.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"

inline jlong RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_get_serialVersionUID();
#define RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_serialVersionUID -7965400327305809232LL
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber, serialVersionUID, jlong)

@implementation RxInternalOperatorsCompletableOnSubscribeConcatArray

- (instancetype)initWithRxCompletableArray:(IOSObjectArray *)sources {
  RxInternalOperatorsCompletableOnSubscribeConcatArray_initWithRxCompletableArray_(self, sources);
  return self;
}

- (void)callWithId:(id<RxCompletableSubscriber>)s {
  RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber *inner = create_RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_initWithRxCompletableSubscriber_withRxCompletableArray_(s, sources_);
  [((id<RxCompletableSubscriber>) nil_chk(s)) onSubscribeWithRxSubscription:inner->sd_];
  [inner next];
}

- (void)dealloc {
  RELEASE_(sources_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxCompletableArray:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "sources_", "[LRxCompletable;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "[LRxCompletable;", "call", "LRxCompletableSubscriber;", "LRxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCompletableOnSubscribeConcatArray = { "CompletableOnSubscribeConcatArray", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 1, -1, 3, -1, -1, -1 };
  return &_RxInternalOperatorsCompletableOnSubscribeConcatArray;
}

@end

void RxInternalOperatorsCompletableOnSubscribeConcatArray_initWithRxCompletableArray_(RxInternalOperatorsCompletableOnSubscribeConcatArray *self, IOSObjectArray *sources) {
  NSObject_init(self);
  JreStrongAssign(&self->sources_, sources);
}

RxInternalOperatorsCompletableOnSubscribeConcatArray *new_RxInternalOperatorsCompletableOnSubscribeConcatArray_initWithRxCompletableArray_(IOSObjectArray *sources) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCompletableOnSubscribeConcatArray, initWithRxCompletableArray_, sources)
}

RxInternalOperatorsCompletableOnSubscribeConcatArray *create_RxInternalOperatorsCompletableOnSubscribeConcatArray_initWithRxCompletableArray_(IOSObjectArray *sources) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCompletableOnSubscribeConcatArray, initWithRxCompletableArray_, sources)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsCompletableOnSubscribeConcatArray)

@implementation RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber

- (instancetype)initWithRxCompletableSubscriber:(id<RxCompletableSubscriber>)actual
                         withRxCompletableArray:(IOSObjectArray *)sources {
  RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_initWithRxCompletableSubscriber_withRxCompletableArray_(self, actual, sources);
  return self;
}

- (void)onSubscribeWithRxSubscription:(id<RxSubscription>)d {
  [((RxSubscriptionsSerialSubscription *) nil_chk(sd_)) setWithRxSubscription:d];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((id<RxCompletableSubscriber>) nil_chk(actual_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  [self next];
}

- (void)next {
  if ([((RxSubscriptionsSerialSubscription *) nil_chk(sd_)) isUnsubscribed]) {
    return;
  }
  if ([self getAndIncrement] != 0) {
    return;
  }
  IOSObjectArray *a = sources_;
  do {
    if ([sd_ isUnsubscribed]) {
      return;
    }
    jint idx = index_++;
    if (idx == ((IOSObjectArray *) nil_chk(a))->size_) {
      [((id<RxCompletableSubscriber>) nil_chk(actual_)) onCompleted];
      return;
    }
    [((RxCompletable *) nil_chk(IOSObjectArray_Get(a, idx))) unsafeSubscribeWithRxCompletableSubscriber:self];
  }
  while ([self decrementAndGet] != 0);
}

- (void)dealloc {
  RELEASE_(actual_);
  RELEASE_(sources_);
  RELEASE_(sd_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxCompletableSubscriber:withRxCompletableArray:);
  methods[1].selector = @selector(onSubscribeWithRxSubscription:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(next);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_serialVersionUID, 0x1a, -1, -1, -1, -1 },
    { "actual_", "LRxCompletableSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "sources_", "[LRxCompletable;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "index_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "sd_", "LRxSubscriptionsSerialSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxCompletableSubscriber;[LRxCompletable;", "onSubscribe", "LRxSubscription;", "onError", "LNSException;", "LRxInternalOperatorsCompletableOnSubscribeConcatArray;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber = { "ConcatInnerSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 5, 5, 5, -1, -1, -1, -1 };
  return &_RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber;
}

@end

void RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_initWithRxCompletableSubscriber_withRxCompletableArray_(RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber *self, id<RxCompletableSubscriber> actual, IOSObjectArray *sources) {
  JavaUtilConcurrentAtomicAtomicInteger_init(self);
  JreStrongAssign(&self->actual_, actual);
  JreStrongAssign(&self->sources_, sources);
  JreStrongAssignAndConsume(&self->sd_, new_RxSubscriptionsSerialSubscription_init());
}

RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber *new_RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_initWithRxCompletableSubscriber_withRxCompletableArray_(id<RxCompletableSubscriber> actual, IOSObjectArray *sources) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber, initWithRxCompletableSubscriber_withRxCompletableArray_, actual, sources)
}

RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber *create_RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber_initWithRxCompletableSubscriber_withRxCompletableArray_(id<RxCompletableSubscriber> actual, IOSObjectArray *sources) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber, initWithRxCompletableSubscriber_withRxCompletableArray_, actual, sources)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsCompletableOnSubscribeConcatArray_ConcatInnerSubscriber)
