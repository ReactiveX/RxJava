/*
 * This is the confidential unpublished intellectual property of EMC Corporation,
 * and includes without limitation exclusive copyright and trade secret rights
 * of EMC throughout the world.
 */
package rx.internal.operators;

import rx.functions.Func1;

/**
 * Converts the element of a Single to the specified type.
 * @param <T> the input value type
 * @param <R> the output value type
 */
public class SingleOperatorCast<T, R> implements Func1<T, R> {

  final Class<R> castClass;

  public SingleOperatorCast(Class<R> castClass) {
    this.castClass = castClass;
  }

  @Override
  public R call(T t) {
    return castClass.cast(t);
  }
}
