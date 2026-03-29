package com.bootcloud.boot.condition;

import com.bootcloud.boot.annotation.ConditionalOnProperty;
import com.bootcloud.boot.context.BootApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnPropertyCondition implements Condition {
    private static final Logger logger = LoggerFactory.getLogger(OnPropertyCondition.class);

    @Override
    public boolean matches(BootApplicationContext context) {
        return true;
    }

    public boolean matches(BootApplicationContext context, String name, String value, boolean havingValue, boolean matchIfMissing) {
        String propertyValue = context.getEnvironment().getProperty(name);
        
        if (propertyValue == null) {
            logger.debug("Property {} not found, using matchIfMissing: {}", name, matchIfMissing);
            return matchIfMissing;
        }

        if (value.isEmpty()) {
            boolean matches = Boolean.parseBoolean(propertyValue) == havingValue;
            logger.debug("Property {} = {}, havingValue = {}, matches: {}", name, propertyValue, havingValue, matches);
            return matches;
        }

        boolean matches = propertyValue.equals(value);
        logger.debug("Property {} = {}, expected = {}, matches: {}", name, propertyValue, value, matches);
        return matches;
    }
}
