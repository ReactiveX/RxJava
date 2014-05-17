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

import rx.Observable;

/**
 * The {@link HttpResponse} for the entire request and accessor to {@link Observable} of the content stream.
 */
public class ObservableHttpResponse {

    private final HttpResponse response;
    private final Observable<byte[]> contentSubscription;

    public ObservableHttpResponse(HttpResponse response, Observable<byte[]> contentSubscription) {
        this.response = response;
        this.contentSubscription = contentSubscription;
    }

    /**
     * The {@link HttpResponse} returned by the Apache client at the beginning of the response.
     * 
     * @return {@link HttpResponse} with HTTP status codes, headers, etc
     */
    public HttpResponse getResponse() {
        return response;
    }

    /**
     * If the response is not chunked then only a single array will be returned. If chunked then multiple arrays.
     */
    public Observable<byte[]> getContent() {
        return contentSubscription;
    }

}
