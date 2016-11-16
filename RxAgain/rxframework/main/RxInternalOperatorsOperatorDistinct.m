//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorDistinct.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxFunctionsFunc1.h"
#include "RxInternalOperatorsOperatorDistinct.h"
#include "RxInternalUtilUtilityFunctions.h"
#include "RxSubscriber.h"
#include "java/util/HashSet.h"
#include "java/util/Set.h"

@interface RxInternalOperatorsOperatorDistinct_$1 : RxSubscriber {
 @public
  RxInternalOperatorsOperatorDistinct *this$0_;
  id<JavaUtilSet> keyMemory_;
  RxSubscriber *val$child_;
}

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxInternalOperatorsOperatorDistinct:(RxInternalOperatorsOperatorDistinct *)outer$
                                           withRxSubscriber:(RxSubscriber *)capture$0
                                           withRxSubscriber:(RxSubscriber *)arg$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDistinct_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDistinct_$1, this$0_, RxInternalOperatorsOperatorDistinct *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDistinct_$1, keyMemory_, id<JavaUtilSet>)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDistinct_$1, val$child_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinct_$1 *self, RxInternalOperatorsOperatorDistinct *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0);

__attribute__((unused)) static RxInternalOperatorsOperatorDistinct_$1 *new_RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinct *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorDistinct_$1 *create_RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinct *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0);

@implementation RxInternalOperatorsOperatorDistinct

+ (RxInternalOperatorsOperatorDistinct *)instance {
  return RxInternalOperatorsOperatorDistinct_instance();
}

- (instancetype)initWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)keySelector {
  RxInternalOperatorsOperatorDistinct_initWithRxFunctionsFunc1_(self, keySelector);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  return create_RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(self, child, child);
}

- (void)dealloc {
  RELEASE_(keySelector_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalOperatorsOperatorDistinct;", 0x9, -1, -1, -1, 0, -1, -1 },
    { NULL, NULL, 0x1, -1, 1, -1, 2, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 3, 4, -1, 5, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(instance);
  methods[1].selector = @selector(initWithRxFunctionsFunc1:);
  methods[2].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "keySelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
  };
  static const void *ptrTable[] = { "<T:Ljava/lang/Object;>()Lrx/internal/operators/OperatorDistinct<TT;TT;>;", "LRxFunctionsFunc1;", "(Lrx/functions/Func1<-TT;+TU;>;)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "Lrx/functions/Func1<-TT;+TU;>;", "LRxInternalOperatorsOperatorDistinct_Holder;", "<T:Ljava/lang/Object;U:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDistinct = { "OperatorDistinct", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 3, 1, -1, 7, -1, 8, -1 };
  return &_RxInternalOperatorsOperatorDistinct;
}

@end

RxInternalOperatorsOperatorDistinct *RxInternalOperatorsOperatorDistinct_instance() {
  RxInternalOperatorsOperatorDistinct_initialize();
  return JreLoadStatic(RxInternalOperatorsOperatorDistinct_Holder, INSTANCE);
}

void RxInternalOperatorsOperatorDistinct_initWithRxFunctionsFunc1_(RxInternalOperatorsOperatorDistinct *self, id<RxFunctionsFunc1> keySelector) {
  NSObject_init(self);
  JreStrongAssign(&self->keySelector_, keySelector);
}

RxInternalOperatorsOperatorDistinct *new_RxInternalOperatorsOperatorDistinct_initWithRxFunctionsFunc1_(id<RxFunctionsFunc1> keySelector) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinct, initWithRxFunctionsFunc1_, keySelector)
}

RxInternalOperatorsOperatorDistinct *create_RxInternalOperatorsOperatorDistinct_initWithRxFunctionsFunc1_(id<RxFunctionsFunc1> keySelector) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinct, initWithRxFunctionsFunc1_, keySelector)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDistinct)

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsOperatorDistinct_Holder)

RxInternalOperatorsOperatorDistinct *RxInternalOperatorsOperatorDistinct_Holder_INSTANCE;

