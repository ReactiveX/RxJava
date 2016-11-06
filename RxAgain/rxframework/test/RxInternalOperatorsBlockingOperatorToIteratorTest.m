//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/operators/BlockingOperatorToIteratorTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsTestException.h"
#include "RxInternalOperatorsBlockingOperatorToIterator.h"
#include "RxInternalOperatorsBlockingOperatorToIteratorTest.h"
#include "RxInternalUtilRxRingBuffer.h"
#include "RxObservable.h"
#include "RxObservablesBlockingObservable.h"
#include "RxSubscriber.h"
#include "RxTestUtil.h"
#include "java/io/PrintStream.h"
#include "java/lang/Boolean.h"
#include "java/lang/Integer.h"
#include "java/lang/Iterable.h"
#include "java/lang/Math.h"
#include "java/lang/System.h"
#include "java/lang/UnsupportedOperationException.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/Iterator.h"
#include "java/util/Spliterator.h"
#include "java/util/function/Consumer.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$3();

__attribute__((unused)) static IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$4();

@interface RxInternalOperatorsBlockingOperatorToIteratorTest_$1 : NSObject < RxObservable_OnSubscribe >

- (void)callWithId:(RxSubscriber *)observer;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsBlockingOperatorToIteratorTest_$1)

__attribute__((unused)) static void RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init(RxInternalOperatorsBlockingOperatorToIteratorTest_$1 *self);

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToIteratorTest_$1 *new_RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToIteratorTest_$1 *create_RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init();

@interface RxInternalOperatorsBlockingOperatorToIteratorTest_$2 : NSObject < RxObservable_OnSubscribe >

- (void)callWithId:(RxSubscriber *)subscriber;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsBlockingOperatorToIteratorTest_$2)

__attribute__((unused)) static void RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init(RxInternalOperatorsBlockingOperatorToIteratorTest_$2 *self);

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToIteratorTest_$2 *new_RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToIteratorTest_$2 *create_RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init();

@interface RxInternalOperatorsBlockingOperatorToIteratorTest_$3 : NSObject < JavaLangIterable > {
 @public
  RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *val$src_;
}

- (id<JavaUtilIterator>)iterator;

- (instancetype)initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter:(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsBlockingOperatorToIteratorTest_$3)

J2OBJC_FIELD_SETTER(RxInternalOperatorsBlockingOperatorToIteratorTest_$3, val$src_, RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *)

__attribute__((unused)) static void RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(RxInternalOperatorsBlockingOperatorToIteratorTest_$3 *self, RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *capture$0);

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToIteratorTest_$3 *new_RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalOperatorsBlockingOperatorToIteratorTest_$3 *create_RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *capture$0);

@implementation RxInternalOperatorsBlockingOperatorToIteratorTest

- (void)constructorShouldBePrivate {
  RxTestUtil_checkUtilityClassWithIOSClass_(RxInternalOperatorsBlockingOperatorToIterator_class_());
}

- (void)testToIterator {
  RxObservable *obs = RxObservable_justWithId_withId_withId_(@"one", @"two", @"three");
  id<JavaUtilIterator> it = RxInternalOperatorsBlockingOperatorToIterator_toIteratorWithRxObservable_(obs);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangBoolean_valueOfWithBoolean_(true), JavaLangBoolean_valueOfWithBoolean_([((id<JavaUtilIterator>) nil_chk(it)) hasNext]));
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [it next]);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangBoolean_valueOfWithBoolean_(true), JavaLangBoolean_valueOfWithBoolean_([it hasNext]));
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [it next]);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangBoolean_valueOfWithBoolean_(true), JavaLangBoolean_valueOfWithBoolean_([it hasNext]));
  OrgJunitAssert_assertEqualsWithId_withId_(@"three", [it next]);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangBoolean_valueOfWithBoolean_(false), JavaLangBoolean_valueOfWithBoolean_([it hasNext]));
}

- (void)testToIteratorWithException {
  RxObservable *obs = RxObservable_createWithRxObservable_OnSubscribe_(create_RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init());
  id<JavaUtilIterator> it = RxInternalOperatorsBlockingOperatorToIterator_toIteratorWithRxObservable_(obs);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangBoolean_valueOfWithBoolean_(true), JavaLangBoolean_valueOfWithBoolean_([((id<JavaUtilIterator>) nil_chk(it)) hasNext]));
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [it next]);
  OrgJunitAssert_assertEqualsWithId_withId_(JavaLangBoolean_valueOfWithBoolean_(true), JavaLangBoolean_valueOfWithBoolean_([it hasNext]));
  [it next];
}

