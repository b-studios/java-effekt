package effekt.stateful;

import scala.collection.immutable.Map;
import scala.collection.immutable.Map$;

public class State implements Stateful<Map> {

    private Map data = Map$.MODULE$.empty();

    public Map exportState() { return data; }
    public void importState(Map state) { data = state; }

    public <T> Field<T> field(T init) {
        Field<T> field = new Field<T>();
        data = data.updated(field, init);
        return field;
    }

    public class Field<T> {
        public T get() {
            return (T) data.apply(this);
        }
        public void put(T value) {
            data = data.updated(this, value);
        }
    }
}
