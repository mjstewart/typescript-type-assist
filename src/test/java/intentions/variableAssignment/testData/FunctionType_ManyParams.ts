function print4<T, A, C>(t1: Type1, t2: number): (t: T, a: A) => (c: A) => (d: T) => (e: C) => string {
    return (t, a) => c => d => e => "hi there";
}

print4<string, number, House>(t1Value, 5)<caret>;
print4<string, number, House>(t1Value, 5)("hi", "there");
print4<string, number, House>(t1Value, 5)("hi", "there")(4);
print4<string, number, House>(t1Value, 5)("hi", "there")(4)("world");
print4<string, number, House>(t1Value, 5)("hi", "there")(4)("world")(house);
print4<string, number, House>(t1Value, 5)("hi", "there")(4)("world")(house)("TooManyArgs");