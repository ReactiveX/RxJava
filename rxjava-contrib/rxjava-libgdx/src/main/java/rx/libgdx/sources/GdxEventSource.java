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

import static org.mockito.Mockito.*;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.libgdx.events.GdxInputEvent;
import rx.libgdx.events.InputEvent;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public enum GdxEventSource { ; // no instances

    /**
     * @see rx.observables.GdxObservable#fromMouseEvents
     */
    public static Observable<InputEvent> fromMouseEventsOf(final Component component) {
        final InputProcessor current = Gdx.input.getInputProcessor();
        Gdx.app.getInput().setInputProcessor(new InputProcessor() {
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (!current.touchUp(screenX, screenY, pointer, button)) {
                  
                }
            }
            
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public boolean scrolled(int amount) {
                return current.scrolled(amount);
            }
            
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return current.mouseMoved(screenX, screenY);
            }
            
            @Override
            public boolean keyUp(int keycode) {
                return current.keyUp(keycode);
            }
            
            @Override
            public boolean keyTyped(char character) {
                return current.keyTyped(character);
            }
            
            @Override
            public boolean keyDown(int keycode) {
                return current.keyDown(keycode);
            }
        });
        
        // TODO
        
        return null;
    }
    
}
