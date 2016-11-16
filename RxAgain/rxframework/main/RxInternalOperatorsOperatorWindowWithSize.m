//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorWindowWithSize.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxInternalOperatorsBackpressureUtils.h"
#include "RxInternalOperatorsOperatorWindowWithSize.h"
#include "RxInternalUtilAtomicSpscLinkedArrayQueue.h"
#include "RxProducer.h"
#include "RxSubjectsSubject.h"
#include "RxSubjectsUnicastSubject.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "RxSubscriptionsSubscriptions.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Long.h"
#include "java/util/ArrayDeque.h"
#include "java/util/Queue.h"
#include "java/util/concurrent/atomic/AtomicBoolean.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"
#include "java/util/concurrent/atomic/AtomicLong.h"

@interface RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 : NSObject < RxProducer > {
 @public
  RxInternalOperatorsOperatorWindowWithSize_WindowExact *this$0_;
}

- (void)requestWithLong:(jlong)n;

- (instancetype)initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact:(RxInternalOperatorsOperatorWindowWithSize_WindowExact *)outer$;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1, this$0_, RxInternalOperatorsOperatorWindowWithSize_WindowExact *)

__attribute__((unused)) static void RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 *self, RxInternalOperatorsOperatorWindowWithSize_WindowExact *outer$);

__attribute__((unused)) static RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 *new_RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(RxInternalOperatorsOperatorWindowWithSize_WindowExact *outer$) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 *create_RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(RxInternalOperatorsOperatorWindowWithSize_WindowExact *outer$);

@interface RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer () {
 @public
  RxInternalOperatorsOperatorWindowWithSize_WindowSkip *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer, this$0_, RxInternalOperatorsOperatorWindowWithSize_WindowSkip *)

inline jlong RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_get_serialVersionUID();
#define RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_serialVersionUID 4625807964358024108LL
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer, serialVersionUID, jlong)

@interface RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer () {
 @public
  RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer, this$0_, RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *)

inline jlong RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_get_serialVersionUID();
#define RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_serialVersionUID 4625807964358024108LL
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer, serialVersionUID, jlong)

@implementation RxInternalOperatorsOperatorWindowWithSize

- (instancetype)initWithInt:(jint)size
                    withInt:(jint)skip {
  RxInternalOperatorsOperatorWindowWithSize_initWithInt_withInt_(self, size, skip);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  if (skip_ == size_) {
    RxInternalOperatorsOperatorWindowWithSize_WindowExact *parent = create_RxInternalOperatorsOperatorWindowWithSize_WindowExact_initWithRxSubscriber_withInt_(child, size_);
    [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:parent->cancel_];
    [child setProducerWithRxProducer:[parent createProducer]];
    return parent;
  }
  else if (skip_ > size_) {
    RxInternalOperatorsOperatorWindowWithSize_WindowSkip *parent = create_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_initWithRxSubscriber_withInt_withInt_(child, size_, skip_);
    [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:parent->cancel_];
    [child setProducerWithRxProducer:[parent createProducer]];
    return parent;
  }
  RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *parent = create_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_initWithRxSubscriber_withInt_withInt_(child, size_, skip_);
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:parent->cancel_];
  [child setProducerWithRxProducer:[parent createProducer]];
  return parent;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 1, 2, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithInt:withInt:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "size_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "skip_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "II", "call", "LRxSubscriber;", "(Lrx/Subscriber<-Lrx/Observable<TT;>;>;)Lrx/Subscriber<-TT;>;", "LRxInternalOperatorsOperatorWindowWithSize_WindowExact;LRxInternalOperatorsOperatorWindowWithSize_WindowSkip;LRxInternalOperatorsOperatorWindowWithSize_WindowOverlap;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<Lrx/Observable<TT;>;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize = { "OperatorWindowWithSize", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 2, -1, 4, -1, 5, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_initWithInt_withInt_(RxInternalOperatorsOperatorWindowWithSize *self, jint size, jint skip) {
  NSObject_init(self);
  self->size_ = size;
  self->skip_ = skip;
}

RxInternalOperatorsOperatorWindowWithSize *new_RxInternalOperatorsOperatorWindowWithSize_initWithInt_withInt_(jint size, jint skip) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize, initWithInt_withInt_, size, skip)
}

