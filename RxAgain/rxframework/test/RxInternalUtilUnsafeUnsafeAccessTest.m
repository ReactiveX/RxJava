//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/internal/util/unsafe/UnsafeAccessTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxInternalUtilUnsafeUnsafeAccess.h"
#include "RxInternalUtilUnsafeUnsafeAccessTest.h"
#include "RxTestUtil.h"
#include "java/lang/annotation/Annotation.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *RxInternalUtilUnsafeUnsafeAccessTest__Annotations$0();

@implementation RxInternalUtilUnsafeUnsafeAccessTest

- (void)constructorShouldBePrivate {
  RxTestUtil_checkUtilityClassWithIOSClass_(RxInternalUtilUnsafeUnsafeAccess_class_());
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilUnsafeUnsafeAccessTest_init(self);
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
  methods[0].selector = @selector(constructorShouldBePrivate);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { (void *)&RxInternalUtilUnsafeUnsafeAccessTest__Annotations$0 };
  static const J2ObjcClassInfo _RxInternalUtilUnsafeUnsafeAccessTest = { "UnsafeAccessTest", "rx.internal.util.unsafe", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_RxInternalUtilUnsafeUnsafeAccessTest;
}

@end

void RxInternalUtilUnsafeUnsafeAccessTest_init(RxInternalUtilUnsafeUnsafeAccessTest *self) {
  NSObject_init(self);
}

RxInternalUtilUnsafeUnsafeAccessTest *new_RxInternalUtilUnsafeUnsafeAccessTest_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilUnsafeUnsafeAccessTest, init)
}

RxInternalUtilUnsafeUnsafeAccessTest *create_RxInternalUtilUnsafeUnsafeAccessTest_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilUnsafeUnsafeAccessTest, init)
}

IOSObjectArray *RxInternalUtilUnsafeUnsafeAccessTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilUnsafeUnsafeAccessTest)
