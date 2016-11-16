//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/OperatorToObservableSortedList.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsFunc2.h"
#include "RxInternalOperatorsOperatorToObservableSortedList.h"
#include "RxInternalProducersSingleDelayedProducer.h"
#include "RxSubscriber.h"
#include "java/lang/Comparable.h"
#include "java/lang/Integer.h"
#include "java/lang/Long.h"
#include "java/util/ArrayList.h"
#include "java/util/Collections.h"
#include "java/util/Comparator.h"
#include "java/util/List.h"
#include "java/util/function/Function.h"
#include "java/util/function/ToDoubleFunction.h"
#include "java/util/function/ToIntFunction.h"
#include "java/util/function/ToLongFunction.h"

#pragma clang diagnostic ignored "-Wprotocol"

inline id<JavaUtilComparator> RxInternalOperatorsOperatorToObservableSortedList_get_DEFAULT_SORT_FUNCTION();
static id<JavaUtilComparator> RxInternalOperatorsOperatorToObservableSortedList_DEFAULT_SORT_FUNCTION;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalOperatorsOperatorToObservableSortedList, DEFAULT_SORT_FUNCTION, id<JavaUtilComparator>)

@interface RxInternalOperatorsOperatorToObservableSortedList_$1 : NSObject < JavaUtilComparator > {
 @public
  id<RxFunctionsFunc2> val$sortFunction_;
}

- (jint)compareWithId:(id)o1
               withId:(id)o2;

- (instancetype)initWithRxFunctionsFunc2:(id<RxFunctionsFunc2>)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorToObservableSortedList_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorToObservableSortedList_$1, val$sortFunction_, id<RxFunctionsFunc2>)

__attribute__((unused)) static void RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(RxInternalOperatorsOperatorToObservableSortedList_$1 *self, id<RxFunctionsFunc2> capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorToObservableSortedList_$1 *new_RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(id<RxFunctionsFunc2> capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorToObservableSortedList_$1 *create_RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(id<RxFunctionsFunc2> capture$0);

@interface RxInternalOperatorsOperatorToObservableSortedList_$2 : RxSubscriber {
 @public
  RxInternalOperatorsOperatorToObservableSortedList *this$0_;
  id<JavaUtilList> list_;
  jboolean completed_;
  RxInternalProducersSingleDelayedProducer *val$producer_;
  RxSubscriber *val$child_;
}

- (void)onStart;

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(id)value;

- (instancetype)initWithRxInternalOperatorsOperatorToObservableSortedList:(RxInternalOperatorsOperatorToObservableSortedList *)outer$
                             withRxInternalProducersSingleDelayedProducer:(RxInternalProducersSingleDelayedProducer *)capture$0
                                                         withRxSubscriber:(RxSubscriber *)capture$1;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorToObservableSortedList_$2)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorToObservableSortedList_$2, this$0_, RxInternalOperatorsOperatorToObservableSortedList *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorToObservableSortedList_$2, list_, id<JavaUtilList>)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorToObservableSortedList_$2, val$producer_, RxInternalProducersSingleDelayedProducer *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorToObservableSortedList_$2, val$child_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(RxInternalOperatorsOperatorToObservableSortedList_$2 *self, RxInternalOperatorsOperatorToObservableSortedList *outer$, RxInternalProducersSingleDelayedProducer *capture$0, RxSubscriber *capture$1);

__attribute__((unused)) static RxInternalOperatorsOperatorToObservableSortedList_$2 *new_RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(RxInternalOperatorsOperatorToObservableSortedList *outer$, RxInternalProducersSingleDelayedProducer *capture$0, RxSubscriber *capture$1) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorToObservableSortedList_$2 *create_RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(RxInternalOperatorsOperatorToObservableSortedList *outer$, RxInternalProducersSingleDelayedProducer *capture$0, RxSubscriber *capture$1);

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsOperatorToObservableSortedList)

@implementation RxInternalOperatorsOperatorToObservableSortedList