RxInternalOperatorsOperatorWindowWithSize *create_RxInternalOperatorsOperatorWindowWithSize_initWithInt_withInt_(jint size, jint skip) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize, initWithInt_withInt_, size, skip)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorWindowWithSize)

@implementation RxInternalOperatorsOperatorWindowWithSize_WindowExact

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                             withInt:(jint)size {
  RxInternalOperatorsOperatorWindowWithSize_WindowExact_initWithRxSubscriber_withInt_(self, actual, size);
  return self;
}

- (void)onNextWithId:(id)t {
  jint i = index_;
  RxSubjectsSubject *w = window_;
  if (i == 0) {
    [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) getAndIncrement];
    w = RxSubjectsUnicastSubject_createWithInt_withRxFunctionsAction0_(size_, self);
    JreStrongAssign(&window_, w);
    [((RxSubscriber *) nil_chk(actual_)) onNextWithId:w];
  }
  i++;
  [((RxSubjectsSubject *) nil_chk(w)) onNextWithId:t];
  if (i == size_) {
    index_ = 0;
    JreStrongAssign(&window_, nil);
    [w onCompleted];
  }
  else {
    index_ = i;
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  RxSubjectsSubject *w = window_;
  if (w != nil) {
    JreStrongAssign(&window_, nil);
    [w onErrorWithNSException:e];
  }
  [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  RxSubjectsSubject *w = window_;
  if (w != nil) {
    JreStrongAssign(&window_, nil);
    [w onCompleted];
  }
  [((RxSubscriber *) nil_chk(actual_)) onCompleted];
}

- (id<RxProducer>)createProducer {
  return create_RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(self);
}

- (void)call {
  if ([((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) decrementAndGet] == 0) {
    [self unsubscribe];
  }
}

- (void)dealloc {
  RELEASE_(actual_);
  RELEASE_(wip_);
  RELEASE_(cancel_);
  RELEASE_(window_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxProducer;", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withInt:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(createProducer);
  methods[5].selector = @selector(call);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 7, -1 },
    { "size_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "wip_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "cancel_", "LRxSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "index_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "window_", "LRxSubjectsSubject;", .constantValue.asLong = 0, 0x0, -1, -1, 8, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;I", "(Lrx/Subscriber<-Lrx/Observable<TT;>;>;I)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "Lrx/Subscriber<-Lrx/Observable<TT;>;>;", "Lrx/subjects/Subject<TT;TT;>;", "LRxInternalOperatorsOperatorWindowWithSize;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;Lrx/functions/Action0;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize_WindowExact = { "WindowExact", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 6, 6, 9, -1, -1, 10, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize_WindowExact;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_WindowExact_initWithRxSubscriber_withInt_(RxInternalOperatorsOperatorWindowWithSize_WindowExact *self, RxSubscriber *actual, jint size) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->actual_, actual);
  self->size_ = size;
  JreStrongAssignAndConsume(&self->wip_, new_JavaUtilConcurrentAtomicAtomicInteger_initWithInt_(1));
  JreStrongAssign(&self->cancel_, RxSubscriptionsSubscriptions_createWithRxFunctionsAction0_(self));
  [self addWithRxSubscription:self->cancel_];
  [self requestWithLong:0];
}

RxInternalOperatorsOperatorWindowWithSize_WindowExact *new_RxInternalOperatorsOperatorWindowWithSize_WindowExact_initWithRxSubscriber_withInt_(RxSubscriber *actual, jint size) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowExact, initWithRxSubscriber_withInt_, actual, size)
}

RxInternalOperatorsOperatorWindowWithSize_WindowExact *create_RxInternalOperatorsOperatorWindowWithSize_WindowExact_initWithRxSubscriber_withInt_(RxSubscriber *actual, jint size) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowExact, initWithRxSubscriber_withInt_, actual, size)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorWindowWithSize_WindowExact)

@implementation RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1

- (void)requestWithLong:(jlong)n {
  if (n < 0LL) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$J", @"n >= 0 required but it was ", n));
  }
  if (n != 0LL) {
    jlong u = RxInternalOperatorsBackpressureUtils_multiplyCapWithLong_withLong_(this$0_->size_, n);
    [this$0_ requestWithLong:u];
  }
}

