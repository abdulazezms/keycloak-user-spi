package aziz.keycloak.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.component.ComponentModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ProviderConfigUtil {

    public static Map<String, Set<String>> getConfigMap(ComponentModel config, String configKey) {
        String configMap = config.get(configKey);
        if(configMap == null)
            return Collections.emptyMap();
        try {
            TypeReference<List<ProviderConfigUtil.StringPair>> MAP_TYPE_REPRESENTATION = new TypeReference<List<ProviderConfigUtil.StringPair>>() {};
            List<ProviderConfigUtil.StringPair> map = JsonSerialization.readValue(configMap, MAP_TYPE_REPRESENTATION);
            return map.stream().collect(
                    Collectors.collectingAndThen(
                            Collectors.groupingBy(ProviderConfigUtil.StringPair::getKey,
                                    Collectors.mapping(ProviderConfigUtil.StringPair::getValue, Collectors.toUnmodifiableSet())),
                            Collections::unmodifiableMap));
        } catch (IOException e) {
            throw new RuntimeException("Could not deserialize json: " + configMap, e);
        }
    }

    static class StringPair {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
