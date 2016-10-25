//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/test/java/android/content/IOContextTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "android/content/Context.h"
#include "android/content/IOContextTest.h"
#include "android/content/IOSContext.h"
#include "android/content/SharedPreferences.h"
#include "android/os/Looper.h"
#include "java/io/PrintStream.h"
#include "java/lang/Error.h"
#include "java/lang/Float.h"
#include "java/lang/System.h"
#include "java/lang/annotation/Annotation.h"
#include "junit/framework/Assert.h"
#include "org/junit/Test.h"

__attribute__((unused)) static IOSObjectArray *AndroidContentIOContextTest__Annotations$0();

@implementation AndroidContentIOContextTest

- (void)customizedExceptionUsed {
  @try {
    AndroidOsLooper_prepareMainLooper();
    AndroidContentIOSContext *iosContext = create_AndroidContentIOSContext_init();
    id<AndroidContentSharedPreferences> test = [iosContext getSharedPreferencesWithNSString:@"test" withInt:AndroidContentContext_MODE_PRIVATE];
    [((id<AndroidContentSharedPreferences_Editor>) nil_chk([((id<AndroidContentSharedPreferences_Editor>) nil_chk([((id<AndroidContentSharedPreferences>) nil_chk(test)) edit])) putStringWithNSString:@"asdf" withNSString:@"qwert"])) apply];
    JunitFrameworkAssert_assertEqualsWithNSString_withNSString_([test getStringWithNSString:@"asdf" withNSString:@"fff"], @"qwer");
    [((id<AndroidContentSharedPreferences_Editor>) nil_chk([((id<AndroidContentSharedPreferences_Editor>) nil_chk([test edit])) removeWithNSString:@"asdf"])) apply];
    JunitFrameworkAssert_assertEqualsWithNSString_withNSString_([test getStringWithNSString:@"asdf" withNSString:@"fff"], @"fff");
    id<AndroidContentSharedPreferences_Editor> edit = [test edit];
    [((id<AndroidContentSharedPreferences_Editor>) nil_chk(edit)) putBooleanWithNSString:@"b" withBoolean:true];
    [edit putBooleanWithNSString:@"b2" withBoolean:false];
    [edit putFloatWithNSString:@"f" withFloat:1.235f];
    [edit putIntWithNSString:@"i" withInt:55543];
    [edit putLongWithNSString:@"l" withLong:492349238437l];
    [edit apply];
    JunitFrameworkAssert_assertEqualsWithBoolean_withBoolean_([test getBooleanWithNSString:@"b" withBoolean:false], true);
    JunitFrameworkAssert_assertEqualsWithBoolean_withBoolean_([test getBooleanWithNSString:@"b2" withBoolean:true], false);
    JunitFrameworkAssert_assertEqualsWithBoolean_withBoolean_([test getBooleanWithNSString:@"bnone" withBoolean:true], true);
    JunitFrameworkAssert_assertEqualsWithBoolean_withBoolean_([test getBooleanWithNSString:@"bnone2" withBoolean:false], false);
    JunitFrameworkAssert_assertEqualsWithId_withId_(JavaLangFloat_valueOfWithFloat_([test getFloatWithNSString:@"f" withFloat:1.11f]), JavaLangFloat_valueOfWithFloat_(1.235f));
    JunitFrameworkAssert_assertEqualsWithInt_withInt_([test getIntWithNSString:@"i" withInt:33], 55543);
    JunitFrameworkAssert_assertEqualsWithLong_withLong_([test getLongWithNSString:@"l" withLong:33], 492349238437l);
  }
  @catch (JavaLangError *e) {
    [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, out))) printlnWithNSString:[((JavaLangError *) nil_chk(e)) getMessage]];
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  AndroidContentIOContextTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, -1, -1, 0, -1, 1, -1 },
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(customizedExceptionUsed);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "LNSException;", (void *)&AndroidContentIOContextTest__Annotations$0 };
  static const J2ObjcClassInfo _AndroidContentIOContextTest = { "IOContextTest", "android.content", ptrTable, methods, NULL, 7, 0x1, 2, 0, -1, -1, -1, -1, -1 };
  return &_AndroidContentIOContextTest;
}

@end

void AndroidContentIOContextTest_init(AndroidContentIOContextTest *self) {
  NSObject_init(self);
}

AndroidContentIOContextTest *new_AndroidContentIOContextTest_init() {
  J2OBJC_NEW_IMPL(AndroidContentIOContextTest, init)
}

AndroidContentIOContextTest *create_AndroidContentIOContextTest_init() {
  J2OBJC_CREATE_IMPL(AndroidContentIOContextTest, init)
}

IOSObjectArray *AndroidContentIOContextTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(AndroidContentIOContextTest)