- (void)testExceptionThrownFromOnSubscribe {
  id<JavaLangIterable> strings = [((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_createWithRxObservable_OnSubscribe_(create_RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init()))) toBlocking])) toIterable];
  for (NSString * __strong string in nil_chk(strings)) {
    [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:string];
  }
}

- (void)testIteratorExertBackpressure {
  RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *src = create_RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_init();
  RxObservable *obs = RxObservable_fromWithJavaLangIterable_(create_RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(src));
  id<JavaUtilIterator> it = RxInternalOperatorsBlockingOperatorToIterator_toIteratorWithRxObservable_(obs);
  while ([((id<JavaUtilIterator>) nil_chk(it)) hasNext]) {
    jint i = [((JavaLangInteger *) nil_chk([it next])) intValue];
    jint expected = i - (i % JreLoadStatic(RxInternalOperatorsBlockingOperatorToIterator_SubscriberIterator, LIMIT)) + JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE);
    expected = JavaLangMath_minWithInt_withInt_(expected, JreLoadStatic(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter, MAX));
    OrgJunitAssert_assertEqualsWithLong_withLong_(expected, src->count_);
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToIteratorTest_init(self);
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
  methods[0].selector = @selector(constructorShouldBePrivate);
  methods[1].selector = @selector(testToIterator);
  methods[2].selector = @selector(testToIteratorWithException);
  methods[3].selector = @selector(testExceptionThrownFromOnSubscribe);
  methods[4].selector = @selector(testIteratorExertBackpressure);
  methods[5].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$0, (void *)&RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$1, (void *)&RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$2, (void *)&RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$3, (void *)&RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$4, "LRxInternalOperatorsBlockingOperatorToIteratorTest_Counter;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToIteratorTest = { "BlockingOperatorToIteratorTest", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x1, 6, 0, -1, 5, -1, -1, -1 };
  return &_RxInternalOperatorsBlockingOperatorToIteratorTest;
}

@end

void RxInternalOperatorsBlockingOperatorToIteratorTest_init(RxInternalOperatorsBlockingOperatorToIteratorTest *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToIteratorTest *new_RxInternalOperatorsBlockingOperatorToIteratorTest_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest, init)
}

RxInternalOperatorsBlockingOperatorToIteratorTest *create_RxInternalOperatorsBlockingOperatorToIteratorTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest, init)
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(RxExceptionsTestException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(RxExceptionsTestException_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxInternalOperatorsBlockingOperatorToIteratorTest__Annotations$4() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsBlockingOperatorToIteratorTest)

J2OBJC_INITIALIZED_DEFN(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter)

jint RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_MAX;

@implementation RxInternalOperatorsBlockingOperatorToIteratorTest_Counter

- (jboolean)hasNext {
  return count_ < RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_MAX;
}

- (JavaLangInteger *)next {
  return JavaLangInteger_valueOfWithInt_(++count_);
}

- (void)remove {
  @throw create_JavaLangUnsupportedOperationException_init();
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)forEachRemainingWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)arg0 {
  JavaUtilIterator_forEachRemainingWithJavaUtilFunctionConsumer_(self, arg0);
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LJavaLangInteger;", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(hasNext);
  methods[1].selector = @selector(next);
  methods[2].selector = @selector(remove);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "MAX", "I", .constantValue.asLong = 0, 0x18, -1, 0, -1, -1 },
    { "count_", "I", .constantValue.asLong = 0, 0x1, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { &RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_MAX, "LRxInternalOperatorsBlockingOperatorToIteratorTest;", "Ljava/lang/Object;Ljava/util/Iterator<Ljava/lang/Integer;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToIteratorTest_Counter = { "Counter", "rx.internal.operators", ptrTable, methods, fields, 7, 0x19, 4, 2, 1, -1, -1, 2, -1 };
  return &_RxInternalOperatorsBlockingOperatorToIteratorTest_Counter;
}

+ (void)initialize {
  if (self == [RxInternalOperatorsBlockingOperatorToIteratorTest_Counter class]) {
    RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_MAX = 5 * JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE);
    J2OBJC_SET_INITIALIZED(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter)
  }
}

@end

void RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_init(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *new_RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter, init)
}

RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *create_RxInternalOperatorsBlockingOperatorToIteratorTest_Counter_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter)

@implementation RxInternalOperatorsBlockingOperatorToIteratorTest_$1

