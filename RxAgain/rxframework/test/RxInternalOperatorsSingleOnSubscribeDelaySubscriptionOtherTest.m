//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/SingleOnSubscribeDelaySubscriptionOtherTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsTestException.h"
#include "RxFunctionsAction0.h"
#include "RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest.h"
#include "RxObserversTestSubscriber.h"
#include "RxSingle.h"
#include "RxSubjectsPublishSubject.h"
#include "RxSubscription.h"
#include "java/lang/Integer.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$2();

@interface RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 : NSObject < RxFunctionsAction0 > {
 @public
  JavaUtilConcurrentAtomicAtomicInteger *val$subscribed_;
}

- (void)call;

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1)

J2OBJC_FIELD_SETTER(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1, val$subscribed_, JavaUtilConcurrentAtomicAtomicInteger *)

__attribute__((unused)) static void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0);

__attribute__((unused)) static RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0);

@interface RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 : NSObject < RxFunctionsAction0 > {
 @public
  JavaUtilConcurrentAtomicAtomicInteger *val$subscribed_;
}

- (void)call;

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2)

J2OBJC_FIELD_SETTER(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2, val$subscribed_, JavaUtilConcurrentAtomicAtomicInteger *)

__attribute__((unused)) static void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0);

__attribute__((unused)) static RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0);

@interface RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 : NSObject < RxFunctionsAction0 > {
 @public
  JavaUtilConcurrentAtomicAtomicInteger *val$subscribed_;
}

- (void)call;

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3)

J2OBJC_FIELD_SETTER(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3, val$subscribed_, JavaUtilConcurrentAtomicAtomicInteger *)

__attribute__((unused)) static void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0);

__attribute__((unused)) static RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0);

@implementation RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest

- (void)noPrematureSubscription {
  RxSubjectsPublishSubject *other = RxSubjectsPublishSubject_create();
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_create();
  JavaUtilConcurrentAtomicAtomicInteger *subscribed = create_JavaUtilConcurrentAtomicAtomicInteger_init();
  [((RxSingle *) nil_chk([((RxSingle *) nil_chk([((RxSingle *) nil_chk(RxSingle_justWithId_(JavaLangInteger_valueOfWithInt_(1)))) doOnSubscribeWithRxFunctionsAction0:create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(subscribed)])) delaySubscriptionWithRxObservable:other])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNotCompleted];
  [ts assertNoErrors];
  [ts assertNoValues];
  OrgJunitAssert_assertEqualsWithNSString_withLong_withLong_(@"Premature subscription", 0, [subscribed get]);
  [((RxSubjectsPublishSubject *) nil_chk(other)) onNextWithId:JavaLangInteger_valueOfWithInt_(1)];
  OrgJunitAssert_assertEqualsWithNSString_withLong_withLong_(@"No subscription", 1, [subscribed get]);
  [ts assertValueWithId:JavaLangInteger_valueOfWithInt_(1)];
  [ts assertNoErrors];
  [ts assertCompleted];
}

- (void)noPrematureSubscriptionToError {
  RxSubjectsPublishSubject *other = RxSubjectsPublishSubject_create();
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_create();
  JavaUtilConcurrentAtomicAtomicInteger *subscribed = create_JavaUtilConcurrentAtomicAtomicInteger_init();
  [((RxSingle *) nil_chk([((RxSingle *) nil_chk([((RxSingle *) nil_chk(RxSingle_errorWithNSException_(create_RxExceptionsTestException_init()))) doOnSubscribeWithRxFunctionsAction0:create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(subscribed)])) delaySubscriptionWithRxObservable:other])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNotCompleted];
  [ts assertNoErrors];
  [ts assertNoValues];
  OrgJunitAssert_assertEqualsWithNSString_withLong_withLong_(@"Premature subscription", 0, [subscribed get]);
  [((RxSubjectsPublishSubject *) nil_chk(other)) onNextWithId:JavaLangInteger_valueOfWithInt_(1)];
  OrgJunitAssert_assertEqualsWithNSString_withLong_withLong_(@"No subscription", 1, [subscribed get]);
  [ts assertNoValues];
  [ts assertNotCompleted];
  [ts assertErrorWithIOSClass:RxExceptionsTestException_class_()];
}

- (void)noSubscriptionIfOtherErrors {
  RxSubjectsPublishSubject *other = RxSubjectsPublishSubject_create();
  RxObserversTestSubscriber *ts = RxObserversTestSubscriber_create();
  JavaUtilConcurrentAtomicAtomicInteger *subscribed = create_JavaUtilConcurrentAtomicAtomicInteger_init();
  [((RxSingle *) nil_chk([((RxSingle *) nil_chk([((RxSingle *) nil_chk(RxSingle_errorWithNSException_(create_RxExceptionsTestException_init()))) doOnSubscribeWithRxFunctionsAction0:create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(subscribed)])) delaySubscriptionWithRxObservable:other])) subscribeWithRxSubscriber:ts];
  [((RxObserversTestSubscriber *) nil_chk(ts)) assertNotCompleted];
  [ts assertNoErrors];
  [ts assertNoValues];
  OrgJunitAssert_assertEqualsWithNSString_withLong_withLong_(@"Premature subscription", 0, [subscribed get]);
  [((RxSubjectsPublishSubject *) nil_chk(other)) onErrorWithNSException:create_RxExceptionsTestException_init()];
  OrgJunitAssert_assertEqualsWithNSString_withLong_withLong_(@"Premature subscription", 0, [subscribed get]);
  [ts assertNoValues];
  [ts assertNotCompleted];
  [ts assertErrorWithIOSClass:RxExceptionsTestException_class_()];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 2, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(noPrematureSubscription);
  methods[1].selector = @selector(noPrematureSubscriptionToError);
  methods[2].selector = @selector(noSubscriptionIfOtherErrors);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$0, (void *)&RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$1, (void *)&RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$2 };
  static const J2ObjcClassInfo _RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest = { "SingleOnSubscribeDelaySubscriptionOtherTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 4, 0, -1, -1, -1, -1, -1 };
  return &_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest;
}

@end

void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_init(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest, init)
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest, init)
}

