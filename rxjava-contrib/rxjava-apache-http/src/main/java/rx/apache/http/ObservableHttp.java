/**
 * Copyright 2014 Netflix, Inc.
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
package rx.apache.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.apache.http.consumers.ResponseConsumerDelegate;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * An {@link Observable} interface to Apache {@link HttpAsyncClient}.
 * <p>
 * The initial {@link HttpResponse} is returned via {@link Observer#onNext} wrapped in a {@link ObservableHttpResponse}.
 * <p>
 * The content stream is retrieved from {@link ObservableHttpResponse#getContent()}.
 * <p>
 * It is aware of Content-Type <i>text/event-stream</i> and will stream each event via {@link Observer#onNext}.
 * <p>
 * Other Content-Types will be returned as a single call to {@link Observer#onNext}.
 * <p>
 * Examples:
 * <p>
 * <pre> {@code
 * ObservableHttp.createGet("http://www.wikipedia.com", httpClient).toObservable();
 * } </pre>
 * <p>
 * <pre> {@code
 * ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://www.wikipedia.com"), httpClient).toObservable();
 * } </pre>
 * 
 * An {@link HttpClient} can be created like this:
 * 
 * <pre> {@code
 * CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
 * httpClient.start(); // start it
 * httpClient.stop(); // stop it
 * } </pre>
 * <p>
 * A client with custom configurations can be created like this:
 * </p>
 * <pre> {@code
 * final RequestConfig requestConfig = RequestConfig.custom()
 *     .setSocketTimeout(1000)
 *     .setConnectTimeout(200).build();
 * final CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom()
 *     .setDefaultRequestConfig(requestConfig)
 *     .setMaxConnPerRoute(20)
 *     .setMaxConnTotal(50)
 *     .build();
 * httpClient.start();
 * }</pre>
 * <p>
 * 
 * @param <T>
 */
public class ObservableHttp<T> {

    private final OnSubscribe<T> onSubscribe;

    private ObservableHttp(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    private static <T> ObservableHttp<T> create(OnSubscribe<T> onSubscribe) {
        return new ObservableHttp<T>(onSubscribe);
    }

    public Observable<T> toObservable() {
        return Observable.create(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> observer) {
                onSubscribe.call(observer);
            }
        });
    }

    public static ObservableHttp<ObservableHttpResponse> createGet(String uri, final HttpAsyncClient client) {
        return createRequest(HttpAsyncMethods.createGet(uri), client);
    }

    /**
     * Execute request using {@link HttpAsyncRequestProducer} to define HTTP Method, URI and payload (if applicable).
     * <p>
     * If the response is chunked (or flushed progressively such as with <i>text/event-stream</i> <a href="http://www.w3.org/TR/2009/WD-eventsource-20091029/">Server-Sent Events</a>) this will call
     * {@link Observer#onNext} multiple times.
     * <p>
     * Use {@code HttpAsyncMethods.create* } factory methods to create {@link HttpAsyncRequestProducer} instances.
     * <p>
     * A client can be retrieved like this:
     * <p>
     * <pre> {@code      CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault(); } </pre> 
     * <p>
     * A client with custom configurations can be created like this:
     * </p>
     * <pre> {@code
     * final RequestConfig requestConfig = RequestConfig.custom()
     *     .setSocketTimeout(3000)
     *     .setConnectTimeout(3000).build();
     * final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
     *     .setDefaultRequestConfig(requestConfig)
     *     .setMaxConnPerRoute(20)
     *     .setMaxConnTotal(50)
     *     .build();
     * httpclient.start();
     * }</pre>
     * 
     * 
     * @param requestProducer
     * @param client
     * @return the observable HTTP response stream
     */
    public static ObservableHttp<ObservableHttpResponse> createRequest(final HttpAsyncRequestProducer requestProducer, final HttpAsyncClient client) {
        return createRequest(requestProducer, client, new BasicHttpContext());
    }

    /**
     * Execute request using {@link HttpAsyncRequestProducer} to define HTTP Method, URI and payload (if applicable).
     * <p>
     * If the response is chunked (or flushed progressively such as with <i>text/event-stream</i> <a href="http://www.w3.org/TR/2009/WD-eventsource-20091029/">Server-Sent Events</a>) this will call
     * {@link Observer#onNext} multiple times.
     * <p>
     * Use {@code HttpAsyncMethods.create* } factory methods to create {@link HttpAsyncRequestProducer} instances.
     * <p>
     * A client can be retrieved like this:
     * <p>
     * <pre> {@code      CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault(); } </pre>
     * <p>
     * A client with custom configurations can be created like this:
     * </p>
     * <pre> {@code
     * final RequestConfig requestConfig = RequestConfig.custom()
     *     .setSocketTimeout(3000)
     *     .setConnectTimeout(3000).build();
     * final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
     *     .setDefaultRequestConfig(requestConfig)
     *     .setMaxConnPerRoute(20)
     *     .setMaxConnTotal(50)
     *     .build();
     * httpclient.start();
     * }</pre>
     *
     *
     * @param requestProducer
     * @param client
     * @param context The HttpContext
     * @return the observable HTTP response stream
     */
    public static ObservableHttp<ObservableHttpResponse> createRequest(final HttpAsyncRequestProducer requestProducer, final HttpAsyncClient client, final HttpContext context) {

        return ObservableHttp.create(new OnSubscribe<ObservableHttpResponse>() {

            @Override
            public void call(final Subscriber<? super ObservableHttpResponse> observer) {

                final CompositeSubscription parentSubscription = new CompositeSubscription();
                observer.add(parentSubscription);

                // return a Subscription that wraps the Future so it can be cancelled
                parentSubscription.add(Subscriptions.from(client.execute(requestProducer, new ResponseConsumerDelegate(observer, parentSubscription),
                        context, new FutureCallback<HttpResponse>() {

                            @Override
                            public void completed(HttpResponse result) {
                                observer.onCompleted();
                            }

                            @Override
                            public void failed(Exception ex) {
                                observer.onError(ex);
                            }

                            @Override
                            public void cancelled() {
                                observer.onCompleted();
                            }

                        })));
            }
        });
    }

}
