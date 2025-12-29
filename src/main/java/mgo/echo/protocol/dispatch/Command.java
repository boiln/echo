package mgo.echo.protocol.dispatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a command handler.
 * 
 * Usage:
 * 
 * <pre>
 * {@code @Command(0x4100)
 * public boolean getCharacterInfo(CommandContext ctx) {
 *     // handle command
 *     return true; // wrote response
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    /**
     * The command ID this method handles.
     */
    int value();
}
