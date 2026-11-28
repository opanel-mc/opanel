package cn.opanel.api.exception;

/**
 * Thrown when an operation requires a Minecraft server implementation but
 * OPanel has not attached one or the server is no longer available.
 *
 * <p>当某项操作需要 Minecraft 服务器实现，但 OPanel 尚未附加服务器
 * 或服务器已不再可用时抛出。</p>
 */
public final class ServerUnavailableException extends OPanelAPIException { // 服务器不可用异常
    public ServerUnavailableException(String message) { // 使用描述性消息构造异常
        super(message);
    }
}
