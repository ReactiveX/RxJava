//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/OperatorDefaultIfEmptyTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsTestException.h"
#include "RxInternalOperatorsOperatorDefaultIfEmptyTest.h"
#include "RxObservable.h"
#include "RxObserver.h"
#include "RxObserversTestSubscriber.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/lang/Integer.h"
#include "java/lang/annotation/Annotation.h"
#include "org/junit/Test.h"
#include "org/mockito/Matchers.h"
#include "org/mockito/Mockito.h"
#include "org/mockito/verification/VerificationMode.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$3();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$4();

@interface RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 : RxSubscriber {
 @public
  id<RxObserver> val$o_;
}

- (void)onNextWithId:(JavaLangInteger *)t;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onCompleted;

- (instancetype)initWithRxObserver:(id<RxObserver>)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsOperatorDefaultIfEmptyTest_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsOperatorDefaultIfEmptyTest_$1, val$o_, id<RxObserver>)

__attribute__((unused)) static void RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 *self, id<RxObserver> capture$0);

__attribute__((unused)) static RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 *new_RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(id<RxObserver> capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 *create_RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(id<RxObserver> capture$0);

@implementation RxInternalOperatorsOperatorDefaultIfEmptyTest

- (void)testDefaultIfEmpty {
  RxObservable *source = RxObservable_justWithId_withId_withId_(JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2), JavaLangInteger_valueOfWithInt_(3));
  RxObservable *observable = [((RxObservable *) nil_chk(source)) defaultIfEmptyWithId:JavaLangInteger_valueOfWithInt_(10)];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onNextWithId:JavaLangInteger_valueOfWithInt_(10)];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(observer))) onNextWithId:JavaLangInteger_valueOfWithInt_(1)];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(observer))) onNextWithId:JavaLangInteger_valueOfWithInt_(2)];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(observer))) onNextWithId:JavaLangInteger_valueOfWithInt_(3)];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(observer))) onCompleted];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
}

- (void)testDefaultIfEmptyWithEmpty {
  RxObservable *source = RxObservable_empty();
  RxObservable *observable = [((RxObservable *) nil_chk(source)) defaultIfEmptyWithId:JavaLangInteger_valueOfWithInt_(10)];
  id<RxObserver> observer = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk(observable)) subscribeWithRxObserver:observer];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(observer))) onNextWithId:JavaLangInteger_valueOfWithInt_(10)];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(observer))) onCompleted];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(observer, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
}

- (void)testEmptyButClientThrows {
  id<RxObserver> o = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_empty())) defaultIfEmptyWithId:JavaLangInteger_valueOfWithInt_(1)])) subscribeWithRxSubscriber:create_RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(o)];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_(o))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(RxExceptionsTestException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(o, OrgMockitoMockito_never()))) onNextWithId:OrgMockitoMatchers_anyWithIOSClass_(JavaLangInteger_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(o, OrgMockitoMockito_never()))) onCompleted];
}

- (void)testBackpressureEmpty {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_empty())) defaultIfEmptyWithId:JavaLangInteger_valueOfWithInt_(1)])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts assertNoTerminalEvent];
  [ts requestMoreWithLong:1];
  [ts assertValueWithId:JavaLangInteger_valueOfWithInt_(1)];
  [ts assertCompleted];
}

- (void)testBackpressureNonEmpty {
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_createWithLong_(0);
  [((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_justWithId_withId_withId_(JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2), JavaLangInteger_valueOfWithInt_(3)))) defaultIfEmptyWithId:JavaLangInteger_valueOfWithInt_(1)])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNoValues];
  [ts assertNoTerminalEvent];
  [ts requestMoreWithLong:2];
  [ts assertValuesWithNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2) } count:2 type:JavaLangInteger_class_()]];
  [ts requestMoreWithLong:1];
  [ts assertValuesWithNSObjectArray:[IOSObjectArray arrayWithObjects:(id[]){ JavaLangInteger_valueOfWithInt_(1), JavaLangInteger_valueOfWithInt_(2), JavaLangInteger_valueOfWithInt_(3) } count:3 type:JavaLangInteger_class_()]];
  [ts assertCompleted];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsOperatorDefaultIfEmptyTest_init(self);
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
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(testDefaultIfEmpty);
  methods[1].selector = @selector(testDefaultIfEmptyWithEmpty);
  methods[2].selector = @selector(testEmptyButClientThrows);
  methods[3].selector = @selector(testBackpressureEmpty);
  methods[4].selector = @selector(testBackpressureNonEmpty);
  methods[5].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$0, (void *)&RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$1, (void *)&RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$2, (void *)&RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$3, (void *)&RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$4 };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDefaultIfEmptyTest = { "OperatorDefaultIfEmptyTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 6, 0, -1, -1, -1, -1, -1 };
  return &_RxInternalOperatorsOperatorDefaultIfEmptyTest;
}

@end

void RxInternalOperatorsOperatorDefaultIfEmptyTest_init(RxInternalOperatorsOperatorDefaultIfEmptyTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsOperatorDefaultIfEmptyTest *new_RxInternalOperatorsOperatorDefaultIfEmptyTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDefaultIfEmptyTest, init)
}

RxInternalOperatorsOperatorDefaultIfEmptyTest *create_RxInternalOperatorsOperatorDefaultIfEmptyTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDefaultIfEmptyTest, init)
}

IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsOperatorDefaultIfEmptyTest__Annotations$4() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsOperatorDefaultIfEmptyTest)

@implementation RxInternalOperatorsOperatorDefaultIfEmptyTest_$1

- (void)onNextWithId:(JavaLangInteger *)t {
  @throw create_RxExceptionsTestException_init();
}

- (void)onErrorWithNSException:(NSException *)e {
  [((id<RxObserver>) nil_chk(val$o_)) onErrorWithNSException:e];
}

- (void)onCompleted {
  [((id<RxObserver>) nil_chk(val$o_)) onCompleted];
}

- (instancetype)initWithRxObserver:(id<RxObserver>)capture$0 {
  RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$o_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 4, -1, 5, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onNextWithId:);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onCompleted);
  methods[3].selector = @selector(initWithRxObserver:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$o_", "LRxObserver;", .constantValue.asLong = 0, 0x1012, -1, -1, 6, -1 },
  };
  static const void *ptrTable[] = { "onNext", "LJavaLangInteger;", "onError", "LNSException;", "LRxObserver;", "(Lrx/Observer<Ljava/lang/Integer;>;)V", "Lrx/Observer<Ljava/lang/Integer;>;", "LRxInternalOperatorsOperatorDefaultIfEmptyTest;", "testEmptyButClientThrows", "Lrx/Subscriber<Ljava/lang/Integer;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 4, 1, 7, -1, 8, 9, -1 };
  return &_RxInternalOperatorsOperatorDefaultIfEmptyTest_$1;
}

@end

void RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 *self, id<RxObserver> capture$0) {
  JreStrongAssign(&self->val$o_, capture$0);
  RxSubscriber_init(self);
}

RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 *new_RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(id<RxObserver> capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsOperatorDefaultIfEmptyTest_$1, initWithRxObserver_, capture$0)
}

RxInternalOperatorsOperatorDefaultIfEmptyTest_$1 *create_RxInternalOperatorsOperatorDefaultIfEmptyTest_$1_initWithRxObserver_(id<RxObserver> capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsOperatorDefaultIfEmptyTest_$1, initWithRxObserver_, capture$0)
}
