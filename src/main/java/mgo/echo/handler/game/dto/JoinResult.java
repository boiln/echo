package mgo.echo.handler.game.dto;

/** Result of join validation. */
public class JoinResult {
    private final boolean success;
    private final Integer errorCode;
    private final String publicIp;
    private final int publicPort;
    private final String privateIp;
    private final int privatePort;
    private final int currentMap;
    private final int currentRule;

    private JoinResult(
            boolean success,
            Integer errorCode,
            String publicIp,
            int publicPort,
            String privateIp,
            int privatePort,
            int currentMap,
            int currentRule) {
        this.success = success;
        this.errorCode = errorCode;
        this.publicIp = publicIp;
        this.publicPort = publicPort;
        this.privateIp = privateIp;
        this.privatePort = privatePort;
        this.currentMap = currentMap;
        this.currentRule = currentRule;
    }

    public static JoinResult error(int errorCode) {
        return new JoinResult(false, errorCode, null, 0, null, 0, 0, 0);
    }

    public static JoinResult success(
            String publicIp,
            int publicPort,
            String privateIp,
            int privatePort,
            int currentMap,
            int currentRule) {
        return new JoinResult(true, null, publicIp, publicPort, privateIp, privatePort, currentMap, currentRule);
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public int getPublicPort() {
        return publicPort;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    public int getCurrentMap() {
        return currentMap;
    }

    public int getCurrentRule() {
        return currentRule;
    }
}
