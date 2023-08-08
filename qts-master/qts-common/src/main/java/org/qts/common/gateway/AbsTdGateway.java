package org.qts.common.gateway;

import lombok.extern.slf4j.Slf4j;
import org.qts.common.disruptor.FastEventService;
import org.qts.common.entity.LoginInfo;

@Slf4j
public abstract class AbsTdGateway implements TdGateway {

	protected LoginInfo loginInfo;
	protected FastEventService fastEventEngineService;

	public AbsTdGateway(FastEventService fastEventEngineService, LoginInfo loginInfo) {
		this.fastEventEngineService = fastEventEngineService;
		this.loginInfo=loginInfo;
		log.info(this.loginInfo.getAccoutId() + "开始初始化");
	}

	@Override
	public void emitEvent(String eventType, Object obj) {
		this.fastEventEngineService.emitEvent(eventType,obj);
	}

	@Override
	public LoginInfo getLoginInfo() {
		return loginInfo;
	}
}
