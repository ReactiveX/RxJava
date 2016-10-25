//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/CachedObservable.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxExceptionsOnErrorThrowable.h"
#include "RxInternalOperatorsCachedObservable.h"
#include "RxInternalOperatorsNotificationLite.h"
#include "RxInternalUtilLinkedArrayList.h"
#include "RxObservable.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "RxSubscriptionsSerialSubscription.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Long.h"
#include "java/lang/System.h"
#include "java/util/concurrent/atomic/AtomicBoolean.h"
#include "java/util/concurrent/atomic/AtomicLong.h"

@interface RxInternalOperatorsCachedObservable () {
 @public
  RxInternalOperatorsCachedObservable_CacheState *state_;
}

- (instancetype)initWithRxObservable_OnSubscribe:(id<RxObservable_OnSubscribe>)onSubscribe
withRxInternalOperatorsCachedObservable_CacheState:(RxInternalOperatorsCachedObservable_CacheState *)state;

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsCachedObservable, state_, RxInternalOperatorsCachedObservable_CacheState *)

__attribute__((unused)) static void RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable *self, id<RxObservable_OnSubscribe> onSubscribe, RxInternalOperatorsCachedObservable_CacheState *state);

__attribute__((unused)) static RxInternalOperatorsCachedObservable *new_RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(id<RxObservable_OnSubscribe> onSubscribe, RxInternalOperatorsCachedObservable_CacheState *state) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsCachedObservable *create_RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(id<RxObservable_OnSubscribe> onSubscribe, RxInternalOperatorsCachedObservable_CacheState *state);

@interface RxInternalOperatorsCachedObservable_CacheState_$1 : RxSubscriber {
 @public
  RxInternalOperatorsCachedObservable_CacheState *this$0_;
}

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxInternalOperatorsCachedObservable_CacheState:(RxInternalOperatorsCachedObservable_CacheState *)outer$;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsCachedObservable_CacheState_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsCachedObservable_CacheState_$1, this$0_, RxInternalOperatorsCachedObservable_CacheState *)

__attribute__((unused)) static void RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState_$1 *self, RxInternalOperatorsCachedObservable_CacheState *outer$);

__attribute__((unused)) static RxInternalOperatorsCachedObservable_CacheState_$1 *new_RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState *outer$) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsCachedObservable_CacheState_$1 *create_RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState *outer$);

inline jlong RxInternalOperatorsCachedObservable_CachedSubscribe_get_serialVersionUID();
#define RxInternalOperatorsCachedObservable_CachedSubscribe_serialVersionUID -2817751667698696782LL
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalOperatorsCachedObservable_CachedSubscribe, serialVersionUID, jlong)

inline jlong RxInternalOperatorsCachedObservable_ReplayProducer_get_serialVersionUID();
#define RxInternalOperatorsCachedObservable_ReplayProducer_serialVersionUID -2557562030197141021LL
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalOperatorsCachedObservable_ReplayProducer, serialVersionUID, jlong)

@implementation RxInternalOperatorsCachedObservable

+ (RxInternalOperatorsCachedObservable *)fromWithRxObservable:(RxObservable *)source {
  return RxInternalOperatorsCachedObservable_fromWithRxObservable_(source);
}

+ (RxInternalOperatorsCachedObservable *)fromWithRxObservable:(RxObservable *)source
                                                      withInt:(jint)capacityHint {
  return RxInternalOperatorsCachedObservable_fromWithRxObservable_withInt_(source, capacityHint);
}

- (instancetype)initWithRxObservable_OnSubscribe:(id<RxObservable_OnSubscribe>)onSubscribe
withRxInternalOperatorsCachedObservable_CacheState:(RxInternalOperatorsCachedObservable_CacheState *)state {
  RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(self, onSubscribe, state);
  return self;
}

- (jboolean)isConnected {
  return JreLoadVolatileBoolean(&((RxInternalOperatorsCachedObservable_CacheState *) nil_chk(state_))->isConnected_);
}