- (instancetype)initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact:(RxInternalOperatorsOperatorWindowWithSize_WindowExact *)outer$ {
  RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(self, outer$);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorWindowWithSize_WindowExact;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOperatorWindowWithSize_WindowExact;", "createProducer" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 2, -1, 3, -1, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 *self, RxInternalOperatorsOperatorWindowWithSize_WindowExact *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  NSObject_init(self);
}

RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 *new_RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(RxInternalOperatorsOperatorWindowWithSize_WindowExact *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1, initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_, outer$)
}

RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1 *create_RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1_initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_(RxInternalOperatorsOperatorWindowWithSize_WindowExact *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowExact_$1, initWithRxInternalOperatorsOperatorWindowWithSize_WindowExact_, outer$)
}

@implementation RxInternalOperatorsOperatorWindowWithSize_WindowSkip

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                             withInt:(jint)size
                             withInt:(jint)skip {
  RxInternalOperatorsOperatorWindowWithSize_WindowSkip_initWithRxSubscriber_withInt_withInt_(self, actual, size, skip);
  return self;
}

- (void)onNextWithId:(id)t {
  jint i = index_;
  RxSubjectsSubject *w = window_;
  if (i == 0) {
    [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) getAndIncrement];
    w = RxSubjectsUnicastSubject_createWithInt_withRxFunctionsAction0_(size_, self);
    JreStrongAssign(&window_, w);
    [((RxSubscriber *) nil_chk(actual_)) onNextWithId:w];
  }
  i++;
  if (w != nil) {
    [w onNextWithId:t];
  }
  if (i == size_) {
    index_ = i;
    JreStrongAssign(&window_, nil);
    [((RxSubjectsSubject *) nil_chk(w)) onCompleted];
  }
  else if (i == skip_) {
    index_ = 0;
  }
  else {
    index_ = i;
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  RxSubjectsSubject *w = window_;
  if (w != nil) {
    JreStrongAssign(&window_, nil);
    [w onErrorWithNSException:e];
  }
  [((RxSubscriber *) nil_chk(actual_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  RxSubjectsSubject *w = window_;
  if (w != nil) {
    JreStrongAssign(&window_, nil);
    [w onCompleted];
  }
  [((RxSubscriber *) nil_chk(actual_)) onCompleted];
}

- (id<RxProducer>)createProducer {
  return create_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_(self);
}

- (void)call {
  if ([((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) decrementAndGet] == 0) {
    [self unsubscribe];
  }
}

- (void)dealloc {
  RELEASE_(actual_);
  RELEASE_(wip_);
  RELEASE_(cancel_);
  RELEASE_(window_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxProducer;", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withInt:withInt:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(createProducer);
  methods[5].selector = @selector(call);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 7, -1 },
    { "size_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "skip_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "wip_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "cancel_", "LRxSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "index_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "window_", "LRxSubjectsSubject;", .constantValue.asLong = 0, 0x0, -1, -1, 8, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;II", "(Lrx/Subscriber<-Lrx/Observable<TT;>;>;II)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "Lrx/Subscriber<-Lrx/Observable<TT;>;>;", "Lrx/subjects/Subject<TT;TT;>;", "LRxInternalOperatorsOperatorWindowWithSize;", "LRxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;Lrx/functions/Action0;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize_WindowSkip = { "WindowSkip", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 6, 7, 9, 10, -1, 11, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize_WindowSkip;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_WindowSkip_initWithRxSubscriber_withInt_withInt_(RxInternalOperatorsOperatorWindowWithSize_WindowSkip *self, RxSubscriber *actual, jint size, jint skip) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->actual_, actual);
  self->size_ = size;
  self->skip_ = skip;
  JreStrongAssignAndConsume(&self->wip_, new_JavaUtilConcurrentAtomicAtomicInteger_initWithInt_(1));
  JreStrongAssign(&self->cancel_, RxSubscriptionsSubscriptions_createWithRxFunctionsAction0_(self));
  [self addWithRxSubscription:self->cancel_];
  [self requestWithLong:0];
}

RxInternalOperatorsOperatorWindowWithSize_WindowSkip *new_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_initWithRxSubscriber_withInt_withInt_(RxSubscriber *actual, jint size, jint skip) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowSkip, initWithRxSubscriber_withInt_withInt_, actual, size, skip)
}

RxInternalOperatorsOperatorWindowWithSize_WindowSkip *create_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_initWithRxSubscriber_withInt_withInt_(RxSubscriber *actual, jint size, jint skip) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowSkip, initWithRxSubscriber_withInt_withInt_, actual, size, skip)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorWindowWithSize_WindowSkip)

@implementation RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer

- (void)requestWithLong:(jlong)n {
  if (n < 0LL) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$J", @"n >= 0 required but it was ", n));
  }
  if (n != 0LL) {
    RxInternalOperatorsOperatorWindowWithSize_WindowSkip *parent = this$0_;
    if (![self get] && [self compareAndSetWithBoolean:false withBoolean:true]) {
      jlong u = RxInternalOperatorsBackpressureUtils_multiplyCapWithLong_withLong_(n, parent->size_);
      jlong v = RxInternalOperatorsBackpressureUtils_multiplyCapWithLong_withLong_(parent->skip_ - parent->size_, n - 1);
      jlong w = RxInternalOperatorsBackpressureUtils_addCapWithLong_withLong_(u, v);
      [parent requestWithLong:w];
    }
    else {
      jlong u = RxInternalOperatorsBackpressureUtils_multiplyCapWithLong_withLong_(n, parent->skip_);
      [parent requestWithLong:u];
    }
  }
}

