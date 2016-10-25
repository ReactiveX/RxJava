//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/IndexedRingBuffer.java
//

#include "IOSClass.h"
#include "J2ObjC_source.h"
#include "RxFunctionsFunc1.h"
#include "RxInternalUtilIndexedRingBuffer.h"
#include "RxInternalUtilObjectPool.h"
#include "RxInternalUtilPlatformDependent.h"
#include "java/io/PrintStream.h"
#include "java/lang/Boolean.h"
#include "java/lang/Integer.h"
#include "java/lang/NumberFormatException.h"
#include "java/lang/System.h"
#include "java/util/concurrent/atomic/AtomicInteger.h"
#include "java/util/concurrent/atomic/AtomicIntegerArray.h"
#include "java/util/concurrent/atomic/AtomicReference.h"
#include "java/util/concurrent/atomic/AtomicReferenceArray.h"

@interface RxInternalUtilIndexedRingBuffer () {
 @public
  RxInternalUtilIndexedRingBuffer_ElementSection *elements_;
  RxInternalUtilIndexedRingBuffer_IndexSection *removed_;
}

- (RxInternalUtilIndexedRingBuffer_IndexSection *)getIndexSectionWithInt:(jint)index;

- (RxInternalUtilIndexedRingBuffer_ElementSection *)getElementSectionWithInt:(jint)index;

- (jint)getIndexForAdd;

- (jint)getIndexFromPreviouslyRemoved;

- (void)pushRemovedIndexWithInt:(jint)elementIndex;

- (jint)forEachWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)action
                            withInt:(jint)startIndex
                            withInt:(jint)endIndex;

@end

J2OBJC_FIELD_SETTER(RxInternalUtilIndexedRingBuffer, elements_, RxInternalUtilIndexedRingBuffer_ElementSection *)
J2OBJC_FIELD_SETTER(RxInternalUtilIndexedRingBuffer, removed_, RxInternalUtilIndexedRingBuffer_IndexSection *)

inline RxInternalUtilObjectPool *RxInternalUtilIndexedRingBuffer_get_POOL();
static RxInternalUtilObjectPool *RxInternalUtilIndexedRingBuffer_POOL;
J2OBJC_STATIC_FIELD_OBJ_FINAL(RxInternalUtilIndexedRingBuffer, POOL, RxInternalUtilObjectPool *)

__attribute__((unused)) static RxInternalUtilIndexedRingBuffer_IndexSection *RxInternalUtilIndexedRingBuffer_getIndexSectionWithInt_(RxInternalUtilIndexedRingBuffer *self, jint index);

__attribute__((unused)) static RxInternalUtilIndexedRingBuffer_ElementSection *RxInternalUtilIndexedRingBuffer_getElementSectionWithInt_(RxInternalUtilIndexedRingBuffer *self, jint index);

__attribute__((unused)) static jint RxInternalUtilIndexedRingBuffer_getIndexForAdd(RxInternalUtilIndexedRingBuffer *self);

__attribute__((unused)) static jint RxInternalUtilIndexedRingBuffer_getIndexFromPreviouslyRemoved(RxInternalUtilIndexedRingBuffer *self);

__attribute__((unused)) static void RxInternalUtilIndexedRingBuffer_pushRemovedIndexWithInt_(RxInternalUtilIndexedRingBuffer *self, jint elementIndex);

__attribute__((unused)) static jint RxInternalUtilIndexedRingBuffer_forEachWithRxFunctionsFunc1_withInt_withInt_(RxInternalUtilIndexedRingBuffer *self, id<RxFunctionsFunc1> action, jint startIndex, jint endIndex);

@interface RxInternalUtilIndexedRingBuffer_IndexSection () {
 @public
  JavaUtilConcurrentAtomicAtomicIntegerArray *unsafeArray_;
  JavaUtilConcurrentAtomicAtomicReference *_next_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalUtilIndexedRingBuffer_IndexSection, unsafeArray_, JavaUtilConcurrentAtomicAtomicIntegerArray *)
J2OBJC_FIELD_SETTER(RxInternalUtilIndexedRingBuffer_IndexSection, _next_, JavaUtilConcurrentAtomicAtomicReference *)

@interface RxInternalUtilIndexedRingBuffer_$1 : RxInternalUtilObjectPool

- (RxInternalUtilIndexedRingBuffer *)createObject;

- (instancetype)init;

@end

