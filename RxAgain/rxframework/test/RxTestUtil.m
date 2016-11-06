//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/TestUtil.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsAction0.h"
#include "RxScheduler.h"
#include "RxSchedulersSchedulers.h"
#include "RxSubscription.h"
#include "RxTestUtil.h"
#include "java/lang/Enum.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/InterruptedException.h"
#include "java/util/ArrayList.h"
#include "java/util/List.h"
#include "java/util/concurrent/CountDownLatch.h"
#include "java/util/concurrent/TimeUnit.h"
#include "java/util/concurrent/TimeoutException.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"

__attribute__((unused)) static void RxTestUtil_initWithNSString_withInt_(RxTestUtil *self, NSString *__name, jint __ordinal);

@interface RxTestUtil_$1 : NSObject < RxFunctionsAction0 > {
 @public
  JavaUtilConcurrentAtomicAtomicInteger *val$counter_;
  id<RxFunctionsAction0> val$r2_;
  IOSObjectArray *val$errors_;
  JavaUtilConcurrentCountDownLatch *val$cdl_;
}

- (void)call;

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0
                                       withRxFunctionsAction0:(id<RxFunctionsAction0>)capture$1
                                         withNSExceptionArray:(IOSObjectArray *)capture$2
                         withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$3;

@end

J2OBJC_EMPTY_STATIC_INIT(RxTestUtil_$1)

J2OBJC_FIELD_SETTER(RxTestUtil_$1, val$counter_, JavaUtilConcurrentAtomicAtomicInteger *)
J2OBJC_FIELD_SETTER(RxTestUtil_$1, val$r2_, id<RxFunctionsAction0>)
J2OBJC_FIELD_SETTER(RxTestUtil_$1, val$errors_, IOSObjectArray *)
J2OBJC_FIELD_SETTER(RxTestUtil_$1, val$cdl_, JavaUtilConcurrentCountDownLatch *)

__attribute__((unused)) static void RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(RxTestUtil_$1 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0, id<RxFunctionsAction0> capture$1, IOSObjectArray *capture$2, JavaUtilConcurrentCountDownLatch *capture$3);

__attribute__((unused)) static RxTestUtil_$1 *new_RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(JavaUtilConcurrentAtomicAtomicInteger *capture$0, id<RxFunctionsAction0> capture$1, IOSObjectArray *capture$2, JavaUtilConcurrentCountDownLatch *capture$3) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxTestUtil_$1 *create_RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(JavaUtilConcurrentAtomicAtomicInteger *capture$0, id<RxFunctionsAction0> capture$1, IOSObjectArray *capture$2, JavaUtilConcurrentCountDownLatch *capture$3);

RxTestUtil *RxTestUtil_values_[0];

@implementation RxTestUtil

+ (void)checkUtilityClassWithIOSClass:(IOSClass *)clazz {
  RxTestUtil_checkUtilityClassWithIOSClass_(clazz);
}

+ (void)raceWithRxFunctionsAction0:(id<RxFunctionsAction0>)r1
            withRxFunctionsAction0:(id<RxFunctionsAction0>)r2 {
  RxTestUtil_raceWithRxFunctionsAction0_withRxFunctionsAction0_(r1, r2);
}

+ (IOSObjectArray *)values {
  return RxTestUtil_values();
}

+ (RxTestUtil *)valueOfWithNSString:(NSString *)name {
  return RxTestUtil_valueOfWithNSString_(name);
}

- (id)copyWithZone:(NSZone *)zone {
  return self;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x9, 0, 1, -1, 2, -1, -1 },
    { NULL, "V", 0x9, 3, 4, -1, -1, -1, -1 },
    { NULL, "[LRxTestUtil;", 0x9, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxTestUtil;", 0x9, 5, 6, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(checkUtilityClassWithIOSClass:);
  methods[1].selector = @selector(raceWithRxFunctionsAction0:withRxFunctionsAction0:);
  methods[2].selector = @selector(values);
  methods[3].selector = @selector(valueOfWithNSString:);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "checkUtilityClass", "LIOSClass;", "(Ljava/lang/Class<*>;)V", "race", "LRxFunctionsAction0;LRxFunctionsAction0;", "valueOf", "LNSString;", "Ljava/lang/Enum<Lrx/TestUtil;>;" };
  static const J2ObjcClassInfo _RxTestUtil = { "TestUtil", "rx", ptrTable, methods, NULL, 7, 0x4011, 4, 0, -1, -1, -1, 7, -1 };
  return &_RxTestUtil;
}

@end

void RxTestUtil_checkUtilityClassWithIOSClass_(IOSClass *clazz) {
  RxTestUtil_initialize();
}

