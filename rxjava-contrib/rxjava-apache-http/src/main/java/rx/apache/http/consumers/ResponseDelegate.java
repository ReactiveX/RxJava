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
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

/**
 * Delegate methods for getting access to protected methods.
 */
abstract interface ResponseDelegate extends HttpAsyncResponseConsumer<HttpResponse> {

    public void _onResponseReceived(HttpResponse response) throws HttpException, IOException;
    
    public void _onContentReceived(ContentDecoder decoder, IOControl ioctrl) throws IOException;

    public void _onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException;

    public HttpResponse _buildResult(HttpContext context) throws Exception;

    public void _releaseResources();
}
