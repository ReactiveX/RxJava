//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/test/java/rx/test/TestObstructionDetection.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxTestTestObstructionDetection")
#ifdef RESTRICT_RxTestTestObstructionDetection
#define INCLUDE_ALL_RxTestTestObstructionDetection 0
#else
#define INCLUDE_ALL_RxTestTestObstructionDetection 1
#endif
#undef RESTRICT_RxTestTestObstructionDetection

#if !defined (RxTestTestObstructionDetection_) && (INCLUDE_ALL_RxTestTestObstructionDetection || defined(INCLUDE_RxTestTestObstructionDetection))
#define RxTestTestObstructionDetection_

@interface RxTestTestObstructionDetection : NSObject

#pragma mark Public

+ (void)checkObstruction;

@end

J2OBJC_EMPTY_STATIC_INIT(RxTestTestObstructionDetection)

FOUNDATION_EXPORT void RxTestTestObstructionDetection_checkObstruction();

J2OBJC_TYPE_LITERAL_HEADER(RxTestTestObstructionDetection)

#endif

#if !defined (RxTestTestObstructionDetection_ObstructionException_) && (INCLUDE_ALL_RxTestTestObstructionDetection || defined(INCLUDE_RxTestTestObstructionDetection_ObstructionException))
#define RxTestTestObstructionDetection_ObstructionException_

#define RESTRICT_JavaLangRuntimeException 1
#define INCLUDE_JavaLangRuntimeException 1
#include "java/lang/RuntimeException.h"

@interface RxTestTestObstructionDetection_ObstructionException : JavaLangRuntimeException

#pragma mark Public

- (instancetype)initWithNSString:(NSString *)message;

@end

J2OBJC_EMPTY_STATIC_INIT(RxTestTestObstructionDetection_ObstructionException)

FOUNDATION_EXPORT void RxTestTestObstructionDetection_ObstructionException_initWithNSString_(RxTestTestObstructionDetection_ObstructionException *self, NSString *message);

FOUNDATION_EXPORT RxTestTestObstructionDetection_ObstructionException *new_RxTestTestObstructionDetection_ObstructionException_initWithNSString_(NSString *message) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT RxTestTestObstructionDetection_ObstructionException *create_RxTestTestObstructionDetection_ObstructionException_initWithNSString_(NSString *message);

J2OBJC_TYPE_LITERAL_HEADER(RxTestTestObstructionDetection_ObstructionException)

#endif

#pragma pop_macro("INCLUDE_ALL_RxTestTestObstructionDetection")
