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

import java.util.concurrent.atomic.AtomicBoolean;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;

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
import rx.libgdx.events.box2d.BeginContactEvent;
import rx.libgdx.events.box2d.ContactEvent;
import rx.libgdx.events.box2d.EndContactEvent;
import rx.libgdx.events.box2d.PostSolveContactEvent;
import rx.libgdx.events.box2d.PreSolveContactEvent;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

public enum GdxEventSource { ; // no instances

    /**
     * @see rx.GdxObservable#fromBox2DContact
     */
    public static Observable<ContactEvent> fromBox2DContact(final World world) {
        return create(new OnSubscribeFunc<ContactEvent>() {
            private final AtomicBoolean subscribed = new AtomicBoolean(true);
            @Override
            public Subscription onSubscribe(final Observer<? super ContactEvent> observer) {
                world.setContactListener(new ContactListener() {
                    @Override
                    public void beginContact(Contact contact) {
                        if (subscribed.get()) {
                            observer.onNext(new BeginContactEvent(contact));
                        }
                    }
  
                    @Override
                    public void endContact(Contact contact) {
                        if (subscribed.get()) {
                            observer.onNext(new EndContactEvent(contact));
                        }
                    }
  
                    @Override
                    public void preSolve(Contact contact, Manifold oldManifold) {
                      if (subscribed.get()) {
                          observer.onNext(new PreSolveContactEvent(contact, oldManifold));
                      }
                    }
  
                    @Override
                    public void postSolve(Contact contact, ContactImpulse impulse) {
                        if (subscribed.get()) {
                            observer.onNext(new PostSolveContactEvent(contact, impulse));
                        }
                    }
                });
                
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        subscribed.set(false);
                    }
                });
            }
        });
    }
    
    /**
     * Returns all "Begin Contact" events. Use this after publishing via {@link rx.GdxObservable#fromBox2DContact}.
     * @param source The observable of contact events to use as source.
     * @return An observable emitting "Begin Contact" events.
     */
    public static Observable<BeginContactEvent> beginContact(Observable<? extends ContactEvent> source) {
        return filtered(source, BeginContactEvent.class);
    }
    
    /**
     * Returns all "End Contact" events. Use this after publishing via {@link rx.GdxObservable#fromBox2DContact}.
     * @param source The observable of contact events to use as source.
     * @return An observable emitting "End Contact" events.
     */
    public static Observable<EndContactEvent> endContact(Observable<? extends ContactEvent> source) {
        return filtered(source, EndContactEvent.class);
    }
    
    /**
     * Returns all "PreSolve" events. Use this after publishing via {@link rx.GdxObservable#fromBox2DContact}.
     * @param source The observable of contact events to use as source.
     * @return An observable emitting "PreSolve" events.
     */
    public static Observable<PreSolveContactEvent> preSolve(Observable<? extends ContactEvent> source) {
        return filtered(source, PreSolveContactEvent.class);
    }
    
    /**
     * Returns all "PostSolve" events. Use this after publishing via {@link rx.GdxObservable#fromBox2DContact}.
     * @param source The observable of contact events to use as source.
     * @return An observable emitting "PostSolve" events.
     */
    public static Observable<PostSolveContactEvent> postSolve(Observable<? extends ContactEvent> source) {
        return filtered(source, PostSolveContactEvent.class);
    }
    
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
                        if (wrapped == null || !wrapped.touchUp(screenX, screenY, pointer, button)) {
                            observer.onNext(new TouchUpEvent(screenX, screenY, pointer, button));
                        }
                        return true;
                    }

                    @Override
                    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                        if (wrapped == null || !wrapped.touchDown(screenX, screenY, pointer, button)) {
                            observer.onNext(new TouchDownEvent(screenX, screenY, pointer, button));
                        }
                        return true;
                    }

                    @Override
                    public boolean touchDragged(int screenX, int screenY, int pointer) {
                        if (wrapped == null || !wrapped.touchDragged(screenX, screenY, pointer)) {
                            observer.onNext(new TouchDraggedEvent(screenX, screenY, pointer));
                        }
                        return true;
                    }

                    @Override
                    public boolean keyDown(int keycode) {
                        if (wrapped == null || !wrapped.keyDown(keycode)) {
                            observer.onNext(new KeyDownEvent(keycode));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean keyUp(int keycode) {
                        if (wrapped == null || !wrapped.keyUp(keycode)) {
                            observer.onNext(new KeyUpEvent(keycode));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean keyTyped(char character) {
                        if (wrapped == null || !wrapped.keyTyped(character)) {
                            observer.onNext(new KeyTypedEvent(character));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean mouseMoved(int screenX, int screenY) {
                        if (wrapped == null || !wrapped.mouseMoved(screenX, screenY)) {
                            observer.onNext(new MouseMovedEvent(screenX, screenY));
                        }
                        return true;
                    }
              
                    @Override
                    public boolean scrolled(int amount) {
                        if (wrapped == null || !wrapped.scrolled(amount)) {
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
    public static Observable<TouchUpEvent> touchUp(Observable<? extends InputEvent> source) {
        return filtered(source, TouchUpEvent.class);
    }
    
    /**
     * Returns all "Touch Down" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Touch Down" events.
     */
    public static Observable<TouchDownEvent> touchDown(Observable<? extends InputEvent> source) {
        return filtered(source, TouchDownEvent.class);
    }
  
    /**
     * Returns all "Touch Dragged" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Touch Dragged" events.
     */
    public static Observable<TouchDraggedEvent> touchDragged(Observable<? extends InputEvent> source) {
        return filtered(source, TouchDraggedEvent.class);
    }

    /**
     * Returns all "Mouse Moved" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Mouse Moved" events.
     */
    public static Observable<MouseMovedEvent> mouseMoved(Observable<? extends InputEvent> source) {
        return filtered(source, MouseMovedEvent.class);
    }

    /**
     * Returns all "Scrolled" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Scrolled" events.
     */
    public static Observable<ScrolledEvent> scrolled(Observable<? extends InputEvent> source) {
        return filtered(source, ScrolledEvent.class);
    }

    /**
     * Returns all "Key Typed" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Key Typed" events.
     */
    public static Observable<KeyTypedEvent> keyTyped(Observable<? extends InputEvent> source) {
        return filtered(source, KeyTypedEvent.class);
    }

    /**
     * Returns all "Key Up" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Key Up" events.
     */
    public static Observable<KeyUpEvent> keyUp(Observable<? extends InputEvent> source) {
        return filtered(source, KeyUpEvent.class);
    }

    /**
     * Returns all "Key Down" events. Use this after publishing via {@link rx.GdxObservable#fromInput}.
     * @param source The observable of input events to use as source.
     * @return An observable emitting "Key Down" events.
     */
    public static Observable<KeyDownEvent> keyDown(Observable<? extends InputEvent> source) {
        return filtered(source, KeyDownEvent.class);
    }
  
    private static <U, T extends U> Observable<T> filtered(Observable<? extends U> source, final Class<T> clazz) {
        return source.filter(new Func1<U, Boolean>() {
            @Override
            public Boolean call(U event) {
                return clazz.isInstance(event);
            }
        }).map(new Func1<U, T>() {
            @Override
            public T call(U event) {
                return clazz.cast(event);
            }
        });
    }
}
