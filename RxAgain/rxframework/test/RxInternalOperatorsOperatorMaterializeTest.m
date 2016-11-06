//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorMaterializeTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxFunctionsAction1.h"
#include "RxInternalOperatorsOperatorMaterializeTest.h"
#include "RxNotification.h"
#include "RxObservable.h"
#include "RxObservablesBlockingObservable.h"
#include "RxObserversTestSubscriber.h"
#include "RxScheduler.h"
#include "RxSchedulersSchedulers.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/io/PrintStream.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Integer.h"
#include "java/lang/InterruptedException.h"
#include "java/lang/NullPointerException.h"
#include "java/lang/Runnable.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/System.h"
#include "java/lang/Thread.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/Arrays.h"
#include "java/util/List.h"
#include "java/util/Vector.h"
#include "java/util/concurrent/Future.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$3();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$4();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$5();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$6();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$7();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$8();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$9();

@interface RxInternalOperatorsOperatorMaterializeTest_TestObserver : RxSubscriber {
 @public
  jboolean onCompleted_;
  jboolean onError_;
  id<JavaUtilList> notifications_;
}

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(RxNotification *)value;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorMaterializeTest_TestObserver)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorMaterializeTest_TestObserver, notifications_, id<JavaUtilList>)

__attribute__((unused)) static void RxInternalOperatorsOperatorMaterializeTest_TestObserver_init(RxInternalOperatorsOperatorMaterializeTest_TestObserver *self);

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_TestObserver *new_RxInternalOperatorsOperatorMaterializeTest_TestObserver_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_TestObserver *create_RxInternalOperatorsOperatorMaterializeTest_TestObserver_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorMaterializeTest_TestObserver)

@interface RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable : NSObject < RxObservable_OnSubscribe > {
 @public
  IOSObjectArray *valuesToReturn_;
  volatile_id t_;
}

- (instancetype)initWithNSStringArray:(IOSObjectArray *)values;

- (void)callWithId:(RxSubscriber *)observer;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable, valuesToReturn_, IOSObjectArray *)
J2OBJC_VOLATILE_FIELD_SETTER(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable, t_, JavaLangThread *)

__attribute__((unused)) static void RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *self, IOSObjectArray *values);

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *new_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(IOSObjectArray *values) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(IOSObjectArray *values);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable)

@interface RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 : NSObject < JavaLangRunnable > {
 @public
  RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *this$0_;
  RxSubscriber *val$observer_;
}

- (void)run;

- (instancetype)initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable:(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *)outer$
                                                                           withRxSubscriber:(RxSubscriber *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1, this$0_, RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *)
J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1, val$observer_, RxSubscriber *)

__attribute__((unused)) static void RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 *self, RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *outer$, RxSubscriber *capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 *new_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *outer$, RxSubscriber *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 *create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *outer$, RxSubscriber *capture$0);

@interface RxInternalOperatorsOperatorMaterializeTest_$1 : NSObject < RxFunctionsAction1 > {
 @public
  JavaLangRuntimeException *val$ex_;
}

- (void)callWithId:(id)t;

- (instancetype)initWithJavaLangRuntimeException:(JavaLangRuntimeException *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorMaterializeTest_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorMaterializeTest_$1, val$ex_, JavaLangRuntimeException *)