IOSObjectArray *RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest)

@implementation RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1

- (void)call {
  [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(val$subscribed_)) getAndIncrement];
}

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0 {
  RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$subscribed_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithJavaUtilConcurrentAtomicAtomicInteger:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$subscribed_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LJavaUtilConcurrentAtomicAtomicInteger;", "LRxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest;", "noPrematureSubscription" };
  static const J2ObjcClassInfo _RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 1, -1, 2, -1, -1 };
  return &_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1;
}

@end

void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  JreStrongAssign(&self->val$subscribed_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1, initWithJavaUtilConcurrentAtomicAtomicInteger_, capture$0)
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1 *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$1, initWithJavaUtilConcurrentAtomicAtomicInteger_, capture$0)
}

@implementation RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2

- (void)call {
  [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(val$subscribed_)) getAndIncrement];
}

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0 {
  RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$subscribed_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithJavaUtilConcurrentAtomicAtomicInteger:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$subscribed_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LJavaUtilConcurrentAtomicAtomicInteger;", "LRxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest;", "noPrematureSubscriptionToError" };
  static const J2ObjcClassInfo _RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 1, -1, 2, -1, -1 };
  return &_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2;
}

@end

void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  JreStrongAssign(&self->val$subscribed_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2, initWithJavaUtilConcurrentAtomicAtomicInteger_, capture$0)
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2 *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$2, initWithJavaUtilConcurrentAtomicAtomicInteger_, capture$0)
}

@implementation RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3

- (void)call {
  [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(val$subscribed_)) getAndIncrement];
}

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0 {
  RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$subscribed_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithJavaUtilConcurrentAtomicAtomicInteger:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$subscribed_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LJavaUtilConcurrentAtomicAtomicInteger;", "LRxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest;", "noSubscriptionIfOtherErrors" };
  static const J2ObjcClassInfo _RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 1, -1, 2, -1, -1 };
  return &_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3;
}

@end

void RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  JreStrongAssign(&self->val$subscribed_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 *new_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3, initWithJavaUtilConcurrentAtomicAtomicInteger_, capture$0)
}

RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3 *create_RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3_initWithJavaUtilConcurrentAtomicAtomicInteger_(JavaUtilConcurrentAtomicAtomicInteger *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsSingleOnSubscribeDelaySubscriptionOtherTest_$3, initWithJavaUtilConcurrentAtomicAtomicInteger_, capture$0)
}
