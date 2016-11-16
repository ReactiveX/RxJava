//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OnSubscribeJoin.java
//

#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsFunc1.h"
#include "RxFunctionsFunc2.h"
#include "RxInternalOperatorsOnSubscribeJoin.h"
#include "RxObservable.h"
#include "RxObserversSerializedSubscriber.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "RxSubscriptionsCompositeSubscription.h"
#include "RxSubscriptionsSerialSubscription.h"
#include "java/lang/Integer.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "java/util/List.h"
#include "java/util/Map.h"
#include "java/util/Set.h"

@interface RxInternalOperatorsOnSubscribeJoin_ResultSink () {
 @public
  RxInternalOperatorsOnSubscribeJoin *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeJoin_ResultSink, this$0_, RxInternalOperatorsOnSubscribeJoin *)

inline jlong RxInternalOperatorsOnSubscribeJoin_ResultSink_get_serialVersionUID();
#define RxInternalOperatorsOnSubscribeJoin_ResultSink_serialVersionUID 3491669543549085380LL
J2OBJC_STATIC_FIELD_CONSTANT(RxInternalOperatorsOnSubscribeJoin_ResultSink, serialVersionUID, jlong)

@interface RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber () {
 @public
  RxInternalOperatorsOnSubscribeJoin_ResultSink *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber, this$0_, RxInternalOperatorsOnSubscribeJoin_ResultSink *)

@interface RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber () {
 @public
  RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber, this$0_, RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *)

@interface RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber () {
 @public
  RxInternalOperatorsOnSubscribeJoin_ResultSink *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber, this$0_, RxInternalOperatorsOnSubscribeJoin_ResultSink *)

@interface RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber () {
 @public
  RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *this$0_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber, this$0_, RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *)

@implementation RxInternalOperatorsOnSubscribeJoin

- (instancetype)initWithRxObservable:(RxObservable *)left
                    withRxObservable:(RxObservable *)right
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)leftDurationSelector
                withRxFunctionsFunc1:(id<RxFunctionsFunc1>)rightDurationSelector
                withRxFunctionsFunc2:(id<RxFunctionsFunc2>)resultSelector {
  RxInternalOperatorsOnSubscribeJoin_initWithRxObservable_withRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc2_(self, left, right, leftDurationSelector, rightDurationSelector, resultSelector);
  return self;
}

- (void)callWithId:(RxSubscriber *)t1 {
  RxInternalOperatorsOnSubscribeJoin_ResultSink *result = create_RxInternalOperatorsOnSubscribeJoin_ResultSink_initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_(self, create_RxObserversSerializedSubscriber_initWithRxSubscriber_(t1));
  [result run];
}

