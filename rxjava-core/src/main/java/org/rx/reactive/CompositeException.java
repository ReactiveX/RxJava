package org.rx.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Exception that is a composite of 1 or more other exceptions.
 * <p>
 * The <code>getMessage()</code> will return a concatenation of the composite exceptions.
 */
public class CompositeException extends RuntimeException {

    private static final long serialVersionUID = 3026362227162912146L;

    private final List<Exception> exceptions;
    private final String message;

    public CompositeException(String messagePrefix, Collection<Exception> errors) {
        StringBuilder _message = new StringBuilder();
        if (messagePrefix != null) {
            _message.append(messagePrefix).append(" => ");
        }

        List<Exception> _exceptions = new ArrayList<Exception>();
        for (Exception e : errors) {
            _exceptions.add(e);
            if (_message.length() > 0) {
                _message.append(", ");
            }
            _message.append(e.getClass().getSimpleName()).append(":").append(e.getMessage());
        }
        this.exceptions = Collections.unmodifiableList(_exceptions);
        this.message = _message.toString();
    }

    public CompositeException(Collection<Exception> errors) {
        this(null, errors);
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    @Override
    public String getMessage() {
        return message;
    }
}