package com.bootcloud.boot.condition;

import com.bootcloud.boot.annotation.ConditionalOnClass;
import com.bootcloud.boot.context.BootApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnClassCondition implements Condition {
    private static final Logger logger = LoggerFactory.getLogger(OnClassCondition.class);

    @Override
    public boolean matches(BootApplicationContext context) {
        return true;
    }

    public boolean matches(BootApplicationContext context, String[] classNames) {
        for (String className : classNames) {
            try {
                Class.forName(className);
                logger.debug("Class condition matches: {}", className);
            } catch (ClassNotFoundException e) {
                logger.debug("Class condition not matches: {}", className);
                return false;
            }
        }
        return true;
    }
}
