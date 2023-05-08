package org.mts.admin.entity.po;

import lombok.Data;

@Data
public class RouterPo {
    private String path;
    private String name;
    private String redirect;

    private String title;
    private String icon;
    private Integer rank;
    private Boolean showLink;
    private Boolean keepAlive;
    private Integer dynamicLevel;
    private String refreshRedirect;
    private String parent;
}