- (void)callWithId:(RxSubscriber *)observer {
  [((RxSubscriber *) nil_chk(observer)) onNextWithId:@"one"];
  [observer onErrorWithNSException:create_RxExceptionsTestException_init()];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init(self);
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
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/String;>;)V", "LRxInternalOperatorsBlockingOperatorToIteratorTest;", "testToIteratorWithException", "Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/lang/String;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToIteratorTest_$1 = { "", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x8008, 2, 0, 3, -1, 4, 5, -1 };
  return &_RxInternalOperatorsBlockingOperatorToIteratorTest_$1;
}

@end

void RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init(RxInternalOperatorsBlockingOperatorToIteratorTest_$1 *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToIteratorTest_$1 *new_RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_$1, init)
}

RxInternalOperatorsBlockingOperatorToIteratorTest_$1 *create_RxInternalOperatorsBlockingOperatorToIteratorTest_$1_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_$1, init)
}

@implementation RxInternalOperatorsBlockingOperatorToIteratorTest_$2

- (void)callWithId:(RxSubscriber *)subscriber {
  @throw create_RxExceptionsTestException_initWithNSString_(@"intentional");
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init(self);
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
  static const void *ptrTable[] = { "call", "LRxSubscriber;", "(Lrx/Subscriber<-Ljava/lang/String;>;)V", "LRxInternalOperatorsBlockingOperatorToIteratorTest;", "testExceptionThrownFromOnSubscribe", "Ljava/lang/Object;Lrx/Observable$OnSubscribe<Ljava/lang/String;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToIteratorTest_$2 = { "", "rx.internal.operators", ptrTable, methods, NULL, 7, 0x8008, 2, 0, 3, -1, 4, 5, -1 };
  return &_RxInternalOperatorsBlockingOperatorToIteratorTest_$2;
}

@end

void RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init(RxInternalOperatorsBlockingOperatorToIteratorTest_$2 *self) {
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToIteratorTest_$2 *new_RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init() {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_$2, init)
}

RxInternalOperatorsBlockingOperatorToIteratorTest_$2 *create_RxInternalOperatorsBlockingOperatorToIteratorTest_$2_init() {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_$2, init)
}

@implementation RxInternalOperatorsBlockingOperatorToIteratorTest_$3

- (id<JavaUtilIterator>)iterator {
  return val$src_;
}

- (instancetype)initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter:(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *)capture$0 {
  RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(self, capture$0);
  return self;
}

- (void)forEachWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)arg0 {
  JavaLangIterable_forEachWithJavaUtilFunctionConsumer_(self, arg0);
}

- (id<JavaUtilSpliterator>)spliterator {
  return JavaLangIterable_spliterator(self);
}

- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len {
  return JreDefaultFastEnumeration(self, state, stackbuf, len);
}

- (void)dealloc {
  RELEASE_(val$src_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LJavaUtilIterator;", 0x1, -1, -1, -1, 0, -1, -1 },
    { NULL, NULL, 0x0, -1, 1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(iterator);
  methods[1].selector = @selector(initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$src_", "LRxInternalOperatorsBlockingOperatorToIteratorTest_Counter;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "()Ljava/util/Iterator<Ljava/lang/Integer;>;", "LRxInternalOperatorsBlockingOperatorToIteratorTest_Counter;", "LRxInternalOperatorsBlockingOperatorToIteratorTest;", "testIteratorExertBackpressure", "Ljava/lang/Object;Ljava/lang/Iterable<Ljava/lang/Integer;>;" };
  static const J2ObjcClassInfo _RxInternalOperatorsBlockingOperatorToIteratorTest_$3 = { "", "rx.internal.operators", ptrTable, methods, fields, 7, 0x8008, 2, 1, 2, -1, 3, 4, -1 };
  return &_RxInternalOperatorsBlockingOperatorToIteratorTest_$3;
}

@end

void RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(RxInternalOperatorsBlockingOperatorToIteratorTest_$3 *self, RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *capture$0) {
  JreStrongAssign(&self->val$src_, capture$0);
  NSObject_init(self);
}

RxInternalOperatorsBlockingOperatorToIteratorTest_$3 *new_RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *capture$0) {
  J2OBJC_NEW_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_$3, initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_, capture$0)
}

RxInternalOperatorsBlockingOperatorToIteratorTest_$3 *create_RxInternalOperatorsBlockingOperatorToIteratorTest_$3_initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_(RxInternalOperatorsBlockingOperatorToIteratorTest_Counter *capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalOperatorsBlockingOperatorToIteratorTest_$3, initWithRxInternalOperatorsBlockingOperatorToIteratorTest_Counter_, capture$0)
}
