//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/OneTest.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OneTest")
#ifdef RESTRICT_OneTest
#define INCLUDE_ALL_OneTest 0
#else
#define INCLUDE_ALL_OneTest 1
#endif
#undef RESTRICT_OneTest

#if !defined (OneTest_) && (INCLUDE_ALL_OneTest || defined(INCLUDE_OneTest))
#define OneTest_

@class IOSObjectArray;
@protocol JavaUtilList;

@interface OneTest : NSObject

#pragma mark Public

- (instancetype)init;

+ (id<JavaUtilList>)allTestClassnames;

+ (void)runNamedTestWithNSString:(NSString *)classname;

+ (void)runTests;

@end

J2OBJC_STATIC_INIT(OneTest)

inline IOSObjectArray *OneTest_get_littletest();
inline IOSObjectArray *OneTest_set_littletest(IOSObjectArray *value);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT IOSObjectArray *OneTest_littletest;
J2OBJC_STATIC_FIELD_OBJ(OneTest, littletest, IOSObjectArray *)

inline IOSObjectArray *OneTest_get_alltests();
inline IOSObjectArray *OneTest_set_alltests(IOSObjectArray *value);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT IOSObjectArray *OneTest_alltests;
J2OBJC_STATIC_FIELD_OBJ(OneTest, alltests, IOSObjectArray *)

inline IOSObjectArray *OneTest_get_bigmem();
inline IOSObjectArray *OneTest_set_bigmem(IOSObjectArray *value);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT IOSObjectArray *OneTest_bigmem;
J2OBJC_STATIC_FIELD_OBJ(OneTest, bigmem, IOSObjectArray *)

FOUNDATION_EXPORT id<JavaUtilList> OneTest_allTestClassnames();

FOUNDATION_EXPORT void OneTest_runNamedTestWithNSString_(NSString *classname);

FOUNDATION_EXPORT void OneTest_runTests();

FOUNDATION_EXPORT void OneTest_init(OneTest *self);

FOUNDATION_EXPORT OneTest *new_OneTest_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OneTest *create_OneTest_init();

J2OBJC_TYPE_LITERAL_HEADER(OneTest)

#endif

#pragma pop_macro("INCLUDE_ALL_OneTest")
