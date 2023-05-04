package org.mts.admin.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author sun0x00@gmail.com
 */
@Data
public class LogData implements Serializable {
	private static final long serialVersionUID = 7122255887442856581L;

	private long timestamp = System.currentTimeMillis(); // 日志创建时间
	private String level = Constant.LOG_INFO; // 日志级别
	private String threadName; //　线程信息
	private String className; // 类名
	private String content; // 日志信息

}
