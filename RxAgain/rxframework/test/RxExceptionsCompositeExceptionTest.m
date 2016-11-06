//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/exceptions/CompositeExceptionTest.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsCompositeException.h"
#include "RxExceptionsCompositeExceptionTest.h"
#include "java/io/ByteArrayOutputStream.h"
#include "java/io/PrintStream.h"
#include "java/lang/System.h"
#include "java/lang/UnsupportedOperationException.h"
#include "java/lang/annotation/Annotation.h"
#include "java/util/ArrayList.h"
#include "java/util/Arrays.h"
#include "java/util/Collections.h"
#include "java/util/List.h"
#include "org/junit/Assert.h"
#include "org/junit/Test.h"

@interface RxExceptionsCompositeExceptionTest () {
 @public
  NSException *ex1_;
  NSException *ex2_;
  NSException *ex3_;
}

- (RxExceptionsCompositeException *)getNewCompositeExceptionWithEx123;

+ (void)assertNoCircularReferencesWithNSException:(NSException *)ex;

+ (NSException *)getRootCauseWithNSException:(NSException *)ex;

@end

J2OBJC_FIELD_SETTER(RxExceptionsCompositeExceptionTest, ex1_, NSException *)
J2OBJC_FIELD_SETTER(RxExceptionsCompositeExceptionTest, ex2_, NSException *)
J2OBJC_FIELD_SETTER(RxExceptionsCompositeExceptionTest, ex3_, NSException *)

__attribute__((unused)) static RxExceptionsCompositeException *RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(RxExceptionsCompositeExceptionTest *self);

__attribute__((unused)) static void RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(NSException *ex);

__attribute__((unused)) static NSException *RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(NSException *ex);

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$0();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$1();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$2();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$3();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$4();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$5();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$6();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$7();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$8();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$9();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$10();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$11();

__attribute__((unused)) static IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$12();

@interface RxExceptionsCompositeExceptionTest_$1 : NSException

- (NSException *)initCauseWithNSException:(NSException *)cause OBJC_METHOD_FAMILY_NONE;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxExceptionsCompositeExceptionTest_$1)

inline jlong RxExceptionsCompositeExceptionTest_$1_get_serialVersionUID();
#define RxExceptionsCompositeExceptionTest_$1_serialVersionUID -3282577447436848385LL
J2OBJC_STATIC_FIELD_CONSTANT(RxExceptionsCompositeExceptionTest_$1, serialVersionUID, jlong)

__attribute__((unused)) static void RxExceptionsCompositeExceptionTest_$1_init(RxExceptionsCompositeExceptionTest_$1 *self);

__attribute__((unused)) static RxExceptionsCompositeExceptionTest_$1 *new_RxExceptionsCompositeExceptionTest_$1_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxExceptionsCompositeExceptionTest_$1 *create_RxExceptionsCompositeExceptionTest_$1_init();

@interface RxExceptionsCompositeExceptionTest_$2 : NSException

- (NSException *)initCauseWithNSException:(NSException *)cause OBJC_METHOD_FAMILY_NONE;

- (instancetype)initWithNSString:(NSString *)arg$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxExceptionsCompositeExceptionTest_$2)

inline jlong RxExceptionsCompositeExceptionTest_$2_get_serialVersionUID();
#define RxExceptionsCompositeExceptionTest_$2_serialVersionUID -7984762607894527888LL
J2OBJC_STATIC_FIELD_CONSTANT(RxExceptionsCompositeExceptionTest_$2, serialVersionUID, jlong)

__attribute__((unused)) static void RxExceptionsCompositeExceptionTest_$2_initWithNSString_(RxExceptionsCompositeExceptionTest_$2 *self, NSString *arg$0);

__attribute__((unused)) static RxExceptionsCompositeExceptionTest_$2 *new_RxExceptionsCompositeExceptionTest_$2_initWithNSString_(NSString *arg$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxExceptionsCompositeExceptionTest_$2 *create_RxExceptionsCompositeExceptionTest_$2_initWithNSString_(NSString *arg$0);

@implementation RxExceptionsCompositeExceptionTest

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxExceptionsCompositeExceptionTest_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (RxExceptionsCompositeException *)getNewCompositeExceptionWithEx123 {
  return RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(self);
}

- (void)testMultipleWithSameCause {
  NSException *rootCause = create_NSException_initWithNSString_(@"RootCause");
  NSException *e1 = create_NSException_initWithNSString_withNSException_(@"1", rootCause);
  NSException *e2 = create_NSException_initWithNSString_withNSException_(@"2", rootCause);
  NSException *e3 = create_NSException_initWithNSString_withNSException_(@"3", rootCause);
  RxExceptionsCompositeException *ce = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ e1, e2, e3 } count:3 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [ce printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk([ce getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(ce);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(ce));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([ce getCause])) printStackTrace];
}

