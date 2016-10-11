package fi.aalto.openoranges.project1.mcc;


public class Application {
    private String name;
    private String id;
    private String icon_url;
    private String description;

    public Application(String name, String id, String icon_url, String run_url) {
        super();
        this.name = name;
        this.id = id;
        this.icon_url = icon_url;
        this.description = run_url;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getIcon_url() {
        return icon_url;
    }
}
