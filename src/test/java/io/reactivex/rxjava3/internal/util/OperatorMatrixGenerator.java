/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.util;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;

/**
 * Generate a table of available operators across base classes in {@code Operator-Matrix.md}.
 * 
 * Should be run with the main project directory as working directory where the {@code docs}
 * folder is.
 */
public final class OperatorMatrixGenerator {

    private OperatorMatrixGenerator() {
        throw new IllegalStateException("No instances!");
    }

    static final String PRESENT = "![present](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png)";
    static final String ABSENT = "![absent](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png)";
    static final String TBD = "![absent](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_half.png)";

    static final Class<?>[] CLASSES = {
            Flowable.class, Observable.class, Maybe.class, Single.class, Completable.class
    };

    static String header(String type) {
        return "![" + type + "](https://raw.github.com/wiki/ReactiveX/RxJava/images/opmatrix-" + type.toLowerCase() + ".png)";
    }

    public static void main(String[] args) throws IOException {
        Set<String> operatorSet = new HashSet<>();
        Map<Class<?>, Set<String>> operatorMap = new HashMap<>();

        for (Class<?> clazz : CLASSES) {
            Set<String> set = operatorMap.computeIfAbsent(clazz, c -> new HashSet<>());

            for (Method m : clazz.getMethods()) {
                String name = m.getName();
                if (!name.equals("bufferSize")
                        && m.getDeclaringClass() == clazz
                        && !m.isSynthetic()) {
                    operatorSet.add(m.getName());
                    set.add(m.getName());
                }
            }
        }

        List<String> sortedOperators = new ArrayList<>(operatorSet);
        sortedOperators.sort(Comparator.naturalOrder());

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("docs", "Operator-Matrix.md"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.print("Operator |");
            for (Class<?> clazz : CLASSES) {
                out.print(" ");
                out.print(header(clazz.getSimpleName()));
                out.print(" |");
            }
            out.println();
            out.print("-----|");
            for (int i = 0; i < CLASSES.length; i++) {
                out.print("---|");
            }
            out.println();

            Map<String, Integer> notesMap = new HashMap<>();
            List<String> notesList = new ArrayList<>();
            List<String> tbdList = new ArrayList<>();
            int[] counters = new int[CLASSES.length];

            for (String operatorName : sortedOperators) {
                out.print("<a name='");
                out.print(operatorName);
                out.print("'></a>`");
                out.print(operatorName);
                out.print("`|");
                int m = 0;
                for (Class<?> clazz : CLASSES) {
                    if (operatorMap.get(clazz).contains(operatorName)) {
                        out.print(PRESENT);
                        counters[m]++;
                    } else {
                        String notes = findNotes(clazz.getSimpleName(), operatorName);
                        if (notes != null) {
                            out.print(ABSENT);
                            Integer index = notesMap.get(notes);
                            if (index == null) {
                                index = notesMap.size() + 1;
                                notesMap.put(notes, index);
                                notesList.add(notes);
                            }
                            out.print(" <sup title='");
                            out.print(notes.replace("`", "").replace("[", "").replace("]", "").replaceAll("\\(#.+\\)", ""));
                            out.print("'>([");
                            out.print(index);
                            out.print("](#notes-");
                            out.print(index);
                            out.print("))</sup>");
                        } else {
                            out.print(TBD);
                            tbdList.add(clazz.getSimpleName() + "." + operatorName + "()");
                        }
                    }
                    out.print("|");
                    m++;
                }
                out.println();
            }
            out.print("<a name='total'></a>**");
            out.print(sortedOperators.size());
            out.print(" operators** |");
            for (int m = 0; m < counters.length; m++) {
                out.print(" **");
                out.print(counters[m]);
                out.print("** |");
            }
            out.println();

            if (!notesList.isEmpty()) {
                out.println();
                out.println("#### Notes");

                for (int i = 0; i < notesList.size(); i++) {
                    out.print("<a name='notes-");
                    out.print(i + 1);
                    out.print("'></a><sup>");
                    out.print(i + 1);
                    out.print("</sup> ");
                    out.print(notesList.get(i));
                    out.println("<br/>");
                }
            }
            if (tbdList.isEmpty()) {
                out.println();
                out.println("#### Under development");
                out.println();
                out.println("*Currently, all intended operators are implemented.*");
            } else {
                out.println();
                out.println("#### Under development");
                out.println();

                for (int i = 0; i < tbdList.size(); i++) {
                    out.print(i + 1);
                    out.print(". ");
                    out.println(tbdList.get(i));
                }
            }
        }
    }