- (jboolean)hasObservers {
  return ((IOSObjectArray *) nil_chk(JreLoadVolatileId(&((RxInternalOperatorsCachedObservable_CacheState *) nil_chk(state_))->producers_)))->size_ != 0;
}

- (void)dealloc {
  RELEASE_(state_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalOperatorsCachedObservable;", 0x9, 0, 1, -1, 2, -1, -1 },
    { NULL, "LRxInternalOperatorsCachedObservable;", 0x9, 0, 3, -1, 4, -1, -1 },
    { NULL, NULL, 0x2, -1, 5, -1, 6, -1, -1 },
    { NULL, "Z", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(fromWithRxObservable:);
  methods[1].selector = @selector(fromWithRxObservable:withInt:);
  methods[2].selector = @selector(initWithRxObservable_OnSubscribe:withRxInternalOperatorsCachedObservable_CacheState:);
  methods[3].selector = @selector(isConnected);
  methods[4].selector = @selector(hasObservers);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "state_", "LRxInternalOperatorsCachedObservable_CacheState;", .constantValue.asLong = 0, 0x12, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "from", "LRxObservable;", "<T:Ljava/lang/Object;>(Lrx/Observable<+TT;>;)Lrx/internal/operators/CachedObservable<TT;>;", "LRxObservable;I", "<T:Ljava/lang/Object;>(Lrx/Observable<+TT;>;I)Lrx/internal/operators/CachedObservable<TT;>;", "LRxObservable_OnSubscribe;LRxInternalOperatorsCachedObservable_CacheState;", "(Lrx/Observable$OnSubscribe<TT;>;Lrx/internal/operators/CachedObservable$CacheState<TT;>;)V", "Lrx/internal/operators/CachedObservable$CacheState<TT;>;", "LRxInternalOperatorsCachedObservable_CacheState;LRxInternalOperatorsCachedObservable_CachedSubscribe;LRxInternalOperatorsCachedObservable_ReplayProducer;", "<T:Ljava/lang/Object;>Lrx/Observable<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCachedObservable = { "CachedObservable", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 5, 1, -1, 8, -1, 9, -1 };
  return &_RxInternalOperatorsCachedObservable;
}

@end

RxInternalOperatorsCachedObservable *RxInternalOperatorsCachedObservable_fromWithRxObservable_(RxObservable *source) {
  RxInternalOperatorsCachedObservable_initialize();
  return RxInternalOperatorsCachedObservable_fromWithRxObservable_withInt_(source, 16);
}

RxInternalOperatorsCachedObservable *RxInternalOperatorsCachedObservable_fromWithRxObservable_withInt_(RxObservable *source, jint capacityHint) {
  RxInternalOperatorsCachedObservable_initialize();
  if (capacityHint < 1) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(@"capacityHint > 0 required");
  }
  RxInternalOperatorsCachedObservable_CacheState *state = create_RxInternalOperatorsCachedObservable_CacheState_initWithRxObservable_withInt_(source, capacityHint);
  RxInternalOperatorsCachedObservable_CachedSubscribe *onSubscribe = create_RxInternalOperatorsCachedObservable_CachedSubscribe_initWithRxInternalOperatorsCachedObservable_CacheState_(state);
  return create_RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(onSubscribe, state);
}

void RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable *self, id<RxObservable_OnSubscribe> onSubscribe, RxInternalOperatorsCachedObservable_CacheState *state) {
  RxObservable_initWithRxObservable_OnSubscribe_(self, onSubscribe);
  JreStrongAssign(&self->state_, state);
}

RxInternalOperatorsCachedObservable *new_RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(id<RxObservable_OnSubscribe> onSubscribe, RxInternalOperatorsCachedObservable_CacheState *state) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCachedObservable, initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_, onSubscribe, state)
}

RxInternalOperatorsCachedObservable *create_RxInternalOperatorsCachedObservable_initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_(id<RxObservable_OnSubscribe> onSubscribe, RxInternalOperatorsCachedObservable_CacheState *state) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCachedObservable, initWithRxObservable_OnSubscribe_withRxInternalOperatorsCachedObservable_CacheState_, onSubscribe, state)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsCachedObservable)

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsCachedObservable_CacheState)

