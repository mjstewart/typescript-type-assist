function booking<T, U, X, P>(id: number): number | State1<U, X, P> | boolean | House<T & P> | State2<X & P>[] {
  return 'hi';
}

const booking = booking<string, Config, House, boolean>(5)<caret>;