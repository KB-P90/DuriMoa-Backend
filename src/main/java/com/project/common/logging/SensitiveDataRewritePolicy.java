package com.project.common.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;

@Plugin(
        name = "SensitiveDataRewritePolicy",
        category = Node.CATEGORY,
        elementType = "rewritePolicy",
        printObject = true)
public class SensitiveDataRewritePolicy implements RewritePolicy {

    @PluginFactory
    public static SensitiveDataRewritePolicy createPolicy() {
        return new SensitiveDataRewritePolicy();
    }

    @Override
    public LogEvent rewrite(LogEvent source) {
        String originalMessage = source.getMessage().getFormattedMessage();
        String maskedMessage = SensitiveDataMasker.mask(originalMessage);
        if (originalMessage.equals(maskedMessage)) {
            return source;
        }

        return new Log4jLogEvent.Builder(source)
                .setMessage(new SimpleMessage(maskedMessage))
                .build();
    }
}
