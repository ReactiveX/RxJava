//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/InternalSafeSubscriberTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxInternalOperatorsInternalSafeSubscriberTest.h"
#include "RxObservable.h"
#include "RxObserver.h"
#include "RxObserversSafeSubscriber.h"
#include "RxObserversTestSubscriber.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/io/PrintStream.h"
#include "java/lang/RuntimeException.h"
#include "java/lang/System.h"
#include "java/lang/annotation/Annotation.h"
#include "org/junit/Test.h"
#include "org/mockito/Matchers.h"
#include "org/mockito/Mockito.h"
#include "org/mockito/verification/VerificationMode.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$3();

@interface RxInternalOperatorsInternalSafeSubscriberTest_TestObservable : NSObject < RxObservable_OnSubscribe > {
 @public
  id<RxObserver> observer_;
}

- (void)sendOnCompleted;

- (void)sendOnNextWithNSString:(NSString *)value;

- (void)sendOnErrorWithNSException:(NSException *)e;

- (void)callWithId:(RxSubscriber *)observer;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable)

J2OBJC_FIELD_SETTER(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable, observer_, id<RxObserver>)

__attribute__((unused)) static void RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *self);

__attribute__((unused)) static RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *new_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init();

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable)

@interface RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 : NSObject < RxSubscription >

- (void)unsubscribe;

- (jboolean)isUnsubscribed;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1)

__attribute__((unused)) static void RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 *self);

__attribute__((unused)) static RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 *new_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 *create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init();

@implementation RxInternalOperatorsInternalSafeSubscriberTest

- (void)testOnNextAfterOnError {
  RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *t = create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init();
  RxObservable *st = RxObservable_createWithRxObservable_OnSubscribe_(t);
  id<RxObserver> w = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  __unused id<RxSubscription> ws = [((RxObservable *) nil_chk(st)) subscribeWithRxSubscriber:create_RxObserversSafeSubscriber_initWithRxSubscriber_(create_RxObserversTestSubscriber_initWithRxObserver_(w))];
  [t sendOnNextWithNSString:@"one"];
  [t sendOnErrorWithNSException:create_JavaLangRuntimeException_initWithNSString_(@"bad")];
  [t sendOnNextWithNSString:@"two"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_never()))) onNextWithId:@"two"];
}

- (void)testOnCompletedAfterOnError {
  RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *t = create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init();
  RxObservable *st = RxObservable_createWithRxObservable_OnSubscribe_(t);
  id<RxObserver> w = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  __unused id<RxSubscription> ws = [((RxObservable *) nil_chk(st)) subscribeWithRxSubscriber:create_RxObserversSafeSubscriber_initWithRxSubscriber_(create_RxObserversTestSubscriber_initWithRxObserver_(w))];
  [t sendOnNextWithNSString:@"one"];
  [t sendOnErrorWithNSException:create_JavaLangRuntimeException_initWithNSString_(@"bad")];
  [t sendOnCompleted];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_never()))) onCompleted];
}

- (void)testOnNextAfterOnCompleted {
  RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *t = create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init();
  RxObservable *st = RxObservable_createWithRxObservable_OnSubscribe_(t);
  id<RxObserver> w = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  __unused id<RxSubscription> ws = [((RxObservable *) nil_chk(st)) subscribeWithRxSubscriber:create_RxObserversSafeSubscriber_initWithRxSubscriber_(create_RxObserversTestSubscriber_initWithRxObserver_(w))];
  [t sendOnNextWithNSString:@"one"];
  [t sendOnCompleted];
  [t sendOnNextWithNSString:@"two"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_never()))) onNextWithId:@"two"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
}

