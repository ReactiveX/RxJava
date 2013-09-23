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
package rx.swing.sources;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.junit.Test;
import org.mockito.Matchers;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

public enum ChangeEventSource { ; // no instances
	
	public static interface ChangeEventComponentWrapper {
		void addChangeListener(ChangeListener l);
		void removeChangeListener(ChangeListener l);
	}

	public static Observable<ChangeEvent> fromChangeEventsOf(final ChangeEventComponentWrapper w) {
        return Observable.create(new OnSubscribeFunc<ChangeEvent>() {
            @Override
            public Subscription onSubscribe(final Observer<? super ChangeEvent> observer) {
                final ChangeListener listener = new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent event) {
						observer.onNext(event);
					}
                };                
                w.addChangeListener(listener);
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        w.removeChangeListener(listener);
                    }
                });
            }
        });
    }

	public static class UnitTest {
		
		class TestCompWrapper implements ChangeEventComponentWrapper {
			final JSpinner spinner;
			public TestCompWrapper() {
				spinner = new JSpinner();
				spinner.setModel(new SpinnerNumberModel(5, 0, 10, 1)); // 0..10, step 1, initial value 5
			}
			void changeTo(int to) {
				spinner.setValue(to);
			}
			@Override
			public void addChangeListener(ChangeListener l) {
				spinner.addChangeListener(l);
			}
			@Override
			public void removeChangeListener(ChangeListener l) {
				spinner.removeChangeListener(l);
			}
		}
		
		@Test
		public void testObservingChangeEvents() {
			@SuppressWarnings("unchecked")
			Action1<ChangeEvent> action = mock(Action1.class);
			@SuppressWarnings("unchecked")
			Action1<Throwable> error = mock(Action1.class);
			Action0 complete = mock(Action0.class);

			TestCompWrapper w = new TestCompWrapper();

			Subscription sub = fromChangeEventsOf(w).subscribe(action, error, complete);

			verify(action, never()).call(Matchers.<ChangeEvent> any());
			verify(error, never()).call(Matchers.<Throwable> any());
			verify(complete, never()).call();

			w.changeTo(6);
			verify(action, times(1)).call(Matchers.<ChangeEvent> any());

			w.changeTo(7);
			verify(action, times(2)).call(Matchers.<ChangeEvent> any());

			sub.unsubscribe();
			w.changeTo(8);
			verify(action, times(2)).call(Matchers.<ChangeEvent> any());
			verify(error, never()).call(Matchers.<Throwable> any());
			verify(complete, never()).call();
		}
	}
	
}
