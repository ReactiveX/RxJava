//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/kgalligan/devel-doppl/RxJava/src/main/java/rx/internal/util/SynchronizedQueue.java
//

#include "IOSClass.h"
#include "IOSObjectArray.h"
#include "J2ObjC_source.h"
#include "RxInternalUtilSynchronizedQueue.h"
#include "java/lang/Iterable.h"
#include "java/util/Collection.h"
#include "java/util/Iterator.h"
#include "java/util/LinkedList.h"
#include "java/util/Queue.h"
#include "java/util/Spliterator.h"
#include "java/util/function/Consumer.h"
#include "java/util/function/Predicate.h"
#include "java/util/stream/Stream.h"

@interface RxInternalUtilSynchronizedQueue () {
 @public
  id<JavaUtilQueue> list_;
  jint size_;
}

@end

J2OBJC_FIELD_SETTER(RxInternalUtilSynchronizedQueue, list_, id<JavaUtilQueue>)

@implementation RxInternalUtilSynchronizedQueue

J2OBJC_IGNORE_DESIGNATED_BEGIN
- (instancetype)init {
  RxInternalUtilSynchronizedQueue_init(self);
  return self;
}
J2OBJC_IGNORE_DESIGNATED_END

- (instancetype)initWithInt:(jint)size {
  RxInternalUtilSynchronizedQueue_initWithInt_(self, size);
  return self;
}

- (jboolean)isEmpty {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) isEmpty];
  }
}

- (jboolean)containsWithId:(id)o {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) containsWithId:o];
  }
}

- (id<JavaUtilIterator>)iterator {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) iterator];
  }
}

- (jint)size {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) size];
  }
}

- (jboolean)addWithId:(id)e {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) addWithId:e];
  }
}

- (jboolean)removeWithId:(id)o {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) removeWithId:o];
  }
}

- (jboolean)containsAllWithJavaUtilCollection:(id<JavaUtilCollection>)c {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) containsAllWithJavaUtilCollection:c];
  }
}

- (jboolean)addAllWithJavaUtilCollection:(id<JavaUtilCollection>)c {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) addAllWithJavaUtilCollection:c];
  }
}

- (jboolean)removeAllWithJavaUtilCollection:(id<JavaUtilCollection>)c {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) removeAllWithJavaUtilCollection:c];
  }
}

- (jboolean)retainAllWithJavaUtilCollection:(id<JavaUtilCollection>)c {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) retainAllWithJavaUtilCollection:c];
  }
}

- (void)clear {
  @synchronized(self) {
    [((id<JavaUtilQueue>) nil_chk(list_)) clear];
  }
}

- (NSString *)description {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) description];
  }
}

- (NSUInteger)hash {
  return ((jint) [((id<JavaUtilQueue>) nil_chk(list_)) hash]);
}

- (jboolean)isEqual:(id)obj {
  if (self == obj) {
    return true;
  }
  if (obj == nil) {
    return false;
  }
  if ([self getClass] != (id) [obj getClass]) {
    return false;
  }
  RxInternalUtilSynchronizedQueue *other = (RxInternalUtilSynchronizedQueue *) cast_chk(obj, [RxInternalUtilSynchronizedQueue class]);
  return [((id<JavaUtilQueue>) nil_chk(list_)) isEqual:other->list_];
}

- (id)peek {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) peek];
  }
}

- (id)element {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) element];
  }
}

- (id)poll {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) poll];
  }
}

- (id)remove {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) remove];
  }
}

- (jboolean)offerWithId:(id)e {
  @synchronized(self) {
    return !(size_ > -1 && [((id<JavaUtilQueue>) nil_chk(list_)) size] + 1 > size_) && [((id<JavaUtilQueue>) nil_chk(list_)) offerWithId:e];
  }
}

- (id)clone {
  @synchronized(self) {
    RxInternalUtilSynchronizedQueue *q = create_RxInternalUtilSynchronizedQueue_initWithInt_(size_);
    [q addAllWithJavaUtilCollection:list_];
    return q;
  }
}