J2OBJC_EMPTY_STATIC_INIT(RxInternalUtilIndexedRingBuffer_$1)

__attribute__((unused)) static void RxInternalUtilIndexedRingBuffer_$1_init(RxInternalUtilIndexedRingBuffer_$1 *self);

__attribute__((unused)) static RxInternalUtilIndexedRingBuffer_$1 *new_RxInternalUtilIndexedRingBuffer_$1_init() NS_RETURNS_RETAINED;

__attribute__((unused)) static RxInternalUtilIndexedRingBuffer_$1 *create_RxInternalUtilIndexedRingBuffer_$1_init();

J2OBJC_INITIALIZED_DEFN(RxInternalUtilIndexedRingBuffer)

jint RxInternalUtilIndexedRingBuffer_SIZE;

@implementation RxInternalUtilIndexedRingBuffer

+ (RxInternalUtilIndexedRingBuffer *)getInstance {
  return RxInternalUtilIndexedRingBuffer_getInstance();
}

- (void)releaseToPool {
  jint maxIndex = [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(index_)) get];
  jint realIndex = 0;
  RxInternalUtilIndexedRingBuffer_ElementSection *section = elements_;
  while (section != nil) {
    for (jint i = 0; i < RxInternalUtilIndexedRingBuffer_SIZE; i++, realIndex++) {
      if (realIndex >= maxIndex) {
        goto break_outer;
      }
      [((JavaUtilConcurrentAtomicAtomicReferenceArray *) nil_chk(section->array_)) setWithInt:i withId:nil];
    }
    section = [((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(section->next_)) get];
  }
  break_outer: ;
  [index_ setWithInt:0];
  [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(removedIndex_)) setWithInt:0];
  [((RxInternalUtilObjectPool *) nil_chk(RxInternalUtilIndexedRingBuffer_POOL)) returnObjectWithId:self];
}

- (void)unsubscribe {
  [self releaseToPool];
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilIndexedRingBuffer_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (jint)addWithId:(id)e {
  jint i = RxInternalUtilIndexedRingBuffer_getIndexForAdd(self);
  if (i < RxInternalUtilIndexedRingBuffer_SIZE) {
    [((JavaUtilConcurrentAtomicAtomicReferenceArray *) nil_chk(((RxInternalUtilIndexedRingBuffer_ElementSection *) nil_chk(elements_))->array_)) setWithInt:i withId:e];
    return i;
  }
  else {
    jint sectionIndex = i % RxInternalUtilIndexedRingBuffer_SIZE;
    [((JavaUtilConcurrentAtomicAtomicReferenceArray *) nil_chk(((RxInternalUtilIndexedRingBuffer_ElementSection *) nil_chk(RxInternalUtilIndexedRingBuffer_getElementSectionWithInt_(self, i)))->array_)) setWithInt:sectionIndex withId:e];
    return i;
  }
}

- (id)removeWithInt:(jint)index {
  id e;
  if (index < RxInternalUtilIndexedRingBuffer_SIZE) {
    e = [((JavaUtilConcurrentAtomicAtomicReferenceArray *) nil_chk(((RxInternalUtilIndexedRingBuffer_ElementSection *) nil_chk(elements_))->array_)) getAndSetWithInt:index withId:nil];
  }
  else {
    jint sectionIndex = index % RxInternalUtilIndexedRingBuffer_SIZE;
    e = [((JavaUtilConcurrentAtomicAtomicReferenceArray *) nil_chk(((RxInternalUtilIndexedRingBuffer_ElementSection *) nil_chk(RxInternalUtilIndexedRingBuffer_getElementSectionWithInt_(self, index)))->array_)) getAndSetWithInt:sectionIndex withId:nil];
  }
  RxInternalUtilIndexedRingBuffer_pushRemovedIndexWithInt_(self, index);
  return e;
}

- (RxInternalUtilIndexedRingBuffer_IndexSection *)getIndexSectionWithInt:(jint)index {
  return RxInternalUtilIndexedRingBuffer_getIndexSectionWithInt_(self, index);
}

- (RxInternalUtilIndexedRingBuffer_ElementSection *)getElementSectionWithInt:(jint)index {
  return RxInternalUtilIndexedRingBuffer_getElementSectionWithInt_(self, index);
}

- (jint)getIndexForAdd {
  return RxInternalUtilIndexedRingBuffer_getIndexForAdd(self);
}

- (jint)getIndexFromPreviouslyRemoved {
  return RxInternalUtilIndexedRingBuffer_getIndexFromPreviouslyRemoved(self);
}

- (void)pushRemovedIndexWithInt:(jint)elementIndex {
  RxInternalUtilIndexedRingBuffer_pushRemovedIndexWithInt_(self, elementIndex);
}

- (jboolean)isUnsubscribed {
  return false;
}

- (jint)forEachWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)action {
  return [self forEachWithRxFunctionsFunc1:action withInt:0];
}