- (instancetype)initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip:(RxInternalOperatorsOperatorWindowWithSize_WindowSkip *)outer$ {
  RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_(self, outer$);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorWindowWithSize_WindowSkip;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "serialVersionUID", "J", .constantValue.asLong = RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_serialVersionUID, 0x1a, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOperatorWindowWithSize_WindowSkip;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer = { "WindowSkipProducer", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 2, 2, 2, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_(RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer *self, RxInternalOperatorsOperatorWindowWithSize_WindowSkip *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  JavaUtilConcurrentAtomicAtomicBoolean_init(self);
}

RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer *new_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_(RxInternalOperatorsOperatorWindowWithSize_WindowSkip *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer, initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_, outer$)
}

RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer *create_RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_(RxInternalOperatorsOperatorWindowWithSize_WindowSkip *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer, initWithRxInternalOperatorsOperatorWindowWithSize_WindowSkip_, outer$)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorWindowWithSize_WindowSkip_WindowSkipProducer)

@implementation RxInternalOperatorsOperatorWindowWithSize_WindowOverlap

- (instancetype)initWithRxSubscriber:(RxSubscriber *)actual
                             withInt:(jint)size
                             withInt:(jint)skip {
  RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_initWithRxSubscriber_withInt_withInt_(self, actual, size, skip);
  return self;
}

- (void)onNextWithId:(id)t {
  jint i = index_;
  JavaUtilArrayDeque *q = windows_;
  if (i == 0 && ![((RxSubscriber *) nil_chk(actual_)) isUnsubscribed]) {
    [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) getAndIncrement];
    RxSubjectsSubject *w = RxSubjectsUnicastSubject_createWithInt_withRxFunctionsAction0_(16, self);
    [((JavaUtilArrayDeque *) nil_chk(q)) offerWithId:w];
    [((id<JavaUtilQueue>) nil_chk(queue_)) offerWithId:w];
    [self drain];
  }
  for (RxSubjectsSubject * __strong w in nil_chk(windows_)) {
    [((RxSubjectsSubject *) nil_chk(w)) onNextWithId:t];
  }
  jint p = produced_ + 1;
  if (p == size_) {
    produced_ = p - skip_;
    RxSubjectsSubject *w = [((JavaUtilArrayDeque *) nil_chk(q)) poll];
    if (w != nil) {
      [w onCompleted];
    }
  }
  else {
    produced_ = p;
  }
  i++;
  if (i == skip_) {
    index_ = 0;
  }
  else {
    index_ = i;
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  for (RxSubjectsSubject * __strong w in nil_chk(windows_)) {
    [((RxSubjectsSubject *) nil_chk(w)) onErrorWithNSException:e];
  }
  [windows_ clear];
  JreStrongAssign(&error_, e);
  JreAssignVolatileBoolean(&done_, true);
  [self drain];
}