- (void)testCompositeExceptionFromParentThenChild {
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ ex1_, ex2_ } count:2 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(2, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

- (void)testCompositeExceptionFromChildThenParent {
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ ex2_, ex1_ } count:2 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(2, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

- (void)testCompositeExceptionFromChildAndComposite {
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ ex1_, RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(self) } count:2 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

- (void)testCompositeExceptionFromCompositeAndChild {
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(self), ex1_ } count:2 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

- (void)testCompositeExceptionFromTwoDuplicateComposites {
  id<JavaUtilList> exs = create_JavaUtilArrayList_init();
  [exs addWithId:RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(self)];
  [exs addWithId:RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(self)];
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(exs);
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(3, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

+ (void)assertNoCircularReferencesWithNSException:(NSException *)ex {
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(ex);
}

+ (NSException *)getRootCauseWithNSException:(NSException *)ex {
  return RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(ex);
}

- (void)testNullCollection {
  RxExceptionsCompositeException *composite = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(nil);
  [composite getCause];
  [composite printStackTrace];
}

- (void)testNullElement {
  RxExceptionsCompositeException *composite = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilCollections_singletonListWithId_(nil));
  [composite getCause];
  [composite printStackTrace];
}

- (void)testCompositeExceptionWithUnsupportedInitCause {
  NSException *t = create_RxExceptionsCompositeExceptionTest_$1_init();
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ t, ex1_ } count:2 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(2, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

- (void)testCompositeExceptionWithNullInitCause {
  NSException *t = create_RxExceptionsCompositeExceptionTest_$2_initWithNSString_(@"ThrowableWithNullInitCause");
  RxExceptionsCompositeException *cex = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ t, ex1_ } count:2 type:NSException_class_()]));
  [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:@"----------------------------- print composite stacktrace"];
  [cex printStackTrace];
  OrgJunitAssert_assertEqualsWithLong_withLong_(2, [((id<JavaUtilList>) nil_chk([cex getExceptions])) size]);
  RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(cex);
  OrgJunitAssert_assertNotNullWithId_(RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(cex));
  [JreLoadStatic(JavaLangSystem, err) printlnWithNSString:@"----------------------------- print cause stacktrace"];
  [((NSException *) nil_chk([cex getCause])) printStackTrace];
}

- (void)messageCollection {
  RxExceptionsCompositeException *compositeException = create_RxExceptionsCompositeException_initWithJavaUtilCollection_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ ex1_, ex3_ } count:2 type:NSException_class_()]));
  OrgJunitAssert_assertEqualsWithId_withId_(@"2 exceptions occurred. ", [compositeException getMessage]);
}

- (void)messageVarargs {
  RxExceptionsCompositeException *compositeException = create_RxExceptionsCompositeException_initWithNSExceptionArray_([IOSObjectArray arrayWithObjects:(id[]){ ex1_, ex2_, ex3_ } count:3 type:NSException_class_()]);
  OrgJunitAssert_assertEqualsWithId_withId_(@"3 exceptions occurred. ", [compositeException getMessage]);
}

- (void)complexCauses {
  NSException *e1 = create_NSException_initWithNSString_(@"1");
  NSException *e2 = create_NSException_initWithNSString_(@"2");
  [e1 initCauseWithNSException:e2];
  NSException *e3 = create_NSException_initWithNSString_(@"3");
  NSException *e4 = create_NSException_initWithNSString_(@"4");
  [e3 initCauseWithNSException:e4];
  NSException *e5 = create_NSException_initWithNSString_(@"5");
  NSException *e6 = create_NSException_initWithNSString_(@"6");
  [e5 initCauseWithNSException:e6];
  RxExceptionsCompositeException *compositeException = create_RxExceptionsCompositeException_initWithNSExceptionArray_([IOSObjectArray arrayWithObjects:(id[]){ e1, e3, e5 } count:3 type:NSException_class_()]);
  JreAssert((([[compositeException getCause] isKindOfClass:[RxExceptionsCompositeException_CompositeExceptionCausalChain class]])), (@"rx/exceptions/CompositeExceptionTest.java:260 condition failed: assert(compositeException.getCause() instanceof CompositeExceptionCausalChain);"));
  id<JavaUtilList> causeChain = create_JavaUtilArrayList_init();
  NSException *cause = [((NSException *) nil_chk([compositeException getCause])) getCause];
  while (cause != nil) {
    [causeChain addWithId:cause];
    cause = [cause getCause];
  }
  OrgJunitAssert_assertEqualsWithId_withId_(JavaUtilArrays_asListWithNSObjectArray_([IOSObjectArray arrayWithObjects:(id[]){ e1, e2, e3, e4, e5, e6 } count:6 type:NSException_class_()]), causeChain);
}

