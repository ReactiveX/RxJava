//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorSkipLastTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxFunctionsFunc1.h"
#include "RxInternalOperatorsOperatorSkipLastTest.h"
#include "RxInternalUtilRxRingBuffer.h"
#include "RxObservable.h"
#include "RxObserver.h"
#include "RxObserversTestSubscriber.h"
#include "RxPluginsRxJavaHooks.h"
#include "RxScheduler.h"
#include "RxSchedulersSchedulers.h"
#include "RxSchedulersTestScheduler.h"
#include "RxSubjectsPublishSubject.h"
#include "RxSubscription.h"
#include "java/lang/IndexOutOfBoundsException.h"
#include "java/lang/Integer.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/Arrays.h"
#include "java/util/List.h"
#include "java/util/concurrent/TimeUnit.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"
#include "org/mockito/InOrder.h"
#include "org/mockito/Matchers.h"
#include "org/mockito/Mockito.h"
#include "org/mockito/verification/VerificationMode.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$3();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$4();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$5();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$6();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$7();

@interface RxInternalOperatorsOperatorSkipLastTest_$1 : NSObject < RxFunctionsFunc1 > {
 @public
  RxSchedulersTestScheduler *val$scheduler_;
}

- (RxScheduler *)callWithId:(RxScheduler *)t;

- (instancetype)initWithRxSchedulersTestScheduler:(RxSchedulersTestScheduler *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorSkipLastTest_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorSkipLastTest_$1, val$scheduler_, RxSchedulersTestScheduler *)

__attribute__((unused)) static void RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(RxInternalOperatorsOperatorSkipLastTest_$1 *self, RxSchedulersTestScheduler *capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorSkipLastTest_$1 *new_RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(RxSchedulersTestScheduler *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorSkipLastTest_$1 *create_RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(RxSchedulersTestScheduler *capture$0);

@implementation RxInternalOperatorsOperatorSkipLastTest

- (void)testSkipLastEmpty {
  RxObservable *observable = [((RxObservable *) nil_chk(RxObservable_empty())) skipLastWithInt:2];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onNextWithId:OrgMockitoMatchers_anyWithIOSClass_(NSString_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
}

- (void)testSkipLast1 {
  RxObservable *observable = [((RxObservable *) nil_chk(RxObservable_fromWithJavaLangIterable_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ @"one", @"two", @"three" } count:3 type:NSString_class_()])))) skipLastWithInt:2];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  id<OrgMockitoInOrder> inOrder = OrgMockitoMockito_inOrderWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ observer } count:1 type:NSObject_class_()]);
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk([((id<OrgMockitoInOrder>) nil_chk(inOrder)) verifyWithId:observer withOrgMockitoVerificationVerificationMode:OrgMockitoMockito_never()])) onNextWithId:@"two"];
  [((id<RxObserver>) nil_chk([inOrder verifyWithId:observer withOrgMockitoVerificationVerificationMode:OrgMockitoMockito_never()])) onNextWithId:@"three"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
}

- (void)testSkipLast2 {
  RxObservable *observable = [((RxObservable *) nil_chk(RxObservable_fromWithJavaLangIterable_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ @"one", @"two" } count:2 type:NSString_class_()])))) skipLastWithInt:2];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onNextWithId:OrgMockitoMatchers_anyWithIOSClass_(NSString_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
}

- (void)testSkipLastWithZeroCount {
  RxObservable *w = RxObservable_justWithId_withId_(@"one", @"two");
  RxObservable *observable = [((RxObservable *) nil_chk(w)) skipLastWithInt:0];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"two"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
}

- (void)testSkipLastWithNull {
  RxObservable *observable = [((RxObservable *) nil_chk(RxObservable_fromWithJavaLangIterable_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ @"one", nil, @"two" } count:3 type:NSString_class_()])))) skipLastWithInt:1];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:nil];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onNextWithId:@"two"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
}

- (void)testSkipLastWithBackpressure {
  RxObservable *o = [((RxObservable *) nil_chk(RxObservable_rangeWithInt_withInt_(0, JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE) * 2))) skipLastWithInt:JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE) + 10];
  RxObserversTestSubscriber *ts = create_RxObserversTestSubscriber_init();
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(o)) observeOnWithRxScheduler:RxSchedulersSchedulers_computation()])) subscribeWithRxSubscriber:ts];
  [ts awaitTerminalEvent];
  [ts assertNoErrors];
  OrgJunitAssert_assertEqualsWithLong_withLong_((JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE)) - 10, [((id<JavaUtilList>) nil_chk([ts getOnNextEvents])) size]);
}

- (void)testSkipLastWithNegativeCount {
  [((RxObservable *) nil_chk(RxObservable_justWithId_(@"one"))) skipLastWithInt:-1];
}

