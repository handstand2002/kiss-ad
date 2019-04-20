package com.brokencircuits.kissad;

import java.net.URISyntaxException;

public interface Extractor<I, O> {

  O extract(I input) throws URISyntaxException;
}
