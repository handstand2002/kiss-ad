package com.brokencircuits.kissad.domain.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ShowListingMsg.class, name = "SHOW_LISTING"),
})
public interface UiMsg {

}
