//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/util/RxRingBufferSpscTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsMissingBackpressureException.h"
#include "RxFunctionsAction0.h"
#include "RxInternalUtilRxRingBuffer.h"
#include "RxInternalUtilRxRingBufferBase.h"
#include "RxInternalUtilRxRingBufferSpscTest.h"
#include "RxObserversTestSubscriber.h"
#include "RxProducer.h"
#include "RxScheduler.h"
#include "RxSchedulersSchedulers.h"
#include "RxSubscription.h"
#include "java/io/PrintStream.h"
#include "java/lang/System.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/concurrent/CountDownLatch.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxInternalUtilRxRingBufferSpscTest__Annotations$0();

@interface RxInternalUtilRxRingBufferSpscTest_$1 : NSObject < RxProducer > {
 @public
  RxScheduler_Worker *val$w1_;
  JavaUtilConcurrentCountDownLatch *val$emitLatch_;
  RxInternalUtilRxRingBuffer *val$b_;
  JavaUtilConcurrentAtomicAtomicInteger *val$emit_;
  JavaUtilConcurrentAtomicAtomicInteger *val$poll_;
  JavaUtilConcurrentAtomicAtomicInteger *val$backpressureExceptions_;
}

- (void)requestWithLong:(jlong)n;

- (instancetype)initWithRxScheduler_Worker:(RxScheduler_Worker *)capture$0
      withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$1
            withRxInternalUtilRxRingBuffer:(RxInternalUtilRxRingBuffer *)capture$2
 withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$3
 withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$4
 withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$5;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilRxRingBufferSpscTest_$1)

J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1, val$w1_, RxScheduler_Worker *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1, val$emitLatch_, JavaUtilConcurrentCountDownLatch *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1, val$b_, RxInternalUtilRxRingBuffer *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1, val$emit_, JavaUtilConcurrentAtomicAtomicInteger *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1, val$poll_, JavaUtilConcurrentAtomicAtomicInteger *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1, val$backpressureExceptions_, JavaUtilConcurrentAtomicAtomicInteger *)

__attribute__((unused)) static void RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(RxInternalUtilRxRingBufferSpscTest_$1 *self, RxScheduler_Worker *capture$0, JavaUtilConcurrentCountDownLatch *capture$1, RxInternalUtilRxRingBuffer *capture$2, JavaUtilConcurrentAtomicAtomicInteger *capture$3, JavaUtilConcurrentAtomicAtomicInteger *capture$4, JavaUtilConcurrentAtomicAtomicInteger *capture$5);

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$1 *new_RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(RxScheduler_Worker *capture$0, JavaUtilConcurrentCountDownLatch *capture$1, RxInternalUtilRxRingBuffer *capture$2, JavaUtilConcurrentAtomicAtomicInteger *capture$3, JavaUtilConcurrentAtomicAtomicInteger *capture$4, JavaUtilConcurrentAtomicAtomicInteger *capture$5) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$1 *create_RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(RxScheduler_Worker *capture$0, JavaUtilConcurrentCountDownLatch *capture$1, RxInternalUtilRxRingBuffer *capture$2, JavaUtilConcurrentAtomicAtomicInteger *capture$3, JavaUtilConcurrentAtomicAtomicInteger *capture$4, JavaUtilConcurrentAtomicAtomicInteger *capture$5);

@interface RxInternalUtilRxRingBufferSpscTest_$1_$1 : NSObject < RxFunctionsAction0 > {
 @public
  RxInternalUtilRxRingBufferSpscTest_$1 *this$0_;
  jlong val$n_;
}

- (void)call;

- (instancetype)initWithRxInternalUtilRxRingBufferSpscTest_$1:(RxInternalUtilRxRingBufferSpscTest_$1 *)outer$
                                                     withLong:(jlong)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilRxRingBufferSpscTest_$1_$1)

J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$1_$1, this$0_, RxInternalUtilRxRingBufferSpscTest_$1 *)

__attribute__((unused)) static void RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(RxInternalUtilRxRingBufferSpscTest_$1_$1 *self, RxInternalUtilRxRingBufferSpscTest_$1 *outer$, jlong capture$0);

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$1_$1 *new_RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(RxInternalUtilRxRingBufferSpscTest_$1 *outer$, jlong capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$1_$1 *create_RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(RxInternalUtilRxRingBufferSpscTest_$1 *outer$, jlong capture$0);

@interface RxInternalUtilRxRingBufferSpscTest_$2 : NSObject < RxFunctionsAction0 > {
 @public
  RxObserversTestSubscriber *val$ts_;
  id<RxProducer> val$p_;
}

- (void)call;

- (instancetype)initWithRxObserversTestSubscriber:(RxObserversTestSubscriber *)capture$0
                                   withRxProducer:(id<RxProducer>)capture$1;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilRxRingBufferSpscTest_$2)

