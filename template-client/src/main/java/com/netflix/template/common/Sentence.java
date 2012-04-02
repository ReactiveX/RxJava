package com.netflix.template.common;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Container for words going back and forth.
 * @author jryan
 *
 */
@XmlRootElement
public class Sentence {
    private String whole;

    @SuppressWarnings("unused")
    private Sentence() {
    };

    /**
     * Initialize sentence.
     * @param whole
     */
    public Sentence(String whole) {
        this.whole = whole;
    }

    /**
     * whole getter.
     * @return
     */
    @XmlElement
    public String getWhole() {
        return whole;
    }

    public void setWhole(String whole) {
        this.whole = whole;
    }
}