IOSObjectArray *RxInternalOperatorsCachedObservable_CacheState_EMPTY;

@implementation RxInternalOperatorsCachedObservable_CacheState

- (instancetype)initWithRxObservable:(RxObservable *)source
                             withInt:(jint)capacityHint {
  RxInternalOperatorsCachedObservable_CacheState_initWithRxObservable_withInt_(self, source, capacityHint);
  return self;
}

- (void)addProducerWithRxInternalOperatorsCachedObservable_ReplayProducer:(RxInternalOperatorsCachedObservable_ReplayProducer *)p {
  @synchronized(connection_) {
    IOSObjectArray *a = JreLoadVolatileId(&producers_);
    jint n = ((IOSObjectArray *) nil_chk(a))->size_;
    IOSObjectArray *b = [IOSObjectArray arrayWithLength:n + 1 type:RxInternalOperatorsCachedObservable_ReplayProducer_class_()];
    JavaLangSystem_arraycopyWithId_withInt_withId_withInt_withInt_(a, 0, b, 0, n);
    IOSObjectArray_Set(b, n, p);
    JreVolatileStrongAssign(&producers_, b);
  }
}

- (void)removeProducerWithRxInternalOperatorsCachedObservable_ReplayProducer:(RxInternalOperatorsCachedObservable_ReplayProducer *)p {
  @synchronized(connection_) {
    IOSObjectArray *a = JreLoadVolatileId(&producers_);
    jint n = ((IOSObjectArray *) nil_chk(a))->size_;
    jint j = -1;
    for (jint i = 0; i < n; i++) {
      if ([((RxInternalOperatorsCachedObservable_ReplayProducer *) nil_chk(IOSObjectArray_Get(a, i))) isEqual:p]) {
        j = i;
        break;
      }
    }
    if (j < 0) {
      return;
    }
    if (n == 1) {
      JreVolatileStrongAssign(&producers_, RxInternalOperatorsCachedObservable_CacheState_EMPTY);
      return;
    }
    IOSObjectArray *b = [IOSObjectArray arrayWithLength:n - 1 type:RxInternalOperatorsCachedObservable_ReplayProducer_class_()];
    JavaLangSystem_arraycopyWithId_withInt_withId_withInt_withInt_(a, 0, b, 0, j);
    JavaLangSystem_arraycopyWithId_withInt_withId_withInt_withInt_(a, j + 1, b, j, n - j - 1);
    JreVolatileStrongAssign(&producers_, b);
  }
}

- (void)connect {
  RxSubscriber *subscriber = create_RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(self);
  [((RxSubscriptionsSerialSubscription *) nil_chk(connection_)) setWithRxSubscription:subscriber];
  [((RxObservable *) nil_chk(source_)) unsafeSubscribeWithRxSubscriber:subscriber];
  JreAssignVolatileBoolean(&isConnected_, true);
}

