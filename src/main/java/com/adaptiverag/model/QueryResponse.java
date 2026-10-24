package com.adaptiverag.model;

import java.util.List;

public record QueryResponse(String answer, List<Source> sources) {
}