- (void)dealloc {
  RELEASE_(left_);
  RELEASE_(right_);
  RELEASE_(leftDurationSelector_);
  RELEASE_(rightDurationSelector_);
  RELEASE_(resultSelector_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxObservable:withRxObservable:withRxFunctionsFunc1:withRxFunctionsFunc1:withRxFunctionsFunc2:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "left_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 5, -1 },
    { "right_", "LRxObservable;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
    { "leftDurationSelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 7, -1 },
    { "rightDurationSelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 8, -1 },
    { "resultSelector_", "LRxFunctionsFunc2;", .constantValue.asLong = 0, 0x10, -1, -1, 9, -1 },
  };
  static const void *ptrTable[] = { "LRxObservable;LRxObservable;LRxFunctionsFunc1;LRxFunctionsFunc1;LRxFunctionsFunc2;", "(Lrx/Observable<TTLeft;>;Lrx/Observable<TTRight;>;Lrx/functions/Func1<TTLeft;Lrx/Observable<TTLeftDuration;>;>;Lrx/functions/Func1<TTRight;Lrx/Observable<TTRightDuration;>;>;Lrx/functions/Func2<TTLeft;TTRight;TR;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TR;>;)V", "Lrx/Observable<TTLeft;>;", "Lrx/Observable<TTRight;>;", "Lrx/functions/Func1<TTLeft;Lrx/Observable<TTLeftDuration;>;>;", "Lrx/functions/Func1<TTRight;Lrx/Observable<TTRightDuration;>;>;", "Lrx/functions/Func2<TTLeft;TTRight;TR;>;", "LRxInternalOperatorsOnSubscribeJoin_ResultSink;", "<TLeft:Ljava/lang/Object;TRight:Ljava/lang/Object;TLeftDuration:Ljava/lang/Object;TRightDuration:Ljava/lang/Object;R:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$OnSubscribe<TR;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeJoin = { "OnSubscribeJoin", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 2, 5, -1, 10, -1, 11, -1 };
  return &_RxInternalOperatorsOnSubscribeJoin;
}

@end

void RxInternalOperatorsOnSubscribeJoin_initWithRxObservable_withRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc2_(RxInternalOperatorsOnSubscribeJoin *self, RxObservable *left, RxObservable *right, id<RxFunctionsFunc1> leftDurationSelector, id<RxFunctionsFunc1> rightDurationSelector, id<RxFunctionsFunc2> resultSelector) {
  NSObject_init(self);
  JreStrongAssign(&self->left_, left);
  JreStrongAssign(&self->right_, right);
  JreStrongAssign(&self->leftDurationSelector_, leftDurationSelector);
  JreStrongAssign(&self->rightDurationSelector_, rightDurationSelector);
  JreStrongAssign(&self->resultSelector_, resultSelector);
}

RxInternalOperatorsOnSubscribeJoin *new_RxInternalOperatorsOnSubscribeJoin_initWithRxObservable_withRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc2_(RxObservable *left, RxObservable *right, id<RxFunctionsFunc1> leftDurationSelector, id<RxFunctionsFunc1> rightDurationSelector, id<RxFunctionsFunc2> resultSelector) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeJoin, initWithRxObservable_withRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc2_, left, right, leftDurationSelector, rightDurationSelector, resultSelector)
}

RxInternalOperatorsOnSubscribeJoin *create_RxInternalOperatorsOnSubscribeJoin_initWithRxObservable_withRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc2_(RxObservable *left, RxObservable *right, id<RxFunctionsFunc1> leftDurationSelector, id<RxFunctionsFunc1> rightDurationSelector, id<RxFunctionsFunc2> resultSelector) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeJoin, initWithRxObservable_withRxObservable_withRxFunctionsFunc1_withRxFunctionsFunc1_withRxFunctionsFunc2_, left, right, leftDurationSelector, rightDurationSelector, resultSelector)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeJoin)

@implementation RxInternalOperatorsOnSubscribeJoin_ResultSink

- (instancetype)initWithRxInternalOperatorsOnSubscribeJoin:(RxInternalOperatorsOnSubscribeJoin *)outer$
                                          withRxSubscriber:(RxSubscriber *)subscriber {
  RxInternalOperatorsOnSubscribeJoin_ResultSink_initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_(self, outer$, subscriber);
  return self;
}

- (JavaUtilHashMap *)leftMap {
  return self;
}

- (void)run {
  [((RxSubscriber *) nil_chk(subscriber_)) addWithRxSubscription:group_];
  RxSubscriber *s1 = create_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(self);
  RxSubscriber *s2 = create_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(self);
  [((RxSubscriptionsCompositeSubscription *) nil_chk(group_)) addWithRxSubscription:s1];
  [group_ addWithRxSubscription:s2];
  [((RxObservable *) nil_chk(this$0_->left_)) unsafeSubscribeWithRxSubscriber:s1];
  [((RxObservable *) nil_chk(this$0_->right_)) unsafeSubscribeWithRxSubscriber:s2];
}

