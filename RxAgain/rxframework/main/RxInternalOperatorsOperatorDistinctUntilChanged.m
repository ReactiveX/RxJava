//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorDistinctUntilChanged.java
//

#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsFunc1.h"
#include "RxFunctionsFunc2.h"
#include "RxInternalOperatorsOperatorDistinctUntilChanged.h"
#include "RxInternalUtilUtilityFunctions.h"
#include "RxSubscriber.h"
#include "java/lang/Boolean.h"

@interface RxInternalOperatorsOperatorDistinctUntilChanged_$1 : RxSubscriber {
 @public
  RxInternalOperatorsOperatorDistinctUntilChanged *this$0_;
  id previousKey_;
  jboolean hasPrevious_;
  RxSubscriber *val$child_;
}

- (void)onNextWithId:(id)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxInternalOperatorsOperatorDistinctUntilChanged:(RxInternalOperatorsOperatorDistinctUntilChanged *)outer$
                                                       withRxSubscriber:(RxSubscriber *)capture$0
                                                       withRxSubscriber:(RxSubscriber *)arg$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDistinctUntilChanged_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDistinctUntilChanged_$1, this$0_, RxInternalOperatorsOperatorDistinctUntilChanged *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDistinctUntilChanged_$1, previousKey_, id)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDistinctUntilChanged_$1, val$child_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinctUntilChanged_$1 *self, RxInternalOperatorsOperatorDistinctUntilChanged *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0);

__attribute__((unused)) static RxInternalOperatorsOperatorDistinctUntilChanged_$1 *new_RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinctUntilChanged *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorDistinctUntilChanged_$1 *create_RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinctUntilChanged *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0);

@implementation RxInternalOperatorsOperatorDistinctUntilChanged

+ (RxInternalOperatorsOperatorDistinctUntilChanged *)instance {
  return RxInternalOperatorsOperatorDistinctUntilChanged_instance();
}

- (instancetype)initWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)keySelector {
  RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc1_(self, keySelector);
  return self;
}

- (instancetype)initWithRxFunctionsFunc2:(id<RxFunctionsFunc2>)comparator {
  RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc2_(self, comparator);
  return self;
}

- (JavaLangBoolean *)callWithId:(id)t1
                         withId:(id)t2 {
  return JavaLangBoolean_valueOfWithBoolean_((t1 == t2 || (t1 != nil && [t1 isEqual:t2])));
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  return create_RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(self, child, child);
}

- (void)dealloc {
  RELEASE_(keySelector_);
  RELEASE_(comparator_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalOperatorsOperatorDistinctUntilChanged;", 0x9, -1, -1, -1, 0, -1, -1 },
    { NULL, NULL, 0x1, -1, 1, -1, 2, -1, -1 },
    { NULL, NULL, 0x1, -1, 3, -1, 4, -1, -1 },
    { NULL, "LJavaLangBoolean;", 0x1, 5, 6, -1, 7, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 5, 8, -1, 9, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(instance);
  methods[1].selector = @selector(initWithRxFunctionsFunc1:);
  methods[2].selector = @selector(initWithRxFunctionsFunc2:);
  methods[3].selector = @selector(callWithId:withId:);
  methods[4].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "keySelector_", "LRxFunctionsFunc1;", .constantValue.asLong = 0, 0x10, -1, -1, 10, -1 },
    { "comparator_", "LRxFunctionsFunc2;", .constantValue.asLong = 0, 0x10, -1, -1, 11, -1 },
  };
  static const void *ptrTable[] = { "<T:Ljava/lang/Object;>()Lrx/internal/operators/OperatorDistinctUntilChanged<TT;TT;>;", "LRxFunctionsFunc1;", "(Lrx/functions/Func1<-TT;+TU;>;)V", "LRxFunctionsFunc2;", "(Lrx/functions/Func2<-TU;-TU;Ljava/lang/Boolean;>;)V", "call", "LNSObject;LNSObject;", "(TU;TU;)Ljava/lang/Boolean;", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)Lrx/Subscriber<-TT;>;", "Lrx/functions/Func1<-TT;+TU;>;", "Lrx/functions/Func2<-TU;-TU;Ljava/lang/Boolean;>;", "LRxInternalOperatorsOperatorDistinctUntilChanged_Holder;", "<T:Ljava/lang/Object;U:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<TT;TT;>;Lrx/functions/Func2<TU;TU;Ljava/lang/Boolean;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDistinctUntilChanged = { "OperatorDistinctUntilChanged", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 5, 2, -1, 12, -1, 13, -1 };
  return &_RxInternalOperatorsOperatorDistinctUntilChanged;
}

