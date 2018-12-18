package Bean;

import java.util.Objects;

public class Host {
    private String name;
    private String ip;
    private int port;
    private String img;

    public Host(String name, String ip, int port, String img) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.img = img;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Host host = (Host) o;

        if (port != host.port) return false;
        if (!Objects.equals(name, host.name)) return false;
        if (!Objects.equals(ip, host.ip)) return false;
        return Objects.equals(img, host.img);
    }
}
