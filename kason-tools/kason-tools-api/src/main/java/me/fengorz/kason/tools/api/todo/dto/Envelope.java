package me.fengorz.kason.tools.api.todo.dto;

import lombok.Data;

@Data
public class Envelope<T> {
    private T data;
    private Object meta;
    public static <T> Envelope<T> of(T d) { Envelope<T> e = new Envelope<>(); e.setData(d); return e; }
    public Envelope<T> meta(Object m){ this.meta=m; return this; }
}

