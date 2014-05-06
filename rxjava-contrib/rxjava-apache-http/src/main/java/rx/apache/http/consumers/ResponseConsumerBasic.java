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
package rx.apache.http.consumers;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.apache.http.ObservableHttpResponse;
import rx.subscriptions.CompositeSubscription;

/**
 * Delegate wrapper around {@link BasicAsyncResponseConsumer} so it works with {@link ResponseConsumerDelegate}
 */
class ResponseConsumerBasic extends BasicAsyncResponseConsumer implements ResponseDelegate {

    private final Observer<? super ObservableHttpResponse> observer;
    private final CompositeSubscription parentSubscription;

    public ResponseConsumerBasic(final Observer<? super ObservableHttpResponse> observer, CompositeSubscription parentSubscription) {
        this.observer = observer;
        this.parentSubscription = parentSubscription;
    }

    @Override
    public void _onResponseReceived(HttpResponse response) throws HttpException, IOException {
        onResponseReceived(response);
    }

    @Override
    public void _onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        if (parentSubscription.isUnsubscribed()) {
            ioctrl.shutdown();
        }
        onContentReceived(decoder, ioctrl);
    }

    @Override
    public void _onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
        onEntityEnclosed(entity, contentType);
    }

    @Override
    public HttpResponse _buildResult(HttpContext context) throws Exception {
        final HttpResponse response = buildResult(context);

        Observable<byte[]> contentObservable = Observable.create(new OnSubscribe<byte[]>() {

            @Override
            public void call(Subscriber<? super byte[]> o) {
                o.add(parentSubscription);
                long length = response.getEntity().getContentLength();
                if (length > Integer.MAX_VALUE) {
                    o.onError(new IllegalStateException("Content Length too large for a byte[] => " + length));
                } else {
                    ExpandableByteBuffer buf = new ExpandableByteBuffer((int) length);
                    try {
                        buf.consumeInputStream(response.getEntity().getContent());
                        o.onNext(buf.getBytes());
                        o.onCompleted();
                    } catch (Throwable e) {
                        o.onError(e);
                    }
                }
            }
        });

        observer.onNext(new ObservableHttpResponse(response, contentObservable));
        return response;
    }

    @Override
    public void _releaseResources() {
        releaseResources();
    }

}
