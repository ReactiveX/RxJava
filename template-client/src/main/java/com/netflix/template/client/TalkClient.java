package com.netflix.template.client;

import com.netflix.template.common.Conversation;
import com.netflix.template.common.Sentence;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

import javax.ws.rs.core.MediaType;

public class TalkClient implements Conversation {

    WebResource webResource;

    TalkClient(String location) {
        Client client = Client.create();
        client.addFilter(new LoggingFilter(System.out));
        webResource = client.resource(location + "/talk");
    }

    public Sentence greeting() {
        Sentence s = webResource.accept(MediaType.APPLICATION_XML).get(Sentence.class);
        return s;
    }

    public Sentence farewell() {
        Sentence s = webResource.accept(MediaType.APPLICATION_XML).delete(Sentence.class);
        return s;
    }

    public static void main(String[] args) {
        TalkClient remote = new TalkClient("http://localhost:8080/template-server/rest");
        System.out.println(remote.greeting().getWhole());
        System.out.println(remote.farewell().getWhole());
    }
}
