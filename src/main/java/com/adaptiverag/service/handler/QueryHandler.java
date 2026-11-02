package com.adaptiverag.service.handler;

import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.model.RouteDecision;

/**
 * One strategy per route. Adding a new source means adding an implementation;
 * the dispatcher picks it up automatically.
 */
public interface QueryHandler {

    RouteDecision route();

    QueryResponse handle(String query);
}