- (void)onCompleted {
  for (RxSubjectsSubject * __strong w in nil_chk(windows_)) {
    [((RxSubjectsSubject *) nil_chk(w)) onCompleted];
  }
  [windows_ clear];
  JreAssignVolatileBoolean(&done_, true);
  [self drain];
}

- (id<RxProducer>)createProducer {
  return create_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_(self);
}

- (void)call {
  if ([((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(wip_)) decrementAndGet] == 0) {
    [self unsubscribe];
  }
}

- (void)drain {
  JavaUtilConcurrentAtomicAtomicInteger *dw = drainWip_;
  if ([((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(dw)) getAndIncrement] != 0) {
    return;
  }
  RxSubscriber *a = actual_;
  id<JavaUtilQueue> q = queue_;
  jint missed = 1;
  for (; ; ) {
    jlong r = [((JavaUtilConcurrentAtomicAtomicLong *) nil_chk(requested_WindowOverlap_)) get];
    jlong e = 0LL;
    while (e != r) {
      jboolean d = JreLoadVolatileBoolean(&done_);
      RxSubjectsSubject *v = [((id<JavaUtilQueue>) nil_chk(q)) poll];
      jboolean empty = v == nil;
      if ([self checkTerminatedWithBoolean:d withBoolean:empty withRxSubscriber:a withJavaUtilQueue:q]) {
        return;
      }
      if (empty) {
        break;
      }
      [((RxSubscriber *) nil_chk(a)) onNextWithId:v];
      e++;
    }
    if (e == r) {
      if ([self checkTerminatedWithBoolean:JreLoadVolatileBoolean(&done_) withBoolean:[((id<JavaUtilQueue>) nil_chk(q)) isEmpty] withRxSubscriber:a withJavaUtilQueue:q]) {
        return;
      }
    }
    if (e != 0 && r != JavaLangLong_MAX_VALUE) {
      [requested_WindowOverlap_ addAndGetWithLong:-e];
    }
    missed = [dw addAndGetWithInt:-missed];
    if (missed == 0) {
      break;
    }
  }
}

- (jboolean)checkTerminatedWithBoolean:(jboolean)d
                           withBoolean:(jboolean)empty
                      withRxSubscriber:(RxSubscriber *)a
                     withJavaUtilQueue:(id<JavaUtilQueue>)q {
  if ([((RxSubscriber *) nil_chk(a)) isUnsubscribed]) {
    [((id<JavaUtilQueue>) nil_chk(q)) clear];
    return true;
  }
  if (d) {
    NSException *e = error_;
    if (e != nil) {
      [((id<JavaUtilQueue>) nil_chk(q)) clear];
      [a onErrorWithNSException:e];
      return true;
    }
    else if (empty) {
      [a onCompleted];
      return true;
    }
  }
  return false;
}

- (void)dealloc {
  RELEASE_(actual_);
  RELEASE_(wip_);
  RELEASE_(cancel_);
  RELEASE_(windows_);
  RELEASE_(requested_WindowOverlap_);
  RELEASE_(drainWip_);
  RELEASE_(queue_);
  RELEASE_(error_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxProducer;", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x0, 7, 8, -1, 9, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxSubscriber:withInt:withInt:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(createProducer);
  methods[5].selector = @selector(call);
  methods[6].selector = @selector(drain);
  methods[7].selector = @selector(checkTerminatedWithBoolean:withBoolean:withRxSubscriber:withJavaUtilQueue:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "actual_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 10, -1 },
    { "size_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "skip_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "wip_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "cancel_", "LRxSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "windows_", "LJavaUtilArrayDeque;", .constantValue.asLong = 0, 0x10, -1, -1, 11, -1 },
    { "requested_WindowOverlap_", "LJavaUtilConcurrentAtomicAtomicLong;", .constantValue.asLong = 0, 0x10, 12, -1, -1, -1 },
    { "drainWip_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "queue_", "LJavaUtilQueue;", .constantValue.asLong = 0, 0x10, -1, -1, 13, -1 },
    { "error_", "LNSException;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "done_", "Z", .constantValue.asLong = 0, 0x40, -1, -1, -1, -1 },
    { "index_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "produced_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxSubscriber;II", "(Lrx/Subscriber<-Lrx/Observable<TT;>;>;II)V", "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "checkTerminated", "ZZLRxSubscriber;LJavaUtilQueue;", "(ZZLrx/Subscriber<-Lrx/subjects/Subject<TT;TT;>;>;Ljava/util/Queue<Lrx/subjects/Subject<TT;TT;>;>;)Z", "Lrx/Subscriber<-Lrx/Observable<TT;>;>;", "Ljava/util/ArrayDeque<Lrx/subjects/Subject<TT;TT;>;>;", "requested", "Ljava/util/Queue<Lrx/subjects/Subject<TT;TT;>;>;", "LRxInternalOperatorsOperatorWindowWithSize;", "LRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer;", "<T:Ljava/lang/Object;>Lrx/Subscriber<TT;>;Lrx/functions/Action0;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize_WindowOverlap = { "WindowOverlap", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 8, 13, 14, 15, -1, 16, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_initWithRxSubscriber_withInt_withInt_(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *self, RxSubscriber *actual, jint size, jint skip) {
  RxSubscriber_init(self);
  JreStrongAssign(&self->actual_, actual);
  self->size_ = size;
  self->skip_ = skip;
  JreStrongAssignAndConsume(&self->wip_, new_JavaUtilConcurrentAtomicAtomicInteger_initWithInt_(1));
  JreStrongAssignAndConsume(&self->windows_, new_JavaUtilArrayDeque_init());
  JreStrongAssignAndConsume(&self->drainWip_, new_JavaUtilConcurrentAtomicAtomicInteger_init());
  JreStrongAssignAndConsume(&self->requested_WindowOverlap_, new_JavaUtilConcurrentAtomicAtomicLong_init());
  JreStrongAssign(&self->cancel_, RxSubscriptionsSubscriptions_createWithRxFunctionsAction0_(self));
  [self addWithRxSubscription:self->cancel_];
  [self requestWithLong:0];
  jint maxWindows = (size + (skip - 1)) / skip;
  JreStrongAssignAndConsume(&self->queue_, new_RxInternalUtilAtomicSpscLinkedArrayQueue_initWithInt_(maxWindows));
}

RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *new_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_initWithRxSubscriber_withInt_withInt_(RxSubscriber *actual, jint size, jint skip) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap, initWithRxSubscriber_withInt_withInt_, actual, size, skip)
}

RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *create_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_initWithRxSubscriber_withInt_withInt_(RxSubscriber *actual, jint size, jint skip) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap, initWithRxSubscriber_withInt_withInt_, actual, size, skip)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap)

@implementation RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer

- (void)requestWithLong:(jlong)n {
  if (n < 0LL) {
    @throw create_JavaLangIllegalArgumentException_initWithNSString_(JreStrcat("$J", @"n >= 0 required but it was ", n));
  }
  if (n != 0LL) {
    RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *parent = this$0_;
    if (![self get] && [self compareAndSetWithBoolean:false withBoolean:true]) {
      jlong u = RxInternalOperatorsBackpressureUtils_multiplyCapWithLong_withLong_(parent->skip_, n - 1);
      jlong v = RxInternalOperatorsBackpressureUtils_addCapWithLong_withLong_(u, parent->size_);
      [parent requestWithLong:v];
    }
    else {
      jlong u = RxInternalOperatorsBackpressureUtils_multiplyCapWithLong_withLong_(parent->skip_, n);
      [this$0_ requestWithLong:u];
    }
    RxInternalOperatorsBackpressureUtils_getAndAddRequestWithJavaUtilConcurrentAtomicAtomicLong_withLong_(parent->requested_WindowOverlap_, n);
    [parent drain];
  }
}

- (instancetype)initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap:(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *)outer$ {
  RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_(self, outer$);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorWindowWithSize_WindowOverlap;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "serialVersionUID", "J", .constantValue.asLong = RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_serialVersionUID, 0x1a, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxInternalOperatorsOperatorWindowWithSize_WindowOverlap;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer = { "WindowOverlapProducer", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 2, 2, 2, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer;
}

@end

void RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer *self, RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  JavaUtilConcurrentAtomicAtomicBoolean_init(self);
}

RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer *new_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer, initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_, outer$)
}

RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer *create_RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer_initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer, initWithRxInternalOperatorsOperatorWindowWithSize_WindowOverlap_, outer$)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorWindowWithSize_WindowOverlap_WindowOverlapProducer)
