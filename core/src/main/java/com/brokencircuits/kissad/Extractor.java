package com.brokencircuits.kissad;

public interface Extractor<I, O> {

  O extract(I input) throws Exception;
}
