//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/subjects/BehaviorSubject.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxExceptionsExceptions.h"
#include "RxFunctionsAction1.h"
#include "RxInternalOperatorsNotificationLite.h"
#include "RxObservable.h"
#include "RxSubjectsBehaviorSubject.h"
#include "RxSubjectsSubject.h"
#include "RxSubjectsSubjectSubscriptionManager.h"
#include "java/lang/reflect/Array.h"
#include "java/util/ArrayList.h"
#include "java/util/List.h"

@interface RxSubjectsBehaviorSubject () {
 @public
  RxSubjectsSubjectSubscriptionManager *state_;
}

+ (RxSubjectsBehaviorSubject *)createWithId:(id)defaultValue
                                withBoolean:(jboolean)hasDefault;

@end

J2OBJC_FIELD_SETTER(RxSubjectsBehaviorSubject, state_, RxSubjectsSubjectSubscriptionManager *)

inline IOSObjectArray *RxSubjectsBehaviorSubject_get_EMPTY_ARRAY();
static IOSObjectArray *RxSubjectsBehaviorSubject_EMPTY_ARRAY;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxSubjectsBehaviorSubject, EMPTY_ARRAY, IOSObjectArray *)

__attribute__((unused)) static RxSubjectsBehaviorSubject *RxSubjectsBehaviorSubject_createWithId_withBoolean_(id defaultValue, jboolean hasDefault);

@interface RxSubjectsBehaviorSubject_$1 : NSObject < RxFunctionsAction1 > {
 @public
  RxSubjectsSubjectSubscriptionManager *val$state_;
}

- (void)callWithId:(RxSubjectsSubjectSubscriptionManager_SubjectObserver *)o;

- (instancetype)initWithRxSubjectsSubjectSubscriptionManager:(RxSubjectsSubjectSubscriptionManager *)capture$0;

@end

J2OBJC_EMPTY_STATIC_INIT(RxSubjectsBehaviorSubject_$1)

J2OBJC_FIELD_SETTER(RxSubjectsBehaviorSubject_$1, val$state_, RxSubjectsSubjectSubscriptionManager *)

__attribute__((unused)) static void RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(RxSubjectsBehaviorSubject_$1 *self, RxSubjectsSubjectSubscriptionManager *capture$0);

__attribute__((unused)) static RxSubjectsBehaviorSubject_$1 *new_RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(RxSubjectsSubjectSubscriptionManager *capture$0) NS_RETURNS_RETAINED;

__attribute__((unused)) static RxSubjectsBehaviorSubject_$1 *create_RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(RxSubjectsSubjectSubscriptionManager *capture$0);

J2OBJC_INITIALIZED_DEFN(RxSubjectsBehaviorSubject)

@implementation RxSubjectsBehaviorSubject

+ (RxSubjectsBehaviorSubject *)create {
  return RxSubjectsBehaviorSubject_create();
}

+ (RxSubjectsBehaviorSubject *)createWithId:(id)defaultValue {
  return RxSubjectsBehaviorSubject_createWithId_(defaultValue);
}

+ (RxSubjectsBehaviorSubject *)createWithId:(id)defaultValue
                                withBoolean:(jboolean)hasDefault {
  return RxSubjectsBehaviorSubject_createWithId_withBoolean_(defaultValue, hasDefault);
}

- (instancetype)initWithRxObservable_OnSubscribe:(id<RxObservable_OnSubscribe>)onSubscribe
        withRxSubjectsSubjectSubscriptionManager:(RxSubjectsSubjectSubscriptionManager *)state {
  RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(self, onSubscribe, state);
  return self;
}

- (void)onCompleted {
  id last = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  if (last == nil || state_->active_) {
    id n = RxInternalOperatorsNotificationLite_completed();
    {
      IOSObjectArray *a__ = [state_ terminateWithId:n];
      RxSubjectsSubjectSubscriptionManager_SubjectObserver * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
      RxSubjectsSubjectSubscriptionManager_SubjectObserver * const *e__ = b__ + a__->size_;
      while (b__ < e__) {
        RxSubjectsSubjectSubscriptionManager_SubjectObserver *bo = *b__++;
        [((RxSubjectsSubjectSubscriptionManager_SubjectObserver *) nil_chk(bo)) emitNextWithId:n];
      }
    }
  }
}