@implementation RxInternalOperatorsOperatorDistinct_Holder

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorDistinct_Holder_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "INSTANCE", "LRxInternalOperatorsOperatorDistinct;", .constantValue.asLong = 0, 0x18, -1, 0, 1, -1 },
  };
  static const void *ptrTable[] = { &RxInternalOperatorsOperatorDistinct_Holder_INSTANCE, "Lrx/internal/operators/OperatorDistinct<**>;", "LRxInternalOperatorsOperatorDistinct;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDistinct_Holder = { "Holder", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 1, 1, 2, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorDistinct_Holder;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsOperatorDistinct_Holder class]) {
    JreStrongAssignAndConsume(&RxInternalOperatorsOperatorDistinct_Holder_INSTANCE, new_RxInternalOperatorsOperatorDistinct_initWithRxFunctionsFunc1_(RxInternalUtilUtilityFunctions_identity()));
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsOperatorDistinct_Holder)
  }
}

@end

void RxInternalOperatorsOperatorDistinct_Holder_init(RxInternalOperatorsOperatorDistinct_Holder *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorDistinct_Holder *new_RxInternalOperatorsOperatorDistinct_Holder_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinct_Holder, init)
}

RxInternalOperatorsOperatorDistinct_Holder *create_RxInternalOperatorsOperatorDistinct_Holder_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinct_Holder, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDistinct_Holder)

@implementation RxInternalOperatorsOperatorDistinct_$1

- (void)onNextWithId:(id)t {
  id key = [((id<RxFunctionsFunc1>) nil_chk(this$0_->keySelector_)) callWithId:t];
  if ([((id<JavaUtilSet>) nil_chk(keyMemory_)) addWithId:key]) {
    [((RxSubscriber *) nil_chk(val$child_)) onNextWithId:t];
  }
  else {
    [self requestWithLong:1];
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  JreStrongAssign(&keyMemory_, nil);
  [((RxSubscriber *) nil_chk(val$child_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  JreStrongAssign(&keyMemory_, nil);
  [((RxSubscriber *) nil_chk(val$child_)) onCompleted];
}

- (instancetype)initWithRxInternalOperatorsOperatorDistinct:(RxInternalOperatorsOperatorDistinct *)outer$
                                           withRxSubscriber:(RxSubscriber *)capture$0
                                           withRxSubscriber:(RxSubscriber *)arg$0 {
  RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(self, outer$, capture$0, arg$0);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  RELEASE_(keyMemory_);
  RELEASE_(val$child_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x1, 3, 4, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 5, -1, 6, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onNextWithId:);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onCompleted);
  methods[3].selector = @selector(initWithRxInternalOperatorsOperatorDistinct:withRxSubscriber:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorDistinct;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "keyMemory_", "LJavaUtilSet;", .constantValue.asLong = 0, 0x0, -1, -1, 7, -1 },
    { "val$child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 8, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "LRxInternalOperatorsOperatorDistinct;LRxSubscriber;LRxSubscriber;", "(Lrx/internal/operators/OperatorDistinct;Lrx/Subscriber<-TT;>;Lrx/Subscriber<*>;)V", "Ljava/util/Set<TU;>;", "Lrx/Subscriber<-TT;>;", "LRxInternalOperatorsOperatorDistinct;", "callWithId:", "Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDistinct_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 3, 9, -1, 10, 11, -1 };
  return &_RxInternalOperatorsOperatorDistinct_$1;
}

@end

void RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinct_$1 *self, RxInternalOperatorsOperatorDistinct *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  JreStrongAssign(&self->this$0_, outer$);
  JreStrongAssign(&self->val$child_, capture$0);
  RxSubscriber_initWithRxSubscriber_(self, arg$0);
  JreStrongAssignAndConsume(&self->keyMemory_, new_JavaUtilHashSet_init());
}

RxInternalOperatorsOperatorDistinct_$1 *new_RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinct *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinct_$1, initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_, outer$, capture$0, arg$0)
}

RxInternalOperatorsOperatorDistinct_$1 *create_RxInternalOperatorsOperatorDistinct_$1_initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinct *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinct_$1, initWithRxInternalOperatorsOperatorDistinct_withRxSubscriber_withRxSubscriber_, outer$, capture$0, arg$0)
}
