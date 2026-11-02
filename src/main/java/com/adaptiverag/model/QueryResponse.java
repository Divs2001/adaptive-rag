package com.adaptiverag.model;

import java.util.List;

public record QueryResponse(RouteDecision route, String answer, List<Source> sources) {
}