- (void)onNextWithId:(id)t {
  if (!sourceDone_) {
    id o = RxInternalOperatorsNotificationLite_nextWithId_(t);
    [self addWithId:o];
    [self dispatch];
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  if (!sourceDone_) {
    sourceDone_ = true;
    id o = RxInternalOperatorsNotificationLite_errorWithNSException_(e);
    [self addWithId:o];
    [((RxSubscriptionsSerialSubscription *) nil_chk(connection_)) unsubscribe];
    [self dispatch];
  }
}

- (void)onCompleted {
  if (!sourceDone_) {
    sourceDone_ = true;
    id o = RxInternalOperatorsNotificationLite_completed();
    [self addWithId:o];
    [((RxSubscriptionsSerialSubscription *) nil_chk(connection_)) unsubscribe];
    [self dispatch];
  }
}

- (void)dispatch {
  IOSObjectArray *a = JreLoadVolatileId(&producers_);
  {
    IOSObjectArray *a__ = a;
    RxInternalOperatorsCachedObservable_ReplayProducer * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
    RxInternalOperatorsCachedObservable_ReplayProducer * const *e__ = b__ + a__->size_;
    while (b__ < e__) {
      RxInternalOperatorsCachedObservable_ReplayProducer *rp = *b__++;
      [((RxInternalOperatorsCachedObservable_ReplayProducer *) nil_chk(rp)) replay];
    }
  }
}

- (void)__javaClone:(RxInternalOperatorsCachedObservable_CacheState *)original {
  [super __javaClone:original];
  JreCloneVolatileStrong(&producers_, &original->producers_);
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsCachedObservable_CacheState class]);
  RELEASE_(source_);
  RELEASE_(connection_);
  JreReleaseVolatile(&producers_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 6, 7, -1, 8, -1, -1 },
    { NULL, "V", 0x1, 9, 10, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObservable:withInt:);
  methods[1].selector = @selector(addProducerWithRxInternalOperatorsCachedObservable_ReplayProducer:);
  methods[2].selector = @selector(removeProducerWithRxInternalOperatorsCachedObservable_ReplayProducer:);
  methods[3].selector = @selector(connect);
  methods[4].selector = @selector(onNextWithId:);
  methods[5].selector = @selector(onErrorWithNSException:);
  methods[6].selector = @selector(onCompleted);
  methods[7].selector = @selector(dispatch);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "source_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 11, -1 },
    { "connection_", "LRxSubscriptionsSerialSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "producers_", "[LRxInternalOperatorsCachedObservable_ReplayProducer;", .constantValue.asLong = 0, 0x40, -1, -1, 12, -1 },
    { "EMPTY", "[LRxInternalOperatorsCachedObservable_ReplayProducer;", .constantValue.asLong = 0, 0x18, -1, 13, 12, -1 },
    { "isConnected_", "Z", .constantValue.asLong = 0, 0x40, -1, -1, -1, -1 },
    { "sourceDone_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;I", "(Lrx/Observable<+TT;>;I)V", "addProducer", "LRxInternalOperatorsCachedObservable_ReplayProducer;", "(Lrx/internal/operators/CachedObservable$ReplayProducer<TT;>;)V", "removeProducer", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "Lrx/Observable<+TT;>;", "[Lrx/internal/operators/CachedObservable$ReplayProducer<*>;", &RxInternalOperatorsCachedObservable_CacheState_EMPTY, "LRxInternalOperatorsCachedObservable;", "<T:Ljava/lang/Object;>Lrx/internal/util/LinkedArrayList;Lrx/Observer<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCachedObservable_CacheState = { "CacheState", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 8, 6, 14, -1, -1, 15, -1 };
  return &_RxInternalOperatorsCachedObservable_CacheState;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsCachedObservable_CacheState class]) {
    JreStrongAssignAndConsume(&RxInternalOperatorsCachedObservable_CacheState_EMPTY, [IOSObjectArray newArrayWithLength:0 type:RxInternalOperatorsCachedObservable_ReplayProducer_class_()]);
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsCachedObservable_CacheState)
  }
}

@end

void RxInternalOperatorsCachedObservable_CacheState_initWithRxObservable_withInt_(RxInternalOperatorsCachedObservable_CacheState *self, RxObservable *source, jint capacityHint) {
  RxInternalUtilLinkedArrayList_initWithInt_(self, capacityHint);
  JreStrongAssign(&self->source_, source);
  JreVolatileStrongAssign(&self->producers_, RxInternalOperatorsCachedObservable_CacheState_EMPTY);
  JreStrongAssignAndConsume(&self->connection_, new_RxSubscriptionsSerialSubscription_init());
}

RxInternalOperatorsCachedObservable_CacheState *new_RxInternalOperatorsCachedObservable_CacheState_initWithRxObservable_withInt_(RxObservable *source, jint capacityHint) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCachedObservable_CacheState, initWithRxObservable_withInt_, source, capacityHint)
}

RxInternalOperatorsCachedObservable_CacheState *create_RxInternalOperatorsCachedObservable_CacheState_initWithRxObservable_withInt_(RxObservable *source, jint capacityHint) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCachedObservable_CacheState, initWithRxObservable_withInt_, source, capacityHint)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsCachedObservable_CacheState)