- (void)skipLastDefaultScheduler {
  RxSchedulersTestScheduler *scheduler = create_RxSchedulersTestScheduler_init();
  RxPluginsRxJavaHooks_setOnComputationSchedulerWithRxFunctionsFunc1_(create_RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(scheduler));
  @try {
    RxObserversTestSubscriber *ts = RxObserversTestSubscriber_create();
    RxSubjectsPublishSubject *ps = RxSubjectsPublishSubject_create();
    [((RxObservable *) nil_chk([((RxSubjectsPublishSubject *) nil_chk(ps)) skipLastWithLong:1 withJavaUtilConcurrentTimeUnit:JreLoadEnum(JavaUtilConcurrentTimeUnit, SECONDS)])) subscribeWithRxSubscriber:ts];
    [ps onNextWithId:JavaLangInteger_valueOfWithInt_(1)];
    [ps onNextWithId:JavaLangInteger_valueOfWithInt_(2)];
    [ps onNextWithId:JavaLangInteger_valueOfWithInt_(3)];
    [scheduler advanceTimeByWithLong:1 withJavaUtilConcurrentTimeUnit:JreLoadEnum(JavaUtilConcurrentTimeUnit, SECONDS)];
    [ps onNextWithId:JavaLangInteger_valueOfWithInt_(4)];
    [ps onNextWithId:JavaLangInteger_valueOfWithInt_(5)];
    [scheduler advanceTimeByWithLong:1 withJavaUtilConcurrentTimeUnit:JreLoadEnum(JavaUtilConcurrentTimeUnit, SECONDS)];
    [ps onCompleted];
    [((RxObserversTestSubscriber *) nil_chk(ts)) assertValuesWithNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2), JavaLangInteger_valueOfWithInt_(3) } count:3 type:JavaLangInteger_class_()]];
    [ts assertNoErrors];
    [ts assertCompleted];
  }
  @finally {
    RxPluginsRxJavaHooks_reset();
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorSkipLastTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 2, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 3, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 4, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 5, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 6, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 7, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(testSkipLastEmpty);
  methods[1].selector = @selector(testSkipLast1);
  methods[2].selector = @selector(testSkipLast2);
  methods[3].selector = @selector(testSkipLastWithZeroCount);
  methods[4].selector = @selector(testSkipLastWithNull);
  methods[5].selector = @selector(testSkipLastWithBackpressure);
  methods[6].selector = @selector(testSkipLastWithNegativeCount);
  methods[7].selector = @selector(skipLastDefaultScheduler);
  methods[8].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$0, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$1, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$2, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$3, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$4, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$5, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$6, (void *)&RxInternalOperatorsOperatorSkipLastTest__Annotations$7 };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSkipLastTest = { "OperatorSkipLastTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 9, 0, -1, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorSkipLastTest;
}

@end

void RxInternalOperatorsOperatorSkipLastTest_init(RxInternalOperatorsOperatorSkipLastTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorSkipLastTest *new_RxInternalOperatorsOperatorSkipLastTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSkipLastTest, init)
}

RxInternalOperatorsOperatorSkipLastTest *create_RxInternalOperatorsOperatorSkipLastTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSkipLastTest, init)
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$4() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$5() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$6() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(JavaLangIndexOutOfBoundsException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorSkipLastTest__Annotations$7() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorSkipLastTest)

@implementation RxInternalOperatorsOperatorSkipLastTest_$1

- (RxScheduler *)callWithId:(RxScheduler *)t {
  return val$scheduler_;
}

- (instancetype)initWithRxSchedulersTestScheduler:(RxSchedulersTestScheduler *)capture$0 {
  RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$scheduler_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxScheduler;", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(callWithId:);
  methods[1].selector = @selector(initWithRxSchedulersTestScheduler:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$scheduler_", "LRxSchedulersTestScheduler;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "call", "LRxScheduler;", "LRxSchedulersTestScheduler;", "LRxInternalOperatorsOperatorSkipLastTest;", "skipLastDefaultScheduler", "Ljava/lang/Object;Lrx/functions/Func1<Lrx/Scheduler;Lrx/Scheduler;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorSkipLastTest_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 3, -1, 4, 5, -1 };
  return &_RxInternalOperatorsOperatorSkipLastTest_$1;
}

@end

void RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(RxInternalOperatorsOperatorSkipLastTest_$1 *self, RxSchedulersTestScheduler *capture$0) {
  JreStrongAssign(&self->val$scheduler_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsOperatorSkipLastTest_$1 *new_RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(RxSchedulersTestScheduler *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorSkipLastTest_$1, initWithRxSchedulersTestScheduler_, capture$0)
}

RxInternalOperatorsOperatorSkipLastTest_$1 *create_RxInternalOperatorsOperatorSkipLastTest_$1_initWithRxSchedulersTestScheduler_(RxSchedulersTestScheduler *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorSkipLastTest_$1, initWithRxSchedulersTestScheduler_, capture$0)
}
