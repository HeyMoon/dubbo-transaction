dubbo并没有提供一个分布式系统中的数据一致性的解决方案，
为此，此项目为dubbo添加分布式最终一致性事务

在设计服务时，声明该服务方法是由全局事务管理器管理，该方法中调用的其他服务方法，可以声明为一个事务过程。
当调用一个全局事务方法时，容器将为该次调用生成唯一id，自动记录该事务，以及该全局事务下的所有子事务过程，并记录状态。
由一个定时事务管理器定时扫描记录，按照约定向前或者回滚一个失败的全局事务中已成功且未回滚(向前)的子事务过程。
       
使用方法：

### 1. 声明某个方法是一个全局事务


````
interface SomeService{
    
    @GlobalTransactional
    Response doSomething();
    
}
````

### 2. 声明某个方法是一个子事务过程，并声明对应的回滚方法(方法名_rollback)

````
interface AnotherService{

    @GlobalTransactionalProcess
    Response innerDoSomething();
    
    /**
    *  doSomething 的回滚方法
    **/
    Response innerDoSomething_rollback();
    
}
````

在方法上`@GlobalTransactionalProcess`声明该方法是一个子事务过程，同时也必须定义一个对应的回滚方法。
定时事务管理器会自动调用该回滚方法，由开发者自己实现回滚方法。


### 3. 在数据库中添加全局事务表和事务过程表

````
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
````


### 4. 使用transaction的项目的配置 
在需要使用最终一致性约束的项目中引入：

````
        <dependency>
            <groupId>com.dyh</groupId>
            <artifactId>transaction-api</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

```` 

在`com.alibaba.dubbo.rpc.Filter`中加入以下配置：

````
dubboProviderFilter=com.dyh.transaction.filter.DubboProviderFilter
dubboConsumerFilter=com.dyh.transaction.filter.DubboConsumerFilter
````

同时在dubbo配置文件中加入以下配置：

````
<dubbo:provider timeout="${dubbo.registry.timeout}" filter="dubboProviderFilter"/>
<dubbo:consumer timeout="${dubbo.registry.timeout}" filter="dubboConsumerFilter" retries="0" />

<dubbo:reference interface="com.dyh.transaction.api.GlobalTransactionService" id="globalTransactionService" check="false"/>
<dubbo:reference interface="com.dyh.transaction.api.GlobalTransactionProcessService" id="globalTransactionProcessService" check="false"/>
````


### 5. transaction项目的配置
在`transaction`的pom文件里引入需要使用最终一致性事务的jar,同时在dubbo配置文件里加入以下配置：

````
<dubbo:reference interface="com.dyh.leaf.doc.api.ISomeService2" id="someService2" check="false"/>
````