- (jint)forEachWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)action
                            withInt:(jint)startIndex {
  jint endedAt = RxInternalUtilIndexedRingBuffer_forEachWithRxFunctionsFunc1_withInt_withInt_(self, action, startIndex, [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(index_)) get]);
  if (startIndex > 0 && endedAt == [index_ get]) {
    endedAt = RxInternalUtilIndexedRingBuffer_forEachWithRxFunctionsFunc1_withInt_withInt_(self, action, 0, startIndex);
  }
  else if (endedAt == [index_ get]) {
    endedAt = 0;
  }
  return endedAt;
}

- (jint)forEachWithRxFunctionsFunc1:(id<RxFunctionsFunc1>)action
                            withInt:(jint)startIndex
                            withInt:(jint)endIndex {
  return RxInternalUtilIndexedRingBuffer_forEachWithRxFunctionsFunc1_withInt_withInt_(self, action, startIndex, endIndex);
}

- (void)dealloc {
  RELEASE_(elements_);
  RELEASE_(removed_);
  RELEASE_(index_);
  RELEASE_(removedIndex_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalUtilIndexedRingBuffer;", 0x9, -1, -1, -1, 0, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, 1, 2, -1, 3, -1, -1 },
    { NULL, "LNSObject;", 0x1, 4, 5, -1, 6, -1, -1 },
    { NULL, "LRxInternalUtilIndexedRingBuffer_IndexSection;", 0x2, 7, 5, -1, -1, -1, -1 },
    { NULL, "LRxInternalUtilIndexedRingBuffer_ElementSection;", 0x2, 8, 5, -1, 9, -1, -1 },
    { NULL, "I", 0x22, -1, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x22, -1, -1, -1, -1, -1, -1 },
    { NULL, "V", 0x22, 10, 5, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, 11, 12, -1, 13, -1, -1 },
    { NULL, "I", 0x1, 11, 14, -1, 15, -1, -1 },
    { NULL, "I", 0x2, 11, 16, -1, 17, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(getInstance);
  methods[1].selector = @selector(releaseToPool);
  methods[2].selector = @selector(unsubscribe);
  methods[3].selector = @selector(init);
  methods[4].selector = @selector(addWithId:);
  methods[5].selector = @selector(removeWithInt:);
  methods[6].selector = @selector(getIndexSectionWithInt:);
  methods[7].selector = @selector(getElementSectionWithInt:);
  methods[8].selector = @selector(getIndexForAdd);
  methods[9].selector = @selector(getIndexFromPreviouslyRemoved);
  methods[10].selector = @selector(pushRemovedIndexWithInt:);
  methods[11].selector = @selector(isUnsubscribed);
  methods[12].selector = @selector(forEachWithRxFunctionsFunc1:);
  methods[13].selector = @selector(forEachWithRxFunctionsFunc1:withInt:);
  methods[14].selector = @selector(forEachWithRxFunctionsFunc1:withInt:withInt:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "elements_", "LRxInternalUtilIndexedRingBuffer_ElementSection;", .constantValue.asLong = 0, 0x12, -1, -1, 18, -1 },
    { "removed_", "LRxInternalUtilIndexedRingBuffer_IndexSection;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "index_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "removedIndex_", "LJavaUtilConcurrentAtomicAtomicInteger;", .constantValue.asLong = 0, 0x10, -1, -1, -1, -1 },
    { "POOL", "LRxInternalUtilObjectPool;", .constantValue.asLong = 0, 0x1a, -1, 19, 20, -1 },
    { "SIZE", "I", .constantValue.asLong = 0, 0x18, -1, 21, -1, -1 },
  };
  static const void *ptrTable[] = { "<T:Ljava/lang/Object;>()Lrx/internal/util/IndexedRingBuffer<TT;>;", "add", "LNSObject;", "(TE;)I", "remove", "I", "(I)TE;", "getIndexSection", "getElementSection", "(I)Lrx/internal/util/IndexedRingBuffer$ElementSection<TE;>;", "pushRemovedIndex", "forEach", "LRxFunctionsFunc1;", "(Lrx/functions/Func1<-TE;Ljava/lang/Boolean;>;)I", "LRxFunctionsFunc1;I", "(Lrx/functions/Func1<-TE;Ljava/lang/Boolean;>;I)I", "LRxFunctionsFunc1;II", "(Lrx/functions/Func1<-TE;Ljava/lang/Boolean;>;II)I", "Lrx/internal/util/IndexedRingBuffer$ElementSection<TE;>;", &RxInternalUtilIndexedRingBuffer_POOL, "Lrx/internal/util/ObjectPool<Lrx/internal/util/IndexedRingBuffer<*>;>;", &RxInternalUtilIndexedRingBuffer_SIZE, "LRxInternalUtilIndexedRingBuffer_ElementSection;LRxInternalUtilIndexedRingBuffer_IndexSection;", "<E:Ljava/lang/Object;>Ljava/lang/Object;Lrx/Subscription;" };
  static const J2ObjcClassInfo _RxInternalUtilIndexedRingBuffer = { "IndexedRingBuffer", "rx.internal.util", ptrTable, methods, fields, 7, 0x11, 15, 6, -1, 22, -1, 23, -1 };
  return &_RxInternalUtilIndexedRingBuffer;
}

+ (void)initialize {
  if (self == [RxInternalUtilIndexedRingBuffer class]) {
    JreStrongAssignAndConsume(&RxInternalUtilIndexedRingBuffer_POOL, new_RxInternalUtilIndexedRingBuffer_$1_init());
    {
      jint defaultSize = 128;
      if (RxInternalUtilPlatformDependent_isAndroid()) {
        defaultSize = 8;
      }
      NSString *sizeFromProperty = JavaLangSystem_getPropertyWithNSString_(@"rx.indexed-ring-buffer.size");
      if (sizeFromProperty != nil) {
        @try {
          defaultSize = JavaLangInteger_parseIntWithNSString_(sizeFromProperty);
        }
        @catch (JavaLangNumberFormatException *e) {
          [((JavaIoPrintStream *) nil_chk(JreLoadStatic(JavaLangSystem, err))) printlnWithNSString:JreStrcat("$$$$", @"Failed to set 'rx.indexed-ring-buffer.size' with value ", sizeFromProperty, @" => ", [((JavaLangNumberFormatException *) nil_chk(e)) getMessage])];
        }
      }
      RxInternalUtilIndexedRingBuffer_SIZE = defaultSize;
    }
    J2OBJC_SET_INITIALIZED(RxInternalUtilIndexedRingBuffer)
  }
}

@end

RxInternalUtilIndexedRingBuffer *RxInternalUtilIndexedRingBuffer_getInstance() {
  RxInternalUtilIndexedRingBuffer_initialize();
  return [((RxInternalUtilObjectPool *) nil_chk(RxInternalUtilIndexedRingBuffer_POOL)) borrowObject];
}

void RxInternalUtilIndexedRingBuffer_init(RxInternalUtilIndexedRingBuffer *self) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->elements_, new_RxInternalUtilIndexedRingBuffer_ElementSection_init());
  JreStrongAssignAndConsume(&self->removed_, new_RxInternalUtilIndexedRingBuffer_IndexSection_init());
  JreStrongAssignAndConsume(&self->index_, new_JavaUtilConcurrentAtomicAtomicInteger_init());
  JreStrongAssignAndConsume(&self->removedIndex_, new_JavaUtilConcurrentAtomicAtomicInteger_init());
}

