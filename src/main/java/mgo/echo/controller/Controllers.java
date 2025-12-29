package mgo.echo.controller;

import java.util.ArrayList;
import java.util.List;

import mgo.echo.protocol.dispatch.CommandRegistry;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.protocol.dispatch.RegistryDispatcher;

/**
 * Factory for creating controller registries.
 * 
 * Centralizes controller instantiation and registration.
 */
public final class Controllers {
    private Controllers() {
    }

    /**
     * Creates the registry for game lobby commands.
     */
    public static RegistryDispatcher createGameLobbyDispatcher() {
        List<Controller> controllers = new ArrayList<>();

        controllers.add(new AccountController());
        controllers.add(new CharacterController());
        controllers.add(new HubController());
        controllers.add(new GameController());
        controllers.add(new HostController());
        controllers.add(new MessageController());
        controllers.add(new ClanController());

        CommandRegistry registry = CommandRegistry.builder()
                .registerAll(controllers)
                .build();

        return new RegistryDispatcher(registry);
    }

    /**
     * Creates the registry for gate lobby commands.
     */
    public static RegistryDispatcher createGateLobbyDispatcher() {
        List<Controller> controllers = new ArrayList<>();

        controllers.add(new GateController());

        CommandRegistry registry = CommandRegistry.builder()
                .registerAll(controllers)
                .build();

        return new RegistryDispatcher(registry);
    }

    /**
     * Creates the registry for account lobby commands.
     */
    public static RegistryDispatcher createAccountLobbyDispatcher() {
        List<Controller> controllers = new ArrayList<>();

        controllers.add(new AccountLobbyController());

        CommandRegistry registry = CommandRegistry.builder()
                .registerAll(controllers)
                .build();

        return new RegistryDispatcher(registry);
    }
}
