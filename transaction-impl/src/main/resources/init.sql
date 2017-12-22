CREATE TABLE `global_transaction` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `status` smallint(2) NOT NULL DEFAULT '1' COMMENT '状态，1：新建；2：成功；3：失败；4：已回滚；5：已部分回滚；99：挂起；',
  `curr_sequence` int(11) NOT NULL COMMENT '当前过程序列号',
  `created_at` datetime NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='全局事务表';

CREATE TABLE `global_transaction_process` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `transaction_id` int(11) NOT NULL,
  `transaction_sequence` int(11) NOT NULL COMMENT '过程所属序列号',
  `status` smallint(2) NOT NULL DEFAULT '1' COMMENT '过程当前状态，1：新建；2：成功；3：失败；4：未知，5：已回滚；',
  `expected_status` smallint(6) NOT NULL DEFAULT '1' COMMENT '过程目标状态，1：成功；2：已回滚；',
  `service_name` varchar(128) NOT NULL COMMENT '服务名称',
  `version_name` varchar(32) NOT NULL COMMENT '服务版本',
  `method_name` varchar(32) NOT NULL COMMENT '方法名称',
  `rollback_method_name` varchar(32) NOT NULL COMMENT '回滚方法名称',
  `request_json` text COMMENT '过程请求参数Json序列化',
  `response_json` text NOT NULL COMMENT '过程响应参数Json序列化',
  `retry_time_count` int(11) DEFAULT '0' COMMENT '重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `created_at` datetime NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COMMENT='事务过程表';