- (void)testOnErrorAfterOnCompleted {
  RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *t = create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init();
  RxObservable *st = RxObservable_createWithRxObservable_OnSubscribe_(t);
  id<RxObserver> w = OrgMockitoMockito_mockWithIOSClass_(RxObserver_class_());
  __unused id<RxSubscription> ws = [((RxObservable *) nil_chk(st)) subscribeWithRxSubscriber:create_RxObserversSafeSubscriber_initWithRxSubscriber_(create_RxObserversTestSubscriber_initWithRxObserver_(w))];
  [t sendOnNextWithNSString:@"one"];
  [t sendOnCompleted];
  [t sendOnErrorWithNSException:create_JavaLangRuntimeException_initWithNSString_(@"bad")];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onNextWithId:@"one"];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_timesWithInt_(1)))) onCompleted];
  [((id<RxObserver>) nil_chk(OrgMockitoMockito_verifyWithId_withOrgMockitoVerificationVerificationMode_(w, OrgMockitoMockito_never()))) onErrorWithNSException:OrgMockitoMatchers_anyWithIOSClass_(NSException_class_())];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsInternalSafeSubscriberTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 2, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 3, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(testOnNextAfterOnError);
  methods[1].selector = @selector(testOnCompletedAfterOnError);
  methods[2].selector = @selector(testOnNextAfterOnCompleted);
  methods[3].selector = @selector(testOnErrorAfterOnCompleted);
  methods[4].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsInternalSafeSubscriberTest__Annotations$0, (void *)&RxInternalOperatorsInternalSafeSubscriberTest__Annotations$1, (void *)&RxInternalOperatorsInternalSafeSubscriberTest__Annotations$2, (void *)&RxInternalOperatorsInternalSafeSubscriberTest__Annotations$3, "LRxInternalOperatorsInternalSafeSubscriberTest_TestObservable;" };
  static const J2ObjcClassInfo _RxInternalOperatorsInternalSafeSubscriberTest = { "InternalSafeSubscriberTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 5, 0, -1, 4, -1, -1, -1 };
  return &_RxInternalOperatorsInternalSafeSubscriberTest;
}

@end

void RxInternalOperatorsInternalSafeSubscriberTest_init(RxInternalOperatorsInternalSafeSubscriberTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsInternalSafeSubscriberTest *new_RxInternalOperatorsInternalSafeSubscriberTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsInternalSafeSubscriberTest, init)
}

RxInternalOperatorsInternalSafeSubscriberTest *create_RxInternalOperatorsInternalSafeSubscriberTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsInternalSafeSubscriberTest, init)
}

IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsInternalSafeSubscriberTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsInternalSafeSubscriberTest)

@implementation RxInternalOperatorsInternalSafeSubscriberTest_TestObservable

- (void)sendOnCompleted {
  [((id<RxObserver>) nil_chk(observer_)) onCompleted];
}

- (void)sendOnNextWithNSString:(NSString *)value {
  [((id<RxObserver>) nil_chk(observer_)) onNextWithId:value];
}

- (void)sendOnErrorWithNSException:(NSException *)e {
  [((id<RxObserver>) nil_chk(observer_)) onErrorWithNSException:e];
}

- (void)callWithId:(RxSubscriber *)observer {
  JreStrongAssign(&self->observer_, observer);
  [((RxSubscriber *) nil_chk(observer)) addWithRxSubscription:create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init()];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  RELEASE_(observer_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 4, 5, -1, 6, -1, -1 },
    { NULL, NULL, 0x2, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(sendOnCompleted);
  methods[1].selector = @selector(sendOnNextWithNSString:);
  methods[2].selector = @selector(sendOnErrorWithNSException:);
  methods[3].selector = @selector(callWithId:);
  methods[4].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "observer_", "LRxObserver;", .constantValue.asLong = 0, 0x0, -1, -1, 7, -1 },
  };
  static const void *ptrTable[] = { "sendOnNext", "LNSString;", "sendOnError", "LNSException;", "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/String;>;)V", "Lrx/Observer<-Ljava/lang/String;>;", "LRxInternalOperatorsInternalSafeSubscriberTest;", "Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/lang/String;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsInternalSafeSubscriberTest_TestObservable = { "TestObservable", "rx.internal.operators", ptrTable, methods, fields, 7, 0xa, 5, 1, 8, -1, -1, 9, -1 };
  return &_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable;
}

@end

void RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *self) {
  NSObject_init(self);
  JreStrongAssign(&self->observer_, nil);
}

RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *new_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable, init)
}

RxInternalOperatorsInternalSafeSubscriberTest_TestObservable *create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable)

@implementation RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1

- (void)unsubscribe {
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:@"==> SynchronizeTest unsubscribe that does nothing!"];
}

- (jboolean)isUnsubscribed {
  return false;
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(unsubscribe);
  methods[1].selector = @selector(isUnsubscribed);
  methods[2].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LRxInternalOperatorsInternalSafeSubscriberTest_TestObservable;", "callWithId:" };
  static const J2ObjcClassInfo _RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 = { "", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x8008, 3, 0, 0, -1, 1, -1, -1 };
  return &_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1;
}

@end

void RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 *self) {
  NSObject_init(self);
}

RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 *new_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1, init)
}

RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1 *create_RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsInternalSafeSubscriberTest_TestObservable_$1, init)
}
