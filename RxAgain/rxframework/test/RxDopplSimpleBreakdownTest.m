//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/doppl/SimpleBreakdownTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxDopplSimpleBreakdownTest.h"
#include "RxObservable.h"
#include "RxSubscriber.h"
#include "RxSubscription.h"
#include "java/io/PrintStream.h"
#include "java/lang/Integer.h"
#include "java/lang/System.h"
#include "java/lang/annotation/Annotation.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxDopplSimpleBreakdownTest__Annotations$0();

@interface RxDopplSimpleBreakdownTest_$1 : RxSubscriber

- (void)onCompleted;

- (void)onErrorWithNSException:(NSException *)e;

- (void)onNextWithId:(JavaLangInteger *)integer;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxDopplSimpleBreakdownTest_$1)

__attribute__((unused)) static void RxDopplSimpleBreakdownTest_$1_init(RxDopplSimpleBreakdownTest_$1 *self);

__attribute__((unused)) static RxDopplSimpleBreakdownTest_$1 *new_RxDopplSimpleBreakdownTest_$1_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxDopplSimpleBreakdownTest_$1 *create_RxDopplSimpleBreakdownTest_$1_init();

@implementation RxDopplSimpleBreakdownTest

- (void)longRunning {
  RxSubscriber *ts = create_RxDopplSimpleBreakdownTest_$1_init();
  jint n = 1000 * 5;
  [((RxObservable *) nil_chk(RxObservable_rangeWithInt_withInt_(1, n))) subscribeWithRxSubscriber:ts];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxDopplSimpleBreakdownTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(longRunning);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxDopplSimpleBreakdownTest__Annotations$0 };
  static const J2ObjcClassInfo _RxDopplSimpleBreakdownTest = { "SimpleBreakdownTest", "rx.doppl", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_RxDopplSimpleBreakdownTest;
}

@end

void RxDopplSimpleBreakdownTest_init(RxDopplSimpleBreakdownTest *self) {
  NSObject_init(self);
}

RxDopplSimpleBreakdownTest *new_RxDopplSimpleBreakdownTest_init() {
  J2OBJC_NEW_IMPL(RxDopplSimpleBreakdownTest, init)
}

RxDopplSimpleBreakdownTest *create_RxDopplSimpleBreakdownTest_init() {
  J2OBJC_CREATE_IMPL(RxDopplSimpleBreakdownTest, init)
}

IOSObjectArray *RxDopplSimpleBreakdownTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxDopplSimpleBreakdownTest)

@implementation RxDopplSimpleBreakdownTest_$1

- (void)onCompleted {
}

- (void)onErrorWithNSException:(NSException *)e {
}

- (void)onNextWithId:(JavaLangInteger *)integer {
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:JreStrcat("$@", @"Mine: ", integer)];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxDopplSimpleBreakdownTest_$1_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  JreCheckFinalize(self, [RxDopplSimpleBreakdownTest_$1 class]);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 3, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(onCompleted);
  methods[1].selector = @selector(onErrorWithNSException:);
  methods[2].selector = @selector(onNextWithId:);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "onError", "LNSException;", "onNext", "LJavaLangInteger;", "LRxDopplSimpleBreakdownTest;", "longRunning", "Lrx/Subscriber<Ljava/lang/Integer;>;" };
  static const J2ObjcClassInfo _RxDopplSimpleBreakdownTest_$1 = { "", "rx.doppl", ptrTable, methods, NULL, 7, 0x8008, 4, 0, 4, -1, 5, 6, -1 };
  return &_RxDopplSimpleBreakdownTest_$1;
}

@end

void RxDopplSimpleBreakdownTest_$1_init(RxDopplSimpleBreakdownTest_$1 *self) {
  RxSubscriber_init(self);
}

RxDopplSimpleBreakdownTest_$1 *new_RxDopplSimpleBreakdownTest_$1_init() {
  J2OBJC_NEW_IMPL(RxDopplSimpleBreakdownTest_$1, init)
}

RxDopplSimpleBreakdownTest_$1 *create_RxDopplSimpleBreakdownTest_$1_init() {
  J2OBJC_CREATE_IMPL(RxDopplSimpleBreakdownTest_$1, init)
}