J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$2, val$ts_, RxObserversTestSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$2, val$p_, id<RxProducer>)

__attribute__((unused)) static void RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(RxInternalUtilRxRingBufferSpscTest_$2 *self, RxObserversTestSubscriber *capture$0, id<RxProducer> capture$1);

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$2 *new_RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(RxObserversTestSubscriber *capture$0, id<RxProducer> capture$1) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$2 *create_RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(RxObserversTestSubscriber *capture$0, id<RxProducer> capture$1);

@interface RxInternalUtilRxRingBufferSpscTest_$3 : NSObject < RxFunctionsAction0 > {
 @public
  RxInternalUtilRxRingBuffer *val$b_;
  JavaUtilConcurrentAtomicAtomicInteger *val$poll_;
  RxObserversTestSubscriber *val$ts_;
  JavaUtilConcurrentCountDownLatch *val$emitLatch_;
  JavaUtilConcurrentCountDownLatch *val$drainLatch_;
}

- (void)call;

- (instancetype)initWithRxInternalUtilRxRingBuffer:(RxInternalUtilRxRingBuffer *)capture$0
         withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$1
                     withRxObserversTestSubscriber:(RxObserversTestSubscriber *)capture$2
              withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$3
              withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$4;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilRxRingBufferSpscTest_$3)

J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$3, val$b_, RxInternalUtilRxRingBuffer *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$3, val$poll_, JavaUtilConcurrentAtomicAtomicInteger *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$3, val$ts_, RxObserversTestSubscriber *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$3, val$emitLatch_, JavaUtilConcurrentCountDownLatch *)
J2OBJC_FIELD_SETTER(RxInternalUtilRxRingBufferSpscTest_$3, val$drainLatch_, JavaUtilConcurrentCountDownLatch *)

__attribute__((unused)) static void RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(RxInternalUtilRxRingBufferSpscTest_$3 *self, RxInternalUtilRxRingBuffer *capture$0, JavaUtilConcurrentAtomicAtomicInteger *capture$1, RxObserversTestSubscriber *capture$2, JavaUtilConcurrentCountDownLatch *capture$3, JavaUtilConcurrentCountDownLatch *capture$4);

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$3 *new_RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(RxInternalUtilRxRingBuffer *capture$0, JavaUtilConcurrentAtomicAtomicInteger *capture$1, RxObserversTestSubscriber *capture$2, JavaUtilConcurrentCountDownLatch *capture$3, JavaUtilConcurrentCountDownLatch *capture$4) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalUtilRxRingBufferSpscTest_$3 *create_RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(RxInternalUtilRxRingBuffer *capture$0, JavaUtilConcurrentAtomicAtomicInteger *capture$1, RxObserversTestSubscriber *capture$2, JavaUtilConcurrentCountDownLatch *capture$3, JavaUtilConcurrentCountDownLatch *capture$4);

@implementation RxInternalUtilRxRingBufferSpscTest

- (RxInternalUtilRxRingBuffer *)createRingBuffer {
  return RxInternalUtilRxRingBuffer_getSpscInstance();
}

