//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/BlockingOperatorToFutureTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsTestException.h"
#include "RxInternalOperatorsBlockingOperatorToFuture.h"
#include "RxInternalOperatorsBlockingOperatorToFutureTest.h"
#include "RxObservable.h"
#include "RxObservablesBlockingObservable.h"
#include "RxSubscriber.h"
#include "RxTestUtil.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Long.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/List.h"
#include "java/util/NoSuchElementException.h"
#include "java/util/concurrent/CancellationException.h"
#include "java/util/concurrent/ExecutionException.h"
#include "java/util/concurrent/Future.h"
#include "java/util/concurrent/TimeUnit.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$3();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$4();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$5();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$6();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$7();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$8();

@interface RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete : NSObject < RxObservable_OnSubscribe >

- (void)callWithId:(RxSubscriber *)unused;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete)

__attribute__((unused)) static void RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete *self);

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete *new_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete *create_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete)

@interface RxInternalOperatorsBlockingOperatorToFutureTest_$1 : NSObject < RxObservable_OnSubscribe >

- (void)callWithId:(RxSubscriber *)observer;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsBlockingOperatorToFutureTest_$1)

__attribute__((unused)) static void RxInternalOperatorsBlockingOperatorToFutureTest_$1_init(RxInternalOperatorsBlockingOperatorToFutureTest_$1 *self);

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToFutureTest_$1 *new_RxInternalOperatorsBlockingOperatorToFutureTest_$1_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToFutureTest_$1 *create_RxInternalOperatorsBlockingOperatorToFutureTest_$1_init();

@implementation RxInternalOperatorsBlockingOperatorToFutureTest

- (void)constructorShouldBePrivate {
  RxTestUtil_checkUtilityClassWithIOSClass_(RxInternalOperatorsBlockingOperatorToFuture_class_());
}

- (void)testToFuture {
  RxObservable *obs = RxObservable_justWithId_(@"one");
  id<JavaUtilConcurrentFuture> f = RxInternalOperatorsBlockingOperatorToFuture_toFutureWithRxObservable_(obs);
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [((id<JavaUtilConcurrentFuture>) nil_chk(f)) get]);
}

- (void)testToFutureList {
  RxObservable *obs = RxObservable_justWithId_withId_withId_(@"one", @"two", @"three");
  id<JavaUtilConcurrentFuture> f = RxInternalOperatorsBlockingOperatorToFuture_toFutureWithRxObservable_([((RxObservable *) nil_chk(obs)) toList]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [((id<JavaUtilList>) nil_chk([((id<JavaUtilConcurrentFuture>) nil_chk(f)) get])) getWithInt:0]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [((id<JavaUtilList>) nil_chk([f get])) getWithInt:1]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"three", [((id<JavaUtilList>) nil_chk([f get])) getWithInt:2]);
}

- (void)testExceptionWithMoreThanOneElement {
  RxObservable *obs = RxObservable_justWithId_withId_(@"one", @"two");
  id<JavaUtilConcurrentFuture> f = RxInternalOperatorsBlockingOperatorToFuture_toFutureWithRxObservable_(obs);
  @try {
    [((id<JavaUtilConcurrentFuture>) nil_chk(f)) get];
  }
  @catch (JavaUtilConcurrentExecutionException *e) {
    @throw [((JavaUtilConcurrentExecutionException *) nil_chk(e)) getCause];
  }
}

- (void)testToFutureWithException {
  RxObservable *obs = RxObservable_createWithRxObservable_OnSubscribe_(create_RxInternalOperatorsBlockingOperatorToFutureTest_$1_init());
  id<JavaUtilConcurrentFuture> f = RxInternalOperatorsBlockingOperatorToFuture_toFutureWithRxObservable_(obs);
  @try {
    [((id<JavaUtilConcurrentFuture>) nil_chk(f)) get];
    OrgJunitAssert_failWithNSString_(@"expected exception");
  }
  @catch (NSException *e) {
    OrgJunitAssert_assertEqualsWithId_withId_(RxExceptionsTestException_class_(), [((NSException *) nil_chk([((NSException *) nil_chk(e)) getCause])) java_getClass]);
  }
}

- (void)testGetAfterCancel {
  RxObservable *obs = RxObservable_createWithRxObservable_OnSubscribe_(create_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init());
  id<JavaUtilConcurrentFuture> f = RxInternalOperatorsBlockingOperatorToFuture_toFutureWithRxObservable_(obs);
  jboolean cancelled = [((id<JavaUtilConcurrentFuture>) nil_chk(f)) cancelWithBoolean:true];
  OrgJunitAssert_assertTrueWithBoolean_(cancelled);
  [f get];
}

- (void)testGetWithTimeoutAfterCancel {
  RxObservable *obs = RxObservable_createWithRxObservable_OnSubscribe_(create_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init());
  id<JavaUtilConcurrentFuture> f = RxInternalOperatorsBlockingOperatorToFuture_toFutureWithRxObservable_(obs);
  jboolean cancelled = [((id<JavaUtilConcurrentFuture>) nil_chk(f)) cancelWithBoolean:true];
  OrgJunitAssert_assertTrueWithBoolean_(cancelled);
  [f getWithLong:JavaLangLong_MAX_VALUE withJavaUtilConcurrentTimeUnit:JreLoadEnum(JavaUtilConcurrentTimeUnit, NANOSECONDS)];
}