- (void)dealloc {
  RELEASE_(this$0_);
  RELEASE_(group_);
  RELEASE_(subscriber_);
  RELEASE_(rightMap_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, 1, -1, -1 },
    { NULL, "LJavaUtilHashMap;", 0x0, -1, -1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxInternalOperatorsOnSubscribeJoin:withRxSubscriber:);
  methods[1].selector = @selector(leftMap);
  methods[2].selector = @selector(run);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOnSubscribeJoin;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "serialVersionUID", "J", .constantValue.asLong = RxInternalOperatorsOnSubscribeJoin_ResultSink_serialVersionUID, 0x1a, -1, -1, -1, -1 },
    { "group_", "LRxSubscriptionsCompositeSubscription;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "subscriber_", "LRxSubscriber;", .constantValue.asLong = 0, 0x10, -1, -1, 3, -1 },
    { "leftDone_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "leftId_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "rightDone_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "rightId_", "I", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "rightMap_", "LJavaUtilMap;", .constantValue.asLong = 0, 0x10, -1, -1, 4, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsOnSubscribeJoin;LRxSubscriber;", "(Lrx/internal/operators/OnSubscribeJoin;Lrx/Subscriber<-TR;>;)V", "()Ljava/util/HashMap<Ljava/lang/Integer;TTLeft;>;", "Lrx/Subscriber<-TR;>;", "Ljava/util/Map<Ljava/lang/Integer;TTRight;>;", "LRxInternalOperatorsOnSubscribeJoin;", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber;LRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber;", "Ljava/util/HashMap<Ljava/lang/Integer;TTLeft;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeJoin_ResultSink = { "ResultSink", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 3, 9, 5, 6, -1, 7, -1 };
  return &_RxInternalOperatorsOnSubscribeJoin_ResultSink;
}

@end

void RxInternalOperatorsOnSubscribeJoin_ResultSink_initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_(RxInternalOperatorsOnSubscribeJoin_ResultSink *self, RxInternalOperatorsOnSubscribeJoin *outer$, RxSubscriber *subscriber) {
  JreStrongAssign(&self->this$0_, outer$);
  JavaUtilHashMap_init(self);
  JreStrongAssign(&self->subscriber_, subscriber);
  JreStrongAssignAndConsume(&self->group_, new_RxSubscriptionsCompositeSubscription_init());
  JreStrongAssignAndConsume(&self->rightMap_, new_JavaUtilHashMap_init());
}

RxInternalOperatorsOnSubscribeJoin_ResultSink *new_RxInternalOperatorsOnSubscribeJoin_ResultSink_initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_(RxInternalOperatorsOnSubscribeJoin *outer$, RxSubscriber *subscriber) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink, initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_, outer$, subscriber)
}

RxInternalOperatorsOnSubscribeJoin_ResultSink *create_RxInternalOperatorsOnSubscribeJoin_ResultSink_initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_(RxInternalOperatorsOnSubscribeJoin *outer$, RxSubscriber *subscriber) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink, initWithRxInternalOperatorsOnSubscribeJoin_withRxSubscriber_, outer$, subscriber)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeJoin_ResultSink)

@implementation RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber

- (void)expireWithInt:(jint)id_
   withRxSubscription:(id<RxSubscription>)resource {
  jboolean complete = false;
  @synchronized(this$0_) {
    if ([((JavaUtilHashMap *) nil_chk([this$0_ leftMap])) removeWithId:JavaLangInteger_valueOfWithInt_(id_)] != nil && [((JavaUtilHashMap *) nil_chk([this$0_ leftMap])) isEmpty] && this$0_->leftDone_) {
      complete = true;
    }
  }
  if (complete) {
    [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onCompleted];
    [this$0_->subscriber_ unsubscribe];
  }
  else {
    [((RxSubscriptionsCompositeSubscription *) nil_chk(this$0_->group_)) removeWithRxSubscription:resource];
  }
}

- (void)onNextWithId:(id)args {
  jint id_;
  jint highRightId;
  @synchronized(this$0_) {
    id_ = this$0_->leftId_++;
    [((JavaUtilHashMap *) nil_chk([this$0_ leftMap])) putWithId:JavaLangInteger_valueOfWithInt_(id_) withId:args];
    highRightId = this$0_->rightId_;
  }
  RxObservable *duration;
  @try {
    duration = [((id<RxFunctionsFunc1>) nil_chk(this$0_->this$0_->leftDurationSelector_)) callWithId:args];
    RxSubscriber *d1 = create_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_(self, id_);
    [((RxSubscriptionsCompositeSubscription *) nil_chk(this$0_->group_)) addWithRxSubscription:d1];
    [((RxObservable *) nil_chk(duration)) unsafeSubscribeWithRxSubscriber:d1];
    id<JavaUtilList> rightValues = create_JavaUtilArrayList_init();
    @synchronized(this$0_) {
      for (id<JavaUtilMap_Entry> __strong entry_ in nil_chk([((id<JavaUtilMap>) nil_chk(this$0_->rightMap_)) entrySet])) {
        if ([((JavaLangInteger *) nil_chk([((id<JavaUtilMap_Entry>) nil_chk(entry_)) getKey])) intValue] < highRightId) {
          [rightValues addWithId:[entry_ getValue]];
        }
      }
    }
    for (id __strong r in rightValues) {
      id result = [((id<RxFunctionsFunc2>) nil_chk(this$0_->this$0_->resultSelector_)) callWithId:args withId:r];
      [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onNextWithId:result];
    }
  }
  @catch (NSException *t) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_(t, self);
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onErrorWithNSException:e];
  [this$0_->subscriber_ unsubscribe];
}