- (void)onErrorWithNSException:(NSException *)e {
  id last = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  if (last == nil || state_->active_) {
    id n = RxInternalOperatorsNotificationLite_errorWithNSException_(e);
    id<JavaUtilList> errors = nil;
    {
      IOSObjectArray *a__ = [state_ terminateWithId:n];
      RxSubjectsSubjectSubscriptionManager_SubjectObserver * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
      RxSubjectsSubjectSubscriptionManager_SubjectObserver * const *e__ = b__ + a__->size_;
      while (b__ < e__) {
        RxSubjectsSubjectSubscriptionManager_SubjectObserver *bo = *b__++;
        @try {
          [((RxSubjectsSubjectSubscriptionManager_SubjectObserver *) nil_chk(bo)) emitNextWithId:n];
        }
        @catch (NSException *e2) {
          if (errors == nil) {
            errors = create_JavaUtilArrayList_init();
          }
          [errors addWithId:e2];
        }
      }
    }
    RxExceptionsExceptions_throwIfAnyWithJavaUtilList_(errors);
  }
}

- (void)onNextWithId:(id)v {
  id last = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  if (last == nil || state_->active_) {
    id n = RxInternalOperatorsNotificationLite_nextWithId_(v);
    {
      IOSObjectArray *a__ = [state_ nextWithId:n];
      RxSubjectsSubjectSubscriptionManager_SubjectObserver * const *b__ = ((IOSObjectArray *) nil_chk(a__))->buffer_;
      RxSubjectsSubjectSubscriptionManager_SubjectObserver * const *e__ = b__ + a__->size_;
      while (b__ < e__) {
        RxSubjectsSubjectSubscriptionManager_SubjectObserver *bo = *b__++;
        [((RxSubjectsSubjectSubscriptionManager_SubjectObserver *) nil_chk(bo)) emitNextWithId:n];
      }
    }
  }
}

