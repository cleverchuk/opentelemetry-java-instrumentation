/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class ConfiguredResourceAttributesHolder {

  private static final Map<String, String> resourceAttribute = new HashMap<>();

  public static Map<String, String> getResourceAttribute() {
    return resourceAttribute;
  }

  public static void initialize(Attributes resourceAttribute) {
    String[] mdcResourceAttributes = getConfiguredAttributes();

    for (String key : mdcResourceAttributes) {
      String value = resourceAttribute.get(stringKey(key));
      if (value != null) {
        ConfiguredResourceAttributesHolder.resourceAttribute.put(
            String.format("otel.resource.%s", key), value);
      }
    }
  }

  private static String[] getConfiguredAttributes() {
    String resourceAttributes =
        ConfigPropertiesUtil.getString("otel.instrumentation.mdc.resource-attributes");
    if (resourceAttributes == null) {
      return new String[] {};
    }
    return resourceAttributes.split(",");
  }

  @Nullable
  public static String getAttributeValue(String key) {
    return resourceAttribute.get(String.format("otel.resource.%s", key));
  }

  private ConfiguredResourceAttributesHolder() {}
}