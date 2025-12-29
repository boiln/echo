package mgo.echo.protocol.dispatch;

/**
 * Marker interface for command controllers.
 * 
 * Controllers contain methods annotated with @Command that handle specific
 * command IDs.
 * The CommandRegistry scans controllers for these methods at startup.
 * 
 * Example:
 * 
 * <pre>
 * {@code
 * public class CharacterController implements Controller { @Command(0x4100)
 *     public boolean getCharacterInfo(CommandContext ctx) {
 *         // ...
 *         return true;
 *     }
 * }
 * }
 * </pre>
 */
public interface Controller {
    // Marker interface - no methods required
}
