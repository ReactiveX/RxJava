//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/subjects/PublishSubject.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxExceptionsMissingBackpressureException.h"
#include "RxInternalOperatorsBackpressureUtils.h"
#include "RxSubjectsPublishSubject.h"
#include "RxSubjectsSubject.h"
#include "RxSubscriber.h"
#include "java/lang/Long.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.h"
#include "java/util/List.h"
#include "java/util/concurrent/atomic/AtomicLong.h"
#include "java/util/concurrent/atomic/AtomicReference.h"

#pragma clang diagnostic ignored "-Wincomplete-implementation"

inline jlong RxSubjectsPublishSubject_PublishSubjectState_get_serialVersionUID();
#define RxSubjectsPublishSubject_PublishSubjectState_serialVersionUID -7568940796666027140LL
J2OBJC_STATIC_FIELD_CONSTANT(RxSubjectsPublishSubject_PublishSubjectState, serialVersionUID, jlong)

inline jlong RxSubjectsPublishSubject_PublishSubjectProducer_get_serialVersionUID();
#define RxSubjectsPublishSubject_PublishSubjectProducer_serialVersionUID 6451806817170721536LL
J2OBJC_STATIC_FIELD_CONSTANT(RxSubjectsPublishSubject_PublishSubjectProducer, serialVersionUID, jlong)

@implementation RxSubjectsPublishSubject

+ (RxSubjectsPublishSubject *)create {
  return RxSubjectsPublishSubject_create();
}

- (instancetype)initWithRxSubjectsPublishSubject_PublishSubjectState:(RxSubjectsPublishSubject_PublishSubjectState *)state {
  RxSubjectsPublishSubject_initWithRxSubjectsPublishSubject_PublishSubjectState_(self, state);
  return self;
}