@end

RxInternalOperatorsOperatorDistinctUntilChanged *RxInternalOperatorsOperatorDistinctUntilChanged_instance() {
  RxInternalOperatorsOperatorDistinctUntilChanged_initialize();
  return JreLoadStatic(RxInternalOperatorsOperatorDistinctUntilChanged_Holder, INSTANCE);
}

void RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc1_(RxInternalOperatorsOperatorDistinctUntilChanged *self, id<RxFunctionsFunc1> keySelector) {
  NSObject_init(self);
  JreStrongAssign(&self->keySelector_, keySelector);
  JreStrongAssign(&self->comparator_, self);
}

RxInternalOperatorsOperatorDistinctUntilChanged *new_RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc1_(id<RxFunctionsFunc1> keySelector) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged, initWithRxFunctionsFunc1_, keySelector)
}

RxInternalOperatorsOperatorDistinctUntilChanged *create_RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc1_(id<RxFunctionsFunc1> keySelector) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged, initWithRxFunctionsFunc1_, keySelector)
}

void RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc2_(RxInternalOperatorsOperatorDistinctUntilChanged *self, id<RxFunctionsFunc2> comparator) {
  NSObject_init(self);
  JreStrongAssign(&self->keySelector_, RxInternalUtilUtilityFunctions_identity());
  JreStrongAssign(&self->comparator_, comparator);
}

RxInternalOperatorsOperatorDistinctUntilChanged *new_RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc2_(id<RxFunctionsFunc2> comparator) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged, initWithRxFunctionsFunc2_, comparator)
}

RxInternalOperatorsOperatorDistinctUntilChanged *create_RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc2_(id<RxFunctionsFunc2> comparator) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged, initWithRxFunctionsFunc2_, comparator)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDistinctUntilChanged)

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsOperatorDistinctUntilChanged_Holder)

RxInternalOperatorsOperatorDistinctUntilChanged *RxInternalOperatorsOperatorDistinctUntilChanged_Holder_INSTANCE;

@implementation RxInternalOperatorsOperatorDistinctUntilChanged_Holder

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorDistinctUntilChanged_Holder_init(self);
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
    { "INSTANCE", "LRxInternalOperatorsOperatorDistinctUntilChanged;", .constantValue.asLong = 0, 0x18, -1, 0, 1, -1 },
  };
  static const void *ptrTable[] = { &RxInternalOperatorsOperatorDistinctUntilChanged_Holder_INSTANCE, "Lrx/internal/operators/OperatorDistinctUntilChanged<**>;", "LRxInternalOperatorsOperatorDistinctUntilChanged;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDistinctUntilChanged_Holder = { "Holder", "rx.internal.operators", ptrTable, methods, fields, 7, 0x18, 1, 1, 2, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorDistinctUntilChanged_Holder;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsOperatorDistinctUntilChanged_Holder class]) {
    JreStrongAssignAndConsume(&RxInternalOperatorsOperatorDistinctUntilChanged_Holder_INSTANCE, new_RxInternalOperatorsOperatorDistinctUntilChanged_initWithRxFunctionsFunc1_(RxInternalUtilUtilityFunctions_identity()));
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsOperatorDistinctUntilChanged_Holder)
  }
}

@end

void RxInternalOperatorsOperatorDistinctUntilChanged_Holder_init(RxInternalOperatorsOperatorDistinctUntilChanged_Holder *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorDistinctUntilChanged_Holder *new_RxInternalOperatorsOperatorDistinctUntilChanged_Holder_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged_Holder, init)
}

