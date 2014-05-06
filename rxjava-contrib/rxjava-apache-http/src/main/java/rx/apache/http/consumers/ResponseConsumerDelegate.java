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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

import rx.Observer;
import rx.apache.http.ObservableHttpResponse;
import rx.subscriptions.CompositeSubscription;

/**
 * AbstractAsyncResponseConsumer that chooses different implementations based on return headers.
 * <p>
 * <ul>
 * <li>Content-Type:text/event-stream == {@link ResponseConsumerEventStream}</li>
 * <li>All others == {@link ResponseConsumerBasic}</li>
 * </ul>
 */
public class ResponseConsumerDelegate extends AbstractAsyncResponseConsumer<HttpResponse> {

    private volatile ResponseDelegate consumer = null;
    final Observer<? super ObservableHttpResponse> observer;
    final CompositeSubscription subscription;

    public ResponseConsumerDelegate(final Observer<? super ObservableHttpResponse> observer, CompositeSubscription subscription) {
        this.observer = observer;
        this.subscription = subscription;
    }

    @Override
    protected void onResponseReceived(HttpResponse response) throws HttpException, IOException {
        // when we receive the response with headers we evaluate what type of consumer we want
        if (responseIsStreamLike(response)) {
            consumer = new ResponseConsumerEventStream(observer, subscription);
        } else {
            consumer = new ResponseConsumerBasic(observer, subscription);
        }
        // forward 'response' to actual consumer
        consumer._onResponseReceived(response);
    }

    private boolean responseIsStreamLike(HttpResponse response) {
        final Header contentType = response.getFirstHeader("Content-Type");
        // use 'contains' instead of equals since Content-Type can contain additional information
        // such as charset ... see here: http://www.w3.org/International/O-HTTP-charset
        if (contentType != null && contentType.getValue().contains("text/event-stream")) {
            return true;
        }
        final Header transferEncoding = response.getFirstHeader("Transfer-Encoding");
        if (transferEncoding != null && transferEncoding.getValue().equals("chunked")) {
            return true;
        }
        return false;
    }

    @Override
    protected void onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException {
        consumer._onContentReceived(decoder, ioctrl);
    }

    @Override
    protected void onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
        consumer._onEntityEnclosed(entity, contentType);
    }

    @Override
    protected HttpResponse buildResult(HttpContext context) throws Exception {
        return consumer._buildResult(context);
    }

    @Override
    protected void releaseResources() {
        if (consumer != null) {
            consumer._releaseResources();
        }
    }

}
