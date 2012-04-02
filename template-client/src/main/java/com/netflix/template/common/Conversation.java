package com.netflix.template.common;

/**
 * Hold a conversation.
 * @author jryan
 *
 */
public interface Conversation {

    /**
     * Initiates a conversation.
     * @return Sentence words from geeting
     */
    Sentence greeting();

    /**
     * End the conversation.
     * @return
     */
    Sentence farewell();
}