@implementation RxInternalOperatorsCachedObservable_CacheState_$1

- (void)onNextWithId:(id)t {
  [this$0_ onNextWithId:t];
}

- (void)onErrorWithNSException:(NSException *)e {
  [this$0_ onErrorWithNSException:e];
}

- (void)onCompleted {
  [this$0_ onCompleted];
}

- (instancetype)initWithRxInternalOperatorsCachedObservable_CacheState:(RxInternalOperatorsCachedObservable_CacheState *)outer$ {
  RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(self, outer$);
  return self;
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsCachedObservable_CacheState_$1 class]);
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 5, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onNextWithId:);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onCompleted);
  methods[3].selector = @selector(initWithRxInternalOperatorsCachedObservable_CacheState:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsCachedObservable_CacheState;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "LRxInternalOperatorsCachedObservable_CacheState;", "connect", "Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCachedObservable_CacheState_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 1, 5, -1, 6, 7, -1 };
  return &_RxInternalOperatorsCachedObservable_CacheState_$1;
}

@end

void RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState_$1 *self, RxInternalOperatorsCachedObservable_CacheState *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  RxSubscriber_init(self);
}

RxInternalOperatorsCachedObservable_CacheState_$1 *new_RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCachedObservable_CacheState_$1, initWithRxInternalOperatorsCachedObservable_CacheState_, outer$)
}

RxInternalOperatorsCachedObservable_CacheState_$1 *create_RxInternalOperatorsCachedObservable_CacheState_$1_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCachedObservable_CacheState_$1, initWithRxInternalOperatorsCachedObservable_CacheState_, outer$)
}

@implementation RxInternalOperatorsCachedObservable_CachedSubscribe

- (instancetype)initWithRxInternalOperatorsCachedObservable_CacheState:(RxInternalOperatorsCachedObservable_CacheState *)state {
  RxInternalOperatorsCachedObservable_CachedSubscribe_initWithRxInternalOperatorsCachedObservable_CacheState_(self, state);
  return self;
}

- (void)callWithId:(RxSubscriber *)t {
  RxInternalOperatorsCachedObservable_ReplayProducer *rp = create_RxInternalOperatorsCachedObservable_ReplayProducer_initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_(t, state_);
  [((RxInternalOperatorsCachedObservable_CacheState *) nil_chk(state_)) addProducerWithRxInternalOperatorsCachedObservable_ReplayProducer:rp];
  [((RxSubscriber *) nil_chk(t)) addWithRxSubscription:rp];
  [t setProducerWithRxProducer:rp];
  if (![self get] && [self compareAndSetWithBoolean:false withBoolean:true]) {
    [state_ connect];
  }
}

- (void)dealloc {
  RELEASE_(state_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxInternalOperatorsCachedObservable_CacheState:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxInternalOperatorsCachedObservable_CachedSubscribe_serialVersionUID, 0x1a, -1, -1, -1, -1 },
    { "state_", "LRxInternalOperatorsCachedObservable_CacheState;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsCachedObservable_CacheState;", "(Lrx/internal/operators/CachedObservable$CacheState<TT;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)V", "Lrx/internal/operators/CachedObservable$CacheState<TT;>;", "LRxInternalOperatorsCachedObservable;", "<T:Ljava/lang/Object;>Ljava/util/concurrent/atomic/AtomicBoolean;Lrx/Observable$OnSubscribe<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCachedObservable_CachedSubscribe = { "CachedSubscribe", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 2, 2, 6, -1, -1, 7, -1 };
  return &_RxInternalOperatorsCachedObservable_CachedSubscribe;
}

@end

void RxInternalOperatorsCachedObservable_CachedSubscribe_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CachedSubscribe *self, RxInternalOperatorsCachedObservable_CacheState *state) {
  JavaUtilConcurrentAtomicAtomicBoolean_init(self);
  JreStrongAssign(&self->state_, state);
}