- (void)testConcurrency {
  RxInternalUtilRxRingBuffer *b = [self createRingBuffer];
  JavaUtilConcurrentCountDownLatch *emitLatch = create_JavaUtilConcurrentCountDownLatch_initWithInt_(255);
  JavaUtilConcurrentCountDownLatch *drainLatch = create_JavaUtilConcurrentCountDownLatch_initWithInt_(1);
  RxScheduler_Worker *w1 = [((RxScheduler *) nil_chk(RxSchedulersSchedulers_newThread())) createWorker];
  RxScheduler_Worker *w2 = [((RxScheduler *) nil_chk(RxSchedulersSchedulers_newThread())) createWorker];
  JavaUtilConcurrentAtomicAtomicInteger *emit = create_JavaUtilConcurrentAtomicAtomicInteger_init();
  JavaUtilConcurrentAtomicAtomicInteger *poll = create_JavaUtilConcurrentAtomicAtomicInteger_init();
  JavaUtilConcurrentAtomicAtomicInteger *backpressureExceptions = create_JavaUtilConcurrentAtomicAtomicInteger_init();
  id<RxProducer> p = create_RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(w1, emitLatch, b, emit, poll, backpressureExceptions);
  RxObserversTestSubscriber *ts = create_RxObserversTestSubscriber_init();
  [((RxScheduler_Worker *) nil_chk(w1)) scheduleWithRxFunctionsAction0:create_RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(ts, p)];
  id<RxFunctionsAction0> drainer = create_RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(b, poll, ts, emitLatch, drainLatch);
  [((RxScheduler_Worker *) nil_chk(w2)) scheduleWithRxFunctionsAction0:drainer];
  [emitLatch await];
  [drainLatch await];
  [w2 unsubscribe];
  [w1 unsubscribe];
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:JreStrcat("$I$I", @"emit: ", [emit get], @" poll: ", [poll get])];
  OrgJunitAssert_assertEqualsWithLong_withLong_(0, [backpressureExceptions get]);
  OrgJunitAssert_assertEqualsWithLong_withLong_([emit get], [poll get]);
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilRxRingBufferSpscTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalUtilRxRingBuffer;", 0x4, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, 0, -1, 1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(createRingBuffer);
  methods[1].selector = @selector(testConcurrency);
  methods[2].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LJavaLangInterruptedException;", (void *)&RxInternalUtilRxRingBufferSpscTest__Annotations$0 };
  static const J2ObjcClassInfo _RxInternalUtilRxRingBufferSpscTest = { "RxRingBufferSpscTest", "rx.internal.util", ptrTable, methods, NULL, 7, 0x1, 3, 0, -1, -1, -1, -1, -1 };
  return &_RxInternalUtilRxRingBufferSpscTest;
}

@end

void RxInternalUtilRxRingBufferSpscTest_init(RxInternalUtilRxRingBufferSpscTest *self) {
  RxInternalUtilRxRingBufferBase_init(self);
}

RxInternalUtilRxRingBufferSpscTest *new_RxInternalUtilRxRingBufferSpscTest_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilRxRingBufferSpscTest, init)
}

RxInternalUtilRxRingBufferSpscTest *create_RxInternalUtilRxRingBufferSpscTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilRxRingBufferSpscTest, init)
}

IOSObjectArray *RxInternalUtilRxRingBufferSpscTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilRxRingBufferSpscTest)

@implementation RxInternalUtilRxRingBufferSpscTest_$1

- (void)requestWithLong:(jlong)n {
  [((RxScheduler_Worker *) nil_chk(val$w1_)) scheduleWithRxFunctionsAction0:create_RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(self, n)];
}

- (instancetype)initWithRxScheduler_Worker:(RxScheduler_Worker *)capture$0
      withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$1
            withRxInternalUtilRxRingBuffer:(RxInternalUtilRxRingBuffer *)capture$2
 withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$3
 withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$4
 withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$5 {
  RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(self, capture$0, capture$1, capture$2, capture$3, capture$4, capture$5);
  return self;
}