- (instancetype)initWithInt:(jint)initialCapacity {
  RxInternalOperatorsOperatorToObservableSortedList_initWithInt_(self, initialCapacity);
  return self;
}

- (instancetype)initWithRxFunctionsFunc2:(id<RxFunctionsFunc2>)sortFunction
                                 withInt:(jint)initialCapacity {
  RxInternalOperatorsOperatorToObservableSortedList_initWithRxFunctionsFunc2_withInt_(self, sortFunction, initialCapacity);
  return self;
}

- (RxSubscriber *)callWithId:(RxSubscriber *)child {
  RxInternalProducersSingleDelayedProducer *producer = create_RxInternalProducersSingleDelayedProducer_initWithRxSubscriber_(child);
  RxSubscriber *result = create_RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(self, producer, child);
  [((RxSubscriber *) nil_chk(child)) addWithRxSubscription:result];
  [child setProducerWithRxProducer:producer];
  return result;
}

- (void)dealloc {
  RELEASE_(sortFunction_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 1, -1, 2, -1, -1 },
    { NULL, "LRxSubscriber;", 0x1, 3, 4, -1, 5, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithInt:);
  methods[1].selector = @selector(initWithRxFunctionsFunc2:withInt:);
  methods[2].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "sortFunction_", "LJavaUtilComparator;", .constantValue.asLong = 0, 0x10, -1, -1, 6, -1 },
    { "initialCapacity_", "I", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "DEFAULT_SORT_FUNCTION", "LJavaUtilComparator;", .constantValue.asLong = 0, 0x1a, -1, 7, -1, -1 },
  };
  static const void *ptrTable[] = { "I", "LRxFunctionsFunc2;I", "(Lrx/functions/Func2<-TT;-TT;Ljava/lang/Integer;>;I)V", "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/util/List<TT;>;>;)Lrx/Subscriber<-TT;>;", "Ljava/util/Comparator<-TT;>;", &RxInternalOperatorsOperatorToObservableSortedList_DEFAULT_SORT_FUNCTION, "LRxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$Operator<Ljava/util/List<TT;>;TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorToObservableSortedList = { "OperatorToObservableSortedList", "rx.internal.operators", ptrTable, methods, fields, 7, 0x11, 3, 3, -1, 8, -1, 9, -1 };
  return &_RxInternalOperatorsOperatorToObservableSortedList;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsOperatorToObservableSortedList class]) {
    JreStrongAssignAndConsume(&RxInternalOperatorsOperatorToObservableSortedList_DEFAULT_SORT_FUNCTION, new_RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction_init());
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsOperatorToObservableSortedList)
  }
}

@end

void RxInternalOperatorsOperatorToObservableSortedList_initWithInt_(RxInternalOperatorsOperatorToObservableSortedList *self, jint initialCapacity) {
  NSObject_init(self);
  JreStrongAssign(&self->sortFunction_, RxInternalOperatorsOperatorToObservableSortedList_DEFAULT_SORT_FUNCTION);
  self->initialCapacity_ = initialCapacity;
}

RxInternalOperatorsOperatorToObservableSortedList *new_RxInternalOperatorsOperatorToObservableSortedList_initWithInt_(jint initialCapacity) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorToObservableSortedList, initWithInt_, initialCapacity)
}

RxInternalOperatorsOperatorToObservableSortedList *create_RxInternalOperatorsOperatorToObservableSortedList_initWithInt_(jint initialCapacity) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorToObservableSortedList, initWithInt_, initialCapacity)
}

void RxInternalOperatorsOperatorToObservableSortedList_initWithRxFunctionsFunc2_withInt_(RxInternalOperatorsOperatorToObservableSortedList *self, id<RxFunctionsFunc2> sortFunction, jint initialCapacity) {
  NSObject_init(self);
  self->initialCapacity_ = initialCapacity;
  JreStrongAssignAndConsume(&self->sortFunction_, new_RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(sortFunction));
}

