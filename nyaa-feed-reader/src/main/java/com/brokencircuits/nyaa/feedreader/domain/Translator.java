package com.brokencircuits.nyaa.feedreader.domain;

public interface Translator<I, O> {
  O apply(I input);
}