    static String findNotes(String clazzName, String operatorName) {
        Map<String, String> classNotes = NOTES_MAP.get(operatorName);
        if (classNotes != null) {
            return classNotes.get(clazzName.substring(0, 1));
        }
        switch (operatorName) {
            case "empty": {
                if ("Completable".equals(clazzName)) {
                    return "Use [`complete()`](#complete).";
                }
                if ("Single".equals(clazzName)) {
                    return "Never empty.";
                }
                break;
            }
        }
        return null;
    }

    static final String[] NOTES = {
            // Format
            // FOMSC methodName note
            "  MS  all                                  Use [`contains()`](#contains).",
            "    C all                                  Always empty.",
            "FOMS  andThen                              Use [`concatWith`](#concatWith).",
            "  MS  any                                  Use [`contains()`](#contains).",
            "    C any                                  Always empty.",
            "FO    blockingAwait                        Use [`blockingFirst()`](#blockingFirst), [`blockingSingle()`](#blockingSingle) or [`blockingLast()`](#blockingLast).",
            "  MS  blockingAwait                        Use [`blockingGet()`](#blockingGet).",
            "  MS  blockingFirst                        At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingFirst                        No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MSC blockingForEach                      Use [`blockingSubscribe()`](#blockingSubscribe)",
            "FO    blockingGet                          Use [`blockingFirst()`](#blockingFirst), [`blockingSingle()`](#blockingSingle) or [`blockingLast()`](#blockingLast).",
            "    C blockingGet                          No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingIterable                     At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingIterable                     No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingLast                         At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingLast                         No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingLatest                       At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingLatest                       No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingMostRecent                   At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingMostRecent                   No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingNext                         At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingNext                         No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingSingle                       At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingSingle                       No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  MS  blockingStream                       At most one element to get. Use [`blockingGet()`](#blockingGet).",
            "    C blockingStream                       No elements to get. Use [`blockingAwait()`](#blockingAwait).",
            "  M   buffer                               Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  buffer                               Use [`map()`](#map) to transform into a list/collection.",
            "    C buffer                               Always empty. Use [`andThen()`](#andThen) to bring in a list/collection.",
            "  MSC cacheWithInitialCapacity             At most one element to store. Use [`cache()`](#cache).",
            "    C cast                                 Always empty.",
            "  M   collect                              At most one element to collect. Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  collect                              One element to collect. Use [`map()`](#map) to transform into a list/collection.",
            "    C collect                              Always empty. Use [`andThen()`](#andThen) to bring in a collection.",
            "  M   collectInto                          At most one element to collect. Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  collectInto                          One element to collect. Use [`map()`](#map) to transform into a list/collection.",
            "    C collectInto                          Always empty. Use [`andThen()`](#andThen) to bring in a collection.",
            "  MS  combineLatest                        At most one element per source. Use [`zip()`](#zip).",
            "    C combineLatest                        Always empty. Use [`merge()`](#merge).",
            "  MS  combineLatestArray                   At most one element per source. Use [`zipArray()`](#zipArray).",
            "    C combineLatestArray                   Always empty. Use [`mergeArray()`](#mergeArray).",
            "  MS  combineLatestDelayError              At most one element per source. Use [`zip()`](#zip).",
            "    C combineLatestDelayError              Always empty. Use [`mergeDelayError()`](#mergeDelayError).",
            "  MS  combineLatestArrayDelayError         At most one element per source. Use [`zipArray()`](#zipArray).",
            "    C combineLatestArrayDelayError         Always empty. Use [`mergeArrayDelayError()`](#mergeArrayDelayError).",
            "FOM   complete                             Use [`empty()`](#empty).",
            "   S  complete                             Never empty.",
            "    C concatArrayEager                     No items to keep ordered. Use [`mergeArray()`](#mergeArray).",
            "    C concatArrayEagerDelayError           No items to keep ordered. Use [`mergeArrayDelayError()`](#mergeArrayDelayError).",
            "    C concatEager                          No items to keep ordered. Use [`merge()`](#merge).",
            "    C concatEagerDelayError                No items to keep ordered. Use [`mergeDelayError()`](#mergeDelayError).",
            "    C concatMap                            Always empty thus no items to map.",
            "    C concatMapCompletable                 Always empty thus no items to map.",
            "  MS  concatMapCompletableDelayError       Either the upstream fails (thus no inner) or the mapped-in source, but never both. Use [`concatMapCompletable`](#concatMapCompletable).",
            "    C concatMapCompletableDelayError       Always empty thus no items to map.",
            "  MS  concatMapDelayError                  Either the upstream fails (thus no inner) or the mapped-in source, but never both.  Use [`concatMap`](#concatMap).",
            "    C concatMapDelayError                  Always empty thus no items to map.",
            "  MS  concatMapEager                       At most one item to map. Use [`concatMap()`](#concatMap).",
            "    C concatMapEager                       Always empty thus no items to map.",
            "  MS  concatMapEagerDelayError             At most one item to map. Use [`concatMap()`](#concatMap).",
            "    C concatMapEagerDelayError             Always empty thus no items to map.",
            "    C concatMapIterable                    Always empty thus no items to map.",
            "  M   concatMapMaybe                       Use [`concatMap`](#concatMap).",
            "    C concatMapMaybe                       Always empty thus no items to map.",
            "  MS  concatMapMaybeDelayError             Either the upstream fails (thus no inner) or the mapped-in source, but never both.  Use [`concatMapMaybe`](#concatMapMaybe).",
            "    C concatMapMaybeDelayError             Always empty thus no items to map.",
            "   S  concatMapSingle                      Use [`concatMap()`](#concatMap).",
            "    C concatMapSingle                      Always empty thus no items to map.",
            "  MS  concatMapSingleDelayError            Either the upstream fails (thus no inner) or the mapped-in source, but never both.  Use [`concatMapSingle`](#concatMapSingle).",
            "    C concatMapSingleDelayError            Always empty thus no items to map.",
            "    C concatMapStream                      Always empty thus no items to map.",
            "  MS  concatMapIterable                    At most one item. Use [`flattenAsFlowable`](#flattenAsFlowable) or [`flattenAsObservable`](#flattenAsObservable).",
            "  MS  concatMapStream                      At most one item. Use [`flattenStreamAsFlowable`](#flattenStreamAsFlowable) or [`flattenStreamAsObservable`](#flattenStreamAsObservable).",
            "    C contains                             Always empty.",
            "   S  count                                Never empty thus always 1.",
            "    C count                                Always empty thus always 0.",
            "  MS  debounce                             At most one item signaled so no subsequent items to work with.",
            "    C debounce                             Always empty thus no items to work with.",
            "   S  defaultIfEmpty                       Never empty.",
            "    C defaultIfEmpty                       Always empty. Use [`andThen()`](#andThen) to chose the follow-up sequence.",
            "    C dematerialize                        Always empty thus no items to work with.",
            "  MS  distinct                             At most one item, always distinct.",
            "    C distinct                             Always empty thus no items to work with.",
            "  MS  distinctUntilChanged                 At most one item, always distinct.",
            "    C distinctUntilChanged                 Always empty thus no items to work with.",
            "  MS  doAfterNext                          Different terminology. Use [`doAfterSuccess()`](#doAfterSuccess).",
            "    C doAfterNext                          Always empty.",
            "FO    doAfterSuccess                       Different terminology. Use [`doAfterNext()`](#doAfterNext).",
            "    C doAfterSuccess                       Always empty thus no items to work with.",
            " OMSC doOnCancel                           Different terminology. Use [`doOnDispose()`](#doOnDispose).",
            "   S  doOnComplete                         Always succeeds or fails, there is no `onComplete` signal.",
            "F     doOnDispose                          Different terminology. Use [`doOnCancel()`](#doOnCancel).",
            "  MS  doOnEach                             At most one item. Use [`doOnEvent()`](#doOnEvent).",
            "    C doOnEach                             Always empty thus no items to work with.",
            "FO    doOnEvent                            Use [`doOnEach()`](#doOnEach).",
            "  MS  doOnNext                             Different terminology. Use [`doOnSuccess()`](#doOnSuccess).",
            "    C doOnNext                             Always empty thus no items to work with.",
            " OMSC doOnRequest                          Backpressure related and not supported outside `Flowable`.",
            "FO    doOnSuccess                          Different terminology. Use [`doOnNext()`](#doOnNext).",
            "    C doOnSuccess                          Always empty thus no items to work with.",
            "  M   elementAt                            At most one item with index 0. Use [`defaultIfEmpty`](#defaultIfEmpty).",
            "   S  elementAt                            Always one item with index 0.",
            "    C elementAt                            Always empty thus no items to work with.",
            "  M   elementAtOrError                     At most one item with index 0. Use [`toSingle`](#toSingle).",
            "   S  elementAtOrError                     Always one item with index 0.",
            "    C elementAtOrError                     Always empty thus no items to work with.",
            "   S  empty                                Never empty.",
            "    C empty                                Use [`complete()`](#complete).",
            "    C filter                               Always empty thus no items to work with.",
            "  M   first                                At most one item. Use [`defaultIfEmpty`](#defaultIfEmpty).",
            "   S  first                                Always one item.",
            "    C first                                Always empty. Use [`andThen()`](#andThen) to chose the follow-up sequence.",
            "  M   firstElement                         At most one item, would be no-op.",
            "   S  firstElement                         Always one item, would be no-op.",
            "    C firstElement                         Always empty.",
            "  M   firstOrError                         At most one item, would be no-op.",
            "   S  firstOrError                         Always one item, would be no-op.",
            "    C firstOrError                         Always empty. Use [`andThen()`](#andThen) and [`error()`](#error).",
            "  MS  firstOrErrorStage                    At most one item. Use [`toCompletionStage()`](#toCompletionStage).",
            "    C firstOrErrorStage                    Always empty. Use [`andThen()`](#andThen), [`error()`](#error) and [`toCompletionStage()`](#toCompletionStage).",
            "  MSC firstStage                           At most one item. Use [`toCompletionStage()`](#toCompletionStage).",
            "    C flatMap                              Always empty thus no items to map.",
            "    C flatMapCompletable                   Always empty thus no items to map.",
            "    C flatMapCompletableDelayError         Always empty thus no items to map.",
            "    C flatMapIterable                      Always empty thus no items to map.",
            "  M   flatMapMaybe                         Use [`flatMap()`](#flatMap).",
            "    C flatMapMaybe                         Always empty thus no items to map.",
            "    C flatMapMaybeDelayError               Always empty thus no items to map.",
            "   S  flatMapSingle                        Use [`flatMap()`](#flatMap).",
            "    C flatMapSingle                        Always empty thus no items to map.",
            "    C flatMapStream                        Always empty thus no items to map.",
            "  MS  flatMapIterable                      At most one item. Use [`flattenAsFlowable`](#flattenAsFlowable) or [`flattenAsObservable`](#flattenAsObservable).",
            "  MS  flatMapStream                        At most one item. Use [`flattenStreamAsFlowable`](#flattenStreamAsFlowable) or [`flattenStreamAsObservable`](#flattenStreamAsObservable).",
            "F     flatMapObservable                    Not supported. Use [`flatMap`](#flatMap) and [`toFlowable()`](#toFlowable).",
            " O    flatMapObservable                    Use [`flatMap`](#flatMap).",
            "    C flatMapObservable                    Always empty thus no items to map.",
            " O    flatMapPublisher                     Not supported. Use [`flatMap`](#flatMap) and [`toObservable()`](#toFlowable).",
            "F     flatMapPublisher                     Use [`flatMap`](#flatMap).",
            "    C flatMapPublisher                     Always empty thus no items to map.",
            "FO    flatMapSingleElement                 Use [`flatMapSingle`](#flatMapSingle).",
            "   S  flatMapSingleElement                 Use [`flatMap`](#flatMap).",
            "    C flatMapSingleElement                 Always empty thus no items to map.",
            "FO    flattenAsFlowable                    Use [`flatMapIterable()`](#flatMapIterable).",
            "    C flattenAsFlowable                    Always empty thus no items to map.",
            "FO    flattenAsObservable                  Use [`flatMapIterable()`](#flatMapIterable).",
            "    C flattenAsObservable                  Always empty thus no items to map.",
            "FO    flattenStreamAsFlowable              Use [`flatMapStream()`](#flatMapStream).",
            "    C flattenStreamAsFlowable              Always empty thus no items to map.",
            "FO    flattenStreamAsObservable            Use [`flatMapStream()`](#flatMapStream).",
            "    C flattenStreamAsObservable            Always empty thus no items to map.",
            "  MSC forEach                              Use [`subscribe()`](#subscribe).",
            "  MSC forEachWhile                         Use [`subscribe()`](#subscribe).",
            "   S  fromAction                           Never empty.",
            "  M   fromArray                            At most one item. Use [`just()`](#just) or [`empty()`](#empty).",
            "   S  fromArray                            Always one item. Use [`just()`](#just).",
            "    C fromArray                            Always empty. Use [`complete()`](#complete).",
            "   S  fromCompletable                      Always error.",
            "    C fromCompletable                      Use [`wrap()`](#wrap).",
            "  M   fromIterable                         At most one item. Use [`just()`](#just) or [`empty()`](#empty).",
            "   S  fromIterable                         Always one item. Use [`just()`](#just).",
            "    C fromIterable                         Always empty. Use [`complete()`](#complete).",
            "  M   fromMaybe                            Use [`wrap()`](#wrap).",
            " O    fromObservable                       Use [`wrap()`](#wrap).",
            "   S  fromOptional                         Always one item. Use [`just()`](#just).",
            "    C fromOptional                         Always empty. Use [`complete()`](#complete).",
            "   S  fromRunnable                         Never empty.",
            "   S  fromSingle                           Use [`wrap()`](#wrap).",
            "  M   fromStream                           At most one item. Use [`just()`](#just) or [`empty()`](#empty).",
            "   S  fromStream                           Always one item. Use [`just()`](#just).",
            "    C fromStream                           Always empty. Use [`complete()`](#complete).",
            "  MSC generate                             Use [`fromSupplier()`](#fromSupplier).",
            "  MS  groupBy                              At most one item.",
            "    C groupBy                              Always empty thus no items to group.",
            "  MS  groupJoin                            At most one item.",
            "    C groupJoin                            Always empty thus no items to join.",
            "FO    ignoreElement                        Use [`ignoreElements()`](#ignoreElements).",
            "    C ignoreElement                        Always empty.",
            "  MS  ignoreElements                       Use [`ignoreElement()`](#ignoreElement).",
            "    C ignoreElements                       Always empty.",
            "  MSC interval                             At most one item. Use [`timer()`](#timer).",
            "  MSC intervalRange                        At most one item. Use [`timer()`](#timer).",
            "   S  isEmpty                              Always one item.",
            "    C isEmpty                              Always empty.",
            "  MS  join                                 At most one item. Use [`zip()`](#zip)",
            "    C join                                 Always empty thus no items to join.",
            "    C just                                 Always empty.",
            "  M   last                                 At most one item. Use [`defaultIfEmpty`](#defaultIfEmpty).",
            "   S  last                                 Always one item.",
            "    C last                                 Always empty. Use [`andThen()`](#andThen) to chose the follow-up sequence.",
            "  M   lastElement                          At most one item, would be no-op.",
            "   S  lastElement                          Always one item, would be no-op.",
            "    C lastElement                          Always empty.",
            "  M   lastOrError                          At most one item, would be no-op.",
            "   S  lastOrError                          Always one item, would be no-op.",
            "    C lastOrError                          Always empty. Use [`andThen()`](#andThen) and [`error()`](#error).",
            "  MS  lastOrErrorStage                     At most one item. Use [`toCompletionStage()`](#toCompletionStage).",
            "    C lastOrErrorStage                     Always empty. Use [`andThen()`](#andThen), [`error()`](#error) and [`toCompletionStage()`](#toCompletionStage).",
            "  MSC lastStage                            At most one item. Use [`toCompletionStage()`](#toCompletionStage).",
            "    C map                                  Always empty thus no items to map.",
            "    C mapOptional                          Always empty thus no items to map.",
            "    C ofType                               Always empty thus no items to filter.",
            " OMSC onBackpressureBuffer                 Backpressure related and not supported outside `Flowable`.",
            " OMSC onBackpressureDrop                   Backpressure related and not supported outside `Flowable`.",
            " OMSC onBackpressureLatest                 Backpressure related and not supported outside `Flowable`.",
            " OMSC parallel                             Needs backpressure thus not supported outside `Flowable`.",
            "  M   publish                              Connectable sources not supported outside `Flowable` and `Observable`. Use a `MaybeSubject`.",
            "   S  publish                              Connectable sources not supported outside `Flowable` and `Observable`. Use a `SingleSubject`.",
            "    C publish                              Connectable sources not supported outside `Flowable` and `Observable`. Use a `ConnectableSubject`.",
            "  MS  range                                At most one item. Use [`just()`](#just).",
            "    C range                                Always empty. Use [`complete()`](#complete).",
            "  MS  rangeLong                            At most one item. Use [`just()`](#just).",
            "    C rangeLong                            Always empty. Use [`complete()`](#complete).",
            " OMSC rebatchRequests                      Backpressure related and not supported outside `Flowable`.",
            "  MS  reduce                               At most one item. Use [`map()`](#map).",
            "    C reduce                               Always empty thus no items to reduce.",
            "  MS  reduceWith                           At most one item. Use [`map()`](#map).",
            "    C reduceWith                           Always empty thus no items to reduce.",
            "  M   replay                               Connectable sources not supported outside `Flowable` and `Observable`. Use a `MaybeSubject`.",
            "   S  replay                               Connectable sources not supported outside `Flowable` and `Observable`. Use a `SingleSubject`.",
            "    C replay                               Connectable sources not supported outside `Flowable` and `Observable`. Use a `ConnectableSubject`.",
            "  MS  sample                               At most one item, would be no-op.",
            "    C sample                               Always empty thus no items to work with.",
            "  MS  scan                                 At most one item. Use [`map()`](#map).",
            "    C scan                                 Always empty thus no items to reduce.",
            "  MS  scanWith                             At most one item. Use [`map()`](#map).",
            "    C scanWith                             Always empty thus no items to reduce.",
            "  MSC serialize                            At most one signal type.",
            "  M   share                                Connectable sources not supported outside `Flowable` and `Observable`. Use a `MaybeSubject`.",
            "   S  share                                Connectable sources not supported outside `Flowable` and `Observable`. Use a `SingleSubject`.",
            "    C share                                Connectable sources not supported outside `Flowable` and `Observable`. Use a `ConnectableSubject`.",
            "  M   single                               At most one item. Use [`defaultIfEmpty`](#defaultIfEmpty).",
            "   S  single                               Always one item.",
            "    C single                               Always empty. Use [`andThen()`](#andThen) to chose the follow-up sequence.",
            "  M   singleElement                        At most one item, would be no-op.",
            "   S  singleElement                        Always one item, would be no-op.",
            "    C singleElement                        Always empty.",
            "  M   singleOrError                        At most one item, would be no-op.",
            "   S  singleOrError                        Always one item, would be no-op.",
            "    C singleOrError                        Always empty. Use [`andThen()`](#andThen) and [`error()`](#error).",
            "  MS  singleOrErrorStage                   At most one item. Use [`toCompletionStage()`](#toCompletionStage).",
            "    C singleOrErrorStage                   Always empty. Use [`andThen()`](#andThen), [`error()`](#error) and [`toCompletionStage()`](#toCompletionStage).",
            "  MSC singleStage                          At most one item. Use [`toCompletionStage()`](#toCompletionStage).",
            "  MSC skip                                 At most one item, would be no-op.",
            "  MSC skipLast                             At most one item, would be no-op.",
            "  MS  skipWhile                            At most one item. Use [`filter()`](#filter).",
            "    C skipWhile                            Always empty.",
            "  MSC skipUntil                            At most one item. Use [`takeUntil()`](#takeUntil).",
            "  MSC sorted                               At most one item.",
            "  MSC startWithArray                       Use [`startWith()`](#startWith) and [`fromArray()`](#fromArray) of `Flowable` or `Observable`.",
            "  MSC startWithItem                        Use [`startWith()`](#startWith) and [`just()`](#just) of another reactive type.",
            "  MSC startWithIterable                    Use [`startWith()`](#startWith) and [`fromIterable()`](#fromArray) of `Flowable` or `Observable`.",
            "   S  switchIfEmpty                        Never empty.",
            "    C switchIfEmpty                        Always empty. Use [`defaultIfEmpty()`](#defaultIfEmpty).",
            "  MS  switchMap                            At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMap                            Always empty thus no items to map.",
            "  MS  switchMapDelayError                  At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapDelayError                  Always empty thus no items to map.",
            "  MS  switchMapCompletable                 At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapCompletable                 Always empty thus no items to map.",
            "  MS  switchMapCompletableDelayError       At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapCompletableDelayError       Always empty thus no items to map.",
            "  MS  switchMapMaybe                       At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapMaybe                       Always empty thus no items to map.",
            "  MS  switchMapMaybeDelayError             At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapMaybeDelayError             Always empty thus no items to map.",
            "  MS  switchMapSingle                      At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapSingle                      Always empty thus no items to map.",
            "  MS  switchMapSingleDelayError            At most one item. Use [`flatMap()`](#flatMap).",
            "    C switchMapSingleDelayError            Always empty thus no items to map.",
            "  MSC take                                 At most one item, would be no-op.",
            "  MSC takeLast                             At most one item, would be no-op.",
            "  MS  takeWhile                            At most one item. Use [`filter()`](#filter).",
            "    C takeWhile                            Always empty.",
            "  MS  throttleFirst                        At most one item signaled so no subsequent items to work with.",
            "    C throttleFirst                        Always empty thus no items to work with.",
            "  MS  throttleLast                         At most one item signaled so no subsequent items to work with.",
            "    C throttleLast                         Always empty thus no items to work with.",
            "  MS  throttleLatest                       At most one item signaled so no subsequent items to work with.",
            "    C throttleLatest                       Always empty thus no items to work with.",
            "  MS  throttleWithTimeout                  At most one item signaled so no subsequent items to work with.",
            "    C throttleWithTimeout                  Always empty thus no items to work with.",
            "    C timeInterval                         Always empty thus no items to work with.",
            "    C timestamp                            Always empty thus no items to work with.",
            "FO    toCompletionStage                    Use [`firstStage`](#firstStage), [`lastStage`](#lastStage) or [`singleStage`](#singleStage).",
            "F     toFlowable                           Would be no-op.",
            "  M   toList                               At most one element to collect. Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  toList                               One element to collect. Use [`map()`](#map) to transform into a list/collection.",
            "    C toList                               Always empty. Use [`andThen()`](#andThen) to bring in a collection.",
            "  M   toMap                                At most one element to collect. Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  toMap                                One element to collect. Use [`map()`](#map) to transform into a list/collection.",
            "    C toMap                                Always empty. Use [`andThen()`](#andThen) to bring in a collection.",
            "  M   toMultimap                           At most one element to collect. Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  toMultimap                           One element to collect. Use [`map()`](#map) to transform into a list/collection.",
            "    C toMultimap                           Always empty. Use [`andThen()`](#andThen) to bring in a collection.",
            "FO    toMaybe                              Use [`firstElement`](#firstElement), [`lastElement`](#lastElement) or [`singleElement`](#singleElement).",
            "  M   toMaybe                              Would be no-op.",
            " O    toObservable                         Would be no-op.",
            "FO    toSingle                             Use [`firstOrError`](#firstOrError), [`lastOrError`](#lastOrError) or [`singleOrError`](#singleOrError).",
            "   S  toSingle                             Would be no-op.",
            "FO    toSingleDefault                      Use [`first`](#first), [`last`](#last) or [`single`](#single).",
            "  M   toSingleDefault                      Use [`defaultIfEmpty()`](#defaultIfEmpty).",
            "   S  toSingleDefault                      Would be no-op.",
            "  M   toSortedList                         At most one element to collect. Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a list/collection.",
            "   S  toSortedList                         One element to collect. Use [`map()`](#map) to transform into a list/collection.",
            "    C toSortedList                         Always empty. Use [`andThen()`](#andThen) to bring in a collection.",
            "  M   window                               Use [`map()`](#map) and [`switchIfEmpty()`](#switchIfEmpty) to transform into a nested source.",
            "   S  window                               Use [`map()`](#map) to transform into a nested source.",
            "    C window                               Always empty. Use [`andThen()`](#andThen) to bring in a nested source.",
            "  MS  withLatestFrom                       At most one element per source. Use [`zip()`](#zip).",
            "    C withLatestFrom                       Always empty. Use [`merge()`](#merge).",
            "F     wrap                                 Use [`fromPublisher()`](#fromPublisher).",
            "    C zip                                  Use [`merge()`](#merge).",
            "    C zipArray                             Use [`mergeArray()`](#mergeArray).",
            "    C zipWith                              Use [`mergeWith()`](#mergeWith).",
    };

    static final Map<String, Map<String, String>> NOTES_MAP;
    static {
        NOTES_MAP = new HashMap<>();
        for (String s : NOTES) {
            char[] classes = s.substring(0, 5).trim().toCharArray();
            int idx = s.indexOf(' ', 7);
            String method = s.substring(6, idx);
            String note = s.substring(idx).trim();

            for (char c : classes) {
                NOTES_MAP.computeIfAbsent(method, v -> new HashMap<>())
                .put(String.valueOf(c), note);
            }
        }
    }
}