- (void)onCompleted {
  jboolean complete = false;
  @synchronized(this$0_) {
    this$0_->leftDone_ = true;
    if (this$0_->rightDone_ || [((JavaUtilHashMap *) nil_chk([this$0_ leftMap])) isEmpty]) {
      complete = true;
    }
  }
  if (complete) {
    [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onCompleted];
    [this$0_->subscriber_ unsubscribe];
  }
  else {
    [((RxSubscriptionsCompositeSubscription *) nil_chk(this$0_->group_)) removeWithRxSubscription:self];
  }
}

- (instancetype)initWithRxInternalOperatorsOnSubscribeJoin_ResultSink:(RxInternalOperatorsOnSubscribeJoin_ResultSink *)outer$ {
  RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(self, outer$);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x4, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 7, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(expireWithInt:withRxSubscription:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(initWithRxInternalOperatorsOnSubscribeJoin_ResultSink:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOnSubscribeJoin_ResultSink;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "expire", "ILRxSubscription;", "onNext", "LNSObject;", "(TTLeft;)V", "onError", "LNSException;", "LRxInternalOperatorsOnSubscribeJoin_ResultSink;", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber;", "Lrx/Subscriber<TTLeft;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber = { "LeftSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 5, 1, 7, 8, -1, 9, -1 };
  return &_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *self, RxInternalOperatorsOnSubscribeJoin_ResultSink *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  RxSubscriber_init(self);
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *new_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(RxInternalOperatorsOnSubscribeJoin_ResultSink *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_, outer$)
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *create_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(RxInternalOperatorsOnSubscribeJoin_ResultSink *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_, outer$)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber)

@implementation RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber

- (instancetype)initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber:(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *)outer$
                                                                             withInt:(jint)id_ {
  RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_(self, outer$, id_);
  return self;
}

- (void)onNextWithId:(id)args {
  [self onCompleted];
}

- (void)onErrorWithNSException:(NSException *)e {
  [this$0_ onErrorWithNSException:e];
}

- (void)onCompleted {
  if (once_) {
    once_ = false;
    [this$0_ expireWithInt:id__ withRxSubscription:self];
  }
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, 3, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber:withInt:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "id__", "I", .constantValue.asLong = 0, 0x10, 6, -1, -1, -1 },
    { "once_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber;I", "onNext", "LNSObject;", "(TTLeftDuration;)V", "onError", "LNSException;", "id", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber;", "Lrx/Subscriber<TTLeftDuration;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber = { "LeftDurationSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 4, 3, 7, -1, -1, 8, -1 };
  return &_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber *self, RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *outer$, jint id_) {
  JreStrongAssign(&self->this$0_, outer$);
  RxSubscriber_init(self);
  self->once_ = true;
  self->id__ = id_;
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber *new_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *outer$, jint id_) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_, outer$, id_)
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber *create_RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber *outer$, jint id_) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_withInt_, outer$, id_)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeJoin_ResultSink_LeftSubscriber_LeftDurationSubscriber)

@implementation RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber

- (void)expireWithInt:(jint)id_
   withRxSubscription:(id<RxSubscription>)resource {
  jboolean complete = false;
  @synchronized(this$0_) {
    if ([((id<JavaUtilMap>) nil_chk(this$0_->rightMap_)) removeWithId:JavaLangInteger_valueOfWithInt_(id_)] != nil && [this$0_->rightMap_ isEmpty] && this$0_->rightDone_) {
      complete = true;
    }
  }
  if (complete) {
    [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onCompleted];
    [this$0_->subscriber_ unsubscribe];
  }
  else {
    [((RxSubscriptionsCompositeSubscription *) nil_chk(this$0_->group_)) removeWithRxSubscription:resource];
  }
}

- (void)onNextWithId:(id)args {
  jint id_;
  jint highLeftId;
  @synchronized(this$0_) {
    id_ = this$0_->rightId_++;
    [((id<JavaUtilMap>) nil_chk(this$0_->rightMap_)) putWithId:JavaLangInteger_valueOfWithInt_(id_) withId:args];
    highLeftId = this$0_->leftId_;
  }
  RxSubscriptionsSerialSubscription *md = create_RxSubscriptionsSerialSubscription_init();
  [((RxSubscriptionsCompositeSubscription *) nil_chk(this$0_->group_)) addWithRxSubscription:md];
  RxObservable *duration;
  @try {
    duration = [((id<RxFunctionsFunc1>) nil_chk(this$0_->this$0_->rightDurationSelector_)) callWithId:args];
    RxSubscriber *d2 = create_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_(self, id_);
    [this$0_->group_ addWithRxSubscription:d2];
    [((RxObservable *) nil_chk(duration)) unsafeSubscribeWithRxSubscriber:d2];
    id<JavaUtilList> leftValues = create_JavaUtilArrayList_init();
    @synchronized(this$0_) {
      for (id<JavaUtilMap_Entry> __strong entry_ in nil_chk([((JavaUtilHashMap *) nil_chk([this$0_ leftMap])) entrySet])) {
        if ([((JavaLangInteger *) nil_chk([((id<JavaUtilMap_Entry>) nil_chk(entry_)) getKey])) intValue] < highLeftId) {
          [leftValues addWithId:[entry_ getValue]];
        }
      }
    }
    for (id __strong lv in leftValues) {
      id result = [((id<RxFunctionsFunc2>) nil_chk(this$0_->this$0_->resultSelector_)) callWithId:lv withId:args];
      [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onNextWithId:result];
    }
  }
  @catch (NSException *t) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_(t, self);
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onErrorWithNSException:e];
  [this$0_->subscriber_ unsubscribe];
}

