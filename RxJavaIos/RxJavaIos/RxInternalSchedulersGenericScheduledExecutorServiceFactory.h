//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/schedulers/GenericScheduledExecutorServiceFactory.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalSchedulersGenericScheduledExecutorServiceFactory")
#ifdef RESTRICT_RxInternalSchedulersGenericScheduledExecutorServiceFactory
#define INCLUDE_ALL_RxInternalSchedulersGenericScheduledExecutorServiceFactory 0
#else
#define INCLUDE_ALL_RxInternalSchedulersGenericScheduledExecutorServiceFactory 1
#endif
#undef RESTRICT_RxInternalSchedulersGenericScheduledExecutorServiceFactory

#if !defined (RxInternalSchedulersGenericScheduledExecutorServiceFactory_) && (INCLUDE_ALL_RxInternalSchedulersGenericScheduledExecutorServiceFactory || defined(INCLUDE_RxInternalSchedulersGenericScheduledExecutorServiceFactory))
#define RxInternalSchedulersGenericScheduledExecutorServiceFactory_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

@class IOSObjectArray;
@class RxInternalUtilRxThreadFactory;
@protocol JavaUtilConcurrentScheduledExecutorService;
@protocol JavaUtilConcurrentThreadFactory;

@interface RxInternalSchedulersGenericScheduledExecutorServiceFactory : JavaLangEnum < NSCopying >

#pragma mark Public

+ (id<JavaUtilConcurrentScheduledExecutorService>)create;

+ (RxInternalSchedulersGenericScheduledExecutorServiceFactory *)valueOfWithNSString:(NSString *)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

+ (id<JavaUtilConcurrentScheduledExecutorService>)createDefault;

+ (id<JavaUtilConcurrentThreadFactory>)threadFactory;

- (id)copyWithZone:(NSZone *)zone;

@end

J2OBJC_STATIC_INIT(RxInternalSchedulersGenericScheduledExecutorServiceFactory)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT RxInternalSchedulersGenericScheduledExecutorServiceFactory *RxInternalSchedulersGenericScheduledExecutorServiceFactory_values_[];

inline NSString *RxInternalSchedulersGenericScheduledExecutorServiceFactory_get_THREAD_NAME_PREFIX();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT NSString *RxInternalSchedulersGenericScheduledExecutorServiceFactory_THREAD_NAME_PREFIX;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalSchedulersGenericScheduledExecutorServiceFactory, THREAD_NAME_PREFIX, NSString *)

inline RxInternalUtilRxThreadFactory *RxInternalSchedulersGenericScheduledExecutorServiceFactory_get_THREAD_FACTORY();
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT RxInternalUtilRxThreadFactory *RxInternalSchedulersGenericScheduledExecutorServiceFactory_THREAD_FACTORY;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalSchedulersGenericScheduledExecutorServiceFactory, THREAD_FACTORY, RxInternalUtilRxThreadFactory *)

FOUNDATION_EXPORT id<JavaUtilConcurrentThreadFactory> RxInternalSchedulersGenericScheduledExecutorServiceFactory_threadFactory();

FOUNDATION_EXPORT id<JavaUtilConcurrentScheduledExecutorService> RxInternalSchedulersGenericScheduledExecutorServiceFactory_create();

FOUNDATION_EXPORT id<JavaUtilConcurrentScheduledExecutorService> RxInternalSchedulersGenericScheduledExecutorServiceFactory_createDefault();

FOUNDATION_EXPORT IOSObjectArray *RxInternalSchedulersGenericScheduledExecutorServiceFactory_values();

FOUNDATION_EXPORT RxInternalSchedulersGenericScheduledExecutorServiceFactory *RxInternalSchedulersGenericScheduledExecutorServiceFactory_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT RxInternalSchedulersGenericScheduledExecutorServiceFactory *RxInternalSchedulersGenericScheduledExecutorServiceFactory_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalSchedulersGenericScheduledExecutorServiceFactory)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalSchedulersGenericScheduledExecutorServiceFactory")
