package org.qts.common.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.qts.common.entity.config.AcctConf;

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
    public MdInfo(AcctConf conf){
        this.id=conf.getId();
        this.type=conf.getMdType();
        this.mdAddress= conf.getMdAddress();
    }
}
