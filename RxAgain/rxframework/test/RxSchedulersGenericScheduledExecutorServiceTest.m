//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/schedulers/GenericScheduledExecutorServiceTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxFunctionsFunc0.h"
#include "RxInternalSchedulersGenericScheduledExecutorService.h"
#include "RxPluginsRxJavaHooks.h"
#include "RxSchedulersGenericScheduledExecutorServiceTest.h"
#include "RxSchedulersSchedulers.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/List.h"
#include "java/util/concurrent/Executors.h"
#include "java/util/concurrent/ScheduledExecutorService.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxSchedulersGenericScheduledExecutorServiceTest__Annotations$0();

@interface RxSchedulersGenericScheduledExecutorServiceTest_$1 : NSObject < RxFunctionsFunc0 > {
 @public
  id<JavaUtilConcurrentScheduledExecutorService> val$exec_;
}

- (id<JavaUtilConcurrentScheduledExecutorService>)call;

- (instancetype)initWithJavaUtilConcurrentScheduledExecutorService:(id<JavaUtilConcurrentScheduledExecutorService>)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxSchedulersGenericScheduledExecutorServiceTest_$1)

J2OBJC_FIELD_SETTER(RxSchedulersGenericScheduledExecutorServiceTest_$1, val$exec_, id<JavaUtilConcurrentScheduledExecutorService>)

__attribute__((unused)) static void RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(RxSchedulersGenericScheduledExecutorServiceTest_$1 *self, id<JavaUtilConcurrentScheduledExecutorService> capture$0);

__attribute__((unused)) static RxSchedulersGenericScheduledExecutorServiceTest_$1 *new_RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(id<JavaUtilConcurrentScheduledExecutorService> capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxSchedulersGenericScheduledExecutorServiceTest_$1 *create_RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(id<JavaUtilConcurrentScheduledExecutorService> capture$0);

@implementation RxSchedulersGenericScheduledExecutorServiceTest

- (void)genericScheduledExecutorServiceHook {
  OrgJunitAssert_assertNotNullWithId_(RxInternalSchedulersGenericScheduledExecutorService_class_());
  id<JavaUtilConcurrentScheduledExecutorService> exec = JavaUtilConcurrentExecutors_newSingleThreadScheduledExecutor();
  @try {
    RxPluginsRxJavaHooks_setOnGenericScheduledExecutorServiceWithRxFunctionsFunc0_(create_RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(exec));
    RxSchedulersSchedulers_shutdown();
    RxSchedulersSchedulers_start();
    OrgJunitAssert_assertSameWithId_withId_(exec, RxInternalSchedulersGenericScheduledExecutorService_getInstance());
    RxPluginsRxJavaHooks_setOnGenericScheduledExecutorServiceWithRxFunctionsFunc0_(nil);
    RxSchedulersSchedulers_shutdown();
    RxSchedulersSchedulers_start();
    OrgJunitAssert_assertNotSameWithId_withId_(exec, RxInternalSchedulersGenericScheduledExecutorService_getInstance());
  }
  @finally {
    RxPluginsRxJavaHooks_reset();
    [((id<JavaUtilConcurrentScheduledExecutorService>) nil_chk(exec)) shutdownNow];
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxSchedulersGenericScheduledExecutorServiceTest_init(self);
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
  methods[0].selector = @selector(genericScheduledExecutorServiceHook);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxSchedulersGenericScheduledExecutorServiceTest__Annotations$0 };
  static const J2ObjcClassInfo _RxSchedulersGenericScheduledExecutorServiceTest = { "GenericScheduledExecutorServiceTest", "rx.schedulers", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_RxSchedulersGenericScheduledExecutorServiceTest;
}

@end

void RxSchedulersGenericScheduledExecutorServiceTest_init(RxSchedulersGenericScheduledExecutorServiceTest *self) {
  NSObject_init(self);
}

RxSchedulersGenericScheduledExecutorServiceTest *new_RxSchedulersGenericScheduledExecutorServiceTest_init() {
  J2OBJC_NEW_IMPL(RxSchedulersGenericScheduledExecutorServiceTest, init)
}

RxSchedulersGenericScheduledExecutorServiceTest *create_RxSchedulersGenericScheduledExecutorServiceTest_init() {
  J2OBJC_CREATE_IMPL(RxSchedulersGenericScheduledExecutorServiceTest, init)
}

IOSObjectArray *RxSchedulersGenericScheduledExecutorServiceTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxSchedulersGenericScheduledExecutorServiceTest)

@implementation RxSchedulersGenericScheduledExecutorServiceTest_$1

- (id<JavaUtilConcurrentScheduledExecutorService>)call {
  return val$exec_;
}

- (instancetype)initWithJavaUtilConcurrentScheduledExecutorService:(id<JavaUtilConcurrentScheduledExecutorService>)capture$0 {
  RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$exec_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LJavaUtilConcurrentScheduledExecutorService;", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 0, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(call);
  methods[1].selector = @selector(initWithJavaUtilConcurrentScheduledExecutorService:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$exec_", "LJavaUtilConcurrentScheduledExecutorService;", .constantValue.asLong = 0, 0x1012, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "LJavaUtilConcurrentScheduledExecutorService;", "LRxSchedulersGenericScheduledExecutorServiceTest;", "genericScheduledExecutorServiceHook", "Ljava/lang/Object;Lrx/functions/Func0<Ljava/util/concurrent/ScheduledExecutorService;>;" };
  static const J2ObjcClassInfo _RxSchedulersGenericScheduledExecutorServiceTest_$1 = { "", "rx.schedulers", ptrTable, methods, fields, 7, 0x8008, 2, 1, 1, -1, 2, 3, -1 };
  return &_RxSchedulersGenericScheduledExecutorServiceTest_$1;
}

@end

void RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(RxSchedulersGenericScheduledExecutorServiceTest_$1 *self, id<JavaUtilConcurrentScheduledExecutorService> capture$0) {
  JreStrongAssign(&self->val$exec_, capture$0);
  NSObject_init(self);
}

RxSchedulersGenericScheduledExecutorServiceTest_$1 *new_RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(id<JavaUtilConcurrentScheduledExecutorService> capture$0) {
  J2OBJC_NEW_IMPL(RxSchedulersGenericScheduledExecutorServiceTest_$1, initWithJavaUtilConcurrentScheduledExecutorService_, capture$0)
}

RxSchedulersGenericScheduledExecutorServiceTest_$1 *create_RxSchedulersGenericScheduledExecutorServiceTest_$1_initWithJavaUtilConcurrentScheduledExecutorService_(id<JavaUtilConcurrentScheduledExecutorService> capture$0) {
  J2OBJC_CREATE_IMPL(RxSchedulersGenericScheduledExecutorServiceTest_$1, initWithJavaUtilConcurrentScheduledExecutorService_, capture$0)
}
