package run.paper;

import effekt.*;
import effekt.stateful.Stateful;
import run.Lists;
import run.amb.*;
import run.exc.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import static effekt.Handler.handle;

public class UseCases implements Runnable {

    // Use Case: Parser

    // (1) receive
    // just like reader, retreives values from the context.
    // handler can capture continuation and pause the request

    // with receive we can implement a simple parser

    interface Parser extends Amb, Exc, Receive<Character> {
        default Character accept(Predicate<Character> p) throws Effects {
            Character t = this.receive();
            return p.test(t) ? t : this.raise("Didn't match " + p);
        }
//        default Character accept(Character t) throws Effects {
//            return accept(t2 -> t2 == t);
//        }
    }

    abstract class NumberParser implements Parser {
        int digit() throws Effects {
            return Character.getNumericValue(accept(Character::isDigit));
        }
        int number() throws Effects {
            int res = digit();

            while (true) {
                if (flip()) {
                    res = res * 10 + digit();
                } else {
                    return res;
                }
            }
        }
    }

    // P<R> = Parser -> R
    public static int digit(Parser p) throws Effects {
        return Character.getNumericValue(p.accept(Character::isDigit));
    }
    public static int number(Parser p) throws Effects {
        int res = digit(p);

        while (true) {
            if (p.flip()) {
                res = res * 10 + digit(p);
            } else {
                return res;
            }
        }
    }

    public static class StringReader<R> implements Handler<R, R>, Receive<Character>, Stateful<Integer> {
        final Exc exc;
        final String input;

        int pos = 0;

        StringReader(String in, Exc exc) { this.input = in; this.exc = exc; }

        public R pure(R value) throws Effects { return value; }

        public Character receive() throws Effects {
            if (pos >= input.length()) {
                return exc.raise("Unexpected EOS");
            } else {
                Character c = input.charAt(pos);
                pos = pos + 1;
                return c;
            }
        }

        public Integer exportState() { return pos; }
        public void importState(Integer state) { pos = state; }
    }

    // just delegating to other handlers
    public static class ParserImpl implements Parser {
        final Exc exc;
        final Amb amb;
        final Receive<Character> reader;

        ParserImpl(Exc exc, Amb amb, Receive<Character> reader) {
            this.exc = exc; this.amb = amb; this.reader = reader;
        }
        public boolean flip() throws Effects { return amb.flip(); }
        public <A> A raise(String msg) throws Effects { return exc.raise(msg); }
        public Character receive() throws Effects { return reader.receive(); }
    }

    // monad Nondet
    public static class Nondet<R> implements AmbList<R>, Exc {
        public <A> A raise(String msg) throws Effects {
            return use(k -> Lists.empty());
        }
    }

    public static class Backtrack<R> implements Maybe<R>, Amb {
        public boolean flip() throws Effects {
            return use(k -> {
                Optional<R> res = k.resume(true);
                if (res.isPresent())
                    return res;

                return k.resume(false);
            });
        }
    }

    public static <R> List<R> parseAll(String in, Effectful<Parser, R> parser) throws Effects {
        return handle(new Nondet<>(), nd ->
            handle(new StringReader<>(in, nd), r ->
                parser.apply(new ParserImpl(nd, nd, r))
            ));
    }
    public static <R> Optional<R> parseBacktrack(String in, Effectful<Parser, R> parser) throws Effects {
        return handle(new Backtrack<>(), nd ->
            handle(new StringReader<>(in, nd), r ->
                parser.apply(new ParserImpl(nd, nd, r))
            ));
    }

    public void run() {
        Effekt.run(() -> {
            List<Integer> res1 = parseAll("123", UseCases::number);
            System.out.println(res1);
            Optional<Integer> res2 = parseBacktrack("123", UseCases::number);
            System.out.println(res2);
            return null;
        });
    }
}