RxInternalUtilIndexedRingBuffer *new_RxInternalUtilIndexedRingBuffer_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilIndexedRingBuffer, init)
}

RxInternalUtilIndexedRingBuffer *create_RxInternalUtilIndexedRingBuffer_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilIndexedRingBuffer, init)
}

RxInternalUtilIndexedRingBuffer_IndexSection *RxInternalUtilIndexedRingBuffer_getIndexSectionWithInt_(RxInternalUtilIndexedRingBuffer *self, jint index) {
  if (index < RxInternalUtilIndexedRingBuffer_SIZE) {
    return self->removed_;
  }
  jint numSections = index / RxInternalUtilIndexedRingBuffer_SIZE;
  RxInternalUtilIndexedRingBuffer_IndexSection *a = self->removed_;
  for (jint i = 0; i < numSections; i++) {
    a = [((RxInternalUtilIndexedRingBuffer_IndexSection *) nil_chk(a)) getNext];
  }
  return a;
}

RxInternalUtilIndexedRingBuffer_ElementSection *RxInternalUtilIndexedRingBuffer_getElementSectionWithInt_(RxInternalUtilIndexedRingBuffer *self, jint index) {
  if (index < RxInternalUtilIndexedRingBuffer_SIZE) {
    return self->elements_;
  }
  jint numSections = index / RxInternalUtilIndexedRingBuffer_SIZE;
  RxInternalUtilIndexedRingBuffer_ElementSection *a = self->elements_;
  for (jint i = 0; i < numSections; i++) {
    a = [((RxInternalUtilIndexedRingBuffer_ElementSection *) nil_chk(a)) getNext];
  }
  return a;
}

