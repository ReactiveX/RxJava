package com.netflix.template.common;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Sentence {
    private String whole;

    private Sentence() {
    };

    public Sentence(String whole) {
        this.whole = whole;
    }

    @XmlElement
    public String getWhole() {
        return whole;
    }

    public void setWhole(String whole) {
        this.whole = whole;
    }
}