- (void)dealloc {
  RELEASE_(val$w1_);
  RELEASE_(val$emitLatch_);
  RELEASE_(val$b_);
  RELEASE_(val$emit_);
  RELEASE_(val$poll_);
  RELEASE_(val$backpressureExceptions_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(requestWithLong:);
  methods[1].selector = @selector(initWithRxScheduler_Worker:withJavaUtilConcurrentCountDownLatch:withRxInternalUtilRxRingBuffer:withJavaUtilConcurrentAtomicAtomicInteger:withJavaUtilConcurrentAtomicAtomicInteger:withJavaUtilConcurrentAtomicAtomicInteger:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$w1_", "LRxScheduler_Worker;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$emitLatch_", "LJavaUtilConcurrentCountDownLatch;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$b_", "LRxInternalUtilRxRingBuffer;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$emit_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$poll_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$backpressureExceptions_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "request", "J", "LRxScheduler_Worker;LJavaUtilConcurrentCountDownLatch;LRxInternalUtilRxRingBuffer;LJavaUtilConcurrentAtomicAtomicInteger;LJavaUtilConcurrentAtomicAtomicInteger;LJavaUtilConcurrentAtomicAtomicInteger;", "LRxInternalUtilRxRingBufferSpscTest;", "testConcurrency" };
  static const J2ObjcClassInfo _RxInternalUtilRxRingBufferSpscTest_$1 = { "", "rx.internal.util", ptrTable, methods, fields, 7, 0x8008, 2, 6, 3, -1, 4, -1, -1 };
  return &_RxInternalUtilRxRingBufferSpscTest_$1;
}

@end

void RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(RxInternalUtilRxRingBufferSpscTest_$1 *self, RxScheduler_Worker *capture$0, JavaUtilConcurrentCountDownLatch *capture$1, RxInternalUtilRxRingBuffer *capture$2, JavaUtilConcurrentAtomicAtomicInteger *capture$3, JavaUtilConcurrentAtomicAtomicInteger *capture$4, JavaUtilConcurrentAtomicAtomicInteger *capture$5) {
  JreStrongAssign(&self->val$w1_, capture$0);
  JreStrongAssign(&self->val$emitLatch_, capture$1);
  JreStrongAssign(&self->val$b_, capture$2);
  JreStrongAssign(&self->val$emit_, capture$3);
  JreStrongAssign(&self->val$poll_, capture$4);
  JreStrongAssign(&self->val$backpressureExceptions_, capture$5);
  NSObject_init(self);
}

RxInternalUtilRxRingBufferSpscTest_$1 *new_RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(RxScheduler_Worker *capture$0, JavaUtilConcurrentCountDownLatch *capture$1, RxInternalUtilRxRingBuffer *capture$2, JavaUtilConcurrentAtomicAtomicInteger *capture$3, JavaUtilConcurrentAtomicAtomicInteger *capture$4, JavaUtilConcurrentAtomicAtomicInteger *capture$5) {
  J2OBJC_NEW_IMPL(RxInternalUtilRxRingBufferSpscTest_$1, initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_, capture$0, capture$1, capture$2, capture$3, capture$4, capture$5)
}

RxInternalUtilRxRingBufferSpscTest_$1 *create_RxInternalUtilRxRingBufferSpscTest_$1_initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_(RxScheduler_Worker *capture$0, JavaUtilConcurrentCountDownLatch *capture$1, RxInternalUtilRxRingBuffer *capture$2, JavaUtilConcurrentAtomicAtomicInteger *capture$3, JavaUtilConcurrentAtomicAtomicInteger *capture$4, JavaUtilConcurrentAtomicAtomicInteger *capture$5) {
  J2OBJC_CREATE_IMPL(RxInternalUtilRxRingBufferSpscTest_$1, initWithRxScheduler_Worker_withJavaUtilConcurrentCountDownLatch_withRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_withJavaUtilConcurrentAtomicAtomicInteger_, capture$0, capture$1, capture$2, capture$3, capture$4, capture$5)
}

@implementation RxInternalUtilRxRingBufferSpscTest_$1_$1

- (void)call {
  if ([((JavaUtilConcurrentCountDownLatch *) nil_chk(this$0_->val$emitLatch_)) getCount] == 0) {
    return;
  }
  for (jint i = 0; i < val$n_; i++) {
    @try {
      [((RxInternalUtilRxRingBuffer *) nil_chk(this$0_->val$b_)) onNextWithId:@"one"];
      [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(this$0_->val$emit_)) incrementAndGet];
    }
    @catch (RxExceptionsMissingBackpressureException *e) {
      [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:JreStrcat("$I$J$I$I", @"BackpressureException => item: ", i, @"  requested: ", val$n_, @" emit: ", [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(this$0_->val$emit_)) get], @"  poll: ", [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(this$0_->val$poll_)) get])];
      [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(this$0_->val$backpressureExceptions_)) incrementAndGet];
    }
  }
  [this$0_->val$emitLatch_ countDown];
}

