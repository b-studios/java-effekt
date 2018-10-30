# Java-Effekt
An Implementation of effect handlers using bytecode manipulation.

## Examples
- The world famous [Drunkflip](tests/src/test/java/run/amb/DrunkFlip.java) example.

An example of some instrumented bytecode can be found [in this gist](https://gist.github.com/b-studios/28b9ed229369b962e0083989343d5ede). The source-file is [Simple.java](tests/src/test/java/run/Simple.java).

## Project Organization
The project currently consists of the following subprojects:

- [core](core) The runtime system of jvm-effekt -- contains necessary runtime classes (like Stack).
- [instrumentation](instrumentation) The implementation of byte code instrumentation, depends on core.
- [sbtplugin](sbtplugin) An sbtplugin for offline instrumentation of classfiles.
- [tests](tests) A [separate](tests/build.sbt) project illustrating the use of Java Effekt.

## Dependencies
The project currently depends on version `2.0.1` of the static analysis and bytecode generation framework [OPAL](https://bitbucket.org/delors/opal/overview).

To run the tests, execute the following steps:
```
# If you haven't already, clone the repo
git clone git@github.com:b-studios/java-effekt.git
cd java-effekt

# clean all previous builds, if any to avoid sbt conflicts (only necessary once in a while ;) )
rm -r **/target
# Start sbt and publish effekt locally
sbt publishLocal

# switch to tests subproject and run tests
cd tests
sbt
> test -- -oDF
```

Running the tests will instrument the class files and dump them for inspection under `target/instrumented`.
To inspect bytecode, I highly recommend the
[OPAL - Bytecode Disassembler Plugin](https://atom.io/packages/java-bytecode-disassembler) for Atom.

## Background
[Scala-Effekt](https://github.com/b-studios/scala-effekt) is a library based implementation of
effect handlers and uses a monad to implement multiprompt delimited continuations (which are
necessary for effect handlers).
Similarly, some experiments in Java show that this is in fact a viable approach. However,
the monadic style might impose usability as well as performance issues.

This project implements stack manipulation (a la [DelimCC](http://okmij.org/ftp/continuations/implementations.html)) on the JVM by
only using the JVM stack for pure (non effectful) operations. For effectful operations, instead a
user-level [`Stack`](core/src/main/java/effekt/Effekt.java)
is used. Before each effectful operation, the continuation is pushed as a first-class frame to this stack.

These first-class frames are instances of [`Frame`](core/src/main/java/effekt/runtime/Frame.java)
and exactly represent the continuation after the effectful call. The method state is stored in the closure of the frame and restored before the original body is executed.

This is very similar to the bytecode instrumentation performed by [Quasar](http://docs.paralleluniverse.co/quasar/). In particular,
we also use Java checked exceptions to mark "effectful" functions.
However, Quasar does not have a notion of first-class frames and still uses the JVM-stack for effectful functions. Thus resuming a
continuation takes O(n) in the depth of the JVM stack.