__attribute__((unused)) static void RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(RxInternalOperatorsOperatorMaterializeTest_$1 *self, JavaLangRuntimeException *capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_$1 *new_RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(JavaLangRuntimeException *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorMaterializeTest_$1 *create_RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(JavaLangRuntimeException *capture$0);

@implementation RxInternalOperatorsOperatorMaterializeTest

- (void)testMaterialize1 {
  RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *o1 = create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_([IOSObjectArray arrayWithObjects:(id[]){ @"one", @"two", nil, @"three" } count:4 type:NSString_class_()]);
  RxInternalOperatorsOperatorMaterializeTest_TestObserver *Observer = create_RxInternalOperatorsOperatorMaterializeTest_TestObserver_init();
  RxObservable *m = [((RxObservable *) nil_chk(RxObservable_createWithRxObservable_OnSubscribe_(o1))) materialize];
  [((RxObservable *) nil_chk(m)) subscribeWithRxSubscriber:Observer];
  @try {
    [((JavaLangThread *) nil_chk(JreLoadVolatileId(&o1->t_))) join];
  }
  @catch (JavaLangInterruptedException *e) {
    @throw create_JavaLangRuntimeException_initWithNSException_(e);
  }
  OrgJunitAssert_assertFalseWithBoolean_(Observer->onError_);
  OrgJunitAssert_assertTrueWithBoolean_(Observer->onCompleted_);
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk(Observer->notifications_)) size]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:0])) getValue]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:0])) isOnNext]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:1])) getValue]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:1])) isOnNext]);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangNullPointerException_class_(), [((NSException *) nil_chk([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:2])) getThrowable])) java_getClass]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:2])) isOnError]);
}

- (void)testMaterialize2 {
  RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *o1 = create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_([IOSObjectArray arrayWithObjects:(id[]){ @"one", @"two", @"three" } count:3 type:NSString_class_()]);
  RxInternalOperatorsOperatorMaterializeTest_TestObserver *Observer = create_RxInternalOperatorsOperatorMaterializeTest_TestObserver_init();
  RxObservable *m = [((RxObservable *) nil_chk(RxObservable_createWithRxObservable_OnSubscribe_(o1))) materialize];
  [((RxObservable *) nil_chk(m)) subscribeWithRxSubscriber:Observer];
  @try {
    [((JavaLangThread *) nil_chk(JreLoadVolatileId(&o1->t_))) join];
  }
  @catch (JavaLangInterruptedException *e) {
    @throw create_JavaLangRuntimeException_initWithNSException_(e);
  }
  OrgJunitAssert_assertFalseWithBoolean_(Observer->onError_);
  OrgJunitAssert_assertTrueWithBoolean_(Observer->onCompleted_);
  OrgJunitAssert_assertEqualsWithLong_withLong_(4, [((id<JavaUtilList>) nil_chk(Observer->notifications_)) size]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:0])) getValue]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:0])) isOnNext]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:1])) getValue]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:1])) isOnNext]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"three", [((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:2])) getValue]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:2])) isOnNext]);
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk(Observer->notifications_)) getWithInt:3])) isOnCompleted]);
}

- (void)testMultipleSubscribes {
  RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *o = create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_([IOSObjectArray arrayWithObjects:(id[]){ @"one", @"two", nil, @"three" } count:4 type:NSString_class_()]);
  RxObservable *m = [((RxObservable *) nil_chk(RxObservable_createWithRxObservable_OnSubscribe_(o))) materialize];
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk([((id<JavaUtilConcurrentFuture>) nil_chk([((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(m)) toList])) toBlocking])) toFuture])) get])) size]);
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk([((id<JavaUtilConcurrentFuture>) nil_chk([((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk([m toList])) toBlocking])) toFuture])) get])) size]);
}

- (void)testBackpressureOnEmptyStream {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_empty())) materialize])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts requestMoreWithLong:1];
  [ts assertValueCountWithInt:1];
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk([ts getOnNextEvents])) getWithInt:0])) isOnCompleted]);
  [ts assertCompleted];
}

- (void)testBackpressureNoError {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_justWithId_withId_withId_(JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2), JavaLangInteger_valueOfWithInt_(3)))) materialize])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts requestMoreWithLong:1];
  [ts assertValueCountWithInt:1];
  [ts requestMoreWithLong:2];
  [ts assertValueCountWithInt:3];
  [ts requestMoreWithLong:1];
  [ts assertValueCountWithInt:4];
  [ts assertCompleted];
}

