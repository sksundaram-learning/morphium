package de.caluga.morphium.aggregation;

import de.caluga.morphium.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Stephan Bösebeck
 * Date: 31.08.12
 * Time: 08:48
 * <p/>
 * Aggregator-Group
 */
@SuppressWarnings("UnusedDeclaration")
public class Group<T, R> {
    private Logger log = new Logger(Group.class);
    private Aggregator<T, R> aggregator;
    private Map<String, Object> id;
    private boolean ended = false;

    private List<Map<String, Object>> operators = new ArrayList<>();

    public Group(Aggregator<T, R> ag, String id) {
        aggregator = ag;
        this.id = getMap("_id", id);
    }

    public Group(Aggregator<T, R> ag, Map<String, Object> id) {
        aggregator = ag;
        this.id = getMap("_id", id);
    }

    private Map<String, Object> getMap(String key, Object value) {
        Map<String, Object> ret = new HashMap<>();
        ret.put(key, value);
        return ret;
    }

    public Group<T, R> addToSet(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$addToSet", p));
        operators.add(o);
        return this;

    }

    public Group<T, R> addToSet(String name, Map<String, Object> param) {
        Map<String, Object> o = getMap(name, getMap("$addToSet", param));
        operators.add(o);
        return this;
    } //don't know what this actually should do???

    public Group<T, R> first(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$first", p));
        operators.add(o);
        return this;
    }

    public Group<T, R> last(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$last", p));
        operators.add(o);
        return this;
    }

    public Group<T, R> max(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$max", p));
        operators.add(o);
        return this;
    }

    public Group<T, R> min(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$min", p));
        operators.add(o);
        return this;
    }

    public Group<T, R> avg(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$avg", p));
        operators.add(o);
        return this;
    }

    public Group<T, R> push(String name, Object o) {
        Map<String, Object> jp = getMap(name, getMap("$push", o));
        operators.add(jp);
        return this;
    }
    public Group<T, R> push(String name, String vn, String value) {
        Map<String, Object> o = getMap(name, getMap("$push", getMap(vn, value)));
        operators.add(o);
        return this;
    }


    public Group<T, R> sum(String name, int p) {
        return sum(name, Integer.valueOf(p));
    }

    public Group<T, R> sum(String name, long p) {
        return sum(name, Long.valueOf(p));
    }

    public Group<T, R> sum(String name, Object p) {
        Map<String, Object> o = getMap(name, getMap("$sum", p));
        operators.add(o);
        return this;
    }

    public Group<T, R> sum(String name, String p) {
        return sum(name, (Object) p);
    }

    public Group<T, R> stdDevPop(String name, Object value) {
        operators.add(getMap(name, getMap("$stdDevPop", value)));
        return this;
    }

    public Group<T, R> stdDevSamp(String name, Object value) {
        operators.add(getMap(name, getMap("stdDevSamp", value)));
        return this;
    }




    public Aggregator<T, R> end() {
        if (ended) {
            log.error("Group.end() already called!");
            return aggregator;
        }
        Map<String, Object> params = new HashMap<>();
        params.putAll(id);
        operators.forEach(params::putAll);
        Map<String, Object> obj = getMap("$group", params);
        aggregator.addOperator(obj);
        ended = true;
        return aggregator;
    }

    public List<Map<String, Object>> getOperators() {
        return operators;
    }
}