RxInternalOperatorsCachedObservable_CachedSubscribe *new_RxInternalOperatorsCachedObservable_CachedSubscribe_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState *state) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCachedObservable_CachedSubscribe, initWithRxInternalOperatorsCachedObservable_CacheState_, state)
}

RxInternalOperatorsCachedObservable_CachedSubscribe *create_RxInternalOperatorsCachedObservable_CachedSubscribe_initWithRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_CacheState *state) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCachedObservable_CachedSubscribe, initWithRxInternalOperatorsCachedObservable_CacheState_, state)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsCachedObservable_CachedSubscribe)

@implementation RxInternalOperatorsCachedObservable_ReplayProducer

- (instancetype)initWithRxSubscriber:(RxSubscriber *)child
withRxInternalOperatorsCachedObservable_CacheState:(RxInternalOperatorsCachedObservable_CacheState *)state {
  RxInternalOperatorsCachedObservable_ReplayProducer_initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_(self, child, state);
  return self;
}

- (void)requestWithLong:(jlong)n {
  for (; ; ) {
    jlong r = [self get];
    if (r < 0) {
      return;
    }
    jlong u = r + n;
    if (u < 0) {
      u = JavaLangLong_MAX_VALUE;
    }
    if ([self compareAndSetWithLong:r withLong:u]) {
      [self replay];
      return;
    }
  }
}

- (jlong)producedWithLong:(jlong)n {
  return [self addAndGetWithLong:-n];
}

- (jboolean)isUnsubscribed {
  return [self get] < 0;
}

- (void)unsubscribe {
  jlong r = [self get];
  if (r >= 0) {
    r = [self getAndSetWithLong:-1LL];
    if (r >= 0) {
      [((RxInternalOperatorsCachedObservable_CacheState *) nil_chk(state_)) removeProducerWithRxInternalOperatorsCachedObservable_ReplayProducer:self];
    }
  }
}

- (void)replay {
  @synchronized(self) {
    if (emitting_) {
      missed_ = true;
      return;
    }
    emitting_ = true;
  }
  jboolean skipFinal = false;
  @try {
    RxSubscriber *child = self->child_;
    for (; ; ) {
      jlong r = [self get];
      if (r < 0LL) {
        skipFinal = true;
        return;
      }
      jint s = [((RxInternalOperatorsCachedObservable_CacheState *) nil_chk(state_)) size];
      if (s != 0) {
        IOSObjectArray *b = currentBuffer_;
        if (b == nil) {
          b = [state_ head];
          JreStrongAssign(&currentBuffer_, b);
        }
        jint n = ((IOSObjectArray *) nil_chk(b))->size_ - 1;
        jint j = index_;
        jint k = currentIndexInBuffer_;
        if (r == 0) {
          id o = IOSObjectArray_Get(b, k);
          if (RxInternalOperatorsNotificationLite_isCompletedWithId_(o)) {
            [((RxSubscriber *) nil_chk(child)) onCompleted];
            skipFinal = true;
            [self unsubscribe];
            return;
          }
          else if (RxInternalOperatorsNotificationLite_isErrorWithId_(o)) {
            [((RxSubscriber *) nil_chk(child)) onErrorWithNSException:RxInternalOperatorsNotificationLite_getErrorWithId_(o)];
            skipFinal = true;
            [self unsubscribe];
            return;
          }
        }
        else if (r > 0) {
          jint valuesProduced = 0;
          while (j < s && r > 0) {
            if ([((RxSubscriber *) nil_chk(child)) isUnsubscribed]) {
              skipFinal = true;
              return;
            }
            if (k == n) {
              b = (IOSObjectArray *) cast_check(IOSObjectArray_Get(b, n), IOSClass_arrayType(NSObject_class_(), 1));
              k = 0;
            }
            id o = IOSObjectArray_Get(nil_chk(b), k);
            @try {
              if (RxInternalOperatorsNotificationLite_acceptWithRxObserver_withId_(child, o)) {
                skipFinal = true;
                [self unsubscribe];
                return;
              }
            }
            @catch (NSException *err) {
              RxExceptionsExceptions_throwIfFatalWithNSException_(err);
              skipFinal = true;
              [self unsubscribe];
              if (!RxInternalOperatorsNotificationLite_isErrorWithId_(o) && !RxInternalOperatorsNotificationLite_isCompletedWithId_(o)) {
                [child onErrorWithNSException:RxExceptionsOnErrorThrowable_addValueAsLastCauseWithNSException_withId_(err, RxInternalOperatorsNotificationLite_getValueWithId_(o))];
              }
              return;
            }
            k++;
            j++;
            r--;
            valuesProduced++;
          }
          if ([((RxSubscriber *) nil_chk(child)) isUnsubscribed]) {
            skipFinal = true;
            return;
          }
          index_ = j;
          currentIndexInBuffer_ = k;
          JreStrongAssign(&currentBuffer_, b);
          [self producedWithLong:valuesProduced];
        }
      }
      @synchronized(self) {
        if (!missed_) {
          emitting_ = false;
          skipFinal = true;
          return;
        }
        missed_ = false;
      }
    }
  }
  @finally {
    if (!skipFinal) {
      @synchronized(self) {
        emitting_ = false;
      }
    }
  }
}