- (void)testBackpressureNoErrorAsync {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_justWithId_withId_withId_(JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2), JavaLangInteger_valueOfWithInt_(3)))) materialize])) subscribeOnWithRxScheduler:RxSchedulersSchedulers_computation()])) subscribeWithRxSubscriber:ts];
  JavaLangThread_sleepWithLong_(100);
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts requestMoreWithLong:1];
  JavaLangThread_sleepWithLong_(100);
  [ts assertValueCountWithInt:1];
  [ts requestMoreWithLong:2];
  JavaLangThread_sleepWithLong_(100);
  [ts assertValueCountWithInt:3];
  [ts requestMoreWithLong:1];
  JavaLangThread_sleepWithLong_(100);
  [ts assertValueCountWithInt:4];
  [ts assertCompleted];
}

- (void)testBackpressureWithError {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_errorWithNSException_(create_JavaLangIllegalArgumentException_init()))) materialize])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts requestMoreWithLong:1];
  [ts assertValueCountWithInt:1];
  [ts assertCompleted];
}

- (void)testBackpressureWithEmissionThenError {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  JavaLangIllegalArgumentException *ex = create_JavaLangIllegalArgumentException_init();
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_fromWithJavaLangIterable_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ JavaLangInteger_valueOfWithInt_(1) } count:1 type:JavaLangInteger_class_()])))) concatWithWithRxObservable:RxObservable_errorWithNSException_(ex)])) materialize])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts requestMoreWithLong:1];
  [ts assertValueCountWithInt:1];
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk([ts getOnNextEvents])) getWithInt:0])) hasValue]);
  [ts requestMoreWithLong:1];
  [ts assertValueCountWithInt:2];
  OrgJunitAssert_assertTrueWithBoolean_([((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk([ts getOnNextEvents])) getWithInt:1])) isOnError]);
  OrgJunitAssert_assertTrueWithBoolean_(ex == [((RxNotification *) nil_chk([((id<JavaUtilList>) nil_chk([ts getOnNextEvents])) getWithInt:1])) getThrowable]);
  [ts assertCompleted];
}

- (void)testWithCompletionCausingError {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_create();
  JavaLangRuntimeException *ex = create_JavaLangRuntimeException_initWithNSString_(@"boo");
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_empty())) materialize])) doOnNextWithRxFunctionsAction1:create_RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(ex)])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertErrorWithNSException:ex];
  [ts assertNoValues];
  [ts assertTerminalEvent];
}

- (void)testUnsubscribeJustBeforeCompletionNotificationShouldPreventThatNotificationArriving {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_empty())) materialize])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts unsubscribe];
  [ts requestMoreWithLong:1];
  [ts assertNoValues];
  [ts assertUnsubscribed];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorMaterializeTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 1, -1 },
    { NULL, "V", 0x1, -1, -1, 2, -1, 3, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 4, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 5, -1 },
    { NULL, "V", 0x1, -1, -1, 6, -1, 7, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 8, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 9, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 10, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 11, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(testMaterialize1);
  methods[1].selector = @selector(testMaterialize2);
  methods[2].selector = @selector(testMultipleSubscribes);
  methods[3].selector = @selector(testBackpressureOnEmptyStream);
  methods[4].selector = @selector(testBackpressureNoError);
  methods[5].selector = @selector(testBackpressureNoErrorAsync);
  methods[6].selector = @selector(testBackpressureWithError);
  methods[7].selector = @selector(testBackpressureWithEmissionThenError);
  methods[8].selector = @selector(testWithCompletionCausingError);
  methods[9].selector = @selector(testUnsubscribeJustBeforeCompletionNotificationShouldPreventThatNotificationArriving);
  methods[10].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$0, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$1, "LJavaLangInterruptedException;LJavaUtilConcurrentExecutionException;", (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$2, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$3, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$4, "LJavaLangInterruptedException;", (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$5, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$6, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$7, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$8, (void *)&RxInternalOperatorsOperatorMaterializeTest__Annotations$9, "LRxInternalOperatorsOperatorMaterializeTest_TestObserver;LRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMaterializeTest = { "OperatorMaterializeTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 11, 0, -1, 12, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorMaterializeTest;
}

@end

void RxInternalOperatorsOperatorMaterializeTest_init(RxInternalOperatorsOperatorMaterializeTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorMaterializeTest *new_RxInternalOperatorsOperatorMaterializeTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMaterializeTest, init)
}

RxInternalOperatorsOperatorMaterializeTest *create_RxInternalOperatorsOperatorMaterializeTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMaterializeTest, init)
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$4() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$5() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$6() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$7() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$8() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorMaterializeTest__Annotations$9() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorMaterializeTest)

