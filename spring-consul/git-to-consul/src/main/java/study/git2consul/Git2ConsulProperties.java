package study.git2consul;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Git2ConsulProperties {

    private String host;
    private String port;
    private String token;
    private String yamlFile;
    private String consulPath;
}