- (IOSObjectArray *)toArray {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) toArray];
  }
}

- (IOSObjectArray *)toArrayWithNSObjectArray:(IOSObjectArray *)a {
  @synchronized(self) {
    return [((id<JavaUtilQueue>) nil_chk(list_)) toArrayWithNSObjectArray:a];
  }
}

- (id<JavaUtilStreamStream>)parallelStream {
  return JavaUtilCollection_parallelStream(self);
}

- (jboolean)removeIfWithJavaUtilFunctionPredicate:(id<JavaUtilFunctionPredicate>)arg0 {
  return JavaUtilCollection_removeIfWithJavaUtilFunctionPredicate_(self, arg0);
}

- (id<JavaUtilSpliterator>)spliterator {
  return JavaUtilCollection_spliterator(self);
}

- (id<JavaUtilStreamStream>)stream {
  return JavaUtilCollection_stream(self);
}

- (void)forEachWithJavaUtilFunctionConsumer:(id<JavaUtilFunctionConsumer>)arg0 {
  JavaLangIterable_forEachWithJavaUtilFunctionConsumer_(self, arg0);
}

- (NSUInteger)countByEnumeratingWithState:(NSFastEnumerationState *)state objects:(__unsafe_unretained id *)stackbuf count:(NSUInteger)len {
  return JreDefaultFastEnumeration(self, state, stackbuf, len);
}

- (void)dealloc {
  RELEASE_(list_);
  [super dealloc];
}

+ (const J2ObjcClassInfo *)__metadata {
  static J2ObjcMethodInfo methods[] = {
    { NULL, NULL, 0x1, -1, -1, -1, -1, -1, -1 },
    { NULL, NULL, 0x1, -1, 0, -1, -1, -1, -1 },
    { NULL, "Z", 0x21, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x21, 1, 2, -1, -1, -1, -1 },
    { NULL, "LJavaUtilIterator;", 0x21, -1, -1, -1, 3, -1, -1 },
    { NULL, "I", 0x21, -1, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x21, 4, 2, -1, 5, -1, -1 },
    { NULL, "Z", 0x21, 6, 2, -1, -1, -1, -1 },
    { NULL, "Z", 0x21, 7, 8, -1, 9, -1, -1 },
    { NULL, "Z", 0x21, 10, 8, -1, 11, -1, -1 },
    { NULL, "Z", 0x21, 12, 8, -1, 9, -1, -1 },
    { NULL, "Z", 0x21, 13, 8, -1, 9, -1, -1 },
    { NULL, "V", 0x21, -1, -1, -1, -1, -1, -1 },
    { NULL, "LNSString;", 0x21, 14, -1, -1, -1, -1, -1 },
    { NULL, "I", 0x1, 15, -1, -1, -1, -1, -1 },
    { NULL, "Z", 0x1, 16, 2, -1, -1, -1, -1 },
    { NULL, "LNSObject;", 0x21, -1, -1, -1, 17, -1, -1 },
    { NULL, "LNSObject;", 0x21, -1, -1, -1, 17, -1, -1 },
    { NULL, "LNSObject;", 0x21, -1, -1, -1, 17, -1, -1 },
    { NULL, "LNSObject;", 0x21, -1, -1, -1, 17, -1, -1 },
    { NULL, "Z", 0x21, 18, 2, -1, 5, -1, -1 },
    { NULL, "LNSObject;", 0x21, -1, -1, -1, -1, -1, -1 },
    { NULL, "[LNSObject;", 0x21, -1, -1, -1, -1, -1, -1 },
    { NULL, "[LNSObject;", 0x21, 19, 20, -1, 21, -1, -1 },
  };
  #pragma clang diagnostic push
  #pragma clang diagnostic ignored "-Wobjc-multiple-method-names"
  methods[0].selector = @selector(init);
  methods[1].selector = @selector(initWithInt:);
  methods[2].selector = @selector(isEmpty);
  methods[3].selector = @selector(containsWithId:);
  methods[4].selector = @selector(iterator);
  methods[5].selector = @selector(size);
  methods[6].selector = @selector(addWithId:);
  methods[7].selector = @selector(removeWithId:);
  methods[8].selector = @selector(containsAllWithJavaUtilCollection:);
  methods[9].selector = @selector(addAllWithJavaUtilCollection:);
  methods[10].selector = @selector(removeAllWithJavaUtilCollection:);
  methods[11].selector = @selector(retainAllWithJavaUtilCollection:);
  methods[12].selector = @selector(clear);
  methods[13].selector = @selector(description);
  methods[14].selector = @selector(hash);
  methods[15].selector = @selector(isEqual:);
  methods[16].selector = @selector(peek);
  methods[17].selector = @selector(element);
  methods[18].selector = @selector(poll);
  methods[19].selector = @selector(remove);
  methods[20].selector = @selector(offerWithId:);
  methods[21].selector = @selector(clone);
  methods[22].selector = @selector(toArray);
  methods[23].selector = @selector(toArrayWithNSObjectArray:);
  #pragma clang diagnostic pop
  static const J2ObjcFieldInfo fields[] = {
    { "list_", "LJavaUtilQueue;", .constantValue.asLong = 0, 0x12, -1, -1, 22, -1 },
    { "size_", "I", .constantValue.asLong = 0, 0x12, -1, -1, -1, -1 },
  };
  static const void *ptrTable[] = { "I", "contains", "LNSObject;", "()Ljava/util/Iterator<TT;>;", "add", "(TT;)Z", "remove", "containsAll", "LJavaUtilCollection;", "(Ljava/util/Collection<*>;)Z", "addAll", "(Ljava/util/Collection<+TT;>;)Z", "removeAll", "retainAll", "toString", "hashCode", "equals", "()TT;", "offer", "toArray", "[LNSObject;", "<R:Ljava/lang/Object;>([TR;)[TR;", "Ljava/util/Queue<TT;>;", "<T:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Queue<TT;>;Ljava/lang/Cloneable;" };
  static const J2ObjcClassInfo _RxInternalUtilSynchronizedQueue = { "SynchronizedQueue", "rx.internal.util", ptrTable, methods, fields, 7, 0x1, 24, 2, -1, -1, -1, 23, -1 };
  return &_RxInternalUtilSynchronizedQueue;
}

