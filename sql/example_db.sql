/*
 Navicat Premium Data Transfer

 Source Server         : index
 Source Server Type    : MySQL
 Source Server Version : 80045
 Source Host           : localhost:3306
 Source Schema         : example_db

 Target Server Type    : MySQL
 Target Server Version : 80045
 File Encoding         : 65001

 Date: 30/05/2026 16:44:34
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for answer_record
-- ----------------------------
DROP TABLE IF EXISTS `answer_record`;
CREATE TABLE `answer_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `is_correct` tinyint(1) NULL DEFAULT 0,
  `answer_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 186 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of answer_record
-- ----------------------------

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `option_a` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `option_b` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `option_c` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `option_d` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `correct_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'single',
  `source_file_id` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_file`(`source_file_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 826 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question
-- ----------------------------
INSERT INTO `question` VALUES (732, '局域网是一个通信子网，而且局域网不存在路由选择问题，因此它不需要网络层，而只包含 OSI 参考模型中的____和____两层。', NULL, NULL, NULL, NULL, '数据链路层、物理层', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (733, 'IEEE802 局域网标准中的数据链路层分为____和____两个子层。', NULL, NULL, NULL, NULL, '逻辑链路控制子层、介质访问控制子层（MAC 子层）', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (734, '计算机局域网常用的访问控制方式有____、、。', NULL, NULL, NULL, NULL, 'CSMA/CD、令牌环、令牌总线', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (735, '____标准定义了 CSMA/CD 访问控制方法。', NULL, NULL, NULL, NULL, 'IEEE 802.3', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (736, 'CSMA/CD 的原理可以概括为____、边听边发、____、随机延迟后重发。', NULL, NULL, NULL, NULL, '先听后发、冲突停止', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (737, '对于采用交换机连网的以太网，其网络拓扑结构为____。', NULL, NULL, NULL, NULL, '星型', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (738, '双机直连保证互连的计算机有相同的子网掩码、网关，不同的____，但两个 IP 地址属于同一____。', NULL, NULL, NULL, NULL, 'IP 地址、网段（网络）', NULL, 'fill', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (739, '局域网参考模型将数据链路层划分为 MAC 子层与（ ）。', '100 BASE-TX', 'PHD', 'LLC', 'ATM', 'C', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (740, '以太网的核心技术是它的随机争用型介质访问控制方法，即（ ）。', 'Token Ring', 'Token bus', 'XMI', 'CSMA/CD', 'D', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (741, '想知道www.163.com域名的 IP 地址可以用（ ）命令查找。', 'Netstat', 'ping', 'ipconfig', 'tracert', 'B', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (742, 'LLC 子层的功能和服务在（ ）中进行定义。', 'IEEE802.1', 'IEEE802.2', 'IEEE802.3', 'IEEE802.4', 'B', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (743, '在 MAC 子层中，数据传输的基本单元是（ ）。', '比特流', 'MAC 帧', 'LLC PDU', '数据报', 'B', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (744, '在局域网参考模型中，实现和维护 MAC 协议的是（ ）。', '物理层', '逻辑链路控制子层', '介质访问控制子层', '网络层', 'C', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (745, '以下各项中，是以太网媒体访问控制方法的标准是（ ）。', 'IEEE802.3', 'IEEE802.4', 'IEEE802.5', 'IEEE802.6', 'A', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (746, '令牌环（Token Ring）的访问方法和物理技术规范由（ ）描述。', 'IEEE 802.2', 'IEEE 802.3', 'IEEE802.4', 'IEEE 802.5', 'D', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (747, 'IEEE802 工程标准中的 802.4 协议是（ ）。', '局域网的载波侦听多路访问标准', '局域网的令牌总线标准', '局域网的令牌环网标准', '局域网的互联标准', 'B', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (748, 'IEEE802 为局域网规定的标准只对应于 OSI 参考模型的（ ）。', '第一层', '第二层', '第一层和第二层', '第二层和第三层', 'C', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (749, '连接到 Internet 的计算机中，必须安装的协议是（ ）。', '双边协议', 'TCP/IP 协议', 'NetBEUI 协议', 'SPSS 协议', 'B', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (750, '在局域网中，价格低廉且可靠性高的传输介质是（ ）。', '粗同轴电缆', '细同轴电缆', '双绞线', '光缆', 'C', NULL, 'single', 46, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question` VALUES (751, '交换机如何处理进入的广播帧？', '查找 MAC 地址表后转发', '直接执行泛洪操作', '丢弃该帧', '转发到特定端口', 'B', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (752, '以下哪个是合法的公网 IP 主机地址？', '127.2.3.5', '172.30.15.255', '231.23.200.9', '192.240.150.254', 'C', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (753, 'IP 地址 219.25.23.56 的缺省子网掩码有几位？', '8', '16', '24', '32', 'C', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (754, '对于一个子网掩码为 255.255.255.240 的 C 类 IP 地址，每个网段能提供多少可用 IP 地址？', '16', '32', '30', '14', 'D', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (755, '如果交换机接收到一个单播帧，且该帧的目的 MAC 地址在 MAC 地址表中找不到，交换机会怎么做？', '丢弃该帧', '转发到所有端口（泛洪）', '转发到特定端口', '查找路由表', 'B', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (756, '以下哪个 IP 地址与 10.110.12.29（子网掩码 255.255.255.224）属于同一网段？', '10.110.12.0', '10.110.12.30', '10.110.12.31', '10.110.12.32', 'B', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (757, '网络地址 192.168.1.0/24，使用子网掩码 255.255.255.224 划分后，每个子网的有效主机数是多少？', '30', '31', '32', '62', 'A', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (758, '以下哪个命令可以创建主干端口并允许 VLAN20 通过？', 'switchport trunk encapsulation dot1q', 'switchport mode trunk', 'switchport trunk allowed vlan 20', 'switchport trunk native vlan 20', 'C', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (759, '关于交换机的学习能力，以下描述正确的是？', '交换机只学习源 MAC 地址', '交换机不学习目的 MAC 地址', '交换机学习后，将 MAC 地址与端口号映射存储在 MAC 地址表中', '交换机不存储任何 MAC 地址信息', 'C', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (760, 'IP 地址 190.233.27.13/16 的网络部分地址是？', '190.0.0.0', '190.233.0.0', '190.233.27.0', '190.233.27.1', 'B', NULL, 'single', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (761, '交换机上的 FastEthernet 接口通常用于初次配置交换机。', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (762, '在子网掩码为 255.255.255.0 的网络中，每个子网可以支持 254 台主机。', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (763, '如果进入交换机的是一个单播帧，且该帧的目的 MAC 地址在 MAC 地址表中找到了，但对应的端口不是该帧进入交换机的端口，交换机会执行丢弃操作。', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (764, 'IP 地址 127.0.0.1 是一个合法的公网 IP 地址。', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (765, '子网掩码的主要作用是区分网络地址和主机地址。', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (766, '关于交换机的工作原理，以下描述正确的是？', '如果进入交换机的是一个广播帧，则交换机不会去查 MAC 地址表，而是直接执行泛洪操作。', '如果一个单播帧的目的 MAC 在 MAC 地址表中查不到，则交换机执行泛洪操作。', '如果一个单播帧的目的 MAC 在 MAC 地址表中找到了，但对应的端口是该帧进入交换机的端口，则交换机执行丢弃操作。', '交换机具有学习能力，能够学习源 MAC 地址并存储在 MAC 地址表中。', 'ABCD', NULL, 'multiple', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (767, '以下哪些 IP 地址与 172.25.15.201（子网掩码 255.255.0.0）在同一网段中？', '172.25.15.202', '172.25.16.15', '172.25.201.15', '172.24.15.201', 'AC', NULL, 'multiple', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (768, '以下哪些是关于子网掩码的正确说法？', '子网掩码用于区分网络地址和主机地址。', '子网掩码的长度决定了可以划分的子网数量。', '子网掩码的长度决定了每个子网中的主机数量。', '子网掩码可以随意设置，不受任何限制。', 'ABC', NULL, 'multiple', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (769, '以下哪些命令可以用于查看交换机接口所属的 VLAN？', 'show interface f0/16', 'show spanning-tree', 'show running-config', 'show version', 'AC', NULL, 'multiple', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (770, '以下哪些是关于交换机端口工作模式的正确说法？', '交换机端口可以工作在半双工模式。', '交换机端口可以工作在全双工模式。', '100Mbps 的全双工端口，端口上下行总带宽为 200Mbps。', '交换机的所有端口都必须工作在同一模式下。', 'ABC', NULL, 'multiple', 47, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question` VALUES (771, '下列选项中用于启动 MySQL 服务器的命令是（1.0）', 'net start', 'net start mysql', 'net stop mysql', 'start mysql', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (772, '下列选项中不属于按照应用层次划分的数据模型是（1.0）', '概念数据模型', '逻辑数据模型', '物理数据模型', '关系数据模型', 'D', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (773, '下列选项中()语句可以在修改数据表时将字段 id 添加在数据表的第一列。（1.0）', 'ALTER TABLE dept ADD FIRST id INT ;', 'ALTER TABLE dept ADD id INT FIRST ;', 'ALTER TABLE dept ADD AFTER id INT ;', 'ALTER TABLE dept ADD id INT AFTER ;', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (774, '在如下语句中,name_index表示( )。', '索引类型', '索引名称', '索引方式', '索引字段', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (775, '若数据库中存在以下数据表,语句 SHOW TABLES LIKE \'sh_\'的结果为()（1.0）', 'fish', 'mydb', 'she', 'unshift', 'C', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (776, '阅读下面SQL语句:', '查询年龄大于21的员工信息', '查询存在年龄大于21的员工所对应的员工信息', '查询存在年龄大于21的员工所对应的部门信息', '查询存在年龄大于21的员工信息', 'C', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (777, '下面选项中,代表匹配任意长度字符串的通配符是( )（1.0）', '%', '*', '_', '?', 'A', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (778, '下列选项中,不属于MySQL中的索引的是( )（1.0）', '普通索引', '主键索引', '唯一性索引', '外键索引', 'D', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (779, '要想分页(每页显示10条)显示test表中的数据,那么获取第2页数据的SQL语句是( )（1.0）', 'SELECT * FROM test LIMIT 10,10;', 'SELECT * FROM test LIMIT 11,10;', 'SELECT * FROM test LIMIT 10,20;', 'SELECT * FROM test LIMIT 11,20;', 'A', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (780, '下面选项中,用于求出表中某个字段所有值的总和的函数是( )（1.0）', 'AVG()', 'SUM()', 'MIN()', 'MAX()', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (781, '下列选项中可以查看数据表的创建信息的是()。（1.0）', 'SHOW TABLES ;', 'DESC 数据表名;', 'SHOW TABLE ;', 'SHOW CREATE TABLE 数据表名', 'D', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (782, '下列关于UPDATE语句的描述正确的是（1.0）', 'UPDATE只能更新表中的部分记录', 'UPDATE只能更新表中的全部记录', 'UPDATE语句更新数据时可以有条件地更新记录', '以上说法都不对', 'C', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (783, '下列关于更新数据的SQL语句正确的是（1.0）', 'UPDATE user SET id = u001;', 'UPDATE user (id,username ) VALUES ( u001\',jack\' ) ;', 'UPDATE user SET id=\'u001\',username=jack\';', 'UPDATE INTO user SET id = u001\', username=\'jack\';', 'C', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (784, '下列选项中,可以查询student表中id字段值小于5,并且gender字段值为“女”的学生姓名的SQL语句是( )（1.0）', 'SELECT name FROM student WHERE id<5 OR gender=\'女\';', 'SELECT name FROM student WHERE id<5 AND gender=\'女\';', 'SELECT name FROM student WHERE id<5 ,gender=\'女\';', 'SELECT name FROM student WHERE id<5 AND WHERE gender=\'女\';', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (785, '下面选项中,查询student表中id值不在2和5之间的学生的SQL语句是( )（1.0）', 'SELECT * FROM student where id!=2,3,4,5;', 'SELECT * FROM student where id not between 5 and 2;', 'SELECT * FROM student where id not between 2 and 5;', 'SELECT * FROM student where id not in 2,3,4,5;', 'C', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (786, '下列选项中向数据表 Student 中添加id为1、name为小王的SQL语正确是（1.0）', 'INSERT INTO Student ( \"id\",\"name\" ) VALUES ( 1,\"小王\");', 'INSERT INTO Student (id,name ) VALUES (1,\" 小王\");', 'INSERT INTO Student VALUES ( 1,小王);', 'INSERT INTO Student ( id,\"name\" )VALUES ( 1,\" 小王\");', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (787, '下列选项中,用于定义全文索引的是( )（1.0）', '由Key定义的索引', '由FULLTEXT定义的索引', '由Unique定义的索引', '由Index定义的索引', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (788, '下列选项中()是 MySQL 默认提供的用户。（1.0）', 'admin', 'test', 'root', 'user', 'C', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (789, '下列选项中对约束的描述错误的是()。（1.0）', '每个数据表中最多只能设置一个主键约束', '非空约束通过 NOT NULL 进行限定', '唯一约束通过关键字 UNIQUE 进行设置', '只可以在数据表中设置1个唯一约束', 'D', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (790, '下列关于删除数据表记录的 SOL 语句正确的是（1.0）', 'DELETE student ,where id=11;', 'DELETE FROM student where id=11;', 'DELETE INTO student where id=11;', 'DELETE student where id=11;', 'B', NULL, 'single', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (791, '使用ORDER BY对查询结果进行排序时,默认是按____排列。（1.0）', NULL, NULL, NULL, NULL, '：', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (792, '____关键字结合子查询时,表示子查询的查询结果集中的任一查询结果。（1.0）', NULL, NULL, NULL, NULL, 'any', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (793, '在MySQL中可以使用____语句向数据表中插入数据。', NULL, NULL, NULL, NULL, 'insert', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (794, 'MySQL提供____语句用于删除表中的数据。（1.0）', NULL, NULL, NULL, NULL, 'delete', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (795, 'MySQL中提供了________关键字,可以在查询时去除重复的值。', NULL, NULL, NULL, NULL, 'distinct', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (796, '聚合函数中,用于求出某个字段平均值的函数是____。（1.0）', NULL, NULL, NULL, NULL, 'avg()', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (797, '只有在查询条件中使用了复合索引中的____字段时,该复合索引才会被使用。（1.0）', NULL, NULL, NULL, NULL, '：', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (798, '普通索引使用KEY或____定义。（1.0）', NULL, NULL, NULL, NULL, 'index', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (799, '交叉连接查询返回的结果是被连接的两张数据表中所有数据行的____。（1.0）', NULL, NULL, NULL, NULL, '：', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (800, '概念数据模型中实体与实体之间的联系有____、____、____3种情况。（1.0）', NULL, NULL, NULL, NULL, '：', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (801, 'SELECT语句中,用于对分组查询结果再进行过滤的关键字是____。（1.0）', NULL, NULL, NULL, NULL, 'having', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (802, '数据模型所描述的内容包括3个部分,分别是数据结构、数据操作和____（1.0）', NULL, NULL, NULL, NULL, '：', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (803, '在 MySQL 中,主键约束是通过____进行设置。（1.0）', NULL, NULL, NULL, NULL, 'primary', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (804, '添加新数据时,如果没有为某个字段赋值,系统会自动为该字段添加____。', NULL, NULL, NULL, NULL, '：', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (805, 'MySQL中使用____语句来更新表中的记录。（1.0）', NULL, NULL, NULL, NULL, 'update', NULL, 'fill', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (806, '如果插入的数据有多条,则多条数据之间用逗号隔开。（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (807, '在对字符串进行模糊查询中,一个下画线通配符可匹配多个字符。（1.0）', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (808, '子查询是指一个查询语句嵌套在另一个语句内部的查询。( )（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (809, '数据库中的数据只包括普通意义上的数字和文字。（1.0）', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (810, '外键所在列的数据类型必须和主表中主键对应列的数据类型相同。( )（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (811, 'SELECT语句中可以使用AS关键字指定表名的别名或字段的别名,AS关键字也可以省略不写。（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (812, '关系模型的数据结构是二维表。（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (813, '当DISTINCT作用多个字段时,只有DISTINCT关键字后指定的多个字段值都相同,才会被认作是重复记录。（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (814, '在 ALTER TABLE 语句中, CHANGE 子句仅可以修改字段名。（1.0）', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (815, '内连接使用INNER JOIN关键字连接两张表,其中INNER关键字可以省略。( )（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (816, '关系模型结构简单明了,并且有逻辑计算、数学计算等坚实的数学理论作为基础。（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (817, '在数据表中,某些列的值可能为空值(NULL),那么在SQL语句中可以通过', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (818, '数据库系统阶段实现了数据独立和共享,但数据的独立与共享能力都非常薄弱。（1.0）', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (819, '索引不会占用一定的磁盘空间,数据表中索引越多查询效率越高。（1.0）', NULL, NULL, NULL, NULL, '错误', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (820, '一个数据表中只能有一个字段设置 AUTO _ INCREMENT 。（1.0）', NULL, NULL, NULL, NULL, '正确', NULL, 'judge', 48, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question` VALUES (821, '<img src=\"/uploads/849396c8-b47d-4c50-b273-0f0ac8691707.png\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />', NULL, NULL, NULL, NULL, 'CREATE TABLE 学生选课表( 学号 int(10) NOT NULL PRIMARY KEY, 课程 varchar(16) NOT NULL, 成绩 float(8) );', NULL, 'essay', 49, '2026-05-30 16:40:12', '2026-05-30 16:40:12');
INSERT INTO `question` VALUES (822, '<img src=\"/uploads/a728cba3-4b99-407e-96dc-6e1903bfd45a.png\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />', NULL, NULL, NULL, NULL, 'SELECT 性别 FROM 企业员工表 GROUP BY 性别;', NULL, 'essay', 50, '2026-05-30 16:40:34', '2026-05-30 16:40:34');
INSERT INTO `question` VALUES (823, '<img src=\"/uploads/25ff54c0-1ec5-452c-895c-a320f6c6f59b.png\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />', NULL, NULL, NULL, NULL, 'INSERT INTO 企业员工表 VALUES(‘005’,‘周旋’，‘女’，‘销售’);', NULL, 'essay', 51, '2026-05-30 16:40:48', '2026-05-30 16:40:48');
INSERT INTO `question` VALUES (824, '<img src=\"/uploads/6e4b15ef-3bda-4c80-a554-d6ab4efee5ba.png\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />', NULL, NULL, NULL, NULL, 'DELETE FROM 教材信息表 WHERE 教材名称=’软件技术’;', NULL, 'essay', 52, '2026-05-30 16:41:02', '2026-05-30 16:41:02');
INSERT INTO `question` VALUES (825, '<img src=\"/uploads/e6dafd8f-bb4d-4309-8b8d-f6b3ffd90fbc.png\" alt=\"题目图片\" style=\"max-width:100%;height:auto;\" />', NULL, NULL, NULL, NULL, 'SELECT * FROM 企业员工表 WHERE 职位=’销售’;', NULL, 'essay', 53, '2026-05-30 16:41:15', '2026-05-30 16:41:15');

-- ----------------------------
-- Table structure for question_bank
-- ----------------------------
DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `total_questions` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 54 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question_bank
-- ----------------------------
INSERT INTO `question_bank` VALUES (46, '3.docx', 'word', NULL, 19, '2026-05-30 16:36:29', '2026-05-30 16:36:29');
INSERT INTO `question_bank` VALUES (47, '4.docx', 'word', NULL, 20, '2026-05-30 16:36:58', '2026-05-30 16:36:58');
INSERT INTO `question_bank` VALUES (48, '模拟题答案.docx', 'word', NULL, 50, '2026-05-30 16:37:47', '2026-05-30 16:37:47');
INSERT INTO `question_bank` VALUES (49, '手动添加题目', 'manual', NULL, 1, '2026-05-30 16:40:12', '2026-05-30 16:40:12');
INSERT INTO `question_bank` VALUES (50, '手动添加题目', 'manual', NULL, 1, '2026-05-30 16:40:34', '2026-05-30 16:40:34');
INSERT INTO `question_bank` VALUES (51, '手动添加题目', 'manual', NULL, 1, '2026-05-30 16:40:48', '2026-05-30 16:40:48');
INSERT INTO `question_bank` VALUES (52, '手动添加题目', 'manual', NULL, 1, '2026-05-30 16:41:02', '2026-05-30 16:41:02');
INSERT INTO `question_bank` VALUES (53, '手动添加题目', 'manual', NULL, 1, '2026-05-30 16:41:15', '2026-05-30 16:41:15');

SET FOREIGN_KEY_CHECKS = 1;