jint RxInternalUtilIndexedRingBuffer_getIndexForAdd(RxInternalUtilIndexedRingBuffer *self) {
  @synchronized(self) {
    jint i;
    jint ri = RxInternalUtilIndexedRingBuffer_getIndexFromPreviouslyRemoved(self);
    if (ri >= 0) {
      if (ri < RxInternalUtilIndexedRingBuffer_SIZE) {
        i = [((RxInternalUtilIndexedRingBuffer_IndexSection *) nil_chk(self->removed_)) getAndSetWithInt:ri withInt:-1];
      }
      else {
        jint sectionIndex = ri % RxInternalUtilIndexedRingBuffer_SIZE;
        i = [((RxInternalUtilIndexedRingBuffer_IndexSection *) nil_chk(RxInternalUtilIndexedRingBuffer_getIndexSectionWithInt_(self, ri))) getAndSetWithInt:sectionIndex withInt:-1];
      }
      if (i == [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(self->index_)) get]) {
        [self->index_ getAndIncrement];
      }
    }
    else {
      i = [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(self->index_)) getAndIncrement];
    }
    return i;
  }
}

jint RxInternalUtilIndexedRingBuffer_getIndexFromPreviouslyRemoved(RxInternalUtilIndexedRingBuffer *self) {
  @synchronized(self) {
    while (true) {
      jint currentRi = [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(self->removedIndex_)) get];
      if (currentRi > 0) {
        if ([self->removedIndex_ compareAndSetWithInt:currentRi withInt:currentRi - 1]) {
          return currentRi - 1;
        }
      }
      else {
        return -1;
      }
    }
  }
}

void RxInternalUtilIndexedRingBuffer_pushRemovedIndexWithInt_(RxInternalUtilIndexedRingBuffer *self, jint elementIndex) {
  @synchronized(self) {
    jint i = [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(self->removedIndex_)) getAndIncrement];
    if (i < RxInternalUtilIndexedRingBuffer_SIZE) {
      [((RxInternalUtilIndexedRingBuffer_IndexSection *) nil_chk(self->removed_)) setWithInt:i withInt:elementIndex];
    }
    else {
      jint sectionIndex = i % RxInternalUtilIndexedRingBuffer_SIZE;
      [((RxInternalUtilIndexedRingBuffer_IndexSection *) nil_chk(RxInternalUtilIndexedRingBuffer_getIndexSectionWithInt_(self, i))) setWithInt:sectionIndex withInt:elementIndex];
    }
  }
}