- (void)onNextWithId:(id)v {
  [((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) onNextWithId:v];
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  [((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) onCompleted];
}

- (jboolean)hasObservers {
  return ((IOSObjectArray *) nil_chk([((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) get]))->size_ != 0;
}

- (jboolean)hasThrowable {
  return [((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) get] == JreLoadStatic(RxSubjectsPublishSubject_PublishSubjectState, TERMINATED) && state_->error_ != nil;
}

- (jboolean)hasCompleted {
  return [((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) get] == JreLoadStatic(RxSubjectsPublishSubject_PublishSubjectState, TERMINATED) && state_->error_ == nil;
}

- (NSException *)getThrowable {
  if ([((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(state_)) get] == JreLoadStatic(RxSubjectsPublishSubject_PublishSubjectState, TERMINATED)) {
    return state_->error_;
  }
  return nil;
}

- (void)dealloc {
  RELEASE_(state_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxSubjectsPublishSubject;", 0x9, -1, -1, -1, 0, -1, -1 },
    { NULL, NULL, 0x4, -1, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, 5, -1, -1 },
    { NULL, "V", 0x1, 6, 7, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LNSException;", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(create);
  methods[1].selector = @selector(initWithRxSubjectsPublishSubject_PublishSubjectState:);
  methods[2].selector = @selector(onNextWithId:);
  methods[3].selector = @selector(onErrorWithNSException:);
  methods[4].selector = @selector(onCompleted);
  methods[5].selector = @selector(hasObservers);
  methods[6].selector = @selector(hasThrowable);
  methods[7].selector = @selector(hasCompleted);
  methods[8].selector = @selector(getThrowable);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "state_", "LRxSubjectsPublishSubject_PublishSubjectState;", .constantValue.asLong = 0, 0x10, -1, -1, 8, -1 },
  };
  static const void *ptrTable[] = { "<T:Ljava/lang/Object;>()Lrx/subjects/PublishSubject<TT;>;", "LRxSubjectsPublishSubject_PublishSubjectState;", "(Lrx/subjects/PublishSubject$PublishSubjectState<TT;>;)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "Lrx/subjects/PublishSubject$PublishSubjectState<TT;>;", "LRxSubjectsPublishSubject_PublishSubjectState;LRxSubjectsPublishSubject_PublishSubjectProducer;", "<T:Ljava/lang/Object;>Lrx/subjects/Subject<TT;TT;>;" };
  static const J2ObjcClassInfo _RxSubjectsPublishSubject = { "PublishSubject", "rx.subjects", ptrTable, methods, fields, 7, 0x11, 9, 1, -1, 9, -1, 10, -1 };
  return &_RxSubjectsPublishSubject;
}

@end

RxSubjectsPublishSubject *RxSubjectsPublishSubject_create() {
  RxSubjectsPublishSubject_initialize();
  return create_RxSubjectsPublishSubject_initWithRxSubjectsPublishSubject_PublishSubjectState_(create_RxSubjectsPublishSubject_PublishSubjectState_init());
}

void RxSubjectsPublishSubject_initWithRxSubjectsPublishSubject_PublishSubjectState_(RxSubjectsPublishSubject *self, RxSubjectsPublishSubject_PublishSubjectState *state) {
  RxSubjectsSubject_initWithRxObservable_OnSubscribe_(self, state);
  JreStrongAssign(&self->state_, state);
}

RxSubjectsPublishSubject *new_RxSubjectsPublishSubject_initWithRxSubjectsPublishSubject_PublishSubjectState_(RxSubjectsPublishSubject_PublishSubjectState *state) {
  J2OBJC_NEW_IMPL(RxSubjectsPublishSubject, initWithRxSubjectsPublishSubject_PublishSubjectState_, state)
}

RxSubjectsPublishSubject *create_RxSubjectsPublishSubject_initWithRxSubjectsPublishSubject_PublishSubjectState_(RxSubjectsPublishSubject_PublishSubjectState *state) {
  J2OBJC_CREATE_IMPL(RxSubjectsPublishSubject, initWithRxSubjectsPublishSubject_PublishSubjectState_, state)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxSubjectsPublishSubject)

J2OBJC_INITIALIZED_DEFN(RxSubjectsPublishSubject_PublishSubjectState)

IOSObjectArray *RxSubjectsPublishSubject_PublishSubjectState_EMPTY;
IOSObjectArray *RxSubjectsPublishSubject_PublishSubjectState_TERMINATED;

@implementation RxSubjectsPublishSubject_PublishSubjectState

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxSubjectsPublishSubject_PublishSubjectState_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)callWithId:(RxSubscriber *)t {
  RxSubjectsPublishSubject_PublishSubjectProducer *pp = create_RxSubjectsPublishSubject_PublishSubjectProducer_initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_(self, t);
  [((RxSubscriber *) nil_chk(t)) addWithRxSubscription:pp];
  [t setProducerWithRxProducer:pp];
  if ([self addWithRxSubjectsPublishSubject_PublishSubjectProducer:pp]) {
    if ([pp isUnsubscribed]) {
      [self removeWithRxSubjectsPublishSubject_PublishSubjectProducer:pp];
    }
  }
  else {
    NSException *ex = error_;
    if (ex != nil) {
      [t onErrorWithNSException:ex];
    }
    else {
      [t onCompleted];
    }
  }
}

- (jboolean)addWithRxSubjectsPublishSubject_PublishSubjectProducer:(RxSubjectsPublishSubject_PublishSubjectProducer *)inner {
  for (; ; ) {
    IOSObjectArray *curr = [self get];
    if (curr == RxSubjectsPublishSubject_PublishSubjectState_TERMINATED) {
      return false;
    }
    jint n = ((IOSObjectArray *) nil_chk(curr))->size_;
    IOSObjectArray *next = [IOSObjectArray arrayWithLength:n + 1 type:RxSubjectsPublishSubject_PublishSubjectProducer_class_()];
    JavaLangSystem_arraycopyWithId_withInt_withId_withInt_withInt_(curr, 0, next, 0, n);
    IOSObjectArray_Set(next, n, inner);
    if ([self compareAndSetWithId:curr withId:next]) {
      return true;
    }
  }
}

- (void)removeWithRxSubjectsPublishSubject_PublishSubjectProducer:(RxSubjectsPublishSubject_PublishSubjectProducer *)inner {
  for (; ; ) {
    IOSObjectArray *curr = [self get];
    if (curr == RxSubjectsPublishSubject_PublishSubjectState_TERMINATED || curr == RxSubjectsPublishSubject_PublishSubjectState_EMPTY) {
      return;
    }
    jint n = ((IOSObjectArray *) nil_chk(curr))->size_;
    jint j = -1;
    for (jint i = 0; i < n; i++) {
      if (IOSObjectArray_Get(curr, i) == inner) {
        j = i;
        break;
      }
    }
    if (j < 0) {
      return;
    }
    IOSObjectArray *next;
    if (n == 1) {
      next = RxSubjectsPublishSubject_PublishSubjectState_EMPTY;
    }
    else {
      next = [IOSObjectArray arrayWithLength:n - 1 type:RxSubjectsPublishSubject_PublishSubjectProducer_class_()];
      JavaLangSystem_arraycopyWithId_withInt_withId_withInt_withInt_(curr, 0, next, 0, j);
      JavaLangSystem_arraycopyWithId_withInt_withId_withInt_withInt_(curr, j + 1, next, j, n - j - 1);
    }
    if ([self compareAndSetWithId:curr withId:next]) {
      return;
    }
  }
}

- (void)onNextWithId:(id)t {
  {
    IOSObjectArray *a__ = [self get];
    RxSubjectsPublishSubject_PublishSubjectProducer * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
    RxSubjectsPublishSubject_PublishSubjectProducer * const *e__ = b__ + a__->size_;
    while (b__ < e__) {
      RxSubjectsPublishSubject_PublishSubjectProducer *pp = *b__++;
      [((RxSubjectsPublishSubject_PublishSubjectProducer *) nil_chk(pp)) onNextWithId:t];
    }
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  JreStrongAssign(&error_, e);
  id<JavaUtilList> errors = nil;
  {
    IOSObjectArray *a__ = [self getAndSetWithId:RxSubjectsPublishSubject_PublishSubjectState_TERMINATED];
    RxSubjectsPublishSubject_PublishSubjectProducer * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
    RxSubjectsPublishSubject_PublishSubjectProducer * const *e__ = b__ + a__->size_;
    while (b__ < e__) {
      RxSubjectsPublishSubject_PublishSubjectProducer *pp = *b__++;
      @try {
        [((RxSubjectsPublishSubject_PublishSubjectProducer *) nil_chk(pp)) onErrorWithNSException:e];
      }
      @catch (NSException *ex) {
        if (errors == nil) {
          errors = create_JavaUtilArrayList_initWithInt_(1);
        }
        [errors addWithId:ex];
      }
    }
  }
  RxExceptionsExceptions_throwIfAnyWithJavaUtilList_(errors);
}

- (void)onCompleted {
  {
    IOSObjectArray *a__ = [self getAndSetWithId:RxSubjectsPublishSubject_PublishSubjectState_TERMINATED];
    RxSubjectsPublishSubject_PublishSubjectProducer * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
    RxSubjectsPublishSubject_PublishSubjectProducer * const *e__ = b__ + a__->size_;
    while (b__ < e__) {
      RxSubjectsPublishSubject_PublishSubjectProducer *pp = *b__++;
      [((RxSubjectsPublishSubject_PublishSubjectProducer *) nil_chk(pp)) onCompleted];
    }
  }
}

- (void)dealloc {
  RELEASE_(error_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "Z", 0x0, 3, 4, -1, 5, -1, -1 },
    { NULL, "V", 0x0, 6, 4, -1, 7, -1, -1 },
    { NULL, "V", 0x1, 8, 9, -1, 10, -1, -1 },
    { NULL, "V", 0x1, 11, 12, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(callWithId:);
  methods[2].selector = @selector(addWithRxSubjectsPublishSubject_PublishSubjectProducer:);
  methods[3].selector = @selector(removeWithRxSubjectsPublishSubject_PublishSubjectProducer:);
  methods[4].selector = @selector(onNextWithId:);
  methods[5].selector = @selector(onErrorWithNSException:);
  methods[6].selector = @selector(onCompleted);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxSubjectsPublishSubject_PublishSubjectState_serialVersionUID, 0x1a, -1, -1, -1, -1 },
    { "EMPTY", "[LRxSubjectsPublishSubject_PublishSubjectProducer;", .constantValue.asLong = 0, 0x18, -1, 13, -1, -1 },
    { "TERMINATED", "[LRxSubjectsPublishSubject_PublishSubjectProducer;", .constantValue.asLong = 0, 0x18, -1, 14, -1, -1 },
    { "error_", "LNSException;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)V", "add", "LRxSubjectsPublishSubject_PublishSubjectProducer;", "(Lrx/subjects/PublishSubject$PublishSubjectProducer<TT;>;)Z", "remove", "(Lrx/subjects/PublishSubject$PublishSubjectProducer<TT;>;)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", &RxSubjectsPublishSubject_PublishSubjectState_EMPTY, &RxSubjectsPublishSubject_PublishSubjectState_TERMINATED, "LRxSubjectsPublishSubject;", "<T:Ljava/lang/Object;>Ljava/util/concurrent/atomic/AtomicReference<[Lrx/subjects/PublishSubject$PublishSubjectProducer<TT;>;>;Lrx/Observable$OnSubscribe<TT;>;Lrx/Observer<TT;>;" };
  static const J2ObjcClassInfo _RxSubjectsPublishSubject_PublishSubjectState = { "PublishSubjectState", "rx.subjects", ptrTable, methods, fields, 7, 0x18, 7, 4, 15, -1, -1, 16, -1 };
  return &_RxSubjectsPublishSubject_PublishSubjectState;
}

+ (void)initialize {
  if (self == [RxSubjectsPublishSubject_PublishSubjectState class]) {
    JreStrongAssignAndConsume(&RxSubjectsPublishSubject_PublishSubjectState_EMPTY, [IOSObjectArray newArrayWithLength:0 type:RxSubjectsPublishSubject_PublishSubjectProducer_class_()]);
    JreStrongAssignAndConsume(&RxSubjectsPublishSubject_PublishSubjectState_TERMINATED, [IOSObjectArray newArrayWithLength:0 type:RxSubjectsPublishSubject_PublishSubjectProducer_class_()]);
    J2OBJC_SET_INITIALIZED(RxSubjectsPublishSubject_PublishSubjectState)
  }
}

@end

void RxSubjectsPublishSubject_PublishSubjectState_init(RxSubjectsPublishSubject_PublishSubjectState *self) {
  JavaUtilConcurrentAtomicAtomicReference_init(self);
  [self lazySetWithId:RxSubjectsPublishSubject_PublishSubjectState_EMPTY];
}

RxSubjectsPublishSubject_PublishSubjectState *new_RxSubjectsPublishSubject_PublishSubjectState_init() {
  J2OBJC_NEW_IMPL(RxSubjectsPublishSubject_PublishSubjectState, init)
}

RxSubjectsPublishSubject_PublishSubjectState *create_RxSubjectsPublishSubject_PublishSubjectState_init() {
  J2OBJC_CREATE_IMPL(RxSubjectsPublishSubject_PublishSubjectState, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxSubjectsPublishSubject_PublishSubjectState)

@implementation RxSubjectsPublishSubject_PublishSubjectProducer

- (instancetype)initWithRxSubjectsPublishSubject_PublishSubjectState:(RxSubjectsPublishSubject_PublishSubjectState *)parent
                                                    withRxSubscriber:(RxSubscriber *)actual {
  RxSubjectsPublishSubject_PublishSubjectProducer_initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_(self, parent, actual);
  return self;
}

- (void)requestWithLong:(jlong)n {
  if (RxInternalOperatorsBackpressureUtils_validateWithLong_(n)) {
    for (; ; ) {
      jlong r = [self get];
      if (r == JavaLangLong_MIN_VALUE) {
        return;
      }
      jlong u = RxInternalOperatorsBackpressureUtils_addCapWithLong_withLong_(r, n);
      if ([self compareAndSetWithLong:r withLong:u]) {
        return;
      }
    }
  }
}

- (jboolean)isUnsubscribed {
  return [self get] == JavaLangLong_MIN_VALUE;
}

- (void)unsubscribe {
  if ([self getAndSetWithLong:JavaLangLong_MIN_VALUE] != JavaLangLong_MIN_VALUE) {
    [((RxSubjectsPublishSubject_PublishSubjectState *) nil_chk(parent_)) removeWithRxSubjectsPublishSubject_PublishSubjectProducer:self];
  }
}

- (void)onNextWithId:(id)t {
  jlong r = [self get];
  if (r != JavaLangLong_MIN_VALUE) {
    jlong p = produced_;
    if (r != p) {
      produced_ = p + 1;
      [((RxSubscriber *) nil_chk(actual_)) onNextWithId:t];
    }
    else {
      [self unsubscribe];
      [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:create_RxExceptionsMissingBackpressureException_initWithNSString_(@"PublishSubject: could not emit value due to lack of requests")];
    }
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  if ([self get] != JavaLangLong_MIN_VALUE) {
    [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:e];
  }
}

- (void)onCompleted {
  if ([self get] != JavaLangLong_MIN_VALUE) {
    [((RxSubscriber *) nil_chk(actual_)) onCompleted];
  }
}

- (void)dealloc {
  RELEASE_(parent_);
  RELEASE_(actual_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, 6, -1, -1 },
    { NULL, "V", 0x1, 7, 8, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubjectsPublishSubject_PublishSubjectState:withRxSubscriber:);
  methods[1].selector = @selector(requestWithLong:);
  methods[2].selector = @selector(isUnsubscribed);
  methods[3].selector = @selector(unsubscribe);
  methods[4].selector = @selector(onNextWithId:);
  methods[5].selector = @selector(onErrorWithNSException:);
  methods[6].selector = @selector(onCompleted);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxSubjectsPublishSubject_PublishSubjectProducer_serialVersionUID, 0x1a, -1, -1, -1, -1 },
    { "parent_", "LRxSubjectsPublishSubject_PublishSubjectState;", .constantValue.asLong = 0, 0x10, -1, -1, 9, -1 },
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 10, -1 },
    { "produced_", "J", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxSubjectsPublishSubject_PublishSubjectState;LRxSubscriber;", "(Lrx/subjects/PublishSubject$PublishSubjectState<TT;>;Lrx/Subscriber<-TT;>;)V", "request", "J", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "Lrx/subjects/PublishSubject$PublishSubjectState<TT;>;", "Lrx/Subscriber<-TT;>;", "LRxSubjectsPublishSubject;", "<T:Ljava/lang/Object;>Ljava/util/concurrent/atomic/AtomicLong;Lrx/Producer;Lrx/Subscription;Lrx/Observer<TT;>;" };
  static const J2ObjcClassInfo _RxSubjectsPublishSubject_PublishSubjectProducer = { "PublishSubjectProducer", "rx.subjects", ptrTable, methods, fields, 7, 0x18, 7, 4, 11, -1, -1, 12, -1 };
  return &_RxSubjectsPublishSubject_PublishSubjectProducer;
}

@end

void RxSubjectsPublishSubject_PublishSubjectProducer_initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_(RxSubjectsPublishSubject_PublishSubjectProducer *self, RxSubjectsPublishSubject_PublishSubjectState *parent, RxSubscriber *actual) {
  JavaUtilConcurrentAtomicAtomicLong_init(self);
  JreStrongAssign(&self->parent_, parent);
  JreStrongAssign(&self->actual_, actual);
}

RxSubjectsPublishSubject_PublishSubjectProducer *new_RxSubjectsPublishSubject_PublishSubjectProducer_initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_(RxSubjectsPublishSubject_PublishSubjectState *parent, RxSubscriber *actual) {
  J2OBJC_NEW_IMPL(RxSubjectsPublishSubject_PublishSubjectProducer, initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_, parent, actual)
}

RxSubjectsPublishSubject_PublishSubjectProducer *create_RxSubjectsPublishSubject_PublishSubjectProducer_initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_(RxSubjectsPublishSubject_PublishSubjectState *parent, RxSubscriber *actual) {
  J2OBJC_CREATE_IMPL(RxSubjectsPublishSubject_PublishSubjectProducer, initWithRxSubjectsPublishSubject_PublishSubjectState_withRxSubscriber_, parent, actual)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxSubjectsPublishSubject_PublishSubjectProducer)
