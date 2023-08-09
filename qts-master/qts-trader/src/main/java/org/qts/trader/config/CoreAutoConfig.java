package org.qts.trader.config;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.dao.AcctMapper;
import org.qts.common.entity.config.AcctConf;
import org.qts.trader.core.AcctExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CoreAutoConfig {

    @Value("${acctId}")
    private String acctId;

}
