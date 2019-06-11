package run.parsers;

import effekt.*;
import effekt.stateful.State;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Optional;

// P<R> = CharParsers -> R
public class Example implements Runnable {

    public static int numberInParens(CharParsers p) throws Effects {
        if (p.alternative()) {
            int res;
            p.expect('(');
            res = numberInParens(p);
            p.expect(')');
            return res;
        } else {
            return p.number();
        }
    }

    public static int something(CharParsers p) throws Effects {
        int res;

        p.expect('a');
        if (p.alternative()) {
            p.expect('1');
            res = 1;
        } else {
            p.expect('2');
            res = 2;
        }

        p.expect('b');
        return res;
    }

    public static int somethingPush(CharParsers ps) throws Effects {
        PushParser<Integer> p = ToPush.convert(ps, Example::something);

        p = p.feed(ps.any());
        p = p.feed('2');
        return p.feedAll(ps);
    }

    public static int someNumberDot(CharParsers p) throws Effects {
        return p.nonterminal("someNumberDot", () -> {
            System.out.println("someNumberDot");

            int res = p.number();
            p.expect('.');
            return res;
        });
    }

    public static int backtrackingExample(CharParsers p) throws Effects {
        if (p.alternative()) {
            return someNumberDot(p) + someNumberDot(p);
        } else {
            return someNumberDot(p);
        }
    }

    public static int backtrackingDelegation(CharParsers ps) throws Effects {
        PushParser<Integer> p = ToPush.convert(ps, Example::backtrackingExample);
        return p.feedAll(ps);
    }

    public static int backtrackingDelegation2(CharParsers ps) throws Effects {
        if (ps.alternative()) {
            return someNumberDot(ps) + someNumberDot(ps);
        } else {
            PushParser<Integer> p = ToPush.convert(ps, Example::someNumberDot);
            p = p.feed('1');
            return p.feedAll(ps);
        }
    }

    public static Optional<Integer> backtrackingExample(String in) throws Effects {
        return Parser.parse(in, Example::backtrackingExample);
    }

    public static Optional<Integer> backtrackingDelegation(String in) throws Effects {
        return Parser.parse(in, Example::backtrackingDelegation);
    }

    public static Optional<Integer> backtrackingDelegation2(String in) throws Effects {
        return Parser.parse(in, Example::backtrackingDelegation2);
    }

    public static Optional<Integer> somethingPush(String in) throws Effects {
        return Parser.parse(in, Example::somethingPush);
    }

    public static Optional<Integer> something(String in) throws Effects {
        return Parser.parse(in, Example::something);
    }

    public static Optional<Integer> number(String in) throws Effects {
        return Parser.parse(in, p -> p.number());
    }
    public static Optional<Integer> numberInParens(String in) throws Effects {
        return Parser.parse(in, Example::numberInParens);
    }

    public void run() {
        Effekt.run(() -> {
            System.out.println("--- something ---");
            System.out.println(something("a1b"));
            System.out.println(something("a2b"));
            System.out.println(something("a3b"));

            System.out.println("--- number ---");
            System.out.println(number("0"));
            System.out.println(number("13"));
            System.out.println(number("558"));

            System.out.println("--- numberInParens ---");
            System.out.println(numberInParens("558"));
            System.out.println(numberInParens("(558)"));
            System.out.println(numberInParens("(((558)))"));
            System.out.println(numberInParens("(((558())"));

            System.out.println("--- somethingPush ---");
            System.out.println(somethingPush("ab"));

            System.out.println("--- backtrackingExample ---");
            System.out.println(backtrackingExample("1234."));

            System.out.println("--- backtrackingDelegation ---");
            System.out.println(backtrackingDelegation("1234."));

            System.out.println("--- backtrackingDelegation2 ---");
            System.out.println(backtrackingDelegation2("1234."));
            return null;
        });
    }

}

interface CharParsers extends Parser<Character> {

    default int digit() throws Effects {
        char c = any();

        if (!Character.isDigit(c))
            return fail("Expected a digit");
        else
            return Character.getNumericValue(c);
    }

    default int number() throws Effects {
        int res = digit();

        while (true) {
            if (alternative()) {
                res = res * 10 + digit();
            } else {
                return res;
            }
        }
    }

}

interface Parser<S> {

    // the reader effect
    S any() throws Effects;

    // the exception effect
    <A> A fail(String explanation) throws Effects;

