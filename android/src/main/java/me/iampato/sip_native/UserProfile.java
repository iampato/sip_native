package me.iampato.sip_native;

public class UserProfile {
    final String username;
    final String password;
    final String domain;
    final int port;
    final String Protocol;

    public UserProfile(String username, String password, String domain, int port, String protocol) {
        this.username = username;
        this.password = password;
        this.domain = domain;
        this.port = port;
        Protocol = protocol;
    }

    public String getDomain() {
        return domain;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return Protocol;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", domain='" + domain + '\'' +
                ", port=" + port +
                ", Protocol='" + Protocol + '\'' +
                '}';
    }
}
