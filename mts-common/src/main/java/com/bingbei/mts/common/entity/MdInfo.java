package com.bingbei.mts.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MdInfo {
    private String id;
    private String type;
    private String mdAddress;


    public MdInfo(String id,String address){
        this.id=id;
        this.type=address.split("\\|")[0];
        this.mdAddress=address.split("\\|")[1];
    }

}
