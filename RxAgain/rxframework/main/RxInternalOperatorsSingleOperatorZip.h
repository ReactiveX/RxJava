//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/operators/SingleOperatorZip.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_RxInternalOperatorsSingleOperatorZip")
#ifdef RESTRICT_RxInternalOperatorsSingleOperatorZip
#define INCLUDE_ALL_RxInternalOperatorsSingleOperatorZip 0
#else
#define INCLUDE_ALL_RxInternalOperatorsSingleOperatorZip 1
#endif
#undef RESTRICT_RxInternalOperatorsSingleOperatorZip

#if !defined (RxInternalOperatorsSingleOperatorZip_) && (INCLUDE_ALL_RxInternalOperatorsSingleOperatorZip || defined(INCLUDE_RxInternalOperatorsSingleOperatorZip))
#define RxInternalOperatorsSingleOperatorZip_

@class IOSObjectArray;
@class RxSingle;
@protocol RxFunctionsFuncN;

@interface RxInternalOperatorsSingleOperatorZip : NSObject

#pragma mark Public

+ (RxSingle *)zipWithRxSingleArray:(IOSObjectArray *)singles
              withRxFunctionsFuncN:(id<RxFunctionsFuncN>)zipper;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalOperatorsSingleOperatorZip)

FOUNDATION_EXPORT RxSingle *RxInternalOperatorsSingleOperatorZip_zipWithRxSingleArray_withRxFunctionsFuncN_(IOSObjectArray *singles, id<RxFunctionsFuncN> zipper);

J2OBJC_TYPE_LITERAL_HEADER(RxInternalOperatorsSingleOperatorZip)

#endif

#pragma pop_macro("INCLUDE_ALL_RxInternalOperatorsSingleOperatorZip")