@implementation RxInternalOperatorsOperatorMaterializeTest_TestObserver

- (void)onCompleted {
  self->onCompleted_ = true;
}

- (void)onErrorWithNSException:(NSException *)e {
  self->onError_ = true;
}

- (void)onNextWithId:(RxNotification *)value {
  [((id<JavaUtilList>) nil_chk(self->notifications_)) addWithId:value];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorMaterializeTest_TestObserver_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  JreCheckFinalize(self, [RxInternalOperatorsOperatorMaterializeTest_TestObserver class]);
  RELEASE_(notifications_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, 4, -1, -1 },
    { NULL, NULL, 0x2, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onCompleted);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onNextWithId:);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "onCompleted_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "onError_", "Z", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "notifications_", "LJavaUtilList;", .constantValue.asLong = 0, 0x0, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "onError", "LNSException;", "onNext", "LRxNotification;", "(Lrx/Notification<Ljava/lang/String;>;)V", "Ljava/util/List<Lrx/Notification<Ljava/lang/String;>;>;", "LRxInternalOperatorsOperatorMaterializeTest;", "Lrx/Subscriber<Lrx/Notification<Ljava/lang/String;>;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMaterializeTest_TestObserver = { "TestObserver", "rx.internal.operators", ptrTable, methods, fields, 7, 0xa, 4, 3, 6, -1, -1, 7, -1 };
  return &_RxInternalOperatorsOperatorMaterializeTest_TestObserver;
}

@end

void RxInternalOperatorsOperatorMaterializeTest_TestObserver_init(RxInternalOperatorsOperatorMaterializeTest_TestObserver *self) {
  RxSubscriber_init(self);
  self->onCompleted_ = false;
  self->onError_ = false;
  JreStrongAssignAndConsume(&self->notifications_, new_JavaUtilVector_init());
}

RxInternalOperatorsOperatorMaterializeTest_TestObserver *new_RxInternalOperatorsOperatorMaterializeTest_TestObserver_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMaterializeTest_TestObserver, init)
}

RxInternalOperatorsOperatorMaterializeTest_TestObserver *create_RxInternalOperatorsOperatorMaterializeTest_TestObserver_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMaterializeTest_TestObserver, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorMaterializeTest_TestObserver)

@implementation RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable

- (instancetype)initWithNSStringArray:(IOSObjectArray *)values {
  RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(self, values);
  return self;
}

- (void)callWithId:(RxSubscriber *)observer {
  JreVolatileStrongAssignAndConsume(&t_, new_JavaLangThread_initWithJavaLangRunnable_(create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(self, observer)));
  [((JavaLangThread *) nil_chk(JreLoadVolatileId(&t_))) start];
}

- (void)__javaClone:(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *)original {
  [super __javaClone:original];
  JreCloneVolatileStrong(&t_, &original->t_);
}

- (void)dealloc {
  RELEASE_(valuesToReturn_);
  JreReleaseVolatile(&t_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x80, -1, 0, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 1, 2, -1, 3, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initWithNSStringArray:);
  methods[1].selector = @selector(callWithId:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "valuesToReturn_", "[LNSString;", .constantValue.asLong = 0, 0x0, -1, -1, -1, -1 },
    { "t_", "LJavaLangThread;", .constantValue.asLong = 0, 0x40, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "[LNSString;", "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/String;>;)V", "LRxInternalOperatorsOperatorMaterializeTest;", "Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/lang/String;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable = { "TestAsyncErrorObservable", "rx.internal.operators", ptrTable, methods, fields, 7, 0xa, 2, 2, 4, -1, -1, 5, -1 };
  return &_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable;
}

@end

void RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *self, IOSObjectArray *values) {
  NSObject_init(self);
  JreStrongAssign(&self->valuesToReturn_, values);
}

RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *new_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(IOSObjectArray *values) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable, initWithNSStringArray_, values)
}

RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_initWithNSStringArray_(IOSObjectArray *values) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable, initWithNSStringArray_, values)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable)

@implementation RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1

- (void)run {
  {
    IOSObjectArray *a__ = this$0_->valuesToReturn_;
    NSString * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
    NSString * const *e__ = b__ + a__->size_;
    while (b__ < e__) {
      NSString *s = *b__++;
      if (s == nil) {
        [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:@"throwing exception"];
        @try {
          JavaLangThread_sleepWithLong_(100);
        }
        @catch (NSException *e) {
        }
        [((RxSubscriber *) nil_chk(val$observer_)) onErrorWithNSException:create_JavaLangNullPointerException_init()];
        return;
      }
      else {
        [((RxSubscriber *) nil_chk(val$observer_)) onNextWithId:s];
      }
    }
  }
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:@"subscription complete"];
  [((RxSubscriber *) nil_chk(val$observer_)) onCompleted];
}

- (instancetype)initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable:(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *)outer$
                                                                           withRxSubscriber:(RxSubscriber *)capture$0 {
  RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(self, outer$, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
  RELEASE_(val$observer_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(run);
  methods[1].selector = @selector(initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable:withRxSubscriber:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$observer_", "LRxSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 2, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable;LRxSubscriber;", "(Lrx/internal/operators/OperatorMaterializeTest$TestAsyncErrorObservable;Lrx/Subscriber<-Ljava/lang/String;>;)V", "Lrx/Subscriber<-Ljava/lang/String;>;", "LRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 2, 3, -1, 4, -1, -1 };
  return &_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1;
}

@end

void RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 *self, RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *outer$, RxSubscriber *capture$0) {
  JreStrongAssign(&self->this$0_, outer$);
  JreStrongAssign(&self->val$observer_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 *new_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *outer$, RxSubscriber *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1, initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_, outer$, capture$0)
}

RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1 *create_RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1_initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable *outer$, RxSubscriber *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_$1, initWithRxInternalOperatorsOperatorMaterializeTest_TestAsyncErrorObservable_withRxSubscriber_, outer$, capture$0)
}

@implementation RxInternalOperatorsOperatorMaterializeTest_$1

- (void)callWithId:(id)t {
  @throw val$ex_;
}

- (instancetype)initWithJavaLangRuntimeException:(JavaLangRuntimeException *)capture$0 {
  RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$ex_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(callWithId:);
  methods[1].selector = @selector(initWithJavaLangRuntimeException:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$ex_", "LJavaLangRuntimeException;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "call", "LNSObject;", "LJavaLangRuntimeException;", "LRxInternalOperatorsOperatorMaterializeTest;", "testWithCompletionCausingError", "Ljava/lang/Object;Lrx/functions/Action1<Ljava/lang/Object;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorMaterializeTest_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 3, -1, 4, 5, -1 };
  return &_RxInternalOperatorsOperatorMaterializeTest_$1;
}

@end

void RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(RxInternalOperatorsOperatorMaterializeTest_$1 *self, JavaLangRuntimeException *capture$0) {
  JreStrongAssign(&self->val$ex_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOperatorMaterializeTest_$1 *new_RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(JavaLangRuntimeException *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorMaterializeTest_$1, initWithJavaLangRuntimeException_, capture$0)
}

RxInternalOperatorsOperatorMaterializeTest_$1 *create_RxInternalOperatorsOperatorMaterializeTest_$1_initWithJavaLangRuntimeException_(JavaLangRuntimeException *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorMaterializeTest_$1, initWithJavaLangRuntimeException_, capture$0)
}
