package com.brokencircuits.kissad;

@FunctionalInterface
public interface Translator<I, O> {

  O translate(I input);
}
