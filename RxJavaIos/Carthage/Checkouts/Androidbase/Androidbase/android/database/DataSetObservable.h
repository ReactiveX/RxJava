//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/core-doppl/androidbase/src/main/java/android/database/DataSetObservable.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_AndroidDatabaseDataSetObservable")
#ifdef RESTRICT_AndroidDatabaseDataSetObservable
#define INCLUDE_ALL_AndroidDatabaseDataSetObservable 0
#else
#define INCLUDE_ALL_AndroidDatabaseDataSetObservable 1
#endif
#undef RESTRICT_AndroidDatabaseDataSetObservable

#if !defined (AndroidDatabaseDataSetObservable_) && (INCLUDE_ALL_AndroidDatabaseDataSetObservable || defined(INCLUDE_AndroidDatabaseDataSetObservable))
#define AndroidDatabaseDataSetObservable_

#define RESTRICT_AndroidDatabaseObservable 1
#define INCLUDE_AndroidDatabaseObservable 1
#include "android/database/Observable.h"

@interface AndroidDatabaseDataSetObservable : AndroidDatabaseObservable

#pragma mark Public

- (instancetype)init;

- (void)notifyChanged;

- (void)notifyInvalidated;

@end

J2OBJC_EMPTY_STATIC_INIT(AndroidDatabaseDataSetObservable)

FOUNDATION_EXPORT void AndroidDatabaseDataSetObservable_init(AndroidDatabaseDataSetObservable *self);

FOUNDATION_EXPORT AndroidDatabaseDataSetObservable *new_AndroidDatabaseDataSetObservable_init() NS_RETURNS_RETAINED;

FOUNDATION_EXPORT AndroidDatabaseDataSetObservable *create_AndroidDatabaseDataSetObservable_init();

J2OBJC_TYPE_LITERAL_HEADER(AndroidDatabaseDataSetObservable)

#endif

#pragma pop_macro("INCLUDE_ALL_AndroidDatabaseDataSetObservable")
