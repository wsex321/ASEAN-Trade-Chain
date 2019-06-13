# ASTC公链部署文档
---
## 环境搭建
Centos7.0为例：
1. 开放端口7876、7877、1905、1906    
```bash
yum install java-1.8.0
```
2. 设置防火墙策略
3. 部署jar包并启动（astc-chain.jar）

4. 安装JDK1.8.0

5. 开放端口7876、7877、1905、1906  
6. 设置防火墙策略
7. 部署jar包并启动（astc-chain.jar）
```bash
## 启动命令
nohup java -jar astc-chain.jar > nohup.out 2>&1 &
## 日志输入文件在同目录下的nohup文件
```
>账户私钥目录  
>默认：/root/account  
> 重启：pwdx `pgrep java` 查找出PID，直接kill 再使用启动命令重启，重启完后，挖矿节点需开启挖矿

## 开启节点挖矿
http://103.117.133.112:7877/FoundryMachineController/startFoundryMachine?passWord=wushihao&address=ax43c32b16ef5393a0f9e6e77c354f82ca4b9d3687  
返回结果116则成功