- (id)copyWithZone:(NSZone *)zone {
  return [[self clone] retain];
}

@end

void RxInternalUtilSynchronizedQueue_init(RxInternalUtilSynchronizedQueue *self) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->list_, new_JavaUtilLinkedList_init());
  self->size_ = -1;
}

RxInternalUtilSynchronizedQueue *new_RxInternalUtilSynchronizedQueue_init() {
  J2OBJC_NEW_IMPL(RxInternalUtilSynchronizedQueue, init)
}

RxInternalUtilSynchronizedQueue *create_RxInternalUtilSynchronizedQueue_init() {
  J2OBJC_CREATE_IMPL(RxInternalUtilSynchronizedQueue, init)
}

void RxInternalUtilSynchronizedQueue_initWithInt_(RxInternalUtilSynchronizedQueue *self, jint size) {
  NSObject_init(self);
  JreStrongAssignAndConsume(&self->list_, new_JavaUtilLinkedList_init());
  self->size_ = size;
}

RxInternalUtilSynchronizedQueue *new_RxInternalUtilSynchronizedQueue_initWithInt_(jint size) {
  J2OBJC_NEW_IMPL(RxInternalUtilSynchronizedQueue, initWithInt_, size)
}

RxInternalUtilSynchronizedQueue *create_RxInternalUtilSynchronizedQueue_initWithInt_(jint size) {
  J2OBJC_CREATE_IMPL(RxInternalUtilSynchronizedQueue, initWithInt_, size)
}

J2OBJC_CLASS_TYPE_LITERAL_SOURCE(RxInternalUtilSynchronizedQueue)
