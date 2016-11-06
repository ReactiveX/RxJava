//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/StartWithTests.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxObservable.h"
#include "RxObservablesBlockingObservable.h"
#include "RxStartWithTests.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/ArrayList.h"
#include "java/util/List.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxStartWithTests__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxStartWithTests__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxStartWithTests__Annotations$2();

@implementation RxStartWithTests

- (void)startWith1 {
  id<JavaUtilList> values = [((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_justWithId_withId_(@"one", @"two"))) startWithWithId:@"zero"])) toList])) toBlocking])) single];
  OrgJunitAssert_assertEqualsWithId_withId_(@"zero", [((id<JavaUtilList>) nil_chk(values)) getWithInt:0]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [values getWithInt:2]);
}

- (void)startWithIterable {
  id<JavaUtilList> li = create_JavaUtilArrayList_init();
  [li addWithId:@"alpha"];
  [li addWithId:@"beta"];
  id<JavaUtilList> values = [((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_justWithId_withId_(@"one", @"two"))) startWithWithJavaLangIterable:li])) toList])) toBlocking])) single];
  OrgJunitAssert_assertEqualsWithId_withId_(@"alpha", [((id<JavaUtilList>) nil_chk(values)) getWithInt:0]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"beta", [values getWithInt:1]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [values getWithInt:2]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [values getWithInt:3]);
}

- (void)startWithObservable {
  id<JavaUtilList> li = create_JavaUtilArrayList_init();
  [li addWithId:@"alpha"];
  [li addWithId:@"beta"];
  id<JavaUtilList> values = [((RxObservablesBlockingObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk([((RxObservable *) nil_chk(RxObservable_justWithId_withId_(@"one", @"two"))) startWithWithRxObservable:RxObservable_fromWithJavaLangIterable_(li)])) toList])) toBlocking])) single];
  OrgJunitAssert_assertEqualsWithId_withId_(@"alpha", [((id<JavaUtilList>) nil_chk(values)) getWithInt:0]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"beta", [values getWithInt:1]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"one", [values getWithInt:2]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"two", [values getWithInt:3]);
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxStartWithTests_init(self);
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
  methods[0].selector = @selector(startWith1);
  methods[1].selector = @selector(startWithIterable);
  methods[2].selector = @selector(startWithObservable);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxStartWithTests__Annotations$0, (void *)&RxStartWithTests__Annotations$1, (void *)&RxStartWithTests__Annotations$2 };
  static const J2ObjcClassInfo _RxStartWithTests = { "StartWithTests", "rx", ptrTable, methods, NULL, 7, 0x1, 4, 0, -1, -1, -1, -1, -1 };
  return &_RxStartWithTests;
}

@end

void RxStartWithTests_init(RxStartWithTests *self) {
  NSObject_init(self);
}

RxStartWithTests *new_RxStartWithTests_init() {
  J2OBJC_NEW_IMPL(RxStartWithTests, init)
}

RxStartWithTests *create_RxStartWithTests_init() {
  J2OBJC_CREATE_IMPL(RxStartWithTests, init)
}

IOSObjectArray *RxStartWithTests__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxStartWithTests__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxStartWithTests__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxStartWithTests)