jint RxInternalUtilIndexedRingBuffer_forEachWithRxFunctionsFunc1_withInt_withInt_(RxInternalUtilIndexedRingBuffer *self, id<RxFunctionsFunc1> action, jint startIndex, jint endIndex) {
  jint lastIndex;
  jint maxIndex = [((JavaUtilConcurrentAtomicAtomicInteger *) nil_chk(self->index_)) get];
  jint realIndex = startIndex;
  RxInternalUtilIndexedRingBuffer_ElementSection *section = self->elements_;
  if (startIndex >= RxInternalUtilIndexedRingBuffer_SIZE) {
    section = RxInternalUtilIndexedRingBuffer_getElementSectionWithInt_(self, startIndex);
    startIndex = startIndex % RxInternalUtilIndexedRingBuffer_SIZE;
  }
  while (section != nil) {
    for (jint i = startIndex; i < RxInternalUtilIndexedRingBuffer_SIZE; i++, realIndex++) {
      if (realIndex >= maxIndex || realIndex >= endIndex) {
        goto break_outer;
      }
      id element = [((JavaUtilConcurrentAtomicAtomicReferenceArray *) nil_chk(section->array_)) getWithInt:i];
      if (element == nil) {
        continue;
      }
      lastIndex = realIndex;
      jboolean continueLoop = [((JavaLangBoolean *) nil_chk([((id<RxFunctionsFunc1>) nil_chk(action)) callWithId:element])) booleanValue];
      if (!continueLoop) {
        return lastIndex;
      }
    }
    section = [((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(section->next_)) get];
    startIndex = 0;
  }
  break_outer: ;
  return realIndex;
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilIndexedRingBuffer)

@implementation RxInternalUtilIndexedRingBuffer_ElementSection

- (RxInternalUtilIndexedRingBuffer_ElementSection *)getNext {
  if ([((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(next_)) get] != nil) {
    return [next_ get];
  }
  else {
    RxInternalUtilIndexedRingBuffer_ElementSection *newSection = create_RxInternalUtilIndexedRingBuffer_ElementSection_init();
    if ([next_ compareAndSetWithId:nil withId:newSection]) {
      return newSection;
    }
    else {
      return [next_ get];
    }
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilIndexedRingBuffer_ElementSection_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  RELEASE_(array_);
  RELEASE_(next_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalUtilIndexedRingBuffer_ElementSection;", 0x0, -1, -1, -1, 0, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(getNext);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "array_", "LJavaUtilConcurrentAtomicAtomicReferenceArray;", .constantValue.asLong = 0, 0x10, -1, -1, 1, -1 },
    { "next_", "LJavaUtilConcurrentAtomicAtomicReference;", .constantValue.asLong = 0, 0x10, -1, -1, 2, -1 },
  };
  static const void *ptrTable[] = { "()Lrx/internal/util/IndexedRingBuffer$ElementSection<TE;>;", "Ljava/util/concurrent/atomic/AtomicReferenceArray<TE;>;", "Ljava/util/concurrent/atomic/AtomicReference<Lrx/internal/util/IndexedRingBuffer$ElementSection<TE;>;>;", "LRxInternalUtilIndexedRingBuffer;", "<E:Ljava/lang/Object;>Ljava/lang/Object;" };
  static const J2ObjcClassInfo _RxInternalUtilIndexedRingBuffer_ElementSection = { "ElementSection", "rx.internal.util", ptrTable, methods, fields, 7, 0x18, 2, 2, 3, -1, -1, 4, -1 };
  return &_RxInternalUtilIndexedRingBuffer_ElementSection;
}

@end

void RxInternalUtilIndexedRingBuffer_ElementSection_init(RxInternalUtilIndexedRingBuffer_ElementSection *self) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->array_, new_JavaUtilConcurrentAtomicAtomicReferenceArray_initWithInt_(JreLoadStatic(RxInternalUtilIndexedRingBuffer, SIZE)));
  JreStrongAssignAndConsume(&self->next_, new_JavaUtilConcurrentAtomicAtomicReference_init());
}

RxInternalUtilIndexedRingBuffer_ElementSection *new_RxInternalUtilIndexedRingBuffer_ElementSection_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilIndexedRingBuffer_ElementSection, init)
}

RxInternalUtilIndexedRingBuffer_ElementSection *create_RxInternalUtilIndexedRingBuffer_ElementSection_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilIndexedRingBuffer_ElementSection, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilIndexedRingBuffer_ElementSection)

@implementation RxInternalUtilIndexedRingBuffer_IndexSection

- (jint)getAndSetWithInt:(jint)expected
                 withInt:(jint)newValue {
  return [((JavaUtilConcurrentAtomicAtomicIntegerArray *) nil_chk(unsafeArray_)) getAndSetWithInt:expected withInt:newValue];
}

- (void)setWithInt:(jint)i
           withInt:(jint)elementIndex {
  [((JavaUtilConcurrentAtomicAtomicIntegerArray *) nil_chk(unsafeArray_)) setWithInt:i withInt:elementIndex];
}