    // the ambiguity effect
    // TODO for more control, define a combinator over P<A>s then we can handle the parser effects locally
    boolean alternative() throws Effects;

    // the memoization effect
    <A> A nonterminal(String name, Prog<A> body) throws Effects;

    default void expect(S token) throws Effects {
        final S res = any();
        if (res != token)
            fail("Expected " + token + ", but got " + res);
    }

    default int alternatives(int n) throws Effects {
        for (int i = 1; i < n; i++) {
            if (alternative())
                return i;
        }
        return 0;
    }

    static <A> Optional<A> parse(String input, Effectful<CharParsers, A> parser) throws Effects {
        StringParser<A> parserImpl = new StringParser<>(input);
        return parserImpl.handle(() -> parser.apply(parserImpl));
    }
}

class StringParser<R> extends State implements CharParsers, Handler<R, Optional<R>>  {

    // HashMap[(Position, NonterminalName), (Position, Result)]
    final HashMap<Tuple2<Integer, String>, Tuple2<Integer, Object>> cache = new HashMap<>();

    final String input;

    final Field<Integer> pos = field(0);

    StringParser(String input) { this.input = input; }

    public Optional<R> pure(R r) throws Effects { return Optional.of(r); }
    public Character any() throws Effects {
        if (pos.get() >= input.length()) {
            return fail("Unexpected EOS");
        } else {
            Character c = input.charAt(pos.get());
            pos.put(pos.get() + 1);
            return c;
        }
    }
    public boolean alternative() throws Effects {
        // does this lead to left biased choice?
        return use(k -> {
            int before = pos.get();
            Optional<R> res = k.resume(true);
            if (res.isPresent())
                return res;

            pos.put(before);
            return k.resume(false);
        });
    }

    public <A> A fail(String explanation) throws Effects { return discard(() -> Optional.empty()); }

    public <A> A nonterminal(String name, Prog<A> body) throws Effects {

        // We could as well use body.getClass().getCanonicalName() as key.
        Tuple2<Integer, String> hashKey = new Tuple2<>(pos.get(), name);

        if (cache.containsKey(hashKey)) {
            Tuple2<Integer, Object> res = cache.get(hashKey);
            pos.put(res._1);
            return (A) res._2;
        }

        A res = body.apply();
        cache.put(hashKey, new Tuple2<>(pos.get(), res));
        return res;
    }
}

class ToPush<R> implements Handler<R, PushParser<R>>, Parser<Character>, CharParsers {

    // would be cool to be able to set super = outer here for easier capability safety
    final private Parser<Character> outer;

    ToPush(Parser<Character> outer) {
        this.outer = outer;
    }

    public PushParser<R> pure(R r) throws Effects {
        return PushParser.succeed(r);
    }

    public Character any() throws Effects {
        return use(k ->
            new PushParser<R>() {
                public R result() { return null; }

                public boolean isDone() { return false; }

                public PushParser<R> feed(char el) throws Effects {
                    return k.resume(el);
                }
            }
        );

    }

    public <A> A fail(String explanation) throws Effects {
        return outer.fail(explanation);
    }

    public boolean alternative() throws Effects {
        return outer.alternative();
    }

    public <A> A nonterminal(String name, Prog<A> body) throws Effects {
        // for now, push parsers normally don't memoize
        return body.apply(); //outer.nonterminal(name, body);
    }

    static <A> PushParser<A> convert(CharParsers outer, Effectful<CharParsers, A> parser) throws Effects {
        ToPush<A> parserImpl = new ToPush<>(outer);
        return parserImpl.handle(() -> parser.apply(parserImpl));
    }
}

// coalgebraic / push-based parsers, specialized to characters
interface PushParser<R> {
    R result();
    boolean isDone();
    PushParser<R> feed(char el) throws Effects;

    default R feedAll(Parser<Character> p) throws Effects {
        PushParser<R> self = this;
        while(!self.isDone()) {
            self = self.feed(p.any());
        }
        return self.result();
    }

    static <R> PushParser<R> succeed(R r) {
        return new PushParser<R>() {
            public boolean isDone() { return true; }
            public R result() { return r; }
            public PushParser<R> feed(char el) { return PushParser.fail; }
        };
    }

    static PushParser fail = new FailParser<>();
}
class FailParser<R> implements PushParser<R> {
    public R result() { return null; }
    public boolean isDone() { return false; }
    public PushParser<R> feed(char el) { return this; }
}
