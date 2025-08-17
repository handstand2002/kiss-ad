package com.brokencircuits.kissad.domain.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InitMsg.class, name = "INIT"),
    @JsonSubTypes.Type(value = ShowUpdateMsg.class, name = "UPDATE_SHOW"),
})
public interface ServerMsg {

}