- (RxInternalUtilIndexedRingBuffer_IndexSection *)getNext {
  if ([((JavaUtilConcurrentAtomicAtomicReference *) nil_chk(_next_)) get] != nil) {
    return [_next_ get];
  }
  else {
    RxInternalUtilIndexedRingBuffer_IndexSection *newSection = create_RxInternalUtilIndexedRingBuffer_IndexSection_init();
    if ([_next_ compareAndSetWithId:nil withId:newSection]) {
      return newSection;
    }
    else {
      return [_next_ get];
    }
  }
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilIndexedRingBuffer_IndexSection_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (void)dealloc {
  RELEASE_(unsafeArray_);
  RELEASE_(_next_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "I", 0x1, 0, 1, -1, -1, -1, -1 },
    { NULL, "V", 0x1, 2, 1, -1, -1, -1, -1 },
    { NULL, "LRxInternalUtilIndexedRingBuffer_IndexSection;", 0x0, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(getAndSetWithInt:withInt:);
  methods[1].selector = @selector(setWithInt:withInt:);
  methods[2].selector = @selector(getNext);
  methods[3].selector = @selector(init);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "unsafeArray_", "LJavaUtilConcurrentAtomicAtomicIntegerArray;", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
    { "_next_", "LJavaUtilConcurrentAtomicAtomicReference;", .constantValue.asLong = 0, 0x12, -1, -1, 3, -1 },
  };
  static const void *ptrTable[] = { "getAndSet", "II", "set", "Ljava/util/concurrent/atomic/AtomicReference<Lrx/internal/util/IndexedRingBuffer$IndexSection;>;", "LRxInternalUtilIndexedRingBuffer;" };
  static const J2ObjcClassInfo _RxInternalUtilIndexedRingBuffer_IndexSection = { "IndexSection", "rx.internal.util", ptrTable, methods, fields, 7, 0x8, 4, 2, 4, -1, -1, -1, -1 };
  return &_RxInternalUtilIndexedRingBuffer_IndexSection;
}

@end

void RxInternalUtilIndexedRingBuffer_IndexSection_init(RxInternalUtilIndexedRingBuffer_IndexSection *self) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->unsafeArray_, new_JavaUtilConcurrentAtomicAtomicIntegerArray_initWithInt_(JreLoadStatic(RxInternalUtilIndexedRingBuffer, SIZE)));
  JreStrongAssignAndConsume(&self->_next_, new_JavaUtilConcurrentAtomicAtomicReference_init());
}

RxInternalUtilIndexedRingBuffer_IndexSection *new_RxInternalUtilIndexedRingBuffer_IndexSection_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilIndexedRingBuffer_IndexSection, init)
}

RxInternalUtilIndexedRingBuffer_IndexSection *create_RxInternalUtilIndexedRingBuffer_IndexSection_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilIndexedRingBuffer_IndexSection, init)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilIndexedRingBuffer_IndexSection)

@implementation RxInternalUtilIndexedRingBuffer_$1

- (RxInternalUtilIndexedRingBuffer *)createObject {
  return create_RxInternalUtilIndexedRingBuffer_init();
}

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilIndexedRingBuffer_$1_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, "LRxInternalUtilIndexedRingBuffer;", 0x4, -1, -1, -1, 0, -1, -1 },
    { NULL, NULL, 0x0, -1, -1, -1, -1, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(createObject);
  methods[1].selector = @selector(init);
  #pragma clang diagnostic pop
  static const void *ptrTable[] = { "()Lrx/internal/util/IndexedRingBuffer<*>;", "LRxInternalUtilIndexedRingBuffer;", "Lrx/internal/util/ObjectPool<Lrx/internal/util/IndexedRingBuffer<*>;>;" };
  static const J2ObjcClassInfo _RxInternalUtilIndexedRingBuffer_$1 = { "", "rx.internal.util", ptrTable, methods, NULL, 7, 0x8008, 2, 0, 1, -1, -1, 2, -1 };
  return &_RxInternalUtilIndexedRingBuffer_$1;
}

@end

void RxInternalUtilIndexedRingBuffer_$1_init(RxInternalUtilIndexedRingBuffer_$1 *self) {
  RxInternalUtilObjectPool_init(self);
}

RxInternalUtilIndexedRingBuffer_$1 *new_RxInternalUtilIndexedRingBuffer_$1_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilIndexedRingBuffer_$1, init)
}

RxInternalUtilIndexedRingBuffer_$1 *create_RxInternalUtilIndexedRingBuffer_$1_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilIndexedRingBuffer_$1, init)
}