- (void)testGetWithEmptyObservable {
  RxObservable *obs = RxObservable_empty();
  id<JavaUtilConcurrentFuture> f = [((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk(obs)) toBlocking])) toFuture];
  @try {
    [((id<JavaUtilConcurrentFuture>) nil_chk(f)) get];
  }
  @catch (JavaUtilConcurrentExecutionException *e) {
    @throw [((JavaUtilConcurrentExecutionException *) nil_chk(e)) getCause];
  }
}

- (void)testGetWithASingleNullItem {
  RxObservable *obs = RxObservable_justWithId_(nil);
  id<JavaUtilConcurrentFuture> f = [((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk(obs)) toBlocking])) toFuture];
  OrgJunitAssert_assertEqualsWithId_withId_(nil, [((id<JavaUtilConcurrentFuture>) nil_chk(f)) get]);
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToFutureTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, "V", 0x1, -1, -1, 1, -1, 2, -1 },
    { NULL, "V", 0x1, -1, -1, 1, -1, 3, -1 },
    { NULL, "V", 0x1, -1, -1, 4, -1, 5, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 6, -1 },
    { NULL, "V", 0x1, -1, -1, 7, -1, 8, -1 },
    { NULL, "V", 0x1, -1, -1, 7, -1, 9, -1 },
    { NULL, "V", 0x1, -1, -1, 4, -1, 10, -1 },
    { NULL, "V", 0x1, -1, -1, 7, -1, 11, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(constructorShouldBePrivate);
  methods[1].selector = @selector(testToFuture);
  methods[2].selector = @selector(testToFutureList);
  methods[3].selector = @selector(testExceptionWithMoreThanOneElement);
  methods[4].selector = @selector(testToFutureWithException);
  methods[5].selector = @selector(testGetAfterCancel);
  methods[6].selector = @selector(testGetWithTimeoutAfterCancel);
  methods[7].selector = @selector(testGetWithEmptyObservable);
  methods[8].selector = @selector(testGetWithASingleNullItem);
  methods[9].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$0, "LJavaLangInterruptedException;LJavaUtilConcurrentExecutionException;", (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$1, (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$2, "LNSException;", (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$3, (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$4, "LJavaLangException;", (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$5, (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$6, (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$7, (void *)&RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$8, "LRxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToFutureTest = { "BlockingOperatorToFutureTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 10, 0, -1, 12, -1, -1, -1 };
  return &_RxInternalOperatorsBlockingOperatorToFutureTest;
}

@end

void RxInternalOperatorsBlockingOperatorToFutureTest_init(RxInternalOperatorsBlockingOperatorToFutureTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToFutureTest *new_RxInternalOperatorsBlockingOperatorToFutureTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToFutureTest, init)
}

RxInternalOperatorsBlockingOperatorToFutureTest *create_RxInternalOperatorsBlockingOperatorToFutureTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToFutureTest, init)
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(JavaLangIllegalArgumentException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$4() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$5() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(JavaUtilConcurrentCancellationException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$6() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(JavaUtilConcurrentCancellationException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$7() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(JavaUtilNoSuchElementException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToFutureTest__Annotations$8() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsBlockingOperatorToFutureTest)

@implementation RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete

- (void)callWithId:(RxSubscriber *)unused {
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, NULL, 0x2, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(callWithId:);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-TT;>;)V", "LRxInternalOperatorsBlockingOperatorToFutureTest;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Observable$OnSubscribe<TT;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete = { "OperationNeverComplete", "rx.internal.operators", ptrTable, methods, NULL, 7, 0xa, 2, 0, 3, -1, -1, 4, -1 };
  return &_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete;
}

@end

void RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete *new_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete, init)
}

RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete *create_RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsBlockingOperatorToFutureTest_OperationNeverComplete)

@implementation RxInternalOperatorsBlockingOperatorToFutureTest_$1

- (void)callWithId:(RxSubscriber *)observer {
  [((RxSubscriber *) nil_chk(observer)) onNextWithId:@"one"];
  [observer onErrorWithNSException:create_RxExceptionsTestException_init()];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToFutureTest_$1_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(callWithId:);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/String;>;)V", "LRxInternalOperatorsBlockingOperatorToFutureTest;", "testToFutureWithException", "Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/lang/String;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToFutureTest_$1 = { "", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x8008, 2, 0, 3, -1, 4, 5, -1 };
  return &_RxInternalOperatorsBlockingOperatorToFutureTest_$1;
}

@end

void RxInternalOperatorsBlockingOperatorToFutureTest_$1_init(RxInternalOperatorsBlockingOperatorToFutureTest_$1 *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToFutureTest_$1 *new_RxInternalOperatorsBlockingOperatorToFutureTest_$1_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToFutureTest_$1, init)
}

RxInternalOperatorsBlockingOperatorToFutureTest_$1 *create_RxInternalOperatorsBlockingOperatorToFutureTest_$1_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToFutureTest_$1, init)
}
