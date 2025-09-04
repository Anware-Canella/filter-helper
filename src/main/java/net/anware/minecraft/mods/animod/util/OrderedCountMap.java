package net.anware.minecraft.mods.animod.util;

import java.util.*;

public class OrderedCountMap<O> {

    public final Map<O, Integer> map = new HashMap<>();
    public final List<O> order = new ArrayList<>();

    public void append(O o, int count) {
        this.map.compute(o, (k, v) -> {
            if (v != null) {
                return count + v;
            } else {
                this.order.add(o);
                return count;
            }
        });
    }

    public void sort() {
        this.order.sort(Comparator.comparingInt(o -> map.get(o)));
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (O o : this.order) {
            str
                    .append(o.toString())
                    .append("\t")
                    .append(this.map.get(o))
                    .append("\n");
        }
        return str.toString();
    }
}
