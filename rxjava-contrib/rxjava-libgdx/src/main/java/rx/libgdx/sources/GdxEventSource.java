/**
 * Copyright 2013 Netflix, Inc.
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
package rx.libgdx.sources;

import static rx.Observable.create;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.libgdx.events.InputEvent;
import rx.libgdx.events.KeyDownEvent;
import rx.libgdx.events.KeyTypedEvent;
import rx.libgdx.events.KeyUpEvent;
import rx.libgdx.events.MouseMovedEvent;
import rx.libgdx.events.ScrolledEvent;
import rx.libgdx.events.TouchDownEvent;
import rx.libgdx.events.TouchDraggedEvent;
import rx.libgdx.events.TouchUpEvent;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public enum GdxEventSource { ; // no instances

    /**
     * @see rx.GdxObservable#fromInput
     */
    public static Observable<InputEvent> fromInput() {
        return create(new OnSubscribeFunc<InputEvent>() {
            @Override
            public Subscription onSubscribe(final Observer<? super InputEvent> observer) {
                final InputProcessor wrapped = Gdx.input.getInputProcessor();
                Gdx.app.getInput().setInputProcessor(new InputProcessor() {
                    @Override
                    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                        if (!wrapped.touchUp(screenX, screenY, pointer, button)) {
                            observer.onNext(new TouchUpEvent(screenX, screenY, pointer, button));
                        }
                        return true;
                    }

                    @Override
                    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                        if (!wrapped.touchDown(screenX, screenY, pointer, button)) {
                            observer.onNext(new TouchDownEvent(screenX, screenY, pointer, button));
                        }
                        return true;
                    }

                    @Override
                    public boolean touchDragged(int screenX, int screenY, int pointer) {
                        if (!wrapped.touchDragged(screenX, screenY, pointer)) {
                            observer.onNext(new TouchDraggedEvent(screenX, screenY, pointer));
                        }
                        return true;
                    }

                    @Override
                    public boolean keyDown(int keycode) {
                        if (!wrapped.keyDown(keycode)) {
                            observer.onNext(new KeyDownEvent(keycode));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean keyUp(int keycode) {
                        if (!wrapped.keyUp(keycode)) {
                            observer.onNext(new KeyUpEvent(keycode));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean keyTyped(char character) {
                        if (!wrapped.keyTyped(character)) {
                            observer.onNext(new KeyTypedEvent(character));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean mouseMoved(int screenX, int screenY) {
                        if (!wrapped.mouseMoved(screenX, screenY)) {
                            observer.onNext(new MouseMovedEvent(screenX, screenY));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean scrolled(int amount) {
                        if (!wrapped.scrolled(amount)) {
                            observer.onNext(new ScrolledEvent(amount));
                        }
                        return true;
                    }
                });
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        Gdx.app.getInput().setInputProcessor(wrapped);
                    }
                });
            }
        });
    }

    /**
     * Returns all "Touch Up" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Touch Up" events.
     */
    public static Observable<TouchUpEvent> touchUp(Observable<InputEvent> source) {
        return filtered(source, TouchUpEvent.class);
    }
    
    /**
     * Returns all "Touch Down" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Touch Down" events.
     */
    public static Observable<TouchDownEvent> touchDown(Observable<InputEvent> source) {
        return filtered(source, TouchDownEvent.class);
    }
  
    /**
     * Returns all "Touch Dragged" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Touch Dragged" events.
     */
    public static Observable<TouchDraggedEvent> touchDragged(Observable<InputEvent> source) {
        return filtered(source, TouchDraggedEvent.class);
    }

    /**
     * Returns all "Mouse Moved" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Mouse Moved" events.
     */
    public static Observable<MouseMovedEvent> mouseMoved(Observable<InputEvent> source) {
        return filtered(source, MouseMovedEvent.class);
    }

    /**
     * Returns all "Scrolled" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Scrolled" events.
     */
    public static Observable<ScrolledEvent> scrolled(Observable<InputEvent> source) {
        return filtered(source, ScrolledEvent.class);
    }

    /**
     * Returns all "Key Typed" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Key Typed" events.
     */
    public static Observable<KeyTypedEvent> keyTyped(Observable<InputEvent> source) {
        return filtered(source, KeyTypedEvent.class);
    }

    /**
     * Returns all "Key Up" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Key Up" events.
     */
    public static Observable<KeyUpEvent> keyUp(Observable<InputEvent> source) {
        return filtered(source, KeyUpEvent.class);
    }

    /**
     * Returns all "Key Down" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Key Down" events.
     */
    public static Observable<KeyDownEvent> keyDown(Observable<InputEvent> source) {
        return filtered(source, KeyDownEvent.class);
    }
  
    private static <T extends InputEvent> Observable<T> filtered(Observable<InputEvent> source, final Class<T> clazz) {
        return source.filter(new Func1<InputEvent, Boolean>() {
            @Override
            public Boolean call(InputEvent event) {
                return clazz.isInstance(event);
            }
        }).map(new Func1<InputEvent, T>() {
            @Override
            public T call(InputEvent event) {
                return clazz.cast(event);
            }
        });
    }
}