- (jint)subscriberCount {
  return ((IOSObjectArray *) nil_chk([((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) observers]))->size_;
}

- (jboolean)hasObservers {
  return ((IOSObjectArray *) nil_chk([((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) observers]))->size_ > 0;
}

- (jboolean)hasValue {
  id o = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  return RxInternalOperatorsNotificationLite_isNextWithId_(o);
}

- (jboolean)hasThrowable {
  id o = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  return RxInternalOperatorsNotificationLite_isErrorWithId_(o);
}

- (jboolean)hasCompleted {
  id o = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  return RxInternalOperatorsNotificationLite_isCompletedWithId_(o);
}

- (id)getValue {
  id o = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  if (RxInternalOperatorsNotificationLite_isNextWithId_(o)) {
    return RxInternalOperatorsNotificationLite_getValueWithId_(o);
  }
  return nil;
}

- (NSException *)getThrowable {
  id o = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  if (RxInternalOperatorsNotificationLite_isErrorWithId_(o)) {
    return RxInternalOperatorsNotificationLite_getErrorWithId_(o);
  }
  return nil;
}

- (IOSObjectArray *)getValuesWithNSObjectArray:(IOSObjectArray *)a {
  id o = [((RxSubjectsSubjectSubscriptionManager *) nil_chk(state_)) getLatest];
  if (RxInternalOperatorsNotificationLite_isNextWithId_(o)) {
    if (((IOSObjectArray *) nil_chk(a))->size_ == 0) {
      a = (IOSObjectArray *) cast_check(JavaLangReflectArray_newInstanceWithIOSClass_withInt_([[a java_getClass] getComponentType], 1), IOSClass_arrayType(NSObject_class_(), 1));
    }
    IOSObjectArray_Set(nil_chk(a), 0, RxInternalOperatorsNotificationLite_getValueWithId_(o));
    if (a->size_ > 1) {
      IOSObjectArray_Set(a, 1, nil);
    }
  }
  else if (((IOSObjectArray *) nil_chk(a))->size_ > 0) {
    IOSObjectArray_Set(a, 0, nil);
  }
  return a;
}

- (IOSObjectArray *)getValues {
  IOSObjectArray *r = [self getValuesWithNSObjectArray:RxSubjectsBehaviorSubject_EMPTY_ARRAY];
  if (r == RxSubjectsBehaviorSubject_EMPTY_ARRAY) {
    return [IOSObjectArray arrayWithLength:0 type:NSObject_class_()];
  }
  return r;
}

- (void)dealloc {
  RELEASE_(state_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxSubjectsBehaviorSubject;", 0x9, -1, -1, -1, 0, -1, -1 },
    { NULL, "LRxSubjectsBehaviorSubject;", 0x9, 1, 2, -1, 3, -1, -1 },
    { NULL, "LRxSubjectsBehaviorSubject;", 0xa, 1, 4, -1, 5, -1, -1 },
    { NULL, NULL, 0x4, -1, 6, -1, 7, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 8, 9, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 10, 2, -1, 11, -1, -1 },
    { NULL, "I", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "LNSObject;", 0x1, -1, -1, -1, 12, -1, -1 },
    { NULL, "LNSException;", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "[LNSObject;", 0x1, 13, 14, -1, -1, -1, -1 },
    { NULL, "[LNSObject;", 0x1, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(create);
  methods[1].selector = @selector(createWithId:);
  methods[2].selector = @selector(createWithId:withBoolean:);
  methods[3].selector = @selector(initWithRxObservable_OnSubscribe:withRxSubjectsSubjectSubscriptionManager:);
  methods[4].selector = @selector(onCompleted);
  methods[5].selector = @selector(onErrorWithNSException:);
  methods[6].selector = @selector(onNextWithId:);
  methods[7].selector = @selector(subscriberCount);
  methods[8].selector = @selector(hasObservers);
  methods[9].selector = @selector(hasValue);
  methods[10].selector = @selector(hasThrowable);
  methods[11].selector = @selector(hasCompleted);
  methods[12].selector = @selector(getValue);
  methods[13].selector = @selector(getThrowable);
  methods[14].selector = @selector(getValuesWithNSObjectArray:);
  methods[15].selector = @selector(getValues);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "EMPTY_ARRAY", "[LNSObject;", .constantValue.asLong = 0, 0x1a, -1, 15, -1, -1 },
    { "state_", "LRxSubjectsSubjectSubscriptionManager;", .constantValue.asLong = 0, 0x12, -1, -1, 16, -1 },
  };
  static const void *ptrTable[] = { "<T:Ljava/lang/Object;>()Lrx/subjects/BehaviorSubject<TT;>;", "create", "LNSObject;", "<T:Ljava/lang/Object;>(TT;)Lrx/subjects/BehaviorSubject<TT;>;", "LNSObject;Z", "<T:Ljava/lang/Object;>(TT;Z)Lrx/subjects/BehaviorSubject<TT;>;", "LRxObservable_OnSubscribe;LRxSubjectsSubjectSubscriptionManager;", "(Lrx/Observable$OnSubscribe<TT;>;Lrx/subjects/SubjectSubscriptionManager<TT;>;)V", "onError", "LNSException;", "onNext", "(TT;)V", "()TT;", "getValues", "[LNSObject;", &RxSubjectsBehaviorSubject_EMPTY_ARRAY, "Lrx/subjects/SubjectSubscriptionManager<TT;>;", "<T:Ljava/lang/Object;>Lrx/subjects/Subject<TT;TT;>;" };
  static const J2ObjcClassInfo _RxSubjectsBehaviorSubject = { "BehaviorSubject", "rx.subjects", ptrTable, methods, fields, 7, 0x11, 16, 2, -1, -1, -1, 17, -1 };
  return &_RxSubjectsBehaviorSubject;
}

+ (void)initialize {
  if (self == [RxSubjectsBehaviorSubject class]) {
    JreStrongAssignAndConsume(&RxSubjectsBehaviorSubject_EMPTY_ARRAY, [IOSObjectArray newArrayWithLength:0 type:NSObject_class_()]);
    J2OBJC_SET_INITIALIZED(RxSubjectsBehaviorSubject)
  }
}

@end

RxSubjectsBehaviorSubject *RxSubjectsBehaviorSubject_create() {
  RxSubjectsBehaviorSubject_initialize();
  return RxSubjectsBehaviorSubject_createWithId_withBoolean_(nil, false);
}

RxSubjectsBehaviorSubject *RxSubjectsBehaviorSubject_createWithId_(id defaultValue) {
  RxSubjectsBehaviorSubject_initialize();
  return RxSubjectsBehaviorSubject_createWithId_withBoolean_(defaultValue, true);
}

RxSubjectsBehaviorSubject *RxSubjectsBehaviorSubject_createWithId_withBoolean_(id defaultValue, jboolean hasDefault) {
  RxSubjectsBehaviorSubject_initialize();
  RxSubjectsSubjectSubscriptionManager *state = create_RxSubjectsSubjectSubscriptionManager_init();
  if (hasDefault) {
    [state setLatestWithId:RxInternalOperatorsNotificationLite_nextWithId_(defaultValue)];
  }
  JreStrongAssignAndConsume(&state->onAdded_, new_RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(state));
  JreStrongAssign(&state->onTerminated_, state->onAdded_);
  return create_RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(state, state);
}

void RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(RxSubjectsBehaviorSubject *self, id<RxObservable_OnSubscribe> onSubscribe, RxSubjectsSubjectSubscriptionManager *state) {
  RxSubjectsSubject_initWithRxObservable_OnSubscribe_(self, onSubscribe);
  JreStrongAssign(&self->state_, state);
}

RxSubjectsBehaviorSubject *new_RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(id<RxObservable_OnSubscribe> onSubscribe, RxSubjectsSubjectSubscriptionManager *state) {
  J2OBJC_NEW_IMPL(RxSubjectsBehaviorSubject, initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_, onSubscribe, state)
}

RxSubjectsBehaviorSubject *create_RxSubjectsBehaviorSubject_initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_(id<RxObservable_OnSubscribe> onSubscribe, RxSubjectsSubjectSubscriptionManager *state) {
  J2OBJC_CREATE_IMPL(RxSubjectsBehaviorSubject, initWithRxObservable_OnSubscribe_withRxSubjectsSubjectSubscriptionManager_, onSubscribe, state)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxSubjectsBehaviorSubject)

@implementation RxSubjectsBehaviorSubject_$1

- (void)callWithId:(RxSubjectsSubjectSubscriptionManager_SubjectObserver *)o {
  [((RxSubjectsSubjectSubscriptionManager_SubjectObserver *) nil_chk(o)) emitFirstWithId:[((RxSubjectsSubjectSubscriptionManager *) nil_chk(val$state_)) getLatest]];
}

- (instancetype)initWithRxSubjectsSubjectSubscriptionManager:(RxSubjectsSubjectSubscriptionManager *)capture$0 {
  RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(self, capture$0);
  return self;
}

- (void)dealloc {
  RELEASE_(val$state_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "V", 0x1, 0, 1, -1, 2, -1, -1 },
    { NULL, NULL, 0x0, -1, 3, -1, 4, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(callWithId:);
  methods[1].selector = @selector(initWithRxSubjectsSubjectSubscriptionManager:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "val$state_", "LRxSubjectsSubjectSubscriptionManager;", .constantValue.asLong = 0, 0x1012, -1, -1, 5, -1 },
  };
  static const void *ptrTable[] = { "call", "LRxSubjectsSubjectSubscriptionManager_SubjectObserver;", "(Lrx/subjects/SubjectSubscriptionManager$SubjectObserver<TT;>;)V", "LRxSubjectsSubjectSubscriptionManager;", "(Lrx/subjects/SubjectSubscriptionManager<TT;>;)V", "Lrx/subjects/SubjectSubscriptionManager<TT;>;", "LRxSubjectsBehaviorSubject;", "createWithId:withBoolean:", "Ljava/lang/Object;Lrx/functions/Action1<Lrx/subjects/SubjectSubscriptionManager$SubjectObserver<TT;>;>;" };
  static const J2ObjcClassInfo _RxSubjectsBehaviorSubject_$1 = { "", "rx.subjects", ptrTable, methods, fields, 7, 0x8008, 2, 1, 6, -1, 7, 8, -1 };
  return &_RxSubjectsBehaviorSubject_$1;
}

@end

void RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(RxSubjectsBehaviorSubject_$1 *self, RxSubjectsSubjectSubscriptionManager *capture$0) {
  JreStrongAssign(&self->val$state_, capture$0);
  NSObject_init(self);
}

RxSubjectsBehaviorSubject_$1 *new_RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(RxSubjectsSubjectSubscriptionManager *capture$0) {
  J2OBJC_NEW_IMPL(RxSubjectsBehaviorSubject_$1, initWithRxSubjectsSubjectSubscriptionManager_, capture$0)
}

RxSubjectsBehaviorSubject_$1 *create_RxSubjectsBehaviorSubject_$1_initWithRxSubjectsSubjectSubscriptionManager_(RxSubjectsSubjectSubscriptionManager *capture$0) {
  J2OBJC_CREATE_IMPL(RxSubjectsBehaviorSubject_$1, initWithRxSubjectsSubjectSubscriptionManager_, capture$0)
}
