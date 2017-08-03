function print4<T, A, C>(): (t: T, a: A) => (c: A) => (d: T) => (e: C) => string {
  return (t, a) => c => d => e => "hi there";
}

print4<string, number, House>()("hi", "there")(4)("world")<caret>;