- (void)dealloc {
  RELEASE_(ex1_);
  RELEASE_(ex2_);
  RELEASE_(ex3_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LRxExceptionsCompositeException;", 0x2, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 0, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 2, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 3, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 4, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 5, -1 },
    { NULL, "V", 0xa, 6, 7, -1, -1, -1, -1 },
    { NULL, "LNSException;", 0xa, 8, 7, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 9, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 10, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 11, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 12, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 13, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 14, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, 15, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(getNewCompositeExceptionWithEx123);
  methods[2].selector = @selector(testMultipleWithSameCause);
  methods[3].selector = @selector(testCompositeExceptionFromParentThenChild);
  methods[4].selector = @selector(testCompositeExceptionFromChildThenParent);
  methods[5].selector = @selector(testCompositeExceptionFromChildAndComposite);
  methods[6].selector = @selector(testCompositeExceptionFromCompositeAndChild);
  methods[7].selector = @selector(testCompositeExceptionFromTwoDuplicateComposites);
  methods[8].selector = @selector(assertNoCircularReferencesWithNSException:);
  methods[9].selector = @selector(getRootCauseWithNSException:);
  methods[10].selector = @selector(testNullCollection);
  methods[11].selector = @selector(testNullElement);
  methods[12].selector = @selector(testCompositeExceptionWithUnsupportedInitCause);
  methods[13].selector = @selector(testCompositeExceptionWithNullInitCause);
  methods[14].selector = @selector(messageCollection);
  methods[15].selector = @selector(messageVarargs);
  methods[16].selector = @selector(complexCauses);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "ex1_", "LNSException;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "ex2_", "LNSException;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "ex3_", "LNSException;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { (void *)&RxExceptionsCompositeExceptionTest__Annotations$0, (void *)&RxExceptionsCompositeExceptionTest__Annotations$1, (void *)&RxExceptionsCompositeExceptionTest__Annotations$2, (void *)&RxExceptionsCompositeExceptionTest__Annotations$3, (void *)&RxExceptionsCompositeExceptionTest__Annotations$4, (void *)&RxExceptionsCompositeExceptionTest__Annotations$5, "assertNoCircularReferences", "LNSException;", "getRootCause", (void *)&RxExceptionsCompositeExceptionTest__Annotations$6, (void *)&RxExceptionsCompositeExceptionTest__Annotations$7, (void *)&RxExceptionsCompositeExceptionTest__Annotations$8, (void *)&RxExceptionsCompositeExceptionTest__Annotations$9, (void *)&RxExceptionsCompositeExceptionTest__Annotations$10, (void *)&RxExceptionsCompositeExceptionTest__Annotations$11, (void *)&RxExceptionsCompositeExceptionTest__Annotations$12 };
  static const J2ObjcClassInfo _RxExceptionsCompositeExceptionTest = { "CompositeExceptionTest", "rx.exceptions", ptrTable, methods, fields, 7, 0x1, 17, 3, -1, -1, -1, -1, -1 };
  return &_RxExceptionsCompositeExceptionTest;
}

@end

void RxExceptionsCompositeExceptionTest_init(RxExceptionsCompositeExceptionTest *self) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->ex1_, new_NSException_initWithNSString_(@"Ex1"));
  JreStrongAssignAndConsume(&self->ex2_, new_NSException_initWithNSString_withNSException_(@"Ex2", self->ex1_));
  JreStrongAssignAndConsume(&self->ex3_, new_NSException_initWithNSString_withNSException_(@"Ex3", self->ex2_));
}

RxExceptionsCompositeExceptionTest *new_RxExceptionsCompositeExceptionTest_init() {
  J2OBJC_NEW_IMPL(RxExceptionsCompositeExceptionTest, init)
}

RxExceptionsCompositeExceptionTest *create_RxExceptionsCompositeExceptionTest_init() {
  J2OBJC_CREATE_IMPL(RxExceptionsCompositeExceptionTest, init)
}

RxExceptionsCompositeException *RxExceptionsCompositeExceptionTest_getNewCompositeExceptionWithEx123(RxExceptionsCompositeExceptionTest *self) {
  id<JavaUtilList> throwables = create_JavaUtilArrayList_init();
  [throwables addWithId:self->ex1_];
  [throwables addWithId:self->ex2_];
  [throwables addWithId:self->ex3_];
  return create_RxExceptionsCompositeException_initWithJavaUtilCollection_(throwables);
}