RxInternalOperatorsOperatorToObservableSortedList *new_RxInternalOperatorsOperatorToObservableSortedList_initWithRxFunctionsFunc2_withInt_(id<RxFunctionsFunc2> sortFunction, jint initialCapacity) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorToObservableSortedList, initWithRxFunctionsFunc2_withInt_, sortFunction, initialCapacity)
}

RxInternalOperatorsOperatorToObservableSortedList *create_RxInternalOperatorsOperatorToObservableSortedList_initWithRxFunctionsFunc2_withInt_(id<RxFunctionsFunc2> sortFunction, jint initialCapacity) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorToObservableSortedList, initWithRxFunctionsFunc2_withInt_, sortFunction, initialCapacity)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorToObservableSortedList)

@implementation RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction

- (jint)compareWithId:(id)t1
               withId:(id)t2 {
  id<JavaLangComparable> c1 = (id<JavaLangComparable>) cast_check(t1, JavaLangComparable_class_());
  id<JavaLangComparable> c2 = (id<JavaLangComparable>) cast_check(t2, JavaLangComparable_class_());
  return [((id<JavaLangComparable>) nil_chk(c1)) compareToWithId:c2];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (id<JavaUtilComparator>)reversed {
  return JavaUtilComparator_reversed(self);
}

- (id<JavaUtilComparator>)thenComparingWithJavaUtilComparator:(id<JavaUtilComparator>)arg0 {
  return JavaUtilComparator_thenComparingWithJavaUtilComparator_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingWithJavaUtilFunctionFunction:(id<JavaUtilFunctionFunction>)arg0 {
  return JavaUtilComparator_thenComparingWithJavaUtilFunctionFunction_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingWithJavaUtilFunctionFunction:(id<JavaUtilFunctionFunction>)arg0
                                             withJavaUtilComparator:(id<JavaUtilComparator>)arg1 {
  return JavaUtilComparator_thenComparingWithJavaUtilFunctionFunction_withJavaUtilComparator_(self, arg0, arg1);
}

- (id<JavaUtilComparator>)thenComparingDoubleWithJavaUtilFunctionToDoubleFunction:(id<JavaUtilFunctionToDoubleFunction>)arg0 {
  return JavaUtilComparator_thenComparingDoubleWithJavaUtilFunctionToDoubleFunction_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingIntWithJavaUtilFunctionToIntFunction:(id<JavaUtilFunctionToIntFunction>)arg0 {
  return JavaUtilComparator_thenComparingIntWithJavaUtilFunctionToIntFunction_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingLongWithJavaUtilFunctionToLongFunction:(id<JavaUtilFunctionToLongFunction>)arg0 {
  return JavaUtilComparator_thenComparingLongWithJavaUtilFunctionToLongFunction_(self, arg0);
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "I", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(compareWithId:withId:);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "compare", "LNSObject;LNSObject;", "LRxInternalOperatorsOperatorToObservableSortedList;", "Ljava/lang/Object;Ljava/util/Comparator<Ljava/lang/Object;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction = { "DefaultComparableFunction", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x18, 2, 0, 2, -1, -1, 3, -1 };
  return &_RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction;
}

@end

void RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction_init(RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction *new_RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction, init)
}

RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction *create_RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorToObservableSortedList_DefaultComparableFunction)

@implementation RxInternalOperatorsOperatorToObservableSortedList_$1

- (jint)compareWithId:(id)o1
               withId:(id)o2 {
  return [((JavaLangInteger *) nil_chk([((id<RxFunctionsFunc2>) nil_chk(val$sortFunction_)) callWithId:o1 withId:o2])) intValue];
}

- (instancetype)initWithRxFunctionsFunc2:(id<RxFunctionsFunc2>)capture$0 {
  RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(self, capture$0);
  return self;
}

- (id<JavaUtilComparator>)reversed {
  return JavaUtilComparator_reversed(self);
}

- (id<JavaUtilComparator>)thenComparingWithJavaUtilComparator:(id<JavaUtilComparator>)arg0 {
  return JavaUtilComparator_thenComparingWithJavaUtilComparator_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingWithJavaUtilFunctionFunction:(id<JavaUtilFunctionFunction>)arg0 {
  return JavaUtilComparator_thenComparingWithJavaUtilFunctionFunction_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingWithJavaUtilFunctionFunction:(id<JavaUtilFunctionFunction>)arg0
                                             withJavaUtilComparator:(id<JavaUtilComparator>)arg1 {
  return JavaUtilComparator_thenComparingWithJavaUtilFunctionFunction_withJavaUtilComparator_(self, arg0, arg1);
}

- (id<JavaUtilComparator>)thenComparingDoubleWithJavaUtilFunctionToDoubleFunction:(id<JavaUtilFunctionToDoubleFunction>)arg0 {
  return JavaUtilComparator_thenComparingDoubleWithJavaUtilFunctionToDoubleFunction_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingIntWithJavaUtilFunctionToIntFunction:(id<JavaUtilFunctionToIntFunction>)arg0 {
  return JavaUtilComparator_thenComparingIntWithJavaUtilFunctionToIntFunction_(self, arg0);
}

- (id<JavaUtilComparator>)thenComparingLongWithJavaUtilFunctionToLongFunction:(id<JavaUtilFunctionToLongFunction>)arg0 {
  return JavaUtilComparator_thenComparingLongWithJavaUtilFunctionToLongFunction_(self, arg0);
}

- (void)dealloc {
  RELEASE_(val$sortFunction_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "I", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, NULL, 0x0, -1, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(compareWithId:withId:);
  methods[1].selector = @selector(initWithRxFunctionsFunc2:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$sortFunction_", "LRxFunctionsFunc2;", .constantValue.asLong = 0, 0x1012, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "compare", "LNSObject;LNSObject;", "(TT;TT;)I", "LRxFunctionsFunc2;", "(Lrx/functions/Func2<-TT;-TT;Ljava/lang/Integer;>;)V", "Lrx/functions/Func2<-TT;-TT;Ljava/lang/Integer;>;", "LRxInternalOperatorsOperatorToObservableSortedList;", "initWithRxFunctionsFunc2:withInt:", "Ljava/lang/Object;Ljava/util/Comparator<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorToObservableSortedList_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 6, -1, 7, 8, -1 };
  return &_RxInternalOperatorsOperatorToObservableSortedList_$1;
}

@end

void RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(RxInternalOperatorsOperatorToObservableSortedList_$1 *self, id<RxFunctionsFunc2> capture$0) {
  JreStrongAssign(&self->val$sortFunction_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOperatorToObservableSortedList_$1 *new_RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(id<RxFunctionsFunc2> capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorToObservableSortedList_$1, initWithRxFunctionsFunc2_, capture$0)
}

RxInternalOperatorsOperatorToObservableSortedList_$1 *create_RxInternalOperatorsOperatorToObservableSortedList_$1_initWithRxFunctionsFunc2_(id<RxFunctionsFunc2> capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorToObservableSortedList_$1, initWithRxFunctionsFunc2_, capture$0)
}

@implementation RxInternalOperatorsOperatorToObservableSortedList_$2

- (void)onStart {
  [self requestWithLong:JavaLangLong_MAX_VALUE];
}

- (void)onCompleted {
  if (!completed_) {
    completed_ = true;
    id<JavaUtilList> a = list_;
    JreStrongAssign(&list_, nil);
    @try {
      JavaUtilCollections_sortWithJavaUtilList_withJavaUtilComparator_(a, this$0_->sortFunction_);
    }
    @catch (NSException *e) {
      RxExceptionsExceptions_throwOrReportWithNSException_withRxObserver_(e, self);
      return;
    }
    [((RxInternalProducersSingleDelayedProducer *) nil_chk(val$producer_)) setValueWithId:a];
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  [((RxSubscriber *) nil_chk(val$child_)) onErrorWithNSException:e];
}

- (void)onNextWithId:(id)value {
  if (!completed_) {
    [((id<JavaUtilList>) nil_chk(list_)) addWithId:value];
  }
}

- (instancetype)initWithRxInternalOperatorsOperatorToObservableSortedList:(RxInternalOperatorsOperatorToObservableSortedList *)outer$
                             withRxInternalProducersSingleDelayedProducer:(RxInternalProducersSingleDelayedProducer *)capture$0
                                                         withRxSubscriber:(RxSubscriber *)capture$1 {
  RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(self, outer$, capture$0, capture$1);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  RELEASE_(list_);
  RELEASE_(val$producer_);
  RELEASE_(val$child_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, NULL, 0x0, -1, 5, -1, 6, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onStart);
  methods[1].selector = @selector(onCompleted);
  methods[2].selector = @selector(onErrorWithNSException:);
  methods[3].selector = @selector(onNextWithId:);
  methods[4].selector = @selector(initWithRxInternalOperatorsOperatorToObservableSortedList:withRxInternalProducersSingleDelayedProducer:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorToObservableSortedList;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "list_", "LJavaUtilList;", .constantValue.asLong = 0, 0x0, -1, -1, 7, -1 },
    { "completed_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "val$producer_", "LRxInternalProducersSingleDelayedProducer;", .constantValue.asLong = 0, 0x1012, -1, -1, 8, -1 },
    { "val$child_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 9, -1 },
  };
  static const void *ptrTable[] = { "onError", "LNSException;", "onNext", "LNSObject;", "(TT;)V", "LRxInternalOperatorsOperatorToObservableSortedList;LRxInternalProducersSingleDelayedProducer;LRxSubscriber;", "(Lrx/internal/operators/OperatorToObservableSortedList;Lrx/internal/producers/SingleDelayedProducer<Ljava/util/List<TT;>;>;Lrx/Subscriber<-Ljava/util/List<TT;>;>;)V", "Ljava/util/List<TT;>;", "Lrx/internal/producers/SingleDelayedProducer<Ljava/util/List<TT;>;>;", "Lrx/Subscriber<-Ljava/util/List<TT;>;>;", "LRxInternalOperatorsOperatorToObservableSortedList;", "callWithId:", "Lrx/Subscriber<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorToObservableSortedList_$2 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 5, 5, 10, -1, 11, 12, -1 };
  return &_RxInternalOperatorsOperatorToObservableSortedList_$2;
}

@end

void RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(RxInternalOperatorsOperatorToObservableSortedList_$2 *self, RxInternalOperatorsOperatorToObservableSortedList *outer$, RxInternalProducersSingleDelayedProducer *capture$0, RxSubscriber *capture$1) {
  JreStrongAssign(&self->this$0_, outer$);
  JreStrongAssign(&self->val$producer_, capture$0);
  JreStrongAssign(&self->val$child_, capture$1);
  RxSubscriber_init(self);
  JreStrongAssignAndConsume(&self->list_, new_JavaUtilArrayList_initWithInt_(outer$->initialCapacity_));
}

RxInternalOperatorsOperatorToObservableSortedList_$2 *new_RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(RxInternalOperatorsOperatorToObservableSortedList *outer$, RxInternalProducersSingleDelayedProducer *capture$0, RxSubscriber *capture$1) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorToObservableSortedList_$2, initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_, outer$, capture$0, capture$1)
}

RxInternalOperatorsOperatorToObservableSortedList_$2 *create_RxInternalOperatorsOperatorToObservableSortedList_$2_initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_(RxInternalOperatorsOperatorToObservableSortedList *outer$, RxInternalProducersSingleDelayedProducer *capture$0, RxSubscriber *capture$1) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorToObservableSortedList_$2, initWithRxInternalOperatorsOperatorToObservableSortedList_withRxInternalProducersSingleDelayedProducer_withRxSubscriber_, outer$, capture$0, capture$1)
}