RxInternalOperatorsOperatorDistinctUntilChanged_Holder *create_RxInternalOperatorsOperatorDistinctUntilChanged_Holder_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged_Holder, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDistinctUntilChanged_Holder)

@implementation RxInternalOperatorsOperatorDistinctUntilChanged_$1

- (void)onNextWithId:(id)t {
  id key;
  @try {
    key = [((id<RxFunctionsFunc1>) nil_chk(this$0_->keySelector_)) callWithId:t];
  }
  @catch (NSException *e) {
    RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_withId_(e, val$child_, t);
    return;
  }
  id currentKey = previousKey_;
  JreStrongAssign(&previousKey_, key);
  if (hasPrevious_) {
    jboolean comparison;
    @try {
      comparison = [((JavaLangBoolean *) nil_chk([((id<RxFunctionsFunc2>) nil_chk(this$0_->comparator_)) callWithId:currentKey withId:key])) booleanValue];
    }
    @catch (NSException *e) {
      RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_withId_(e, val$child_, key);
      return;
    }
    if (!comparison) {
      [((RxSubscriber *) nil_chk(val$child_)) onNextWithId:t];
    }
    else {
      [self requestWithLong:1];
    }
  }
  else {
    hasPrevious_ = true;
    [((RxSubscriber *) nil_chk(val$child_)) onNextWithId:t];
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubscriber *) nil_chk(val$child_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  [((RxSubscriber *) nil_chk(val$child_)) onCompleted];
}

- (instancetype)initWithRxInternalOperatorsOperatorDistinctUntilChanged:(RxInternalOperatorsOperatorDistinctUntilChanged *)outer$
                                                       withRxSubscriber:(RxSubscriber *)capture$0
                                                       withRxSubscriber:(RxSubscriber *)arg$0 {
  RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(self, outer$, capture$0, arg$0);
  return self;
}

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOperatorDistinctUntilChanged_$1 class]);
  RELEASE_(this$0_);
  RELEASE_(previousKey_);
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
  methods[3].selector = @selector(initWithRxInternalOperatorsOperatorDistinctUntilChanged:withRxSubscriber:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorDistinctUntilChanged;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "previousKey_", "LNSObject;", .constantValue.asLong = 0, 0x0, -1, -1, 7, -1 },
    { "hasPrevious_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "val$child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 8, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LNSObject;", "(TT;)V", "onError", "LNSException;", "LRxInternalOperatorsOperatorDistinctUntilChanged;LRxSubscriber;LRxSubscriber;", "(Lrx/internal/operators/OperatorDistinctUntilChanged;Lrx/Subscriber<-TT;>;Lrx/Subscriber<*>;)V", "TU;", "Lrx/Subscriber<-TT;>;", "LRxInternalOperatorsOperatorDistinctUntilChanged;", "callWithId:", "Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDistinctUntilChanged_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 4, 9, -1, 10, 11, -1 };
  return &_RxInternalOperatorsOperatorDistinctUntilChanged_$1;
}

@end

void RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinctUntilChanged_$1 *self, RxInternalOperatorsOperatorDistinctUntilChanged *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  JreStrongAssign(&self->this$0_, outer$);
  JreStrongAssign(&self->val$child_, capture$0);
  RxSubscriber_initWithRxSubscriber_(self, arg$0);
}

RxInternalOperatorsOperatorDistinctUntilChanged_$1 *new_RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinctUntilChanged *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged_$1, initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_, outer$, capture$0, arg$0)
}

RxInternalOperatorsOperatorDistinctUntilChanged_$1 *create_RxInternalOperatorsOperatorDistinctUntilChanged_$1_initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_(RxInternalOperatorsOperatorDistinctUntilChanged *outer$, RxSubscriber *capture$0, RxSubscriber *arg$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDistinctUntilChanged_$1, initWithRxInternalOperatorsOperatorDistinctUntilChanged_withRxSubscriber_withRxSubscriber_, outer$, capture$0, arg$0)
}