void RxTestUtil_raceWithRxFunctionsAction0_withRxFunctionsAction0_(id<RxFunctionsAction0> r1, id<RxFunctionsAction0> r2) {
  RxTestUtil_initialize();
  JavaUtilConcurrentAtomicAtomicInteger *counter = create_JavaUtilConcurrentAtomicAtomicInteger_initWithInt_(2);
  IOSObjectArray *errors = [IOSObjectArray arrayWithObjects:(id[]){ nil, nil } count:2 type:NSException_class_()];
  JavaUtilConcurrentCountDownLatch *cdl = create_JavaUtilConcurrentCountDownLatch_initWithInt_(1);
  RxScheduler_Worker *w = [((RxScheduler *) nil_chk(RxSchedulersSchedulers_io())) createWorker];
  @try {
    [((RxScheduler_Worker *) nil_chk(w)) scheduleWithRxFunctionsAction0:create_RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(counter, r2, errors, cdl)];
    if ([counter decrementAndGet] != 0) {
      while ([counter get] != 0) ;
    }
    @try {
      [((id<RxFunctionsAction0>) nil_chk(r1)) call];
    }
    @catch (NSException *ex) {
      IOSObjectArray_Set(errors, 0, ex);
    }
    id<JavaUtilList> errorList = create_JavaUtilArrayList_init();
    @try {
      if (![cdl awaitWithLong:5 withJavaUtilConcurrentTimeUnit:JreLoadEnum(JavaUtilConcurrentTimeUnit, SECONDS)]) {
        [errorList addWithId:create_JavaUtilConcurrentTimeoutException_init()];
      }
    }
    @catch (JavaLangInterruptedException *ex) {
      [errorList addWithId:ex];
    }
    if (IOSObjectArray_Get(errors, 0) != nil) {
      [errorList addWithId:IOSObjectArray_Get(errors, 0)];
    }
    if (IOSObjectArray_Get(errors, 1) != nil) {
      [errorList addWithId:IOSObjectArray_Get(errors, 1)];
    }
    RxExceptionsExceptions_throwIfAnyWithJavaUtilList_(errorList);
  }
  @finally {
    [w unsubscribe];
  }
}

void RxTestUtil_initWithNSString_withInt_(RxTestUtil *self, NSString *__name, jint __ordinal) {
  JavaLangEnum_initWithNSString_withInt_(self, __name, __ordinal);
}

IOSObjectArray *RxTestUtil_values() {
  RxTestUtil_initialize();
  return [IOSObjectArray arrayWithObjects:RxTestUtil_values_ count:0 type:RxTestUtil_class_()];
}

RxTestUtil *RxTestUtil_valueOfWithNSString_(NSString *name) {
  RxTestUtil_initialize();
  @throw create_JavaLangIllegalArgumentException_initWithNSString_(name);
  return nil;
}

RxTestUtil *RxTestUtil_fromOrdinal(NSUInteger ordinal) {
  return nil;
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxTestUtil)

@implementation RxTestUtil_$1

- (void)call {
  if ([((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(val$counter_)) decrementAndGet] != 0) {
    while ([val$counter_ get] != 0) ;
  }
  @try {
    [((id<RxFunctionsAction0>) nil_chk(val$r2_)) call];
  }
  @catch (NSException *ex) {
    IOSObjectArray_Set(nil_chk(val$errors_), 1, ex);
  }
  [((JavaUtilConcurrentCountDownLatch *) nil_chk(val$cdl_)) countDown];
}

- (instancetype)initWithJavaUtilConcurrentAtomicAtomicInteger:(JavaUtilConcurrentAtomicAtomicInteger *)capture$0
                                       withRxFunctionsAction0:(id<RxFunctionsAction0>)capture$1
                                         withNSExceptionArray:(IOSObjectArray *)capture$2
                         withJavaUtilConcurrentCountDownLatch:(JavaUtilConcurrentCountDownLatch *)capture$3 {
  RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(self, capture$0, capture$1, capture$2, capture$3);
  return self;
}

- (void)dealloc {
  RELEASE_(val$counter_);
  RELEASE_(val$r2_);
  RELEASE_(val$errors_);
  RELEASE_(val$cdl_);
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
  methods[1].selector = @selector(initWithJavaUtilConcurrentAtomicAtomicInteger:withRxFunctionsAction0:withNSExceptionArray:withJavaUtilConcurrentCountDownLatch:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$counter_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$r2_", "LRxFunctionsAction0;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$errors_", "[LNSException;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
    { "val$cdl_", "LJavaUtilConcurrentCountDownLatch;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LJavaUtilConcurrentAtomicAtomicInteger;LRxFunctionsAction0;[LNSException;LJavaUtilConcurrentCountDownLatch;", "LRxTestUtil;", "raceWithRxFunctionsAction0:withRxFunctionsAction0:" };
  static const J2ObjcClassInfo _RxTestUtil_$1 = { "", "rx", ptrTable, methods, fields, 7, 0x8008, 2, 4, 1, -1, 2, -1, -1 };
  return &_RxTestUtil_$1;
}

@end

void RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(RxTestUtil_$1 *self, JavaUtilConcurrentAtomicAtomicInteger *capture$0, id<RxFunctionsAction0> capture$1, IOSObjectArray *capture$2, JavaUtilConcurrentCountDownLatch *capture$3) {
  JreStrongAssign(&self->val$counter_, capture$0);
  JreStrongAssign(&self->val$r2_, capture$1);
  JreStrongAssign(&self->val$errors_, capture$2);
  JreStrongAssign(&self->val$cdl_, capture$3);
  NSObject_init(self);
}

RxTestUtil_$1 *new_RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(JavaUtilConcurrentAtomicAtomicInteger *capture$0, id<RxFunctionsAction0> capture$1, IOSObjectArray *capture$2, JavaUtilConcurrentCountDownLatch *capture$3) {
  J2OBJC_NEW_IMPL(RxTestUtil_$1, initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_, capture$0, capture$1, capture$2, capture$3)
}

RxTestUtil_$1 *create_RxTestUtil_$1_initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_(JavaUtilConcurrentAtomicAtomicInteger *capture$0, id<RxFunctionsAction0> capture$1, IOSObjectArray *capture$2, JavaUtilConcurrentCountDownLatch *capture$3) {
  J2OBJC_CREATE_IMPL(RxTestUtil_$1, initWithJavaUtilConcurrentAtomicAtomicInteger_withRxFunctionsAction0_withNSExceptionArray_withJavaUtilConcurrentCountDownLatch_, capture$0, capture$1, capture$2, capture$3)
}