void RxExceptionsCompositeExceptionTest_assertNoCircularReferencesWithNSException_(NSException *ex) {
  RxExceptionsCompositeExceptionTest_initialize();
  JavaIoByteArrayOutputStream *baos = create_JavaIoByteArrayOutputStream_init();
  JavaIoPrintStream *printStream = create_JavaIoPrintStream_initWithJavaIoOutputStream_(baos);
  [((NSException *) nil_chk(ex)) printStackTraceWithJavaIoPrintStream:printStream];
  OrgJunitAssert_assertFalseWithBoolean_([((NSString *) nil_chk([baos description])) contains:@"CIRCULAR REFERENCE"]);
}

NSException *RxExceptionsCompositeExceptionTest_getRootCauseWithNSException_(NSException *ex) {
  RxExceptionsCompositeExceptionTest_initialize();
  NSException *root = [((NSException *) nil_chk(ex)) getCause];
  if (root == nil) {
    return nil;
  }
  else {
    while (true) {
      if ([((NSException *) nil_chk(root)) getCause] == nil) {
        return root;
      }
      else {
        root = [root getCause];
      }
    }
  }
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$0() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$1() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$2() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$3() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$4() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$5() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$6() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$7() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$8() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$9() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 1000) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$10() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$11() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

IOSObjectArray *RxExceptionsCompositeExceptionTest__Annotations$12() {
  return [IOSObjectArray arrayWithObjects:(id[]){ create_OrgJunitTest(OrgJunitTest_None_class_(), 0) } count:1 type:JavaLangAnnotationAnnotation_class_()];
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxExceptionsCompositeExceptionTest)

@implementation RxExceptionsCompositeExceptionTest_$1

- (NSException *)initCauseWithNSException:(NSException *)cause {
  @synchronized(self) {
    @throw create_JavaLangUnsupportedOperationException_init();
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxExceptionsCompositeExceptionTest_$1_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LNSException;", 0x21, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initCauseWithNSException:);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxExceptionsCompositeExceptionTest_$1_serialVersionUID, 0x1a, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "initCause", "LNSException;", "LRxExceptionsCompositeExceptionTest;", "testCompositeExceptionWithUnsupportedInitCause" };
  static const J2ObjcClassInfo _RxExceptionsCompositeExceptionTest_$1 = { "", "rx.exceptions", ptrTable, methods, fields, 7, 0x8008, 2, 1, 2, -1, 3, -1, -1 };
  return &_RxExceptionsCompositeExceptionTest_$1;
}

@end

void RxExceptionsCompositeExceptionTest_$1_init(RxExceptionsCompositeExceptionTest_$1 *self) {
  NSException_init(self);
}

RxExceptionsCompositeExceptionTest_$1 *new_RxExceptionsCompositeExceptionTest_$1_init() {
  J2OBJC_NEW_IMPL(RxExceptionsCompositeExceptionTest_$1, init)
}

RxExceptionsCompositeExceptionTest_$1 *create_RxExceptionsCompositeExceptionTest_$1_init() {
  J2OBJC_CREATE_IMPL(RxExceptionsCompositeExceptionTest_$1, init)
}

@implementation RxExceptionsCompositeExceptionTest_$2

- (NSException *)initCauseWithNSException:(NSException *)cause {
  @synchronized(self) {
    return nil;
  }
}

- (instancetype)initWithNSString:(NSString *)arg$0 {
  RxExceptionsCompositeExceptionTest_$2_initWithNSString_(self, arg$0);
  return self;
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LNSException;", 0x21, 0, 1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, 2, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(initCauseWithNSException:);
  methods[1].selector = @selector(initWithNSString:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "serialVersionUID", "J", .constantValue.asLong = RxExceptionsCompositeExceptionTest_$2_serialVersionUID, 0x1a, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "initCause", "LNSException;", "LNSString;", "LRxExceptionsCompositeExceptionTest;", "testCompositeExceptionWithNullInitCause" };
  static const J2ObjcClassInfo _RxExceptionsCompositeExceptionTest_$2 = { "", "rx.exceptions", ptrTable, methods, fields, 7, 0x8008, 2, 1, 3, -1, 4, -1, -1 };
  return &_RxExceptionsCompositeExceptionTest_$2;
}

@end

void RxExceptionsCompositeExceptionTest_$2_initWithNSString_(RxExceptionsCompositeExceptionTest_$2 *self, NSString *arg$0) {
  NSException_initWithNSString_(self, arg$0);
}

RxExceptionsCompositeExceptionTest_$2 *new_RxExceptionsCompositeExceptionTest_$2_initWithNSString_(NSString *arg$0) {
  J2OBJC_NEW_IMPL(RxExceptionsCompositeExceptionTest_$2, initWithNSString_, arg$0)
}

RxExceptionsCompositeExceptionTest_$2 *create_RxExceptionsCompositeExceptionTest_$2_initWithNSString_(NSString *arg$0) {
  J2OBJC_CREATE_IMPL(RxExceptionsCompositeExceptionTest_$2, initWithNSString_, arg$0)
}
