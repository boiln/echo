package mgo.echo.protocol.dispatch;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registry that discovers and stores command handlers from controllers.
 * 
 * Scans controllers for methods annotated with @Command and builds
 * a lookup table for fast dispatch.
 */
public final class CommandRegistry {
    private static final Logger logger = LogManager.getLogger(CommandRegistry.class);

    private final Map<Integer, RegisteredHandler> handlers;

    private CommandRegistry(Map<Integer, RegisteredHandler> handlers) {
        this.handlers = Collections.unmodifiableMap(handlers);
    }

    public RegisteredHandler get(int command) {
        return handlers.get(command);
    }

    public boolean has(int command) {
        return handlers.containsKey(command);
    }

    public int size() {
        return handlers.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Integer, RegisteredHandler> handlers = new HashMap<>();

        private Builder() {
        }

        /**
         * Register all @Command methods from a controller.
         */
        public Builder register(Controller controller) {
            Class<?> clazz = controller.getClass();

            for (Method method : clazz.getDeclaredMethods()) {
                Command annotation = method.getAnnotation(Command.class);
                if (annotation == null) {
                    continue;
                }

                int commandId = annotation.value();
                validateMethod(method, commandId);

                if (handlers.containsKey(commandId)) {
                    RegisteredHandler existing = handlers.get(commandId);
                    logger.warn("Duplicate handler for command 0x{}: {} overwrites {}",
                            Integer.toHexString(commandId),
                            formatMethod(controller, method),
                            formatMethod(existing.controller(), existing.method()));
                }

                method.setAccessible(true);
                handlers.put(commandId, new RegisteredHandler(controller, method));
                logger.debug("Registered command 0x{} -> {}",
                        Integer.toHexString(commandId),
                        formatMethod(controller, method));
            }

            return this;
        }

        /**
         * Register multiple controllers at once.
         */
        public Builder registerAll(Controller... controllers) {
            for (Controller controller : controllers) {
                register(controller);
            }

            return this;
        }

        /**
         * Register multiple controllers at once.
         */
        public Builder registerAll(List<Controller> controllers) {
            for (Controller controller : controllers) {
                register(controller);
            }

            return this;
        }

        public CommandRegistry build() {
            logger.info("Built command registry with {} handlers", handlers.size());
            return new CommandRegistry(handlers);
        }

        private void validateMethod(Method method, int commandId) {
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !CommandContext.class.isAssignableFrom(params[0])) {
                throw new IllegalArgumentException(
                        String.format("@Command method %s.%s must have exactly one parameter of type CommandContext",
                                method.getDeclaringClass().getSimpleName(),
                                method.getName()));
            }

            Class<?> returnType = method.getReturnType();
            if (returnType != boolean.class && returnType != Boolean.class && returnType != void.class) {
                throw new IllegalArgumentException(
                        String.format("@Command method %s.%s must return boolean or void",
                                method.getDeclaringClass().getSimpleName(),
                                method.getName()));
            }
        }

        private String formatMethod(Controller controller, Method method) {
            return controller.getClass().getSimpleName() + "." + method.getName() + "()";
        }
    }

    public static final class RegisteredHandler {
        private final Controller controller;
        private final Method method;

        public RegisteredHandler(Controller controller, Method method) {
            this.controller = controller;
            this.method = method;
        }

        public Controller controller() {
            return controller;
        }

        public Method method() {
            return method;
        }

        public boolean invoke(CommandContext ctx) throws Exception {
            Object result = method.invoke(controller, ctx);
            if (result == null) {
                return true;
            }
            return (Boolean) result;
        }
    }
}