- (void)onCompleted {
  jboolean complete = false;
  @synchronized(this$0_) {
    this$0_->rightDone_ = true;
    if (this$0_->leftDone_ || [((id<JavaUtilMap>) nil_chk(this$0_->rightMap_)) isEmpty]) {
      complete = true;
    }
  }
  if (complete) {
    [((RxSubscriber *) nil_chk(this$0_->subscriber_)) onCompleted];
    [this$0_->subscriber_ unsubscribe];
  }
  else {
    [((RxSubscriptionsCompositeSubscription *) nil_chk(this$0_->group_)) removeWithRxSubscription:self];
  }
}

- (instancetype)initWithRxInternalOperatorsOnSubscribeJoin_ResultSink:(RxInternalOperatorsOnSubscribeJoin_ResultSink *)outer$ {
  RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(self, outer$);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x0, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, "V", 0x1, 5, 6, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 7, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(expireWithInt:withRxSubscription:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  methods[4].selector = @selector(initWithRxInternalOperatorsOnSubscribeJoin_ResultSink:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOnSubscribeJoin_ResultSink;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "expire", "ILRxSubscription;", "onNext", "LNSObject;", "(TTRight;)V", "onError", "LNSException;", "LRxInternalOperatorsOnSubscribeJoin_ResultSink;", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber;", "Lrx/Subscriber<TTRight;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber = { "RightSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 5, 1, 7, 8, -1, 9, -1 };
  return &_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *self, RxInternalOperatorsOnSubscribeJoin_ResultSink *outer$) {
  JreStrongAssign(&self->this$0_, outer$);
  RxSubscriber_init(self);
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *new_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(RxInternalOperatorsOnSubscribeJoin_ResultSink *outer$) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_, outer$)
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *create_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_(RxInternalOperatorsOnSubscribeJoin_ResultSink *outer$) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_, outer$)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber)

@implementation RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber

- (instancetype)initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber:(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *)outer$
                                                                              withInt:(jint)id_ {
  RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_(self, outer$, id_);
  return self;
}

- (void)onNextWithId:(id)args {
  [self onCompleted];
}

- (void)onErrorWithNSException:(NSException *)e {
  [this$0_ onErrorWithNSException:e];
}

- (void)onCompleted {
  if (once_) {
    once_ = false;
    [this$0_ expireWithInt:id__ withRxSubscription:self];
  }
}

- (void)dealloc {
  RELEASE_(this$0_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, 3, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber:withInt:);
  methods[1].selector = @selector(onNextWithId:);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onCompleted);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "id__", "I", .constantValue.asLong = 0, 0x10, 6, -1, -1, -1 },
    { "once_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber;I", "onNext", "LNSObject;", "(TTRightDuration;)V", "onError", "LNSException;", "id", "LRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber;", "Lrx/Subscriber<TTRightDuration;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber = { "RightDurationSubscriber", "rx.internal.operators", ptrTable, methods, fields, 7, 0x10, 4, 3, 7, -1, -1, 8, -1 };
  return &_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber;
}

@end

void RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber *self, RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *outer$, jint id_) {
  JreStrongAssign(&self->this$0_, outer$);
  RxSubscriber_init(self);
  self->once_ = true;
  self->id__ = id_;
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber *new_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *outer$, jint id_) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_, outer$, id_)
}

RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber *create_RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber_initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber *outer$, jint id_) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber, initWithRxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_withInt_, outer$, id_)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOnSubscribeJoin_ResultSink_RightSubscriber_RightDurationSubscriber)