- (instancetype)initWithRxInternalUtilRxRingBufferSpscTest_$1:(RxInternalUtilRxRingBufferSpscTest_$1 *)outer$
                                                     withLong:(jlong)capture$0 {
  RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(self, outer$, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(this$0_);
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
  methods[1].selector = @selector(initWithRxInternalUtilRxRingBufferSpscTest_$1:withLong:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "this$0_", "LRxInternalUtilRxRingBufferSpscTest_$1;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$n_", "J", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalUtilRxRingBufferSpscTest_$1;J", "LRxInternalUtilRxRingBufferSpscTest_$1;", "requestWithLong:" };
  static const J2ObjcClassInfo _RxInternalUtilRxRingBufferSpscTest_$1_$1 = { "", "rx.internal.util", ptrTable, methods, fields, 7, 0x8008, 2, 2, 1, -1, 2, -1, -1 };
  return &_RxInternalUtilRxRingBufferSpscTest_$1_$1;
}

@end

void RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(RxInternalUtilRxRingBufferSpscTest_$1_$1 *self, RxInternalUtilRxRingBufferSpscTest_$1 *outer$, jlong capture$0) {
  JreStrongAssign(&self->this$0_, outer$);
  self->val$n_ = capture$0;
  NSObject_init(self);
}

RxInternalUtilRxRingBufferSpscTest_$1_$1 *new_RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(RxInternalUtilRxRingBufferSpscTest_$1 *outer$, jlong capture$0) {
  J2OBJC_NEW_IMPL(RxInternalUtilRxRingBufferSpscTest_$1_$1, initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_, outer$, capture$0)
}

RxInternalUtilRxRingBufferSpscTest_$1_$1 *create_RxInternalUtilRxRingBufferSpscTest_$1_$1_initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_(RxInternalUtilRxRingBufferSpscTest_$1 *outer$, jlong capture$0) {
  J2OBJC_CREATE_IMPL(RxInternalUtilRxRingBufferSpscTest_$1_$1, initWithRxInternalUtilRxRingBufferSpscTest_$1_withLong_, outer$, capture$0)
}

@implementation RxInternalUtilRxRingBufferSpscTest_$2

- (void)call {
  [((RxObserversTestSubscriber *) nil_chk(val$ts_)) requestMoreWithLong:JreLoadStatic(RxInternalUtilRxRingBuffer, SIZE)];
  [val$ts_ setProducerWithRxProducer:val$p_];
}

- (instancetype)initWithRxObserversTestSubscriber:(RxObserversTestSubscriber *)capture$0
                                   withRxProducer:(id<RxProducer>)capture$1 {
  RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(self, capture$0, capture$1);
  return self;
}

- (void)dealloc {
  RELEASE_(val$ts_);
  RELEASE_(val$p_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithRxObserversTestSubscriber:withRxProducer:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$ts_", "LRxObserversTestSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 2, -1 },
    { "val$p_", "LRxProducer;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxObserversTestSubscriber;LRxProducer;", "(Lrx/observers/TestSubscriber<Ljava/lang/String;>;Lrx/Producer;)V", "Lrx/observers/TestSubscriber<Ljava/lang/String;>;", "LRxInternalUtilRxRingBufferSpscTest;", "testConcurrency" };
  static const J2ObjcClassInfo _RxInternalUtilRxRingBufferSpscTest_$2 = { "", "rx.internal.util", ptrTable, methods, fields, 7, 0x8008, 2, 2, 3, -1, 4, -1, -1 };
  return &_RxInternalUtilRxRingBufferSpscTest_$2;
}

@end

void RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(RxInternalUtilRxRingBufferSpscTest_$2 *self, RxObserversTestSubscriber *capture$0, id<RxProducer> capture$1) {
  JreStrongAssign(&self->val$ts_, capture$0);
  JreStrongAssign(&self->val$p_, capture$1);
  NSObject_init(self);
}

RxInternalUtilRxRingBufferSpscTest_$2 *new_RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(RxObserversTestSubscriber *capture$0, id<RxProducer> capture$1) {
  J2OBJC_NEW_IMPL(RxInternalUtilRxRingBufferSpscTest_$2, initWithRxObserversTestSubscriber_withRxProducer_, capture$0, capture$1)
}

RxInternalUtilRxRingBufferSpscTest_$2 *create_RxInternalUtilRxRingBufferSpscTest_$2_initWithRxObserversTestSubscriber_withRxProducer_(RxObserversTestSubscriber *capture$0, id<RxProducer> capture$1) {
  J2OBJC_CREATE_IMPL(RxInternalUtilRxRingBufferSpscTest_$2, initWithRxObserversTestSubscriber_withRxProducer_, capture$0, capture$1)
}

@implementation RxInternalUtilRxRingBufferSpscTest_$3

- (void)call {
  jint emitted = 0;
  jint shutdownCount = 0;
  while (true) {
    id o = [((RxInternalUtilRxRingBuffer *) nil_chk(val$b_)) poll];
    if (o != nil) {
      emitted++;
      [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(val$poll_)) incrementAndGet];
    }
    else {
      if (emitted > 0) {
        [((RxObserversTestSubscriber *) nil_chk(val$ts_)) requestMoreWithLong:emitted];
        emitted = 0;
      }
      else {
        if ([((JavaUtilConcurrentCountDownLatch *) nil_chk(val$emitLatch_)) getCount] == 0) {
          shutdownCount++;
          if (shutdownCount > 5) {
            [((JavaUtilConcurrentCountDownLatch *) nil_chk(val$drainLatch_)) countDown];
            return;
          }
        }
      }
    }
  }
}

- (instancetype)initWithRxInternalUtilRxRingBuffer:(RxInternalUtilRxRingBuffer *)capture$0
         withJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$1
                     withRxObserversTestSubscriber:(RxObserversTestSubscriber *)capture$2
              withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$3
              withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$4 {
  RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(self, capture$0, capture$1, capture$2, capture$3, capture$4);
  return self;
}

- (void)dealloc {
  RELEASE_(val$b_);
  RELEASE_(val$poll_);
  RELEASE_(val$ts_);
  RELEASE_(val$emitLatch_);
  RELEASE_(val$drainLatch_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, 1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithRxInternalUtilRxRingBuffer:withJavaUtilConcurrentAtomicAtomicInteger:withRxObserversTestSubscriber:withJavaUtilConcurrentCountDownLatch:withJavaUtilConcurrentCountDownLatch:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$b_", "LRxInternalUtilRxRingBuffer;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$poll_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$ts_", "LRxObserversTestSubscriber;", .constantValue.asLong = 0, 0x1012, -1, -1, 2, -1 },
    { "val$emitLatch_", "LJavaUtilConcurrentCountDownLatch;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$drainLatch_", "LJavaUtilConcurrentCountDownLatch;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LRxInternalUtilRxRingBuffer;LJavaUtilConcurrentAtomicAtomicInteger;LRxObserversTestSubscriber;LJavaUtilConcurrentCountDownLatch;LJavaUtilConcurrentCountDownLatch;", "(Lrx/internal/util/RxRingBuffer;Ljava/util/concurrent/atomic/AtomicInteger;Lrx/observers/TestSubscriber<Ljava/lang/String;>;Ljava/util/concurrent/CountDownLatch;Ljava/util/concurrent/CountDownLatch;)V", "Lrx/observers/TestSubscriber<Ljava/lang/String;>;", "LRxInternalUtilRxRingBufferSpscTest;", "testConcurrency" };
  static const J2ObjcClassInfo _RxInternalUtilRxRingBufferSpscTest_$3 = { "", "rx.internal.util", ptrTable, methods, fields, 7, 0x8008, 2, 5, 3, -1, 4, -1, -1 };
  return &_RxInternalUtilRxRingBufferSpscTest_$3;
}

@end

void RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(RxInternalUtilRxRingBufferSpscTest_$3 *self, RxInternalUtilRxRingBuffer *capture$0, JavaUtilConcurrentAtomicAtomicInteger *capture$1, RxObserversTestSubscriber *capture$2, JavaUtilConcurrentCountDownLatch *capture$3, JavaUtilConcurrentCountDownLatch *capture$4) {
  JreStrongAssign(&self->val$b_, capture$0);
  JreStrongAssign(&self->val$poll_, capture$1);
  JreStrongAssign(&self->val$ts_, capture$2);
  JreStrongAssign(&self->val$emitLatch_, capture$3);
  JreStrongAssign(&self->val$drainLatch_, capture$4);
  NSObject_init(self);
}

RxInternalUtilRxRingBufferSpscTest_$3 *new_RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(RxInternalUtilRxRingBuffer *capture$0, JavaUtilConcurrentAtomicAtomicInteger *capture$1, RxObserversTestSubscriber *capture$2, JavaUtilConcurrentCountDownLatch *capture$3, JavaUtilConcurrentCountDownLatch *capture$4) {
  J2OBJC_NEW_IMPL(RxInternalUtilRxRingBufferSpscTest_$3, initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_, capture$0, capture$1, capture$2, capture$3, capture$4)
}

RxInternalUtilRxRingBufferSpscTest_$3 *create_RxInternalUtilRxRingBufferSpscTest_$3_initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_(RxInternalUtilRxRingBuffer *capture$0, JavaUtilConcurrentAtomicAtomicInteger *capture$1, RxObserversTestSubscriber *capture$2, JavaUtilConcurrentCountDownLatch *capture$3, JavaUtilConcurrentCountDownLatch *capture$4) {
  J2OBJC_CREATE_IMPL(RxInternalUtilRxRingBufferSpscTest_$3, initWithRxInternalUtilRxRingBuffer_withJavaUtilConcurrentAtomicAtomicInteger_withRxObserversTestSubscriber_withJavaUtilConcurrentCountDownLatch_withJavaUtilConcurrentCountDownLatch_, capture$0, capture$1, capture$2, capture$3, capture$4)
}
