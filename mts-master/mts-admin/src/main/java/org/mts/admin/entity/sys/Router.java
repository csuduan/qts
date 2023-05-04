package org.mts.admin.entity.sys;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Router {
    private String path;
    private String name;
    private String redirect;
    private RounterMeta meta;
    private List<Router> children;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class RounterMeta{
        private String title;
        private String icon;
        private Integer rank;
        private Boolean showLink;
        private Boolean keepAlive;
        private Integer dynamicLevel;
        private String refreshRedirect;


    }
}