- (void)__javaClone:(RxInternalOperatorsCachedObservable_ReplayProducer *)original {
  [super __javaClone:original];
  [state_ release];
}

- (void)dealloc {
  RELEASE_(child_);
  RELEASE_(currentBuffer_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "J", 0x1, 4, 3, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withRxInternalOperatorsCachedObservable_CacheState:);
  methods[1].selector = @selector(requestWithLong:);
  methods[2].selector = @selector(producedWithLong:);
  methods[3].selector = @selector(isUnsubscribed);
  methods[4].selector = @selector(unsubscribe);
  methods[5].selector = @selector(replay);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxInternalOperatorsCachedObservable_ReplayProducer_serialVersionUID, 0x1a, -1, -1, -1, -1 },
    { "child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "state_", "LRxInternalOperatorsCachedObservable_CacheState;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
    { "currentBuffer_", "[LNSObject;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "currentIndexInBuffer_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "index_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "emitting_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "missed_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;LRxInternalOperatorsCachedObservable_CacheState;", "(Lrx/Subscriber<-TT;>;Lrx/internal/operators/CachedObservable$CacheState<TT;>;)V", "request", "J", "produced", "Lrx/Subscriber<-TT;>;", "Lrx/internal/operators/CachedObservable$CacheState<TT;>;", "LRxInternalOperatorsCachedObservable;", "<T:Ljava/lang/Object;>Ljava/util/concurrent/atomic/AtomicLong;Lrx/Producer;Lrx/Subscription;" };
  static const J2ObjcClassInfo _RxInternalOperatorsCachedObservable_ReplayProducer = { "ReplayProducer", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 6, 8, 7, -1, -1, 8, -1 };
  return &_RxInternalOperatorsCachedObservable_ReplayProducer;
}

@end

void RxInternalOperatorsCachedObservable_ReplayProducer_initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_(RxInternalOperatorsCachedObservable_ReplayProducer *self, RxSubscriber *child, RxInternalOperatorsCachedObservable_CacheState *state) {
  JavaUtilConcurrentAtomicAtomicLong_init(self);
  JreStrongAssign(&self->child_, child);
  self->state_ = state;
}

RxInternalOperatorsCachedObservable_ReplayProducer *new_RxInternalOperatorsCachedObservable_ReplayProducer_initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_(RxSubscriber *child, RxInternalOperatorsCachedObservable_CacheState *state) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsCachedObservable_ReplayProducer, initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_, child, state)
}

RxInternalOperatorsCachedObservable_ReplayProducer *create_RxInternalOperatorsCachedObservable_ReplayProducer_initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_(RxSubscriber *child, RxInternalOperatorsCachedObservable_CacheState *state) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsCachedObservable_ReplayProducer, initWithRxSubscriber_withRxInternalOperatorsCachedObservable_CacheState_, child, state)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsCachedObservable_ReplayProducer)
