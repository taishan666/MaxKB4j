package com.maxkb4j.system.strategy;

import com.maxkb4j.system.entity.SourceResource;

import java.util.Collection;
import java.util.List;

public interface SourceResourceResolver {

    String resourceType();

    List<SourceResource> resolve(Collection<String> ids, String resourceName, List<String> userIdFilter);
}
