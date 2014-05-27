/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.android.observables;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;


public class CursorObservable {

    // constants for args
    public static final String PROJECTION = "cursor_observable_projection";
    public static final String SELECTION = "cursor_observable_selection";
    public static final String SELECTION_ARGS = "cursor_observable_selection_args";
    public static final String SORT_ORDER = "cursor_observable_sort_order";

    /**
     * The most confusion parameter here is Scheduler.
     * You need to pass worker because we need to do queries
     * in background thread. We can not create Handler for ContentObservable
     * inside rx.Observable, so, best solution here is to pass
     * worker which you have had to pass in subscribeOn method.
     * <p/>
     * <p/>
     * creates new cursor observable with specified uri and args in bundle
     *
     * @param context   application context to call ContentProvider
     * @param uri       for specified table
     * @param scheduler worker for backgropund tasks
     * @param args      Bundle with some values for query. See constants above
     * @return observable that observing cursor in specified uri
     */
    public static Observable<Cursor> from(Context context, Uri uri, Scheduler scheduler, Bundle args) {
        return Observable.create(new CursorOperator(context, uri, scheduler.createWorker(),
                args.getStringArray(PROJECTION), args.getString(SELECTION),
                args.getStringArray(SELECTION_ARGS), args.getString(SORT_ORDER)));
    }

    public static Observable<Cursor> from(Context context, Uri uri, Scheduler scheduler, String[] projection,
                                          String selection, String[] selectionArgs, String sortOrder) {
        return Observable.create(new CursorOperator(context, uri, scheduler.createWorker(),
                projection, selection, selectionArgs, sortOrder));
    }

    public static Observable<Cursor> from(Context context, Uri uri, Scheduler scheduler) {
        return Observable.create(new CursorOperator(context, uri, scheduler.createWorker(), null, null, null, null));
    }

    public static Observable<Cursor> from(Context context, Uri uri, Scheduler scheduler, String[] projection) {
        return Observable.create(new CursorOperator(context, uri, scheduler.createWorker(), projection, null, null, null));
    }

    public static Observable<Cursor> from(Context context, Uri uri, Scheduler scheduler, String[] projection, String sortOdred) {
        return Observable.create(new CursorOperator(context, uri, scheduler.createWorker(), projection, null, null, sortOdred));
    }

    private static class CursorOperator implements Observable.OnSubscribe<Cursor> {

        private ContentResolver resolver;
        private Uri uri;
        private Scheduler.Worker worker;
        private String[] projection;
        private String selection;
        private String[] selectionArgs;
        private String sortOrder;

        public CursorOperator(Context context, Uri uri, Scheduler.Worker scheduler, String[] projection, String selection,
                              String[] selectionArgs, String sortOrder) {
            this.resolver = context.getApplicationContext().getContentResolver();
            this.uri = uri;
            this.projection = projection;
            this.selection = selection;
            this.selectionArgs = selectionArgs;
            this.sortOrder = sortOrder;
            this.worker = scheduler;
        }

        @Override
        public void call(final Subscriber<? super Cursor> subscriber) {


            final DefaultContentObserver observer = new DefaultContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            subscriber.onNext(executeQuery());
                        }
                    });
                }
            };

            final Subscription subscription = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    resolver.unregisterContentObserver(observer);
                }
            });

            worker.schedule(new Action0() {
                @Override
                public void call() {
                    subscriber.onNext(executeQuery());
                }
            });

            resolver.registerContentObserver(uri, true, observer);

            subscriber.add(subscription);
        }

        private Cursor executeQuery() {
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        }


        private class DefaultContentObserver extends ContentObserver {
            /**
             * Creates a content observer.
             *
             * @param handler The handler to run {@link #onChange} on, or null if none.
             */
            public DefaultContentObserver(Handler handler) {
                super(handler);
            }

            @Override
            public boolean deliverSelfNotifications() {
                return false;
            }

            @Override
            public void onChange(boolean selfChange) {
                //do nothing
            }
        }
    }
}
