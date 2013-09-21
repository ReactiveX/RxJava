package rx.apache.http.examples;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.client.methods.HttpAsyncMethods;

import rx.Observable;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class ExampleObservableHttp {

    public static void main(String args[]) {
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();

        //        final RequestConfig requestConfig = RequestConfig.custom()
        //                .setSocketTimeout(3000)
        //                .setConnectTimeout(3000).build();
        //        final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom()
        //                .setDefaultRequestConfig(requestConfig)
        //                .setMaxConnPerRoute(20)
        //                .setMaxConnTotal(50)
        //                .build();

        try {
            httpclient.start();
            executeViaObservableHttpWithForEach(httpclient);
            executeStreamingViaObservableHttpWithForEach(httpclient);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
        ObservableHttp.createGet("http://www.wikipedia.com", httpClient).toObservable();
        ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://www.wikipedia.com"), httpClient).toObservable();
    }

    protected static void executeViaObservableHttpWithForEach(final HttpAsyncClient client) throws URISyntaxException, IOException, InterruptedException {
        System.out.println("---- executeViaObservableHttpWithForEach");
        ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://www.wikipedia.com"), client)
                .toObservable()
                .flatMap(new Func1<ObservableHttpResponse, Observable<String>>() {

                    @Override
                    public Observable<String> call(ObservableHttpResponse response) {
                        return response.getContent().map(new Func1<byte[], String>() {

                            @Override
                            public String call(byte[] bb) {
                                return new String(bb);
                            }

                        });
                    }
                })
                .toBlockingObservable()
                .forEach(new Action1<String>() {

                    @Override
                    public void call(String resp) {
                        System.out.println(resp);
                    }
                });
    }

    protected static void executeStreamingViaObservableHttpWithForEach(final HttpAsyncClient client) throws URISyntaxException, IOException, InterruptedException {
        System.out.println("---- executeStreamingViaObservableHttpWithForEach");
        for (int i = 0; i < 5; i++) {
            final int c = i + 1;
            ObservableHttp.createRequest(HttpAsyncMethods.createGet("http://ec2-54-211-91-164.compute-1.amazonaws.com:8077/eventbus.stream?topic=hystrix-metrics"), client)
                    .toObservable()
                    .flatMap(new Func1<ObservableHttpResponse, Observable<String>>() {

                        @Override
                        public Observable<String> call(ObservableHttpResponse response) {
                            return response.getContent().map(new Func1<byte[], String>() {

                                @Override
                                public String call(byte[] bb) {
                                    return new String(bb);
                                }

                            });
                        }
                    })
                    .filter(new Func1<String, Boolean>() {

                        @Override
                        public Boolean call(String t1) {
                            return !t1.startsWith(": ping");
                        }
                    })
                    .take(3)
                    .toBlockingObservable()
                    .forEach(new Action1<String>() {

                        @Override
                        public void call(String resp) {
                            System.out.println("Response [" + c + "]: " + resp + " (" + resp.length() + ")");
                        }
                    });
        }
    }

}
